package org.bpel4chor.splitprocess.test.utils;

import static org.junit.Assert.*;

import java.io.File;

import javax.xml.namespace.QName;

import org.bpel4chor.splitprocess.exceptions.ActivityNotFoundException;
import org.bpel4chor.splitprocess.utils.ActivityFinder;
import org.bpel4chor.utils.BPEL4ChorReader;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Assign;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.BPELPlugin;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Source;
import org.eclipse.bpel.model.Target;
import org.eclipse.bpel.model.messageproperties.Property;
import org.eclipse.bpel.model.proxy.PropertyProxy;
import org.eclipse.bpel.model.resource.BPELResourceFactoryImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.wst.wsdl.Definition;
import org.eclipse.wst.wsdl.internal.util.WSDLResourceFactoryImpl;
import org.eclipse.xsd.XSDSimpleTypeDefinition;
import org.eclipse.xsd.util.XSDConstants;
import org.eclipse.xsd.util.XSDResourceFactoryImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.uni_stuttgart.iaas.bpel.model.utilities.MyBPELUtils;
import de.uni_stuttgart.iaas.bpel.model.utilities.MyWSDLUtil;
import de.uni_stuttgart.iaas.bpel.model.utilities.exceptions.AmbiguousPropertyForLinkException;

public class MyBPELUtilsTest {
	static File testFileDir = null;

	static Process process = null;

	static Definition defn = null;

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
		URI uri = URI.createFileURI(testFileDir.getAbsolutePath() + "\\OrderInfo\\bpelContent\\OrderingProcess.bpel");
		Resource resource = resourceSet.getResource(uri, true);
		process = (Process) resource.getContents().get(0);

		defn = MyWSDLUtil.readWSDL(testFileDir.getAbsolutePath()
				+ "\\OrderInfo\\bpelContent\\OrderingProcess.wsdl");
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
	public void testFindActivityInProcess() {
		// find activity "AssignB"
		Activity assignB = MyBPELUtils.resolveActivity("B", process);
		// exptected Not Null, Name = "B", Instanceof Assign
		assertNotNull(assignB);
		assertTrue(assignB.getName().equals("B"));
		assertTrue(assignB instanceof Assign);
	}
	
	@Test
	public void testGetFirstProperty() {
		
		// property in BPEL
		Property property = MyBPELUtils.findFirstProperty(process);
		assertNotNull(property);
		assertEquals(true, property.getName().equals("correlProperty"));
		
		// get property in wsdl
		QName propertyQName = property.getQName();
		Property propertyInWSDL = MyWSDLUtil.resolveBPELProperty(defn, propertyQName);
		XSDSimpleTypeDefinition xsd = (XSDSimpleTypeDefinition)propertyInWSDL.getType();
		
		// we have pre-defined that the property is type of boolean
		assertEquals(true, xsd.getName().equals("string"));
	}
	
	@Test
	public void testGetLinkInActivityTarget() throws AmbiguousPropertyForLinkException {
		Link link = MyBPELUtils.findLinkInActivityTarget("ReceiveA2AssignB", process);
		assertNotNull(link);
		
		Source source = link.getSources().get(0);
		Activity sourceActivity = source.getActivity();
		assertEquals("A", sourceActivity.getName());
		
		Target target = link.getTargets().get(0);
		Activity targetActivity = target.getActivity();
		assertEquals("B", targetActivity.getName());
		
		Link notexistLink = MyBPELUtils.findLinkInActivityTarget("XYZ", process);
		assertNull(notexistLink);
	}
	
	@Test
	public void testGetFirstParentFlow() throws AmbiguousPropertyForLinkException {
		Link link = MyBPELUtils.findLinkInActivityTarget("ReceiveA2AssignB", process);
		Flow flow = MyBPELUtils.findFirstParentFlow(link);
		
		assertNotNull(flow);
		assertEquals("Flow", flow.getName());
		assertEquals(true, flow instanceof Flow);
		
		Link notExistLink = BPELFactory.eINSTANCE.createLink();
		notExistLink.setName("notExistedLink");
		Flow notExistFlow = MyBPELUtils.findFirstParentFlow(notExistLink);
		assertNull(notExistFlow);
	}
	
	@Test
	public void testResolveLink() throws ActivityNotFoundException {
		ActivityFinder finder = new ActivityFinder(process);
		Flow flow = (Flow) finder.find("Flow");
		Link link = MyBPELUtils.resolveLink(flow, "InvokeF2ReplyH");
		assertNotNull(link);
		assertEquals("InvokeF2ReplyH", link.getName());
		
		
	}
	
	
	
}
