package org.bpel4chor.splitprocess.test.fragmentation;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.wsdl.WSDLException;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import junit.framework.Assert;

import de.uni_stuttgart.iaas.bpel.model.utilities.MyBPELUtils;
import de.uni_stuttgart.iaas.bpel.model.utilities.MyWSDLUtil;
import de.uni_stuttgart.iaas.bpel.model.utilities.exceptions.AmbiguousPropertyForLinkException;

import org.apache.log4j.Logger;
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
import org.bpel4chor.splitprocess.utils.SplitProcessConstants;
import org.bpel4chor.splitprocess.utils.VariableResolver;
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
import org.eclipse.bpel.model.Target;
import org.eclipse.bpel.model.To;
import org.eclipse.bpel.model.Variable;
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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.uni_stuttgart.iaas.bpel_d.algorithm.analysis.AnalysisResult;
/**
 * Test for DataDependencyFragmenter - case different partition, single query set
 * 
 * @since Jul 03, 2012
 * @author Daojun Cui
 *
 */
public class DataDependencyFragmenterTestCase2 {

	static File testFileDir = null;

	static Process process = null;

	static Definition definition = null;

	static RuntimeData data = null;

	static PartitionSpecification partitionSpec = null;

	static AnalysisResult analysis = null;

	static Logger log = Logger.getLogger(DataDependencyFragmenterTestCase2.class);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		//
		// use the process "OrderingProcess" in Project OrderInfo4DDTestCase2
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
				+ "\\OrderInfo4DDTestCase2\\bpelContent\\OrderingProcess.bpel";
		process = loadBPEL(bpelURI);

		// load wsdl
		definition = MyWSDLUtil.readWSDL(testFileDir.getAbsolutePath()
				+ "\\OrderInfo4DDTestCase2\\bpelContent\\OrderingProcess.wsdl");

		// partition specification
		String partitionURI = testFileDir.getAbsolutePath()
				+ "\\OrderInfo4DDTestCase2\\bpelContent\\Partition.xml";
		partitionSpec = loadPartitionSpec(partitionURI, process);

		// analyse process
		analysis = DataFlowAnalyzer.analyze(process);

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

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		// runtime data
		data = new RuntimeData(process, partitionSpec, definition);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testSplitDataDependencyNonFullProcedure() throws PartitionSpecificationException,
			SplitControlLinkException, SplitDataDependencyException {

		// fragment process
		ProcessFragmenter procFragmenter = new ProcessFragmenter(data);
		procFragmenter.fragmentizeProcess();

		// split the control link
		ControlLinkFragmenter ctrlLinkFragmenter = new ControlLinkFragmenter(data);
		ctrlLinkFragmenter.splitControlLink();

		// split the data dependency
		// the scenario act = H, var = response
		// the partition is p1=(x, {A, B, H}), p2=(y, {C, D})
		// PWDG(H, response) = {
		// V={n1=(x,{B}), n2=(y,{C, D})}
		// E={(n1,n2)}
		// }

		// Note that node n1 is in the same partition with the reader H.
		//

		// split data dependency using PWDG based on the reader=H, var=response
		DataDependencyFragmenterScenario2 ddFragmenter = new DataDependencyFragmenterScenario2(data);
		ddFragmenter.splitDataDependency();

		// assert local resolver,
		// the node n2 is in different partition as the reader, the single query
		// set is {.text}, therefore for n2 a sending block is created.
		PWDG pwdg = ddFragmenter.getPWDG();
		Set<PWDGNode> nodeSet = pwdg.getNodeSet("y");
		PWDGNode n2 = nodeSet.iterator().next();
		assertLocalResolver4N2(n2, ddFragmenter.getParticipant2FragProcMap(),
				ddFragmenter.getNode2NameMap());

		// assert receiving flow
		Process readerProcess = ddFragmenter.getParticipant2FragProcMap().get("x");
		assertReceivingFlow4N2("responseRFFlow", readerProcess, pwdg,
				ddFragmenter.getNode2NameMap());

		// assert topology/grounding message link y -> x
		assertMessageLinkY2X(ddFragmenter, "y");
	}

	private void assertMessageLinkY2X(DataDependencyFragmenterScenario2 ddFragmenter, String sender) {

		// topology message links
		PWDG pwdg = ddFragmenter.getPWDG();
		PWDGNode node = pwdg.getNodeSet(sender).iterator().next();
		String sendAct = "Sendresponse" + ddFragmenter.id(node);
		org.bpel4chor.model.topology.impl.MessageLink topoMsgLink = BPEL4ChorUtil
				.resolveTopologyMessageLinkBySendAct(data.getTopology(), sendAct);
		Assert.assertNotNull(topoMsgLink);
		Assert.assertTrue(topoMsgLink.getSender().equals(sender));
		Assert.assertTrue(topoMsgLink.getReceiver().equals("x"));
		Assert.assertTrue(topoMsgLink.getReceiveActivity()
				.equals("Receive" + ddFragmenter.id(node)));

		// grounding message link
		Grounding grounding = data.getGrounding();
		org.bpel4chor.model.grounding.impl.MessageLink groundMsgLink = BPEL4ChorUtil
				.resolveGroundingMessageLinkByName(grounding, topoMsgLink.getName());

		Definition defn4w = data.getFragmentDefinition("x");

		QName porTypeQName = groundMsgLink.getPortType();
		PortType portType = MyWSDLUtil.resolvePortType(defn4w, porTypeQName);
		assertNotNull(portType);
		String opName = groundMsgLink.getOperation();
		Operation op = MyWSDLUtil.resolveOperation(defn4w, porTypeQName, opName);
		assertNotNull(op);

	}

	@Test
	public void testSplitDataDependencyFullProcedure() throws PartitionSpecificationException,
			SplitControlLinkException, SplitDataDependencyException, IOException, WSDLException, AmbiguousPropertyForLinkException {
		
		// Fragment process
		ProcessFragmenter procFragmenter = new ProcessFragmenter(data);
		procFragmenter.fragmentizeProcess();

		// Split the control link
		ControlLinkFragmenter ctrlLinkFragmenter = new ControlLinkFragmenter(data);
		ctrlLinkFragmenter.splitControlLink();

		// Split data dependency
		DataDependencyFragmenter ddFragmenter = new DataDependencyFragmenter(data);
		ddFragmenter.splitDataDependency();

		
		assertReceiveAIsWrappedBySequence("x");
	}
	
	private void assertReceiveAIsWrappedBySequence(String participat) throws AmbiguousPropertyForLinkException {
		
		Process fragProcess = data.getParticipant2FragProcMap().get(participat);
		
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

	private void assertReceivingFlow4N2(String flowName, Process readerProcess, PWDG pwdg,
			Map<PWDGNode, String> idn) {
		// receiving flow
		Activity receivingFlow = MyBPELUtils.resolveActivity(flowName, readerProcess);
		Assert.assertNotNull(receivingFlow);

		// assert the tmpVar name : 'tmp'+varName+'4'+readerActName
		String tmpVarName = "tmpresponse4H";
		Variable tmpVar = MyBPELUtils.resolveVariable(tmpVarName, readerProcess);
		Assert.assertNotNull(tmpVar);
		Message msg4TmpVar = tmpVar.getMessageType();

		Variable var = MyBPELUtils.resolveVariable("response", readerProcess);
		Assert.assertNotNull(var);
		Message msg4Var = var.getMessageType();

		Assert.assertEquals(msg4TmpVar, msg4Var);

		//
		// the receiving flow contains for node n1 an assign activity, for n2 an
		// receive activity and an assign activity
		//

		// for node n1=(x, {B})
		Set<PWDGNode> node1Set = pwdg.getNodeSet("x");
		PWDGNode n1 = node1Set.iterator().next();

		String idStr4N1 = idn.get(n1);
		String assign4Node1Name = "Assign" + idStr4N1;
		Activity assign4Node1 = MyBPELUtils.resolveActivity(assign4Node1Name, readerProcess);
		Assert.assertNotNull(assign4Node1);
		Assert.assertTrue(assign4Node1 instanceof Assign);

		// for node n2=(y, {C, D})
		Set<PWDGNode> node2Set = pwdg.getNodeSet("y");
		PWDGNode n2 = node2Set.iterator().next();

		String idStr4N2 = idn.get(n2);

		String receive4Node2Name = "Receive" + idStr4N2;

		Activity receive4Node2 = MyBPELUtils.resolveActivity(receive4Node2Name, readerProcess);
		Assert.assertNotNull(receive4Node2);
		Assert.assertTrue(receive4Node2 instanceof Receive);
		
		assertSequence4ReceiveAndAssign((Receive)receive4Node2, receivingFlow);

		assertCorrelationExist((Receive)receive4Node2);
		
		String assign4Node2Name = "Assign" + idStr4N2;
		Activity assign4Node2 = MyBPELUtils.resolveActivity(assign4Node2Name, readerProcess);
		Assert.assertTrue(receivingFlow.equals(assign4Node2.eContainer()));
		
		Sequence sequence = (Sequence) receive4Node2.eContainer();
		String sequenceName = sequence.getName();

		assertExistLink((Flow) receivingFlow, sequenceName + "2" + assign4Node2Name,
				(Receive) receive4Node2);

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
		Assert.assertNotNull(correl.getSet().getProperties().size()==1);
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

	private void assertLocalResolver4N2(PWDGNode n2, Map<String, Process> participant2FragProcMap,
			Map<PWDGNode, String> node2NameMap) {

		// get the fragment process
		Process proc = participant2FragProcMap.get(n2.getParticipant());

		String nodeId = node2NameMap.get(n2);

		// assert the scope for sending block is there with the name
		// "LR"+varName+id(node)+"Scope"
		String scopeName = "LRresponse" + nodeId + "Scope";

		Activity scope = MyBPELUtils.resolveActivity(scopeName, proc);
		Assert.assertNotNull(scope);
		Assert.assertTrue(scope instanceof Scope);

		// the C and D are linked to this scope
		assertLinkBetweenActivityAndScope("C", (Scope) scope, proc);
		assertLinkBetweenActivityAndScope("D", (Scope) scope, proc);

		// the scope contains a sequence as activity
		Activity sequenceTrue = ((Scope) scope).getActivity();
		Assert.assertNotNull(sequenceTrue);
		Assert.assertTrue(sequenceTrue instanceof Sequence);

		List<Activity> acts4True = ((Sequence) sequenceTrue).getActivities();
		Assert.assertEquals(2, acts4True.size());

		Activity assign4True = acts4True.get(0);
		Assert.assertTrue(assign4True instanceof Assign);
		
		// assert the assign activity is as expected
		assertAssignWithCopy4StatusDataAndCorrelation((Assign)assign4True);

		Activity invoke4True = acts4True.get(1);
		Assert.assertTrue(invoke4True instanceof Invoke);
		Assert.assertTrue(invoke4True.getSuppressJoinFailure() == false);

		assertVariable4InvokeTrueExist((Invoke) invoke4True, node2NameMap.get(n2), proc);

		// the scope contains a sequence for catch exception
		FaultHandler fh = ((Scope) scope).getFaultHandlers();
		Catch catch4FH = fh.getCatch().get(0);
		Activity seq4False = catch4FH.getActivity();
		Assert.assertTrue(seq4False instanceof Sequence);

		List<Activity> acts4False = ((Sequence) seq4False).getActivities();
		Assert.assertEquals(2, acts4False.size());

		Activity assign4False = acts4False.get(0);
		Assert.assertTrue(assign4False instanceof Assign);
		
		// assert the assign activity is as expected
		assertAssignWithCopy4StatusAndCorrelation((Assign)assign4False);

		Activity invoke4False = acts4False.get(1);
		Assert.assertTrue(invoke4False instanceof Invoke);
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
				// copy from expression "false()" to status part
				Assert.assertTrue(from.getExpression().getBody().equals("false()"));
				Assert.assertNotNull(to.getVariable().getMessageType().getPart("status"));
			}
			if (from.getVariable() != null) {
				// copy from global variable to the correlation part in the message
				Variable fromVar = from.getVariable();
				Assert.assertTrue(fromVar.getName().equals(
						SplitProcessConstants.VARIABLE_FOR_CORRELATION_NAME));
				Assert.assertTrue(to.getVariable() != null
						&& to.getPart().getName()
								.equals(SplitProcessConstants.CORRELATION_PART_NAME));

			}
		}
		
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
				// copy from expression "true()" to the status part
				Assert.assertTrue(from.getExpression().getBody().equals("true()"));
				Assert.assertNotNull(to.getVariable().getMessageType().getPart("status"));
			}
			if (from.getVariable() != null) {
				Variable fromVar = from.getVariable();
				if (fromVar.getName().equals(SplitProcessConstants.VARIABLE_FOR_CORRELATION_NAME)) {
					// copy from the global variable to correlation part in the message
					Assert.assertTrue(to.getVariable() != null
							&& to.getPart().getName()
									.equals(SplitProcessConstants.CORRELATION_PART_NAME));
				} else {
					Assert.assertTrue(to.getPart().getName().equals("data"));
				}

			}
		}
		
	}

	private void assertVariable4InvokeTrueExist(Invoke invoke4True, String nodeId, Process proc) {
		Variable var4InProc = MyBPELUtils.resolveVariable(nodeId, proc);
		Assert.assertNotNull(var4InProc);

		Variable var4InvokeTrue = invoke4True.getInputVariable();

		Assert.assertEquals(var4InProc, var4InvokeTrue);

		Message msg = var4InProc.getMessageType();
		Part statusPart = (Part) msg.getPart("status");
		Part dataPart = (Part) msg.getPart("data");

		Assert.assertNotNull(statusPart);
		Assert.assertNotNull(dataPart);
	}

	private void assertLinkBetweenActivityAndScope(String actName, Scope sendingBlock, Process proc) {

		Activity act = MyBPELUtils.resolveActivity(actName, proc);

		Flow flow = (Flow) proc.getActivity();

		String linkName = act.getName() + "2" + sendingBlock.getName();
		Link linkAct2Scope = MyBPELUtils.resolveLink(flow, linkName);

		Source source = linkAct2Scope.getSources().iterator().next();
		Activity sourceAct = source.getActivity();
		Assert.assertEquals(sourceAct, act);

		Target target = linkAct2Scope.getTargets().iterator().next();
		Activity targetAct = target.getActivity();
		Assert.assertEquals(targetAct, sendingBlock);

	}

}

class DataDependencyFragmenterScenario2 extends DataDependencyFragmenter {

	protected PWDG pwdg = null;

	public DataDependencyFragmenterScenario2(RuntimeData data) {
		super(data);
	}

	/**
	 * Only fragment the data dependency based on the given activity H and
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

			// get the activity H
			List<Activity> activities = new ArrayList<Activity>();
			for (Participant p : this.partitionSpec.getParticipants()) {
				activities.addAll(p.getActivities());
			}
			sortActivities(activities);
			Activity act = getActivity("H", activities);

			// get the variable response
			VariableResolver resolver = new VariableResolver(this.nonSplitProcess);
			List<Variable> variables = resolver.resolveReadVariable(act);
			sortVariables(variables);
			Variable var = getVariable("response", variables);

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