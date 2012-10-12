package org.bpel4chor.splitprocess.test.fragmentation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.wsdl.WSDLException;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;
import org.bpel4chor.splitprocess.ProcessSplitter;
import org.bpel4chor.splitprocess.RuntimeData;
import org.bpel4chor.splitprocess.dataflowanalysis.DataFlowAnalyzer;
import org.bpel4chor.splitprocess.exceptions.DataFlowAnalysisException;
import org.bpel4chor.splitprocess.exceptions.PartitionSpecificationException;
import org.bpel4chor.splitprocess.exceptions.SplitControlLinkException;
import org.bpel4chor.splitprocess.exceptions.SplitDataDependencyException;
import org.bpel4chor.splitprocess.partition.model.PartitionSpecification;
import org.bpel4chor.splitprocess.partition.representation.PartitionSpecReader;
import org.bpel4chor.utils.BPEL4ChorReader;
import org.eclipse.bpel.model.BPELPlugin;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.resource.BPELResourceFactoryImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.wst.wsdl.Definition;
import org.eclipse.wst.wsdl.internal.util.WSDLResourceFactoryImpl;
import org.eclipse.xsd.util.XSDResourceFactoryImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.uni_stuttgart.iaas.bpel_d.algorithm.analysis.AnalysisResult;

/**
 * Test for BPEL4Chor output
 * 
 * @since Jun 20, 2012
 * @author Daojun Cui
 * 
 */
public class BPEL4ChorOutputTest {

	static File testFileDir = null;

	static Process process = null;

	static Definition definition = null;

	static RuntimeData data = null;

	static PartitionSpecification partitionSpec = null;

	static AnalysisResult analysis = null;

	static Logger log = Logger.getLogger(DataDependencyFragmenterTestCase2.class);

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
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testBEPL4ChorOutput1() throws WSDLException, IOException, JAXBException,
			PartitionSpecificationException, DataFlowAnalysisException, SplitControlLinkException,
			SplitDataDependencyException, XMLStreamException {

		//
		// use the process "OrderingProcess" in Project OrderInfoSample1
		//

		// load bpel resource
		String bpelURI = testFileDir.getAbsolutePath()
				+ "\\OrderInfoSimple1\\bpelContent\\OrderingProcessSimple1.bpel";
		process = loadBPEL(bpelURI);

		// load wsdl
		definition = BPEL4ChorReader.readWSDL(testFileDir.getAbsolutePath()
				+ "\\OrderInfoSimple1\\bpelContent\\OrderingProcessSimple1.wsdl");

		// partition specification
		String partitionURI = testFileDir.getAbsolutePath()
				+ "\\OrderInfoSimple1\\bpelContent\\Partition.xml";
		partitionSpec = loadPartitionSpec(partitionURI, process);

		// analyse process
		analysis = DataFlowAnalyzer.analyze(process);

		// runtime data
		data = new RuntimeData(process, partitionSpec, definition);

		// setup process splitter
		ProcessSplitter spliter = new ProcessSplitter(process, partitionSpec);

		// split
		spliter.split();

	}

	@Test
	public void testBEPL4ChorOutput2() throws WSDLException, IOException, JAXBException,
			PartitionSpecificationException, DataFlowAnalysisException, SplitControlLinkException,
			SplitDataDependencyException, XMLStreamException {

		//
		// use the process "OrderingProcess" in Project OrderInfoDDTestCase1
		//

		// load bpel resource
		String bpelURI = testFileDir.getAbsolutePath()
				+ "\\OrderInfo4DDTestCase1\\bpelContent\\OrderingProcess.bpel";
		process = loadBPEL(bpelURI);

		// load wsdl
		definition = BPEL4ChorReader.readWSDL(testFileDir.getAbsolutePath()
				+ "\\OrderInfo4DDTestCase1\\bpelContent\\OrderingProcess.wsdl");

		// partition specification
		String partitionURI = testFileDir.getAbsolutePath()
				+ "\\OrderInfo4DDTestCase1\\bpelContent\\Partition.xml";
		partitionSpec = loadPartitionSpec(partitionURI, process);

		// analyse process
		analysis = DataFlowAnalyzer.analyze(process);

		// runtime data
		data = new RuntimeData(process, partitionSpec, definition);

		// setup process splitter
		ProcessSplitter spliter = new ProcessSplitter(process, partitionSpec);

		// split
		spliter.split();
	}

	@Test
	public void testBEPL4ChorOutput3() throws WSDLException, IOException, JAXBException,
			PartitionSpecificationException, DataFlowAnalysisException, SplitControlLinkException,
			SplitDataDependencyException, XMLStreamException {

		//
		// use the process "OrderingProcess" in Project OrderInfoDDTestCase2
		//

		// load bpel resource
		String bpelURI = testFileDir.getAbsolutePath()
				+ "\\OrderInfo4DDTestCase2\\bpelContent\\OrderingProcess.bpel";
		process = loadBPEL(bpelURI);

		// load wsdl
		definition = BPEL4ChorReader.readWSDL(testFileDir.getAbsolutePath()
				+ "\\OrderInfo4DDTestCase2\\bpelContent\\OrderingProcess.wsdl");

		// partition specification
		String partitionURI = testFileDir.getAbsolutePath()
				+ "\\OrderInfo4DDTestCase2\\bpelContent\\Partition.xml";
		partitionSpec = loadPartitionSpec(partitionURI, process);

		// analyse process
		analysis = DataFlowAnalyzer.analyze(process);

		// runtime data
		data = new RuntimeData(process, partitionSpec, definition);

		// setup process splitter
		ProcessSplitter spliter = new ProcessSplitter(process, partitionSpec);

		// split
		spliter.split();

	}
	
	@Test
	public void testBEPL4ChorOutput4() throws WSDLException, IOException, JAXBException,
			PartitionSpecificationException, DataFlowAnalysisException, SplitControlLinkException,
			SplitDataDependencyException, XMLStreamException {

		//
		// use the process "OrderingProcess" in Project OrderInfoDDTestCase3
		//

		// load bpel resource
		String bpelURI = testFileDir.getAbsolutePath()
				+ "\\OrderInfo4DDTestCase3\\bpelContent\\OrderingProcess.bpel";
		process = loadBPEL(bpelURI);

		// load wsdl
		definition = BPEL4ChorReader.readWSDL(testFileDir.getAbsolutePath()
				+ "\\OrderInfo4DDTestCase3\\bpelContent\\OrderingProcess.wsdl");

		// partition specification
		String partitionURI = testFileDir.getAbsolutePath()
				+ "\\OrderInfo4DDTestCase3\\bpelContent\\Partition.xml";
		partitionSpec = loadPartitionSpec(partitionURI, process);

		// analyse process
		analysis = DataFlowAnalyzer.analyze(process);

		// runtime data
		data = new RuntimeData(process, partitionSpec, definition);

		// setup process splitter
		ProcessSplitter spliter = new ProcessSplitter(process, partitionSpec);

		// split
		spliter.split();

	}
	
	@Test
	public void testBEPL4ChorOutput5() throws WSDLException, IOException, JAXBException,
			PartitionSpecificationException, DataFlowAnalysisException, SplitControlLinkException,
			SplitDataDependencyException, XMLStreamException {

		//
		// use the process "OrderingProcess" in Project OrderInfoDDTestCase3
		//

		// load bpel resource
		String bpelURI = testFileDir.getAbsolutePath()
				+ "\\OrderInfo4DDTestCase3\\bpelContent\\OrderingProcess.bpel";
		process = loadBPEL(bpelURI);

		// load wsdl
		definition = BPEL4ChorReader.readWSDL(testFileDir.getAbsolutePath()
				+ "\\OrderInfo4DDTestCase3\\bpelContent\\OrderingProcess.wsdl");

		// partition specification
		String partitionURI = testFileDir.getAbsolutePath()
				+ "\\OrderInfo4DDTestCase3\\bpelContent\\Partition-SamePartitionMultipleQuerySet.xml";
		partitionSpec = loadPartitionSpec(partitionURI, process);

		// analyse process
		analysis = DataFlowAnalyzer.analyze(process);

		// runtime data
		data = new RuntimeData(process, partitionSpec, definition);

		// setup process splitter
		ProcessSplitter spliter = new ProcessSplitter(process, partitionSpec);

		// split
		spliter.split();

	}
}
