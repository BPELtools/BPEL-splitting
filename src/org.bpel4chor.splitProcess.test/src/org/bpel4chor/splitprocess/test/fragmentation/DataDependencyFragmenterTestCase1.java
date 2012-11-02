package org.bpel4chor.splitprocess.test.fragmentation;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import junit.framework.Assert;

import org.bpel4chor.model.grounding.impl.Grounding;
import org.bpel4chor.splitprocess.RuntimeData;
import org.bpel4chor.splitprocess.dataflowanalysis.AnalysisResultParser;
import org.bpel4chor.splitprocess.dataflowanalysis.DataFlowAnalyzer;
import org.bpel4chor.splitprocess.dataflowanalysis.QueryWriterSet;
import org.bpel4chor.splitprocess.exceptions.DataFlowAnalysisException;
import org.bpel4chor.splitprocess.exceptions.PartitionSpecificationException;
import org.bpel4chor.splitprocess.exceptions.SplitControlLinkException;
import org.bpel4chor.splitprocess.exceptions.SplitDataDependencyException;
import org.bpel4chor.splitprocess.fragmentation.ControlLinkFragmenter;
import org.bpel4chor.splitprocess.fragmentation.DataDependencyFragmenter;
import org.bpel4chor.splitprocess.fragmentation.DataDependencyHelper;
import org.bpel4chor.splitprocess.fragmentation.ProcessFragmenter;
import org.bpel4chor.splitprocess.partition.model.Participant;
import org.bpel4chor.splitprocess.partition.model.PartitionSpecification;
import org.bpel4chor.splitprocess.partition.representation.PartitionSpecReader;
import org.bpel4chor.splitprocess.pwdg.model.PWDG;
import org.bpel4chor.splitprocess.pwdg.model.PWDGNode;
import org.bpel4chor.splitprocess.pwdg.model.WDG;
import org.bpel4chor.splitprocess.pwdg.util.PWDGFactory;
import org.bpel4chor.splitprocess.pwdg.util.WDGFactory;
import org.bpel4chor.splitprocess.utils.VariableResolver;
import org.bpel4chor.utils.BPEL4ChorModelConstants;
import org.bpel4chor.utils.BPEL4ChorReader;
import org.bpel4chor.utils.BPEL4ChorUtil;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Assign;
import org.eclipse.bpel.model.BPELPlugin;
import org.eclipse.bpel.model.Catch;
import org.eclipse.bpel.model.Condition;
import org.eclipse.bpel.model.Copy;
import org.eclipse.bpel.model.Correlation;
import org.eclipse.bpel.model.FaultHandler;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.From;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Receive;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Sequence;
import org.eclipse.bpel.model.Source;
import org.eclipse.bpel.model.To;
import org.eclipse.bpel.model.Variable;
import org.eclipse.bpel.model.resource.BPELResourceFactoryImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.wst.wsdl.Definition;
import org.eclipse.wst.wsdl.Operation;
import org.eclipse.wst.wsdl.PortType;
import org.eclipse.wst.wsdl.internal.util.WSDLResourceFactoryImpl;
import org.eclipse.xsd.util.XSDResourceFactoryImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.uni_stuttgart.iaas.bpel.model.utilities.MyBPELUtils;
import de.uni_stuttgart.iaas.bpel.model.utilities.MyWSDLUtil;
import de.uni_stuttgart.iaas.bpel.model.utilities.exceptions.AmbiguousPropertyForLinkException;
import de.uni_stuttgart.iaas.bpel_d.algorithm.analysis.AnalysisResult;
/**
 * Test for DataDependencyFragmenter - case different partition, single query set
 * 
 * @since Jul 03, 2012
 * @author Daojun Cui
 *
 */
public class DataDependencyFragmenterTestCase1 {

	static File testFileDir = null;

	static Process process = null;

	static Definition definition = null;

	static RuntimeData data = null;

	static PartitionSpecification partitionSpec = null;

	static AnalysisResult analysis = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		//
		// use the process "OrderingProcess" in Project OrderInfo4DDTestCase1
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
		String bpelURI = testFileDir.getAbsolutePath()
				+ "\\OrderInfo4DDTestCase1\\bpelContent\\OrderingProcess.bpel";
		process = loadBPEL(bpelURI);

		// load wsdl
		definition = MyWSDLUtil.readWSDL(testFileDir.getAbsolutePath()
				+ "\\OrderInfo4DDTestCase1\\bpelContent\\OrderingProcess.wsdl");

		// partition specification
		String partitionURI = testFileDir.getAbsolutePath()
				+ "\\OrderInfo4DDTestCase1\\bpelContent\\Partition.xml";
		partitionSpec = loadPartitionSpec(partitionURI, process);

		// analyse process
		analysis = DataFlowAnalyzer.analyze(process);

		// runtime data
		data = new RuntimeData(process, partitionSpec, definition);

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

	protected static Process loadBPEL(String strURI) {
		ResourceSet resourceSet = new ResourceSetImpl();
		URI uri = URI.createFileURI(strURI);
		Resource resource = resourceSet.getResource(uri, true);
		return (Process) resource.getContents().get(0);
	}

	protected static PartitionSpecification loadPartitionSpec(String strURI, Process process)
			throws JAXBException, FileNotFoundException, PartitionSpecificationException {
		FileInputStream inputStream = new FileInputStream(new File(strURI));
		PartitionSpecReader reader = new PartitionSpecReader();
		return reader.readSpecification(inputStream, process);
	}

	@Test
	public void testSplitDataDependencyNonFullProcedure() throws PartitionSpecificationException,
			SplitControlLinkException, SplitDataDependencyException, AmbiguousPropertyForLinkException {

		// fragment process
		ProcessFragmenter procFragmenter = new ProcessFragmenter(data);
		procFragmenter.fragmentizeProcess();

		// split the control link
		ControlLinkFragmenter ctrlLinkFragmenter = new ControlLinkFragmenter(data);
		ctrlLinkFragmenter.splitControlLink();

		// split the data dependency
		// the scenario act = G, var = paymentInfo
		// the partition is p1=(w, {G}), p2=(x, {A, B}), p3=(y, {C}), p4=(z,{D})
		// the PWDG(G, paymentInfo) => V={n1=(x,{B}), n2=(y,{C}), n3=(z,{D})},
		// E={(n1,n2),(n1, n3)}

		// based on the reader = G, variable = paymentInfo

		DataDependencyFragmenterScenario1 ddFragmenter = new DataDependencyFragmenterScenario1(data);
		ddFragmenter.splitDataDependency();

		// Assert Local Resolver - sending block for B, because the B is
		// different participant as G, there is only single query set
		assertLocalResolver("B", "x", ddFragmenter.getParticipant2FragProcMap(),
				ddFragmenter.getPWDG(), ddFragmenter.getNode2NameMap());

		// Assert Local Resolver - sending block for C, because the C is
		// different participant as G, there is only single query set
		assertLocalResolver("C", "y", ddFragmenter.getParticipant2FragProcMap(),
				ddFragmenter.getPWDG(), ddFragmenter.getNode2NameMap());

		// Assert Local Resolver - sending block for D, because the D is
		// different participant as G, there is only single query set
		assertLocalResolver("D", "z", ddFragmenter.getParticipant2FragProcMap(),
				ddFragmenter.getPWDG(), ddFragmenter.getNode2NameMap());

		// Assert Receiving Flow - 3 Receives and 3 Assigns
		Process readerProcess = ddFragmenter.getParticipant2FragProcMap().get("w");
		assertReceivingFlow("paymentInfoRFFlow", readerProcess, ddFragmenter.getPWDG(),
				ddFragmenter.getNode2NameMap());

		// Assert the initial receive activity is wrapped by a sequence activity now
		assertReceiveAIsWrappedBySequence(ddFragmenter, "x");
		
		// Assert topology/grounding message link x -> w
		assertMessageLinkSender2W(ddFragmenter, "x");

		// Assert topology/grounding message link y -> w
		assertMessageLinkSender2W(ddFragmenter, "y");

		// Assert topology/grounding message link z -> w
		assertMessageLinkSender2W(ddFragmenter, "z");
		

	}

	private void assertReceiveAIsWrappedBySequence(DataDependencyFragmenterScenario1 ddFragmenter,
			String participat) throws AmbiguousPropertyForLinkException {
		
		Process fragProcess = ddFragmenter.getParticipant2FragProcMap().get(participat);
		
		// Assert the receive activity 'A' is now in a sequence activity
		Sequence sequence = (Sequence) MyBPELUtils.resolveActivity("InitialReceiveSequence", fragProcess);
		Receive receiveA = (Receive) MyBPELUtils.resolveActivity("A", fragProcess);
		
		Assert.assertTrue(sequence.getActivities().contains(receiveA));
		
		// the sequence contains a 'assign' activity
		Assert.assertTrue(sequence.getActivities().get(1) instanceof Assign);
		
		// Find the link 'linkA2B'
		Link link = MyBPELUtils.findLinkInActivitySource("linkA2B", fragProcess);
		Assert.assertTrue(link.getSources().get(0).getActivity().equals(sequence));
	}

	private void assertMessageLinkSender2W(DataDependencyFragmenterScenario1 ddFragmenter,
			String sender) {

		// topology message links
		PWDG pwdg = ddFragmenter.getPWDG();
		PWDGNode node = pwdg.getNodeSet(sender).iterator().next();
		String sendAct = "SendpaymentInfo" + ddFragmenter.id(node);
		org.bpel4chor.model.topology.impl.MessageLink topoMsgLink = BPEL4ChorUtil
				.resolveTopologyMessageLinkBySendAct(data.getTopology(), sendAct);
		Assert.assertNotNull(topoMsgLink);
		Assert.assertTrue(topoMsgLink.getSender().equals(sender));
		Assert.assertTrue(topoMsgLink.getReceiver().equals("w"));
		Assert.assertTrue(topoMsgLink.getReceiveActivity()
				.equals("Receive" + ddFragmenter.id(node)));

		// grounding message link
		Grounding grounding = data.getGrounding();
		org.bpel4chor.model.grounding.impl.MessageLink groundMsgLink = BPEL4ChorUtil
				.resolveGroundingMessageLinkByName(grounding, topoMsgLink.getName());

		Definition defn4w = data.getFragmentDefinition("w");

		QName porTypeQName = groundMsgLink.getPortType();
		PortType portType = MyWSDLUtil.resolvePortType(defn4w, porTypeQName);
		assertNotNull(portType);
		String opName = groundMsgLink.getOperation();
		Operation op = MyWSDLUtil.resolveOperation(defn4w, porTypeQName, opName);
		assertNotNull(op);

	}

	private void assertLocalResolver(String writerName, String participantName,
			Map<String, Process> participant2Proc, PWDG pwdg, Map<PWDGNode, String> idn) {

		// get the invoking fragment process
		Process proc = participant2Proc.get(participantName);

		// get the pwdg node
		Set<PWDGNode> nodeSet = pwdg.getNodeSet(participantName);
		PWDGNode node = nodeSet.iterator().next();

		// get the writer activity in the fragment process
		Activity nodeAct = MyBPELUtils.resolveActivity(writerName, proc);

		// assert the scope for sending block is there with the name
		// "LR"+varName+id(node)+"Scope"
		String scopeName = "LRpaymentInfo" + idn.get(node) + "Scope";

		Activity scope = MyBPELUtils.resolveActivity(scopeName, proc);
		Assert.assertNotNull(scope);
		Assert.assertTrue(scope instanceof Scope);

		// the scope contains a sequence as activity
		Activity sequenceTrue = ((Scope) scope).getActivity();
		Assert.assertNotNull(sequenceTrue);
		Assert.assertTrue(sequenceTrue instanceof Sequence);

		List<Activity> acts4True = ((Sequence) sequenceTrue).getActivities();
		Assert.assertEquals(2, acts4True.size());

		Activity assign4True = acts4True.get(0);
		Assert.assertTrue(assign4True instanceof Assign);

		assertAssignWithCopy4StatusDataAndCorrelation((Assign) assign4True);

		Activity invoke4True = acts4True.get(1);
		Assert.assertTrue(invoke4True instanceof Invoke);
		Assert.assertTrue(invoke4True.getSuppressJoinFailure() == false);

		// the scope contains a sequence for catch exception
		FaultHandler fh = ((Scope) scope).getFaultHandlers();
		Catch catch4FH = fh.getCatch().get(0);
		Activity seq4False = catch4FH.getActivity();
		Assert.assertTrue(seq4False instanceof Sequence);

		List<Activity> acts4False = ((Sequence) seq4False).getActivities();
		Assert.assertEquals(2, acts4False.size());

		Activity assign4False = acts4False.get(0);
		Assert.assertTrue(assign4False instanceof Assign);

		assertAssignWithCopy4StatusAndCorrelation((Assign) assign4False);

		Activity invoke4False = acts4False.get(1);
		Assert.assertTrue(invoke4False instanceof Invoke);

	}

	private void assertAssignWithCopy4StatusDataAndCorrelation(Assign assign4True) {
		List<Copy> cpList = assign4True.getCopy();
		Assert.assertNotNull(cpList);
		Assert.assertTrue(cpList.size() == 3);
		for (Copy cp : cpList) {
			From from = cp.getFrom();
			To to = cp.getTo();
			Assert.assertNotNull(from);
			Assert.assertNotNull(to);
			if (from.getExpression() != null) {
				Assert.assertTrue(from.getExpression().getBody().equals("true()"));
				Assert.assertNotNull(to.getVariable().getMessageType().getPart("status"));
			}
			if (from.getVariable() != null) {
				Variable fromVar = from.getVariable();
				if (fromVar.getName().equals(BPEL4ChorModelConstants.VARIABLE_FOR_CORRELATION_NAME)) {
					Assert.assertTrue(to.getVariable() != null
							&& to.getPart().getName()
									.equals(BPEL4ChorModelConstants.CORRELATION_PART_NAME));
				} else {
					Assert.assertTrue(to.getPart().getName().equals("data"));
				}

			}
		}
	}

	private void assertAssignWithCopy4StatusAndCorrelation(Assign assign4False) {

		List<Copy> cpList = assign4False.getCopy();
		Assert.assertNotNull(cpList);
		Assert.assertTrue(cpList.size() == 2);
		for (Copy cp : cpList) {
			From from = cp.getFrom();
			To to = cp.getTo();
			Assert.assertNotNull(from);
			Assert.assertNotNull(to);
			if (from.getExpression() != null) {
				Assert.assertTrue(from.getExpression().getBody().equals("false()"));
				Assert.assertNotNull(to.getVariable().getMessageType().getPart("status"));
			}
			if (from.getVariable() != null) {
				Variable fromVar = from.getVariable();
				Assert.assertTrue(fromVar.getName().equals(
						BPEL4ChorModelConstants.VARIABLE_FOR_CORRELATION_NAME));
				Assert.assertTrue(to.getVariable() != null
						&& to.getPart().getName()
								.equals(BPEL4ChorModelConstants.CORRELATION_PART_NAME));

			}
		}
	}

	private void assertReceivingFlow(String flowName, Process readerProcess, PWDG pwdg,
			Map<PWDGNode, String> idn) {
		// receiving flow
		Activity receivingFlow = MyBPELUtils.resolveActivity(flowName, readerProcess);
		Assert.assertNotNull(receivingFlow);

		//
		// the flow contains for each node an receive activity and an assign
		// activity
		//

		// for node (x, {B})
		Set<PWDGNode> nodeSet = pwdg.getNodeSet("x");
		PWDGNode node4B = nodeSet.iterator().next();

		String idStr4NodeB = idn.get(node4B);

		String receive4NodeBName = "Receive" + idStr4NodeB;

		Activity receive4NodeB = MyBPELUtils.resolveActivity(receive4NodeBName, readerProcess);
		Assert.assertNotNull(receive4NodeB);
		Assert.assertTrue(receive4NodeB instanceof Receive);

		// the sequence with the receive and assign
		assertSequence4ReceiveAndAssign((Receive)receive4NodeB, receivingFlow);
		
		// the correlation exists in the receive
		assertCorrelationExist((Receive) receive4NodeB);

		String assign4NodeBName = "Assign" + idStr4NodeB;
		Activity assign4NodeB = MyBPELUtils.resolveActivity(assign4NodeBName, readerProcess);
		Assert.assertTrue(receivingFlow.equals(assign4NodeB.eContainer()));

		Sequence sequence4B = (Sequence) receive4NodeB.eContainer();
		String sequence4BName = sequence4B.getName();
		assertExistLink((Flow) receivingFlow, sequence4BName + "2" + assign4NodeBName,
				(Receive) receive4NodeB);

		// for node (y, {C})
		nodeSet = pwdg.getNodeSet("y");
		PWDGNode node4C = nodeSet.iterator().next();

		String idStr4NodeC = idn.get(node4C);

		String receive4NodeCName = "Receive" + idStr4NodeC;

		Activity receive4NodeC = MyBPELUtils.resolveActivity(receive4NodeCName, readerProcess);
		Assert.assertNotNull(receive4NodeC);
		Assert.assertTrue(receive4NodeC instanceof Receive);
		
		// the sequence with the receive and assign
		assertSequence4ReceiveAndAssign((Receive)receive4NodeC, receivingFlow);
		
		// the correlation exists in the receive
		assertCorrelationExist((Receive) receive4NodeC);

		String assign4NodeCName = "Assign" + idStr4NodeC;
		Activity assign4NodeC = MyBPELUtils.resolveActivity(assign4NodeCName, readerProcess);
		Assert.assertTrue(receivingFlow.equals(assign4NodeC.eContainer()));

		Sequence sequence4C = (Sequence) receive4NodeC.eContainer();
		String sequence4CName = sequence4C.getName();
		assertExistLink((Flow) receivingFlow, sequence4CName + "2" + assign4NodeCName,
				(Receive) receive4NodeC);

		// for node (z, {D})
		nodeSet = pwdg.getNodeSet("z");
		PWDGNode node4D = nodeSet.iterator().next();

		String idStr4NodeD = idn.get(node4D);

		String receive4NodeDName = "Receive" + idStr4NodeD;

		Activity receive4NodeD = MyBPELUtils.resolveActivity(receive4NodeDName, readerProcess);
		Assert.assertNotNull(receive4NodeD);
		Assert.assertTrue(receive4NodeD instanceof Receive);
		
		// the sequence with the receive and assign
		assertSequence4ReceiveAndAssign((Receive)receive4NodeD, receivingFlow);

		// the correlation exists in the receive
		assertCorrelationExist((Receive) receive4NodeD);

		String assign4NodeDName = "Assign" + idStr4NodeD;
		Activity assign4NodeD = MyBPELUtils.resolveActivity(assign4NodeDName, readerProcess);
		Assert.assertTrue(receivingFlow.equals(assign4NodeD.eContainer()));

		Sequence sequence4D = (Sequence) receive4NodeD.eContainer();
		String sequence4DName = sequence4D.getName();
		assertExistLink((Flow) receivingFlow, sequence4DName + "2" + assign4NodeDName,
				(Receive) receive4NodeD);

	}

	private void assertSequence4ReceiveAndAssign(Receive receive4Node, Activity receivingFlow) {
		Activity sequence = (Activity) receive4Node.eContainer();
		Assert.assertNotNull(sequence);
		Assert.assertTrue(((Sequence) sequence).getActivities().get(1) instanceof Assign);
		Assert.assertTrue(receivingFlow.equals(sequence.eContainer()));
	}

	private void assertCorrelationExist(Receive receive) {

		Correlation correl = receive.getCorrelations().getChildren().get(0);
		Assert.assertNotNull(correl);
		Assert.assertTrue(correl.getInitiate().equals("join"));
		Assert.assertNotNull(correl.getSet());
		Assert.assertNotNull(correl.getSet().getProperties().size() == 1);
	}

	private void assertExistLink(Flow receivingFlow, String linkName, Receive receive4Node) {

		List<Link> links = receivingFlow.getLinks().getChildren();

		boolean existLink = false;
		Link foundLink = null;
		for (Link link : links) {
			if (link.getName().equals(linkName)) {
				existLink = true;
				foundLink = link;
			}
		}

		Assert.assertTrue(existLink);
		Source source = foundLink.getSources().get(0);
		Condition cond = source.getTransitionCondition();
		Assert.assertTrue(cond.getBody().equals(
				"$" + receive4Node.getVariable().getName() + ".status"));

	}
}

class DataDependencyFragmenterScenario1 extends DataDependencyFragmenter {

	protected PWDG pwdg = null;

	public DataDependencyFragmenterScenario1(RuntimeData data) {
		super(data);
	}

	/**
	 * Only fragment the data dependency based on the given activity G and
	 * variable paymentInfo
	 * 
	 * @throws SplitDataDependencyException
	 * @throws DataFlowAnalysisException
	 */
	@Override
	public void splitDataDependency() throws SplitDataDependencyException {
		try {
			// analyze data flow of the process
			this.analysisRes = DataFlowAnalyzer.analyze(this.nonSplitProcess);

			// get the activity G
			List<Activity> activities = new ArrayList<Activity>();
			for (Participant p : this.partitionSpec.getParticipants()) {
				activities.addAll(p.getActivities());
			}
			sortActivities(activities);
			Activity act = getActivity("G", activities);

			// get the variable paymentInfo
			VariableResolver resolver = new VariableResolver(this.nonSplitProcess);
			List<Variable> variables = resolver.resolveReadVariable(act);
			sortVariables(variables);
			Variable var = getVariable("paymentInfo", variables);

			// prepare the tuple set of Query set and Writer set
			QueryWriterSet qwSet = AnalysisResultParser.parse(act, var, this.analysisRes);
			WDG wdg = WDGFactory.createWDG(qwSet.getAllWriters());
			pwdg = PWDGFactory.createPWDG(wdg, this.nonSplitProcess, this.partitionSpec);

			// initialize the id maps for the given Q_s(act, var)
			initQuerySet2nameMap(var.getName(), qwSet.querySets());
			initNode2NameMap(pwdg);

			// create prerequisite: message, portType, partnerLinkType,
			// partnerLink
			DataDependencyHelper helper = new DataDependencyHelper(participant2FragProc,
					participant2WSDL, partitionSpec, act, var);
			helper.createPrerequisites(pwdg, qwSet, querySet2NameMap, node2NameMap);

			// local resolver
			for (PWDGNode node : pwdg.vertexSet()) {
				// get the Q_s(n, a, x)
				QueryWriterSet qwSet4Node = qwSet.getQueryWriterSetFor(node);
				updateQuerySet2NameMap(qwSet4Node);
				createLocalResolver(node, act, var, qwSet4Node);
			}

			// receiving flow
			createReceivingFlow(act, var, pwdg, qwSet);
			
			// wrap the initial receive
			wrapInitialReceive();

		} catch (Exception e) {
			throw new SplitDataDependencyException(e);
		}

	}

	private Variable getVariable(String varName, List<Variable> variables) {

		for (Variable var : variables) {
			if (var.getName().equals(varName))
				return var;
		}
		return null;
	}

	private Activity getActivity(String actName, List<Activity> activities) {

		for (Activity act : activities) {
			if (act.getName().equals(actName))
				return act;
		}
		return null;
	}

	public String id(Set<String> queryset) {
		return super.id(queryset);
	}

	public String id(PWDGNode node) {
		return super.id(node);
	}

	public void sortActivities(List<Activity> activities) {
		super.sortActivities(activities);
	}

	public void sortVariables(List<Variable> variables) {
		super.sortVariables(variables);
	}

	public void initNode2NameMap(PWDG pwdg) {
		super.initNode2NameMap(pwdg);
	}

	public void initQuerySet2nameMap(String varName, Set<Set<String>> keySet) {
		super.initQuerySet2nameMap(varName, keySet);
	}

	public Map<Set<String>, String> getQuerySet2NameMap() {
		return super.querySet2NameMap;
	}

	public Map<PWDGNode, String> getNode2NameMap() {
		return super.node2NameMap;
	}

	public Map<String, Process> getParticipant2FragProcMap() {
		return this.participant2FragProc;
	}

	public Map<String, Definition> getParticipant2WSDLMap() {
		return this.participant2WSDL;
	}

	public PWDG getPWDG() {
		return this.pwdg;
	}
}