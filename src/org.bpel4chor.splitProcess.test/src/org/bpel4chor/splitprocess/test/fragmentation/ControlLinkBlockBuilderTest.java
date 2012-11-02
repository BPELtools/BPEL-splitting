package org.bpel4chor.splitprocess.test.fragmentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import javax.wsdl.WSDLException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.bpel4chor.model.grounding.impl.Grounding;
import org.bpel4chor.model.topology.impl.MessageLink;
import org.bpel4chor.model.topology.impl.Participant;
import org.bpel4chor.model.topology.impl.ParticipantType;
import org.bpel4chor.model.topology.impl.Topology;
import org.bpel4chor.splitprocess.RuntimeData;
import org.bpel4chor.splitprocess.fragmentation.ControlLinkBlockBuilder;
import org.bpel4chor.splitprocess.fragmentation.ProcessFragmenter;
import org.bpel4chor.splitprocess.partition.model.PartitionSpecification;
import org.bpel4chor.splitprocess.partition.representation.PartitionSpecReader;
import org.bpel4chor.utils.BPEL4ChorModelConstants;
import org.bpel4chor.utils.BPEL4ChorReader;
import org.bpel4chor.utils.BPEL4ChorUtil;
import org.bpel4chor.utils.BPEL4ChorWriter;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Assign;
import org.eclipse.bpel.model.BPELPlugin;
import org.eclipse.bpel.model.Copy;
import org.eclipse.bpel.model.Correlation;
import org.eclipse.bpel.model.CorrelationSet;
import org.eclipse.bpel.model.Correlations;
import org.eclipse.bpel.model.FaultHandler;
import org.eclipse.bpel.model.From;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.PartnerLink;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Receive;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Sequence;
import org.eclipse.bpel.model.Source;
import org.eclipse.bpel.model.Target;
import org.eclipse.bpel.model.To;
import org.eclipse.bpel.model.Variable;
import org.eclipse.bpel.model.messageproperties.Property;
import org.eclipse.bpel.model.messageproperties.PropertyAlias;
import org.eclipse.bpel.model.partnerlinktype.PartnerLinkType;
import org.eclipse.bpel.model.partnerlinktype.Role;
import org.eclipse.bpel.model.resource.BPELResourceFactoryImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.wst.wsdl.Definition;
import org.eclipse.wst.wsdl.Message;
import org.eclipse.wst.wsdl.Operation;
import org.eclipse.wst.wsdl.Part;
import org.eclipse.wst.wsdl.PortType;
import org.eclipse.wst.wsdl.internal.util.WSDLResourceFactoryImpl;
import org.eclipse.xsd.util.XSDResourceFactoryImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.uni_stuttgart.iaas.bpel.model.utilities.MyBPELUtils;
import de.uni_stuttgart.iaas.bpel.model.utilities.MyWSDLUtil;
import de.uni_stuttgart.iaas.bpel.model.utilities.exceptions.AmbiguousPropertyForLinkException;

public class ControlLinkBlockBuilderTest {
	static File testFileDir = null;

	static Process process = null;

	static Definition definition = null;

	static Process sourceProcess = null;

	static Definition sourceDefn = null;

	static Process targetProcess = null;

	static Definition targetDefn = null;

	static PartitionSpecification partitionSpec = null;

	static MyBlockCreator blockBuilder = null;

	static Link linkInSourceProcess = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		//
		// use the process "OrderingProcessSimple1", it has only 2 basic
		// activities, suit to block creator.
		//

		File projectDir = new File("");
		testFileDir = new File(projectDir.getAbsolutePath(), "files");

		// init
		BPELPlugin bpelPlugin = new BPELPlugin();
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("bpel",
				new BPELResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("wsdl",
				new WSDLResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xsd",
				new XSDResourceFactoryImpl());

		// load bpel resource
		ResourceSet resourceSet = new ResourceSetImpl();
		URI uri = URI.createFileURI(testFileDir.getAbsolutePath()
				+ "\\OrderInfoSimple1\\bpelContent\\OrderingProcessSimple1.bpel");
		Resource resource = resourceSet.getResource(uri, true);
		process = (Process) resource.getContents().get(0);

		definition = MyWSDLUtil.readWSDL(testFileDir.getAbsolutePath()
				+ "\\OrderInfoSimple1\\bpelContent\\OrderingProcessSimple1.wsdl");

		// partition specification
		FileInputStream inputStream = new FileInputStream(new File(testFileDir.getAbsolutePath()
				+ "\\OrderInfoSimple1\\bpelContent\\Partition.xml"));
		PartitionSpecReader partitionReader = new PartitionSpecReader();
		partitionSpec = partitionReader.readSpecification(inputStream, process);

		// fragment process
		RuntimeData data = new RuntimeData(process, partitionSpec, definition,
				BPEL4ChorModelConstants.DEFAULT_SPLITTING_OUTPUT_DIR);
		ProcessFragmenter procFragmenter = new ProcessFragmenter(data);
		procFragmenter.fragmentizeProcess();

		// source
		sourceProcess = data.getFragmentProcess("participant1");
		sourceDefn = data.getFragmentDefinition("participant1");

		// target
		targetProcess = data.getFragmentProcess("participant2");
		targetDefn = data.getFragmentDefinition("participant2");

		// control link
		linkInSourceProcess = MyBPELUtils.findLinkInActivitySource("ReceiveA2AssignB",
				sourceProcess);

		// blockcreator
		blockBuilder = new MyBlockCreator(targetProcess, sourceProcess, process, targetDefn,
				linkInSourceProcess, data.getTopology(), data.getGrounding());
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testAll() throws AmbiguousPropertyForLinkException, WSDLException, IOException{
		testCreatePrerequisites();
		testCreateSendingBlock();
		testCreateReceivingBlock();
	}
	
	public void testCreatePrerequisites() {

		blockBuilder.createPrerequisites();

		// assert all should be NOT null
		assertAllPrerequisitesNotNull();

		// assert the propertyAlias is added in participant y
		assertPropertyAliasCreatedInParticipantY();
	}

	private void assertPropertyAliasCreatedInParticipantY() {

		QName propertyQName = new QName(targetDefn.getTargetNamespace(),
				BPEL4ChorModelConstants.CORRELATION_PROPERTY_NAME);

		PropertyAlias[] aliases = MyWSDLUtil.findPropertyAlias(targetDefn, propertyQName);

		assertNotNull(aliases.length > 0);

		assertPropertyAlias4ControlLinkMessageIsCreated(aliases);

	}

	private void assertPropertyAlias4ControlLinkMessageIsCreated(PropertyAlias[] aliases) {
		boolean alias4ControlLinkMessage = false;

		for (PropertyAlias alias : aliases) {
			if (((Message) alias.getMessageType()).getQName().getLocalPart()
					.equals(BPEL4ChorModelConstants.CONTROL_LINK_MESSAGE_NAME))
				alias4ControlLinkMessage = true;
		}
		assertTrue(alias4ControlLinkMessage);
	}

	private void assertAllPrerequisitesNotNull() {
		assertNotNull(blockBuilder.getCtrlLinkMessage());
		assertNotNull(blockBuilder.getVarTrueAndCorrel());
		assertNotNull(blockBuilder.getVarFalseAndCorrel());
		assertNotNull(blockBuilder.getVarReceive());
		assertNotNull(blockBuilder.getOperation());
		assertNotNull(blockBuilder.getPortType());
		assertNotNull(blockBuilder.getRole());
		assertNotNull(blockBuilder.getPartnerLinkType());
		assertNotNull(blockBuilder.getSourcePartnerLink());
		assertNotNull(blockBuilder.getTargetPartnerLink());
	}

	
	public void testCreateSendingBlock() throws WSDLException, IOException {

		blockBuilder.createSendingBlock();

		// MyWSDLUtil.print(targetDefn);

		// assertion: the sending block is now connected to the link
		Target target = linkInSourceProcess.getTargets().get(0);
		assertNotNull(target.getActivity());
		Activity scope = target.getActivity();
		assertTrue(scope instanceof Scope);

		// assertion: its activity is sequenceTrue
		Activity sequence4True = ((Scope) scope).getActivity();
		assertTrue(sequence4True instanceof Sequence);

		// assertion: the sequence accommodates an assign and an invoke which
		// has 'suppressJoinFailure=false'
		assertTrue(((Sequence) sequence4True).getActivities().size() == 2);
		Activity assignTrue = ((Sequence) sequence4True).getActivities().get(0);
		Activity invokeTrue = ((Sequence) sequence4True).getActivities().get(1);
		assertTrue(assignTrue instanceof Assign);
		assertAssignCopyGlobalVarToMessageCorrelationPart(assignTrue, sourceProcess);

		assertTrue(invokeTrue instanceof Invoke);
		assertTrue(invokeTrue.getSuppressJoinFailure() == false);

		assertTrue(((Invoke) invokeTrue).getInputVariable().getMessageType() != null);

		// assertion: faultHandler contains sequenceFalse
		FaultHandler fh = ((Scope) scope).getFaultHandlers();
		Activity sequenceFalse = fh.getCatch().get(0).getActivity();

		// assertion: sequence accommodates an assign and an invoke
		assertTrue(sequenceFalse instanceof Sequence);
		assertTrue(((Sequence) sequenceFalse).getActivities().size() == 2);
		Activity assignFalse = ((Sequence) sequenceFalse).getActivities().get(0);
		Activity invokeFalse = ((Sequence) sequenceFalse).getActivities().get(1);
		assertTrue(assignFalse instanceof Assign);
		assertTrue(invokeFalse instanceof Invoke);

		assertTrue(((Invoke) invokeFalse).getInputVariable().getMessageType() != null);

		// assertion: two participantTypes are created in the topology
		Topology topology = blockBuilder.getTopology();
		assertTrue(topology.getName().equals(process.getName() + "Topology"));

		List<ParticipantType> pTypes = topology.getParticipantTypes();
		assertTrue(pTypes.size() == 2);

		// assertion: two participants are created in the topology
		List<Participant> pList = topology.getParticipants();
		assertTrue(pList.size() == 2);

		Participant p1 = BPEL4ChorUtil.resolveParticipant(topology, "participant1");
		assertNotNull(p1);
		assertTrue(p1.getType().equals("participant1Type"));

		Participant p2 = BPEL4ChorUtil.resolveParticipant(topology, "participant2");
		assertNotNull(p2);
		assertTrue(p2.getType().equals("participant2Type"));

		// assertion: two messageLinks are created in the topology
		List<MessageLink> msgLinks = topology.getMessageLinks();
		assertTrue(msgLinks.size() == 2);

		for (MessageLink msgLink : msgLinks) {
			assertTrue(msgLink.getSender().equals(sourceProcess.getName()));
			assertTrue(msgLink.getMessageName().equals("controlLinkMessage"));
		}

		// assertion: two messageLinks are created in the grounding
		Grounding grounding = blockBuilder.getGrounding();
		List<org.bpel4chor.model.grounding.impl.MessageLink> groundMsgLinks = grounding
				.getMessageLinks();
		assertTrue(groundMsgLinks.size() == 2);

		for (org.bpel4chor.model.grounding.impl.MessageLink gMsgLink : groundMsgLinks) {
			QName porTypeQName = gMsgLink.getPortType();
			PortType portType = MyWSDLUtil.resolvePortType(targetDefn, porTypeQName);
			assertNotNull(portType);
			String opName = gMsgLink.getOperation();
			Operation op = MyWSDLUtil.resolveOperation(targetDefn, porTypeQName, opName);
			assertNotNull(op);
		}
	}

	private void assertAssignCopyGlobalVarToMessageCorrelationPart(Activity assignAct,
			Process sourceProcess) {

		List<Copy> cpList = ((Assign) assignAct).getCopy();
		Copy cpCorrel = null;
		for (Copy cp : cpList) {
			From from = cp.getFrom();
			if (from.getVariable() != null
					&& from.getVariable().getName()
							.equals(BPEL4ChorModelConstants.VARIABLE_FOR_CORRELATION_NAME)) {
				cpCorrel = cp;
				break;
			}
		}
		Assert.assertNotNull(cpCorrel);
		To to = cpCorrel.getTo();
		Part correlpart = to.getPart();
		Assert.assertEquals(correlpart, ((Message) to.getVariable().getMessageType())
				.getPart(BPEL4ChorModelConstants.CORRELATION_PART_NAME));

	}

	
	public void testCreateReceivingBlock() throws AmbiguousPropertyForLinkException, WSDLException,
			IOException {
		blockBuilder.createReceivingBlock();
		MyBlockCreator mybc = (MyBlockCreator) blockBuilder;

		// MyWSDLUtil.print(targetDefn);

		// start with the link in the target activity
		Link linkInTargetProcess = MyBPELUtils.findLinkInActivityTarget(
				linkInSourceProcess.getName(), targetProcess);

		// assertion: the 'receive' should be in the sequence that is combined
		// to the link by now
		Source source = linkInTargetProcess.getSources().get(0);
		Activity sequence = source.getActivity();
		assertNotNull(sequence);
		assertTrue(sequence instanceof Sequence);

		// assert the first activity in the sequence is the 'receive'
		Activity receive = ((Sequence) sequence).getActivities().get(0);
		assertTrue(receive instanceof Receive);

		// assert the second activity in the sequence is the 'assign'
		Activity assign = ((Sequence) sequence).getActivities().get(1);
		assertAssignCopy4GlobalVar((Assign) assign);

		// assertion: transitionCondition
		Variable receiveVar = ((Receive) receive).getVariable();
		assertTrue(receiveVar.getMessageType().equals(mybc.getCtrlLinkMessage()));
		assertTrue(source.getTransitionCondition().getBody()
				.equals("$" + receiveVar.getName() + ".status)"));

		// assert correlation for receive is created
		Correlations correlations = ((Receive) receive).getCorrelations();
		assertNotNull(correlations);
		CorrelationSet correlSet = correlations.getChildren().get(0).getSet();
		assertTrue(correlSet.getName().equals(BPEL4ChorModelConstants.CORRELATION_SET_NAME));

		Property actualProperty = correlSet.getProperties().get(0);
		Property expectProperty = MyWSDLUtil.findProperty(targetDefn,
				BPEL4ChorModelConstants.CORRELATION_PROPERTY_NAME);
		assertEqualProperty(expectProperty, actualProperty);

		// assert the propertyAlias for the received message is created in the
		QName propertyQName = new QName(targetDefn.getTargetNamespace(),
				BPEL4ChorModelConstants.CORRELATION_PROPERTY_NAME);
		QName messageQName = new QName(targetDefn.getTargetNamespace(), receiveVar.getMessageType()
				.getQName().getLocalPart());
		PropertyAlias expectedAlias = MyWSDLUtil.findPropertyAlias(targetDefn, propertyQName,
				messageQName, BPEL4ChorModelConstants.CORRELATION_PART_NAME);
		assertNotNull(expectedAlias);

		// get topology
		Topology topology = blockBuilder.getTopology();

		// assertion: 'selects' in the participant is updated
		Participant p1 = BPEL4ChorUtil.resolveParticipant(topology, "participant1");
		assertNotNull(p1);
		assertTrue(p1.getSelects().contains("participant2"));

		// assertion: the messageLinks for topology are updated, 'receiver' and
		// 'receiveActivity' are changed.

		List<MessageLink> msgLinks = topology.getMessageLinks();
		assertTrue(msgLinks.size() == 2);

		for (MessageLink msgLink : msgLinks) {
			assertTrue(msgLink.getReceiver().equals(targetProcess.getName()));
			assertTrue(msgLink.getReceiveActivity() != null);
		}

		try {
			Grounding grounding = blockBuilder.getGrounding();
			BPEL4ChorWriter.printTopology(topology);
			BPEL4ChorWriter.printGrounding(grounding);
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}

	}

	private void assertAssignCopy4GlobalVar(Assign assign) {
		assertNotNull(assign);
		assertTrue(assign.getCopy().size() == 1);
		Copy copy = assign.getCopy().get(0);
		assertTrue(copy.getFrom().getProperty() != null);
		assertTrue(copy.getTo().getVariable().getName()
				.equals(BPEL4ChorModelConstants.VARIABLE_FOR_CORRELATION_NAME));
	}

	private void assertEqualProperty(Property expectProperty, Property actualProperty) {
		assertEquals(expectProperty.getName(), actualProperty.getName());
		assertEquals(expectProperty.getType(), actualProperty.getType());
	}
}

class MyBlockCreator extends ControlLinkBlockBuilder {

	public MyBlockCreator(Process targetProcess, Process sourceProcess, Process nonSplitProcess,
			Definition targetDefn, Link linkInSource, Topology topology, Grounding grounding) {
		super(targetProcess, sourceProcess, nonSplitProcess, targetDefn, linkInSource, topology,
				grounding);
	}

	public Process getTargetProcess() {
		return this.targetProcess;
	}

	public Process getSourceProcess() {
		return this.sourceProcess;
	}

	public Message getCtrlLinkMessage() {
		return this.ctrlLinkMessage;
	}

	public Variable getVarTrueAndCorrel() {
		return this.varTrueAndCorrel;
	}

	public Variable getVarFalseAndCorrel() {
		return this.varFalseAndCorrel;
	}

	public Variable getVarReceive() {
		return this.varReceive;
	}

	public Operation getOperation() {
		return this.operation;
	}

	public PortType getPortType() {
		return this.portType;
	}

	public Role getRole() {
		return this.role;
	}

	public PartnerLinkType getPartnerLinkType() {
		return this.plt;
	}

	public PartnerLink getTargetPartnerLink() {
		return this.targetPartnerLink;
	}

	public PartnerLink getSourcePartnerLink() {
		return this.sourcePartnerLink;
	}

	public Topology getTopology() {
		return this.topology;
	}

	public Grounding getGrounding() {
		return this.grounding;
	}
}
