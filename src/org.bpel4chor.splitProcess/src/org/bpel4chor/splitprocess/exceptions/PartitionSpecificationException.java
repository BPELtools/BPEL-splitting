package org.bpel4chor.splitprocess.exceptions;

/**
 * PartitionSpecificationException shows up most likely when something is wrong
 * with the xpath in partition specification.
 * <p>
 * In case that the partition specification is invalid, you will be expecting
 * this exception.
 * 
 * @since Feb 29, 2012
 * @author Daojun Cui
 */
public class PartitionSpecificationException extends Exception {

	private static final long serialVersionUID = 4721262199181162134L;

	public PartitionSpecificationException() {
		super();
	}

	public PartitionSpecificationException(String message, Throwable cause) {
		super(message, cause);
	}

	public PartitionSpecificationException(String message) {
		super(message);
	}

	public PartitionSpecificationException(Throwable cause) {
		super(cause);
	}

}
