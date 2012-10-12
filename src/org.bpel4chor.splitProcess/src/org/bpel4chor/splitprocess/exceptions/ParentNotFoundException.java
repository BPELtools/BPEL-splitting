package org.bpel4chor.splitprocess.exceptions;


/**
 * Exception when no parent for given activity can be found by ActivityFinder
 * 
 * @since Feb 12, 2012
 * @author Daojun Cui
 * 
 */
public class ParentNotFoundException extends ActivityNotFoundException {

	private static final long serialVersionUID = 5611216356870351052L;

	public ParentNotFoundException() {
		super();
	}

	public ParentNotFoundException(String message) {
		super(message);
	}

	public ParentNotFoundException(Throwable cause) {
		super(cause);
	}

	public ParentNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

}
