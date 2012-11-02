package org.bpel4chor.splitprocess.test.fragmentation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.wsdl.WSDLException;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import junit.framework.Assert;

import de.uni_stuttgart.iaas.bpel.model.utilities.MyBPELUtils;
import de.uni_stuttgart.iaas.bpel.model.utilities.MyWSDLUtil;

import org.apache.log4j.Logger;
import org.bpel4chor.splitprocess.RuntimeData;
import org.bpel4chor.splitprocess.exceptions.ActivityNotFoundException;
import org.bpel4chor.splitprocess.exceptions.PartitionSpecificationException;
import org.bpel4chor.splitprocess.fragmentation.FragmentFactory;
import org.bpel4chor.splitprocess.fragmentation.ProcessFragmenter;
import org.bpel4chor.splitprocess.partition.model.Participant;
import org.bpel4chor.splitprocess.partition.model.PartitionSpecification;
import org.bpel4chor.splitprocess.partition.representation.PartitionSpecReader;
import org.bpel4chor.splitprocess.test.TestUtil;
import org.bpel4chor.splitprocess.utils.ActivityFinder;
import org.bpel4chor.utils.BPEL4ChorModelConstants;
import org.bpel4chor.utils.BPEL4ChorReader;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Assign;
import org.eclipse.bpel.model.BPELExtensibleElement;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.BPELPlugin;
import org.eclipse.bpel.model.Copy;
import org.eclipse.bpel.model.CorrelationSets;
import org.eclipse.bpel.model.Correlations;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.From;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.PartnerLink;
import org.eclipse.bpel.model.PartnerLinks;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Receive;
import org.eclipse.bpel.model.To;
import org.eclipse.bpel.model.Variable;
import org.eclipse.bpel.model.Variables;
import org.eclipse.bpel.model.While;
import org.eclipse.bpel.model.messageproperties.Property;
import org.eclipse.bpel.model.partnerlinktype.PartnerLinkType;
import org.eclipse.bpel.model.resource.BPELResource;
import org.eclipse.bpel.model.resource.BPELResourceFactoryImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.wst.wsdl.Definition;
import org.eclipse.wst.wsdl.Input;
import org.eclipse.wst.wsdl.Message;
import org.eclipse.wst.wsdl.Operation;
import org.eclipse.wst.wsdl.Output;
import org.eclipse.wst.wsdl.PortType;
import org.eclipse.wst.wsdl.internal.util.WSDLResourceFactoryImpl;
import org.eclipse.xsd.util.XSDResourceFactoryImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test for ProcessFragmenter
 * 
 * @since Feb 3, 2012
 * @author Daojun Cui
 */
public class ProcessFragmenterTest extends TestUtil {

	private static Process nonSplitProcess4;
	private static Process nonSplitProcess3;
	private static Process nonSplitProcess2;

	private static PartitionSpecification partitionSpec2;
	private static PartitionSpecification partitionSpec3;
	private static PartitionSpecification partitionSpec4;

	private static ActivityFinder finder2;
	private static ActivityFinder finder3;
	private static ActivityFinder finder4;

	private static RuntimeData data2;
	private static RuntimeData data3;
	private static RuntimeData data4;

	private static ProcessFragmenterTestWrapper fragmenter2;
	private static ProcessFragmenterTestWrapper fragmenter3;
	private static ProcessFragmenterTestWrapper fragmenter4;

	private static File testFileDir;// where the test files locate

	private static BPELPlugin bpelPlugin;

	private static Logger logger = Logger.getLogger(ProcessFragmenterTest.class);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		File projectDir = new File("");
		testFileDir = new File(projectDir.getAbsolutePath(), "files");

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {

	}

	@Before
	public void setUp() throws Exception {
		// init eclipse plugin
		bpelPlugin = new BPELPlugin();
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("bpel",
				new BPELResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("wsdl",
				new WSDLResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xsd",
				new XSDResourceFactoryImpl());
	}

	@After
	public void tearDown() throws Exception {
		nonSplitProcess2 = null;
		nonSplitProcess3 = null;
		nonSplitProcess4 = null;
		partitionSpec2 = null;
		partitionSpec3 = null;
		partitionSpec4 = null;
		finder2 = null;
		finder3 = null;
		finder4 = null;
		fragmenter2 = null;
		fragmenter3 = null;
		fragmenter4 = null;
		bpelPlugin = null;
		data2 = null;
		data3 = null;
		data4 = null;
	}

	protected static Process loadBPEL(String strURI) {
		ResourceSet resourceSet = new ResourceSetImpl();
		URI uri = URI.createFileURI(strURI);
		Resource resource = resourceSet.getResource(uri, true);
		return (Process) resource.getContents().get(0);
	}

	protected static PartitionSpecification loadPartitionSpec(String strURI, Process process)
			throws JAXBException, FileNotFoundException, PartitionSpecificationException {
		FileInputStream inputStream = new FileInputStream(new File(strURI));
		PartitionSpecReader reader = new PartitionSpecReader();
		return reader.readSpecification(inputStream, process);
	}

	public void prepareTestExample3() throws JAXBException, WSDLException, IOException,
			PartitionSpecificationException {

		// load bpel resource,
		String strURI = testFileDir.getAbsolutePath() + File.separator + "OrderInfoSimple3"
				+ File.separator + "bpelContent" + File.separator + "OrderingProcessSimple3.bpel";
		nonSplitProcess3 = loadBPEL(strURI);

		logger.info(strURI + " is parsed.");

		// set finder
		finder3 = new ActivityFinder(nonSplitProcess3);

		// partition specification
		String partitionURI = testFileDir.getAbsolutePath()
				+ "\\OrderInfoSimple3\\bpelContent\\Partition.xml";
		partitionSpec3 = loadPartitionSpec(partitionURI, nonSplitProcess3);

		logger.info(partitionURI + " is parsed.");

		// the test target
		String wsdlURI = testFileDir.getAbsolutePath() + File.separator + "OrderInfoSimple3"
				+ File.separator + "bpelContent" + File.separator + "OrderingProcessSimple3.wsdl";
		Definition defn = MyWSDLUtil.readWSDL(wsdlURI);
		data3 = new RuntimeData(nonSplitProcess3, partitionSpec3, defn);
		fragmenter3 = new ProcessFragmenterTestWrapper(data3);

	}

	public void prepareTestExample4() throws JAXBException, WSDLException, IOException,
			PartitionSpecificationException {
		// load bpel resource,
		// [project_path]\files\OrderInfoWithLoop\OrderingProcess.bpel
		ResourceSet resourceSet4 = new ResourceSetImpl();
		URI uri4 = URI.createFileURI(testFileDir + File.separator + "OrderInfoWithLoop"
				+ File.separator + "bpelContent" + File.separator + "OrderingProcess.bpel");
		BPELResource resource4 = (BPELResource) resourceSet4.createResource(uri4);

		// prepare the inputStream,
		// [project_path]\files\OrderInfoWithLoop\OrderingProcess.bpel
		FileInputStream inputStream4 = new FileInputStream(new File(testFileDir + File.separator
				+ "OrderInfoWithLoop" + File.separator + "bpelContent", "OrderingProcess.bpel"));

		// read in the BPEL process
		nonSplitProcess4 = BPEL4ChorReader.readBPEL(resource4, inputStream4);
		logger.info(uri4.toFileString() + " is parsed.");

		// set finder
		finder4 = new ActivityFinder(nonSplitProcess4);

		PartitionSpecReader psReader = new PartitionSpecReader();
		File partFile4 = new File(testFileDir + File.separator + "OrderInfoWithLoop"
				+ File.separator + "bpelContent", "Partition.xml");

		FileInputStream partFileInputStream4 = new FileInputStream(partFile4);

		partitionSpec4 = psReader.readSpecification(partFileInputStream4, nonSplitProcess4);

		logger.info(partFile4.getAbsolutePath() + " is parsed.");

		// the test target
		String wsdlURI = testFileDir + File.separator + "OrderInfoWithLoop" + File.separator
				+ "bpelContent" + File.separator + "OrderingProcess.wsdl";
		Definition defn = MyWSDLUtil.readWSDL(wsdlURI);
		data4 = new RuntimeData(nonSplitProcess4, partitionSpec4, defn);
		fragmenter4 = new ProcessFragmenterTestWrapper(data4);

	}

	@Test
	public void testAddCorrelations() throws JAXBException, WSDLException, IOException,
			PartitionSpecificationException {

		// load process
		String strURI = testFileDir.getAbsolutePath() + File.separator + "OrderInfo4DDTestCase1"
				+ File.separator + "bpelContent" + File.separator + "OrderingProcess.bpel";
		Process nonSplitProcess = loadBPEL(strURI);

		// load partition specification
		String partitoinURI = testFileDir.getAbsolutePath() + File.separator
				+ "OrderInfo4DDTestCase1" + File.separator + "bpelContent" + File.separator
				+ "Partition.xml";
		PartitionSpecification partitionSpec = loadPartitionSpec(partitoinURI, nonSplitProcess);

		// definition
		String wsdlURI = testFileDir.getAbsolutePath() + File.separator + "OrderInfo4DDTestCase1"
				+ File.separator + "bpelContent" + File.separator + "OrderingProcess.wsdl";
		Definition nonSplitDefn = MyWSDLUtil.readWSDL(wsdlURI);

		// runtime data
		RuntimeData data = new RuntimeData(nonSplitProcess, partitionSpec, nonSplitDefn);

		// get participant "x"
		Participant participant = partitionSpec.getParticipant("x");

		// process fragmenter
		ProcessFragmenterTestWrapper fragmenter = new ProcessFragmenterTestWrapper(data);

		// create fragment wsdl definition
		Definition fragDefn = FragmentFactory.createWSDLDefinition(nonSplitProcess, nonSplitDefn,
				participant.getName());
		data.getParticipant2WSDLMap().put(participant.getName(), fragDefn);

		// initialize a new process for participant x
		Process fragProcess = fragmenter.createSkeletonProcess(participant);

		// add the correlation into the fragment process wsdl definition
		fragmenter.addCorrelations(fragProcess);

		// the correlation set should be copied into the fragment's process
		assertCorrelationSetIsCopied(fragProcess);

		// the property/Alias should be copied into the fragment's wsdl
		// definition
		assertPropertyIsCopied(fragDefn);

	}

	private void assertPropertyIsCopied(Definition fragDefn) {

		String propertyName = "correlProperty";
		Property foundProperty = MyWSDLUtil.findProperty(fragDefn, propertyName);
		Assert.assertNotNull(foundProperty);

		// QName propertyQName = new QName(fragDefn.getTargetNamespace(),
		// propertyName);
		// PropertyAlias[] foundPropertyAliases =
		// MyWSDLUtil.findPropertyAlias(fragDefn, propertyQName);
		// Assert.assertTrue(foundPropertyAliases.length == 1);

	}

	private void assertCorrelationSetIsCopied(Process fragProcess) {
		CorrelationSets cs = fragProcess.getCorrelationSets();
		Assert.assertTrue(cs.getChildren().size() == 1);
		Assert.assertEquals("CorrelationSet", cs.getChildren().get(0).getName());
		Assert.assertNotNull(cs.getChildren().get(0).getProperties().size() == 1);
	}

	@Test
	public void testInitParticipant2ActMap() throws JAXBException, WSDLException, IOException,
			PartitionSpecificationException {

		prepareTestExample3();

		// use BPEL in example 3 as test object
		Set<Activity> p1Acts = partitionSpec3.getParticipant("participant1").getActivities();
		Set<Activity> p2Acts = partitionSpec3.getParticipant("participant2").getActivities();
		Set<Activity> p3Acts = partitionSpec3.getParticipant("participant3").getActivities();

		for (Activity act : p1Acts) {
			Assert.assertEquals(true,
					((act.getName().equals("ReceiveA") && act instanceof Receive) || (act.getName()
							.equals("AssignB") && act instanceof Assign)));
		}

		for (Activity act : p2Acts) {
			Assert.assertEquals(
					true,
					((act.getName().equals("AssignC") || act.getName().equals("AssignD")) && act instanceof Assign));
		}

		for (Activity act : p3Acts) {
			Assert.assertEquals(true,
					((act.getName().equals("AssignE") && act instanceof Assign) || (act.getName()
							.equals("InvokeF") && act instanceof Invoke)));
		}

	}

	@Test
	public void testAddPartnerLink1() throws JAXBException, WSDLException, IOException,
			PartitionSpecificationException {

		// Test Object OrderInfo4DDTestCase1 OrderingProcess.bpel, Participant x

		// load process
		String strURI = testFileDir.getAbsolutePath() + File.separator + "OrderInfo4DDTestCase1"
				+ File.separator + "bpelContent" + File.separator + "OrderingProcess.bpel";
		Process nonSplitProcess = loadBPEL(strURI);

		// load partition specification
		String partitoinURI = testFileDir.getAbsolutePath() + File.separator
				+ "OrderInfo4DDTestCase1" + File.separator + "bpelContent" + File.separator
				+ "Partition.xml";
		PartitionSpecification partitionSpec = loadPartitionSpec(partitoinURI, nonSplitProcess);

		// definition
		String wsdlURI = testFileDir.getAbsolutePath() + File.separator + "OrderInfo4DDTestCase1"
				+ File.separator + "bpelContent" + File.separator + "OrderingProcess.wsdl";
		Definition nonSplitDefn = MyWSDLUtil.readWSDL(wsdlURI);

		// runtime data
		RuntimeData data = new RuntimeData(nonSplitProcess, partitionSpec, nonSplitDefn);

		// get participant "x"
		Participant participant = partitionSpec.getParticipant("x");

		// process fragmenter
		ProcessFragmenterTestWrapper fragmenter = new ProcessFragmenterTestWrapper(data);

		// create fragment wsdl definition
		Definition fragDefn = FragmentFactory.createWSDLDefinition(nonSplitProcess, nonSplitDefn,
				participant.getName());
		data.getParticipant2WSDLMap().put(participant.getName(), fragDefn);

		// initialize a new process for participant x
		Process fragProcess = fragmenter.createSkeletonProcess(participant);

		// add variables referred by participant x
		fragmenter.addVariable(fragProcess, participant);

		// add partnerLink referred by participant x
		fragmenter.addPartnerLink(fragProcess, participant);

		// assertion for fragment process x
		assertParnterLinkIsCopiedInProcessX(fragProcess, fragDefn);
		assertMessageIsCopiedInWSDLX(fragDefn);

		// print definition of participant x
		logger.info("WSDL definition of participant x");
		MyWSDLUtil.print(fragDefn);

		// get participant "w"
		participant = partitionSpec.getParticipant("w");

		// create fragment wsdl definition for participant w
		fragDefn = FragmentFactory.createWSDLDefinition(nonSplitProcess, nonSplitDefn,
				participant.getName());
		data.getParticipant2WSDLMap().put(participant.getName(), fragDefn);

		// initialize a new process for participant w
		fragProcess = fragmenter.createSkeletonProcess(participant);

		// add variables referred by participant w
		fragmenter.addVariable(fragProcess, participant);

		// add partnerLink referred by participant w
		fragmenter.addPartnerLink(fragProcess, participant);

		// assertion for fragment process w
		assertPartnerLinkIsCopiedInProcessW(fragProcess, fragDefn);

		// print definition of participant w
		logger.info("WSDL definition of participant w");
		MyWSDLUtil.print(fragDefn);
	}

	private void assertParnterLinkIsCopiedInProcessX(Process fragProcess, Definition fragDefn) {

		// reeive A, assign B is in participant x

		// assert partnerLink "client" is in fragment process
		PartnerLink pl = MyBPELUtils.getPartnerLink(fragProcess, "client");
		Assert.assertNotNull(pl);

		// assert the partnerLinkType "OrderingProcessPLT" is in fragment
		// process
		PartnerLinkType actualPlt = pl.getPartnerLinkType();
		Assert.assertNotNull(actualPlt);
		Assert.assertTrue(actualPlt.getName().equals("OrderingProcessPLT"));

		PartnerLinkType expectedPlt = MyWSDLUtil
				.findPartnerLinkType(fragDefn, "OrderingProcessPLT");

		Assert.assertEquals(expectedPlt, actualPlt);

		// assert the portType OrderingProcessPT is in fragment definition
		QName ptQname = new QName(fragDefn.getTargetNamespace(), "OrderingProcessPT");
		PortType actPt = MyWSDLUtil.resolvePortType(fragDefn, ptQname);

		Assert.assertNotNull(actPt);

		// assert the operation "initiate" is also present in the portType
		Operation op = MyWSDLUtil.resolveOperation(fragDefn, ptQname, "initiate");
		Assert.assertNotNull(op);

	}

	private void assertMessageIsCopiedInWSDLX(Definition fragDefn) {

		// assert the message "OrderInfoRequestMessage" is copied in the
		// fragment definition

		QName msgQname = new QName(fragDefn.getTargetNamespace(), "OrderInfoRequestMessage");

		Message msg = MyWSDLUtil.resolveMessage(fragDefn, msgQname);

		Assert.assertNotNull(msg);

	}

	private void assertPartnerLinkIsCopiedInProcessW(Process fragProcess, Definition fragDefn) {

		// assert the partnerLink "paymentPL" is copied in fragProcess
		PartnerLink pl = MyBPELUtils.getPartnerLink(fragProcess, "paymentPL");
		Assert.assertNotNull(pl);

	}

	@Test
	public void testAddPartnerLink2() throws JAXBException, WSDLException, IOException,
			PartitionSpecificationException {

		// Test Object OrderInfo4DDTestCase2 OrderingProcess.bpel, Participant y

		// load process
		String strURI = testFileDir.getAbsolutePath() + File.separator + "OrderInfo4DDTestCase2"
				+ File.separator + "bpelContent" + File.separator + "OrderingProcess.bpel";
		Process nonSplitProcess = loadBPEL(strURI);

		// load partition specification
		String partitoinURI = testFileDir.getAbsolutePath() + File.separator
				+ "OrderInfo4DDTestCase2" + File.separator + "bpelContent" + File.separator
				+ "Partition.xml";
		PartitionSpecification partitionSpec = loadPartitionSpec(partitoinURI, nonSplitProcess);

		// definition
		String wsdlURI = testFileDir.getAbsolutePath() + File.separator + "OrderInfo4DDTestCase2"
				+ File.separator + "bpelContent" + File.separator + "OrderingProcess.wsdl";
		Definition nonSplitDefn = MyWSDLUtil.readWSDL(wsdlURI);

		// runtime data
		RuntimeData data = new RuntimeData(nonSplitProcess, partitionSpec, nonSplitDefn);

		// process fragmenter
		ProcessFragmenterTestWrapper fragmenter = new ProcessFragmenterTestWrapper(data);

		// get participant "y"
		Participant participant = partitionSpec.getParticipant("y");

		// create fragment wsdl definition
		Definition fragDefn = FragmentFactory.createWSDLDefinition(nonSplitProcess, nonSplitDefn,
				participant.getName());
		data.getParticipant2WSDLMap().put(participant.getName(), fragDefn);

		// initialize a new process for participant y
		Process fragProcess = fragmenter.createSkeletonProcess(participant);

		// add variables referred by participant y
		fragmenter.addVariable(fragProcess, participant);

		// add partnerLink referred by participant y
		fragmenter.addPartnerLink(fragProcess, participant);

		// assertion for fragment process y
		assertMessageIsCopiedInWSDLY(fragDefn);

		// print definition of participant y
		logger.info("WSDL definition of participant y");
		MyWSDLUtil.print(fragDefn);

		// get participant "x"
		participant = partitionSpec.getParticipant("x");

		// create fragment wsdl definition for participant x
		fragDefn = FragmentFactory.createWSDLDefinition(nonSplitProcess, nonSplitDefn,
				participant.getName());
		data.getParticipant2WSDLMap().put(participant.getName(), fragDefn);

		// initialize a new process for participant x
		fragProcess = fragmenter.createSkeletonProcess(participant);

		// add variables referred by participant x
		fragmenter.addVariable(fragProcess, participant);

		// add partnerLink referred by participant x
		fragmenter.addPartnerLink(fragProcess, participant);

		// assertion for fragment process x
		assertPartnerLinkIsCopiedInProcessX2(fragProcess, fragDefn);

		// print definition of participant x
		logger.info("WSDL definition of participant x");
		MyWSDLUtil.print(fragDefn);

	}

	private void assertPartnerLinkIsCopiedInProcessX2(Process fragProcess, Definition fragDefn) {

		// assert the partnerLink "client" is in fragment process
		PartnerLink pl = MyBPELUtils.getPartnerLink(fragProcess, "client");
		Assert.assertNotNull(pl);

		// assert the partnerLinkType "OrderingProcessPLT" is in fragment
		// definition
		PartnerLinkType actualPlt = pl.getPartnerLinkType();
		Assert.assertNotNull(actualPlt);

		Assert.assertEquals(actualPlt.getName(), "OrderingProcessPLT");
		QName pltQname = new QName(fragDefn.getTargetNamespace(), "OrderingProcessPLT");
		PartnerLinkType expectPlt = MyWSDLUtil.resolveBPELPartnerLinkType(fragDefn, pltQname);

		Assert.assertEquals(expectPlt, actualPlt);

		// assert the portType "OrderingProcessPT" is in fragment definition
		QName ptQname = new QName(fragDefn.getTargetNamespace(), "OrderingProcessPT");
		PortType pt = MyWSDLUtil.resolvePortType(fragDefn, ptQname);

		Assert.assertNotNull(pt);

		Assert.assertEquals(pt.getOperations().size(), 1);

		// assert the operatation "process" is in the fragment definition
		Operation op = MyWSDLUtil.findOperation(pt, "process");
		Assert.assertNotNull(op);

		// assert the input of the operation is message:
		// OrderingProcessRequestMessage
		Input input = (Input) op.getInput();
		Assert.assertNotNull(input);

		Message actualInputMsg = input.getEMessage();
		Assert.assertNotNull(actualInputMsg);

		QName inputMsgQname = new QName(fragDefn.getTargetNamespace(),
				"OrderingProcessRequestMessage");
		Message expectedInputMsg = MyWSDLUtil.resolveMessage(fragDefn, inputMsgQname);

		Assert.assertNotNull(expectedInputMsg);

		Assert.assertEquals(expectedInputMsg, actualInputMsg);

		// assert the output of the operation is message:
		// OrderingProcessResponseMessage
		Output output = (Output) op.getOutput();
		Message actualOutputMsg = output.getEMessage();

		QName outputQname = new QName(fragDefn.getTargetNamespace(),
				"OrderingProcessResponseMessage");
		Message expectedOutputMsg = MyWSDLUtil.resolveMessage(fragDefn, outputQname);

		Assert.assertNotNull(expectedOutputMsg);

		Assert.assertEquals(expectedOutputMsg, actualOutputMsg);

		// the input message consists of 4 parts
		Assert.assertEquals(expectedInputMsg.getParts().size(), actualInputMsg.getParts().size());

		// the output message consists of 1 part
		Assert.assertEquals(expectedOutputMsg.getParts().size(), actualOutputMsg.getParts().size());
	}

	private void assertMessageIsCopiedInWSDLY(Definition fragDefn) {
		// assert the message of variable "response" :
		// OrderingProcessResponseMessage is copied in the definition
		QName msgQname = new QName(fragDefn.getTargetNamespace(), "OrderingProcessResponseMessage");
		Message msg = MyWSDLUtil.resolveMessage(fragDefn, msgQname);
		Assert.assertNotNull(msg);
	}

	@Test
	public void testAddVariable() throws JAXBException, WSDLException, IOException,
			PartitionSpecificationException {

		// Test Object OrderInfo4DDTestCase1 - OrderingProcess

		// load process
		String strURI = testFileDir.getAbsolutePath() + File.separator + "OrderInfo4DDTestCase1"
				+ File.separator + "bpelContent" + File.separator + "OrderingProcess.bpel";
		Process nonSplitProcess = loadBPEL(strURI);

		// load partition specification
		String partitoinURI = testFileDir.getAbsolutePath() + File.separator
				+ "OrderInfo4DDTestCase1" + File.separator + "bpelContent" + File.separator
				+ "Partition.xml";
		PartitionSpecification partitionSpec = loadPartitionSpec(partitoinURI, nonSplitProcess);

		// get participant "x"
		Participant participant = partitionSpec.getParticipant("x");

		// definition
		String wsdlURI = testFileDir.getAbsolutePath() + File.separator + "OrderInfo4DDTestCase1"
				+ File.separator + "bpelContent" + File.separator + "OrderingProcess.wsdl";
		Definition nonSplitDefn = MyWSDLUtil.readWSDL(wsdlURI);

		// runtime data
		RuntimeData data = new RuntimeData(nonSplitProcess, partitionSpec, nonSplitDefn);

		// process fragmenter
		ProcessFragmenterTestWrapper fragmenter = new ProcessFragmenterTestWrapper(data);

		// create fragment wsdl definition
		Definition fragDefn = FragmentFactory.createWSDLDefinition(nonSplitProcess, nonSplitDefn,
				participant.getName());
		data.getParticipant2WSDLMap().put(participant.getName(), fragDefn);

		// initialize a new process for participant
		Process fragProcess = fragmenter.createSkeletonProcess(participant);

		// add variables used by participant x
		fragmenter.addVariable(fragProcess, participant);

		// variable input and paymentInfo
		assertVarialbesAndMessageCopied(fragProcess, fragDefn, nonSplitDefn);

		// print fragment definition
		MyWSDLUtil.print(fragDefn);

	}

	private void assertVarialbesAndMessageCopied(Process fragProcess, Definition fragDefn,
			Definition nonSplitDefn) {

		// variable input exists
		Variable input = MyBPELUtils.resolveVariable("input", fragProcess);
		Assert.assertNotNull(input);

		// message of input exists
		Message inputMsg = input.getMessageType();
		Assert.assertNotNull(inputMsg);

		Assert.assertTrue(MyWSDLUtil.resolveMessage(fragDefn, inputMsg.getQName()) != null);
		Assert.assertTrue(MyWSDLUtil.resolveMessage(nonSplitDefn, inputMsg.getQName()) != null);

		// variable paymentInfo exists
		Variable paymentInfo = MyBPELUtils.resolveVariable("paymentInfo", fragProcess);
		Assert.assertNotNull(paymentInfo);

		// message of paymentInfo does not exist
		Message paymentMsg = paymentInfo.getMessageType();
		Assert.assertNotNull(paymentMsg);

		Assert.assertTrue(MyWSDLUtil.resolveMessage(fragDefn, paymentMsg.getQName()) == null);
		Assert.assertTrue(MyWSDLUtil.resolveMessage(nonSplitDefn, paymentMsg.getQName()) == null);

	}

	@Test
	public void testGetParentStructuredAct() throws JAXBException, ActivityNotFoundException,
			WSDLException, IOException, PartitionSpecificationException {

		// Test Object: Example4 OrderInfoWithLoop, parent of InvokeJ
		prepareTestExample4();

		Activity invokeJ = finder4.find("InvokeJ");
		Activity flow = finder4.find("Flow");
		Activity flow1 = finder4.find("Flow1");

		Activity invokeParent = (Activity) fragmenter4.getParentStructuredAct(invokeJ);
		Assert.assertEquals(true,
				invokeParent instanceof Flow && invokeParent.getName().equals("Flow1"));

		BPELExtensibleElement flowParent = fragmenter4.getParentStructuredAct(flow);
		Assert.assertEquals(true, flowParent instanceof Process);

		Activity flow1Parent = (Activity) fragmenter4.getParentStructuredAct(flow1);
		Assert.assertEquals(true,
				flow1Parent instanceof While && flow1Parent.getName().equals("WhileI"));

	}

	@Test
	public void testGetEquivalentAct() throws JAXBException, ActivityNotFoundException,
			WSDLException, IOException, PartitionSpecificationException {

		prepareTestExample4();

		Participant testParticipant5 = partitionSpec4.getParticipant("participant5");

		Process testFragProcess4;
		// initialise a new process for participant
		testFragProcess4 = BPELFactory.eINSTANCE.createProcess();
		testFragProcess4.setName(testParticipant5.getName());
		testFragProcess4.setSuppressJoinFailure(nonSplitProcess4.getSuppressJoinFailure());
		testFragProcess4.setTargetNamespace(nonSplitProcess4.getTargetNamespace());
		testFragProcess4.setExtensions(nonSplitProcess4.getExtensions());
		testFragProcess4.setVariables(BPELFactory.eINSTANCE.createVariables());
		testFragProcess4.setPartnerLinks(BPELFactory.eINSTANCE.createPartnerLinks());
		testFragProcess4.setCorrelationSets(BPELFactory.eINSTANCE.createCorrelationSets());

		// construct the fragment process
		Flow flow = BPELFactory.eINSTANCE.createFlow();
		flow.setName("Flow");
		While whileI = BPELFactory.eINSTANCE.createWhile();
		whileI.setName("WhileI");
		flow.getActivities().add(whileI);
		testFragProcess4.setActivity(flow);

		// find flow1 parent in non-split process
		Activity flow1 = finder4.find("Flow1");
		Activity flowParent = (Activity) fragmenter4.getParentStructuredAct(flow1);

		// find equivalent activity of the flow1 parent in the fragment process
		Activity eqAct = (Activity) fragmenter4.getEquivalentAct(testFragProcess4, flowParent);
		Assert.assertEquals(true, eqAct.getName().equals("WhileI"));

		// find equivalent activity of the "process" in the fragment process
		BPELExtensibleElement eqProc = fragmenter4.getEquivalentAct(testFragProcess4,
				nonSplitProcess4);
		Assert.assertEquals(testFragProcess4, eqProc);
	}

	@Test
	public void testProcessChild() throws JAXBException, WSDLException, IOException,
			PartitionSpecificationException {

		// Test Object OrderInfo4DDTestCase1 OrderingProcess.bpel, Participant x

		// load process
		String strURI = testFileDir.getAbsolutePath() + File.separator + "OrderInfo4DDTestCase1"
				+ File.separator + "bpelContent" + File.separator + "OrderingProcess.bpel";
		Process nonSplitProcess = loadBPEL(strURI);

		// load partition specification
		String partitoinURI = testFileDir.getAbsolutePath() + File.separator
				+ "OrderInfo4DDTestCase1" + File.separator + "bpelContent" + File.separator
				+ "Partition.xml";
		PartitionSpecification partitionSpec = loadPartitionSpec(partitoinURI, nonSplitProcess);

		// definition
		String wsdlURI = testFileDir.getAbsolutePath() + File.separator + "OrderInfo4DDTestCase1"
				+ File.separator + "bpelContent" + File.separator + "OrderingProcess.wsdl";
		Definition nonSplitDefn = MyWSDLUtil.readWSDL(wsdlURI);

		// runtime data
		RuntimeData data = new RuntimeData(nonSplitProcess, partitionSpec, nonSplitDefn);

		// get participant "x"
		Participant participant = partitionSpec.getParticipant("x");

		// process fragmenter
		ProcessFragmenterTestWrapper fragmenter = new ProcessFragmenterTestWrapper(data);

		// create fragment wsdl definition
		Definition fragDefn = FragmentFactory.createWSDLDefinition(nonSplitProcess, nonSplitDefn,
				participant.getName());
		data.getParticipant2WSDLMap().put(participant.getName(), fragDefn);

		// initialize a new process for participant x
		Process fragProcess = fragmenter.createSkeletonProcess(participant);

		// add correlation
		fragmenter.addCorrelations(fragProcess);

		// add variables referred by participant x
		fragmenter.addVariable(fragProcess, participant);

		// add partnerLink referred by participant x
		fragmenter.addPartnerLink(fragProcess, participant);

		// process child
		fragmenter.processChild(nonSplitProcess.getActivity(), fragProcess, participant);

		// add property alias
		fragmenter.addPropertyAlias(fragProcess);

		// assert that the Receive 'A' is correctly configured
		assertReceiveAConfigurationCorrect(fragProcess, fragDefn);

		// assert that the assign 'B' is correctly configured
		assertAssignBConfigurationCorrect(fragProcess, fragDefn);

		// assert the propertyAlias
		assertPropertyAndAliasConfiguredCorrect(fragDefn);
	}

	private void assertPropertyAndAliasConfiguredCorrect(Definition fragDefn) {
		// property
		Assert.assertNotNull(MyWSDLUtil.findProperty(fragDefn, BPEL4ChorModelConstants.CORRELATION_PROPERTY_NAME));
		QName propertyQName = new QName(fragDefn.getTargetNamespace(), BPEL4ChorModelConstants.CORRELATION_PROPERTY_NAME);
		Assert.assertTrue(MyWSDLUtil.findPropertyAlias(fragDefn, propertyQName).length == 1);
	}

	private void assertReceiveAConfigurationCorrect(Process fragProcess, Definition fragDefn) {

		// receive name 'A'
		Receive receive = (Receive) MyBPELUtils.resolveActivity("A", fragProcess);
		Assert.assertTrue(receive != null && receive.getName().equals("A"));

		// variable
		Variable actualVar = receive.getVariable();
		Variable expectVar = MyBPELUtils.resolveVariable("input", fragProcess);
		Assert.assertEquals(expectVar, actualVar);

		// partnerLink
		PartnerLink actualPl = receive.getPartnerLink();
		PartnerLink expectPl = MyBPELUtils.getPartnerLink(fragProcess, "client");
		Assert.assertEquals(expectPl, actualPl);

		// portType
		PortType actualPt = receive.getPortType();
		PortType expectPt = MyWSDLUtil.findPortType(fragDefn, "OrderingProcessPT");
		Assert.assertEquals(expectPt, actualPt);

		// operation
		Operation actualOp = receive.getOperation();
		Operation expectOp = MyWSDLUtil.findOperation(expectPt, "initiate");
		Assert.assertEquals(expectOp, actualOp);

		// correlation
		Correlations correls = receive.getCorrelations();
		Assert.assertNotNull(correls);
		Assert.assertTrue(correls.getChildren().size() == 1);

	}

	private void assertAssignBConfigurationCorrect(Process fragProcess, Definition fragDefn) {

		// assign name 'B'
		Assign assign = (Assign) MyBPELUtils.resolveActivity("B", fragProcess);
		Assert.assertTrue(assign != null && assign.getName().equals("B"));

		// copy
		List<Copy> cps = assign.getCopy();
		Assert.assertTrue(cps.size() == 2);

		for (Copy cp : cps) {

			From from = cp.getFrom();
			if (from.getVariable() != null) {
				Assert.assertEquals(from.getVariable().getName(), "input");
			} else if (from.getExpression() != null) {
				Assert.assertEquals(from.getExpression().getBody(),
						"$input.orderTotalPrice + 5*$input.numDeliveries");
			} else {
				Assert.fail();
			}

			To to = cp.getTo();
			Assert.assertEquals(to.getVariable().getName(), "paymentInfo");

		}
	}

	@Test
	public void testCreateFragmentProcess() throws JAXBException, WSDLException, IOException,
			PartitionSpecificationException {

		// Test Object OrderInfo4DDTestCase1 - OrderingProcess

		// load process
		String strURI = testFileDir.getAbsolutePath() + File.separator + "OrderInfo4DDTestCase1"
				+ File.separator + "bpelContent" + File.separator + "OrderingProcess.bpel";
		Process nonSplitProcess = loadBPEL(strURI);

		// load partition specification
		String partitoinURI = testFileDir.getAbsolutePath() + File.separator
				+ "OrderInfo4DDTestCase1" + File.separator + "bpelContent" + File.separator
				+ "Partition.xml";
		PartitionSpecification partitionSpec = loadPartitionSpec(partitoinURI, nonSplitProcess);

		// get participant "x"
		Participant participant = partitionSpec.getParticipant("x");

		// definition
		String wsdlURI = testFileDir.getAbsolutePath() + File.separator + "OrderInfo4DDTestCase1"
				+ File.separator + "bpelContent" + File.separator + "OrderingProcess.wsdl";
		Definition nonSplitDefn = MyWSDLUtil.readWSDL(wsdlURI);

		// runtime data
		RuntimeData data = new RuntimeData(nonSplitProcess, partitionSpec, nonSplitDefn);

		// process fragmenter
		ProcessFragmenterTestWrapper fragmenter = new ProcessFragmenterTestWrapper(data);

		// create fragment wsdl definition
		Definition fragDefn = FragmentFactory.createWSDLDefinition(nonSplitProcess, nonSplitDefn,
				participant.getName());
		data.getParticipant2WSDLMap().put(participant.getName(), fragDefn);

		// create fragment process x
		Process fragProc = fragmenter.createFragmentProcess(participant, nonSplitProcess);

		// variable size is 2, 'input', 'paymentInfo', and the 'globalCorrel'
		Variables vars = fragProc.getVariables();
		Assert.assertEquals(3, vars.getChildren().size());
		for (Variable var : vars.getChildren()) {
			Assert.assertTrue(var.getName().equals("input") || var.getName().equals("paymentInfo")
					|| var.getName().equals("globalCorrel"));
		}

		// partnerlink - client
		PartnerLinks pls = fragProc.getPartnerLinks();
		Assert.assertEquals(1, pls.getChildren().size());
		Assert.assertEquals(true, pls.getChildren().get(0).getName().equals("client"));

		// corelationSet, it is NOT null
		CorrelationSets actualCorrelSets = fragProc.getCorrelationSets();
		CorrelationSets expectedCorrelSets = nonSplitProcess.getCorrelationSets();
		// TODO Assert.assertTrue(isEqual(actualCorrelSets,
		// expectedCorrelSets));

		//
		// activity
		//

		Activity child = nonSplitProcess.getActivity();

		// first child should be 'Flow'
		Activity fragFirstChild = fragProc.getActivity();
		Assert.assertEquals(true, child.getName().equals(fragFirstChild.getName())
				&& fragFirstChild instanceof Flow);

		// child of 'Flow' - Receive 'A'
		Activity fragDescendant1 = MyBPELUtils.resolveActivity("A", fragProc);
		Assert.assertTrue(fragDescendant1 instanceof Receive
				&& fragDescendant1.eContainer().equals(fragFirstChild));

		// child of 'Flow' - Assign 'B'
		Activity assignB = MyBPELUtils.resolveActivity("B", fragProc);
		Assert.assertTrue(assignB instanceof Assign && assignB.eContainer().equals(fragFirstChild));

	}

	@Test
	public void testFragmentizeProcess() throws JAXBException, WSDLException, IOException,
			PartitionSpecificationException {

		// load process
		String strURI = testFileDir.getAbsolutePath() + File.separator + "OrderInfo4DDTestCase1"
				+ File.separator + "bpelContent" + File.separator + "OrderingProcess.bpel";
		Process nonSplitProcess = loadBPEL(strURI);

		// load partition specification
		String partitoinURI = testFileDir.getAbsolutePath() + File.separator
				+ "OrderInfo4DDTestCase1" + File.separator + "bpelContent" + File.separator
				+ "Partition.xml";
		PartitionSpecification partitionSpec = loadPartitionSpec(partitoinURI, nonSplitProcess);

		// get participant "x"
		Participant participant = partitionSpec.getParticipant("x");

		// definition
		String wsdlURI = testFileDir.getAbsolutePath() + File.separator + "OrderInfo4DDTestCase1"
				+ File.separator + "bpelContent" + File.separator + "OrderingProcess.wsdl";
		Definition nonSplitDefn = MyWSDLUtil.readWSDL(wsdlURI);

		// runtime data
		RuntimeData data = new RuntimeData(nonSplitProcess, partitionSpec, nonSplitDefn);

		// process fragmenter
		ProcessFragmenterTestWrapper fragmenter = new ProcessFragmenterTestWrapper(data);

		// fragment the main process
		fragmenter.fragmentizeProcess();

		// assert 4 fragment processes are created
		Map<String, Process> participant2ProcessMap = fragmenter.getParticipant2FragProcessMap();
		Assert.assertEquals(4, participant2ProcessMap.size());
		for (Participant p : partitionSpec.getParticipants()) {
			Process fragProc = participant2ProcessMap.get(p.getName());
			Assert.assertEquals(true, fragProc != null && fragProc.getName().equals(p.getName()));
		}

	}

}

/**
 * Class for test intention
 * 
 * <p>
 * It expose the protected methods of the super class, so that we can test it.
 * 
 * @since Feb 2, 2012
 * @author Daojun Cui
 */
class ProcessFragmenterTestWrapper extends ProcessFragmenter {

	public ProcessFragmenterTestWrapper(RuntimeData data) throws PartitionSpecificationException {
		super(data);
	}

	public Process createFragmentProcess(Participant participant, Process unsplitProcess) {
		return super.createFragmentProcess(participant, unsplitProcess);
	}

	@Override
	protected Process createSkeletonProcess(Participant participant) {
		return super.createSkeletonProcess(participant);
	}

	@Override
	protected void addPropertyAlias(Process fragProc) {
		super.addPropertyAlias(fragProc);
	}

	@Override
	public void addPartnerLink(Process proc, Participant participant) {
		super.addPartnerLink(proc, participant);
	}

	@Override
	public void addCorrelations(Process proc) {
		super.addCorrelations(proc);
	}

	@Override
	public void addVariable(Process proc, Participant participant) {
		super.addVariable(proc, participant);
	}

	@Override
	public void addActivity(BPELExtensibleElement parentInFragment, Activity newAct) {
		super.addActivity(parentInFragment, newAct);
	}

	@Override
	public void processChild(Activity child, Process proc, Participant p) {
		super.processChild(child, proc, p);
	}

	@Override
	public BPELExtensibleElement getParentStructuredAct(Activity child) {
		return super.getParentStructuredAct(child);
	}

	@Override
	public BPELExtensibleElement getEquivalentAct(Process proc, BPELExtensibleElement act)
			throws ActivityNotFoundException {
		return super.getEquivalentAct(proc, act);
	}

	@Override
	public boolean isBasicActivity(Activity act) {
		return super.isBasicActivity(act);
	}

	@Override
	public boolean isSimpleActivity(Activity act) {
		return super.isSimpleActivity(act);
	}

	@Override
	public boolean isInParticipant(Activity act, Participant participant) {
		return super.isInParticipant(act, participant);
	}

	@Override
	public boolean isDescendantInParticipant(Activity act, Participant participant) {
		return super.isDescendantInParticipant(act, participant);
	}

	@Override
	public List<Activity> getDirectChildren(Activity act) {
		return super.getDirectChildren(act);
	}

}
