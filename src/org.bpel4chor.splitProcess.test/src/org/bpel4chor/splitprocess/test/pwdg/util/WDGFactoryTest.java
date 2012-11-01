package org.bpel4chor.splitprocess.test.pwdg.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.uni_stuttgart.iaas.bpel.model.utilities.MyBPELUtils;

import org.bpel4chor.splitprocess.dataflowanalysis.AnalysisResultParser;
import org.bpel4chor.splitprocess.dataflowanalysis.DataFlowAnalyzer;
import org.bpel4chor.splitprocess.dataflowanalysis.QueryWriterSet;
import org.bpel4chor.splitprocess.exceptions.WDGException;
import org.bpel4chor.splitprocess.pwdg.Path;
import org.bpel4chor.splitprocess.pwdg.model.WDG;
import org.bpel4chor.splitprocess.pwdg.model.WDGNode;
import org.bpel4chor.splitprocess.pwdg.util.WDGFactory;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.BPELPlugin;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Variable;
import org.eclipse.bpel.model.resource.BPELResourceFactoryImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.wst.wsdl.internal.util.WSDLResourceFactoryImpl;
import org.eclipse.xsd.util.XSDResourceFactoryImpl;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph.CycleFoundException;
import org.jgrapht.graph.DefaultEdge;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.uni_stuttgart.iaas.bpel_d.algorithm.analysis.AnalysisResult;

public class WDGFactoryTest {

	static File testFileDir = null;

	static Process process = null;

	static QueryWriterSet queryWrtSet = null;

	static AnalysisResult analysisRes = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		File projectDir = new File("");
		testFileDir = new File(projectDir.getAbsolutePath(), "files");

		// init
		BPELPlugin bpelPlugin = new BPELPlugin();
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("bpel", new BPELResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("wsdl", new WSDLResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xsd", new XSDResourceFactoryImpl());

		String uri = testFileDir.getAbsolutePath() + "\\OrderInfo\\bpelContent\\OrderingProcess.bpel";
		process = loadBPEL(uri);

		analysisRes = DataFlowAnalyzer.analyze(process);

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

	@Test
	public void testIsEquals() {

		// test subject E, B
		Activity E = MyBPELUtils.resolveActivity("E", process);
		Activity B = MyBPELUtils.resolveActivity("B", process);

		// they are not equal naturally
		assertFalse(MyWDGFactory.isEquals(B, E));

		// get queryWriterSet, activity=E, variable=paymentInfo
		Variable pymInfo = MyBPELUtils.resolveVariable("paymentInfo", process);

		queryWrtSet = AnalysisResultParser.parse(E, pymInfo, analysisRes);
		Set<Activity> writers = queryWrtSet.getAllWriters();
		for (Activity act : writers) {
			if (act.getName().equals("B")) {
				// the "B" from MyBPELUtils and the one from
				// DataFlowAnalyzer, they should be equal.
				assertTrue(MyWDGFactory.isEquals(act, B));
				break;
			}
		}

	}

	@Test 
	public void testFindAllPathCurrentEqualTarget() {
		// start point = A = end point
		Activity receiveA = MyBPELUtils.resolveActivity("A", process);
		Path<Activity> visted = new Path<Activity>();
		List<Path<Activity>> foundList = new ArrayList<Path<Activity>>();
		MyWDGFactory.findPath(visted, receiveA, receiveA, foundList);
		
		// the found list = {}
		assertEquals(0, foundList.size());
		
	}
	
	@Test
	public void testFindAllPathCurrentNOTEqualTarget() {
		// find path from A -> F
		List<Path<Activity>> paths = new ArrayList<Path<Activity>>();

		// start point = A
		Activity receiveA = MyBPELUtils.resolveActivity("A", process);
		// end point
		Activity invokeF = MyBPELUtils.resolveActivity("F", process);

		// current path
		Path<Activity> path = new Path<Activity>();
		MyWDGFactory.findPath(path, receiveA, invokeF, paths);

		// there should be 3 paths found
		assertEquals(3, paths.size());

		// test all of them
		List<String> path1 = Arrays.asList(new String[] { "A", "B", "E", "F" });
		List<String> path2 = Arrays.asList(new String[] { "A", "B", "C", "E", "F" });
		List<String> path3 = Arrays.asList(new String[] { "A", "B", "D", "E", "F" });

		// test path1
		boolean contain = false;
		for (Path<Activity> nextPath : paths) {
			if (isSamePath(path1, nextPath)) {
				contain = true;
				break;
			}
		}
		assertTrue(contain);
		// test path2
		contain = false;
		for (Path<Activity> nextPath : paths) {
			if (isSamePath(path2, nextPath)) {
				contain = true;
				break;
			}
		}
		assertTrue(contain);
		// test path3
		contain = false;
		for (Path<Activity> nextPath : paths) {
			if (isSamePath(path3, nextPath)) {
				contain = true;
				break;
			}
		}
		assertTrue(contain);
	}

	protected boolean isSamePath(List<String> path1, Path<Activity> path2) {
		if (path1.size() != path2.length())
			return false;
		for (int i = 0; i < path1.size(); i++) {
			if (path1.get(i).equals(path2.get(i).getName()) == false)
				return false;
		}
		return true;
	}

	@Test
	public void testPathContainIntermediateWdgNode() {

		WDGNode B = new WDGNode(MyBPELUtils.resolveActivity("B", process));
		WDGNode C = new WDGNode(MyBPELUtils.resolveActivity("C", process));
		WDGNode D = new WDGNode(MyBPELUtils.resolveActivity("D", process));
		WDGNode E = new WDGNode(MyBPELUtils.resolveActivity("E", process));

		// wdg nodes : B, D, E
		Set<WDGNode> artificialWdgNodes = new HashSet<WDGNode>();
		artificialWdgNodes.add(B);
		artificialWdgNodes.add(D);
		artificialWdgNodes.add(E);

		// path1 : B->C->E
		// C is intermediate, but NOT WDG node. Path does NOT contain
		// intermediate wdg node.
		Path<Activity> path1 = new Path<Activity>();
		path1.append(B.activity());
		path1.append(C.activity());
		path1.append(E.activity());
		assertFalse(MyWDGFactory.pathContainIntermediateWdgNode(path1, artificialWdgNodes));

		// path2 : B->E
		// it just contains the start and end node, so Path does NOT contain
		// intermediate wdg node.
		Path<Activity> path2 = new Path<Activity>();
		path2.append(B.activity());
		path2.append(E.activity());
		assertFalse(MyWDGFactory.pathContainIntermediateWdgNode(path2, artificialWdgNodes));

		// path3 : B->D->E
		// D is wdg node, so path does contain the intermediate wdg node.
		Path<Activity> path3 = new Path<Activity>();
		path3.append(B.activity());
		path3.append(D.activity());
		path3.append(E.activity());
		assertTrue(MyWDGFactory.pathContainIntermediateWdgNode(path3, artificialWdgNodes));
	}

	@Test
	public void testCreateEdge() throws CycleFoundException {
		WDGNode B = new WDGNode(MyBPELUtils.resolveActivity("B", process));
		WDGNode C = new WDGNode(MyBPELUtils.resolveActivity("C", process));
		WDGNode D = new WDGNode(MyBPELUtils.resolveActivity("D", process));
		WDGNode E = new WDGNode(MyBPELUtils.resolveActivity("E", process));

		// wdg nodes : B, C, D, E
		WDG wdg = new WDG();
		wdg.addVertex(B);
		wdg.addVertex(C);
		wdg.addVertex(D);
		wdg.addVertex(E);

		// B->E, exists
		MyWDGFactory.createEdge(B, E, wdg);
		DefaultEdge BE = wdg.getEdge(B, E);
		assertNotNull(BE);
		assertTrue(BE.toString().contains(B.getName()));
		assertTrue(BE.toString().contains(E.getName()));
		assertTrue(BE.toString().indexOf(B.getName()) < BE.toString().indexOf(E.getName()));

		// C->D, not exists
		MyWDGFactory.createEdge(C, D, wdg);
		DefaultEdge CD = wdg.getEdge(C, D);
		assertNull(CD);
	}

	@Test
	public void testCreateWDGWDGNodes() throws WDGException{
		WDGNode B = new WDGNode(MyBPELUtils.resolveActivity("B", process));
		WDGNode C = new WDGNode(MyBPELUtils.resolveActivity("C", process));
		WDGNode D = new WDGNode(MyBPELUtils.resolveActivity("D", process));
		WDGNode E = new WDGNode(MyBPELUtils.resolveActivity("E", process));
		Set<WDGNode> nodes = new HashSet<WDGNode>();
		nodes.add(B);
		nodes.add(C);
		nodes.add(D);
		nodes.add(E);
		WDG wdg = WDGFactory.createWDG(nodes);
		
		// Graph should look like V={B,C,D,E}, E={BC, BE, BD, DE, ED}
		DefaultEdge BC = wdg.getEdge(B, C);
		DefaultEdge BE = wdg.getEdge(B, E);
		DefaultEdge BD = wdg.getEdge(B, D);
		DefaultEdge CE = wdg.getEdge(C, E);
		DefaultEdge DE = wdg.getEdge(D, E);
		assertNotNull(BC);
		assertNotNull(BE);
		assertNotNull(BD);
		assertNotNull(CE);
		assertNotNull(DE);
		assertEquals(5, wdg.edgeSet().size());
		
	}
	
	@Test
	public void testCreateWDGActivities() throws WDGException{
		
		// create wdg with the activities set
		Activity B = MyBPELUtils.resolveActivity("B", process);
		Activity C = MyBPELUtils.resolveActivity("C", process);
		Activity D = MyBPELUtils.resolveActivity("D", process);
		Activity E = MyBPELUtils.resolveActivity("E", process);
		List<Activity> actsForWDGNodes = Arrays.asList(new Activity[]{B, C, D, E});
		
		WDG wdg = WDGFactory.createWDG(actsForWDGNodes);
		
		// Graph should look like V={B,C,D,E}, E={BC, BE, BD, DE, ED} 
		DefaultEdge BC = wdg.getEdge(B, C);
		DefaultEdge BE = wdg.getEdge(B, E);
		DefaultEdge BD = wdg.getEdge(B, D);
		DefaultEdge CE = wdg.getEdge(C, E);
		DefaultEdge DE = wdg.getEdge(D, E);
		
		assertNotNull(BC);
		assertNotNull(BE);
		assertNotNull(BD);
		assertNotNull(CE);
		assertNotNull(DE);
		assertEquals(5, wdg.edgeSet().size());
	}

}

class MyWDGFactory extends WDGFactory {

	public static boolean isEquals(Activity act1, Activity act2) {
		return WDGFactory.isEquals(act1, act2);
	}

	public static boolean pathContainIntermediateWdgNode(Path<Activity> currentPath, Set<WDGNode> wdgNodes) {
		return WDGFactory.pathContainIntermediateWdgNode(currentPath, wdgNodes);
	}

	public static void findPath(Path<Activity> path, Activity currentNode, Activity endNode,
			List<Path<Activity>> foundList) {
		WDGFactory.findAllPath(path, currentNode, endNode, foundList);
	}

	public static void createEdge(WDGNode node1, WDGNode node2, WDG wdg) throws CycleFoundException {
		WDGFactory.createEdge(node1, node2, wdg);
	}
}
