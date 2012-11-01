package org.bpel4chor.splitprocess.test.utils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.bpel4chor.splitprocess.exceptions.LinkNotFoundException;
import org.bpel4chor.splitprocess.utils.LinkFinder;
import org.eclipse.bpel.model.BPELPlugin;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Target;
import org.eclipse.bpel.model.While;
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

import de.uni_stuttgart.iaas.bpel.model.utilities.exceptions.AmbiguousPropertyForLinkException;

public class LinkFinderTest {

	static File testFileDir = null;
	static Process process = null;
	static LinkFinder finder = null;
	
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
		
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		testFileDir = null;
		process = null;
		finder = null;
	}

	@Before
	public void setUp() throws Exception {
		
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testLinkFinder() {
		
		try {
			finder = new LinkFinder(null);
			fail();
		} catch (Exception e) {
		}
		
		finder = new LinkFinder(process);
	}

	@Test
	public void testFindLinkInActivityTarget() throws LinkNotFoundException, AmbiguousPropertyForLinkException {
		
		finder = new LinkFinder(process);
		
		// find with null, should be NPE
		try {
			Link findwithnull = finder.findLinkInActivityTarget(null);
			fail();
		} catch (Exception e) {
			if(e instanceof NullPointerException) {
				// it is supposed to be
			} else {
				fail("finder should be throwing a NullPointerException, with null as arguemtn. exception:" + e.getClass());
			}
		} 
		
		// find wtih "InvokeF2WhileI", should be found
		Link invokeF2WhileI = finder.findLinkInActivityTarget("InvokeF2WhileI");
		assertNotNull(invokeF2WhileI);
		assertTrue(invokeF2WhileI.getName().equals("InvokeF2WhileI"));
		assertNotNull(invokeF2WhileI.getTargets());
		assertTrue(invokeF2WhileI.getTargets().size()==1);
		Target target = invokeF2WhileI.getTargets().get(0); 
		assertTrue( target.getActivity() instanceof While);
		
		// find with "CatchMeIfYouCan", should not be found
		try {
			Link catchMeIfYouCan = finder.findLinkInActivityTarget("CatchMeIfYouCan");
			fail();
		} catch (Exception e) {
			if(e instanceof LinkNotFoundException){
				// it is supposed to be
			} else {
				fail("finder should throw out a LinkNotFoundException. exception:" + e.getClass());
			}
		}
		
	}

}
