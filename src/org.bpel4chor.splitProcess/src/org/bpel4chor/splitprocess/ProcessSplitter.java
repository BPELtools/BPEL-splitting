package org.bpel4chor.splitprocess;

import java.io.IOException;
import java.util.Calendar;

import javax.wsdl.WSDLException;
import javax.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;
import org.bpel4chor.splitprocess.exceptions.DataFlowAnalysisException;
import org.bpel4chor.splitprocess.exceptions.PartitionSpecificationException;
import org.bpel4chor.splitprocess.exceptions.SplitControlLinkException;
import org.bpel4chor.splitprocess.exceptions.SplitDataDependencyException;
import org.bpel4chor.splitprocess.fragmentation.ControlLinkFragmenter;
import org.bpel4chor.splitprocess.fragmentation.DataDependencyFragmenter;
import org.bpel4chor.splitprocess.fragmentation.ProcessFragmenter;
import org.bpel4chor.splitprocess.partition.model.PartitionSpecification;
import org.bpel4chor.splitprocess.partition.representation.PartitionSpecReader;
import org.bpel4chor.splitprocess.utils.SplitProcessConstants;
import org.bpel4chor.utils.BPEL4ChorReader;
import org.bpel4chor.utils.BPEL4ChorWriter;
import org.bpel4chor.utils.MyWSDLUtil;
import org.eclipse.bpel.model.Process;
import org.eclipse.wst.wsdl.Definition;

/**
 * The BPEL Process Splitter reads in a BPEL process and a partition
 * specification, splits the process then generates PBDs, Grounding, Topology
 * and the associated WSDLs into output directory. Besides, a ZIP file that
 * contains the contents of that directory will also be created. The file name
 * will take the naming convention as follows: <code>Non-Split-ProcessName +
 * 'Choreography' + CurrentDateInMilliseconds.zip"</code>
 * <p>
 * The output directory can be customized, if it is not be given, the default
 * value will be used. Default output directory is
 * {@link SplitProcessConstants#DEFAULT_SPLITTING_OUTPUT_DIR}
 * 
 * @since Feb 9, 2012
 * @author Daojun Cui
 */
public class ProcessSplitter {

	/**
	 * Necessary data for running fragmenting process, splitting control link,
	 * splitting data dependencies
	 */
	protected RuntimeData data = null;

	protected Logger logger = Logger.getLogger(ProcessSplitter.class);

	/**
	 * Constructor
	 * 
	 * @param bpelFilePath
	 * @param partitionFilePath
	 * @throws WSDLException
	 * @throws IOException
	 * @throws PartitionSpecificationException
	 */
	public ProcessSplitter(String bpelFilePath, String partitionFilePath) throws WSDLException,
			IOException, PartitionSpecificationException {

		// main process
		Process process = BPEL4ChorReader.readBPEL(bpelFilePath);

		// definition
		Definition defn = MyWSDLUtil.getWSDLOf(process);

		// partition specification
		PartitionSpecReader reader = new PartitionSpecReader();
		PartitionSpecification partitionSpec = reader.readSpecification(partitionFilePath, process);

		// runtime data
		data = new RuntimeData(process, partitionSpec, defn);
	}

	/**
	 * Constructor
	 * 
	 * @param bpelFilePath
	 * @param partitionFilePath
	 * @param outputDir
	 * @throws PartitionSpecificationException
	 * @throws IOException
	 * @throws WSDLException
	 */
	public ProcessSplitter(String bpelFilePath, String partitionFilePath, String outputDir)
			throws PartitionSpecificationException, WSDLException, IOException {

		// main process
		Process process = BPEL4ChorReader.readBPEL(bpelFilePath);

		// definition
		Definition defn = MyWSDLUtil.getWSDLOf(process);

		// partition specification
		PartitionSpecReader reader = new PartitionSpecReader();
		PartitionSpecification partitionSpec = reader.readSpecification(partitionFilePath, process);

		// runtime data
		data = new RuntimeData(process, partitionSpec, defn, outputDir);
	}

	/**
	 * Constructor
	 * 
	 * @param process
	 * @param partitionSpec
	 * @param outputDir
	 * @throws IOException
	 * @throws WSDLException
	 */
	public ProcessSplitter(Process process, PartitionSpecification partitionSpec, String outputDir)
			throws WSDLException, IOException {

		if (process == null || partitionSpec == null || outputDir == null || outputDir.isEmpty())
			throw new NullPointerException("Illeagal arguments. process==null:" + (process == null)
					+ " partitionSpec==null:" + (partitionSpec == null));

		// read in the wsdl of non-split process
		Definition defn = MyWSDLUtil.getWSDLOf(process);

		if (outputDir == null || outputDir.isEmpty()) {
			// initialize runtime data without given output directory
			data = new RuntimeData(process, partitionSpec, defn);
		} else {
			// initialize runtime data with given output directory
			data = new RuntimeData(process, partitionSpec, defn, outputDir);
		}
	}

	/**
	 * Constructor
	 * 
	 * @param process
	 * @param partitionSpec
	 * @throws IOException
	 * @throws WSDLException
	 */
	public ProcessSplitter(Process process, PartitionSpecification partitionSpec)
			throws WSDLException, IOException {

		if (process == null || partitionSpec == null)
			throw new IllegalArgumentException("Illeagal arguments. process==null:"
					+ (process == null) + " partitionSpec==null:" + (partitionSpec == null));

		// read in the wsdl of non-split process
		Definition defn = MyWSDLUtil.getWSDLOf(process);

		// instantiate runtime data
		data = new RuntimeData(process, partitionSpec, defn);
	}

	/**
	 * Split the process and compress the output bpel4chor artifacts into zip
	 * file
	 * 
	 * @return The result zip file name, null in case of exception.
	 * 
	 * @throws SplitControlLinkException
	 * @throws DataFlowAnalysisException
	 * @throws PartitionSpecificationException
	 * @throws SplitDataDependencyException
	 * @throws IOException
	 * @throws WSDLException
	 * @throws XMLStreamException
	 */
	public String split() throws SplitControlLinkException, DataFlowAnalysisException,
			PartitionSpecificationException, SplitDataDependencyException, IOException,
			WSDLException, XMLStreamException {

		// if there is only one partition, no need to split
		if (data.getPartitionSpec().getParticipants().size() == 1) {
			logger.info("There is only one partition, no need to split.");
			return null;
		}

		// fragment the main process into smaller fragment processes
		ProcessFragmenter procFragmenter = new ProcessFragmenter(data);
		procFragmenter.fragmentizeProcess();

		// split control link - creating sending block and receiving block
		ControlLinkFragmenter linkFragmenter = new ControlLinkFragmenter(data);
		linkFragmenter.splitControlLink();

		// split data dependencies - creating local resolver and receiving flow
		DataDependencyFragmenter dataDepFragmenter = new DataDependencyFragmenter(data);
		dataDepFragmenter.splitDataDependency();

		// output BPEL4Chor artifacts
		//
		// the artifacts will be output into the pre-set output directory in the
		// runtime data, after that the files in the output directory will be
		// compressed into one zip file, which will then be placed in the given
		// destined directory.
		String outputZipFileName = data.getNonSplitProcess().getName() + "Choreography-"
				+ Calendar.getInstance().getTimeInMillis() + ".zip";
		BPEL4ChorWriter.writeBPEL4Chor(data.getParticipant2FragProcMap(),
				data.getParticipant2WSDLMap(), data.getTopology(), data.getGrounding(),
				data.getOutputDir(), SplitProcessConstants.DEFAULT_SPLITTING_OUTPUT_DIR,
				outputZipFileName);

		return outputZipFileName;
	}

}
