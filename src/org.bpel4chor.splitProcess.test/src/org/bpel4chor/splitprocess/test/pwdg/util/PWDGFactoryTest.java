package org.bpel4chor.splitprocess.test.pwdg.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import de.uni_stuttgart.iaas.bpel.model.utilities.MyBPELUtils;
import de.uni_stuttgart.iaas.bpel.model.utilities.MyWSDLUtil;

import org.bpel4chor.splitprocess.exceptions.PWDGException;
import org.bpel4chor.splitprocess.exceptions.PartitionSpecificationException;
import org.bpel4chor.splitprocess.partition.model.Participant;
import org.bpel4chor.splitprocess.partition.model.PartitionSpecification;
import org.bpel4chor.splitprocess.partition.representation.PartitionSpecReader;
import org.bpel4chor.splitprocess.pwdg.model.PWDG;
import org.bpel4chor.splitprocess.pwdg.model.PWDGNode;
import org.bpel4chor.splitprocess.pwdg.model.WDG;
import org.bpel4chor.splitprocess.pwdg.model.WDGNode;
import org.bpel4chor.splitprocess.pwdg.util.PWDGFactory;
import org.bpel4chor.utils.BPEL4ChorReader;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.BPELPlugin;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.resource.BPELResourceFactoryImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.wst.wsdl.Definition;
import org.eclipse.wst.wsdl.internal.util.WSDLResourceFactoryImpl;
import org.eclipse.xsd.util.XSDResourceFactoryImpl;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph.CycleFoundException;
import org.jgrapht.graph.DefaultEdge;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class PWDGFactoryTest {

	static File testFileDir = null;

	static Process process = null;

	static Definition definition = null;

	static PartitionSpecification partition = null;
	
	static WDG wdg = null;
	
	// new wdgnodes
	static WDGNode U = null;
	static WDGNode R = null;
	static WDGNode S = null;
	static WDGNode T = null;
	static WDGNode W = null;
	static WDGNode V = null;
	static WDGNode X = null;
	static WDGNode Y = null;
	static WDGNode Z = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		//
		// use the process "PWDGProcess"
		//

		File projectDir = new File("");
		testFileDir = new File(projectDir.getAbsolutePath(), "files");

		// init
		BPELPlugin bpelPlugin = new BPELPlugin();
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("bpel", new BPELResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("wsdl", new WSDLResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xsd", new XSDResourceFactoryImpl());

		// load bpel resource
		String bpelURI = testFileDir.getAbsolutePath() + "\\PWDGProcess\\bpelContent\\PWDGProcess.bpel";
		process = loadBPEL(bpelURI);

		definition = MyWSDLUtil.readWSDL(testFileDir.getAbsolutePath()
				+ "\\OrderInfoSimple2\\bpelContent\\OrderingProcessSimple2.wsdl");

		// partition specification
		String partitionURI = testFileDir.getAbsolutePath()	+ "\\PWDGProcess\\bpelContent\\Partition.xml";
		partition = loadPartitionSpec(partitionURI, process);
		
		
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		// wdg
		initWDG();
	}

	@After
	public void tearDown() throws Exception {
	}
	
	protected static void initWDG() throws CycleFoundException {
		//
		// prepare the artificial wdg, part2rootMap, and part2wdgNodeMap for
		// test using the process PWDGProcess
		//

		// new wdgnodes
		U = new WDGNode(getActivity("U"));
		R = new WDGNode(getActivity("R"));
		S = new WDGNode(getActivity("S"));
		T = new WDGNode(getActivity("T"));
		W = new WDGNode(getActivity("W"));
		V = new WDGNode(getActivity("V"));
		X = new WDGNode(getActivity("X"));
		Y = new WDGNode(getActivity("Y"));
		Z = new WDGNode(getActivity("Z"));

		// new wdg
		wdg = new WDG();
		wdg.addVertex(U);
		wdg.addVertex(R);
		wdg.addVertex(S);
		wdg.addVertex(T);
		wdg.addVertex(W);
		wdg.addVertex(V);
		wdg.addVertex(X);
		wdg.addVertex(Y);
		wdg.addVertex(Z);

		// new edges
		wdg.addDagEdge(U, T);
		wdg.addDagEdge(R, T);
		wdg.addDagEdge(S, T);
		wdg.addDagEdge(T, W);
		wdg.addDagEdge(W, V);
		wdg.addDagEdge(W, X);
		wdg.addDagEdge(V, Y);
		wdg.addDagEdge(X, Y);
		wdg.addDagEdge(Y, Z);

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

	protected static Activity getActivity(String name) {
		Activity act = MyBPELUtils.resolveActivity(name, process);
		return act;
	}
	

	@Test
	public void testHasIncomingEdgeFromSamePartition() {
		// create wdgnodes in participant 2
		Set<WDGNode> nodesInPart2 = new HashSet<WDGNode>();
		
		nodesInPart2.add(T);
		nodesInPart2.add(W);
		
		// test subject T
		assertFalse(MyPWDGFactory.hasIncomingEdgeFromSamePartition(T, nodesInPart2, wdg));
		assertTrue(MyPWDGFactory.hasIncomingEdgeFromSamePartition(W, nodesInPart2, wdg));
		
	}
	
	@Test
	public void testHasWdgEdge() {
		// create pwdgNode1 and pwdgNode2
		PWDGNode p1 = new PWDGNode();
		p1.add(R);
		p1.add(S);
		
		PWDGNode p2 = new PWDGNode();
		p2.add(T);
		p2.add(W);
		
		PWDGNode p3 = new PWDGNode();
		p3.add(X);
		p3.add(Y);
		
		// there is edge p1->p2
		assertTrue(MyPWDGFactory.hasWDGEdgeBetween(p1, p2, wdg));
		// no edge from p1 to p3
		assertFalse(MyPWDGFactory.hasWDGEdgeBetween(p1, p3, wdg));
	}

	@Test
	public void testInsertTempRootNode() throws CycleFoundException, PartitionSpecificationException {
		// provide wdg, part2RootMap, part2wdgnodeMap, process, partitionSpec
		
		// participant to WDG node map
		Map<Participant, Set<WDGNode>> part2WDGNodeMap = new HashMap<Participant, Set<WDGNode>>();

		// temporary root nodes
		Map<Participant, WDGNode> part2RootMap = new HashMap<Participant, WDGNode>();
		
		MyPWDGFactory.insertTempRootNode(wdg, part2RootMap, part2WDGNodeMap, process, partition);
		
		// test part2rootMap = {r1, r2, r3}
		assertEquals(3, part2RootMap.size());
		
		// test part2wdgnodeMap, partticipant1->{r1, u, v}
		Set<WDGNode> wdgNodes = part2WDGNodeMap.get(partition.getParticipant("participant1"));
		assertNotNull(wdgNodes);
		assertEquals(3, wdgNodes.size());
		for(WDGNode node : wdgNodes){
			assertTrue(node.equals(U) || node.equals(V) || node.getName().contains("RootNode"));
			assertTrue(wdg.vertexSet().contains(node));
			if(node.getName().endsWith("RootNode")) {
				// root node is combined to U and V
				Set<DefaultEdge> outEdges = wdg.outgoingEdgesOf(node);
				assertNotNull(outEdges);
				assertEquals(2, outEdges.size());
			}
		}
	}

	@Test
	public void testFormPWDGNodes() throws CycleFoundException, PartitionSpecificationException {
		// provide wdg, part2RootMap, part2wdgnodeMap, process, partitionSpec
		
		// participant to WDG node map
		Map<Participant, Set<WDGNode>> part2WDGNodeMap = new HashMap<Participant, Set<WDGNode>>();

		// temporary root nodes
		Map<Participant, WDGNode> part2RootMap = new HashMap<Participant, WDGNode>();
		
		// insertTempRootNode
		MyPWDGFactory.insertTempRootNode(wdg, part2RootMap, part2WDGNodeMap, process, partition);
		
		// now formWDGNodes
		Set<PWDGNode> pwdgNodes = MyPWDGFactory.formPWDGNodes(wdg, part2RootMap, part2WDGNodeMap);
		
		// test it, size = 5, the rest is already tested by PWDGNodeConstructorTest
		assertNotNull(pwdgNodes);
		assertEquals(5, pwdgNodes.size());
		
	}
	
	@Test
	public void testRemoveTempRootNode() throws CycleFoundException, PartitionSpecificationException {
		// participant to WDG node map
		Map<Participant, Set<WDGNode>> part2WDGNodeMap = new HashMap<Participant, Set<WDGNode>>();

		// temporary root nodes
		Map<Participant, WDGNode> part2RootMap = new HashMap<Participant, WDGNode>();
		
		// insertTempRootNode
		MyPWDGFactory.insertTempRootNode(wdg, part2RootMap, part2WDGNodeMap, process, partition);
		
		// formWDGNodes
		Set<PWDGNode> pwdgNodes = MyPWDGFactory.formPWDGNodes(wdg, part2RootMap, part2WDGNodeMap);
		
		// remove temporary root
		MyPWDGFactory.removeTempRootNode(wdg, part2RootMap, pwdgNodes);
		
		// now, the root nodes should not exist in both pwdg nodes and wdg
		for(WDGNode node : part2RootMap.values()) {
			assertFalse(wdg.vertexSet().contains(node));
			for(PWDGNode pnode : pwdgNodes) {
				assertFalse(pnode.getWdgNodes().contains(node));
			}
		}
	}
	
	@Test
	public void testCreatePwdgEdge() throws CycleFoundException, PartitionSpecificationException {
		// participant to WDG node map
		Map<Participant, Set<WDGNode>> part2WDGNodeMap = new HashMap<Participant, Set<WDGNode>>();

		// temporary root nodes
		Map<Participant, WDGNode> part2RootMap = new HashMap<Participant, WDGNode>();
		
		// insertTempRootNode
		MyPWDGFactory.insertTempRootNode(wdg, part2RootMap, part2WDGNodeMap, process, partition);
		
		// formWDGNodes
		Set<PWDGNode> pwdgNodes = MyPWDGFactory.formPWDGNodes(wdg, part2RootMap, part2WDGNodeMap);
		
		// remove temporary root
		MyPWDGFactory.removeTempRootNode(wdg, part2RootMap, pwdgNodes);
		
		// new pwdg
		PWDG pwdg = new PWDG();
		for (PWDGNode v : pwdgNodes)
			pwdg.addVertex(v);
		
		// create edges
		MyPWDGFactory.createPwdgEdge(pwdg, wdg);
		
		// pwdgNode1 = {participant3, {R, S}}, pwdgNode2 = {participant2, {T, W}}
		PWDGNode p1 = pwdg.getNodeWith("participant3", R);
		PWDGNode p2 = pwdg.getNodeWith("participant2", T);
		PWDGNode p3 = pwdg.getNodeWith("participant3", X);
		
		// there should be edge p1->p2
		assertNotNull(pwdg.getEdge(p1, p2));
		 
		// non edge between p1,p3
		assertNull(pwdg.getEdge(p1, p3));
	}
	
	@Test
	public void testCreatePWDG() throws PWDGException  {
		PWDG pwdg = PWDGFactory.createPWDG(wdg, process, partition);
		assertNotNull(pwdg);
		assertEquals(5, pwdg.vertexSet().size());
		
		// pwdgNode1 = {participant3, {R, S}}, pwdgNode2 = {participant2, {T, W}}
		PWDGNode p1 = pwdg.getNodeWith("participant3", R);
		PWDGNode p2 = pwdg.getNodeWith("participant2", T);
		PWDGNode p3 = pwdg.getNodeWith("participant3", X);
		
		// there should be edge p1->p2
		assertNotNull(pwdg.getEdge(p1, p2));
		 
		// non edge between p1,p3
		assertNull(pwdg.getEdge(p1, p3));
		
	}
}

class MyPWDGFactory extends PWDGFactory {

	protected static boolean hasIncomingEdgeFromSamePartition(WDGNode nodeInPart, Set<WDGNode> nodesCurrPart, WDG wdg) {
		return PWDGFactory.hasIncomingEdgeFromSamePartition(nodeInPart, nodesCurrPart, wdg);
	}
	
	protected static boolean hasWDGEdgeBetween(PWDGNode n1, PWDGNode n2, WDG wdg){
		return PWDGFactory.hasWdgEdgeBetween(n1, n2, wdg);
	}
	
	protected static void insertTempRootNode(WDG wdg, Map<Participant, WDGNode> part2RootMap,
			Map<Participant, Set<WDGNode>> part2wdgNodeMap, Process process, PartitionSpecification partitionSpec)
			throws CycleFoundException, PartitionSpecificationException {
		PWDGFactory.insertTempRootNode(wdg, part2RootMap, part2wdgNodeMap, process, partitionSpec);
	}
	
	protected static Set<PWDGNode> formPWDGNodes(WDG wdg, Map<Participant, WDGNode> part2RootMap,
			Map<Participant, Set<WDGNode>> part2wdgNodeMap) throws PartitionSpecificationException {
		return PWDGFactory.formPWDGNodes(wdg, part2RootMap, part2wdgNodeMap);
	}
	
	protected static void removeTempRootNode(WDG wdg, Map<Participant, WDGNode> part2RootMap, Set<PWDGNode> pwdgNodes) {
		PWDGFactory.removeTempRootNode(wdg, part2RootMap, pwdgNodes);
	}
	
	protected static void createPwdgEdge(PWDG pwdg, WDG wdg) throws CycleFoundException {
		PWDGFactory.createPwdgEdge(pwdg, wdg);
	}
}
