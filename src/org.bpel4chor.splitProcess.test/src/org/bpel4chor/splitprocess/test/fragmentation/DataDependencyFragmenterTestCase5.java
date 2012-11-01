package org.bpel4chor.splitprocess.test.fragmentation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

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
import org.eclipse.bpel.model.Copy;
import org.eclipse.bpel.model.From;
import org.eclipse.bpel.model.Process;
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
 * Test for DataDependencyFragmenter - case same partition, single query set
 * 
 * @since Jul 06, 2012
 * @author Daojun Cui
 * 
 */
public class DataDependencyFragmenterTestCase5 {

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
		DataDependencyFragmenterScenario5 ddFragmenter = new DataDependencyFragmenterScenario5(data);
		ddFragmenter.splitDataDependency();

		PWDG pwdg = ddFragmenter.getPWDG();
		Set<PWDGNode> nodeSet = pwdg.getNodeSet("x");
		PWDGNode n1 = nodeSet.iterator().next();

		Process readerProcess = ddFragmenter.getParticipant2FragProcMap().get("x");

		// assert local resolver for the node n1 is in same partition as the
		// reader, there are more than one query sets
		assertLocalResolver4N1(n1, ddFragmenter.getParticipant2FragProcMap(),
				ddFragmenter.getNode2NameMap(), readerProcess);

		// assert receiving flow
		assertReceivingFlow4N1(n1, "responseRFFlow", readerProcess, pwdg,
				ddFragmenter.getNode2NameMap(), ddFragmenter.getQuerySet2NameMap(),
				ddFragmenter.getQuerySet4Node(n1));
	}

	private void assertLocalResolver4N1(PWDGNode n1, Map<String, Process> participant2FragProcMap,
			Map<PWDGNode, String> node2NameMap, Process readerProcess) {

		String emptyName = node2NameMap.get(n1) + "Empty";
		Activity empty = MyBPELUtils.resolveActivity(emptyName, readerProcess);
		Assert.assertNotNull(empty);

		Target target = empty.getTargets().getChildren().get(0);
		Source source = target.getLink().getSources().get(0);
		Activity sourceAct = source.getActivity();

		Activity actB = MyBPELUtils.resolveActivity("B", readerProcess);

		Assert.assertEquals(sourceAct, actB);

	}

	private void assertReceivingFlow4N1(PWDGNode n1, String flowName, Process readerProcess,
			PWDG pwdg, Map<PWDGNode, String> node2NameMap,
			Map<Set<String>, String> querySet2NameMap, Set<Set<String>> querySet4Node) {

		// assign for same partition, single query set
		String assignName = "Assign" + node2NameMap.get(n1);
		Activity assign = MyBPELUtils.resolveActivity(assignName, readerProcess);

		List<Copy> cpList = ((Assign) assign).getCopy();

		String fromVarName = "response";
		String toVarName = "tmpresponse4H";

		for (Copy cp : cpList) {

			From from = cp.getFrom();
			To to = cp.getTo();

			Assert.assertTrue(from.getVariable().getName().equals(fromVarName));
			Assert.assertTrue(to.getVariable().getName().equals(toVarName));

			Assert.assertTrue(from.getPart().getName().equals(to.getPart().getName()));
		}

		Target target = assign.getTargets().getChildren().get(0);
		Source source = target.getLink().getSources().get(0);

		Activity sourceAct = source.getActivity();

		Activity actB = MyBPELUtils.resolveActivity("B", readerProcess);

		Assert.assertEquals(actB, sourceAct);

	}

}

class DataDependencyFragmenterScenario5 extends DataDependencyFragmenter {

	protected QueryWriterSet qwSet = null;

	protected PWDG pwdg = null;

	public DataDependencyFragmenterScenario5(RuntimeData data) {
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
