package org.bpel4chor.splitprocess.test.pwdg.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.xml.bind.JAXBException;

import de.uni_stuttgart.iaas.bpel.model.utilities.MyBPELUtils;

import org.bpel4chor.splitprocess.exceptions.PartitionSpecificationException;
import org.bpel4chor.splitprocess.partition.model.Participant;
import org.bpel4chor.splitprocess.partition.model.PartitionSpecification;
import org.bpel4chor.splitprocess.partition.representation.PartitionSpecReader;
import org.bpel4chor.splitprocess.pwdg.Path;
import org.bpel4chor.splitprocess.pwdg.model.PWDGNode;
import org.bpel4chor.splitprocess.pwdg.model.WDG;
import org.bpel4chor.splitprocess.pwdg.model.WDGNode;
import org.bpel4chor.splitprocess.pwdg.util.PWDGNodeConstructor;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.BPELPlugin;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.resource.BPELResourceFactoryImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.wst.wsdl.internal.util.WSDLResourceFactoryImpl;
import org.eclipse.xsd.util.XSDResourceFactoryImpl;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph.CycleFoundException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class PWDGNodeConstructorTest {

	static File testFileDir = null;

	static Process process = null;

	static PartitionSpecification partition = null;

	static WDG wdg = null;

	static Map<Participant, WDGNode> part2rootMap = null;

	static Map<Participant, Set<WDGNode>> part2wdgNodeMap = null;

	static MyPWDGNodeConstructor constructor = null;

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

	// temporary root
	static WDGNode tempRoot1 = null;
	static WDGNode tempRoot2 = null;
	static WDGNode tempRoot3 = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		File projectDir = new File("");
		testFileDir = new File(projectDir.getAbsolutePath(), "files");

		// init
		BPELPlugin bpelPlugin = new BPELPlugin();
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("bpel", new BPELResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("wsdl", new WSDLResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xsd", new XSDResourceFactoryImpl());

		String uriBPEL = testFileDir.getAbsolutePath() + "\\PWDGProcess\\bpelContent\\PWDGProcess.bpel";
		process = loadBPEL(uriBPEL);

		String uriPartition = testFileDir.getAbsolutePath() + "\\PWDGProcess\\bpelContent\\Partition.xml";
		partition = loadPartitionSpec(uriPartition, process);

		initPWDGNodeConstructorPrerequisites();

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		// initialise PWDGNodeConstructor before test
		constructor = new MyPWDGNodeConstructor(wdg, part2rootMap, part2wdgNodeMap);
	}

	@After
	public void tearDown() throws Exception {
	}

	protected static void initPWDGNodeConstructorPrerequisites() throws CycleFoundException {
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

		// get participants
		Participant participant1 = partition.getParticipant("participant1");
		Participant participant2 = partition.getParticipant("participant2");
		Participant participant3 = partition.getParticipant("participant3");

		// temporary root wdg node for each participant
		Activity root1 = BPELFactory.eINSTANCE.createActivity();
		root1.setName("participant1Root");
		tempRoot1 = new WDGNode(root1);

		Activity root2 = BPELFactory.eINSTANCE.createActivity();
		root1.setName("participant2Root");
		tempRoot2 = new WDGNode(root2);

		Activity root3 = BPELFactory.eINSTANCE.createActivity();
		root1.setName("participant3Root");
		tempRoot3 = new WDGNode(root3);

		part2rootMap = new HashMap<Participant, WDGNode>();
		part2rootMap.put(participant1, tempRoot1);
		part2rootMap.put(participant2, tempRoot2);
		part2rootMap.put(participant3, tempRoot3);

		// add temporary root into wdg first
		wdg.addVertex(tempRoot1);
		wdg.addVertex(tempRoot2);
		wdg.addVertex(tempRoot3);

		// combine root with the other nodes
		wdg.addDagEdge(tempRoot1, U);
		wdg.addDagEdge(tempRoot1, V);
		wdg.addDagEdge(tempRoot2, T);
		wdg.addDagEdge(tempRoot3, R);
		wdg.addDagEdge(tempRoot3, S);
		wdg.addDagEdge(tempRoot3, X);

		// add participant to wdgNodes map
		part2wdgNodeMap = new HashMap<Participant, Set<WDGNode>>();
		Set<WDGNode> wdgNodesP1 = new HashSet<WDGNode>();
		wdgNodesP1.add(tempRoot1);
		wdgNodesP1.add(U);
		wdgNodesP1.add(V);
		Set<WDGNode> wdgNodesP2 = new HashSet<WDGNode>();
		wdgNodesP2.add(tempRoot2);
		wdgNodesP2.add(T);
		wdgNodesP2.add(W);
		Set<WDGNode> wdgNodesP3 = new HashSet<WDGNode>();
		wdgNodesP3.add(tempRoot3);
		wdgNodesP3.add(R);
		wdgNodesP3.add(S);
		wdgNodesP3.add(X);
		wdgNodesP3.add(Y);
		wdgNodesP3.add(Z);
		part2wdgNodeMap.put(participant1, wdgNodesP1);
		part2wdgNodeMap.put(participant2, wdgNodesP2);
		part2wdgNodeMap.put(participant3, wdgNodesP3);

		// now initialise PWDGNodeConstructor
		// constructor = new MyPWDGNodeConstructor(wdg, part2rootMap,
		// part2wdgNodeMap);

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
	public void testIsInParticipant() {
		Participant p1 = partition.getParticipant("participant1");
		Participant p2 = partition.getParticipant("participant2");
		// U and V are in participant1
		assertTrue(constructor.isInParticipant(U, p1));
		assertTrue(constructor.isInParticipant(V, p1));
		// T is in participant2
		assertFalse(constructor.isInParticipant(T, p1));
		assertTrue(constructor.isInParticipant(T, p2));
	}

	@Test
	public void testGetTargetsWDGNode() {

		Set<WDGNode> targets = constructor.getTargets(W);
		assertNotNull(targets);
		assertEquals(2, targets.size());
		// targets of W are X and V
		for (WDGNode node : targets) {
			assertTrue(node.equals(X) || node.equals(V));
		}
	}

	@Test
	public void testPathViaOtherParticipant() {
		// test path constraint violation

		// r1 -> v, there is another path via other participant
		assertTrue(constructor.pathViaOtherParticipant(tempRoot1, V, partition.getParticipant("participant1")));
		// r1-> u, no path constraint violation
		assertFalse(constructor.pathViaOtherParticipant(tempRoot1, U, partition.getParticipant("participant1")));
	}

	@Test
	public void testAddRecursivelyToPWDGNode() throws PartitionSpecificationException {
		//
		// test with participant 3, {X, Y, Z} should be recursively added to a
		// pwdgNode
		//

		// setup toCheck
		Participant p3 = partition.getParticipant("participant3");
		constructor.setToCheck(part2wdgNodeMap.get(p3));

		// setup pwdgNode
		PWDGNode pwdgNode = new PWDGNode(p3.getName());
		pwdgNode.add(X);
		// run...
		constructor.addRecursivelyToPWDGNode(X, Y, pwdgNode, p3);

		// test for pwdgNode=(p3, {X, Y, Z})
		assertEquals(true, pwdgNode.getParticipant().equals(p3.getName()));
		assertEquals(3, pwdgNode.getWdgNodes().size());
		Set<WDGNode> actual = pwdgNode.getWdgNodes();
		Set<WDGNode> expected = new HashSet<WDGNode>();
		expected.add(X);
		expected.add(Y);
		expected.add(Z);
		for (WDGNode node : expected) {
			assertTrue(actual.contains(node));
		}

		//
		// test path constraint violation
		// with participant 3, start with tempRoot3, and current = X,
		//

		// setup toCheck
		constructor.setToCheck(part2wdgNodeMap.get(p3));

		// setup pwdgNode
		pwdgNode = new PWDGNode(p3.getName());
		pwdgNode.add(tempRoot3);
		// run...
		constructor.addRecursivelyToPWDGNode(tempRoot3, X, pwdgNode, p3);
		assertTrue(pwdgNode.getWdgNodes().size() == 1);
		assertEquals(pwdgNode.getWdgNodes().iterator().next(), tempRoot3);
	}

	@Test
	public void testPWDGNodeConstructor() throws Exception {
		try {
			constructor = new MyPWDGNodeConstructor(wdg, part2rootMap, null);
			fail();
		} catch (Exception e) {
			// there should be a NPE
			if ((e instanceof NullPointerException) == false)
				throw new Exception(e);
		}
	}

	@Test
	public void testFormNodes() throws PartitionSpecificationException {
		constructor = new MyPWDGNodeConstructor(wdg, part2rootMap, part2wdgNodeMap);
		Set<PWDGNode> pwdgNodes = constructor.formNodes();
		assertNotNull(pwdgNodes);
		assertEquals(5, pwdgNodes.size());

		// 2 in participant 1
		int expect = 2;
		int actual = 0;
		for (PWDGNode node : pwdgNodes) {
			if (node.getParticipant().equals("participant1"))
				actual++;
		}
		assertEquals(expect, actual);

		// 1 in participant 2
		expect = 1;
		actual = 0;
		PWDGNode pwdgNode = null;
		for (PWDGNode node : pwdgNodes) {
			if (node.getParticipant().equals("participant2")) {
				actual++;
				pwdgNode = node;
			}

		}
		assertEquals(expect, actual);
		// test the pwdgNode = {temproot2, T, W}
		assertNotNull(pwdgNode);
		Set<WDGNode> wdgNodes = pwdgNode.getWdgNodes();
		assertEquals(3, wdgNodes.size());
		for (WDGNode wnode : wdgNodes) {
			assertTrue(wnode.equals(tempRoot2) || wnode.equals(T) || wnode.equals(W));
		}

		// 2 in participant 3
		expect = 2;
		actual = 0;
		for (PWDGNode node : pwdgNodes) {
			if (node.getParticipant().equals("participant3"))
				actual++;
		}
		assertEquals(expect, actual);
	}

}

class MyPWDGNodeConstructor extends PWDGNodeConstructor {

	public MyPWDGNodeConstructor(WDG wdg, Map<Participant, WDGNode> part2rootMap,
			Map<Participant, Set<WDGNode>> part2wdgNodeMap) {
		super(wdg, part2rootMap, part2wdgNodeMap);
	}

	@Override
	protected void addRecursivelyToPWDGNode(WDGNode parent, WDGNode current, PWDGNode pwdgNode, Participant participant)
			throws PartitionSpecificationException {
		super.addRecursivelyToPWDGNode(parent, current, pwdgNode, participant);
	}

	@Override
	protected Set<WDGNode> getTargets(WDGNode source) {
		return super.getTargets(source);
	}

	@Override
	protected boolean pathViaOtherParticipant(WDGNode source, WDGNode target, Participant participant) {
		return super.pathViaOtherParticipant(source, target, participant);
	}

	@Override
	protected boolean findPathViaOtherParticipant(Path<WDGNode> visited, WDGNode current, WDGNode end,
			Participant participant) {
		return super.findPathViaOtherParticipant(visited, current, end, participant);
	}

	@Override
	protected boolean isInParticipant(WDGNode node, Participant participant) {
		return super.isInParticipant(node, participant);
	}

	@Override
	protected Set<WDGNode> getToCheck() {
		return super.getToCheck();
	}

	@Override
	protected Queue<WDGNode> getQueue() {
		return super.getQueue();
	}

	@Override
	protected WDGNode getRoot() {
		return super.getRoot();
	}

	@Override
	protected void setToCheck(Set<WDGNode> toCheck) {
		super.setToCheck(toCheck);
	}

	@Override
	protected void setQueue(Queue<WDGNode> queue) {
		super.setQueue(queue);
	}

	@Override
	protected void setRoot(WDGNode root) {
		super.setRoot(root);
	}

}
