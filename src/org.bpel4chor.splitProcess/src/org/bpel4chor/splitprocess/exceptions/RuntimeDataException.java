package org.bpel4chor.splitprocess.exceptions;
/**
 * Exception in Runtime Data
 * 
 * @since Feb 11, 2012
 * @author Daojun Cui
 */
public class RuntimeDataException extends Exception {

	private static final long serialVersionUID = 7178860996087951340L;

	public RuntimeDataException() {
		super();
	}

	public RuntimeDataException(String message, Throwable cause) {
		super(message, cause);
	}

	public RuntimeDataException(String message) {
		super(message);
	}

	public RuntimeDataException(Throwable cause) {
		super(cause);
	}

}
