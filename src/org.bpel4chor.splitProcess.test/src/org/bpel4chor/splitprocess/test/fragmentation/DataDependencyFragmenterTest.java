package org.bpel4chor.splitprocess.test.fragmentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.bpel4chor.splitprocess.RuntimeData;
import org.bpel4chor.splitprocess.dataflowanalysis.AnalysisResultParser;
import org.bpel4chor.splitprocess.dataflowanalysis.DataFlowAnalyzer;
import org.bpel4chor.splitprocess.dataflowanalysis.QueryWriterSet;
import org.bpel4chor.splitprocess.exceptions.DataFlowAnalysisException;
import org.bpel4chor.splitprocess.exceptions.PWDGException;
import org.bpel4chor.splitprocess.exceptions.PartitionSpecificationException;
import org.bpel4chor.splitprocess.exceptions.WDGException;
import org.bpel4chor.splitprocess.fragmentation.DataDependencyFragmenter;
import org.bpel4chor.splitprocess.partition.model.PartitionSpecification;
import org.bpel4chor.splitprocess.partition.representation.PartitionSpecReader;
import org.bpel4chor.splitprocess.pwdg.model.PWDG;
import org.bpel4chor.splitprocess.pwdg.model.PWDGNode;
import org.bpel4chor.splitprocess.pwdg.model.WDG;
import org.bpel4chor.splitprocess.pwdg.util.PWDGFactory;
import org.bpel4chor.splitprocess.pwdg.util.WDGFactory;
import org.bpel4chor.splitprocess.utils.SplitProcessConstants;
import org.bpel4chor.utils.BPEL4ChorReader;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.BPELPlugin;
import org.eclipse.bpel.model.Process;
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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.uni_stuttgart.iaas.bpel.model.utilities.MyBPELUtils;
import de.uni_stuttgart.iaas.bpel.model.utilities.MyWSDLUtil;
import de.uni_stuttgart.iaas.bpel_d.algorithm.analysis.AnalysisResult;

public class DataDependencyFragmenterTest {

	static File testFileDir = null;

	static Process process = null;

	static Definition definition = null;

	static PartitionSpecification partitionSpec1 = null;
	static PartitionSpecification partitionSpec2 = null;

	static AnalysisResult analysis = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		//
		// use the process "OrderingProcess" in Project OrderInfo
		//

		File projectDir = new File("");
		testFileDir = new File(projectDir.getAbsolutePath(), "files");

		// init
		BPELPlugin bpelPlugin = new BPELPlugin();
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("bpel", new BPELResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("wsdl", new WSDLResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xsd", new XSDResourceFactoryImpl());

		// load bpel resource
		String bpelURI = testFileDir.getAbsolutePath() + "\\OrderInfo\\bpelContent\\OrderingProcess.bpel";
		process = loadBPEL(bpelURI);

		// load wsdl
		definition = MyWSDLUtil.readWSDL(testFileDir.getAbsolutePath()
				+ "\\OrderInfo\\bpelContent\\OrderingProcess.wsdl");

		// partition specification
		String partition1URI = testFileDir.getAbsolutePath() + "\\OrderInfo\\bpelContent\\Partition1.xml";
		partitionSpec1 = loadPartitionSpec(partition1URI, process);
		String partition2URI = testFileDir.getAbsolutePath() + "\\OrderInfo\\bpelContent\\Partition2.xml";
		partitionSpec2 = loadPartitionSpec(partition2URI, process);

		// analyse process
		analysis = DataFlowAnalyzer.analyze(process);
	}

	protected static Process loadBPEL(String strURI) {
		ResourceSet resourceSet = new ResourceSetImpl();
		URI uri = URI.createFileURI(strURI);
		Resource resource = resourceSet.getResource(uri, true);
		return (Process) resource.getContents().get(0);
	}

	protected static PartitionSpecification loadPartitionSpec(String strURI, Process process) throws JAXBException,
			FileNotFoundException, PartitionSpecificationException {
		FileInputStream inputStream = new FileInputStream(new File(strURI));
		PartitionSpecReader reader = new PartitionSpecReader();
		return reader.readSpecification(inputStream, process);
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
	public void testInitNode2NameMap() throws DataFlowAnalysisException, WDGException, PWDGException {
		// prepare pwdg for test
		Activity act = MyBPELUtils.resolveActivity("E", process);
		Variable var = MyBPELUtils.resolveVariable("paymentInfo", process);
		QueryWriterSet qwSet = AnalysisResultParser.parse(act, var, analysis);
		WDG wdg = WDGFactory.createWDG(qwSet.getAllWriters());

		// we have the pwdg {n1=(x,{B}), n2=(y,{C}), n3=(z,{D})}, e1=(n1,n2),
		// e2=(n1,n3)
		PWDG pwdg = PWDGFactory.createPWDG(wdg, process, partitionSpec1);
		assertTrue(pwdg.vertexSet().size() == 3);

		// create dataDependencyFragmenter
		RuntimeData data = new RuntimeData(process, partitionSpec1, definition,
				SplitProcessConstants.DEFAULT_SPLITTING_OUTPUT_DIR);
		MyDataDependencyFragmenter fragmenter = new MyDataDependencyFragmenter(data);

		// init node2NameMap
		fragmenter.initNode2NameMap(pwdg);

		// test whether the id associated to the node is unique
		Set<String> ids = new HashSet<String>();
		for (PWDGNode node : pwdg.vertexSet()) {
			String id = fragmenter.getNode2NameMap().get(node);
			assertNotNull(id);
			assertFalse(ids.contains(id));
			ids.add(id);
		}

	}

	@Test
	public void testInitQuerySet2nameMap() throws DataFlowAnalysisException, WDGException, PWDGException {
		// prepare pwdg for test
		Activity act = MyBPELUtils.resolveActivity("E", process);
		Variable var = MyBPELUtils.resolveVariable("paymentInfo", process);

		// we have a key set for variable "paymentInfo" { {.actNum}:{B},
		// {.amt}:{C, D, B} }
		QueryWriterSet qwSet = AnalysisResultParser.parse(act, var, analysis);
		assertTrue(qwSet.size() == 2);

		// create dataDependencyFragmenter
		RuntimeData data = new RuntimeData(process, partitionSpec1, definition,
				SplitProcessConstants.DEFAULT_SPLITTING_OUTPUT_DIR);
		MyDataDependencyFragmenter fragmenter = new MyDataDependencyFragmenter(data);

		// init querySet2nameMap
		fragmenter.initQuerySet2nameMap(var.getName(), qwSet.querySets());

		// test the querySet:id
		Set<String> qs1 = new HashSet<String>();
		Set<String> qs2 = new HashSet<String>();
		qs1.add(".actNum");
		qs2.add(".amt");
		String id1 = fragmenter.getQuerySet2NameMap().get(qs1);
		String id2 = fragmenter.getQuerySet2NameMap().get(qs2);
		assertNotNull(id1);
		assertNotNull(id2);
		assertFalse(id1.equals(id2));

	}

	@Test
	public void testGetIdQuerySet() {
		// prepare pwdg for test
		Activity act = MyBPELUtils.resolveActivity("E", process);
		Variable var = MyBPELUtils.resolveVariable("paymentInfo", process);

		// we have a key set for variable "paymentInfo" { {.actNum}:{B},
		// {.amt}:{C, D, B} }
		QueryWriterSet qwSet = AnalysisResultParser.parse(act, var, analysis);
		assertTrue(qwSet.size() == 2);

		// create dataDependencyFragmenter
		RuntimeData data = new RuntimeData(process, partitionSpec1, definition);
		MyDataDependencyFragmenter fragmenter = new MyDataDependencyFragmenter(data);

		// init querySet2nameMap
		fragmenter.initQuerySet2nameMap(var.getName(), qwSet.querySets());

		Set<String> qs1 = new HashSet<String>();
		Set<String> qs2 = new HashSet<String>();
		Set<String> qs3 = new HashSet<String>();

		qs1.add(".actNum");
		qs2.addAll(qs1);
		qs3.add(".amt");

		// id1 and id2 should be equal , they have the same query set
		String id1 = fragmenter.id(qs1);
		String id2 = fragmenter.id(qs2);
		assertNotNull(id1);
		assertNotNull(id2);
		assertEquals(id1, id2);

		// id1 and id3 are not equal, they have differnt query set
		String id3 = fragmenter.id(qs3);
		assertNotNull(id3);
		assertFalse(id3.equals(id1));
	}

	@Test
	public void testGetIdNode() throws PWDGException, WDGException {
		// prepare pwdg for test
		Activity act = MyBPELUtils.resolveActivity("E", process);
		Variable var = MyBPELUtils.resolveVariable("paymentInfo", process);
		QueryWriterSet qwSet = AnalysisResultParser.parse(act, var, analysis);
		WDG wdg = WDGFactory.createWDG(qwSet.getAllWriters());

		// we have the pwdg {n1=(x,{B}), n2=(y,{C}), n3=(z,{D})}, e1=(n1,n2),
		// e2=(n1,n3)
		PWDG pwdg = PWDGFactory.createPWDG(wdg, process, partitionSpec1);
		assertTrue(pwdg.vertexSet().size() == 3);

		RuntimeData data = new RuntimeData(process, partitionSpec1, definition);
		MyDataDependencyFragmenter fragmenter = new MyDataDependencyFragmenter(data);

		fragmenter.initNode2NameMap(pwdg);

		Set<String> ids = new HashSet<String>();
		for (PWDGNode node : pwdg.vertexSet()) {
			String id = fragmenter.getNode2NameMap().get(node);
			assertNotNull(id);
			assertFalse(ids.contains(id));
			ids.add(id);
		}
	}

	@Test
	public void testGetSortedActivities() {
		RuntimeData data = new RuntimeData(process, partitionSpec1, definition);
		MyDataDependencyFragmenter fragmenter = new MyDataDependencyFragmenter(data);

		// create unsorted array
		List<Activity> acts = new ArrayList<Activity>();
		Activity act1 = BPELFactory.eINSTANCE.createActivity();
		Activity act2 = BPELFactory.eINSTANCE.createActivity();
		act1.setName("kActivity");
		act2.setName("iActi");
		acts.add(act1);
		acts.add(act2);
		// sort them
		fragmenter.sortActivities(acts);
		// assert they are sorted
		assertActivitiesIsSorted(acts);
		
		
	}

	private void assertActivitiesIsSorted(List<Activity> acts) {
		String last = null;
		for (int i = 0; i < acts.size(); i++) {
			String current = acts.get(i).getName();
			assertNotNull(current);
			if (last != null)
				assertTrue(last.compareTo(current) < 0);
			last = current;
		}
	}

	@Test
	public void testSortVariables() {
		RuntimeData data = new RuntimeData(process, partitionSpec1, definition);
		MyDataDependencyFragmenter fragmenter = new MyDataDependencyFragmenter(data);
		
		// create unsorted array
		List<Variable> vars = new ArrayList<Variable>();
		Variable var1 = BPELFactory.eINSTANCE.createVariable();
		Variable var2 = BPELFactory.eINSTANCE.createVariable();
		var1.setName("BVriable");
		var2.setName("AVriable");
		vars.add(var1);
		vars.add(var2);
		// sort them
		fragmenter.sortVariables(vars);
		// assert they are sorted
		assertVariablesIsSorted(vars);
		
	}

	private void assertVariablesIsSorted(List<Variable> vars) {
		String last = null;
		for (int i = 0; i < vars.size(); i++) {
			String current = vars.get(i).getName();
			assertNotNull(current);
			if (last != null) {
				assertTrue(last.compareTo(current) < 0);
			}
			last = current;
		}
		
	}

	@Test
	public void testDataDependencyFragmenter() {
		
		try {
			@SuppressWarnings("unused")
			DataDependencyFragmenter frag = new DataDependencyFragmenter(null);
			fail(); 
		} catch (NullPointerException e) {
			// catch the NPE is good.
		}
	}

//	@Test
//	public void createLocalResolver() {
//		
//	}
//	
//	@Test
//	public void createReceivingFlow() {
//		
//	}
//	
//	@Test
//	public void testSplitDataDependency() {
//		fail("Not yet implemented"); // TODO
//	}

}

class MyDataDependencyFragmenter extends DataDependencyFragmenter {

	public MyDataDependencyFragmenter(RuntimeData data) {
		super(data);
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
}