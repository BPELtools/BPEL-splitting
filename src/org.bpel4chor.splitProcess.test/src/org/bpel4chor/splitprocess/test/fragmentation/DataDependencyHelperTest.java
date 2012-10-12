package org.bpel4chor.splitprocess.test.fragmentation;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.wsdl.WSDLException;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.bpel4chor.splitprocess.RuntimeData;
import org.bpel4chor.splitprocess.dataflowanalysis.AnalysisResultParser;
import org.bpel4chor.splitprocess.dataflowanalysis.DataFlowAnalyzer;
import org.bpel4chor.splitprocess.dataflowanalysis.QueryWriterSet;
import org.bpel4chor.splitprocess.exceptions.PWDGException;
import org.bpel4chor.splitprocess.exceptions.PartitionSpecificationException;
import org.bpel4chor.splitprocess.exceptions.WDGException;
import org.bpel4chor.splitprocess.fragmentation.DataDependencyHelper;
import org.bpel4chor.splitprocess.fragmentation.ProcessFragmenter;
import org.bpel4chor.splitprocess.partition.model.Participant;
import org.bpel4chor.splitprocess.partition.model.PartitionSpecification;
import org.bpel4chor.splitprocess.partition.representation.PartitionSpecReader;
import org.bpel4chor.splitprocess.pwdg.model.PWDG;
import org.bpel4chor.splitprocess.pwdg.model.PWDGNode;
import org.bpel4chor.splitprocess.pwdg.model.WDG;
import org.bpel4chor.splitprocess.pwdg.util.PWDGFactory;
import org.bpel4chor.splitprocess.pwdg.util.WDGFactory;
import org.bpel4chor.splitprocess.utils.RandomIdGenerator;
import org.bpel4chor.splitprocess.utils.SplitProcessConstants;
import org.bpel4chor.utils.BPEL4ChorReader;
import org.bpel4chor.utils.MyBPELUtils;
import org.bpel4chor.utils.MyWSDLUtil;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.BPELPlugin;
import org.eclipse.bpel.model.PartnerLink;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Variable;
import org.eclipse.bpel.model.messageproperties.PropertyAlias;
import org.eclipse.bpel.model.partnerlinktype.PartnerLinkType;
import org.eclipse.bpel.model.partnerlinktype.PartnerlinktypeFactory;
import org.eclipse.bpel.model.resource.BPELResourceFactoryImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.wst.wsdl.Definition;
import org.eclipse.wst.wsdl.Input;
import org.eclipse.wst.wsdl.Message;
import org.eclipse.wst.wsdl.Operation;
import org.eclipse.wst.wsdl.Part;
import org.eclipse.wst.wsdl.PortType;
import org.eclipse.wst.wsdl.WSDLFactory;
import org.eclipse.wst.wsdl.internal.util.WSDLResourceFactoryImpl;
import org.eclipse.xsd.util.XSDResourceFactoryImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.uni_stuttgart.iaas.bpel_d.algorithm.analysis.AnalysisResult;
/**
 * Test for DataDependencyHelper
 * 
 * @since Jul 03, 2012
 * @author Daojun Cui
 *
 */
public class DataDependencyHelperTest {

	static File testFileDir = null;

	static Process process = null;

	static Definition definition = null;

	static PartitionSpecification partitionSpec1 = null;
	static PartitionSpecification partitionSpec2 = null;

	static AnalysisResult analysis = null;

	static RuntimeData data = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		//
		// use the process "OrderingProcess"
		//

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
		String bpelURI = testFileDir.getAbsolutePath()
				+ "\\OrderInfo\\bpelContent\\OrderingProcess.bpel";
		process = loadBPEL(bpelURI);

		// load wsdl
		definition = BPEL4ChorReader.readWSDL(testFileDir.getAbsolutePath()
				+ "\\OrderInfo\\bpelContent\\OrderingProcess.wsdl");

		// partition specification
		String partition1URI = testFileDir.getAbsolutePath()
				+ "\\OrderInfo\\bpelContent\\Partition1.xml";
		partitionSpec1 = loadPartitionSpec(partition1URI, process);
		String partition2URI = testFileDir.getAbsolutePath()
				+ "\\OrderInfo\\bpelContent\\Partition2.xml";
		partitionSpec2 = loadPartitionSpec(partition2URI, process);

		// analyse process
		analysis = DataFlowAnalyzer.analyze(process);

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

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		// fragment the process
		data = new RuntimeData(process, partitionSpec1, definition);
		ProcessFragmenter procFragmenter = new ProcessFragmenter(data);
		procFragmenter.fragmentizeProcess();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testConstructor() {
		// The DataDependencyHelper should hold the following global information
		// 1. participant to process map
		// 2. participant to definition map
		// 3. partitionSpec
		try {
			Activity act = BPELFactory.eINSTANCE.createActivity();
			Variable var = BPELFactory.eINSTANCE.createVariable();
			DataDependencyHelper helper = new DataDependencyHelper(
					data.getParticipant2FragProcMap(), data.getParticipant2WSDLMap(),
					partitionSpec1, act, var);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testExistSQMsgBetween() {
		Activity act = MyBPELUtils.resolveActivity("E", process);
		Variable var = MyBPELUtils.resolveVariable("paymentInfo", process);
		MyDataDependencyHelper helper = new MyDataDependencyHelper(
				data.getParticipant2FragProcMap(), data.getParticipant2WSDLMap(), partitionSpec1,
				act, var);

		Participant pn = partitionSpec1.getParticipant("x");
		Participant pr = partitionSpec1.getParticipant("z");
		Definition dfnpr = data.getParticipant2WSDLMap().get(pr.getName());

		// assert Single QuerySet Msg Not Exist
		assertSQMessageNotExist(helper, pn, pr);

		// insert a single querySet message
		Message msg = WSDLFactory.eINSTANCE.createMessage();
		msg.setQName(new QName(dfnpr.getTargetNamespace(), pn.getName() + pr.getName()
				+ var.getName() + "SQMessage"));
		dfnpr.addMessage(msg);

		// assert single querySet msg exist now
		assertSQMessageExist(helper, pn, pr);
	}

	private void assertSQMessageNotExist(MyDataDependencyHelper helper, Participant pn,
			Participant pr) {
		assertEquals(false, helper.existSQMsgBetween(pn, pr));
	}

	private void assertSQMessageExist(MyDataDependencyHelper helper, Participant pn, Participant pr) {
		assertEquals(true, helper.existSQMsgBetween(pn, pr));
	}

	@Test
	public void testExistPortTypeFor() {
		Activity act = MyBPELUtils.resolveActivity("E", process);
		Variable var = MyBPELUtils.resolveVariable("paymentInfo", process);
		MyDataDependencyHelper helper = new MyDataDependencyHelper(
				data.getParticipant2FragProcMap(), data.getParticipant2WSDLMap(), partitionSpec1,
				act, var);

		Participant pn = partitionSpec1.getParticipant("x");
		Participant pr = partitionSpec1.getParticipant("z");
		Definition dfn = data.getParticipant2WSDLMap().get(pr.getName());

		// assert porttype not exists
		assertPortTypeNotExist(helper, pn, pr);

		// insert a port
		StringBuffer ptname = new StringBuffer();
		ptname.append(pn.getName());
		ptname.append(pr.getName());
		ptname.append("PT");
		QName qname = new QName(dfn.getTargetNamespace(), ptname.toString());
		PortType pt = WSDLFactory.eINSTANCE.createPortType();
		pt.setQName(qname);
		dfn.addPortType(pt);

		// assert portType exist
		assertPortTypeExist(helper, pn, pr);
	}

	private void assertPortTypeNotExist(MyDataDependencyHelper helper, Participant pn,
			Participant pr) {
		assertEquals(false, helper.existPortTypeFor(pn, pr));

	}

	private void assertPortTypeExist(MyDataDependencyHelper helper, Participant pn, Participant pr) {
		assertEquals(true, helper.existPortTypeFor(pn, pr));
	}

	@Test
	public void testExistPartnerLinkTypeFor() {
		Activity act = MyBPELUtils.resolveActivity("E", process);
		Variable var = MyBPELUtils.resolveVariable("paymentInfo", process);
		MyDataDependencyHelper helper = new MyDataDependencyHelper(
				data.getParticipant2FragProcMap(), data.getParticipant2WSDLMap(), partitionSpec1,
				act, var);

		Participant pn = partitionSpec1.getParticipant("x");
		Participant pr = partitionSpec1.getParticipant("z");
		Definition dfn = data.getParticipant2WSDLMap().get(pr.getName());

		// assert partnerLinkType not exist
		assertPartnerLinkTypeNotExist(helper, pn, pr);

		// insert one partnerlinkType
		StringBuffer pltname = new StringBuffer();
		pltname.append(pn.getName());
		pltname.append(pr.getName());
		pltname.append("PLT");
		PartnerLinkType plt = PartnerlinktypeFactory.eINSTANCE.createPartnerLinkType();
		plt.setName(pltname.toString());
		dfn.addExtensibilityElement(plt);

		// assert partnerLinkType exists
		assertPartnerLinkTypeExist(helper, pn, pr);
	}

	private void assertPartnerLinkTypeNotExist(MyDataDependencyHelper helper, Participant pn,
			Participant pr) {
		assertEquals(false, helper.existPartnerLinkTypeFor(pn, pr));
	}

	private void assertPartnerLinkTypeExist(MyDataDependencyHelper helper, Participant pn,
			Participant pr) {
		assertEquals(true, helper.existPartnerLinkTypeFor(pn, pr));
	}

	@Test
	public void testExistPartnerLinkFor() {

		Activity act = MyBPELUtils.resolveActivity("E", process);
		Variable var = MyBPELUtils.resolveVariable("paymentInfo", process);
		MyDataDependencyHelper helper = new MyDataDependencyHelper(
				data.getParticipant2FragProcMap(), data.getParticipant2WSDLMap(), partitionSpec1,
				act, var);

		Participant pn = partitionSpec1.getParticipant("x");
		Participant pr = partitionSpec1.getParticipant("z");
		Process procn = data.getParticipant2FragProcMap().get(pn.getName());
		Process procr = data.getParticipant2FragProcMap().get(pr.getName());

		// assert partnerLink not exist
		assertPartnerLinkNotExist(helper, pn, pr);

		// insert partnerLink to pn and pr
		StringBuffer plname = new StringBuffer();
		plname.append(pn.getName());
		plname.append(pr.getName());
		plname.append("PL");
		PartnerLink pln = BPELFactory.eINSTANCE.createPartnerLink();
		pln.setName(plname.toString());
		PartnerLink plr = BPELFactory.eINSTANCE.createPartnerLink();
		plr.setName(plname.toString());

		procr.getPartnerLinks().getChildren().add(pln);
		procn.getPartnerLinks().getChildren().add(plr);

		// assert partnerLink exist
		assertPartnerLinkExist(helper, pn, pr);
	}

	private void assertPartnerLinkNotExist(MyDataDependencyHelper helper, Participant pn,
			Participant pr) {
		assertEquals(false, helper.existPartnerLink(pn, pr));

	}

	private void assertPartnerLinkExist(MyDataDependencyHelper helper, Participant pn,
			Participant pr) {
		assertEquals(true, helper.existPartnerLink(pn, pr));

	}

	@Test
	public void testCreatePrerequisiteSQMessage() {

		Activity act = MyBPELUtils.resolveActivity("E", process);
		Variable var = MyBPELUtils.resolveVariable("paymentInfo", process);
		MyDataDependencyHelper helper = new MyDataDependencyHelper(
				data.getParticipant2FragProcMap(), data.getParticipant2WSDLMap(), partitionSpec1,
				act, var);

		Participant pn = partitionSpec1.getParticipant("x");
		Participant pr = partitionSpec1.getParticipant("z");

		// create single query message
		helper.createPrerequisiteSQMessage(pn, pr);

		// assert the SQMessage exist
		assertSQMessageExist(helper, pn, pr);

		// assert the propertyAlias exist
		assertPropertyAlias4SQMessageExist(helper, pn, pr);
	}

	private void assertPropertyAlias4SQMessageExist(MyDataDependencyHelper helper, Participant pn,
			Participant pr) {

		// definition of reader participant
		Definition defn = helper.getParticipant2WSDLMap().get(pr.getName());

		StringBuffer sqMsgNameSb = new StringBuffer();
		sqMsgNameSb.append(pn.getName());
		sqMsgNameSb.append(pr.getName());
		sqMsgNameSb.append(helper.getVar().getName());
		sqMsgNameSb.append("SQMessage");
		QName msgQName = new QName(defn.getTargetNamespace(), sqMsgNameSb.toString());

		// property qname
		QName propertyQName = new QName(defn.getTargetNamespace(),
				SplitProcessConstants.CORRELATION_PROPERTY_NAME);

		// search the propertyAlias
		PropertyAlias alias = MyWSDLUtil.findPropertyAlias(defn, propertyQName, msgQName,
				SplitProcessConstants.CORRELATION_PART_NAME);

		// it should be not null
		assertNotNull(alias);
	}

	@Test
	public void testCreatePrerequisiteMQMessage() throws WSDLException, IOException {
		Activity act = MyBPELUtils.resolveActivity("E", process);
		Variable var = MyBPELUtils.resolveVariable("paymentInfo", process);
		MyDataDependencyHelper helper = new MyDataDependencyHelper(
				data.getParticipant2FragProcMap(), data.getParticipant2WSDLMap(), partitionSpec1,
				act, var);

		Participant pn = partitionSpec1.getParticipant("x");
		Participant pr = partitionSpec1.getParticipant("z");

		Set<String> qs1 = new HashSet<String>();
		Set<String> qs2 = new HashSet<String>();

		qs1.add("paymentInfo.actNum");
		qs2.add("paymentInfo.amt");

		Map<Set<String>, Set<Activity>> query2WriterMap = new HashMap<Set<String>, Set<Activity>>();
		query2WriterMap.put(qs1, null);
		query2WriterMap.put(qs2, null);
		MyQueryWriterSet qws = new MyQueryWriterSet(act, var, query2WriterMap);
		String idn = "AVirtualUniqueString";

		Map<Set<String>, String> id = new HashMap<Set<String>, String>();
		id.put(qs1, "xdk");
		id.put(qs2, "lk3");

		// create Multiple querySet message
		helper.createPrerequisiteMQMessage(pn, pr, qws, id, idn);

		// assert MQMessage exists
		assertMQMessageAndPropertyAliasExist(pn, pr, qws, idn);
	}

	private void assertMQMessageAndPropertyAliasExist(Participant pn, Participant pr, MyQueryWriterSet qws,
			String idn) throws WSDLException, IOException {
		StringBuffer sb = new StringBuffer();
		sb.append(pn.getName());
		sb.append(pr.getName());
		sb.append(qws.getVariable().getName());
		sb.append(idn);
		sb.append("MQMessage");
		Definition dfn = data.getParticipant2WSDLMap().get(pr.getName());
		
		MyWSDLUtil.print(dfn);
		
		QName msgQname = new QName(dfn.getTargetNamespace(), sb.toString());
		Message msg = MyWSDLUtil.resolveMessage(dfn, msgQname);
		assertNotNull(msg);

		List<Part> parts = msg.getEParts();
		assertEquals(4, parts.size());

		// assert the propertyAlias that points the correlation property to this
		// message exists
		QName propertyQName = new QName(dfn.getTargetNamespace(),
				SplitProcessConstants.CORRELATION_PROPERTY_NAME);

		PropertyAlias alias = MyWSDLUtil.findPropertyAlias(dfn, propertyQName, msgQname,
				SplitProcessConstants.CORRELATION_PART_NAME);
		
		assertNotNull(alias);
	}

	@Test
	public void testCreatePrerequisiteMessage() throws WSDLException, IOException {
		Activity act = MyBPELUtils.resolveActivity("G", process);
		Variable var = MyBPELUtils.resolveVariable("paymentInfo", process);
		MyDataDependencyHelper helper = new MyDataDependencyHelper(
				data.getParticipant2FragProcMap(), data.getParticipant2WSDLMap(), partitionSpec1,
				act, var);

		// two partitions
		Participant pn1 = partitionSpec1.getParticipant("x");// A, B
		Participant pn2 = partitionSpec1.getParticipant("y");// C, E
		Participant pr = partitionSpec1.getParticipant("w");

		// two query sets
		Set<String> qs1 = new HashSet<String>();
		Set<String> qs2 = new HashSet<String>();
		Set<String> qs3 = new HashSet<String>();

		qs1.add("paymentInfo.actNum");
		qs2.add("paymentInfo.amt");

		// two writer sets
		Set<Activity> ws1 = new HashSet<Activity>();
		Set<Activity> ws2 = new HashSet<Activity>();

		// two virtual pwdg nodes, one with only one query set, one with
		// multiple query set
		PWDGNode n1 = new PWDGNode();
		PWDGNode n2 = new PWDGNode();
		n1.setParticipant("x");
		n2.setParticipant("y");

		// querySet to writerSet map 1, single query
		Map<Set<String>, Set<Activity>> query2WriterMap1 = new HashMap<Set<String>, Set<Activity>>();
		query2WriterMap1.put(qs1, null);
		MyQueryWriterSet qws1 = new MyQueryWriterSet(act, var, query2WriterMap1);

		// querySet to writerSet map 1, multiple queries
		Map<Set<String>, Set<Activity>> query2WriterMap2 = new HashMap<Set<String>, Set<Activity>>();
		query2WriterMap2.put(qs2, null);
		query2WriterMap2.put(qs3, null);
		MyQueryWriterSet qws2 = new MyQueryWriterSet(act, var, query2WriterMap2);

		// id map for query sets
		Map<Set<String>, String> id = new HashMap<Set<String>, String>();
		id.put(qs1, "xdk");
		id.put(qs2, "lk3");
		id.put(qs3, "nix");

		// id map for nodes
		Map<PWDGNode, String> idn = new HashMap<PWDGNode, String>();
		idn.put(n1, "xm0");
		idn.put(n2, "qeo");

		// create prerequisite message for node1
		helper.createPrerequisiteMessage(n1, qws1, id, idn);

		// assert single querySet message exist
		assertSQMessageExist(helper, pn1, pr);

		// create prerequisite message for node2
		helper.createPrerequisiteMessage(n2, qws2, id, idn);

		// assert multiple querySet message exist
		assertMQMessageAndPropertyAliasExist(pn2, pr, qws2, idn.get(n2));
	}

	@Test
	public void testCreatePrerequisitePortType() {

		Activity act = MyBPELUtils.resolveActivity("G", process);
		Variable var = MyBPELUtils.resolveVariable("paymentInfo", process);
		MyDataDependencyHelper helper = new MyDataDependencyHelper(
				data.getParticipant2FragProcMap(), data.getParticipant2WSDLMap(), partitionSpec1,
				act, var);

		// two partitions
		Participant pn = partitionSpec1.getParticipant("x");// A, B
		Participant pr = partitionSpec1.getParticipant("w");// C, E

		// assert portType not exist
		assertPortTypeNotExist(helper, pn, pr);

		// create portType
		helper.createPrerequisitePortType(pn, pr);

		// assert portType exist
		assertPortTypeExist(helper, pn, pr);
	}

	@Test
	public void testCreatePrerequisiteOperation() {
		Activity act = MyBPELUtils.resolveActivity("G", process);
		Variable var = MyBPELUtils.resolveVariable("paymentInfo", process);
		MyDataDependencyHelper helper = new MyDataDependencyHelper(
				data.getParticipant2FragProcMap(), data.getParticipant2WSDLMap(), partitionSpec1,
				act, var);

		// two partitions
		Participant pn = partitionSpec1.getParticipant("x");// A, B
		Participant pr = partitionSpec1.getParticipant("w");// C, E

		// node n
		PWDGNode n = new PWDGNode();

		// id map for node n
		Map<PWDGNode, String> idn = new HashMap<PWDGNode, String>();
		idn.put(n, "zlx");

		// get the definition of reader participant
		Definition dfn = data.getParticipant2WSDLMap().get(pr.getName());

		boolean isSQMsg = true;

		// message for the operation
		helper.createPrerequisiteSQMessage(pn, pr);
		Message msg = helper.getMessageFor(pn, pr, n, isSQMsg, idn);

		// the portType for the operation
		helper.createPrerequisitePortType(pn, pr);
		PortType pt = helper.getPortTypeFor(pn, pr);

		// assert operation not exist
		assertOperationNotExist(pn, pr, dfn, var, idn.get(n), pt);

		// create operation
		helper.createPrerequisiteOperation(pn, pr, msg, idn.get(n), pt);

		// assert operation exist
		assertOperationExist(pn, pr, dfn, var, idn.get(n), pt, isSQMsg);

	}

	private void assertOperationNotExist(Participant pn, Participant pr, Definition dfn,
			Variable var, String idn, PortType pt) {
		StringBuffer sb = new StringBuffer();
		sb.append(pn.getName());
		sb.append(pr.getName());
		sb.append(var.getName());
		sb.append(idn);
		sb.append("OP");
		assertEquals(true, MyWSDLUtil.resolveOperation(dfn, pt.getQName(), sb.toString()) == null);
	}

	private void assertOperationExist(Participant pn, Participant pr, Definition dfn, Variable var,
			String idn, PortType pt, boolean isSQMsg) {
		StringBuffer sb = new StringBuffer();
		sb.append(pn.getName());
		sb.append(pr.getName());
		sb.append(var.getName());
		sb.append(idn);
		sb.append("OP");
		Operation op = MyWSDLUtil.resolveOperation(dfn, pt.getQName(), sb.toString());
		assertEquals(true, op != null);

		// assert message in operation correctly created
		assertMsgInOperationCorrectlyCreated(pn, pr, dfn, var, idn, op, isSQMsg);
	}

	private void assertMsgInOperationCorrectlyCreated(Participant pn, Participant pr,
			Definition dfn, Variable var, String idn, Operation op, boolean isSQMsg) {
		Input input = (Input) op.getInput();
		Message msg = input.getEMessage();
		StringBuffer sb = new StringBuffer();
		QName qname = null;
		if (isSQMsg) {
			sb.append(pn.getName());
			sb.append(pr.getName());
			sb.append(var.getName());
			sb.append("SQMessage");
			qname = new QName(dfn.getTargetNamespace(), sb.toString());
			assertEquals(true, msg.getQName().equals(qname));
		} else {
			sb.append(pn.getName());
			sb.append(pr.getName());
			sb.append(var.getName());
			sb.append(idn);
			sb.append("MQMessage");
			qname = new QName(dfn.getTargetNamespace(), sb.toString());
			assertEquals(true, msg.getQName().equals(qname));
		}

	}

	@Test
	public void testCreatePrerequisitePortTypeAndOperation() {
		Activity act = MyBPELUtils.resolveActivity("G", process);
		Variable var = MyBPELUtils.resolveVariable("paymentInfo", process);
		MyDataDependencyHelper helper = new MyDataDependencyHelper(
				data.getParticipant2FragProcMap(), data.getParticipant2WSDLMap(), partitionSpec1,
				act, var);

		// two partitions
		Participant pn = partitionSpec1.getParticipant("x");// A, B
		Participant pr = partitionSpec1.getParticipant("w");// C, E

		// assert no portType exists
		assertPortTypeNotExist(helper, pn, pr);

		// create a message first, for the operation and portType

		// node n
		PWDGNode n = new PWDGNode();

		// id map for node n
		Map<PWDGNode, String> idn = new HashMap<PWDGNode, String>();
		idn.put(n, "zlx");

		// two query sets
		Set<String> qs1 = new HashSet<String>();
		Set<String> qs2 = new HashSet<String>();

		qs1.add(".actNum");
		qs2.add(".amt");

		Map<Set<String>, String> id = new HashMap<Set<String>, String>();
		id.put(qs1, "23k");
		id.put(qs2, "ckm");

		// querySet to writerSet map 1, multiple querySets
		Map<Set<String>, Set<Activity>> query2WriterMap1 = new HashMap<Set<String>, Set<Activity>>();
		query2WriterMap1.put(qs1, null);
		query2WriterMap1.put(qs2, null);
		MyQueryWriterSet qws1 = new MyQueryWriterSet(act, var, query2WriterMap1);

		boolean isSQMsg = false;

		// message for the operation
		helper.createPrerequisiteMQMessage(pn, pr, qws1, id, idn.get(n));
		Message msg = helper.getMessageFor(pn, pr, n, isSQMsg, idn);

		// create portType and operation
		helper.createPrerequisitePortTypeOperation(pn, pr, n, qws1, idn);

		// assert portType exists
		assertPortTypeExist(helper, pn, pr);

		// assert operation exists
		Definition dfn = data.getParticipant2WSDLMap().get(pr.getName());
		PortType pt = helper.getPortTypeFor(pn, pr);
		assertOperationExist(pn, pr, dfn, var, idn.get(n), pt, isSQMsg);
	}

	@Test
	public void testCreatePrerequisitePartnerLinkType() {
		Activity act = MyBPELUtils.resolveActivity("G", process);
		Variable var = MyBPELUtils.resolveVariable("paymentInfo", process);
		MyDataDependencyHelper helper = new MyDataDependencyHelper(
				data.getParticipant2FragProcMap(), data.getParticipant2WSDLMap(), partitionSpec1,
				act, var);

		// two partitions
		Participant pn = partitionSpec1.getParticipant("x");// A, B
		Participant pr = partitionSpec1.getParticipant("w");// C, E

		//
		// create message
		//

		// node n
		PWDGNode n = new PWDGNode();

		// id map for node n
		Map<PWDGNode, String> idn = new HashMap<PWDGNode, String>();
		idn.put(n, "zlx");

		// two query sets
		Set<String> qs1 = new HashSet<String>();

		qs1.add(".actNum");

		Map<Set<String>, String> id = new HashMap<Set<String>, String>();
		id.put(qs1, "23k");

		// querySet to writerSet map 1, multiple querySets
		Map<Set<String>, Set<Activity>> query2WriterMap1 = new HashMap<Set<String>, Set<Activity>>();
		query2WriterMap1.put(qs1, null);
		MyQueryWriterSet qws1 = new MyQueryWriterSet(act, var, query2WriterMap1);

		// message for the operation
		helper.createPrerequisiteSQMessage(pn, pr);

		// create portType and operation
		helper.createPrerequisitePortTypeOperation(pn, pr, n, qws1, idn);

		// assert partnerlinkType not exist
		assertPartnerLinkTypeNotExist(pn, pr);

		// create partnerLinkType
		helper.createPrerequisitePartnerLinkType(pn, pr);

		// assert partnerLinkType exist
		assertPartnerLinkTypeExistInWSDL(pn, pr);
	}

	private void assertPartnerLinkTypeNotExist(Participant pn, Participant pr) {

		Definition dfn = data.getParticipant2WSDLMap().get(pr.getName());

		StringBuffer sb = new StringBuffer();
		sb.append(pn.getName());
		sb.append(pr.getName());
		sb.append("PLT");

		PartnerLinkType plt = MyWSDLUtil.findPartnerLinkType(dfn, sb.toString());
		assertEquals(true, plt == null);
	}

	private void assertPartnerLinkTypeExistInWSDL(Participant pn, Participant pr) {

		Definition dfn = data.getParticipant2WSDLMap().get(pr.getName());

		StringBuffer sb = new StringBuffer();
		sb.append(pn.getName());
		sb.append(pr.getName());
		sb.append("PLT");

		PartnerLinkType plt = MyWSDLUtil.findPartnerLinkType(dfn, sb.toString());
		assertEquals(true, plt != null);

		StringBuffer sb2 = new StringBuffer();
		sb2.append(pn.getName());
		sb2.append(pr.getName());
		sb2.append("ROLE");

		QName pltqname = new QName(dfn.getTargetNamespace(), sb.toString());
		String rolename = sb2.toString();
		EObject role = MyWSDLUtil.resolveBPELRole(dfn, pltqname, rolename);
		assertEquals(true, role != null);

		StringBuffer sb3 = new StringBuffer();
		sb3.append(pn.getName());
		sb3.append(pr.getName());
		sb3.append("PT");
		QName ptqname = new QName(dfn.getTargetNamespace(), sb3.toString());
		PortType pt = MyWSDLUtil.resolvePortType(dfn, ptqname);
		assertEquals(true, pt != null);

	}

	@Test
	public void testCreatePrerequisitePartnerLink() {
		Activity act = MyBPELUtils.resolveActivity("G", process);
		Variable var = MyBPELUtils.resolveVariable("paymentInfo", process);
		MyDataDependencyHelper helper = new MyDataDependencyHelper(
				data.getParticipant2FragProcMap(), data.getParticipant2WSDLMap(), partitionSpec1,
				act, var);

		// two partitions
		Participant pn = partitionSpec1.getParticipant("x");// A, B
		Participant pr = partitionSpec1.getParticipant("w");// C, E

		// assert partnerLink not exist
		assertPartnerLinkNotExist(helper, pn, pr);

		// create partnerLinkType first then partnerLink
		helper.createPrerequisitePartnerLinkType(pn, pr);
		helper.createPrerequisitePartnerLink(pn, pr);

		// assert partnerLink exists
		assertPartnerLinkExist(helper, pn, pr);
		assertPartnerLinkTypeExistInBothPartnerLinks(pn, pr);
	}

	private void assertPartnerLinkTypeExistInBothPartnerLinks(Participant pn, Participant pr) {
		Process procN = data.getFragmentProcess(pn.getName());
		Process procR = data.getFragmentProcess(pr.getName());
		StringBuffer plname = new StringBuffer();
		plname.append(pn.getName());
		plname.append(pr.getName());
		plname.append("PL");
		PartnerLink pln = MyBPELUtils.getPartnerLink(procN, plname.toString());
		PartnerLink plr = MyBPELUtils.getPartnerLink(procR, plname.toString());
		PartnerLinkType plt4n = pln.getPartnerLinkType();
		PartnerLinkType plt4r = plr.getPartnerLinkType();
		assertEquals(true, plt4n != null);
		assertEquals(true, plt4r != null);
		assertEquals(plt4n, plt4r);
	}

	@Test
	public void testCreatePrerequisites() throws WDGException, PWDGException {

		Activity act = MyBPELUtils.resolveActivity("G", process);
		Variable var = MyBPELUtils.resolveVariable("paymentInfo", process);
		MyDataDependencyHelper helper = new MyDataDependencyHelper(
				data.getParticipant2FragProcMap(), data.getParticipant2WSDLMap(), partitionSpec1,
				act, var);

		// participant of G is 'w'
		Participant pr = partitionSpec1.getParticipant("w");

		// create queryWriterTuple Q
		QueryWriterSet qws = AnalysisResultParser.parse(act, var, analysis);

		// create pwdg(act, var)
		WDG wdg = WDGFactory.createWDG(qws.getAllWriters());
		PWDG pwdg = PWDGFactory.createPWDG(wdg, process, partitionSpec1);

		// create id map
		Map<Set<String>, String> query2NameMap = initQuery2NameMap(var.getName(), qws.querySets());

		// create idn map
		Map<PWDGNode, String> node2NameMap = initNode2NameMap(pwdg);

		// create prerequisites
		helper.createPrerequisites(pwdg, qws, query2NameMap, node2NameMap);

		// assert there are 3 more messages in participant 'w' WSDL definition
		assertMessagesForDataDependencyExist();

		// assert there are 3 more portTypes in participant 'w' WSDL definition
		assertPortTypesForDataDependencyExist();

		// assert there are 3 more partnerLinkType in participant 'w' WSDL
		// definition
		assertPartnerLinkTypeForDataDependencyExist();

		// assert there are 3 more partnerLinks in participant 'w' WSDL
		// definition
		assertPartnerLinkForDataDependencyExist();
	}

	private void assertMessagesForDataDependencyExist() {

		assertSQMessageBetweenXandWExist();
		assertSQMessageBetweenYandWExist();
		assertSQMessageBetweenZandWExist();
	}

	private void assertSQMessageBetweenXandWExist() {
		Definition dfn = data.getFragmentDefinition("w");

		String msqname = "xwpaymentInfoSQMessage";

		boolean mqmsgExist = false;
		for (Object key : dfn.getMessages().keySet()) {
			QName qname = (QName) key;
			if (msqname.equals(qname.getLocalPart())) {
				mqmsgExist = true;
				break;
			}
		}
		assertEquals(true, mqmsgExist);
	}

	private void assertSQMessageBetweenYandWExist() {
		Definition dfn = data.getFragmentDefinition("w");
		String msgname = "ywpaymentInfoSQMessage";

		boolean mqmsgExist = false;
		for (Object key : dfn.getMessages().keySet()) {
			QName qname = (QName) key;
			if (msgname.equals(qname.getLocalPart())) {
				mqmsgExist = true;
				break;
			}
		}
		assertEquals(true, mqmsgExist);
	}

	private void assertSQMessageBetweenZandWExist() {
		Definition dfn = data.getFragmentDefinition("w");
		String msgname = "zwpaymentInfoSQMessage";

		boolean mqmsgExist = false;
		for (Object key : dfn.getMessages().keySet()) {
			QName qname = (QName) key;
			if (msgname.equals(qname.getLocalPart())) {
				mqmsgExist = true;
				break;
			}
		}
		assertEquals(true, mqmsgExist);
	}

	private void assertPortTypesForDataDependencyExist() {
		assertPortTypeForXandWExist();
		assertPortTypeForYandWExist();
		assertPortTypeForZandWExist();
	}

	private void assertPortTypeForXandWExist() {
		Definition dfn = data.getFragmentDefinition("w");
		String ptname = "xwPT";
		QName ptqname = new QName(dfn.getTargetNamespace(), ptname);
		PortType pt = MyWSDLUtil.resolvePortType(dfn, ptqname);
		assertEquals(true, pt != null);
	}

	private void assertPortTypeForYandWExist() {
		Definition dfn = data.getFragmentDefinition("w");
		String ptname = "ywPT";
		QName ptqname = new QName(dfn.getTargetNamespace(), ptname);
		PortType pt = MyWSDLUtil.resolvePortType(dfn, ptqname);
		assertEquals(true, pt != null);
	}

	private void assertPortTypeForZandWExist() {
		Definition dfn = data.getFragmentDefinition("w");
		String ptname = "zwPT";
		QName ptqname = new QName(dfn.getTargetNamespace(), ptname);
		PortType pt = MyWSDLUtil.resolvePortType(dfn, ptqname);
		assertEquals(true, pt != null);
	}

	private void assertPartnerLinkTypeForDataDependencyExist() {
		assertPartnerLinkTypeForXandWExist();
		assertPartnerLinkTypeForYandWExist();
		assertPartnerLinkTypeForZandWExist();
	}

	private void assertPartnerLinkTypeForXandWExist() {
		Definition dfn = data.getFragmentDefinition("w");
		String ptname = "xwPLT";
		QName pltqname = new QName(dfn.getTargetNamespace(), ptname);
		PartnerLinkType plt = MyWSDLUtil.resolveBPELPartnerLinkType(dfn, pltqname);
		assertEquals(true, plt != null);

	}

	private void assertPartnerLinkTypeForYandWExist() {
		Definition dfn = data.getFragmentDefinition("w");
		String ptname = "ywPLT";
		QName pltqname = new QName(dfn.getTargetNamespace(), ptname);
		PartnerLinkType plt = MyWSDLUtil.resolveBPELPartnerLinkType(dfn, pltqname);
		assertEquals(true, plt != null);

	}

	private void assertPartnerLinkTypeForZandWExist() {
		Definition dfn = data.getFragmentDefinition("w");
		String ptname = "zwPLT";
		QName pltqname = new QName(dfn.getTargetNamespace(), ptname);
		PartnerLinkType plt = MyWSDLUtil.resolveBPELPartnerLinkType(dfn, pltqname);
		assertEquals(true, plt != null);

	}

	private void assertPartnerLinkForDataDependencyExist() {
		assertPartnerLinkForXandWExist();
		assertPartnerLinkForYandWExist();
		assertPartnerLinkForZandWExist();
	}

	private void assertPartnerLinkForXandWExist() {
		Process procx = data.getFragmentProcess("x");
		Process procw = data.getFragmentProcess("w");

		String plname = "xwPL";

		PartnerLink plx = MyBPELUtils.getPartnerLink(procx, plname);
		assertEquals(true, plx != null);
		PartnerLinkType pltx = plx.getPartnerLinkType();
		assertEquals(true, pltx != null);

		PartnerLink plw = MyBPELUtils.getPartnerLink(procw, plname);
		assertEquals(true, plw != null);
		PartnerLinkType pltw = plw.getPartnerLinkType();
		assertEquals(true, pltw != null);

	}

	private void assertPartnerLinkForYandWExist() {
		Process procw = data.getFragmentProcess("w");
		Process procy = data.getFragmentProcess("y");

		String plname = "ywPL";

		PartnerLink ply = MyBPELUtils.getPartnerLink(procy, plname);
		assertEquals(true, ply != null);
		PartnerLinkType plty = ply.getPartnerLinkType();
		assertEquals(true, plty != null);

		PartnerLink plw = MyBPELUtils.getPartnerLink(procw, plname);
		assertEquals(true, plw != null);
		PartnerLinkType pltw = plw.getPartnerLinkType();
		assertEquals(true, pltw != null);
	}

	private void assertPartnerLinkForZandWExist() {
		Process procw = data.getFragmentProcess("w");
		Process procz = data.getFragmentProcess("z");

		String plname = "zwPL";

		PartnerLink plz = MyBPELUtils.getPartnerLink(procz, plname);
		assertEquals(true, plz != null);
		PartnerLinkType pltz = plz.getPartnerLinkType();
		assertEquals(true, pltz != null);

		PartnerLink plw = MyBPELUtils.getPartnerLink(procw, plname);
		assertEquals(true, plw != null);
		PartnerLinkType pltw = plw.getPartnerLinkType();
		assertEquals(true, pltw != null);
	}

	private Map<Set<String>, String> initQuery2NameMap(String varName, Set<Set<String>> querySets) {
		Map<Set<String>, String> querySet2NameMap = new HashMap<Set<String>, String>();

		for (Set<String> key : querySets) {

			StringBuffer sb = new StringBuffer();
			sb.append(varName);

			for (String query : key) {
				if (query.startsWith("."))
					sb.append(query.substring(1));
				else
					sb.append(query);

			}
			sb.append("-");
			sb.append(RandomIdGenerator.getId());
			querySet2NameMap.put(key, sb.toString());
		}
		return querySet2NameMap;
	}

	private Map<PWDGNode, String> initNode2NameMap(PWDG pwdg) {
		Map<PWDGNode, String> node2NameMap = new HashMap<PWDGNode, String>();

		for (PWDGNode node : pwdg.vertexSet()) {
			StringBuffer sb = new StringBuffer();
			for (Activity act : node.getActivities()) {
				sb.append(act.getName());
			}
			sb.append("-");
			sb.append(RandomIdGenerator.getId());
			node2NameMap.put(node, sb.toString());
		}

		return node2NameMap;
	}
}

class MyQueryWriterSet extends QueryWriterSet {

	protected MyQueryWriterSet(Activity act, Variable var,
			Map<Set<String>, Set<Activity>> query2WriterMap) {
		super(act, var, query2WriterMap);
	}

}

class MyDataDependencyHelper extends DataDependencyHelper {

	public MyDataDependencyHelper(Map<String, Process> participant2fragProc,
			Map<String, Definition> participant2wsdl, PartitionSpecification partitionSpec,
			Activity act, Variable var) {
		super(participant2fragProc, participant2wsdl, partitionSpec, act, var);
	}

	public Variable getVar() {
		return super.var;
	}

	public Activity getAct() {
		return super.act;
	}

	public Map<String, Definition> getParticipant2WSDLMap() {
		return super.participant2wsdl;
	}

	public boolean existSQMsgBetween(Participant pn, Participant pr) {
		return super.existSQMsgBetween(pn, pr);
	}

	public boolean existPortTypeFor(Participant pn, Participant pr) {
		return super.existPortTypeFor(pn, pr);
	}

	public boolean existPartnerLinkTypeFor(Participant pn, Participant pr) {
		return super.existPartnerLinkTypeFor(pn, pr);
	}

	public boolean existPartnerLink(Participant pn, Participant pr) {
		return super.existPartnerLinkBetween(pn, pr);
	}

	public void createPrerequisiteSQMessage(Participant pn, Participant pr) {
		super.createPrerequisiteSQMessageDiffPartition(pn, pr);
	}

	public void createPrerequisiteMQMessage(Participant pn, Participant pr, QueryWriterSet qws,
			Map<Set<String>, String> id, String idn) {
		super.createPrerequisiteMQMessageDiffPartition(pn, pr, qws, id, idn);
	}

	public void createPrerequisiteMessage(PWDGNode n1, QueryWriterSet qws1,
			Map<Set<String>, String> id, Map<PWDGNode, String> idn) {
		super.createPrerequisiteMessage(n1, qws1, id, idn);
	}

	public PortType getPortTypeFor(Participant pn, Participant pr) {
		return super.getPortTypeFor(pn, pr);
	}

	public Message getMessageFor(Participant pn, Participant pr, PWDGNode n, boolean isSQMsg,
			Map<PWDGNode, String> idn) {
		return super.getMessageFor(pn, pr, n, isSQMsg, idn);
	}

	public void createPrerequisiteOperation(Participant pn, Participant pr, Message msg,
			String idn, PortType pt) {
		super.createPrerequisiteOperation(pn, pr, msg, idn, pt);
	}

	public void createPrerequisitePortType(Participant pn, Participant pr) {
		super.createPrerequisitePortType(pn, pr);
	}

	public void createPrerequisitePortTypeOperation(Participant pn, Participant pr, PWDGNode n,
			QueryWriterSet qws1, Map<PWDGNode, String> idn) {
		super.createPrerequisitePortTypeOperation(pn, pr, n, qws1, idn);
	}

	public void createPrerequisitePartnerLinkType(Participant pn, Participant pr) {
		super.createPrerequisitePartnerLinkType(pn, pr);
	}

	public void createPrerequisitePartnerLink(Participant pn, Participant pr) {
		super.createPrerequisitePartnerLink(pn, pr);
	}

}
