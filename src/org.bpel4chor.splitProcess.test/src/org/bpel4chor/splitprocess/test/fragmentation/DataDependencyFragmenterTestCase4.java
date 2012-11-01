package org.bpel4chor.splitprocess.test.fragmentation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import de.uni_stuttgart.iaas.bpel.model.utilities.MyBPELUtils;
import de.uni_stuttgart.iaas.bpel.model.utilities.MyWSDLUtil;

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
import org.bpel4chor.utils.BPEL4ChorReader;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Assign;
import org.eclipse.bpel.model.BPELPlugin;
import org.eclipse.bpel.model.Condition;
import org.eclipse.bpel.model.Copy;
import org.eclipse.bpel.model.Empty;
import org.eclipse.bpel.model.From;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Scope;
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
import org.eclipse.wst.wsdl.Part;
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
 * Test for DataDependencyFragmenter - case same partition, multiple query sets
 * 
 * @since Jul 03, 2012
 * @author Daojun Cui
 */
public class DataDependencyFragmenterTestCase4 {

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
				+ "\\OrderInfo4DDTestCase3\\bpelContent\\Partition-SamePartitionMultipleQuerySet.xml";
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
		// the partition is p1=(x, {A, B, C, H}), p2=(y, {C})
		// PWDG(H, response) = {
		// V={n1=(x,{B, C}), n2=(y,{D})}
		// E={(n1,n2)}
		// }

		// Note that node n1 is in the same partition with the reader H.
		//

		// split data dependency using PWDG based on the reader=H, var=response
		DataDependencyFragmenterScenario4 ddFragmenter = new DataDependencyFragmenterScenario4(data);
		ddFragmenter.splitDataDependency();

		PWDG pwdg = ddFragmenter.getPWDG();
		Set<PWDGNode> nodeSet = pwdg.getNodeSet("x");
		PWDGNode n1 = nodeSet.iterator().next();

		// assert the message for same partition, multiple query sets
		String samePartitionMQMessage = "xxresponse" + ddFragmenter.id(n1) + "MQMessage";
		assertMessage4SamePartitionMultipleQuerySet(samePartitionMQMessage,
				ddFragmenter.getDefinition("x"), ddFragmenter.getQuerySet4Node(n1),
				ddFragmenter.getQuerySet2NameMap());

		// assert local resolver for the node n1 is in same partition as the
		// reader, there are more than one query sets
		assertLocalResolver4N1(n1, ddFragmenter.getParticipant2FragProcMap(),
				ddFragmenter.getNode2NameMap(), ddFragmenter.getQuerySet2NameMap());

		// assert receiving flow
		Process readerProcess = ddFragmenter.getParticipant2FragProcMap().get("x");
		assertReceivingFlow4N1("responseRFFlow", readerProcess, pwdg,
				ddFragmenter.getNode2NameMap(), ddFragmenter.getQuerySet2NameMap(),
				ddFragmenter.getQuerySet4Node(n1));

	}

	private void assertMessage4SamePartitionMultipleQuerySet(String samePartitionMQMessage,
			Definition defn, Set<Set<String>> qs4Node, Map<Set<String>, String> qs2NameMap) {

		QName msgQname = new QName(defn.getTargetNamespace(), samePartitionMQMessage);
		Message msg = (Message) defn.getMessage(msgQname);
		Assert.assertNotNull(msg);

		// status parts are created
		for (Set<String> qs : qs4Node) {
			String partName = "status" + qs2NameMap.get(qs);
			Part part = (Part) msg.getPart(partName);
			Assert.assertNotNull(part);
		}

//		// data parts are created
//		Variable var = MyBPELUtils.resolveVariable("response", process);
//		Message msg4Var = var.getMessageType();
//		for (Object obj : msg4Var.getParts().values()) {
//			Part origPart = (Part) obj;
//			Part actualPart = (Part) msg.getPart(origPart.getName());
//			Assert.assertNotNull(actualPart);
//		}

	}

	private void assertLocalResolver4N1(PWDGNode n1, Map<String, Process> participant2FragProcMap,
			Map<PWDGNode, String> node2NameMap, Map<Set<String>, String> querySet2NameMap) {

		Process fragProc = participant2FragProcMap.get(n1.getParticipant());

		String inputVarName = node2NameMap.get(n1);
		Variable inputVar = MyBPELUtils.resolveVariable(inputVarName, fragProc);
		Assert.assertNotNull(inputVar);
		Assert.assertNotNull(inputVar.getMessageType());

		// assert the message parts of this message is size(original message
		// part) + size(status parts)
		int sizeMsgParts = inputVar.getMessageType().getParts().size();
		int sizeStatusParts = 2;// normal, gold
		Assert.assertTrue(sizeMsgParts == sizeStatusParts);

		// assert Assign-Scope for writer "C", the query set of "C" on variable
		// "response" is ".gold"
		Set<String> querySet4C = new HashSet<String>();
		querySet4C.add(".gold");
		assertAssignScope4Writer(fragProc, n1, node2NameMap, querySet2NameMap, querySet4C, inputVar);

		// assert Assign-Scope for writer "B", the query set of "B" on variable
		// "response" is ".normal"
		Set<String> querySet4B = new HashSet<String>();
		querySet4B.add(".normal");
		assertAssignScope4Writer(fragProc, n1, node2NameMap, querySet2NameMap, querySet4B, inputVar);

		// assert the empty activity
		assertEmptyActivity(fragProc, n1, node2NameMap);
	}

	private void assertEmptyActivity(Process fragProc, PWDGNode n1,
			Map<PWDGNode, String> node2NameMap) {

	}

	private void assertAssignScope4Writer(Process fragProc, PWDGNode n1,
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

	private void assertReceivingFlow4N1(String flowName, Process readerProcess, PWDG pwdg,
			Map<PWDGNode, String> node2NameMap, Map<Set<String>, String> querySet2NameMap,
			Set<Set<String>> querySet4Node) {
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
		// the receiving flow contains two <assign> activities and one <empty>
		// activity for node n1
		//

		// for node n1=(x, {B, C})
		Set<PWDGNode> node1Set = pwdg.getNodeSet("x");
		PWDGNode n1 = node1Set.iterator().next();

		// assert <empty> activity in receiving flow
		String emptyActName = "RF" + node2NameMap.get(n1) + "Empty";
		Activity act = MyBPELUtils.resolveActivity(emptyActName, readerProcess);
		Assert.assertTrue(act != null && act instanceof Empty);

		// assert out-coming link from <empty> activity with the condition
		List<Source> sources = act.getSources().getChildren();
		Assert.assertTrue(sources.size() == 2);

		Iterator<Set<String>> qsIt = querySet4Node.iterator();
		Set<String> qs1 = qsIt.next();
		Set<String> qs2 = qsIt.next();
		String condition1 = "$" + node2NameMap.get(n1) + ".status" + querySet2NameMap.get(qs1);
		String condition2 = "$" + node2NameMap.get(n1) + ".status" + querySet2NameMap.get(qs2);

		String fromVarName = "response";
		String toVarName = tmpVarName;
		
		for (Source source : sources) {
			Condition cond = source.getTransitionCondition();
			Assert.assertNotNull(cond);
			
			// assert <assign> activity
			if (cond.getBody().equals(condition1)) {
				assertAssign4QueryExist(readerProcess, source, n1, qs1, fromVarName, toVarName);
			} else if (cond.getBody().equals(condition2)) {
				assertAssign4QueryExist(readerProcess, source, n1, qs2, fromVarName, toVarName);
			} else {
				Assert.fail();
			}
		}

	}

	private void assertAssign4QueryExist(Process readerProcess, Source source, PWDGNode node,
			Set<String> qs, String fromVarName, String toVarName) {

		// assert the activity that is connected in the target of the link is
		// assign
		Link link = source.getLink();
		Activity actAssign = link.getTargets().get(0).getActivity();
		Assert.assertTrue(actAssign != null && actAssign instanceof Assign);

		Assign assign = (Assign) actAssign;
		List<Copy> cpList = assign.getCopy();
		Assert.assertTrue(cpList.size() == qs.size());

		for (Copy cp : cpList) {

			From from = cp.getFrom();
			To to = cp.getTo();

			Variable fromVar = from.getVariable();
			Assert.assertTrue(fromVar != null && fromVar.getName().equals(fromVarName));
			
			Part fromPart = from.getPart();
			Assert.assertTrue(fromPart != null && qs.contains("." + fromPart.getName()));

			Variable toVar = to.getVariable();
			Assert.assertTrue(toVar != null && toVar.getName().equals(toVarName));
			
			Part toPart = to.getPart();
			Assert.assertTrue(toPart != null && qs.contains("." + toPart.getName()));
		}

	}
}

class DataDependencyFragmenterScenario4 extends DataDependencyFragmenter {

	protected QueryWriterSet qwSet = null;

	protected PWDG pwdg = null;

	public DataDependencyFragmenterScenario4(RuntimeData data) {
		super(data);
	}

	public Definition getDefinition(String participantName) {
		return super.participant2WSDL.get(participantName);
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
