package org.bpel4chor.splitprocess.test.fragmentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import javax.wsdl.WSDLException;
import javax.xml.namespace.QName;

import de.uni_stuttgart.iaas.bpel.model.utilities.MyBPELUtils;
import de.uni_stuttgart.iaas.bpel.model.utilities.MyWSDLUtil;

import org.bpel4chor.splitprocess.fragmentation.FragmentFactory;
import org.bpel4chor.utils.BPEL4ChorModelConstants;
import org.bpel4chor.utils.BPEL4ChorReader;
import org.bpel4chor.utils.BPEL4ChorWriter;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.BPELPlugin;
import org.eclipse.bpel.model.From;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.PartnerLink;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Receive;
import org.eclipse.bpel.model.Variable;
import org.eclipse.bpel.model.Variables;
import org.eclipse.bpel.model.messageproperties.Property;
import org.eclipse.bpel.model.messageproperties.PropertyAlias;
import org.eclipse.bpel.model.partnerlinktype.PartnerLinkType;
import org.eclipse.bpel.model.partnerlinktype.PartnerlinktypeFactory;
import org.eclipse.bpel.model.partnerlinktype.Role;
import org.eclipse.bpel.model.resource.BPELResourceFactoryImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.wst.wsdl.Definition;
import org.eclipse.wst.wsdl.Message;
import org.eclipse.wst.wsdl.Operation;
import org.eclipse.wst.wsdl.Part;
import org.eclipse.wst.wsdl.PortType;
import org.eclipse.wst.wsdl.internal.util.WSDLResourceFactoryImpl;
import org.eclipse.xsd.util.XSDResourceFactoryImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class FragmentFactoryTest {
	static File testFileDir = null;

	static Process process = null;

	static Definition defn = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		File projectDir = new File("");
		testFileDir = new File(projectDir.getAbsolutePath(), "files");

		// init
		BPELPlugin bpelPlugin = new BPELPlugin();
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("bpel",
				new BPELResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("wsdl",
				new WSDLResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xsd",
				new XSDResourceFactoryImpl());

		// load bpel resource
		ResourceSet resourceSet = new ResourceSetImpl();
		URI uri = URI.createFileURI(testFileDir.getAbsolutePath()
				+ "\\OrderInfo\\bpelContent\\OrderingProcess.bpel");
		Resource resource = resourceSet.getResource(uri, true);
		process = (Process) resource.getContents().get(0);

		// load definition
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
	public void testCreateControlLinkMessage() {

		Message ctrlLinkMessage = FragmentFactory.createControlLinkMessage(process
				.getTargetNamespace());
		assertNotNull(ctrlLinkMessage);
		assertEquals(new QName(process.getTargetNamespace(),
				BPEL4ChorModelConstants.CONTROL_LINK_MESSAGE_NAME), ctrlLinkMessage.getQName());
		assertEquals(true, ctrlLinkMessage.getParts().size() == 2);

		// one part should be status and of type boolean
		Part statusPart = (Part) ctrlLinkMessage.getPart("status");
		assertNotNull(statusPart);
		assertTrue(statusPart.getTypeName().getLocalPart().equals("boolean"));

		// one part should be correlation and of type boolean
		Part correlPart = (Part) ctrlLinkMessage.getPart("correlation");
		assertNotNull(correlPart);
		assertTrue(correlPart.getTypeName().getLocalPart().equals("string"));

	}

	@Test
	public void testCreateDataDependencyMessage() throws WSDLException, IOException {
		Variables vars = process.getVariables();
		for (Variable var : vars.getChildren()) {
			Message dataMessage = FragmentFactory.createDataDependencyMessage(new QName(""),
					new String[] { "" });
			for (Object obj : dataMessage.getEParts()) {
				Part part = (Part) obj;
				if (part.getEMessage() != null)
					System.out.println("Message part - name:" + part.getName() + " element:"
							+ part.getEMessage().getQName());
				else if (part.getTypeDefinition() != null)
					System.out.println("Message part - name:" + part.getName() + " simpleType:"
							+ part.getTypeDefinition().getName());

			}
			defn.addMessage(dataMessage);
		}

		BPEL4ChorWriter.writeWSDL(defn, System.out);
	}

	@Test
	public void testCreateWSDLDefinition() {
		Definition definition = FragmentFactory.createWSDLDefinition(process, defn,
				"associatedProcess");
		assertNotNull(definition);

		// property correlProperty
		Property expectedProperty = MyBPELUtils.findFirstProperty(process);
		QName qname = expectedProperty.getQName();
		Property actualProperty = MyWSDLUtil.resolveBPELProperty(definition, qname);
		assertNotNull(actualProperty);
		assertEquals(qname, actualProperty.getQName());

//		// propertyAlias point to correlProperty
//		QName propertyName = actualProperty.getQName();
//		QName messageType = new QName(process.getTargetNamespace(),
//				SplitProcessConstants.CONTROL_LINK_MESSAGE_NAME);
//		PropertyAlias propertyAlias = MyWSDLUtil.findPropertyAlias(definition, propertyName,
//				messageType, "correlation");
//		assertNotNull(propertyAlias);
//		assertEquals(true, propertyAlias.getPropertyName().equals(actualProperty));

	}

	@Test
	public void testCreateVariable() {
		// create variable=orderInfo, message, with inline initialisation:
		// from-spec status=true.
		String sugguest = "orderInfo";
		Message message = FragmentFactory.createControlLinkMessage(process.getTargetNamespace());
		boolean status = true;
		Variable variable = FragmentFactory.createSendingBlockVariable(process, sugguest, message,
				status);
		assertNotNull(variable);
		assertEquals(false, variable.getName().equals(sugguest));
		assertEquals(message, variable.getMessageType());
		// test the from-spec
		From from = variable.getFrom();
		assertNotNull(from);
		// ture()
		assertEquals(status + "()", from.getExpression().getBody().toString());
	}

	@Test
	public void testCreateOperation() {
		String sugguest = "order";
		Message message = FragmentFactory.createControlLinkMessage(process.getTargetNamespace());
		Operation operation = FragmentFactory.createOperation(defn, message, sugguest);
		assertNotNull(operation);
		assertEquals(false, operation.getName().equals(sugguest));
		assertEquals(message, operation.getInput().getMessage());
	}

	@Test
	public void testCreatePortType() {
		String sugguest = "OrderingProcess";
		Message message = FragmentFactory.createControlLinkMessage(process.getTargetNamespace());
		Operation operation = FragmentFactory.createOperation(defn, message, sugguest);
		PortType portType = FragmentFactory.createPortType(defn, operation, sugguest);
		assertNotNull(portType);
		assertEquals(false, portType.getQName().getLocalPart().equals(sugguest));
		assertEquals(operation, portType.getOperations().get(0));
	}

	@Test
	public void testCreateSendingBlockInvoke() {
		String name = "InvokeF";
		String sugguestVarName = "varTrue";
		String sugguestOpName = "order";
		String sugguestPTName = "invokePT";
		Message message = FragmentFactory.createControlLinkMessage(process.getTargetNamespace());
		Operation op = FragmentFactory.createOperation(defn, message, sugguestOpName);
		boolean statusTrue = true;
		Variable inputVariable = FragmentFactory.createSendingBlockVariable(process,
				sugguestVarName, message, statusTrue);
		PortType pt = FragmentFactory.createPortType(defn, op, sugguestPTName);
		Role role = PartnerlinktypeFactory.eINSTANCE.createRole();
		role.setName("someRole");
		role.setPortType(pt);
		PartnerLinkType plt = PartnerlinktypeFactory.eINSTANCE.createPartnerLinkType();
		plt.setName("myPartnerlinkType");
		plt.getRole().add(role);
		PartnerLink pl = BPELFactory.eINSTANCE.createPartnerLink();
		pl.setName("myPL");
		pl.setPartnerLinkType(plt);
		pl.setPartnerRole(role);
		boolean suppressJoinFailure = false;
		Invoke invoke = FragmentFactory.createSendingBlockInvoke(name, pl, pt, op, inputVariable,
				suppressJoinFailure);
		assertNotNull(invoke);
		assertEquals(suppressJoinFailure, invoke.getSuppressJoinFailure());
	}

	@Test
	public void testCreateReceivingBlockReceive() {
		String sugguestName = "receiveStatusAndCorrel";
		String sugguestVarName = "varReceive";
		String sugguestOpName = "order";
		String sugguestPTName = "invokePT";
		Message message = FragmentFactory.createControlLinkMessage(process.getTargetNamespace());
		Operation op = FragmentFactory.createOperation(defn, message, sugguestOpName);
		Variable inputVariable = FragmentFactory.createReceivingBlockVariable(process,
				sugguestVarName, message);
		PortType pt = FragmentFactory.createPortType(defn, op, sugguestPTName);
		Role role = PartnerlinktypeFactory.eINSTANCE.createRole();
		role.setName("someRole");
		role.setPortType(pt);
		PartnerLinkType plt = PartnerlinktypeFactory.eINSTANCE.createPartnerLinkType();
		plt.setName("myPartnerlinkType");
		plt.getRole().add(role);
		PartnerLink pl = BPELFactory.eINSTANCE.createPartnerLink();
		pl.setName("myPL");
		pl.setPartnerLinkType(plt);
		pl.setPartnerRole(role);
		Receive receive = FragmentFactory.createReceivingBlockReceive(process, defn, pl, pt, op,
				inputVariable, sugguestName);

		// assert the propertyAlias
		
		
		
		assertNotNull(receive);
		assertEquals(true, receive.getCreateInstance());
	}

}
