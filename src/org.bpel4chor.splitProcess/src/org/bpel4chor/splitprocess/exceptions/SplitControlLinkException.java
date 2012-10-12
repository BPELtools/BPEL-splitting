package org.bpel4chor.splitprocess.exceptions;
/**
 * Exception from splitting control link
 * 
 * @since Feb 11, 2012
 * @author Daojun Cui
 */
public class SplitControlLinkException extends Exception {

	private static final long serialVersionUID = 4046031229430334259L;

	public SplitControlLinkException() {
		super();
	}

	public SplitControlLinkException(String message) {
		super(message);
	}

	public SplitControlLinkException(Throwable cause) {
		super(cause);
	}

	public SplitControlLinkException(String message, Throwable cause) {
		super(message, cause);
	}

}
