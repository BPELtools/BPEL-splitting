package org.bpel4chor.splitprocess.exceptions;
/**
 * Exception from splitting data dependency
 * 
 * @since Feb 11, 2012
 * @author Daojun Cui
 */
public class SplitDataDependencyException extends Exception {

	private static final long serialVersionUID = -4230699747946806299L;

	public SplitDataDependencyException() {
		super();
	}

	public SplitDataDependencyException(String message) {
		super(message);
	}

	public SplitDataDependencyException(Throwable cause) {
		super(cause);
	}

	public SplitDataDependencyException(String message, Throwable cause) {
		super(message, cause);
	}

}
