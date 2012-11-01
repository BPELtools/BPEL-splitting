package org.bpel4chor.splitprocess.test.partition.representation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import de.uni_stuttgart.iaas.bpel.model.utilities.MyBPELUtils;

import org.bpel4chor.splitprocess.exceptions.PartitionSpecificationException;
import org.bpel4chor.splitprocess.partition.model.Participant;
import org.bpel4chor.splitprocess.partition.model.PartitionSpecification;
import org.bpel4chor.splitprocess.partition.representation.PartitionSpecReader;
import org.bpel4chor.splitprocess.partition.representation.PartitionSpecWriter;
import org.bpel4chor.utils.BPEL4ChorConstants;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.BPELPlugin;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.resource.BPELResourceFactoryImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.wst.wsdl.internal.util.WSDLResourceFactoryImpl;
import org.eclipse.xsd.util.XSDResourceFactoryImpl;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class PartitionSpecWriterTest {

	PartitionSpecification partitionSpec;

	Process process = null;

	static File testFileDir = null;

	static File testResultsDir;// where to write the test result files

	
	@BeforeClass
	public static void setupBeforeClass() {
		// init bpel eclipse plugin
		BPELPlugin bpelPlugin = new BPELPlugin();
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("bpel", new BPELResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("wsdl", new WSDLResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xsd", new XSDResourceFactoryImpl());
	}
	
	@Before
	public void setup() {
		// use the same dir as the BPEL4ChorWriter
		testResultsDir = new File(BPEL4ChorConstants.BPEL4CHOR_DEFAULT_WRITE_DIR);
		if (!testResultsDir.exists()) {
			testResultsDir.mkdirs();
		}

		File projectDir = new File("");
		testFileDir = new File(projectDir.getAbsolutePath(), "files");

		// load bpel process
		String strURI = testFileDir.getAbsolutePath() + "\\OrderInfoSimple1\\bpelContent\\OrderingProcessSimple1.bpel";
		process = loadBPEL(strURI);

		
	}

	protected static Process loadBPEL(String strURI) {
		ResourceSet resourceSet = new ResourceSetImpl();
		URI uri = URI.createFileURI(strURI);
		Resource resource = resourceSet.getResource(uri, true);
		return (Process) resource.getContents().get(0);
	}

	@Test
	public void testSaveSpecificationToXML() throws JAXBException, IOException, ParserConfigurationException, PartitionSpecificationException {

		// prepare partitionSpec
		
		partitionSpec = new PartitionSpecification();
		Set<Activity> activities1 = new HashSet<Activity>();
		Set<Activity> activities2 = new HashSet<Activity>();
		Activity act1 = MyBPELUtils.resolveActivity("ReceiveA", process);
		Activity act2 = MyBPELUtils.resolveActivity("AssignB", process);
		activities1.add(act1);
		activities2.add(act2);
		Participant participant1 = new Participant("participant1", activities1);
		Participant participant2 = new Participant("participant2", activities2);
		partitionSpec.add(participant1);
		partitionSpec.add(participant2);
		
		// now write
		PartitionSpecWriter writer = new PartitionSpecWriter();
		FileOutputStream outputStream = null;

		File testFile = new File(testResultsDir, "Partition." + process.getName()+"-"
				+ new Date().getTime() + ".xml");
		outputStream = new FileOutputStream(testFile);
		writer.writeSpecification(partitionSpec, outputStream);
		System.out.println("Write Partition Specification. Result ==> " + testFile.getAbsolutePath());
		
		// test, it should be able to be read again
		PartitionSpecReader reader = new PartitionSpecReader();
		FileInputStream inputStream = new FileInputStream(testFile);
		PartitionSpecification actual = reader.readSpecification(inputStream, process);
		
		// now test the actual partition Spec
		Participant actualParticipant1 = actual.getParticipant("participant1");
		Participant expectedParticipant1 = partitionSpec.getParticipant("participant1");
		assertNotNull(actualParticipant1);
		// the both activities should be equal
		Activity actualAct1 = actualParticipant1.getActivities().iterator().next();
		Activity expectAct1 = expectedParticipant1.getActivities().iterator().next();
		assertEquals(expectAct1, actualAct1);
	}


}
