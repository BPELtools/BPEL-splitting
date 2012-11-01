package org.bpel4chor.splitprocess.test.fragmentation;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import de.uni_stuttgart.iaas.bpel.model.utilities.MyBPELUtils;
import de.uni_stuttgart.iaas.bpel.model.utilities.MyWSDLUtil;

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
import org.eclipse.bpel.model.Condition;
import org.eclipse.bpel.model.Copy;
import org.eclipse.bpel.model.Correlation;
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
import org.eclipse.wst.wsdl.Message;
import org.eclipse.wst.wsdl.Operation;
import org.eclipse.wst.wsdl.PortType;
import org.eclipse.wst.wsdl.internal.util.WSDLResourceFactoryImpl;
import org.eclipse.xsd.util.XSDResourceFactoryImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.uni_stuttgart.iaas.bpel_d.algorithm.analysis.AnalysisResult;
/**
 * Test for DataDepenencyFragmenter - Case different Partition, multiple query sets
 * 
 * @since Jul 03, 2012
 * @author Daojun Cui
 */
public class DataDependencyFragmenterTestCase3 {

	static File testFileDir = null;

	static Process process = null;

	static Definition definition = null;

	static RuntimeData data = null;

	static PartitionSpecification partitionSpec = null;

	static AnalysisResult analysis = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		//
		// use the process "OrderingProcess" in Project OrderInfo4DDTestCase3
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
				+ "\\OrderInfo4DDTestCase3\\bpelContent\\OrderingProcess.bpel";
		process = loadBPEL(bpelURI);

		// load wsdl
		definition = MyWSDLUtil.readWSDL(testFileDir.getAbsolutePath()
				+ "\\OrderInfo4DDTestCase3\\bpelContent\\OrderingProcess.wsdl");

		// partition specification
		String partitionURI = testFileDir.getAbsolutePath()
				+ "\\OrderInfo4DDTestCase3\\bpelContent\\Partition.xml";
		partitionSpec = loadPartitionSpec(partitionURI, process);

		// analyze process
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
		DataDependencyFragmenterScenario3 ddFragmenter = new DataDependencyFragmenterScenario3(data);
		ddFragmenter.splitDataDependency();

		// assert local resolver,
		// the node n2 is in different partition as the reader, there are more
		// than one query sets, therefore for n2 a local resolver with multiple
		// assign-scope and one invoke is created.
		PWDG pwdg = ddFragmenter.getPWDG();
		Set<PWDGNode> nodeSet = pwdg.getNodeSet("y");
		PWDGNode n2 = nodeSet.iterator().next();

		assertLocalResolver4N2(n2, ddFragmenter.getParticipant2FragProcMap(),
				ddFragmenter.getNode2NameMap(), ddFragmenter.getQuerySet2NameMap());

		// assert receiving flow
		Process readerProcess = ddFragmenter.getParticipant2FragProcMap().get("x");
		assertReceivingFlow4N2("responseRFFlow", readerProcess, pwdg,
				ddFragmenter.getNode2NameMap(), ddFragmenter.getQuerySet2NameMap(),
				ddFragmenter.getQuerySet4Node(n2));

		// assert topology/grounding message link y -> x
		assertMessageLinkY2X(ddFragmenter, "y");
	}

	private void assertLocalResolver4N2(PWDGNode n2, Map<String, Process> participant2FragProcMap,
			Map<PWDGNode, String> node2NameMap, Map<Set<String>, String> querySet2NameMap) {

		Process fragProc = participant2FragProcMap.get(n2.getParticipant());

		String inputVarName = node2NameMap.get(n2);
		Variable inputVar = MyBPELUtils.resolveVariable(inputVarName, fragProc);
		Assert.assertNotNull(inputVar);
		Assert.assertNotNull(inputVar.getMessageType());

		// assert the message contains 4 parts - status4gold, status4silver,
		// data, correlation
		Assert.assertTrue(inputVar.getMessageType().getParts().size() == 4);

		// assert Assign-Scope for writer "C", the query set of "C" on variable
		// "response" is ".gold"
		Set<String> querySet4C = new HashSet<String>();
		querySet4C.add(".gold");
		assertAssignScope4Writer(fragProc, n2, node2NameMap, querySet2NameMap, querySet4C, inputVar);

		// assert Assign-Scope for writer "D", the query set of "D" on variable
		// "response" is ".silver"
		Set<String> querySet4D = new HashSet<String>();
		querySet4D.add(".silver");
		assertAssignScope4Writer(fragProc, n2, node2NameMap, querySet2NameMap, querySet4D, inputVar);

		// assert Sequence that contains assign and invoke
		assertSequenceContainAssignAndInvoke(fragProc, n2, node2NameMap, querySet2NameMap, inputVar);

	}

	private void assertAssignScope4Writer(Process fragProc, PWDGNode n2,
			Map<PWDGNode, String> node2NameMap, Map<Set<String>, String> querySet2NameMap,
			Set<String> querySet4Writer, Variable inputVar) {

		Assert.assertNotNull(querySet2NameMap.get(querySet4Writer));

		// assign-scope exists
		String name4AssignScope = "AssignScoperesponse" + querySet2NameMap.get(querySet4Writer);
		Activity assignScope = MyBPELUtils.resolveActivity(name4AssignScope, fragProc);
		Assert.assertNotNull(assignScope);
		Assert.assertTrue(assignScope instanceof Scope);

		// activity of the scope is <assign> with the naming convention "Assign"
		// + varname + id(qs)
		Activity actInScope = ((Scope) assignScope).getActivity();
		Assert.assertNotNull(actInScope);
		Assert.assertTrue(actInScope instanceof Assign);
		Assert.assertTrue(actInScope.getName().equals(
				"Assignresponse" + querySet2NameMap.get(querySet4Writer)));

		// This assign contains one <copy> for status 'true()'
		List<Copy> cpList = ((Assign) actInScope).getCopy();
		Assert.assertTrue(cpList.size() == 1);

		Copy cp4Status = cpList.get(0);
		From from4Status = cp4Status.getFrom();
		To to4Status = cp4Status.getTo();
		Assert.assertTrue(from4Status.getExpression().getBody().equals("true()"));
		Assert.assertTrue(to4Status.getPart().getName()
				.equals("status" + querySet2NameMap.get(querySet4Writer))
				&& to4Status.getVariable() != null);

		// in the fauhltHandler is also an <assign> with the naming convention
		// "Assign" + varName + id(qs) + "FH"
		Activity actInScopeFH = ((Scope) assignScope).getFaultHandlers().getCatch().get(0)
				.getActivity();
		Assert.assertNotNull(actInScopeFH);
		Assert.assertTrue(actInScopeFH instanceof Assign);
		Assert.assertTrue(actInScopeFH.getName().equals(
				"Assignresponse" + querySet2NameMap.get(querySet4Writer) + "FH"));

		// This <assign> contains one <copy> for status 'false()'
		List<Copy> cpListFH = ((Assign) actInScopeFH).getCopy();
		Assert.assertTrue(cpListFH.size() == 1);

		Copy cp4StatusFH = cpListFH.get(0);
		From from4StatusFH = cp4StatusFH.getFrom();
		To to4StatusFH = cp4StatusFH.getTo();
		Assert.assertTrue(from4StatusFH.getExpression().getBody().equals("false()"));
		Assert.assertTrue(to4StatusFH.getPart().getName()
				.equals("status" + querySet2NameMap.get(querySet4Writer))
				&& to4StatusFH.getVariable() != null);

	}

	private void assertSequenceContainAssignAndInvoke(Process fragProc, PWDGNode n2,
			Map<PWDGNode, String> node2NameMap, Map<Set<String>, String> querySet2NameMap,
			Variable inputVar) {

		// assert the sequence with id(node) + "Sequence"
		Sequence seq4AssAndInv = (Sequence) MyBPELUtils.resolveActivity(node2NameMap.get(n2)
				+ "Sequence", fragProc);
		Assert.assertNotNull(seq4AssAndInv);

		// assert the assign activity
		Activity assInSeq = seq4AssAndInv.getActivities().get(0);
		Assert.assertNotNull(assInSeq);
		Assert.assertTrue(assInSeq instanceof Assign);

		List<Copy> cpList = ((Assign) assInSeq).getCopy();
		Assert.assertTrue(cpList.size() == 2);// copy the variable and the
												// correlation

		assertAssignWithCopy4DataAndCorrelation((Assign) assInSeq);

		// assert the invoke activity
		Activity invInSeq = seq4AssAndInv.getActivities().get(1);
		Assert.assertNotNull(invInSeq);
		Assert.assertTrue(invInSeq instanceof Invoke);

		Assert.assertTrue(((Invoke) invInSeq).getInputVariable().equals(inputVar));

	}

	private void assertAssignWithCopy4DataAndCorrelation(Assign assInSeq) {

		// assert the <assign> copy "data" part and "correlation" part.
		List<Copy> cpList = assInSeq.getCopy();
		Assert.assertNotNull(cpList);
		Assert.assertTrue(cpList.size() == 2);
		for (Copy cp : cpList) {
			From from = cp.getFrom();
			To to = cp.getTo();
			Assert.assertNotNull(from);
			Assert.assertNotNull(to);

			Assert.assertTrue(from.getVariable() != null);

			if (from.getVariable().getName()
					.equals(SplitProcessConstants.VARIABLE_FOR_CORRELATION_NAME)) {
				// copy from global variable to the correlation part in the
				// message
				Variable fromVar = from.getVariable();
				Assert.assertTrue(fromVar.getName().equals(
						SplitProcessConstants.VARIABLE_FOR_CORRELATION_NAME));
				Assert.assertTrue(to.getVariable() != null
						&& to.getPart().getName()
								.equals(SplitProcessConstants.CORRELATION_PART_NAME));
			} else {

				// to variable is also not null and the "data" part is not null
				Assert.assertNotNull(to.getVariable());
				Assert.assertTrue(to.getPart().getName().equals("data"));

			}

		}

	}

	private void assertReceivingFlow4N2(String flowName, Process readerProcess, PWDG pwdg,
			Map<PWDGNode, String> node2NameMap, Map<Set<String>, String> querySet2NameMap,
			Set<Set<String>> querySets4Node2) {

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

		String idStr4N1 = node2NameMap.get(n1);
		String assign4Node1Name = "Assign" + idStr4N1;
		Activity assign4Node1 = MyBPELUtils.resolveActivity(assign4Node1Name, readerProcess);
		Assert.assertNotNull(assign4Node1);
		Assert.assertTrue(assign4Node1 instanceof Assign);

		// for node n2=(y, {C, D})
		Set<PWDGNode> node2Set = pwdg.getNodeSet("y");
		PWDGNode n2 = node2Set.iterator().next();

		String idStr4N2 = node2NameMap.get(n2);

		String receive4Node2Name = "Receive" + idStr4N2;

		Activity receive4Node2 = MyBPELUtils.resolveActivity(receive4Node2Name, readerProcess);
		Assert.assertNotNull(receive4Node2);
		Assert.assertTrue(receive4Node2 instanceof Receive);

		// assert the sequence that encapsulates the Receive and an Assign is
		// created due to the correlation
		assertSequence4ReceiveAndAssign((Receive) receive4Node2, receivingFlow);

		// assert the receive activity contains correlation set
		assertCorrelationExist((Receive) receive4Node2);

		// assert there are multiple <assign>, one for each query set.
		for (Set<String> qs : querySets4Node2) {
			String assign4Node2Name = "Assign" + idStr4N2 + querySet2NameMap.get(qs);
			Activity assign4Node2 = MyBPELUtils.resolveActivity(assign4Node2Name, readerProcess);
			Assert.assertTrue(receivingFlow.equals(assign4Node2.eContainer()));

			Sequence sequence = (Sequence) receive4Node2.eContainer();
			String sequenceName = sequence.getName();
			String linkName = sequenceName + "2" + assign4Node2Name;
			assertExistLink((Flow) receivingFlow, linkName, (Receive) receive4Node2,
					querySet2NameMap.get(qs));
		}

	}

	private void assertSequence4ReceiveAndAssign(Receive receive4Node, Activity receivingFlow) {
		Activity sequence = (Activity) receive4Node.eContainer();
		Assert.assertNotNull(sequence);
		Assert.assertTrue(((Sequence) sequence).getActivities().get(1) instanceof Assign);
		Assert.assertTrue(receivingFlow.equals(sequence.eContainer()));
	}

	private void assertCorrelationExist(Receive receive4Node) {
		Correlation correl = receive4Node.getCorrelations().getChildren().get(0);
		Assert.assertNotNull(correl);
		Assert.assertTrue(correl.getInitiate().equals("join"));
		Assert.assertNotNull(correl.getSet());
		Assert.assertNotNull(correl.getSet().getProperties().size() == 1);
	}

	private void assertExistLink(Flow receivingFlow, String linkName, Receive receive4Node,
			String idOfQS) {
		List<Link> links = receivingFlow.getLinks().getChildren();

		boolean existLink = false;
		Link foundLink = null;
		for (Link link : links) {
			if (link.getName().equals(linkName)) {
				existLink = true;
				foundLink = link;
				break;
			}
		}

		Assert.assertTrue(existLink);
		Source source = foundLink.getSources().get(0);
		Condition cond = source.getTransitionCondition();
		Assert.assertTrue(cond.getBody().equals(
				"$" + receive4Node.getVariable().getName() + ".status" + idOfQS));
	}

	private void assertMessageLinkY2X(DataDependencyFragmenterScenario3 ddFragmenter, String sender) {
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

}

class DataDependencyFragmenterScenario3 extends DataDependencyFragmenter {

	protected QueryWriterSet qwSet = null;

	protected PWDG pwdg = null;

	public DataDependencyFragmenterScenario3(RuntimeData data) {
		super(data);
	}

	/**
	 * Only fragment the data dependency based on the given activity H and
	 * variable response
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
			qwSet = AnalysisResultParser.parse(act, var, this.analysisRes);
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

	public Set<Set<String>> getQuerySet4Node(PWDGNode node) {
		return this.qwSet.getQueryWriterSetFor(node).querySets();
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
