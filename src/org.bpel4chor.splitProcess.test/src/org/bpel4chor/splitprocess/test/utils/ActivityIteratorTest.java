package org.bpel4chor.splitprocess.test.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.bpel.model.Activity;
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

import de.uni_stuttgart.iaas.bpel.model.utilities.ActivityIterator;

public class ActivityIteratorTest {

	static Process process = null;

	static File testFileDir = null;

	static List<String> actNames = new ArrayList<String>();

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
				+ "\\OrderInfoWithLoop\\bpelContent\\OrderingProcess.bpel");
		Resource resource = resourceSet.getResource(uri, true);
		process = (Process) resource.getContents().get(0);

		// activity list
		actNames.addAll(Arrays.asList("Flow", "ReceiveA", "AssignB", "AssignC", "AssignD", "AssignE", "InvokeF",
				"InvokeG", "ReplyH", "WhileI", "Flow1", "InvokeJ", "AssignK", "AssignDeliverRequest"));
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
	public void testActivityIterator() {

		ActivityIterator actIterator = new ActivityIterator(process);

	}

	@Test
	public void testHasNext() {
		ActivityIterator actIterator = new ActivityIterator(process);
		assertTrue(actIterator.hasNext());
	}

	@Test
	public void testNext() {
		ActivityIterator actIterator = new ActivityIterator(process);
		assertNotNull(actIterator.next());
	}

	@Test
	public void testHasNextAndNextTogether() {
		ActivityIterator actIterator = new ActivityIterator(process);
		while (actIterator.hasNext()) {
			Activity current = actIterator.next();
			assertNotNull(current);
			assertEquals(true, actNames.contains(current.getName()));
			int i = actNames.indexOf(current.getName());
			actNames.remove(i);
		}
		assertTrue(actNames.isEmpty());
	}

	@Test
	public void testReset() {
		ActivityIterator actIterator = new ActivityIterator(process);

		int i = 0;
		int j = 0;

		while (actIterator.hasNext()) {
			actIterator.next();
			i++;
		}

		actIterator.reset();

		while (actIterator.hasNext()) {
			actIterator.next();
			j++;
		}

		assertEquals(i, j);
	}
}
