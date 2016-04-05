/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.exception;

/**
 * @author dcollins
 * @version $Id: WWRuntimeException.java 733 2012-09-02 17:15:09Z dcollins $
 */
public class WWRuntimeException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4560764319453773951L;

	/**
	 * Construct an exception with a message string.
	 * 
	 * @param msg
	 *            the message.
	 */
	public WWRuntimeException(String msg) {
		super(msg);
	}

	/**
	 * Construct an exception from an initial-cause exception.
	 * 
	 * @param throwable
	 *            the exception causing this exception.
	 */
	public WWRuntimeException(Throwable throwable) {
		super(throwable);
	}

	/**
	 * Construct an exception with a message string and a initial-cause exception.
	 * 
	 * @param msg
	 *            the message.
	 * @param throwable
	 *            the exception causing this exception.
	 */
	public WWRuntimeException(String msg, Throwable throwable) {
		super(msg, throwable);
	}
}
