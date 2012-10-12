package org.bpel4chor.splitprocess.test.partition.representation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

import junit.framework.Assert;

import org.bpel4chor.splitprocess.exceptions.PartitionSpecificationException;
import org.bpel4chor.splitprocess.partition.model.Participant;
import org.bpel4chor.splitprocess.partition.model.PartitionSpecification;
import org.bpel4chor.splitprocess.partition.representation.PartitionSpecReader;
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

public class PartitionSpecReaderTest {

	private FileInputStream inputStream = null; 
	private PartitionSpecReader reader = null;
	private PartitionSpecification partitionSpec = null;

	static File testFileDir = null;
	static Process process = null;


	
	@BeforeClass
	public static void setupBeforeClass() {
		//
		// use the process "OrderingProcessSimple1"
		//

		File projectDir = new File("");
		testFileDir = new File(projectDir.getAbsolutePath(), "files");

		// init
		BPELPlugin bpelPlugin = new BPELPlugin();
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("bpel", new BPELResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("wsdl", new WSDLResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xsd", new XSDResourceFactoryImpl());

		// load bpel resource
		String bpelURI = testFileDir.getAbsolutePath() + "\\OrderInfoSimple1\\bpelContent\\OrderingProcessSimple1.bpel";
		process = loadBPEL(bpelURI);
	}
	
	protected static Process loadBPEL(String strURI) {
		ResourceSet resourceSet = new ResourceSetImpl();
		URI uri = URI.createFileURI(strURI);
		Resource resource = resourceSet.getResource(uri, true);
		return (Process) resource.getContents().get(0);
	}
	
	@Before
	public void setup() throws FileNotFoundException
	{
		inputStream = 
			new FileInputStream(new File(testFileDir.getAbsolutePath() + "\\OrderInfoSimple1\\bpelContent\\Partition-NewSyntax.xml"));
		reader = new PartitionSpecReader();
	}
	
	/* Test file:
	<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
	<partitionSpecification>
	    <participant name="fragment2">
	        <activity path="/xpath/to/node1"/>
	        <activity path="/xpath/to/node2"/>
	    </participant>
	    <participant name="fragment1">
	        <activity path="/xpath/to/node1"/>
	        <activity path="/xpath/to/node2"/>
	    </participant>
	</partitionSpecification>
	 */
	@Test
	public void testLoadSpecification() throws FileNotFoundException, JAXBException, PartitionSpecificationException
	{
		
		partitionSpec = reader.readSpecification(inputStream, process);
		
		int actualPartitionSize = partitionSpec.getParticipants().size();
		Assert.assertEquals(2, actualPartitionSize);
		
		for(Participant p : partitionSpec.getParticipants()){
			Assert.assertEquals(1, p.getActivities().size());
		}
	}
	
}
