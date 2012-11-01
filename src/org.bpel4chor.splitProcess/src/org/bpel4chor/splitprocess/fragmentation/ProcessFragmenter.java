package org.bpel4chor.splitprocess.fragmentation;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.bpel4chor.model.grounding.impl.Grounding;
import org.bpel4chor.model.topology.impl.ParticipantType;
import org.bpel4chor.model.topology.impl.Topology;
import org.bpel4chor.splitprocess.RuntimeData;
import org.bpel4chor.splitprocess.exceptions.ActivityNotFoundException;
import org.bpel4chor.splitprocess.exceptions.PartitionSpecificationException;
import org.bpel4chor.splitprocess.partition.model.Participant;
import org.bpel4chor.splitprocess.partition.model.PartitionSpecification;
import org.bpel4chor.splitprocess.utils.ActivityFinder;
import org.bpel4chor.splitprocess.utils.ActivityUtil;
import org.bpel4chor.splitprocess.utils.SplitProcessConstants;
import org.bpel4chor.splitprocess.utils.VariableResolver;
import org.bpel4chor.splitprocess.utils.VariableUtil;
import org.bpel4chor.utils.BPEL4ChorConstants;
import org.bpel4chor.utils.BPEL4ChorFactory;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.BPELExtensibleElement;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.CorrelationSet;
import org.eclipse.bpel.model.CorrelationSets;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.ForEach;
import org.eclipse.bpel.model.If;
import org.eclipse.bpel.model.PartnerActivity;
import org.eclipse.bpel.model.PartnerLink;
import org.eclipse.bpel.model.PartnerLinks;
import org.eclipse.bpel.model.Pick;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Receive;
import org.eclipse.bpel.model.RepeatUntil;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Sequence;
import org.eclipse.bpel.model.Variable;
import org.eclipse.bpel.model.Variables;
import org.eclipse.bpel.model.While;
import org.eclipse.bpel.model.messageproperties.MessagepropertiesFactory;
import org.eclipse.bpel.model.messageproperties.Property;
import org.eclipse.bpel.model.messageproperties.PropertyAlias;
import org.eclipse.bpel.model.partnerlinktype.Role;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.wst.wsdl.Definition;
import org.eclipse.wst.wsdl.Message;
import org.eclipse.wst.wsdl.Operation;
import org.eclipse.wst.wsdl.PortType;
import org.eclipse.xsd.XSDFactory;
import org.eclipse.xsd.XSDTypeDefinition;
import org.eclipse.xsd.util.XSDConstants;

import de.uni_stuttgart.iaas.bpel.model.utilities.FragmentDuplicator;
import de.uni_stuttgart.iaas.bpel.model.utilities.MyBPELUtils;
import de.uni_stuttgart.iaas.bpel.model.utilities.MyWSDLUtil;

/**
 * ProcessFragmenter fragments the main process into smaller fragment processes
 * based on the given partition specification.
 * 
 * @since Dec 18, 2011
 * @author Daojun Cui
 */
public class ProcessFragmenter {

	/** The non split BPEL process */
	protected Process nonSplitProcess = null;

	/** The WSDL definition of non split BPEL process */
	protected Definition nonSplitProcessDefn = null;

	/** The Partition Specification */
	protected PartitionSpecification partitionSpec = null;

	/** The Participant to Fragment Process map */
	protected Map<String, Process> participant2FragProc = null;

	/** The Participant to WSDL Definitions map */
	protected Map<String, Definition> participant2WSDL = null;

	/** The BPEL4Chor Topology */
	protected Topology topology = null;

	/** The BPEL4Chor Grounding */
	protected Grounding grounding = null;

	protected Logger logger = Logger.getLogger(ProcessFragmenter.class);

	/**
	 * Constructor
	 * 
	 * @param process
	 *            The non-split process
	 * @param partitionSpec
	 *            The partition specification
	 * @throws PartitionSpecificationException
	 * @throws NullPointerException
	 *             if argument is null
	 */
	public ProcessFragmenter(RuntimeData data) throws PartitionSpecificationException {
		if (data == null)
			throw new NullPointerException("data==null:" + (data == null));

		this.nonSplitProcess = data.getNonSplitProcess();
		this.nonSplitProcessDefn = data.getNonSplitProcessDfn();
		this.partitionSpec = data.getPartitionSpec();
		this.participant2FragProc = data.getParticipant2FragProcMap();
		this.participant2WSDL = data.getParticipant2WSDLMap();
		this.topology = data.getTopology();
		this.grounding = data.getGrounding();

	}

	/**
	 * Fragment the main process
	 * <p>
	 * It creates a fragment process for each participant, a WSDL definition for
	 * each fragment process.
	 * <p>
	 * As results, the necessary variables, correlationSets, PartnerLinks,
	 * activities, and links will be added into the fragment processes.
	 * <p>
	 * Due to the Rubble Band Effect. If one 'Invoke' is contained in a 'Flow',
	 * then the 'Flow' must also be copied and be added into the fragment
	 * process that the 'Invoke' resides.
	 * <p>
	 * For the control link between activities, we only copied the link name and
	 * set it into a new link, then separately added it into the source and
	 * target of the activities. It means that after the process fragmenting,
	 * the original link will have two copies, both have the exact name. One
	 * copy is in an activity's source, the other one is in another activity's
	 * target. Both activities are not necessarily in the same fragment process.
	 */
	public void fragmentizeProcess() {

		for (Participant participant : partitionSpec.getParticipants()) {

			String fragProcName = participant.getName();

			Definition fragmentWsdlDeinition = FragmentFactory.createWSDLDefinition(
					nonSplitProcess, nonSplitProcessDefn, fragProcName);
			participant2WSDL.put(fragProcName, fragmentWsdlDeinition);

			Process fragmentProcess = createFragmentProcess(participant, nonSplitProcess);
			participant2FragProc.put(fragProcName, fragmentProcess);

		}

	}

	/**
	 * Create fragment process for the given participant
	 * 
	 * <p>
	 * The following will be done:
	 * <ol>
	 * <li>initialize a new process for participant
	 * <li>add correlation set to the new process
	 * <li>add variable used by the fragment
	 * <li>add partnerLinks used by the fragment
	 * <li>add activities specified by participant
	 * <li>create bpel4chor artifacts
	 * </ol>
	 * 
	 * @param participant
	 *            The participant specification given by designer, it consists
	 *            one ore more
	 *            {@link org.bpel4chor.splitprocess.partition.model.PActivity}
	 * @param nonSplitProcess
	 *            The main process
	 * 
	 * @return The created fragment process
	 * 
	 * @throws NullPointerException
	 *             if argument is null
	 */
	protected Process createFragmentProcess(Participant participant, Process nonSplitProcess) {

		if (participant == null || nonSplitProcess == null)
			throw new NullPointerException("participant==null:" + (participant == null)
					+ " nonSplitProcess==null:" + (nonSplitProcess == null));

		// create a new process for participant
		Process fragProcess = createSkeletonProcess(participant);

		// add correlation set
		addCorrelations(fragProcess);

		// add variables used by the fragment, adding the variable results
		// adding the messages referred by the variable.
		addVariable(fragProcess, participant);

		// add partnerLinks used by the fragment, adding the partnerLink results
		// the partnerLinkType, role, portType that are underlying artifacts
		// under the partnerLink.
		addPartnerLink(fragProcess, participant);

		// recursively handle with the activity from root of the process tree
		// down to the leaves of the tree, add it into fragment process
		// respectively.
		Activity firstAct = nonSplitProcess.getActivity();
		processChild(firstAct, fragProcess, participant);

		// add propertyAlias into fragment definition
		addPropertyAlias(fragProcess);

		// create a participantType and a participant in BPEL4Chor topology
		ParticipantType bpel4chorParticipantType = BPEL4ChorFactory
				.createParticipantType(fragProcess);
		org.bpel4chor.model.topology.impl.Participant bpel4chorParticpiant = BPEL4ChorFactory
				.createParticipant(fragProcess.getName(), bpel4chorParticipantType.getName());
		topology.add(bpel4chorParticipantType);
		topology.add(bpel4chorParticpiant);

		return fragProcess;
	}

	/**
	 * Add the properAlias that is referred by by the original activity.
	 * 
	 * @param fragProc
	 */
	protected void addPropertyAlias(Process fragProc) {

		// get fragment definition
		Definition fragDefn = participant2WSDL.get(fragProc.getName());

		// get original property
		Property origProperty = MyWSDLUtil.findProperty(nonSplitProcessDefn,
				SplitProcessConstants.CORRELATION_PROPERTY_NAME);

		// get the corresponding property in fragment process
		Property fragProperty = MyWSDLUtil.findProperty(fragDefn,
				SplitProcessConstants.CORRELATION_PROPERTY_NAME);

		if (origProperty == null || fragProperty == null)
			throw new NullPointerException();

		// get related alias in the original definition, test whether the
		// referred message is also present in the fragment definition, if yes,
		// then add the propertyAlias into the fragment definition
		QName propertyQName = new QName(nonSplitProcessDefn.getTargetNamespace(),
				origProperty.getName());
		PropertyAlias[] aliases = MyWSDLUtil.findPropertyAlias(nonSplitProcessDefn, propertyQName);
		for (PropertyAlias origAlias : aliases) {
			Message origMsg = (Message) origAlias.getMessageType();
			QName msgQName = new QName(fragDefn.getTargetNamespace(), origMsg.getQName()
					.getLocalPart());
			Message newMsg = MyWSDLUtil.resolveMessage(fragDefn, msgQName);
			if (newMsg != null) {
				PropertyAlias newAlias = MessagepropertiesFactory.eINSTANCE.createPropertyAlias();
				newAlias.setPropertyName(fragProperty);
				newAlias.setMessageType(newMsg);
				newAlias.setPart(origAlias.getPart());
				fragDefn.addExtensibilityElement(newAlias);
			}

		}
	}

	/**
	 * Create a skeleton process for the participant given
	 * 
	 * @param participant
	 * @return
	 */
	protected Process createSkeletonProcess(Participant participant) {

		Process fragProcess = BPELFactory.eINSTANCE.createProcess();

		fragProcess.setName(participant.getName());
		fragProcess.setSuppressJoinFailure(nonSplitProcess.getSuppressJoinFailure());
		fragProcess.setTargetNamespace(nonSplitProcess.getTargetNamespace());

		// set as abstract process
		fragProcess.setAbstractProcessProfile(BPEL4ChorConstants.PBD_ABSTRACT_PROCESS_PROFILE);
		if (nonSplitProcess.getExtensions() != null)
			fragProcess.setExtensions(nonSplitProcess.getExtensions());

		// create the global variable for storage of the correlation set
		// the variable is of simple type "xsd:boolean"
		Variable globalCorrelVar = BPELFactory.eINSTANCE.createVariable();
		XSDTypeDefinition varType = XSDFactory.eINSTANCE.createXSDSimpleTypeDefinition();
		varType.setTargetNamespace(XSDConstants.SCHEMA_FOR_SCHEMA_URI_2001);
		varType.setName(SplitProcessConstants.VARIABLE_FOR_CORRELATION_TYPE);
		globalCorrelVar.setType(varType);
		globalCorrelVar.setName(SplitProcessConstants.VARIABLE_FOR_CORRELATION_NAME);

		// create variables and add the global variables
		Variables fragVars = BPELFactory.eINSTANCE.createVariables();
		fragVars.getChildren().add(globalCorrelVar);
		fragProcess.setVariables(fragVars);

		return fragProcess;
	}

	/**
	 * Copy the original partnerLinks used by the participant into the
	 * participant's WSDL definition, besides, the partnerLinkType and role are
	 * also copied.
	 * 
	 * <p>
	 * There are 2 types of partner link
	 * <ol>
	 * <li>the partnerLink from the non-split process, that were exposed to the
	 * clients
	 * <li>the partnerLink for interaction between the fragments
	 * </ol>
	 * In this method only the partnerLink of the <b>(1)</b> type will be added
	 * into the fragment, the partnerLinks for interaction they don't exists
	 * yet.
	 * 
	 * @param participant
	 *            The participant that contains information which activity is in
	 *            the fragment process
	 * @param proc
	 *            The fragment process
	 */
	protected void addPartnerLink(Process proc, Participant participant) {

		if (proc == null || participant == null)
			throw new NullPointerException("proc==null:" + (proc == null) + " participant==null:"
					+ (participant == null));

		if (proc.getPartnerLinks() == null) {
			PartnerLinks partnerLinks = BPELFactory.eINSTANCE.createPartnerLinks();
			proc.setPartnerLinks(partnerLinks);
		}

		Set<Activity> actsInParticipant = participant.getActivities();

		for (Activity act : actsInParticipant) {
			if (act instanceof PartnerActivity) {

				// Activities that extend PartnerActivity are Invoke,
				// Receive and Reply. For each of the PartnerActivity, we
				// test whether the partnerLinkType, which is referred in
				// the partnerLink, exists in the non-split process's wsdl
				// definition, if yes, then we create a copy of the
				// partnerLinkType (including role, portType) and add into
				// the fragment wsdl definition. Otherwise, the
				// partnerLinkType exists in external imported wsdl, then we
				// copy the 'import' of the external wsdl and insert it
				// into the fragment process.

				Definition fragDefn = participant2WSDL.get(participant.getName());

				// if the partnerLink is defined in the non-split wsdl
				// definition, and still does not exist in the fragment wsdl
				// definition, then add it into the fragment wsdl definition
				// too.
				PartnerLink origPartnerLink = ((PartnerActivity) act).getPartnerLink();

				// in case that there are combined <receive> and <reply>,
				// they use the same partnerLink, just skip it if the
				// partnerLink is already copied and existed in the fragment
				// process.
				if (isExistedInProcess(origPartnerLink, proc))
					continue;

				PartnerLink newPartnerLink = FragmentDuplicator.copyPartnerLink(origPartnerLink);

				// add the partnerLink into fragment process
				proc.getPartnerLinks().getChildren().add(newPartnerLink);

				QName origPltQname = new QName(nonSplitProcessDefn.getTargetNamespace(),
						origPartnerLink.getPartnerLinkType().getName());
				QName newPltQname = new QName(fragDefn.getTargetNamespace(),
						newPartnerLink.getName());
				if (MyWSDLUtil.resolveBPELPartnerLinkType(nonSplitProcessDefn, origPltQname) != null
						&& MyWSDLUtil.resolveBPELPartnerLinkType(fragDefn, newPltQname) == null) {

					// add portType that is contained in the partnerLinkType
					List<Role> newRoles = newPartnerLink.getPartnerLinkType().getRole();
					for (Role newRole : newRoles) {
						PortType newPortType = (PortType) newRole.getPortType();
						QName newPtQname = newPortType.getQName();
						if (MyWSDLUtil.resolvePortType(fragDefn, newPtQname) == null) {
							fragDefn.addPortType(newPortType);
						}
					}

					// add partnerLinkType
					fragDefn.addExtensibilityElement(newPartnerLink.getPartnerLinkType());

				}

				// if the operation referred by this activity exists in the
				// non-split wsdl definition, but not in the fragment
				// definition, then add it to the wsdl.
				PortType origPortType = ((PartnerActivity) act).getPortType();
				Operation origOp = ((PartnerActivity) act).getOperation();
				QName portTypeQName = new QName(fragDefn.getTargetNamespace(), origPortType
						.getQName().getLocalPart());
				String operationName = origOp.getName();
				if (MyWSDLUtil.resolveBPELPartnerLinkType(nonSplitProcessDefn, origPltQname) != null
						&& MyWSDLUtil.resolveOperation(fragDefn, portTypeQName, operationName) == null) {
					// add operation
					Operation newOp = FragmentDuplicator.copyOperation(origOp, fragDefn);
					PortType portType = MyWSDLUtil.resolvePortType(fragDefn, portTypeQName);
					portType.addOperation(newOp);
				}

			}
		}

	}

	/**
	 * Test whether the partnerLink given is already existed in the process
	 * given using name comparison.
	 * 
	 * @param partnerLink
	 * @param proc
	 * @return 'true' for existed, false for otherwise.
	 */
	private boolean isExistedInProcess(PartnerLink partnerLink, Process proc) {
		String partnerLinkName = partnerLink.getName();
		return (MyBPELUtils.getPartnerLink(proc, partnerLinkName) != null);
	}

	/**
	 * Add copy of the correlation sets from non-split process into the fragment
	 * process.
	 * <p>
	 * Note that we have only one correlation set with the property name
	 * "correlProperty".
	 * 
	 * @param fragProcess
	 */
	protected void addCorrelations(Process fragProcess) {

		if (fragProcess == null)
			throw new NullPointerException("fragProcess==null:" + (fragProcess == null));

		CorrelationSets newCorrelationSets = BPELFactory.eINSTANCE.createCorrelationSets();

		if (nonSplitProcess.getCorrelationSets() != null
				&& nonSplitProcess.getCorrelationSets().getChildren().size() == 1) {

			List<CorrelationSet> origCorrelationSets = nonSplitProcess.getCorrelationSets()
					.getChildren();

			CorrelationSet origCorrelSet = origCorrelationSets.get(0);
			CorrelationSet newCorrelSet = BPELFactory.eINSTANCE.createCorrelationSet();

			newCorrelSet.setName(origCorrelSet.getName());

			Definition fragDefn = participant2WSDL.get(fragProcess.getName());

			Property origProperty = origCorrelSet.getProperties().get(0);
			Property newProperty = MyWSDLUtil.findProperty(fragDefn, origProperty.getName());

			if (newProperty == null)
				throw new NullPointerException();
			newCorrelSet.getProperties().add(newProperty);

			newCorrelationSets.getChildren().add(newCorrelSet);

			fragProcess.setCorrelationSets(newCorrelationSets);

			// collect the wsdl property in grounding
			String propertyName = newProperty.getName();
			QName wsdlPropertyQname = new QName(fragDefn.getTargetNamespace(),
					newProperty.getName());
			org.bpel4chor.model.grounding.impl.Property grouProperty = BPEL4ChorFactory
					.createGroundingProperty(propertyName, wsdlPropertyQname);
			grounding.add(grouProperty);
		}

	}

	/**
	 * Add the used variables into the fragment process, including the
	 * referenced messages.
	 * <p>
	 * Adding the referred variable into fragment BPEL process results copying
	 * the corresponding message into the fragment WSDL definition.
	 * 
	 * <p>
	 * The added variables include:
	 * <ol>
	 * <li>variables used by activity
	 * <li>variables used by outgoing links of the activity
	 * </ol>
	 * 
	 * @param fragProcess
	 *            The fragment process
	 * @param participant
	 *            The participant
	 */
	protected void addVariable(Process fragProcess, Participant participant) {

		if (fragProcess == null || participant == null)
			throw new NullPointerException("fragProcess==null:" + (fragProcess == null)
					+ " participant==null:" + (participant == null));

		Variables fragVars = null;

		if (fragProcess.getVariables() == null) {
			fragVars = BPELFactory.eINSTANCE.createVariables();
			fragProcess.setVariables(fragVars);
		} else {
			fragVars = fragProcess.getVariables();
		}

		Definition fragDefn = participant2WSDL.get(participant.getName());

		Set<Activity> actSet = participant.getActivities();
		for (Activity act : actSet) {
			// collect all the variables in the activity and insert into
			// fragProcess
			VariableResolver resolver = new VariableResolver(nonSplitProcess);
			List<Variable> variablesInAct = resolver.resolveVariable(act);

			for (Variable origVarInAct : variablesInAct) {

				// copy variable, create new message in fragment definition.
				Variable newVar = FragmentDuplicator.copyVariable(origVarInAct);

				Message newMsg = newVar.getMessageType();
				// if the message exists in the original WSDL definition,
				// and yet does not exist in the fragment definition, then
				// add it.
				if (newMsg != null
						&& MyWSDLUtil.resolveMessage(nonSplitProcessDefn, newMsg.getQName()) != null
						&& MyWSDLUtil.resolveMessage(fragDefn, newMsg.getQName()) == null) {
					fragDefn.addMessage(newMsg);
				}

				// add into fragment process variables
				List<Variable> varChildren = fragVars.getChildren();
				if (VariableUtil.isExistedVariable(newVar, varChildren) == false) {
					varChildren.add(newVar);
				}
			}
		}

	}

	/**
	 * Place the activity in the corresponding parent in the fragment
	 * respectively.
	 * 
	 * <p>
	 * The parent should be a structured activity, exceptionally, i can be a
	 * process.
	 * 
	 * @param parentInFragment
	 *            The parent in fragment process
	 * @param newAct
	 *            The new activity to add, It is allowed to be <tt>null</tt>.
	 */
	protected void addActivity(BPELExtensibleElement parentInFragment, Activity newAct) {

		if (parentInFragment == null || newAct == null)
			throw new NullPointerException("parentInFragment==null:" + (parentInFragment == null)
					+ " newAct == null:" + (newAct == null));

		// Parent is process
		if (parentInFragment instanceof Process) {
			((Process) parentInFragment).setActivity((Activity) newAct);
			return;
		}

		// Parent is structured activity: Flow, While, Scope, If, Sequence,
		// Pick, ForEach, RepeatUntil
		if (parentInFragment instanceof Flow) {
			Flow parent = (Flow) parentInFragment;
			parent.getActivities().add(newAct);

		} else if (parentInFragment instanceof Scope) {
			// Scope parent = (Scope) parentInFragment;
			// parent.setActivity(newAct);
			throw new RuntimeException(
					"Process Fragmentation with <scope> activity is still not implemented.");
		} else if (parentInFragment instanceof While) {
			// While parent = (While) parentInFragment;
			// parent.setActivity(newAct);
			throw new RuntimeException(
					"Process Fragmentation with <while> activity is still not implemented.");
		} else if (parentInFragment instanceof If) {
			throw new RuntimeException(
					"Process Fragmentation with <if> activity is still not implemented.");
		} else if (parentInFragment instanceof Sequence) {
			throw new RuntimeException(
					"Process Fragmentation with <sequence> activity is still not implemented.");
		} else if (parentInFragment instanceof Pick) {
			throw new RuntimeException(
					"Process Fragmentation with <pick> activity is still not implemented.");
		} else if (parentInFragment instanceof ForEach) {
			throw new RuntimeException(
					"Process Fragmentation with <forEach> activity is still not implemented.");
		} else if (parentInFragment instanceof RepeatUntil) {
			throw new RuntimeException(
					"Process Fragmentation with <repeatUntil> activity is still not implemented.");
		}

	}

	/**
	 * Determine for each child activity whether and how to map it to the target
	 * fragment's process.
	 * 
	 * <p>
	 * The <b>idea</b>: If the child is in or has activities that are in the
	 * fragment, then it must be placed in that fragment’s process - We find the
	 * child's PARENT_LOOP_OR_SCOPE in the main process, then find the
	 * equivalent one in the fragment process, by using GET_BY_NAME.
	 * 
	 * <p>
	 * 
	 * <pre>
	 * The <b>Scenarios</b>:
	 * <ol>
	 * <li>If the child is basic AND it is in the participant                ==> copy child into fragment
	 * <li>If the child is basic AND it is NOT in the participant            ==> do nothing
	 * <li>If the child is Flow AND its children are in the participant      ==> copy child into fragment
	 * <li>If the child is Flow AND its children are NOT in the participant  ==> do nothing
	 * <li>If the child is Scope AND its children are in the participant     ==> copy child into fragment
	 * <li>If the child is Scope AND its children are NOT in the participant ==> do nothing
	 * <li>If the child is While AND its children are in the participant     ==> copy child into fragment
	 * <li>If the child is While AND its children are NOT in the participant ==> do nothing
	 * </ol>
	 * </pre>
	 * 
	 * <p>
	 * 
	 * <pre>
	 * <b>Note</b>: Construction of this procedure will be divided into two stages:
	 * <li>stage 1: implementation for the minimal set : invoke, receive, reply, assign and flow.
	 * <li>stage 2: implementation for scope and while, compensation handlers. TODO
	 * </pre>
	 * 
	 * @param child
	 *            The child activity
	 * @param proc
	 *            The fragment process
	 * @param p
	 *            The participant that tells you which activity is in the given
	 *            fragment process
	 * @throws NullPointerException
	 *             If any of the parameters is <tt>null</tt>
	 */
	protected void processChild(Activity child, Process proc, Participant p) {

		if (child == null || proc == null || p == null)
			throw new NullPointerException("child==null:" + (child == null) + " proc==null:"
					+ (proc == null) + " p==null:" + (p == null));

		Definition fragDefn = participant2WSDL.get(p.getName());

		BPELExtensibleElement parent = null;
		BPELExtensibleElement parentInFragment;
		try {
			// find the parent in non-split process
			parent = getParentStructuredAct(child);
			// find equivalent parent in fragment process
			parentInFragment = getEquivalentAct(proc, parent);
		} catch (ActivityNotFoundException e) {
			throw new RuntimeException(
					"Can not find parent or equivalent activity for given activity: "
							+ child.getName(), e);
		}

		Activity newAct = null;

		final boolean isChildBasicActivity = isBasicActivity(child);
		final boolean isChildInParticipant = isInParticipant(child, p);
		final boolean isDescendantOfChildInParticipant = isDescendantInParticipant(child, p);

		if (isChildBasicActivity && isChildInParticipant) {
			// If the 'child' is basic activity, AND 'child' itself is in the
			// participant. Recall Basic Activity: Invoke, Receive, Reply,
			// Assign.
			newAct = FragmentDuplicator.copyActivity(child, proc, fragDefn);

		} else if (!isChildBasicActivity && isDescendantOfChildInParticipant) {

			// If the 'child' is NOT basic activity, e.g. Flow, Scope and While,
			// and its basic descendants are in the participant, add it into the
			// fragment.
			if (child instanceof Flow) {

				newAct = FragmentDuplicator.copyActivity(child, proc, fragDefn);

			} else if (child instanceof Scope || child instanceof While) {

				newAct = FragmentDuplicator.copyActivity(child, proc, fragDefn);

				if (!isAllChildrenInParticipant(child, p)) {
					// Scope is fragmented, Cross-Partner-Scope
					throw new RuntimeException(
							"Process Fragmentation with fragmented <scope> or <while> is still not implemented.");
				}
			} else if (child instanceof Pick) {
				throw new RuntimeException(
						"Process Fragmentation with fragmented <pick> is still not implemented.");
			} else if (child instanceof ForEach) {
				throw new RuntimeException(
						"Process Fragmentation with fragmented <forEach> is still not implemented.");
			} else if (child instanceof RepeatUntil) {
				throw new RuntimeException(
						"Process Fragmentation with fragmented <repeatUntil> is still not implemented.");
			}

		} else {
			// The 'child' does not belong to participant, neither contains
			// descendants that are in the participant
			return;
		}

		if (parent instanceof Scope && isHandlerMember(child, (Activity) parent)) {
			// add new activity into corresponding handler
			throw new RuntimeException(
					"Process Fragmentation for <scope> with compensation handler is still not implemented.");
		} else {
			addActivity(parentInFragment, newAct);
		}

		// if the child is not basic, and it has descendant children, that
		// are in the participant, then run processChild against the grand
		// children.
		if (!isChildBasicActivity && isDescendantOfChildInParticipant) {
			List<Activity> grandChildren = this.getDirectChildren(child);
			for (Activity grandChild : grandChildren) {
				processChild(grandChild, proc, p);
			}
		}
	}

	/**
	 * Test whether the child activity is one handler of the parent.
	 * 
	 * @param child
	 *            The child activity
	 * @param parent
	 *            The parent activity
	 * @return <tt>true</tt> if the child is a handler of the parent, other wise
	 *         <tt>false</tt>.
	 */
	protected boolean isHandlerMember(Activity child, Activity parent) {
		// TODO
		return false;
	}

	/**
	 * Test whether all children of the structured activity are in the
	 * participant.
	 * 
	 * @param act
	 *            The activity
	 * @param participant
	 * @return <tt>true</tt> if activity is structured activity and its children
	 *         are all in the participant. Otherwise <tt>false</tt>
	 */
	protected boolean isAllChildrenInParticipant(Activity act, Participant participant) {
		// TODO
		return false;
	}

	/**
	 * Get the first parent loop or scope of the activity in the fragment
	 * process.
	 * 
	 * <p>
	 * <b>Note</b>: In moment, structured activity consists of <tt>flow</tt>,
	 * <tt>scope</tt>, <tt>while</tt>. And as exception: The "Process" is
	 * regarded as the root scope.
	 * 
	 * @param child
	 *            The child activity
	 * @return The parent of the child activity in the fragment process. It can
	 *         be process itself, which is instance of Process; it can be Flow,
	 *         Scope or Loop, which is instance of activity.
	 * @throws NullPointerException
	 *             if child is <tt>null</tt>
	 */
	protected BPELExtensibleElement getParentStructuredAct(Activity child) {

		if (child == null) {
			throw new NullPointerException();
		}

		BPELExtensibleElement parent = null;

		if (child.equals(nonSplitProcess.getActivity())) {
			parent = nonSplitProcess;// process is the root scope
		} else {
			// the child is in deeper position
			EObject container = child.eContainer();
			while (ActivityUtil.isStructuredActivity(container) == false) {
				container = container.eContainer();
			}
			parent = (BPELExtensibleElement) container;
		}

		return parent;
	}

	/**
	 * Get the equivalent activity in the fragment's process if any is found.
	 * 
	 * @param proc
	 *            The fragment process
	 * @param act
	 *            The activity name
	 * @return The equivalent activity in the fragment or <tt>null</tt> if
	 *         nothing is found
	 * @throws ActivityNotFoundException
	 * @throws NullPointerException
	 *             if argument is null
	 */
	protected BPELExtensibleElement getEquivalentAct(Process proc, BPELExtensibleElement act)
			throws ActivityNotFoundException {

		if (proc == null || act == null) {
			throw new NullPointerException();
		}

		BPELExtensibleElement equivalentAct = null;

		if (act instanceof Process) {
			// equivalent activity to the "process" is the "proc".
			return proc;
		} else {
			ActivityFinder finder = new ActivityFinder(proc);
			equivalentAct = finder.find(act.getElement().getAttribute("name"));
		}
		return equivalentAct;
	}

	/**
	 * Test if the activity is basic activity
	 * 
	 * @param act
	 *            The activity
	 * @return <tt>true</tt> if the activity is <tt>not-null</tt> and
	 *         <tt>basic</tt>, otherwise <tt>false</tt>
	 * 
	 * @see {@link ActivityUtil#isBasicActivity(Activity)}
	 */
	protected boolean isBasicActivity(Activity act) {
		// delegate to the ActivityUtil
		return act != null ? ActivityUtil.isBasicActivity(act) : false;
	}

	/**
	 * Test if the activity is simple activity
	 * 
	 * @param act
	 *            The activity
	 * @return <tt>true</tt> if the activity is <tt>not-null</tt> and
	 *         <tt>simple</tt>, otherwise <tt>false</tt>
	 * @see {@link ActivityUtil#isSimpleActivity(Activity)}
	 */
	protected boolean isSimpleActivity(Activity act) {
		// delegate to ActivityUtil
		return act != null ? ActivityUtil.isSimpleActivity(act) : false;
	}

	/**
	 * Test if the activity is the start point in the process
	 * 
	 * @param act
	 *            The activity
	 * @return <tt>true</tt> if it is the start receive, or <tt>false</tt>
	 */
	protected boolean isStartActivity(Activity act) {
		if (act == null)
			throw new NullPointerException();

		EObject container = act.eContainer();
		if (container instanceof Flow || container instanceof Scope
				|| container instanceof Sequence) {
			EObject superContainer = container.eContainer();
			if ((superContainer instanceof Process) && act instanceof Receive
					&& (act.getTargets() == null)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Test whether the activity is in the participant
	 * 
	 * @param act
	 *            The activity
	 * @param participant
	 *            The participant
	 * @return <tt>true</tt> if the activity is in the participant,
	 *         <tt>false</tt> otherwise.
	 * @throws NullPointerException
	 *             if act or participant is <tt>null</tt>
	 * 
	 */
	protected boolean isInParticipant(Activity act, Participant participant) {

		if ((act == null) || (participant == null)) {
			throw new NullPointerException();
		}

		Set<Activity> actsInParticipant = participant.getActivities();
		return actsInParticipant.contains(act);
	}

	/**
	 * Test whether the direct- or sub- basic children of the activity are in
	 * the participant.
	 * 
	 * @param act
	 *            The activity, recall that the activity is from the main BPEL
	 *            process.
	 * @param participant
	 *            The participant
	 * @return <tt>true</tt> if children of the activity is in the participant,
	 *         <tt>false</tt> otherwise.
	 * @throws NullPointerException
	 *             if any of the arguments is <tt>null</tt>
	 */
	protected boolean isDescendantInParticipant(Activity act, Participant participant) {

		if ((act == null) || (participant == null)) {
			throw new NullPointerException();
		}

		List<Activity> children = ActivityUtil.getAllDescBasicChildren(act);
		Set<Activity> actsInParticipant = participant.getActivities();
		for (Activity child : children) {
			if (actsInParticipant.contains(child)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get direct children activity of the given activity
	 * 
	 * @param act
	 *            The activity
	 * @return The children of the activity
	 */
	protected List<Activity> getDirectChildren(Activity act) {
		return ActivityUtil.getDirectChildren(act);
	}

	/**
	 * Reset the fragmenter's properties
	 * 
	 * @param process
	 *            The BPEL process
	 * @param partitionSpec
	 *            The partition specification
	 * @throws PartitionSpecificationException
	 * @throws NullPointerException
	 *             if arguments are <tt>null</tt>
	 */
	public void reset(Process process, PartitionSpecification partitionSpec)
			throws PartitionSpecificationException {
		if (process == null || partitionSpec == null)
			throw new NullPointerException();

		this.nonSplitProcess = process;
		this.partitionSpec = partitionSpec;
		this.participant2FragProc.clear();
		this.participant2WSDL.clear();
	}

	public Map<String, Definition> getParticipant2WSDLMap() {
		return participant2WSDL;
	}

	public Map<String, Process> getParticipant2FragProcessMap() {
		return participant2FragProc;
	}

}
