package org.bpel4chor.splitprocess.test.utils;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Set;

import org.bpel4chor.splitprocess.utils.NameGenerator;
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

import de.uni_stuttgart.iaas.bpel.model.utilities.MyWSDLUtil;

public class NameGeneratorTest {

	static File testFileDir = null;
	static NameGenerator ng = null;
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
		testFileDir = null;
		ng = null;
		process = null;
		defn = null;
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testNameGeneratorProcess() {
		
		try {
			NameGenerator myNG = new NameGenerator(process);
		} catch (Exception e) {
			
			e.printStackTrace();
			fail();
		}
		
	}

	@Test
	public void testNameGeneratorDefinition() {
		try {
			NameGenerator myNG = new NameGenerator(defn);
		} catch (Exception e) {
			
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testInitExistedVariableNames() {
		MyNameGenerator myNG = new MyNameGenerator(process);
		Set<String> variableNames = myNG.getExistedVariablesNames();
		assertNotNull(variableNames);
		assertEquals(true, variableNames.size()==5);
		assertEquals(true, variableNames.contains("processOrderPLRequest"));
	}

	@Test
	public void testInitExistedActivityNames() {
		MyNameGenerator myNG = new MyNameGenerator(process);
		Set<String> actNames = myNG.getExistedActivityNames();
		assertNotNull(actNames);
		assertEquals(true, actNames.size()==9);
		assertEquals(true, actNames.contains("D"));
	}
	
	@Test
	public void testInitExistedPortTypeNames() {
		MyNameGenerator myNG = new MyNameGenerator(defn);
		Set<String> ptNames = myNG.getExistedPortTypeNames();
		assertNotNull(ptNames);
		assertEquals(true, ptNames.size()==1);
		assertEquals(true, ptNames.contains("OrderingProcess"));
	}

	@Test
	public void testInitExistedOperationNames() {
		MyNameGenerator ng = new MyNameGenerator(defn);
		Set<String> opNames = ng.getExistedOperationNames();
		assertNotNull(opNames);
		assertEquals(true, opNames.size()==1);
		assertEquals(true, opNames.contains("order"));
	}

	@Test
	public void testGetUniqueVariableName() {
		NameGenerator ng = new NameGenerator(process);
		String sugguest = "orderInfo";
		String uniqueName = ng.getUniqueVariableName(sugguest);
		assertNotNull(uniqueName);
		assertEquals(false, uniqueName.isEmpty());
		assertEquals(false, uniqueName.equals(sugguest));
	}
	
	@Test
	public void testGetUniqueActivityName() {
		NameGenerator ng = new NameGenerator(process);
		String sugguest = "MyNameIsUnique";
		String uniqueName = ng.getUniqueActivityName(sugguest);
		assertNotNull(uniqueName);
		assertEquals(true, uniqueName.equals(sugguest));
		sugguest = "H";
		uniqueName = ng.getUniqueActivityName(sugguest);
		assertEquals(false, uniqueName.equals(sugguest));
	}

	@Test
	public void testGetUniquePortTypeName() {
		NameGenerator ng = new NameGenerator(defn);
		String sugguest = "OrderingProcess";
		String uniqueName = ng.getUniquePortTypeName(sugguest);
		assertNotNull(uniqueName);
		assertEquals(false, uniqueName.isEmpty());
		assertEquals(false, uniqueName.equals(sugguest));
	}

	@Test
	public void testGetUniqueOperationName() {
		NameGenerator ng = new NameGenerator(defn);
		String sugguest = "order";
		String uniqueName = ng.getUniqueOperationName(sugguest);
		assertNotNull(uniqueName);
		assertEquals(false, uniqueName.isEmpty());
		assertEquals(false, uniqueName.equals(sugguest));
	}

	
	@Test
	public void testGetUniqueRoleName() {
		NameGenerator ng = new NameGenerator(defn);
		String sugguest = "OrderProcessCallback";
		String uniqueName = ng.getUniqueRoleName(sugguest);
		assertNotNull(uniqueName);
		assertEquals(false, uniqueName.isEmpty());
		assertEquals(false, uniqueName.equals(sugguest));
	}
	
}

class MyNameGenerator extends NameGenerator {

	public MyNameGenerator(Process process) {
		super(process);
	}
	
	public MyNameGenerator(Definition defn) {
		super(defn);
	}

	public Set<String> getExistedVariablesNames() {
		return super.existedVariableNames;
	}
	
	public Set<String> getExistedOperationNames() {
		return super.existedOperationNames;
	}
	
	public Set<String> getExistedPortTypeNames() {
		return super.existedPortTypeNames;
	}
	
	public Set<String> getExistedActivityNames() {
		return super.existedActivityNames;
	}
	
	
}
