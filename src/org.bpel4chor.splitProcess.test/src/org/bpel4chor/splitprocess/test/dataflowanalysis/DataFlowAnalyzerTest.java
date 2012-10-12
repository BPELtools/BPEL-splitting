package org.bpel4chor.splitprocess.test.dataflowanalysis;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.bpel4chor.splitprocess.dataflowanalysis.AnalysisResultParser;
import org.bpel4chor.splitprocess.dataflowanalysis.DataFlowAnalyzer;
import org.bpel4chor.splitprocess.exceptions.DataFlowAnalysisException;
import org.eclipse.bpel.model.BPELPlugin;
import org.eclipse.bpel.model.Process;
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

import de.uni_stuttgart.iaas.bpel_d.algorithm.analysis.AnalysisResult;

public class DataFlowAnalyzerTest {

	static File testFileDir = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		File projectDir = new File("");
		testFileDir = new File(projectDir.getAbsolutePath(), "files");

		// init
		BPELPlugin bpelPlugin = new BPELPlugin();
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("bpel", new BPELResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("wsdl", new WSDLResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xsd", new XSDResourceFactoryImpl());
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
	public void testAnalyze() {
		// analyse all the test samples
		String sample1URI = testFileDir.getAbsolutePath()
				+ "\\OrderInfoSimple1\\bpelContent\\OrderingProcessSimple1.bpel";
		String sample2URI = testFileDir.getAbsolutePath()
				+ "\\OrderInfoSimple2\\bpelContent\\OrderingProcessSimple2.bpel";
		String sample3URI = testFileDir.getAbsolutePath()
				+ "\\OrderInfoSimple3\\bpelContent\\OrderingProcessSimple3.bpel";
		String sample4URI = testFileDir.getAbsolutePath()
				+ "\\OrderInfoSimple4\\bpelContent\\OrderingProcessSimple4.bpel";
		String sample5URI = testFileDir.getAbsolutePath() + "\\OrderInfoWithLoop\\bpelContent\\OrderingProcess.bpel";
		String sample6URI = testFileDir.getAbsolutePath() + "\\OrderInfo\\bpelContent\\OrderingProcess.bpel";
		String sample7URI = testFileDir.getAbsolutePath() + "\\OrderInfo4DDTestCase3\\bpelContent\\OrderingProcess.bpel";
		List<String> URIs = Arrays.asList(new String[] { sample1URI, sample2URI, sample3URI, sample4URI, sample5URI,
				sample6URI, sample7URI });

		try {
			for (String uri : URIs) {
				System.out.println();
				System.out.println("Analyze: " + uri);
				AnalysisResult res = DataFlowAnalyzer.analyze(loadBPEL(uri));
				AnalysisResultParser.print(res);
				
			}
		} catch (DataFlowAnalysisException e) {
			e.printStackTrace();
			fail();
		}
	}

	protected Process loadBPEL(String strURI) {
		ResourceSet resourceSet = new ResourceSetImpl();
		URI uri = URI.createFileURI(strURI);
		Resource resource = resourceSet.getResource(uri, true);
		return (Process) resource.getContents().get(0);
	}

}
