package org.bpel4chor.splitprocess.test.utils;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.bpel4chor.splitprocess.exceptions.ActivityNotFoundException;
import org.bpel4chor.splitprocess.utils.ActivityFinder;
import org.bpel4chor.splitprocess.utils.VariableResolver;
import org.bpel4chor.splitprocess.utils.VariableUtil;
import org.bpel4chor.utils.BPEL4ChorReader;
import org.bpel4chor.utils.MyBPELUtils;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Assign;
import org.eclipse.bpel.model.BPELPlugin;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Variable;
import org.eclipse.bpel.model.resource.BPELResource;
import org.eclipse.bpel.model.resource.BPELResourceFactoryImpl;
import org.eclipse.bpel.model.util.BPELUtils;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.wst.wsdl.internal.util.WSDLResourceFactoryImpl;
import org.eclipse.xsd.util.XSDResourceFactoryImpl;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test for VariableResolver
 *
 * @since Feb 13, 2012
 * @author Daojun Cui
 */
public class VariableResolverTest {

	private static VariableResolver variableResolver = null; // test target

	private static ActivityFinder finder = null;// activity finder

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

			finder = new ActivityFinder(process);

			// the test target
			variableResolver = new VariableResolver(process);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testResolveReadVariableActivity() {
		Activity assignB = MyBPELUtils.resolveActivity("AssignB", process);
		Activity invokeF = MyBPELUtils.resolveActivity("InvokeF", process);
		
		// resolve AssignB
		List<Variable> vars1 = variableResolver.resolveReadVariable(assignB);
		
		// assert read-variables
		assertReadVariablesAssignB(vars1);
		
		// resolve Invoke
		List<Variable> vars2 = variableResolver.resolveReadVariable(invokeF);
		
		// assert read-variables
		assertReadVariableInvokeF(vars2);
	}

	private void assertReadVariablesAssignB(List<Variable> vars1) {
		Variable oderInfo = MyBPELUtils.resolveVariable("orderInfo", process);
		assertEquals(true, vars1.contains(oderInfo));
	}

	private void assertReadVariableInvokeF(List<Variable> vars2) {
		Variable orderRequest = MyBPELUtils.resolveVariable("processOrderPLRequest", process);
		assertEquals(true, vars2.contains(orderRequest));
	}

	@Test
	public void testGetVariablesInLiteral() {
		String literal = "$orderInfo.status=\"gold\"";
		List<Variable> variables = variableResolver.getVariableInLiteral(literal);
		Assert.assertEquals("orderInfo", variables.get(0).getName());
	}

	@Test
	public void testRangeCondition() {
		assertNotAcceptNull();
	}
	
	protected void assertNotAcceptNull() {
		// test object null
		try{
			variableResolver.resolveVariable(null);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals(true, e instanceof NullPointerException);
		}
	}
	
	@Test
	public void testGetVariablesInActivity() throws ActivityNotFoundException {

		// test object "AssignB"
		Assign assignB = (Assign) finder.find("AssignB");

		Set<String> varNames = new HashSet<String>();
		varNames.add("orderInfo");
		varNames.add("response");
		varNames.add("paymentInfo");

		List<Variable> usedVariables = variableResolver.resolveVariable(assignB);
		Assert.assertEquals(varNames.size(), usedVariables.size());

		for (Variable var : usedVariables) {
			Assert.assertEquals(true, varNames.contains(var.getName()));
		}
		// test object "InvokeF"
		Invoke invokeF = (Invoke) finder.find("InvokeF");
		List<Variable> usedVarInvoke = variableResolver.resolveVariable(invokeF);
		Assert.assertEquals(1, usedVarInvoke.size());
		Assert.assertEquals("processOrderPLRequest", usedVarInvoke.get(0).getName());

	}
}
