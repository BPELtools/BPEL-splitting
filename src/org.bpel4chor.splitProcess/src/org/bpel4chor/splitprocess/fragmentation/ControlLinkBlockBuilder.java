package org.bpel4chor.splitprocess.fragmentation;

import java.util.List;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.bpel4chor.model.grounding.impl.Grounding;
import org.bpel4chor.model.topology.impl.Topology;
import org.bpel4chor.splitprocess.utils.NameGenerator;
import org.bpel4chor.splitprocess.utils.SplitProcessConstants;
import org.bpel4chor.utils.BPEL4ChorFactory;
import org.bpel4chor.utils.BPEL4ChorUtil;
import org.bpel4chor.utils.MyBPELUtils;
import org.bpel4chor.utils.MyWSDLUtil;
import org.bpel4chor.utils.exceptions.AmbiguousPropertyForLinkException;
import org.eclipse.bpel.model.Assign;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.Catch;
import org.eclipse.bpel.model.Condition;
import org.eclipse.bpel.model.FaultHandler;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.PartnerLink;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Receive;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Sequence;
import org.eclipse.bpel.model.Source;
import org.eclipse.bpel.model.Target;
import org.eclipse.bpel.model.Variable;
import org.eclipse.bpel.model.messageproperties.MessagepropertiesFactory;
import org.eclipse.bpel.model.messageproperties.Property;
import org.eclipse.bpel.model.messageproperties.PropertyAlias;
import org.eclipse.bpel.model.partnerlinktype.PartnerLinkType;
import org.eclipse.bpel.model.partnerlinktype.PartnerlinktypeFactory;
import org.eclipse.bpel.model.partnerlinktype.Role;
import org.eclipse.bpel.model.util.BPELConstants;
import org.eclipse.bpel.model.util.BPELUtils;
import org.eclipse.wst.wsdl.Definition;
import org.eclipse.wst.wsdl.Message;
import org.eclipse.wst.wsdl.Operation;
import org.eclipse.wst.wsdl.PortType;

/**
 * BlockBuilder creates sending block, receiving block, and the prerequisites in
 * WSDL for them.
 * <p>
 * Note that this utility is used in Splitting Control Link, namely in the
 * {@link ControlLinkFragmenter}.
 * 
 * @since Feb 18, 2012
 * @author Daojun Cui
 */
public class ControlLinkBlockBuilder {

	/** the process invoked */
	protected Process targetProcess = null;

	/** the invoking process */
	protected Process sourceProcess = null;

	/** non-split process */
	protected Process nonSplitProcess = null;

	/** WSDL Definition of invoked process */
	protected Definition targetDefn = null;

	/** The link in the source process */
	protected Link linkInSourceProcess = null;

	/** Message to convey control information */
	protected Message ctrlLinkMessage = null;

	/** Variable to handle the control information 'true' and correlation */
	protected Variable varTrueAndCorrel = null;

	/** Variable to handle the control information 'false' and correlation */
	protected Variable varFalseAndCorrel = null;

	/** Variable to receive control information */
	protected Variable varReceive = null;

	/** Operation for invoked portType */
	protected Operation operation = null;

	/** PortType for invoked process */
	protected PortType portType = null;

	/** Role for invoked portType */
	protected Role role = null;

	/** PartnerLinkType for the invoker and invokee */
	protected PartnerLinkType plt = null;

	/** PartnerLink for invoked process */
	protected PartnerLink targetPartnerLink = null;

	/** PartnerLink for invoking process */
	protected PartnerLink sourcePartnerLink = null;

	/** Topology */
	protected Topology topology = null;

	/** Grounding */
	protected Grounding grounding = null;

	/** The message link name for invoke sending true */
	protected String topoMsgLinkTrueName = null;

	/** The message link name for invoke sending false */
	protected String topoMsgLinkFalseName = null;

	protected Logger logger = Logger.getLogger(ControlLinkBlockBuilder.class);

	public ControlLinkBlockBuilder(Process targetProcess, Process sourceProcess,
			Process nonSplitProcess, Definition targetDefn, Link linkInSourceProcess,
			Topology topology, Grounding grounding) {

		if (targetProcess == null || sourceProcess == null || nonSplitProcess == null
				|| targetDefn == null || linkInSourceProcess == null || topology == null
				|| grounding == null)
			throw new NullPointerException();

		this.targetProcess = targetProcess;
		this.sourceProcess = sourceProcess;
		this.nonSplitProcess = nonSplitProcess;
		this.targetDefn = targetDefn;
		this.linkInSourceProcess = linkInSourceProcess;
		this.topology = topology;
		this.grounding = grounding;
		// this.createPrerequisites();
	}

	/**
	 * Create prerequisites that enable the creation of sending- and receiving
	 * block.
	 * <p>
	 * Create Message, Operation, PortType, PartnerLinkType, PropertyAlias in
	 * target WSDL definition; create variable and partnerLink for invoke and
	 * receive activity in the sending- and receiving block.
	 * <p>
	 * Things to be done before creating sending and receiving block:
	 * <ol>
	 * <li>create control link message
	 * <li>create variables for invoke and receive
	 * <li>create operation per link,
	 * <li>create portType, role, and partnerLinkType for the invoked process
	 * </ol>
	 * 
	 */
	public void createPrerequisites() {

		// 1. create control link message with the given
		// QName
		QName ctrlMsgQName = new QName(targetDefn.getTargetNamespace(),
				SplitProcessConstants.CONTROL_LINK_MESSAGE_NAME);
		ctrlLinkMessage = MyWSDLUtil.resolveMessage(targetDefn, ctrlMsgQName);
		if (ctrlLinkMessage == null) {
			ctrlLinkMessage = FragmentFactory.createControlLinkMessage(targetDefn
					.getTargetNamespace());
			targetDefn.addMessage(ctrlLinkMessage);

			// add propertyAlias that points the correlation property to the
			// part "correlation" in the control link message

			Property correlProperty = MyWSDLUtil.findProperty(targetDefn,
					SplitProcessConstants.CORRELATION_PROPERTY_NAME);

			PropertyAlias propertyAlias = MessagepropertiesFactory.eINSTANCE.createPropertyAlias();
			propertyAlias.setPropertyName(correlProperty);
			propertyAlias.setMessageType(ctrlLinkMessage);
			propertyAlias.setPart(SplitProcessConstants.CORRELATION_PART_NAME);
			targetDefn.addExtensibilityElement(propertyAlias);
		}

		// 2. create sending and receiving variables
		// with the created message
		String varTrueName = "variableTrueAndCorrel";
		String varFalseName = "variableFalseAndCorrel";
		String varReceiveName = "variableReceive";
		varTrueAndCorrel = FragmentFactory.createSendingBlockVariable(sourceProcess, varTrueName,
				ctrlLinkMessage);
		varFalseAndCorrel = FragmentFactory.createSendingBlockVariable(sourceProcess, varFalseName,
				ctrlLinkMessage);
		varReceive = FragmentFactory.createReceivingBlockVariable(targetProcess, varReceiveName,
				ctrlLinkMessage);

		addVariableToProcess(varTrueAndCorrel, sourceProcess);
		addVariableToProcess(varFalseAndCorrel, sourceProcess);
		addVariableToProcess(varReceive, targetProcess);

		// 3. one operation per link
		operation = FragmentFactory.createOperation(targetDefn, ctrlLinkMessage,
				linkInSourceProcess.getName() + "Operation");

		// 4. one portType for target process
		String portTypeName = sourceProcess.getName() + targetProcess.getName() + "PT";
		portType = MyWSDLUtil.findPortType(targetDefn, portTypeName);
		if (portType == null) {
			portType = FragmentFactory.createPortType(targetDefn, operation, portTypeName);
			addPortTypeToDefinition(portType, targetDefn);
		} else {
			portType.addOperation(operation);
		}

		// 6. PartnerLinkType and its Role
		String pltName = sourceProcess.getName() + targetProcess.getName() + "PLT";
		String roleName = sourceProcess.getName() + targetProcess.getName() + "Role";
		plt = MyWSDLUtil.findPartnerLinkType(targetDefn, pltName);
		if (plt == null) {
			plt = PartnerlinktypeFactory.eINSTANCE.createPartnerLinkType();
			plt.setName(pltName);
			Role newRole = PartnerlinktypeFactory.eINSTANCE.createRole();
			newRole.setName(roleName);
			newRole.setPortType(portType);
			plt.getRole().add(newRole);
			addPartnerLinkTypeToDefinition(plt, targetDefn);
		}
		role = MyWSDLUtil.findRole(plt, roleName);

		// 7. PartnerLink for each processes
		String partnerLinkName = sourceProcess.getName() + targetProcess.getName() + "PL";

		// 7.1 PartnerLink target process
		targetPartnerLink = BPELUtils.getPartnerLink(targetProcess, partnerLinkName);
		if (targetPartnerLink == null) {
			targetPartnerLink = BPELFactory.eINSTANCE.createPartnerLink();
			targetPartnerLink.setName(partnerLinkName);
			targetPartnerLink.setPartnerLinkType(plt);
			targetPartnerLink.setMyRole(role);
			addPartnerLinkToProcess(targetPartnerLink, targetProcess);
		}

		// 7.2 PartnerLink source process
		sourcePartnerLink = BPELUtils.getPartnerLink(sourceProcess, partnerLinkName);
		if (sourcePartnerLink == null) {
			sourcePartnerLink = BPELFactory.eINSTANCE.createPartnerLink();
			sourcePartnerLink.setName(partnerLinkName);
			sourcePartnerLink.setPartnerLinkType(plt);
			sourcePartnerLink.setPartnerRole(role);
			addPartnerLinkToProcess(sourcePartnerLink, sourceProcess);
		}
	}

	/**
	 * Create sending block in the source process, insert the created structure
	 * into the parent, at the end set up the link from its source activity to
	 * the target sending block.
	 * <p>
	 * Note that for each sending block there is a message link in topology and
	 * grounding
	 */
	public void createSendingBlock() {

		logger.debug("Create Sending Block for link " + linkInSourceProcess.getName()
				+ " in process " + sourceProcess.getName());

		// assign for inputVariable for true
		boolean statusTrue = true;
		Assign assignTrue = FragmentFactory.createSendingBlockAssign(sourceProcess,
				varTrueAndCorrel, statusTrue);

		// invoke for sending true
		boolean supJoinFailureInvokeTrue = false;
		Invoke invokeTrue = FragmentFactory.createSendingBlockInvoke(linkInSourceProcess.getName()
				+ "InvokeTrue", sourcePartnerLink, portType, operation, varTrueAndCorrel,
				supJoinFailureInvokeTrue);

		// sequence to accommodate both assign and invoke for true
		String sugguestName4SeqTrue = "sequenceSendTrue";
		Sequence sequenceTrue = FragmentFactory.createSendingBlockSequence(sourceProcess,
				assignTrue, invokeTrue, sugguestName4SeqTrue);

		// assign for inputVariable for false
		boolean statusFalse = false;
		Assign assignFalse = FragmentFactory.createSendingBlockAssign(sourceProcess,
				varFalseAndCorrel, statusFalse);

		// invoke for sending false
		boolean supJoinFailureInvokeFalse = true;
		Invoke invokeFalse = FragmentFactory.createSendingBlockInvoke(linkInSourceProcess.getName()
				+ "InvokeFalse", sourcePartnerLink, portType, operation, varFalseAndCorrel,
				supJoinFailureInvokeFalse);

		// sequence to accommodate both assign and invoke for true
		String sugguestName4SeqFalse = "sequenceSendFalse";
		Sequence sequenceFalse = FragmentFactory.createSendingBlockSequence(sourceProcess,
				assignFalse, invokeFalse, sugguestName4SeqFalse);

		// fault handler for scope
		Catch scopeCatch = BPELFactory.eINSTANCE.createCatch();
		scopeCatch.setFaultName(new QName(nonSplitProcess.getTargetNamespace(), "joinFailure",
				BPELConstants.PREFIX));
		scopeCatch.setActivity(sequenceFalse);

		FaultHandler fh = BPELFactory.eINSTANCE.createFaultHandler();
		fh.getCatch().add(scopeCatch);

		// scope
		String sugguestScopeName = "sendingBlockScope";
		Scope scope = FragmentFactory.createSendingBlockScope(sourceProcess, sequenceTrue, fh,
				sugguestScopeName);

		// insert the scope into parent
		Flow flow = MyBPELUtils.findFirstParentFlow(linkInSourceProcess);
		flow.getActivities().add(scope);

		// connect the link to sending block, since the link just has source,
		// still has no target
		Target target = BPELFactory.eINSTANCE.createTarget();
		target.setActivity(scope);
		target.setLink(linkInSourceProcess);
		scope.getTargets().getChildren().add(target);

		// add link to flow
		if (MyBPELUtils.resolveLink(flow, linkInSourceProcess.getName()) == null)
			flow.getLinks().getChildren().add(linkInSourceProcess);
		else
			throw new IllegalStateException("duplicated link name " + linkInSourceProcess.getName()
					+ ". sourceProcess: " + sourceProcess.getName() + " targetProcess: "
					+ targetProcess.getName());

		// create a message link in topology for the invokes true
		String msgName = ctrlLinkMessage.getQName().getLocalPart();
		NameGenerator nameGen = new NameGenerator(topology);

		org.bpel4chor.model.topology.impl.MessageLink topologyMsgLinkTrue = BPEL4ChorFactory
				.createTopologyMessageLink();
		topoMsgLinkTrueName = nameGen.getUniqueTopoMsgLinkName(msgName + "Link");
		String sender = sourceProcess.getName();
		String sendActTrue = invokeTrue.getName();

		topologyMsgLinkTrue.setName(topoMsgLinkTrueName);
		topologyMsgLinkTrue.setSender(sender);
		topologyMsgLinkTrue.setSendActivity(sendActTrue);
		topologyMsgLinkTrue.setMessageName(msgName);

		topology.add(topologyMsgLinkTrue);

		// the corresponding message link in grounding
		org.bpel4chor.model.grounding.impl.MessageLink groundingMsgLinkTrue = BPEL4ChorFactory
				.createGroundingMessageLink(topologyMsgLinkTrue, portType, operation);
		grounding.add(groundingMsgLinkTrue);

		// create a message link in topology for the invoke false
		org.bpel4chor.model.topology.impl.MessageLink topologyMsgLinkFalse = BPEL4ChorFactory
				.createTopologyMessageLink();
		topoMsgLinkFalseName = nameGen.getUniqueTopoMsgLinkName(msgName + "Link");
		String sendActFalse = invokeFalse.getName();

		topologyMsgLinkFalse.setName(topoMsgLinkFalseName);
		topologyMsgLinkFalse.setSender(sender);
		topologyMsgLinkFalse.setSendActivity(sendActFalse);
		topologyMsgLinkFalse.setMessageName(msgName);

		topology.add(topologyMsgLinkFalse);

		// the corresponding message in grounding
		org.bpel4chor.model.grounding.impl.MessageLink groundingMsgLinkFalse = BPEL4ChorFactory
				.createGroundingMessageLink(topologyMsgLinkFalse, portType, operation);
		grounding.add(groundingMsgLinkFalse);
	}

	/**
	 * Create receiving block in the target process
	 * <p>
	 * A receive activity is created, a link with a transition condition, which
	 * is set to the part "status" of the inputVariable in the receive activity,
	 * is created. The link combine the receive activity with the target
	 * activity.
	 * 
	 * @throws AmbiguousPropertyForLinkException
	 */
	public void createReceivingBlock() throws AmbiguousPropertyForLinkException {

		logger.debug("Create Receiving Block for link " + linkInSourceProcess.getName()
				+ " in process " + sourceProcess.getName());

		// create the receive
		String sugguestName = "from" + sourceProcess.getName();
		Receive receive = FragmentFactory.createReceivingBlockReceive(targetProcess, targetDefn,
				targetPartnerLink, portType, operation, varReceive, sugguestName);

		// create assign for copying correlation to global variable
		Assign assign = FragmentFactory.createAssign4GlobalVar(targetProcess, varReceive);

		// create a sequence for accommodate the receive and assign
		String seqName = "ReceivingBlock";
		Sequence sequence = FragmentFactory.createSequence4GlobalVar(targetProcess, seqName);

		// add the receive and the assign in the sequence
		sequence.getActivities().add(receive);
		sequence.getActivities().add(assign);

		// setup transition condition
		Condition condition = BPELFactory.eINSTANCE.createCondition();
		condition.setBody("$" + varReceive.getName() + ".status)");

		// set transitionCondition and combine the sequence (now the receive is
		// in the sequence) to the link,
		Link linkInTargetProcess = MyBPELUtils.findLinkInActivityTarget(
				linkInSourceProcess.getName(), targetProcess);
		Source source = BPELFactory.eINSTANCE.createSource();
		source.setTransitionCondition(condition);
		source.setLink(linkInTargetProcess);
		source.setActivity(sequence);

		// insert the sequence into parent flow
		Flow flow = MyBPELUtils.findFirstParentFlow(linkInTargetProcess);
		flow.getActivities().add(sequence);

		// add the link into flow too
		if (MyBPELUtils.resolveLink(flow, linkInTargetProcess.getName()) == null) {
			flow.getLinks().getChildren().add(linkInTargetProcess);
		} else
			throw new IllegalStateException("duplicated link name");

		// update the attribute 'selects' of the participant in the topology
		String participantName = sourceProcess.getName();
		org.bpel4chor.model.topology.impl.Participant bpel4chorParticipant = BPEL4ChorUtil
				.resolveParticipant(topology, participantName);
		List<String> selects = bpel4chorParticipant.getSelects();
		String select = targetProcess.getName();
		if (selects.contains(select) == false)
			bpel4chorParticipant.getSelects().add(select);

		// update in the topology message links for the 'receiver' and
		// 'receiveActivity'
		org.bpel4chor.model.topology.impl.MessageLink topologyMsgLinkTrue = BPEL4ChorUtil
				.resolveTopologyMessageLinkByName(topology, topoMsgLinkTrueName);
		topologyMsgLinkTrue.setReceiver(targetProcess.getName());
		topologyMsgLinkTrue.setReceiveActivity(receive.getName());

		org.bpel4chor.model.topology.impl.MessageLink topologyMsgLinkFalse = BPEL4ChorUtil
				.resolveTopologyMessageLinkByName(topology, topoMsgLinkFalseName);
		topologyMsgLinkFalse.setReceiver(targetProcess.getName());
		topologyMsgLinkFalse.setReceiveActivity(receive.getName());

	}

	protected void addVariableToProcess(Variable variable, Process process) {

		if (variable == null || process == null || process.getVariables() == null)
			throw new NullPointerException();

		for (Variable var : process.getVariables().getChildren()) {
			if (var.getName().equals(variable.getName())) {
				return;// exists
			}
		}

		process.getVariables().getChildren().add(variable);
	}

	protected void addPartnerLinkTypeToDefinition(PartnerLinkType plt, Definition definition) {

		if (plt == null || definition == null)
			throw new NullPointerException();

		if (MyWSDLUtil.findPartnerLinkType(definition, plt.getName()) == null) {
			definition.addExtensibilityElement(plt);
		}
	}

	protected void addPartnerLinkToProcess(PartnerLink partnerLink, Process process) {

		if (partnerLink == null || process == null || process.getPartnerLinks() == null)
			throw new NullPointerException();

		if (BPELUtils.getPartnerLink(process, partnerLink.getName()) == null) {
			process.getPartnerLinks().getChildren().add(partnerLink);
		}

	}

	protected void addMessageToDefinition(Message msg, Definition definition) {

		if (msg == null || definition == null)
			throw new NullPointerException();

		if (MyWSDLUtil.resolveMessage(definition, msg.getQName()) == null) {
			definition.addMessage(msg);
		}

	}

	protected void addPortTypeToDefinition(PortType portType, Definition definition) {
		if (portType == null || definition == null)
			throw new NullPointerException();

		List ptList = definition.getEPortTypes();

		for (Object pt : ptList) {
			if (((PortType) pt).getQName().equals(portType.getQName())) {
				logger.warn("PortType :" + BPEL4ChorUtil.getString(portType.getQName())
						+ " has already existed.");
				return;
			}
		}

		definition.addPortType(portType);
	}

}
