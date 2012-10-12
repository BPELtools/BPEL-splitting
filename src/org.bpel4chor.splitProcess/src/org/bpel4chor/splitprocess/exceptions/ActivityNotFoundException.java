package org.bpel4chor.splitprocess.exceptions;
/**
 * Exception when no activity can be found by ActivityFinder
 * 
 * @since Feb 12, 2012
 * @author Daojun Cui
 */
public class ActivityNotFoundException extends Exception {

	private static final long serialVersionUID = 8594043120961425645L;

	public ActivityNotFoundException() {
		super();
	}

	public ActivityNotFoundException(String message) {
		super(message);
	}

	public ActivityNotFoundException(Throwable cause) {
		super(cause);
	}

	public ActivityNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

}
