package org.bpel4chor.splitprocess.test.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import junit.framework.Assert;

import org.bpel4chor.splitprocess.utils.VariableUtil;
import org.bpel4chor.utils.BPEL4ChorReader;
import org.eclipse.bpel.model.BPELPlugin;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Variable;
import org.eclipse.bpel.model.resource.BPELResource;
import org.eclipse.bpel.model.resource.BPELResourceFactoryImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.wst.wsdl.internal.util.WSDLResourceFactoryImpl;
import org.eclipse.xsd.util.XSDResourceFactoryImpl;
import org.junit.BeforeClass;
import org.junit.Test;

public class VariableUtilTest {

	private static Process process = null;// BPEL process

	private static File testFileDir;// where the test files locate
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		File projectDir = new File("");
		testFileDir = new File(projectDir.getAbsolutePath(), "files");

		try {
			// init eclipse plugin
			BPELPlugin bpelPlugin = new BPELPlugin();
			Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("bpel", new BPELResourceFactoryImpl());
			Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("wsdl", new WSDLResourceFactoryImpl());
			Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xsd", new XSDResourceFactoryImpl());

			// load bpel resource,
			// [project_path]\files\OrderInfoSimple3\OrderingProcessSimple3.bpel
			ResourceSet resourceSet = new ResourceSetImpl();
			URI uri = URI.createFileURI(testFileDir + File.separator + "OrderInfoSimple3" + File.separator
					+ "bpelContent" + File.separator + "OrderingProcessSimple3.bpel");
			BPELResource resource = (BPELResource) resourceSet.createResource(uri);

			// prepare the inputStream,
			// [project_path]\files\OrderInfoSimple3\OrderingProcessSimple3.bpel
			FileInputStream inputStream = new FileInputStream(new File(testFileDir + File.separator
					+ "OrderInfoSimple3" + File.separator + "bpelContent", "OrderingProcessSimple3.bpel"));

			// read in the BPEL process
			process = BPEL4ChorReader.readBPEL(resource, inputStream);

			System.out.println("BPEL Process " + process.getName() + " in " + uri.toFileString() + " is parsed.");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testIsExistedVariable() {
		List<Variable> varList = process.getVariables().getChildren();
		Variable testVar1 = null;
		Variable testVar2 = varList.get(0);

		try{
			VariableUtil.isExistedVariable(testVar1, varList);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals(true, e instanceof NullPointerException);
		}
		
		Assert.assertEquals(true, VariableUtil.isExistedVariable(testVar2, varList));

	}
}
