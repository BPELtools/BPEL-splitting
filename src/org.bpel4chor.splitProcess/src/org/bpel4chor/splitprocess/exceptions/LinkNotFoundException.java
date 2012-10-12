package org.bpel4chor.splitprocess.exceptions;
/**
 * Exception when no link is found by given criterion.
 * 
 * @since Feb 12, 2012
 * @author Daojun Cui
 */
public class LinkNotFoundException extends Exception {
	
	private static final long serialVersionUID = -3455258050641741864L;
	
	public LinkNotFoundException() {
		super();
	}

	public LinkNotFoundException(String message) {
		super(message);
	}

	public LinkNotFoundException(Throwable cause) {
		super(cause);
	}

	public LinkNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

}
