package org.bpel4chor.splitprocess.test.utils;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.wsdl.WSDLException;
import javax.xml.namespace.QName;

import junit.framework.Assert;

import org.bpel4chor.utils.BPEL4ChorReader;
import org.bpel4chor.utils.MyWSDLUtil;
import org.eclipse.bpel.model.BPELPlugin;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.messageproperties.PropertyAlias;
import org.eclipse.bpel.model.partnerlinktype.PartnerLinkType;
import org.eclipse.bpel.model.resource.BPELResourceFactoryImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.wst.wsdl.Definition;
import org.eclipse.wst.wsdl.Message;
import org.eclipse.wst.wsdl.Operation;
import org.eclipse.wst.wsdl.PortType;
import org.eclipse.wst.wsdl.internal.util.WSDLResourceFactoryImpl;
import org.eclipse.xsd.util.XSDResourceFactoryImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class MyWSDLUtilTest {

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

		defn = BPEL4ChorReader.readWSDL(testFileDir.getAbsolutePath()
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
	public void testGetWSDLOf() throws WSDLException, IOException {
		Definition definition = MyWSDLUtil.getWSDLOf(process);
		Assert.assertNotNull(definition);

	}

	@Test
	public void testFindPortType() {
		PortType porttype = MyWSDLUtil.findPortType(defn, "OrderingProcess");
		Assert.assertNotNull(porttype);
		List<Operation> ops = porttype.getOperations();
		Assert.assertEquals(true, ops.size() == 1);
		Assert.assertEquals(true, ops.get(0).getName().equals("order"));
	}

	@Test
	public void testFindPartnerLinkType() {
		PartnerLinkType plt = MyWSDLUtil.findPartnerLinkType(defn, "OrderingProcessPT");

		Assert.assertNotNull(plt);

		Assert.assertEquals(true, plt.getName().equals("OrderingProcessPT"));
		Assert.assertEquals(true, plt.getRole().get(0).getName().equals("OrderProcessProvider"));
	}

	@Test
	public void testFindPropertyAlias() throws WSDLException, IOException {
		Definition definition = BPEL4ChorReader.readWSDL(testFileDir.getAbsolutePath()
				+ "\\ProcessBla\\bpelContent\\ProcessBla.wsdl");
		QName propertyName = new QName("correlProperty");
		QName messageType = new QName("ProcessBlaRequestMessage");
		PropertyAlias alias = MyWSDLUtil.findPropertyAlias(definition, propertyName, messageType, "payload");
		Assert.assertNotNull(alias);

		PropertyAlias notExistAlias = MyWSDLUtil.findPropertyAlias(definition, propertyName, messageType, "something not exists");
		Assert.assertNull(notExistAlias);
	}

	@Test
	public void testResolveMessage() {
		QName msgQName = new QName(defn.getTargetNamespace(), "orderRequest");
		Message msg = MyWSDLUtil.resolveMessage(defn, msgQName);
		assertNotNull(msg);
		assertTrue(msg.getQName().getLocalPart().equals("orderRequest"));
	}
}
