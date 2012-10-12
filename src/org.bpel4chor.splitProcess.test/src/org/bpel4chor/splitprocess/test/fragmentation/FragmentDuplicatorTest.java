package org.bpel4chor.splitprocess.test.fragmentation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import junit.framework.Assert;

import org.bpel4chor.splitprocess.exceptions.ActivityNotFoundException;
import org.bpel4chor.splitprocess.exceptions.PartitionSpecificationException;
import org.bpel4chor.splitprocess.partition.model.PartitionSpecification;
import org.bpel4chor.splitprocess.partition.representation.PartitionSpecReader;
import org.bpel4chor.splitprocess.utils.ActivityFinder;
import org.bpel4chor.utils.BPEL4ChorReader;
import org.bpel4chor.utils.FragmentDuplicator;
import org.eclipse.bpel.model.Assign;
import org.eclipse.bpel.model.BPELPlugin;
import org.eclipse.bpel.model.Condition;
import org.eclipse.bpel.model.Copy;
import org.eclipse.bpel.model.Correlation;
import org.eclipse.bpel.model.Correlations;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.From;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.PartnerLink;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Receive;
import org.eclipse.bpel.model.Source;
import org.eclipse.bpel.model.To;
import org.eclipse.bpel.model.Variable;
import org.eclipse.bpel.model.impl.BPELFactoryImpl;
import org.eclipse.bpel.model.partnerlinktype.Role;
import org.eclipse.bpel.model.resource.BPELResourceFactoryImpl;
import org.eclipse.bpel.model.util.BPELUtils;
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

/**
 * Test for FragmentDuplicator
 * 
 * @since Feb 1, 2012
 * @author Daojun Cui
 */
public class FragmentDuplicatorTest {

	private static ActivityFinder finder = null;// activity finder

	private static Process process = null;// BPEL process

	private static Definition defn = null;

	private static PartitionSpecification partitionSpec = null;// partition
																// specification

	private static File testFileDir;// where the test files locate

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		File projectDir = new File("");
		testFileDir = new File(projectDir.getAbsolutePath(), "files");

		try {
			// init eclipse plugin
			BPELPlugin bpelPlugin = new BPELPlugin();
			Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("bpel",
					new BPELResourceFactoryImpl());
			Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("wsdl",
					new WSDLResourceFactoryImpl());
			Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xsd",
					new XSDResourceFactoryImpl());

			// load bpel
			String bpelUri = testFileDir + File.separator + "OrderInfoSimple3" + File.separator
					+ "bpelContent" + File.separator + "OrderingProcessSimple3.bpel";
			process = loadBPEL(bpelUri);

			// load wsdl
			String wsdlUri = testFileDir + File.separator + "OrderInfoSimple3" + File.separator
					+ "bpelContent" + File.separator + "OrderingProcessSimple3.wsdl";
			defn = BPEL4ChorReader.readWSDL(wsdlUri);

			// define Activity Finder
			finder = new ActivityFinder(process);

			// load partition specification
			String partitionUri = testFileDir + File.separator + "OrderInfoSimple3"
					+ File.separator + "bpelContent" + File.separator + "Partition.xml";
			partitionSpec = loadPartitionSpec(partitionUri, process);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
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
	public void testCopyStandardAttributes() throws ActivityNotFoundException {
		// test object the root flow
		Flow flow = (Flow) finder.find("Flow");
		Flow newFlow = BPELFactoryImpl.eINSTANCE.createFlow();
		FragmentDuplicator.copyStandardAttributes(flow, newFlow);
		Assert.assertEquals(true, flow.getName().equals(newFlow.getName()));
		Assert.assertEquals(true, flow.getSuppressJoinFailure() == newFlow.getSuppressJoinFailure());
	}

	@Test
	public void testCopyStandardElements() throws ActivityNotFoundException {

		// test object "AssignB"
		Assign assignB = (Assign) finder.find("AssignB");

		try {
			FragmentDuplicator.copyStandardElements(assignB, null);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals(true, e instanceof NullPointerException);
		}

		Assign newAssign = BPELFactoryImpl.eINSTANCE.createAssign();
		FragmentDuplicator.copyStandardElements(assignB, newAssign);
		Assert.assertEquals(true, newAssign.getTargets().getChildren().size() == assignB
				.getTargets().getChildren().size());
		Assert.assertEquals(true, newAssign.getSources().getChildren().size() == assignB
				.getSources().getChildren().size());

		int sourcesSize = assignB.getSources().getChildren().size();

		// get a random source and test it
		for (Source expectedSource : assignB.getSources().getChildren()) {

			// Source expectedSource =
			// assignB.getSources().getChildren().get((int) (Math.random() *
			// sourcesSize));
			Condition expectedCond = expectedSource.getTransitionCondition();
			Link expectedLink = expectedSource.getLink();

			// find it in the actual sources
			boolean copiedCorrect = false;
			for (Source actualSource : newAssign.getSources().getChildren()) {

				Condition actualCond = actualSource.getTransitionCondition();
				Link actualLink = actualSource.getLink();

				if (expectedCond == null && actualCond == null) {
					if (expectedLink.getName().equals(actualLink.getName())) {
						copiedCorrect = true;
						break;
					}

				} else if (expectedCond != null && actualCond != null) {
					if (expectedCond.getBody().equals(actualCond.getBody())
							&& expectedLink.getName().equals(actualLink.getName())) {
						copiedCorrect = true;
						break;
					}
				}
			}

			Assert.assertEquals(true, copiedCorrect);
		}

	}

	@Test
	public void testCopyActivity() throws ActivityNotFoundException {

		// copy "AssignB"
		Assign assignB = (Assign) finder.find("AssignB");
		Assign newAssign = FragmentDuplicator.copyAssign(assignB);
		List<Copy> expectedCopies = assignB.getCopy();
		List<Copy> actualCopies = newAssign.getCopy();
		int expectedSize = expectedCopies.size();
		int actualSize = actualCopies.size();
		Assert.assertEquals(expectedSize, actualSize);
		Copy expectedCopy = expectedCopies.get((int) (expectedSize * Math.random()));
		From expectedFrom = expectedCopy.getFrom();
		To expectedTo = expectedCopy.getTo();
		boolean goodCopy = false;
		for (Copy actualCopy : actualCopies) {
			From actualFrom = actualCopy.getFrom();
			To actualTo = actualCopy.getTo();

			if (testFrom(expectedFrom, actualFrom) && testTo(expectedTo, actualTo)) {
				goodCopy = true;
				break;
			}
		}
		Assert.assertEquals(true, goodCopy);

		// copy "Receive"
		Receive receiveA = (Receive) finder.find("ReceiveA");
		Receive newReceiveA = FragmentDuplicator.copyReceive(receiveA, process, defn);
		assertCorrelationIsCorrectlyCopied(newReceiveA, process, defn);
	}

	private void assertCorrelationIsCorrectlyCopied(Receive newReceiveA, Process process2,
			Definition defn2) {

		Correlations newCorrelations = newReceiveA.getCorrelations();
		Assert.assertNotNull(newCorrelations);
		Assert.assertTrue(newCorrelations.getChildren().size() == 1);
		Correlation newCorrel = newCorrelations.getChildren().get(0);
		Assert.assertNotNull(newCorrel.getSet());
		Assert.assertTrue(newCorrel.getInitiate().equals("yes"));
	}

	/**
	 * Test whether the original from and the new from is the same
	 * 
	 * @param origFrom
	 * @param newFrom
	 * @return
	 */
	public boolean testFrom(From origFrom, From newFrom) {
		if (origFrom.getVariable() != null && newFrom.getVariable() != null) {
			if (origFrom.getVariable().getName().equals(newFrom.getVariable().getName()))
				return true;
			else
				return false;
		} else if (origFrom.getVariable() == null && newFrom.getVariable() == null) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Test whether the original to and the new to is the same
	 * 
	 * @param origTo
	 * @param newTo
	 * @return
	 */
	public boolean testTo(To origTo, To newTo) {

		if (origTo.getVariable() != null && newTo.getVariable() != null) {
			if (origTo.getVariable().getName().equals(newTo.getVariable().getName()))
				return true;
			else
				return false;
		} else if (origTo.getVariable() == null && newTo.getVariable() == null) {
			return true;
		} else {
			return false;
		}
	}

	@Test
	public void testCopyVariable() {
		List<Variable> variables = process.getVariables().getChildren();
		Assert.assertEquals(5, variables.size());

		for (Variable expected : variables) {
			Variable actual = FragmentDuplicator.copyVariable(expected);
			Assert.assertEquals(false, expected.equals(actual));
			Assert.assertEquals(true, actual.getName().equals(expected.getName()));
			if (expected.getType() != null) {
				Assert.assertNotNull(actual.getType());
				Assert.assertEquals(true, actual.getType().getName().equals(expected.getName()));
			} else {
				Assert.assertNull(actual.getType());
			}

			if (expected.getMessageType() != null) {
				Assert.assertNotNull(actual.getMessageType());
				Assert.assertEquals(true, actual.getMessageType().eIsProxy() == expected
						.getMessageType().eIsProxy());
				QName expectedQName = expected.getMessageType().getQName();
				QName actualQName = actual.getMessageType().getQName();
				Assert.assertEquals(expectedQName, actualQName);
			} else {
				Assert.assertNull(actual.getMessageType());
			}
		}
	}

	@Test
	public void testCopyPartnerLink() {

		String[] plNames = { "orderingPL", "processPaymentPL" };
		for (String plName : Arrays.asList(plNames)) {
			PartnerLink expected = BPELUtils.getPartnerLink(process, plName);
			PartnerLink actual = FragmentDuplicator.copyPartnerLink(expected);

			Assert.assertEquals(expected.getName(), actual.getName());

			Role expectedMyRole = expected.getMyRole();
			Role actualMyRole = actual.getMyRole();
			if (expectedMyRole == null)
				Assert.assertEquals(true, actual.getMyRole() == null);
			else {
				Assert.assertNotNull(actualMyRole);
				String actualName = actualMyRole.getName();
				String expectedName = expectedMyRole.getName();
				Assert.assertEquals(true, actualName.equals(expectedName));
			}

			Role expPRole = expected.getPartnerRole();
			Role actualPRole = actual.getPartnerRole();
			if (expPRole == null)
				Assert.assertEquals(true, actual.getPartnerRole() == null);
			else {
				Assert.assertNotNull(actualPRole);
				String actualName = actualPRole.getName();
				String expName = expPRole.getName();
				Assert.assertEquals(true, actualName.equals(expName));
			}
		}
	}

	// @Test
	// public void testCopyInvoke() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testCopyAssign() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testCopyReply() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testCopyReceive() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testCopyWhile() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testCopyScope() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testCopyExtensionActivity() {
	// fail("Not yet implemented");
	// }
	//

}
