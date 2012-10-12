package org.bpel4chor.splitprocess.dataflowanalysis;

import org.bpel4chor.splitprocess.exceptions.DataFlowAnalysisException;
import org.eclipse.bpel.model.Process;

import de.uni_stuttgart.iaas.bpel_d.algorithm.analysis.AnalysisResult;
/**
 * The DataFlowAnalyzer analyses the data flow of the process given and returns the AnalysisResult.
 * 
 * @since Feb 24, 2012
 * @author Daojun Cui
 */
public class DataFlowAnalyzer {
	/**
	 * analyse the data flow of the process given
	 * 
	 * @throws DataFlowAnalysisException
	 */
	public static AnalysisResult analyze(Process process) throws DataFlowAnalysisException {
		AnalysisResult analysisRes = null;
		try {
			analysisRes = de.uni_stuttgart.iaas.bpel_d.algorithm.analysis.Process.analyzeProcessModel(process);
			return analysisRes;
		} catch (Exception e) {
			throw new DataFlowAnalysisException(e);
		}
	}
}
