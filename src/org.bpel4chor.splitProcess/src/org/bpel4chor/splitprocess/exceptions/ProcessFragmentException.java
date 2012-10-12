package org.bpel4chor.splitprocess.exceptions;
/**
 * Exception from fragmenting process
 * 
 * @since Feb 11, 2012
 * @author Daojun Cui
 */
public class ProcessFragmentException extends Exception {

	private static final long serialVersionUID = 6134493297940080715L;

	public ProcessFragmentException() {
		super();
	}

	public ProcessFragmentException(String message, Throwable cause) {
		super(message, cause);
	}

	public ProcessFragmentException(String message) {
		super(message);
	}

	public ProcessFragmentException(Throwable cause) {
		super(cause);
	}

}
