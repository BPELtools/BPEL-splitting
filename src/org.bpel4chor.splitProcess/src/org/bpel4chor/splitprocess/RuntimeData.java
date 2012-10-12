package org.bpel4chor.splitprocess;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.bpel4chor.model.grounding.impl.Grounding;
import org.bpel4chor.model.topology.impl.Topology;
import org.bpel4chor.splitprocess.exceptions.RuntimeDataException;
import org.bpel4chor.splitprocess.partition.model.Participant;
import org.bpel4chor.splitprocess.partition.model.PartitionSpecification;
import org.bpel4chor.splitprocess.utils.SplitProcessConstants;
import org.bpel4chor.utils.BPEL4ChorFactory;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Process;
import org.eclipse.wst.wsdl.Definition;

import de.uni_stuttgart.iaas.bpel_d.algorithm.analysis.AnalysisResult;

/**
 * Runtime Data, collection of all necessary data in runtime.
 * 
 * @since Feb 10, 2012
 * @author Daojun Cui
 */
public class RuntimeData {

	/** output directory for BPEL4ChorWriter */
	protected String outputDir = null;

	/** BPEL process */
	protected Process nonSplitProcess = null;

	/** WSDL definition of non-split process */
	protected Definition nonSplitProcessDefn = null;

	/** The Partition Specification */
	protected PartitionSpecification partitionSpec = null;

	/** Data Flow Analysis of BPEL process */
	protected AnalysisResult dataFlowAnalysisRes = null;

	/** Participant to fragment process map */
	protected Map<String, Process> participant2FragProc = new HashMap<String, Process>();

	/** Participant to WSDL definition map */
	protected Map<String, Definition> participant2WSDL = new HashMap<String, Definition>();

	/** Topology for BPEL4Chor */
	protected Topology topology = null;

	/** Grounding for BPEL4Chor */
	protected Grounding grounding = null;

	protected Logger logger = Logger.getLogger(RuntimeData.class);

	/**
	 * Constructor
	 * 
	 * @param nonSplitProcess
	 * @param partitionSpec
	 * @param nonSplitProcessDefn
	 * @param outputDir
	 */
	public RuntimeData(Process nonSplitProcess, PartitionSpecification partitionSpec,
			Definition nonSplitProcessDefn, String outputDir) {

		if (nonSplitProcess == null || partitionSpec == null || nonSplitProcessDefn == null
				|| outputDir == null)
			throw new NullPointerException();

		if (outputDir.isEmpty())
			throw new IllegalStateException("outputDir can not be empty");

		this.nonSplitProcess = nonSplitProcess;
		this.partitionSpec = partitionSpec;
		this.nonSplitProcessDefn = nonSplitProcessDefn;
		this.outputDir = outputDir;
		this.topology = BPEL4ChorFactory.createTopology(nonSplitProcess.getName() + "Topology");
		this.grounding = BPEL4ChorFactory.createGrounding(this.topology);

	}

	/**
	 * Constructor
	 * 
	 * @param nonSplitProcess
	 * @param partitionSpec
	 * @param nonSplitProcessDefn
	 */
	public RuntimeData(Process nonSplitProcess, PartitionSpecification partitionSpec,
			Definition nonSplitProcessDefn) {

		if (nonSplitProcess == null || partitionSpec == null || nonSplitProcessDefn == null)
			throw new NullPointerException();

		this.nonSplitProcess = nonSplitProcess;
		this.partitionSpec = partitionSpec;
		this.nonSplitProcessDefn = nonSplitProcessDefn;
		this.topology = BPEL4ChorFactory.createTopology(nonSplitProcess.getName() + "Topology");
		this.grounding = BPEL4ChorFactory.createGrounding(this.topology);
		this.outputDir = SplitProcessConstants.DEFAULT_SPLITTING_OUTPUT_DIR + File.separator
				+ Calendar.getInstance().getTimeInMillis();
	}

	/**
	 * Get the participant's fragment process
	 * 
	 * @param participantName
	 * @return
	 * @throws RuntimeDataException
	 */
	public Process getFragmentProcess(String participantName) {
		if (participantName == null)
			throw new NullPointerException("illegal argument, participant==null:"
					+ (participantName == null));
		if (participant2FragProc == null)
			throw new NullPointerException("the participant2FragProcMap is null");

		return participant2FragProc.get(participantName);
	}

	/**
	 * Get activities that are present in the participant
	 * 
	 * @param participantName
	 * @return
	 */
	public Set<Activity> getActivities(String participantName) {
		if (participantName == null || participantName.isEmpty())
			throw new IllegalStateException("illegal argument, participant==null:"
					+ (participantName == null) + " or participant is empty");
		if (partitionSpec == null)
			throw new NullPointerException("the partitionSpec is null");

		Participant participant = partitionSpec.getParticipant(participantName);
		if (participant == null)
			throw new NullPointerException("Participant '" + participantName + "' does not exists.");

		return participant.getActivities();

	}

	/**
	 * Get the participant's WSDL definition
	 * 
	 * @param participantName
	 * @return
	 */
	public Definition getFragmentDefinition(String participantName) {
		if (participantName == null)
			throw new NullPointerException("illegal argument, participant==null:"
					+ (participantName == null));
		if (participant2WSDL == null)
			throw new IllegalStateException("the participant2WSDLMap is null");

		return participant2WSDL.get(participantName);
	}

	public Definition getNonSplitProcessDfn() {
		return nonSplitProcessDefn;
	}

	public void setNonSplitProcessDfn(Definition nonSplitProcessDfn) {
		if (nonSplitProcessDfn == null)
			throw new NullPointerException();
		this.nonSplitProcessDefn = nonSplitProcessDfn;
	}

	/**
	 * @return Output directory for BPEL4ChorWriter
	 */
	public String getOutputDir() {
		return outputDir;
	}

	public void setOutputDir(String outputDir) {
		this.outputDir = outputDir;
	}

	/**
	 * @return Non Split Process
	 */
	public Process getNonSplitProcess() {
		return nonSplitProcess;
	}

	public void setNonSplitProcess(Process process) {
		if (process == null)
			throw new NullPointerException();
		this.nonSplitProcess = process;
	}

	/**
	 * @return Partition Specification
	 */
	public PartitionSpecification getPartitionSpec() {
		return partitionSpec;
	}

	public void setPartitionSpec(PartitionSpecification partitionSpec) {
		if (partitionSpec == null)
			throw new NullPointerException();
		this.partitionSpec = partitionSpec;
	}

	public AnalysisResult getDataFlowAnalysisRes() {
		return dataFlowAnalysisRes;
	}

	public void setDataFlowAnalysisRes(AnalysisResult dataFlowAnalysisRes) {
		if (dataFlowAnalysisRes == null)
			throw new NullPointerException("argument is null.");
		this.dataFlowAnalysisRes = dataFlowAnalysisRes;
	}

	public Map<String, Process> getParticipant2FragProcMap() {
		if (participant2FragProc == null) {
			participant2FragProc = new HashMap<String, Process>();
		}
		return participant2FragProc;
	}

	public void setParticipant2FragProcMap(Map<String, Process> participant2FragProcMap) {
		if (participant2FragProcMap == null)
			throw new NullPointerException("argument is null.");
		this.participant2FragProc = participant2FragProcMap;
	}

	public Map<String, Definition> getParticipant2WSDLMap() {
		if (participant2WSDL == null) {
			participant2WSDL = new HashMap<String, Definition>();
		}
		return participant2WSDL;
	}

	public void setParticipant2WSDLMap(Map<String, Definition> participant2wsdlMap) {
		if (participant2wsdlMap == null)
			throw new NullPointerException("argument is null.");
		this.participant2WSDL = participant2wsdlMap;
	}

	public Topology getTopology() {
		if (topology == null)
			throw new NullPointerException("topology is set to null");
		return topology;
	}

	public void setTopology(Topology topology) {
		if (topology == null)
			throw new NullPointerException();
		this.topology = topology;
	}

	public Grounding getGrounding() {
		if (grounding == null)
			throw new NullPointerException();
		return grounding;
	}

	public void setGrounding(Grounding grounding) {
		if (grounding == null)
			throw new NullPointerException();
		this.grounding = grounding;
	}
}
