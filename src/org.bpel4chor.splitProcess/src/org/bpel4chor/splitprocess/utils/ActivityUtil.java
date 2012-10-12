package org.bpel4chor.splitprocess.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Assign;
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
import org.eclipse.bpel.model.Receive;
import org.eclipse.bpel.model.RepeatUntil;
import org.eclipse.bpel.model.Reply;
import org.eclipse.bpel.model.Rethrow;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Sequence;
import org.eclipse.bpel.model.Throw;
import org.eclipse.bpel.model.Wait;
import org.eclipse.bpel.model.While;
import org.eclipse.emf.ecore.EObject;

/**
 * Utilities around activity, e.g. to test whether activity is basic or simple,
 * to get children or to get descendant basic children of an activity.
 * 
 * <p>
 * <b>changeLog date user remark</b> <br>
 * 
 * @001 2012-01-24 DC initial version
 * 
 * @since Jan 24, 2012
 * @author Daojun Cui
 */
public class ActivityUtil {

	/**
	 * Test if the activity is basic activity
	 * 
	 * <P>
	 * MINIMAL BASIC ACTIVITY SET: Invoke, Receive, Reply, Assign
	 * <p>
	 * THE FULL BASIC ACTIVITY SET: Empty, Invoke, Assign, Reply, Receive, Wait,
	 * Throw, Exit, OpaqueActivity, Rethrow, ExtensionActivity.
	 * <p>
	 * The basic activity is defined in
	 * 
	 * <pre>
	 * <a href="http://docs.oasis-open.org/wsbpel/2.0/wsbpel-v2.0.html">WS-BPEL 2.0 Specification</a>
	 * </pre>
	 * 
	 * <tt>Chapter 10</tt>.
	 * 
	 * @param act
	 *            The activity
	 * @return <tt>true</tt> if the activity is <tt>not-null</tt> and
	 *         <tt>basic</tt>, otherwise <tt>false</tt>
	 */
	public static boolean isBasicActivity(Activity act) {

		if (act == null)
			throw new NullPointerException("argument is null");

		// full basic activity set
		if ((act instanceof Empty) || (act instanceof Invoke) || (act instanceof Assign) || (act instanceof Reply)
				|| (act instanceof Receive) || (act instanceof Wait) || (act instanceof Throw) || (act instanceof Exit)
				|| (act instanceof OpaqueActivity) || (act instanceof Rethrow) || (act instanceof ExtensionActivity)) {
			return true;
		}

		return false;
	}
	
	/**
	 * Test if the two activities are equals
	 * @param act1
	 * @param act2
	 * @return
	 */
	public static boolean isEqual(Activity act1, Activity act2) {
		if (act1 == null && act2 == null) {
			return true;
			
		} else if (act1 != null && act2 != null) {
			if(act1.equals(act2))
				return true;
			
			return (act1.getName().equals(act2.getName()));
			
		} else {
			return false;
		}
	}
	
	/**
	 * Test whether activity set 1 and activity set 2 are the same
	 * 
	 * @param actSet1
	 * @param actSet2
	 * @return true if they are same, otherwise false.
	 */
	public static boolean isEqual(Set<Activity> actSet1, Set<Activity> actSet2) {
		if (actSet1 == null && actSet2 == null) {
			return true;
		}
		if (actSet1 != null && actSet2 != null) {
			for (Activity act1 : actSet1) {
				if (!actSet2.contains(act1))
					return false;
			}
			for (Activity act2 : actSet2) {
				if (!actSet1.contains(act2))
					return false;
			}
			return true;
		}
		// either of the set is null, and the other not null.
		return false;
	}

	/**
	 * Test if the activity is simple activity
	 * 
	 * <P>
	 * MINIMAL SIMPLE ACTIVITY SET: Invoke, Receive, Reply, Assign, <b>Flow</b>
	 * 
	 * <p>
	 * FULL SIMPLE ACTIVITY SET: Empty, Invoke, Assign, Reply, Receive, Wait,
	 * Throw, Exit, OpaqueActivity, Rethrow, Validate, ExtensionActivity,
	 * Compensate, CompensateScope, <b>Flow</b>
	 * 
	 * @param act
	 *            The activity
	 * @return <tt>true</tt> if the activity is <tt>not-null</tt> and
	 *         <tt>simple</tt>, otherwise <tt>false</tt>
	 */
	public static boolean isSimpleActivity(Activity act) {

		if (act == null)
			throw new NullPointerException("Argument is null");

		if (isBasicActivity(act) || act instanceof Flow) {
			return true;
		}
		return false;
	}

	/**
	 * Test whether the activity is structured activity
	 * 
	 * @param act
	 *            The activity
	 * @return <tt>true</tt> if the activity is one of the Flow, If, While,
	 *         Sequence, Pick, Scope, ForEach, and RepeatUntil, otherwise
	 *         <tt>false</tt>.
	 */
	public static boolean isStructuredActivity(Activity act) {
		if (act == null)
			throw new NullPointerException();

		if ((act instanceof Flow) || (act instanceof If) || (act instanceof While) || (act instanceof Sequence)
				|| (act instanceof Pick) || (act instanceof Scope) || (act instanceof ForEach)
				|| (act instanceof RepeatUntil)) {
			return true;
		}
		return false;
	}

	/**
	 * Test whether the eObject is structured activity
	 * 
	 * @param act
	 *            The eobject
	 * @return true if it is a structured activity, otherwise false.
	 */
	public static boolean isStructuredActivity(EObject act) {
		if (act instanceof Activity)
			return isStructuredActivity((Activity) act);
		else
			return false;
	}

	/**
	 * Get all descendant basic activity children of the given activity
	 * 
	 * <p>
	 * Basic Activities: Empty, Invoke, Assign, Reply, Receive, Wait, Throw,
	 * Exit, OpaqueActivity, Rethrow, Validate, ExtensionActivity, Compensate,
	 * CompensateScope
	 * 
	 * @param act
	 *            The activity
	 * @return The recursively descendant basic children of the activity, if the
	 *         activity is basic one or is <tt>null</tt>, return an empty array
	 *         list.
	 */
	public static List<Activity> getAllDescBasicChildren(Activity act) {

		if (act == null)
			return new ArrayList<Activity>();

		List<Activity> children = new ArrayList<Activity>();

		if (isBasicActivity(act)) {
			// basic activities don't contains basic children.
			return new ArrayList<Activity>();

		} else if (act instanceof Flow) {
			List<Activity> descendantsInFlow = getDescBasicChildren((Flow) act);
			children.addAll(descendantsInFlow);

		} else if (act instanceof If) {
			List<Activity> descendantsInIf = getDescBasicChildren((If) act);
			children.addAll(descendantsInIf);

		} else if (act instanceof While) {
			List<Activity> descendantsInWhile = getDescBasicChildren((While) act);
			children.addAll(descendantsInWhile);

		} else if (act instanceof Sequence) {
			List<Activity> descendantsInSequence = getDescBasicChildren((Sequence) act);
			children.addAll(descendantsInSequence);

		} else if (act instanceof Pick) {
			List<Activity> descendantsInPick = getDescBasicChildren((Pick) act);
			children.addAll(descendantsInPick);

		} else if (act instanceof Scope) {
			List<Activity> descendantsInScope = getDescBasicChildren((Scope) act);
			children.addAll(descendantsInScope);

		} else if (act instanceof ForEach) {
			List<Activity> descendantsInForEach = getDescBasicChildren((ForEach) act);
			children.addAll(descendantsInForEach);

		} else if (act instanceof RepeatUntil) {
			List<Activity> descendantsInRepeatUntil = getDescBasicChildren((RepeatUntil) act);
			children.addAll(descendantsInRepeatUntil);
		}

		return children;
	}

	/**
	 * Get the basic activities in the flow
	 * 
	 * @param flow
	 *            The flow activities
	 * @return The children activities
	 */
	public static List<Activity> getDescBasicChildren(Flow flow) {
		List<Activity> children = new ArrayList<Activity>();

		if (flow == null)
			return children;

		List<Activity> flowActList = flow.getActivities();
		for (Activity flowAct : flowActList) {
			if (isBasicActivity(flowAct)) {
				children.add(flowAct);
			} else {
				List<Activity> descedants = getAllDescBasicChildren(flowAct);
				if (!descedants.isEmpty())
					children.addAll(descedants);
			}
		}

		return children;
	}

	/**
	 * Get the basic activities in the if-else_if-else branches.
	 * 
	 * @param ifAct
	 *            The If activity
	 * @return The basic children activities
	 */
	public static List<Activity> getDescBasicChildren(If ifAct) {
		List<Activity> children = new ArrayList<Activity>();

		if (ifAct == null)
			return children;

		// if-branch, if the child is basic collect it, or collect the
		// descendant children recursively
		Activity actInIfBranch = ifAct.getActivity();
		if (isBasicActivity(actInIfBranch)) {
			children.add(actInIfBranch);
		} else {
			List<Activity> descActsIfBranch = getAllDescBasicChildren(actInIfBranch);
			if (!descActsIfBranch.isEmpty())
				children.addAll(descActsIfBranch);
		}

		// else-if-branch, if the child is basic collect it, or collect the
		// descendant children recursively
		List<ElseIf> elseIfs = ifAct.getElseIf();

		for (ElseIf elseIf : elseIfs) {
			Activity subActInElseIfBranch = elseIf.getActivity();
			if (isBasicActivity(subActInElseIfBranch)) {
				children.add(subActInElseIfBranch);
			} else {
				List<Activity> descActsIfElseBranch = getAllDescBasicChildren(subActInElseIfBranch);
				if (!descActsIfElseBranch.isEmpty())
					children.addAll(descActsIfElseBranch);
			}
		}

		// else-branch, if the child is basic collect it, or collect the
		// descendant children recursively
		Else elseBranch = ifAct.getElse();
		if (elseBranch != null) {
			Activity actInElseBranch = elseBranch.getActivity();
			if (isBasicActivity(actInElseBranch)) {
				children.add(actInElseBranch);
			} else {
				List<Activity> descActsElseBranch = getAllDescBasicChildren(actInElseBranch);
				if (!descActsElseBranch.isEmpty())
					children.addAll(descActsElseBranch);
			}
		}

		return children;
	}

	/**
	 * Get the descendant basic activities in the while activity
	 * 
	 * @param whileAct
	 *            The while activity
	 * @return The basic children activity
	 */
	public static List<Activity> getDescBasicChildren(While whileAct) {
		List<Activity> children = new ArrayList<Activity>();

		if (whileAct == null)
			return children;

		Activity childAct = whileAct.getActivity();
		if (isBasicActivity(childAct)) {
			children.add(childAct);
		} else {
			List<Activity> descActsWhile = getAllDescBasicChildren(childAct);
			if (!descActsWhile.isEmpty())
				children.addAll(descActsWhile);
		}
		return children;
	}

	/**
	 * Get the descendant basic activities in the sequence activity
	 * 
	 * @param sequence
	 *            The sequence activity
	 * @return The basic children activity
	 */
	public static List<Activity> getDescBasicChildren(Sequence sequence) {
		List<Activity> children = new ArrayList<Activity>();

		if (sequence == null)
			return children;

		List<Activity> actsInSequence = sequence.getActivities();
		for (Activity act : actsInSequence) {
			if (isBasicActivity(act)) {
				children.add(act);
			} else {
				List<Activity> descActsSequence = getAllDescBasicChildren(act);
				if (!descActsSequence.isEmpty())
					children.addAll(descActsSequence);
			}
		}

		return children;
	}

	/**
	 * Get the descendant basic activities in the sequence activity
	 * 
	 * @param pick
	 *            The Pick Activity
	 * @return The descendant basic children activity
	 */
	public static List<Activity> getDescBasicChildren(Pick pick) {
		List<Activity> children = new ArrayList<Activity>();

		if (pick == null)
			return children;

		List<OnMessage> listOnMessage = pick.getMessages();
		for (OnMessage onMsg : listOnMessage) {
			Activity actInOnMsg = onMsg.getActivity();
			if (isBasicActivity(actInOnMsg)) {
				children.add(actInOnMsg);
			} else {
				List<Activity> descActsOnMsg = getAllDescBasicChildren(actInOnMsg);
				if (!descActsOnMsg.isEmpty())
					children.addAll(descActsOnMsg);
			}
		}

		List<OnAlarm> listOnAlarm = pick.getAlarm();
		for (OnAlarm onAlarm : listOnAlarm) {
			Activity actInOnAlarm = onAlarm.getActivity();
			if (isBasicActivity(actInOnAlarm)) {
				children.add(actInOnAlarm);
			} else {
				List<Activity> descActsOnAlarm = getAllDescBasicChildren(actInOnAlarm);
				if (!descActsOnAlarm.isEmpty())
					children.addAll(descActsOnAlarm);
			}
		}

		return children;
	}

	/**
	 * Get the descendant basic activities in the scope activity
	 * 
	 * @param scope
	 *            The scope activity
	 * @return The descendant basic children activities
	 */
	public static List<Activity> getDescBasicChildren(Scope scope) {
		List<Activity> children = new ArrayList<Activity>();

		if (scope == null)
			return children;

		Activity actInScope = scope.getActivity();

		if (isBasicActivity(actInScope)) {
			children.add(actInScope);
		} else {
			List<Activity> descActsScope = getAllDescBasicChildren(actInScope);
			if (!descActsScope.isEmpty())
				children.addAll(descActsScope);
		}

		return children;
	}

	/**
	 * Get the descendant basic activities in the forEach activity
	 * 
	 * @param forEach
	 *            The ForEach activity
	 * @return The descendant basic children activities
	 */
	public static List<Activity> getDescBasicChildren(ForEach forEach) {
		List<Activity> children = new ArrayList<Activity>();

		if (forEach == null)
			return children;

		Activity actInForEach = forEach.getActivity();

		if (isBasicActivity(actInForEach)) {
			children.add(actInForEach);
		} else {
			List<Activity> descActsForEach = getAllDescBasicChildren(actInForEach);
			if (!descActsForEach.isEmpty())
				children.addAll(descActsForEach);
		}

		return children;
	}

	/**
	 * Get the descendant basic activities in the RepeatUntil activity
	 * 
	 * @param repeatUntil
	 *            The RepeatUntil activity
	 * @return The descendant basic children activities
	 */
	public static List<Activity> getDescBasicChildren(RepeatUntil repeatUntil) {
		List<Activity> children = new ArrayList<Activity>();

		if (repeatUntil == null)
			return children;

		Activity actInRepeatUntil = repeatUntil.getActivity();

		if (isBasicActivity(actInRepeatUntil)) {
			children.add(actInRepeatUntil);
		} else {
			List<Activity> descActsRepeatUntil = getAllDescBasicChildren(actInRepeatUntil);
			if (!descActsRepeatUntil.isEmpty())
				children.addAll(descActsRepeatUntil);
		}

		return children;
	}

	/**
	 * Get the direct children of the given activity
	 * 
	 * @param act
	 *            The activity
	 * @return The direct children of the given activity
	 */
	public static List<Activity> getDirectChildren(Activity act) {
		List<Activity> children = new ArrayList<Activity>();

		if (act == null)
			return children;

		if (isBasicActivity(act)) {
			// basic activity
			return children;

		} else if (act instanceof Flow) {
			List<Activity> descendantsInFlow = ((Flow) act).getActivities();
			children.addAll(descendantsInFlow);

		} else if (act instanceof If) {
			// TODO HOW SHOULD THE DIRECT CHILDREN BE DEFINED? the children in
			// all branches or children belong to each branch itself.
			List<Activity> descendantsInIf = getDirectChildren((If) act);
			children.addAll(descendantsInIf);

		} else if (act instanceof While) {
			Activity descendantInWhile = ((While) act).getActivity();
			children.add(descendantInWhile);

		} else if (act instanceof Sequence) {
			List<Activity> descendantsInSequence = ((Sequence) act).getActivities();
			children.addAll(descendantsInSequence);

		} else if (act instanceof Pick) {
			// TODO, HOW TO DEFINE CHILDREN OF PICK, SHOULD ALSO THE ONMESSAGE
			// AND ONALARM BE ACCOUNTED?
			List<Activity> descendantsInPick = getDirectChildren((Pick) act);
			children.addAll(descendantsInPick);

		} else if (act instanceof Scope) {
			Activity descendantInScope = ((Scope) act).getActivity();
			children.add(descendantInScope);

		} else if (act instanceof ForEach) {
			Activity descendantInForEach = ((ForEach) act).getActivity();
			children.add(descendantInForEach);

		} else if (act instanceof RepeatUntil) {
			Activity descendantInRepeatUntil = ((RepeatUntil) act).getActivity();
			children.add(descendantInRepeatUntil);
		}

		return children;
	}

	public static List<Activity> getDirectChildren(If ifAct) {
		List<Activity> children = new ArrayList<Activity>();
		// TODO
		return children;
	}

	public static List<Activity> getDirectChildren(Pick pick) {
		List<Activity> children = new ArrayList<Activity>();
		// TODO
		return children;
	}

}
