package org.bpel4chor.splitprocess.test.pwdg.model;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.bpel4chor.splitprocess.pwdg.model.PWDGNode;
import org.bpel4chor.splitprocess.pwdg.model.WDGNode;
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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class PWDGNodeTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
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
	public void testPWDGNode() {
		PWDGNode node = new PWDGNode();
		assertNotNull(node.getParticipant());
		assertNotNull(node.getWdgNodes());
	}

	@Test
	public void testPWDGNodeStringSetOfWDGNode() {
		Set<WDGNode> wdgnodes = new HashSet<WDGNode>();
		PWDGNode node = new PWDGNode("participant1", wdgnodes);
		assertNotNull(node.getParticipant());
		assertEquals(true,node.getParticipant().equals("participant1"));
		assertNotNull(node.getWdgNodes());
	}

	@Test
	public void testPWDGNodeString() {
		PWDGNode node = new PWDGNode("participant1");
		assertNotNull(node.getParticipant());
		assertEquals(true,node.getParticipant().equals("participant1"));
		assertNotNull(node.getWdgNodes());
	}

	@Test
	public void testAdd() {
		WDGNode wdgnode = new WDGNode(BPELFactory.eINSTANCE.createActivity());
		PWDGNode node = new PWDGNode("participant1");
		node.add(wdgnode);
		assertTrue(node.getWdgNodes().size()==1);
		assertTrue(node.getWdgNodes().iterator().next().equals(wdgnode));
	}

	@Test
	public void testRemove() {
		WDGNode wdgnode = new WDGNode(BPELFactory.eINSTANCE.createActivity());
		PWDGNode node = new PWDGNode("participant1");
		node.add(wdgnode);
		node.remove(wdgnode);
		assertTrue(node.getWdgNodes().isEmpty());
	}

	@Test
	public void testGetParticipant() {
		PWDGNode node = new PWDGNode("participant1");
		assertEquals(true, node.getParticipant().equals("participant1"));
	}

	@Test
	public void testSetParticipant() {
		PWDGNode node = new PWDGNode("participant1");
		node.setParticipant("participant2");
		assertEquals(true, node.getParticipant().equals("participant2"));
	}

	@Test
	public void testGetActivities() {
		WDGNode wdgnode = new WDGNode(BPELFactory.eINSTANCE.createActivity());
		PWDGNode node = new PWDGNode("participant1");
		node.add(wdgnode);
		assertTrue(node.getActivities().size()==1);
	}

	@Test
	public void testGetWdgNodes() {
		WDGNode wdgnode = new WDGNode(BPELFactory.eINSTANCE.createActivity());
		PWDGNode node = new PWDGNode("participant1");
		node.add(wdgnode);
		Set<WDGNode> actual = node.getWdgNodes();
		assertNotNull(actual);
		assertTrue(actual.size()==1);
	}

	@Test
	public void testSetWdgNodes() {
		Set<WDGNode> wdgNodes = new HashSet<WDGNode>();
		WDGNode wdgnode = new WDGNode(BPELFactory.eINSTANCE.createActivity());
		wdgNodes.add(wdgnode);
		PWDGNode node = new PWDGNode("participant1");
		node.setWdgNodes(wdgNodes);
		assertTrue(node.getWdgNodes().equals(wdgNodes));
	}

}
