package org.bpel4chor.splitprocess.test.fragmentation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.wsdl.WSDLException;
import javax.xml.bind.JAXBException;

import de.uni_stuttgart.iaas.bpel.model.utilities.MyBPELUtils;
import de.uni_stuttgart.iaas.bpel.model.utilities.MyWSDLUtil;

import org.apache.log4j.Logger;
import org.bpel4chor.splitprocess.RuntimeData;
import org.bpel4chor.splitprocess.dataflowanalysis.AnalysisResultParser;
import org.bpel4chor.splitprocess.dataflowanalysis.DataFlowAnalyzer;
import org.bpel4chor.splitprocess.dataflowanalysis.QueryWriterSet;
import org.bpel4chor.splitprocess.exceptions.DataFlowAnalysisException;
import org.bpel4chor.splitprocess.exceptions.PWDGException;
import org.bpel4chor.splitprocess.exceptions.PartitionSpecificationException;
import org.bpel4chor.splitprocess.exceptions.WDGException;
import org.bpel4chor.splitprocess.partition.model.PartitionSpecification;
import org.bpel4chor.splitprocess.partition.representation.PartitionSpecReader;
import org.bpel4chor.splitprocess.pwdg.model.PWDG;
import org.bpel4chor.splitprocess.pwdg.model.WDG;
import org.bpel4chor.splitprocess.pwdg.util.PWDGFactory;
import org.bpel4chor.splitprocess.pwdg.util.WDGFactory;
import org.bpel4chor.utils.BPEL4ChorReader;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.BPELPlugin;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Variable;
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

public class IllustrationDataFlowAnalysis2PWDG {

	static File testFileDir = null;

	static Process process = null;

	static Definition definition = null;

	static RuntimeData data = null;

	static PartitionSpecification partitionSpec = null;

	static AnalysisResult analysisRes = null;
	
	static Logger logger = Logger.getLogger(IllustrationDataFlowAnalysis2PWDG.class);

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
	public void testFromDataFlowAnalysis2PWDG() throws DataFlowAnalysisException, WSDLException,
			IOException, JAXBException, PartitionSpecificationException, WDGException, PWDGException {

		// load bpel resource
		String bpelURI = testFileDir.getAbsolutePath()
				+ "\\OrderInfo4DDTestCase3\\bpelContent\\OrderingProcess.bpel";
		process = loadBPEL(bpelURI);

		// load wsdl
		String wsdlURI = testFileDir.getAbsolutePath()
				+ "\\OrderInfo4DDTestCase3\\bpelContent\\OrderingProcess.wsdl";
		definition = MyWSDLUtil.readWSDL(wsdlURI);

		// load partition specification
		String partitionURI = testFileDir.getAbsolutePath()
				+ "\\OrderInfo4DDTestCase3\\bpelContent\\Partition.xml";
		partitionSpec = loadPartitionSpec(partitionURI, process);
		logger.info(partitionSpec.toString());

		// 1. analyze the process given
		analysisRes = DataFlowAnalyzer.analyze(process);

		// print out the analysis result
		AnalysisResultParser.printActPreorder(analysisRes);

		// 2. parse the analysis result upon the reader ‘H’ and variable
		// ‘response’ then return QueryWriterSet Q_s(H, response)
		Activity act = MyBPELUtils.resolveActivity("H", process);
		Variable var = MyBPELUtils.resolveVariable("response", process);
		QueryWriterSet qws = AnalysisResultParser.parse(act, var, analysisRes);
		logger.info("QueryWriterSet={" + qws.toString() + "}");

		// 3. create WDG upon (H, response) using all writers against 'response'
		WDG wdg = WDGFactory.createWDG(qws.getAllWriters());
		logger.info("WDG=(V, E) as " + wdg.toString());
		
		// 4. create PWDG upon (H, response) using WDG, Process, PartitionSepecification
		PWDG pwdg = PWDGFactory.createPWDG(wdg, process, partitionSpec);
		logger.info("PWDG=(V, E) as " + pwdg.toString());
		
	}
}
