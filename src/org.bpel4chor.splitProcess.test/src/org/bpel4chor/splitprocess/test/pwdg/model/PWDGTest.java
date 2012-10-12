package org.bpel4chor.splitprocess.test.pwdg.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashSet;
import java.util.Set;

import org.bpel4chor.splitprocess.pwdg.model.PWDG;
import org.bpel4chor.splitprocess.pwdg.model.PWDGNode;
import org.bpel4chor.splitprocess.pwdg.model.WDGNode;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.BPELFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class PWDGTest {

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
	public void testGetNode() {
		// construct a artificial pwdg
		Set<WDGNode> wdgnodes = new HashSet<WDGNode>();
		PWDGNode node = new PWDGNode("participant1", wdgnodes);
		PWDG pwdg = new PWDG();
		pwdg.addVertex(node);
		// test
		Set<PWDGNode> actual = pwdg.getNodeSet("participant1"); 
		assertNotNull(actual);
		assertEquals(1, actual.size());
		assertEquals(node, actual.iterator().next());
		
	}

	@Test
	public void testGetNodeWith() {
		// construct a artificial pwdg
		
		// node 1
		Set<WDGNode> wdgnodes1 = new HashSet<WDGNode>();
		PWDGNode node1 = new PWDGNode("participant1", wdgnodes1);
		
		// node 2
		Set<WDGNode> wdgnodes2 = new HashSet<WDGNode>();
		Activity activity = BPELFactory.eINSTANCE.createActivity();
		WDGNode wdgNode = new WDGNode(activity);
		wdgnodes2.add(wdgNode);
		
		PWDGNode node2 = new PWDGNode("participant2", wdgnodes2);
		
		// pwdg with node1, node2
		PWDG pwdg = new PWDG();
		pwdg.addVertex(node1);
		pwdg.addVertex(node2);
		
		// get the node2 with "participant2" and the activity
		PWDGNode actual = pwdg.getNodeWith("participant2", wdgNode);
		
		// actual should be equal to the node2 
		assertNotNull(actual);
		assertEquals(node2, actual);
		// and the activity too
		assertEquals(wdgNode.activity(), actual.getActivities().iterator().next());
	}

}
