package org.bpel4chor.splitprocess.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bpel4chor.splitprocess.exceptions.ActivityNotFoundException;
import org.bpel4chor.splitprocess.exceptions.ParentNotFoundException;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Assign;
import org.eclipse.bpel.model.BPELExtensibleElement;
import org.eclipse.bpel.model.Compensate;
import org.eclipse.bpel.model.CompensateScope;
import org.eclipse.bpel.model.Else;
import org.eclipse.bpel.model.ElseIf;
import org.eclipse.bpel.model.Empty;
import org.eclipse.bpel.model.Exit;
import org.eclipse.bpel.model.ExtensionActivity;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.ForEach;
import org.eclipse.bpel.model.If;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.OnAlarm;
import org.eclipse.bpel.model.OnMessage;
import org.eclipse.bpel.model.OpaqueActivity;
import org.eclipse.bpel.model.Pick;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Receive;
import org.eclipse.bpel.model.RepeatUntil;
import org.eclipse.bpel.model.Reply;
import org.eclipse.bpel.model.Rethrow;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Sequence;
import org.eclipse.bpel.model.Throw;
import org.eclipse.bpel.model.Validate;
import org.eclipse.bpel.model.Wait;
import org.eclipse.bpel.model.While;
import org.w3c.dom.Element;

/**
 * ActivityFinder finds the BPEL Activity/ExtensibleEleemnt, that resides in the
 * given BPEL process, upon the given DOM element or the given name of the
 * activity, additionally, it also finds the parent of a child activity, whose
 * name is provided by invoker.
 * 
 * <p>
 * A couple of <b>maps</b> used in this class are for optimisation purpose, they
 * cache the previously found key-value pair, as of next time, value for the
 * incoming query against the same key can directly retrieved from the map,
 * instead of going through the whole process again.
 * 
 * @since Dec 8, 2011
 * @author Daojun Cui
 */
public class ActivityFinder {

	/** The BPEL process */
	private Process process = null;

	/** The element to activity map, the element is contained in the activity */
	private Map<Element, Activity> element2ActivityMap = null;

	/** The activity name to activity map */
	private Map<String, Activity> name2ActivityMap = null;

	/** The child to parent map */
	private Map<String, BPELExtensibleElement> child2ParentMap = null;

	/**
	 * Constructor
	 * 
	 * @param process
	 *            BPEL process
	 */
	public ActivityFinder(Process process) {
		this.process = process;
		this.element2ActivityMap = new HashMap<Element, Activity>();
		this.name2ActivityMap = new HashMap<String, Activity>();
		this.child2ParentMap = new HashMap<String, BPELExtensibleElement>();
	}

	/**
	 * Find the activity that contains the given element
	 * 
	 * @param element
	 *            DOM Element of a activity
	 * @return the activity that contains the given element, <tt>null</tt> if
	 *         none is found.
	 */
	public Activity find(Element element) {

		if (element == null)
			throw new NullPointerException("argument is null");

		Activity act = element2ActivityMap.get(element);
		if (act != null)
			return act;

		// start with the direct child activity in process
		act = process.getActivity();

		Activity found = findByActivity(act, element);

		return found;

	}

	/**
	 * Find the activity with the given name.
	 * 
	 * <p>
	 * Use Cases of this method
	 * <ol>
	 * <li>Given the main process, find the the activity with the given name.
	 * <li>Given the fragment process, find the corresponding activity that has
	 * the name same as the given one.
	 * </ol>
	 * 
	 * @param actName
	 *            The name of the activity
	 * @return The activity that has the given name
	 * @throws ActivityNotFoundException
	 * 
	 * @deprecated better use {@link MyBPELUtils#resolveActivity(String, Process)}
	 * 
	 */
	public Activity find(String actName) throws ActivityNotFoundException {

		if (actName == null)
			throw new NullPointerException("argument is null, actName == null:" + (actName == null));
		if (actName.isEmpty())
			throw new IllegalArgumentException("argument actName is empty");

		Activity act = name2ActivityMap.get(actName);
		if (act != null)
			return act;

		act = process.getActivity();
		Activity found = findByActivity(act, actName);

		if (found == null)
			throw new ActivityNotFoundException("Can not find activity with :" + actName);

		return found;
	}

	/**
	 * Find the parent of the activity with the given name
	 * 
	 * @param actName
	 *            The activity name
	 * @return The parent of the given activity, or <tt>null</tt>
	 * @throws ParentNotFoundException
	 */
	public BPELExtensibleElement findParent(String actName) throws ActivityNotFoundException {

		if (actName == null)
			throw new NullPointerException("argument is null, actName == null:" + (actName == null));
		if (actName.isEmpty())
			throw new IllegalArgumentException("argument actName is empty");

		BPELExtensibleElement parent = child2ParentMap.get(actName);

		if (parent != null) {
			return parent;
		}

		// always save the parent information before go down a level.
		Activity act = process.getActivity();
		child2ParentMap.put(act.getName(), process);

		parent = findParentByActivity(act, actName);

		if (parent == null)
			throw new ParentNotFoundException("Can not find parent for :" + actName);

		return parent;
	}

	/**
	 * Find the activity with the given name recursively.
	 * 
	 * @param startWith
	 *            The activity where the finding starts
	 * @param actName
	 *            The wanted activity name
	 * @return The found activity, or <tt>null</tt>
	 * 
	 * @see {@link #findByActivity(Activity, Element)},
	 *      {@link #findParentByActivity(Activity, String)}
	 */
	private Activity findByActivity(Activity startWith, String actName) {

		if (startWith == null || actName == null)
			throw new NullPointerException("argument is null, startWith == null:" + (startWith == null)
					+ " actName ==null:" + (actName == null));

		if (name2ActivityMap.get(startWith.getName()) == null) {
			name2ActivityMap.put(startWith.getName(), startWith);
		}

		Activity foundActivity = null;

		if (startWith instanceof Empty)
			foundActivity = findByEmpty((Empty) startWith, actName);
		else if (startWith instanceof Invoke)
			foundActivity = findByInvoke((Invoke) startWith, actName);
		else if (startWith instanceof Assign)
			foundActivity = findByAssign((Assign) startWith, actName);
		else if (startWith instanceof Reply)
			foundActivity = findByReply((Reply) startWith, actName);
		else if (startWith instanceof Receive)
			foundActivity = findByReceive((Receive) startWith, actName);
		else if (startWith instanceof Wait)
			foundActivity = findByWait((Wait) startWith, actName);
		else if (startWith instanceof Throw)
			foundActivity = findByThrow((Throw) startWith, actName);
		else if (startWith instanceof Exit)
			foundActivity = findByExit((Exit) startWith, actName);
		else if (startWith instanceof Flow)
			foundActivity = findByFlow((Flow) startWith, actName);
		else if (startWith instanceof If)
			foundActivity = findByIf((If) startWith, actName);
		else if (startWith instanceof While)
			foundActivity = findByWhile((While) startWith, actName);
		else if (startWith instanceof Sequence)
			foundActivity = findBySequence((Sequence) startWith, actName);
		else if (startWith instanceof Pick)
			foundActivity = findByPick((Pick) startWith, actName);
		else if (startWith instanceof Scope)
			foundActivity = findByScope((Scope) startWith, actName);
		else if (startWith instanceof Compensate)
			foundActivity = findByCompensate((Compensate) startWith, actName);
		else if (startWith instanceof CompensateScope)
			foundActivity = findByCompensateScope((CompensateScope) startWith, actName);
		else if (startWith instanceof Rethrow)
			foundActivity = findByRethrow((Rethrow) startWith, actName);
		else if (startWith instanceof OpaqueActivity)
			foundActivity = findByOpaqueActivity((OpaqueActivity) startWith, actName);
		else if (startWith instanceof ForEach)
			foundActivity = findByForEach((ForEach) startWith, actName);
		else if (startWith instanceof RepeatUntil)
			foundActivity = findByRepeatUntil((RepeatUntil) startWith, actName);
		else if (startWith instanceof Validate)
			foundActivity = findByValidate((Validate) startWith, actName);
		else if (startWith instanceof ExtensionActivity)
			foundActivity = findByExtensionActivity((ExtensionActivity) startWith, actName);

		return foundActivity;
	}

	private Activity findByExtensionActivity(ExtensionActivity startWith, String actName) {
		return startWith.getName().equals(actName) ? startWith : null;
	}

	private Activity findByValidate(Validate startWith, String actName) {
		return startWith.getName().equals(actName) ? startWith : null;
	}

	private Activity findByRepeatUntil(RepeatUntil startWith, String actName) {
		if (startWith.getName().equals(actName)) {
			return startWith;
		}

		Activity found = findByActivity(startWith.getActivity(), actName);
		return found;
	}

	private Activity findByForEach(ForEach startWith, String actName) {

		if (startWith.getName().equals(actName)) {
			return startWith;
		}
		Activity found = findByActivity(startWith.getActivity(), actName);
		return found;
	}

	private Activity findByOpaqueActivity(OpaqueActivity startWith, String actName) {
		return startWith.getName().equals(actName) ? startWith : null;
	}

	private Activity findByRethrow(Rethrow startWith, String actName) {
		return startWith.getName().equals(actName) ? startWith : null;
	}

	private Activity findByCompensateScope(CompensateScope startWith, String actName) {
		return startWith.getName().equals(actName) ? startWith : null;
	}

	private Activity findByCompensate(Compensate startWith, String actName) {
		return startWith.getName().equals(actName) ? startWith : null;
	}

	private Activity findByScope(Scope startWith, String actName) {
		if (startWith.getName().equals(actName)) {
			return startWith;
		}
		Activity found = findByActivity(startWith.getActivity(), actName);
		return found;
	}

	private Activity findByPick(Pick startWith, String actName) {
		if (startWith.getName().equals(actName))
			return startWith;

		Activity found = null;
		List<?> listOfOnMessage = startWith.getMessages();
		for (Object next : listOfOnMessage) {
			found = findByOnMessage((OnMessage) next, actName);
			if (found != null)
				return found;
		}

		List<?> listOfOnAlarm = startWith.getAlarm();
		for (Object next : listOfOnAlarm) {
			found = findByOnAlarm((OnAlarm) next, actName);
			if (found != null)
				return found;
		}
		return null;
	}

	private Activity findByOnMessage(OnMessage startWith, String actName) {
		Activity found = findByActivity(startWith.getActivity(), actName);

		return found;

	}

	private Activity findByOnAlarm(OnAlarm start, String actName) {
		Activity found = findByActivity(start.getActivity(), actName);

		return found;
	}

	private Activity findBySequence(Sequence startWith, String actName) {
		if (startWith.getName().equals(actName))
			return startWith;

		Activity res = null;
		List<?> listOfActivities = startWith.getActivities();
		for (Object next : listOfActivities) {
			res = findByActivity((Activity) next, actName);
			if (res != null)
				return res;
		}
		return null;
	}

	private Activity findByWhile(While startWith, String actName) {
		if (startWith.getName().equals(actName))
			return startWith;

		Activity res = null;
		if (startWith.getActivity() != null) {
			res = findByActivity(startWith.getActivity(), actName);
		}
		return res;
	}

	private Activity findByIf(If startWith, String actName) {
		if (startWith.getName().equals(actName))
			return startWith;

		Activity res = null;
		// if
		if (startWith.getActivity() != null) {
			res = findByActivity(startWith.getActivity(), actName);
			if (res != null)
				return res;
		}
		// elseIf
		List<?> elseIfs = startWith.getElseIf();
		if (!elseIfs.isEmpty()) {
			for (Object next : elseIfs) {
				ElseIf elseIf = (ElseIf) next;
				res = findByElseIf(elseIf, actName);
				if (res != null)
					return res;
			}
		}
		// else
		Else elseBranch = startWith.getElse();
		if (elseBranch != null) {
			res = findByElse(elseBranch, actName);
			if (res != null)
				return res;
		}
		return null;
	}

	private Activity findByElseIf(ElseIf startWith, String actName) {

		Activity res = null;
		if (startWith.getActivity() != null) {
			res = findByActivity(startWith.getActivity(), actName);
		}

		return res;
	}

	private Activity findByElse(Else startWith, String actName) {
		Activity res = null;
		if (startWith.getActivity() != null) {
			res = findByActivity(startWith.getActivity(), actName);
		}
		return res;
	}

	private Activity findByFlow(Flow startWith, String actName) {
		if (startWith.getName().equals(actName))
			return startWith;

		List<?> listOfActivities = startWith.getActivities();

		Activity res = null;

		for (Object next : listOfActivities) {
			res = findByActivity((Activity) next, actName);
			if (res != null)
				return res;
		}

		return null;
	}

	private Activity findByExit(Exit startWith, String actName) {
		return startWith.getName().equals(actName) ? startWith : null;
	}

	private Activity findByThrow(Throw startWith, String actName) {
		return startWith.getName().equals(actName) ? startWith : null;
	}

	private Activity findByWait(Wait startWith, String actName) {
		return startWith.getName().equals(actName) ? startWith : null;
	}

	private Activity findByReceive(Receive startWith, String actName) {
		return startWith.getName().equals(actName) ? startWith : null;
	}

	private Activity findByReply(Reply startWith, String actName) {
		return startWith.getName().equals(actName) ? startWith : null;
	}

	private Activity findByAssign(Assign startWith, String actName) {
		return startWith.getName().equals(actName) ? startWith : null;
	}

	private Activity findByInvoke(Invoke startWith, String actName) {
		return startWith.getName().equals(actName) ? startWith : null;
	}

	private Activity findByEmpty(Empty startWith, String actName) {
		return startWith.getName().equals(actName) ? startWith : null;
	}

	/**
	 * Find the activity that the given element describes, start with the given
	 * activity. The iteration direction is recursively top-down.
	 * 
	 * @param startWith
	 *            the activity to start with
	 * @param element
	 *            the element that the wanted activity should contains
	 * @return the activity if one is found, otherwise <tt>null</tt>
	 */
	private Activity findByActivity(Activity startWith, Element element) {

		if (startWith == null)
			return null;

		// save the element to activity pair if it is not saved before
		if (element2ActivityMap.get(startWith.getElement()) == null) {
			element2ActivityMap.put(startWith.getElement(), startWith);
		}

		Activity foundActivity = null;

		if (startWith instanceof Empty)
			foundActivity = findByEmpty((Empty) startWith, element);
		else if (startWith instanceof Invoke)
			foundActivity = findByInvoke((Invoke) startWith, element);
		else if (startWith instanceof Assign)
			foundActivity = findByAssign((Assign) startWith, element);
		else if (startWith instanceof Reply)
			foundActivity = findByReply((Reply) startWith, element);
		else if (startWith instanceof Receive)
			foundActivity = findByReceive((Receive) startWith, element);
		else if (startWith instanceof Wait)
			foundActivity = findByWait((Wait) startWith, element);
		else if (startWith instanceof Throw)
			foundActivity = findByThrow((Throw) startWith, element);
		else if (startWith instanceof Exit)
			foundActivity = findByExit((Exit) startWith, element);
		else if (startWith instanceof Flow)
			foundActivity = findByFlow((Flow) startWith, element);
		else if (startWith instanceof If)
			foundActivity = findByIf((If) startWith, element);
		else if (startWith instanceof While)
			foundActivity = findByWhile((While) startWith, element);
		else if (startWith instanceof Sequence)
			foundActivity = findBySequence((Sequence) startWith, element);
		else if (startWith instanceof Pick)
			foundActivity = findByPick((Pick) startWith, element);
		else if (startWith instanceof Scope)
			foundActivity = findByScope((Scope) startWith, element);
		else if (startWith instanceof Compensate)
			foundActivity = findByCompensate((Compensate) startWith, element);
		else if (startWith instanceof CompensateScope)
			foundActivity = findByCompensateScope((CompensateScope) startWith, element);
		else if (startWith instanceof Rethrow)
			foundActivity = findByRethrow((Rethrow) startWith, element);
		else if (startWith instanceof OpaqueActivity)
			foundActivity = findByOpaqueActivity((OpaqueActivity) startWith, element);
		else if (startWith instanceof ForEach)
			foundActivity = findByForEach((ForEach) startWith, element);
		else if (startWith instanceof RepeatUntil)
			foundActivity = findByRepeatUntil((RepeatUntil) startWith, element);
		else if (startWith instanceof Validate)
			foundActivity = findByValidate((Validate) startWith, element);
		else if (startWith instanceof ExtensionActivity)
			foundActivity = findByExtensionActivity((ExtensionActivity) startWith, element);

		return foundActivity;

	}

	private Activity findByEmpty(Empty start, Element element) {
		return (start.getElement().equals(element)) ? start : null;

	}

	private Activity findByInvoke(Invoke start, Element element) {
		return (start.getElement().equals(element)) ? start : null;
	}

	private Activity findByAssign(Assign start, Element element) {
		return (start.getElement().equals(element)) ? start : null;

	}

	private Activity findByReply(Reply start, Element element) {
		return (start.getElement().equals(element)) ? start : null;

	}

	private Activity findByReceive(Receive start, Element element) {
		return (start.getElement().equals(element)) ? start : null;

	}

	private Activity findByWait(Wait start, Element element) {
		return (start.getElement().equals(element)) ? start : null;

	}

	private Activity findByThrow(Throw start, Element element) {
		return (start.getElement().equals(element)) ? start : null;

	}

	private Activity findByExit(Exit start, Element element) {
		return (start.getElement().equals(element)) ? start : null;

	}

	private Activity findByFlow(Flow start, Element element) {
		if (start.getElement().equals(element))
			return start;

		List<?> listOfActivities = start.getActivities();

		Activity res = null;

		for (Object next : listOfActivities) {
			res = findByActivity((Activity) next, element);
			if (res != null)
				return res;
		}

		return null;

	}

	private Activity findByIf(If startAct, Element element) {
		if (startAct.getElement().equals(element))
			return startAct;

		Activity res = null;
		// if
		if (startAct.getActivity() != null) {
			res = findByActivity(startAct.getActivity(), element);
			if (res != null)
				return res;
		}
		// elseIf
		List<?> elseIfs = startAct.getElseIf();
		if (!elseIfs.isEmpty()) {
			for (Object next : elseIfs) {
				ElseIf elseIf = (ElseIf) next;
				res = findByElseIf(elseIf, element);
				if (res != null)
					return res;
			}
		}
		// else
		Else elseBranch = startAct.getElse();
		if (elseBranch != null) {
			res = findByElse(elseBranch, element);
			if (res != null)
				return res;
		}
		return null;
	}

	private Activity findByElseIf(ElseIf startAct, Element element) {

		Activity res = null;
		if (startAct.getActivity() != null) {
			res = findByActivity(startAct.getActivity(), element);
		}

		return res;
	}

	private Activity findByElse(Else startAct, Element element) {
		Activity res = null;
		if (startAct.getActivity() != null) {
			res = findByActivity(startAct.getActivity(), element);
		}
		return res;
	}

	private Activity findByWhile(While startAct, Element element) {
		if (startAct.getElement().equals(element))
			return startAct;

		Activity res = null;
		if (startAct.getActivity() != null) {
			res = findByActivity(startAct.getActivity(), element);
		}
		return res;

	}

	private Activity findBySequence(Sequence start, Element element) {
		if (start.getElement().equals(element))
			return start;

		Activity res = null;
		List<?> listOfActivities = start.getActivities();
		for (Object next : listOfActivities) {
			res = findByActivity((Activity) next, element);
			if (res != null)
				return res;
		}
		return null;

	}

	private Activity findByPick(Pick start, Element element) {
		if (start.getElement().equals(element))
			return start;
		Activity res = null;
		List<?> listOfOnMessage = start.getMessages();
		for (Object next : listOfOnMessage) {
			res = findByOnMessage((OnMessage) next, element);
			if (res != null)
				return res;
		}

		List<?> listOfOnAlarm = start.getAlarm();
		for (Object next : listOfOnAlarm) {
			res = findByOnAlarm((OnAlarm) next, element);
			if (res != null)
				return res;
		}
		return null;

	}

	private Activity findByOnMessage(OnMessage start, Element element) {
		Activity res = findByActivity(start.getActivity(), element);

		return res;

	}

	private Activity findByOnAlarm(OnAlarm start, Element element) {
		Activity res = findByActivity(start.getActivity(), element);

		return res;
	}

	private Activity findByScope(Scope start, Element element) {
		if (start.getElement().equals(element))
			return start;

		Activity res = findByActivity(start.getActivity(), element);

		return res;

	}

	private Activity findByCompensate(Compensate start, Element element) {

		return (start.getElement().equals(element)) ? start : null;

	}

	private Activity findByCompensateScope(CompensateScope start, Element element) {

		return (start.getElement().equals(element)) ? start : null;

	}

	private Activity findByRethrow(Rethrow start, Element element) {
		return (start.getElement().equals(element)) ? start : null;

	}

	private Activity findByOpaqueActivity(OpaqueActivity start, Element element) {
		return (start.getElement().equals(element)) ? start : null;
	}

	private Activity findByForEach(ForEach start, Element element) {
		if (start.getElement().equals(element))
			return start;
		Activity res = findByActivity(start.getActivity(), element);

		return res;

	}

	private Activity findByRepeatUntil(RepeatUntil start, Element element) {
		if (start.getElement().equals(element))
			return start;

		Activity res = findByActivity(start.getActivity(), element);

		return res;

	}

	private Activity findByValidate(Validate start, Element element) {
		return (start.getElement().equals(element)) ? start : null;

	}

	private Activity findByExtensionActivity(ExtensionActivity start, Element element) {
		return (start.getElement().equals(element)) ? start : null;

	}

	/**
	 * Find parent that contains the child activity with the given name.
	 * 
	 * <p>
	 * The idea is that we firstly find the activity with the given child name,
	 * starting with the given activity(startWith), the child-parent pair will
	 * be stored in {@link #child2ParentMap} along with the search path. At the
	 * end we get the parent by using the child name as key against the
	 * act2ParentMap, if parent is found, the (child_name:parent) pair should
	 * have been stored before.
	 * 
	 * @param startWith
	 *            Start Activity
	 * @param childName
	 *            The child name
	 * @return The parent BPELExtensibleElement or <tt>null</null>
	 * 
	 * @see {@link #findByActivity(Activity, Element)},
	 *      {@link #findByActivity(Activity, String)}
	 */
	private BPELExtensibleElement findParentByActivity(Activity startWith, String childName) {

		if (startWith == null)
			return null;

		BPELExtensibleElement parent = null;

		if ((startWith instanceof Empty) || (startWith instanceof Invoke) || (startWith instanceof Assign)
				|| (startWith instanceof Reply) || (startWith instanceof Receive) || (startWith instanceof Wait)
				|| (startWith instanceof Throw) || (startWith instanceof Exit) || (startWith instanceof OpaqueActivity)
				|| (startWith instanceof Rethrow) || (startWith instanceof Validate)
				|| (startWith instanceof ExtensionActivity) || (startWith instanceof Compensate)
				|| (startWith instanceof CompensateScope)) {
			// these activities contain no further sub-activity, the searching
			// stop here. if the child name matches, the child to parent pair
			// should have been stored in the child2ParentMap.
			if (startWith.getName().equals(childName)) {
				parent = child2ParentMap.get(childName);
			}
		} else if (startWith instanceof Flow) {
			parent = findParentByFlow((Flow) startWith, childName);
		} else if (startWith instanceof If) {
			parent = findParentByIf((If) startWith, childName);
		} else if (startWith instanceof While) {
			parent = findParentByWhile((While) startWith, childName);
		} else if (startWith instanceof Sequence) {
			parent = findParentBySequence((Sequence) startWith, childName);
		} else if (startWith instanceof Pick) {
			parent = findParentByPick((Pick) startWith, childName);
		} else if (startWith instanceof Scope) {
			parent = findParentByScope((Scope) startWith, childName);
		} else if (startWith instanceof ForEach) {
			parent = findParentByForEach((ForEach) startWith, childName);
		} else if (startWith instanceof RepeatUntil) {
			parent = findParentByRepeatUntil((RepeatUntil) startWith, childName);
		}

		return parent;
	}

	private BPELExtensibleElement findParentByRepeatUntil(RepeatUntil startWith, String childName) {

		BPELExtensibleElement parent = null;

		if (startWith.getName().equals(childName)) {
			// if the startWith is the CHILD ACTIVITY, then the child-parent
			// pair for it should have been inserted in the map.
			parent = child2ParentMap.get(childName);
			return parent;
		}

		// else, store the child-parent pair, then keep finding recursively
		// along down
		Activity subAct = startWith.getActivity();
		if ((subAct != null) && (child2ParentMap.get(subAct.getName()) == null)) {
			child2ParentMap.put(subAct.getName(), startWith);
		}
		parent = findParentByActivity(subAct, childName);
		return parent;
	}

	private BPELExtensibleElement findParentByForEach(ForEach startWith, String childName) {

		BPELExtensibleElement parent = null;

		if (startWith.getName().equals(childName)) {
			// if the startWith is the CHILD ACTIVITY, then the child-parent
			// pair for it should have been inserted in the map.
			parent = child2ParentMap.get(childName);
			return parent;
		}

		// else, store the child-parent pair, then keep finding recursively
		// along down
		Activity subAct = startWith.getActivity();
		if ((subAct != null) && (child2ParentMap.get(subAct.getName()) == null)) {
			child2ParentMap.put(subAct.getName(), startWith);
		}
		parent = findParentByActivity(subAct, childName);
		return parent;
	}

	private BPELExtensibleElement findParentByScope(Scope startWith, String childName) {

		BPELExtensibleElement parent = null;

		if (startWith.getName().equals(childName)) {
			// if the startWith is the CHILD ACTIVITY, then the child-parent
			// pair for it should have been inserted in the map.
			parent = child2ParentMap.get(childName);
			return parent;
		}

		// else, store the child-parent pair, then keep finding recursively
		// along down
		Activity subAct = startWith.getActivity();
		if ((subAct != null) && (child2ParentMap.get(subAct.getName()) == null)) {
			child2ParentMap.put(subAct.getName(), startWith);
		}
		parent = findParentByActivity(subAct, childName);
		return parent;
	}

	private BPELExtensibleElement findParentByPick(Pick startWith, String childName) {

		BPELExtensibleElement parent = null;

		if (startWith.getName().equals(childName)) {
			// if the startWith is the CHILD ACTIVITY, then the child-parent
			// pair for it should have been inserted in the map.
			parent = child2ParentMap.get(childName);
			return parent;
		}

		// else, store the child-parent pair, then keep finding recursively
		// along down
		List<OnMessage> listOfOnMessage = startWith.getMessages();
		for (OnMessage next : listOfOnMessage) {
			Activity onMsgContainedAct = next.getActivity();
			if (child2ParentMap.get(onMsgContainedAct.getName()) == null) {
				child2ParentMap.put(onMsgContainedAct.getName(), startWith);
			}
			parent = findParentByActivity(onMsgContainedAct, childName);
			if (parent != null)
				return parent;
		}

		List<OnAlarm> listOfOnAlarm = startWith.getAlarm();
		for (OnAlarm next : listOfOnAlarm) {
			Activity onAlarmContainedAct = next.getActivity();
			if (child2ParentMap.get(onAlarmContainedAct.getName()) == null) {
				child2ParentMap.put(onAlarmContainedAct.getName(), startWith);
			}
			parent = findParentByActivity(onAlarmContainedAct, childName);
			if (parent != null)
				return parent;
		}
		return parent;
	}

	private BPELExtensibleElement findParentBySequence(Sequence startWith, String childName) {

		BPELExtensibleElement parent = null;

		if (startWith.getName().equals(childName)) {
			// if the startWith is the CHILD ACTIVITY, then the child-parent
			// pair for it should have been inserted in the map.
			parent = child2ParentMap.get(childName);
			return parent;
		}

		// else, recursively iterate down
		List<?> listOfActivities = startWith.getActivities();
		for (Object next : listOfActivities) {
			Activity subAct = (Activity) next;
			if (child2ParentMap.get(subAct.getName()) == null) {
				child2ParentMap.put(subAct.getName(), startWith);
			}
			parent = findParentByActivity(subAct, childName);
			if (parent != null)
				return parent;
		}
		return null;
	}

	private BPELExtensibleElement findParentByWhile(While startWith, String childName) {
		BPELExtensibleElement parent = null;

		if (startWith.getName().equals(childName)) {
			// if the startWith is the CHILD ACTIVITY, then the child-parent
			// pair for it should have been inserted in the map.
			parent = child2ParentMap.get(childName);
			return parent;
		}

		Activity subAct = startWith.getActivity();
		if ((subAct != null) && (child2ParentMap.get(subAct.getName()) == null)) {
			child2ParentMap.put(subAct.getName(), startWith);
		}
		parent = findParentByActivity(subAct, childName);
		return parent;
	}

	/**
	 * Find parent of the given child name
	 * 
	 * @param startWith
	 *            The "IF" activity
	 * @param childName
	 *            The child activity name
	 * @return If the child is in any branch of the "If", then the parent is the
	 *         "If".
	 */
	private BPELExtensibleElement findParentByIf(If startWith, String childName) {

		BPELExtensibleElement parent = null;

		if (startWith.getName().equals(childName)) {
			// if the startWith is the CHILD ACTIVITY, then the child-parent
			// pair for it should have been inserted in the map.
			parent = child2ParentMap.get(childName);
			return parent;
		}

		// recursively iterate down all the branches

		// if-branch
		Activity subActInIfBranch = startWith.getActivity();
		if ((subActInIfBranch != null) && (child2ParentMap.get(subActInIfBranch.getName()) == null)) {
			child2ParentMap.put(subActInIfBranch.getName(), startWith);
		}
		parent = findByActivity(subActInIfBranch, childName);
		if (parent != null)
			return parent;

		// elseIf-branch
		List<ElseIf> elseIfs = startWith.getElseIf();

		for (ElseIf elseIf : elseIfs) {
			Activity subActInElseIfBranch = elseIf.getActivity();
			if ((subActInElseIfBranch != null) && (child2ParentMap.get(subActInElseIfBranch.getName()) == null)) {
				child2ParentMap.put(subActInElseIfBranch.getName(), startWith);
			}
			parent = findParentByActivity(subActInElseIfBranch, childName);
			if (parent != null)
				return parent;
		}

		// else-branch
		Else elseBranch = startWith.getElse();
		if (elseBranch != null) {
			Activity subActInElseBranch = elseBranch.getActivity();
			if ((subActInElseBranch != null) && (child2ParentMap.get(subActInElseBranch.getName()) == null)) {
				child2ParentMap.put(subActInElseBranch.getName(), startWith);
			}
			parent = findParentByActivity(subActInElseBranch, childName);
			if (parent != null)
				return parent;
		}
		return parent;
	}

	private BPELExtensibleElement findParentByFlow(Flow startWith, String childName) {

		BPELExtensibleElement parent = null;

		if (startWith.getName().equals(childName)) {
			// if the startWith is the CHILD ACTIVITY, then the child-parent
			// pair for it should have been inserted in the map.
			parent = child2ParentMap.get(childName);
			return parent;
		}

		List<Activity> listOfActivities = startWith.getActivities();

		for (Activity next : listOfActivities) {

			if (child2ParentMap.get(next.getName()) == null) {
				child2ParentMap.put(next.getName(), startWith);
			}
			parent = findParentByActivity((Activity) next, childName);
			if (parent != null)
				return parent;
		}

		return parent;
	}
}
