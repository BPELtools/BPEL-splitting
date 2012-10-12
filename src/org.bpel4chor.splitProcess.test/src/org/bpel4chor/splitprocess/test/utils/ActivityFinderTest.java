package org.bpel4chor.splitprocess.test.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import junit.framework.Assert;

import org.bpel4chor.splitprocess.exceptions.ActivityNotFoundException;
import org.bpel4chor.splitprocess.utils.ActivityFinder;
import org.bpel4chor.utils.BPEL4ChorConstants;
import org.bpel4chor.utils.BPEL4ChorReader;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.BPELExtensibleElement;
import org.eclipse.bpel.model.BPELPlugin;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.While;
import org.eclipse.bpel.model.resource.BPELResource;
import org.eclipse.bpel.model.resource.BPELResourceFactoryImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
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

public class ActivityFinderTest {

	private static ActivityFinder finder;// test target

	private static Process process;// BPEL process

	private static File testFileDir;// where the test files locate

	private static File outputDir;// where to write the output files

	/**
	 * Init function, run only once
	 * @throws FileNotFoundException 
	 * 
	 * @throws Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws FileNotFoundException  {

		File projectDir = new File("");
		testFileDir = new File(projectDir.getAbsolutePath(), "files");

		outputDir = new File(BPEL4ChorConstants.BPEL4CHOR_DEFAULT_WRITE_DIR);
		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}

		// init eclipse plugin
		BPELPlugin bpelPlugin = new BPELPlugin();
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("bpel", new BPELResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("wsdl", new WSDLResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xsd", new XSDResourceFactoryImpl());

		// load bpel resource
		ResourceSet resourceSet = new ResourceSetImpl();
		URI uri = URI.createFileURI(testFileDir + File.separator + "OrderInfoWithLoop" + File.separator + "bpelContent"
				+ File.separator + "OrderingProcess.bpel");
		BPELResource resource = (BPELResource) resourceSet.createResource(uri);

		// prepare the inputStream
		FileInputStream inputStream = new FileInputStream(new File(testFileDir + File.separator + "OrderInfoWithLoop"
				+ File.separator + "bpelContent", "OrderingProcess.bpel"));

		// read in the BPEL process
		process = BPEL4ChorReader.readBPEL(resource, inputStream);

		System.out.println("BPEL Process " + process.getName() + " in " + uri.toFileString() + " is parsed.");

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {

		// initialise the finder before test
		finder = new ActivityFinder(process);

	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testFindElement() {
		// has already been tested in PartitionSpecUtilTest
	}

	@Test
	public void testFindString() throws ActivityNotFoundException {

		String actName = null;
		EObject parent = null;
		EObject grandparent = null;

		// Find null
		try {
			finder.find(actName);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals(true, e instanceof NullPointerException);
		}
		try {
			finder.find("");
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals(true, e instanceof IllegalArgumentException);
		}

		// Find InvokeJ
		actName = "InvokeJ";
		Activity invokeJ = finder.find(actName);
		Assert.assertEquals(actName, invokeJ.getName());

		parent = invokeJ.eContainer();
		Assert.assertEquals(true, parent instanceof Flow);

		grandparent = parent.eContainer();
		Assert.assertEquals(true, grandparent instanceof While);

		// Find AssignK
		actName = "AssignK";
		Activity assignK = finder.find(actName);
		Assert.assertEquals(actName, assignK.getName());

		parent = invokeJ.eContainer();
		Assert.assertEquals(true, parent instanceof Flow);

		grandparent = parent.eContainer();
		Assert.assertEquals(true, grandparent instanceof While);

		// Find ReceiveA
		actName = "ReceiveA";
		Activity receiveA = finder.find(actName);
		Assert.assertEquals(actName, receiveA.getName());

		parent = receiveA.eContainer();
		Assert.assertEquals(true, parent instanceof Flow);

		// Find ReplyH
		actName = "ReplyH";
		Activity replyH = finder.find(actName);
		Assert.assertEquals(actName, replyH.getName());

		parent = replyH.eContainer();
		Assert.assertEquals(true, parent instanceof Flow);

	}

	@Test
	public void testFindParent() throws ActivityNotFoundException {
		String actName = null;
		BPELExtensibleElement parent = null;

		// find parent of null
		try {
			finder.findParent(null);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals(true, e instanceof NullPointerException);
		}

		// find parent of non-existed activity
		try {
			finder.findParent("CatchMeIfYouCan");
			Assert.fail();// not ok.
		} catch (ActivityNotFoundException e) {
			// it is ok
		}

		// find parent of the root flow "Flow"
		actName = "Flow";
		Assert.assertEquals(true, finder.findParent(actName) instanceof Process);

		// find parent of InvokeG
		actName = "InvokeG";
		parent = finder.findParent(actName);
		Assert.assertEquals(true, parent instanceof Flow);
		Assert.assertEquals(true, ((Flow) parent).getName().equals("Flow"));

		// find parent of AssignK
		actName = "AssignK";
		parent = finder.findParent(actName);
		Assert.assertEquals(true, parent instanceof Flow);
		Assert.assertEquals(true, ((Flow) parent).getName().equals("Flow1"));

	}

}
