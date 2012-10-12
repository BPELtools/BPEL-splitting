package org.bpel4chor.splitprocess.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
@RunWith(Suite.class)
@Suite.SuiteClasses({
	/**
	 * Test Suite for all.
	 * If there are more tests coming, just add them into the following list regarding the format.
	 */
	org.bpel4chor.splitprocess.test.partition.representation.PartitionSpecReaderTest.class,
	org.bpel4chor.splitprocess.test.partition.representation.PartitionSpecWriterTest.class,
	org.bpel4chor.splitprocess.test.partition.util.PartitionVerificatorTest.class,
	org.bpel4chor.splitprocess.test.utils.ActivityIteratorTest.class,
	org.bpel4chor.splitprocess.test.utils.ActivityUtilTest.class,
	org.bpel4chor.splitprocess.test.utils.ActivityFinderTest.class,
	org.bpel4chor.splitprocess.test.fragmentation.FragmentDuplicatorTest.class,
	org.bpel4chor.splitprocess.test.utils.LinkFinderTest.class,
	org.bpel4chor.splitprocess.test.utils.MyBPELUtilsTest.class,
	org.bpel4chor.splitprocess.test.utils.MyWSDLUtilTest.class,
	org.bpel4chor.splitprocess.test.utils.NameGeneratorTest.class,
	org.bpel4chor.splitprocess.test.utils.VariableResolverTest.class,
	org.bpel4chor.splitprocess.test.utils.VariableUtilTest.class,
	org.bpel4chor.splitprocess.test.dataflowanalysis.DataFlowAnalyzerTest.class,
	org.bpel4chor.splitprocess.test.dataflowanalysis.AnalysisResultParserTest.class,
	org.bpel4chor.splitprocess.test.dataflowanalysis.QueryWriterSetTest.class,
	org.bpel4chor.splitprocess.test.pwdg.model.PWDGNodeTest.class,
	org.bpel4chor.splitprocess.test.pwdg.model.PWDGTest.class,
	org.bpel4chor.splitprocess.test.pwdg.util.WDGFactoryTest.class,
	org.bpel4chor.splitprocess.test.pwdg.util.PWDGNodeConstructorTest.class,
	org.bpel4chor.splitprocess.test.pwdg.util.PWDGFactoryTest.class,
	org.bpel4chor.splitprocess.test.pwdg.util.RandomIdGeneratorTest.class,
	org.bpel4chor.splitprocess.test.fragmentation.BPEL4ChorOutputTest.class,
	org.bpel4chor.splitprocess.test.fragmentation.ControlLinkBlockBuilderTest.class,
	org.bpel4chor.splitprocess.test.fragmentation.FragmentFactoryTest.class,
	org.bpel4chor.splitprocess.test.fragmentation.ProcessFragmenterTest.class,
	org.bpel4chor.splitprocess.test.fragmentation.ControlLinkFragmenterTest.class,
	org.bpel4chor.splitprocess.test.fragmentation.DataDependencyHelperTest.class,
	org.bpel4chor.splitprocess.test.fragmentation.DataDependencyFragmenterTest.class,
	org.bpel4chor.splitprocess.test.fragmentation.DataDependencyFragmenterTestCase1.class,
	org.bpel4chor.splitprocess.test.fragmentation.DataDependencyFragmenterTestCase2.class,
	org.bpel4chor.splitprocess.test.fragmentation.DataDependencyFragmenterTestCase3.class,
	org.bpel4chor.splitprocess.test.fragmentation.DataDependencyFragmenterTestCase4.class,
	org.bpel4chor.splitprocess.test.fragmentation.DataDependencyFragmenterTestCase5.class,
	org.bpel4chor.splitprocess.test.cli.SplitProcessMainCmdTest.class,
	org.bpel4chor.splitprocess.test.ProcessSplitterTestCase1.class
})
public class AllTests {

//	public static Test suite() {
//		TestSuite suite = new TestSuite(AllTests.class.getName());
//		//$JUnit-BEGIN$
//
//		//$JUnit-END$
//		return suite;
//	}

}
