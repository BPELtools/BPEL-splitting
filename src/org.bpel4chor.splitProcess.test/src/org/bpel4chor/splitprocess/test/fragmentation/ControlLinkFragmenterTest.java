package org.bpel4chor.splitprocess.test.fragmentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;

import org.bpel4chor.splitprocess.RuntimeData;
import org.bpel4chor.splitprocess.exceptions.RuntimeDataException;
import org.bpel4chor.splitprocess.exceptions.SplitControlLinkException;
import org.bpel4chor.splitprocess.fragmentation.ControlLinkFragmenter;
import org.bpel4chor.splitprocess.fragmentation.ProcessFragmenter;
import org.bpel4chor.splitprocess.partition.model.PartitionSpecification;
import org.bpel4chor.splitprocess.partition.representation.PartitionSpecReader;
import org.bpel4chor.utils.BPEL4ChorReader;
import org.bpel4chor.utils.MyBPELUtils;
import org.bpel4chor.utils.exceptions.AmbiguousPropertyForLinkException;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Assign;
import org.eclipse.bpel.model.BPELPlugin;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Receive;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Sequence;
import org.eclipse.bpel.model.Source;
import org.eclipse.bpel.model.Sources;
import org.eclipse.bpel.model.Target;
import org.eclipse.bpel.model.Targets;
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

public class ControlLinkFragmenterTest {
	static File testFileDir = null;

	static Process process = null;

	static RuntimeData data = null;

	static Definition definition = null;

	static PartitionSpecification partitionSpec = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		//
		// use the process "OrderingProcessSimple3" as process under test
		//

		File projectDir = new File("");
		testFileDir = new File(projectDir.getAbsolutePath(), "files");

		// init
		BPELPlugin bpelPlugin = new BPELPlugin();
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("bpel", new BPELResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("wsdl", new WSDLResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xsd", new XSDResourceFactoryImpl());

		// load bpel resource
		ResourceSet resourceSet = new ResourceSetImpl();
		URI uri = URI.createFileURI(testFileDir.getAbsolutePath()
				+ "\\OrderInfoSimple3\\bpelContent\\OrderingProcessSimple3.bpel");
		Resource resource = resourceSet.getResource(uri, true);
		process = (Process) resource.getContents().get(0);

		definition = BPEL4ChorReader.readWSDL(testFileDir.getAbsolutePath()
				+ "\\OrderInfoSimple3\\bpelContent\\OrderingProcessSimple3.wsdl");

		// partition specification
		FileInputStream inputStream = new FileInputStream(new File(testFileDir.getAbsolutePath()
				+ "\\OrderInfoSimple3\\bpelContent\\Partition.xml"));
		PartitionSpecReader partitionReader = new PartitionSpecReader();
		partitionSpec = partitionReader.readSpecification(inputStream, process);

		// fragment the process into fragments, "participant1", "participant2",
		// "participant3".
		data = new RuntimeData(process, partitionSpec, definition);
		ProcessFragmenter procFragmenter = new ProcessFragmenter(data);
		procFragmenter.fragmentizeProcess();

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
	public void testGetTargetFragmentProcess() throws RuntimeDataException, AmbiguousPropertyForLinkException {

		MyControlLinkFragmenter ctrlLinkFragmenter = new MyControlLinkFragmenter(data);

		// get fragment "participant"
		Process procParticipant1 = data.getFragmentProcess("participant1");

		// find the link in the source process, 'AssignB2E'
		Link linkInSourceProcess = MyBPELUtils.findLinkInActivitySource("AssignB2E", procParticipant1);

		// get target fragment process
		Process targetProcess = ctrlLinkFragmenter.getTargetFragemntProcess(linkInSourceProcess);

		// it is expected to be process "participant3"
		assertNotNull(targetProcess);
		assertTrue(targetProcess.getName().equals("participant3"));

	}

	@Test
	public void testSplitControlLink() throws SplitControlLinkException, RuntimeDataException,
			AmbiguousPropertyForLinkException {

		ControlLinkFragmenter ctrlLinkFragmenter = new ControlLinkFragmenter(data);

		// split the control link
		ctrlLinkFragmenter.splitControlLink();

		//
		// test split links - sending block
		//
		Process participant1 = data.getFragmentProcess("participant1");

		// all the links coming out of AssignB are split
		// we expect sending blocks there.
		Activity assignB = MyBPELUtils.resolveActivity("AssignB", participant1);
		Sources assignBSources = assignB.getSources();
		// assignB has 3 out-coming links
		assertEquals(assignBSources.getChildren().size(), 3);
		// test sending blocks
		for (Source source : assignBSources.getChildren()) {
			Link link = source.getLink();
			Target target = link.getTargets().get(0);
			Activity sendingBlock = target.getActivity();
			// the combined activity is expected to be a scope
			assertNotNull(sendingBlock);
			assertTrue(sendingBlock instanceof Scope);
		}

		//
		// test split links - receiving block
		//
		
		// all the links coming in the AssignE are split,
		// we expect receiving blocks there
		Process participant3 = data.getFragmentProcess("participant3");
		Activity assignE = MyBPELUtils.resolveActivity("AssignE", participant3);
		Targets assignETargets = assignE.getTargets();
		// assignE has 3 in-coming links
		assertEquals(assignETargets.getChildren().size(), 3);
		// test receiving blocks
		for (Target target : assignETargets.getChildren()) {
			Link link = target.getLink();
			Source source = link.getSources().get(0);
			Activity receivingBlock = source.getActivity();
			assertNotNull(receivingBlock);
			assertTrue(receivingBlock instanceof Sequence);
			
			Sequence sequence = (Sequence) receivingBlock;
			Activity receive = sequence.getActivities().get(0);
			assertTrue(receive instanceof Receive);
			
			Activity assign = sequence.getActivities().get(1);
			assertTrue(assign instanceof Assign);
		}

		//
		// test unsplit links
		//
		Link unsplitLink = MyBPELUtils.findLinkInActivityTarget("ReceiveA2AssignB", participant1);
		// test target
		Target targetOfLink = unsplitLink.getTargets().get(0);
		Activity targetAct = targetOfLink.getActivity();
		assertNotNull(targetAct);
		assertTrue(targetAct instanceof Assign);
		assertTrue(targetAct.getName().equals("AssignB"));

		// test source
		Source sourceOfLink = unsplitLink.getSources().get(0);
		Activity sourceAct = sourceOfLink.getActivity();
		assertNotNull(sourceAct);
		assertTrue(sourceAct instanceof Receive);
		assertTrue(sourceAct.getName().equals("ReceiveA"));

	}

}

class MyControlLinkFragmenter extends ControlLinkFragmenter {

	public MyControlLinkFragmenter(RuntimeData data) {
		super(data);
	}

	public Process getTargetFragemntProcess(Link linkInSourceProcess) {
		return super.getTargetFragmentProcess(linkInSourceProcess);
	}

}
