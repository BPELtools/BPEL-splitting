package org.bpel4chor.splitprocess.test.dataflowanalysis;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bpel4chor.splitprocess.dataflowanalysis.AnalysisResultParser;
import org.bpel4chor.splitprocess.dataflowanalysis.DataFlowAnalyzer;
import org.bpel4chor.splitprocess.dataflowanalysis.QueryWriterSet;
import org.bpel4chor.utils.MyBPELUtils;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.BPELPlugin;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Variable;
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

@SuppressWarnings("restriction")
public class AnalysisResultParserTest {

	static File testFileDir = null;

	static Process process = null;

	static AnalysisResult analysis = null;

	@SuppressWarnings({ "unused" })
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

		// analyse
		analysis = DataFlowAnalyzer.analyze(process);

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
	public void testPrint() throws Exception {

		// analysis.output();
		AnalysisResultParser.print(analysis);
	}

	@Test
	public void testParse() {

		// test variable = paymentInfo
		Variable var = MyBPELUtils.resolveVariable("paymentInfo", process);

		// test activity = E
		Activity act = MyBPELUtils.resolveActivity("E", process);

		// parse the result against the given activity and variable
		QueryWriterSet queryWrtSet = AnalysisResultParser.parse(act, var, analysis);
		assertNotNull(queryWrtSet);

		// Test query2writers set for assignE and paymentInfo
		
		// there are 2 querySet2WriterSet entries
		// ({.actNum},{B}),
		// ({.amt},{D,C,B})
		assertEquals(2, queryWrtSet.size());
		Set<String> qs1 = new HashSet<String>();
		Set<String> qs2 = new HashSet<String>();
		qs1.add(".actNum");
		qs2.add(".amt");
		// writer set for {.actNum} should be {B}
		Set<Activity> actSet = queryWrtSet.get(qs1);
		assertNotNull(actSet);
		assertEquals(1, actSet.size());
		assertTrue(actSet.iterator().next().getName().equals("B"));
		// writer set for {.amt} should be {D, C, B}
		actSet = queryWrtSet.get(qs2);
		assertNotNull(actSet);
		assertEquals(3, actSet.size());
		for (Activity actual : actSet) {
			assertTrue(actual.getName().equals("B") || actual.getName().equals("C") || actual.getName().equals("D"));
		}
		
	}
	
	@Test
	public void testQueryWholeVar() {
		// test variable = paymentInfo
		Variable var = MyBPELUtils.resolveVariable("delivered", process);

		// test activity = E
		Activity act = MyBPELUtils.resolveActivity("F", process);
		
		// parse the result against the given activity and variable
		QueryWriterSet queryWrtSet = AnalysisResultParser.parse(act, var, analysis);
		assertNotNull(queryWrtSet);
		
		// now test the query set for the whole variable {}
		Set<String> queryForTheWholeVariable = new HashSet<String>();
		queryForTheWholeVariable.add("");
		Set<Activity> writerSet = queryWrtSet.get(queryForTheWholeVariable);
		
		// the writer set for activity "F" and variable "delivered" = {E}
		assertNotNull(writerSet);
		assertEquals(1, writerSet.size());
		Activity actual = writerSet.iterator().next();
		assertTrue(actual.getName().equals("E"));
	}
}
