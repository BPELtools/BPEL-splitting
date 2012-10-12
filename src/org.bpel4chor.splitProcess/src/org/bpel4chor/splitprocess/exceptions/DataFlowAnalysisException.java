package org.bpel4chor.splitprocess.exceptions;
/**
 * Exception from Data flow analysis
 * 
 * @since Feb 11, 2012
 * @author Daojun Cui
 */
public class DataFlowAnalysisException extends Exception {

	private static final long serialVersionUID = 5627702301106981028L;

	public DataFlowAnalysisException() {
		super();
	}

	public DataFlowAnalysisException(String message, Throwable cause) {
		super(message, cause);
	}

	public DataFlowAnalysisException(String message) {
		super(message);
	}

	public DataFlowAnalysisException(Throwable cause) {
		super(cause);
	}
	
}
