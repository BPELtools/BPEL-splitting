package org.bpel4chor.splitprocess.exceptions;

import org.bpel4chor.splitprocess.pwdg.model.PWDG;

/**
 * PWDGException can be caused by CycleFoundException or
 * PartitionSpecificationException while the {@link PWDG} is being created.
 * 
 * @since Mar 2, 2012
 * @author Daojun Cui
 */
public class PWDGException extends Exception {

	private static final long serialVersionUID = 909591633495125141L;

	public PWDGException() {
		super();
	}

	public PWDGException(String message, Throwable cause) {
		super(message, cause);
	}

	public PWDGException(String message) {
		super(message);
	}

	public PWDGException(Throwable cause) {
		super(cause);
	}

}
