package org.bpel4chor.splitprocess.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.wsdl.WSDLException;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

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
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.uni_stuttgart.iaas.bpel.model.utilities.MyWSDLUtil;
import de.uni_stuttgart.iaas.bpel_d.algorithm.analysis.AnalysisResult;

/**
 * Test for ProcessSplitter - Case only one partition
 * 
 * @since Jul 07, 2012
 * @author Daojun Cui
 * 
 */
public class ProcessSplitterTestCase1 {

	static File testFileDir = null;

	static Process process = null;

	static Definition definition = null;

	static RuntimeData data = null;

	static PartitionSpecification partitionSpec = null;

	static AnalysisResult analysis = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		//
		// use the process "OrderingProcess" in Project OrderInfo4DDTestCase3
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
				+ "\\DeliverProcess\\bpelContent\\DeliverProcess.bpel";
		process = loadBPEL(bpelURI);

		// load wsdl
		definition = MyWSDLUtil.readWSDL(testFileDir.getAbsolutePath()
				+ "\\DeliverProcess\\bpelContent\\DeliverProcess.wsdl");

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
	public void test() {
		try {
			// partition specification
			String partitionURI = testFileDir.getAbsolutePath()
					+ "\\DeliverProcess\\bpelContent\\Partition.xml";
			partitionSpec = loadPartitionSpec(partitionURI, process);

			// analyze process
			analysis = DataFlowAnalyzer.analyze(process);
			
			ProcessSplitter splitter = new ProcessSplitter(process, partitionSpec);
			splitter.split();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (JAXBException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (PartitionSpecificationException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (DataFlowAnalysisException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (WSDLException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (SplitControlLinkException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (SplitDataDependencyException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (XMLStreamException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

}
