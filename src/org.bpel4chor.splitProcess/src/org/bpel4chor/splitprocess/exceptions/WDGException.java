package org.bpel4chor.splitprocess.exceptions;

import org.bpel4chor.splitprocess.pwdg.model.WDG;

/**
 * WDGException can be caused by CycleFoundException whilea a {@link WDG} is
 * being created.
 * 
 * @since Mar 2, 2012
 * @author Daojun Cui
 */
public class WDGException extends Exception {

	private static final long serialVersionUID = -8927876299901947537L;

	public WDGException() {
		super();
	}

	public WDGException(String message, Throwable cause) {
		super(message, cause);
	}

	public WDGException(String message) {
		super(message);
	}

	public WDGException(Throwable cause) {
		super(cause);
	}

}
