package org.bpel4chor.splitprocess.test.dataflowanalysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bpel4chor.splitprocess.dataflowanalysis.AnalysisResultParser;
import org.bpel4chor.splitprocess.dataflowanalysis.QueryWriterSet;
import org.bpel4chor.splitprocess.partition.model.PartitionSpecification;
import org.bpel4chor.splitprocess.pwdg.model.PWDGNode;
import org.bpel4chor.splitprocess.pwdg.model.WDGNode;
import org.bpel4chor.utils.MyBPELUtils;
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

import de.uni_stuttgart.iaas.bpel_d.algorithm.analysis.AnalysisResult;

public class QueryWriterSetTest {

	static File testFileDir = null;

	static Process process = null;

	static Definition definition = null;

	static PartitionSpecification partitionSpec = null;

	static AnalysisResult analysis = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		File projectDir = new File("");
		testFileDir = new File(projectDir.getAbsolutePath(), "files");

		// init
		BPELPlugin bpelPlugin = new BPELPlugin();
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("bpel", new BPELResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("wsdl", new WSDLResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xsd", new XSDResourceFactoryImpl());

		// load bpel resource
		ResourceSet resourceSet = new ResourceSetImpl();
		URI uri = URI.createFileURI(testFileDir.getAbsolutePath()
				+ "\\OrderInfoSimple3\\bpelContent\\OrderingProcessSimple3.bpel");
		Resource resource = resourceSet.getResource(uri, true);
		process = (Process) resource.getContents().get(0);

		// analyse
		analysis = de.uni_stuttgart.iaas.bpel_d.algorithm.analysis.Process.analyzeProcessModel(process);
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
	public void testQueryWriterSet() {
		try {
			QueryWriterSet qws = new MyQueryWriterSet(null, null, null);
			fail();
		} catch (Exception e) {

		}
	}

	@Test
	public void testGetWriters() {

		// get the query writers set of "paymentInfo" for "AssignE"
		Activity act = MyBPELUtils.resolveActivity("AssignE", process);
		Variable var = MyBPELUtils.resolveVariable("paymentInfo", process);
		QueryWriterSet assignEAndPaymentInfo = AnalysisResultParser.parse(act, var, analysis);

		// get the writers union
		Set<Activity> writers = assignEAndPaymentInfo.getAllWriters();

		assertNotNull(writers);
		assertEquals(3, writers.size());

		// the 3 writers are "AssignB", "AssignC", "AssignD"
		List<String> wrtNames = Arrays.asList(new String[] { "AssignB", "AssignC", "AssignD" });
		for (Activity actWriter : writers) {
			assertTrue(wrtNames.contains(actWriter.getName()));
		}
	}

	@Test
	public void testMergeQuerySet() {
		// create a queryWriterSet
		Set<String> qs1 = new HashSet<String>();
		Set<String> qs2 = new HashSet<String>();
		Set<String> qs3 = new HashSet<String>();
		Set<String> qs4 = new HashSet<String>();
		Set<String> qs5 = new HashSet<String>();
		Set<String> qs6 = new HashSet<String>();
		Set<Activity> as1 = new HashSet<Activity>();
		Set<Activity> as2 = new HashSet<Activity>();
		Set<Activity> as3 = new HashSet<Activity>();
		Set<Activity> as4 = new HashSet<Activity>();
		Set<Activity> as5 = new HashSet<Activity>();
		Set<Activity> as6 = new HashSet<Activity>();
		Activity A = BPELFactory.eINSTANCE.createActivity();
		Activity B = BPELFactory.eINSTANCE.createActivity();
		Activity C = BPELFactory.eINSTANCE.createActivity();

		A.setName("A");
		B.setName("B");
		C.setName("C");
		qs1.add(".a");
		qs2.add(".b");
		qs3.add(".c");
		qs4.add(".d");
		qs5.add(".e");
		qs6.add(".f");
		as1.add(A);
		as2.add(A);
		as3.add(A);
		as4.add(B);
		as5.add(B);
		as6.add(C);
		Map<Set<String>, Set<Activity>> qs2wsMap = new HashMap<Set<String>, Set<Activity>>();
		qs2wsMap.put(qs1, as1);
		qs2wsMap.put(qs2, as2);
		qs2wsMap.put(qs3, as3);
		qs2wsMap.put(qs4, as4);
		qs2wsMap.put(qs5, as5);
		qs2wsMap.put(qs6, as6);
		// now try it
		Activity a = BPELFactory.eINSTANCE.createActivity();
		Variable x = BPELFactory.eINSTANCE.createVariable();
		Map<Set<String>, Set<Activity>> emptyMap = new HashMap<Set<String>, Set<Activity>>();
		MyQueryWriterSet myqws = new MyQueryWriterSet(a, x, emptyMap);
		myqws.mergeQuerySet(qs2wsMap);
		// test
		assertNotNull(qs2wsMap);
		assertEquals(3, qs2wsMap.size());
		// now there should be a {.a, .b, .c}:{A} entry
		Set<String> expectedQs = new HashSet<String>();
		expectedQs.add(".a");
		expectedQs.add(".b");
		expectedQs.add(".c");
		Set<Activity> expectedWs = new HashSet<Activity>();
		expectedWs.add(A);
		Set<Activity> actualWs = qs2wsMap.get(expectedQs);
		assertNotNull(actualWs);
		assertTrue(expectedWs.equals(actualWs));

		// {.d, .e}:{B}
		expectedQs.clear();
		expectedQs.add(".d");
		expectedQs.add(".e");
		expectedWs.clear();
		expectedWs.add(B);
		actualWs = qs2wsMap.get(expectedQs);
		assertNotNull(actualWs);
		assertTrue(expectedWs.equals(actualWs));

		// now there should also be {x.f}:{C} entry
		expectedQs.clear();
		expectedQs.add(".f");
		expectedWs.clear();
		expectedWs.add(C);
		actualWs = qs2wsMap.get(expectedQs);
		assertNotNull(actualWs);
		assertTrue(expectedWs.equals(actualWs));

	}

	@Test
	public void testGetQueryWriterSetForNode() {
		Set<String> qs1 = new HashSet<String>();
		Set<String> qs2 = new HashSet<String>();
		Set<String> qs3 = new HashSet<String>();
		Set<String> qs4 = new HashSet<String>();
		Set<String> qs5 = new HashSet<String>();
		Set<String> qs6 = new HashSet<String>();
		Set<Activity> as1 = new HashSet<Activity>();
		Set<Activity> as2 = new HashSet<Activity>();
		Set<Activity> as3 = new HashSet<Activity>();
		Set<Activity> as4 = new HashSet<Activity>();
		Set<Activity> as5 = new HashSet<Activity>();
		Set<Activity> as6 = new HashSet<Activity>();
		Activity A = BPELFactory.eINSTANCE.createActivity();
		Activity B = BPELFactory.eINSTANCE.createActivity();
		Activity C = BPELFactory.eINSTANCE.createActivity();
		Activity D = BPELFactory.eINSTANCE.createActivity();
		Activity E = BPELFactory.eINSTANCE.createActivity();
		Activity F = BPELFactory.eINSTANCE.createActivity();

		A.setName("A");
		B.setName("B");
		C.setName("C");
		D.setName("D");
		E.setName("E");
		F.setName("F");
		qs1.add(".a");
		qs2.add(".b");
		qs3.add(".c");
		qs4.add(".d");
		qs5.add(".e");
		qs6.add(".f");
		as1.add(A);
		as1.add(D);
		as2.add(A);
		as2.add(E);
		as3.add(A);
		as4.add(B);
		as4.add(D);
		as5.add(B);
		as5.add(E);
		as6.add(C);
		

		Map<Set<String>, Set<Activity>> qs2wsMap = new HashMap<Set<String>, Set<Activity>>();
		qs2wsMap.put(qs1, as1);
		qs2wsMap.put(qs2, as2);
		qs2wsMap.put(qs3, as3);
		qs2wsMap.put(qs4, as4);
		qs2wsMap.put(qs5, as5);
		qs2wsMap.put(qs6, as6);
		// now we have the tuples (query set: writer set) - {x.a}:{A,D},
		// {x.b}:{A,E}, {x.c}:{A}, {x.d}:{B, D},
		// {x.e}:{B,E}, {x.f}:{C}

		// create a queryWriterSet
		Activity a = BPELFactory.eINSTANCE.createActivity();
		Variable x = BPELFactory.eINSTANCE.createVariable();
		MyQueryWriterSet myqws = new MyQueryWriterSet(a, x, qs2wsMap);
		// now create a pwdg node
		PWDGNode node = new PWDGNode();
		WDGNode wnode1 = new WDGNode(A);
		WDGNode wnode2 = new WDGNode(B);
		node.add(wnode1);
		node.add(wnode2);
		// now we try the getQuerywriterSetFor the pwdgNode
		QueryWriterSet qwsForNode = myqws.getQueryWriterSetFor(node);
		assertNotNull(qwsForNode);
		assertEquals(qwsForNode.getActivity(), myqws.getActivity());
		assertEquals(qwsForNode.getVariable(), myqws.getVariable());
		// the writer set now is {A, B}
		assertEquals(qwsForNode.getAllWriters().size(), 2);
		assertTrue(qwsForNode.getAllWriters().contains(A));
		assertTrue(qwsForNode.getAllWriters().contains(B));
		// the node filtered queryWriterSet should contain {x.a, x.b, x.c}:{A}
		Set<String> expectedQs = new HashSet<String>();
		expectedQs.add(".a");
		expectedQs.add(".b");
		expectedQs.add(".c");
		Set<Activity> expectedWs = new HashSet<Activity>();
		expectedWs.add(A);
		Set<Activity> actualWs = qwsForNode.get(expectedQs);
		assertNotNull(actualWs);
		assertTrue(expectedWs.equals(actualWs));
		// the node filtered queryWriterSet should contain {x.d, x.e}:{B}
		expectedQs.clear();
		expectedQs.add(".d");
		expectedQs.add(".e");
		expectedWs.clear();
		expectedWs.add(B);
		actualWs = qwsForNode.get(expectedQs);
		assertNotNull(actualWs);
		assertTrue(expectedWs.equals(actualWs));

		// for pwdg node {F}, the query writer set is empty
		WDGNode wdgNodeF = new WDGNode(F);
		PWDGNode node2 = new PWDGNode();
		node2.add(wdgNodeF);
		QueryWriterSet qwsForNodeF = myqws.getQueryWriterSetFor(node2);
		assertNotNull(qwsForNodeF);
		assertTrue(qwsForNodeF.isEmpty());
		
	}
}

class MyQueryWriterSet extends QueryWriterSet {

	public MyQueryWriterSet(Activity act, Variable var, Map<Set<String>, Set<Activity>> query2WriterMap) {
		super(act, var, query2WriterMap);
	}

	public void mergeQuerySet(Map<Set<String>, Set<Activity>> qws) {
		super.mergeQuerySet(qws);
	}
	
}
