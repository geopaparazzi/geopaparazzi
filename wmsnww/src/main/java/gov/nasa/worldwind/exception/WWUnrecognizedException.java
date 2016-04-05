/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.exception;

/**
 * @author dcollins
 * @version $Id: WWUnrecognizedException.java 733 2012-09-02 17:15:09Z dcollins $
 */
public class WWUnrecognizedException extends WWRuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5525150187232623055L;

	/**
	 * Construct an exception with a message string.
	 * 
	 * @param msg
	 *            the message.
	 */
	public WWUnrecognizedException(String msg) {
		super(msg);
	}

	/**
	 * Construct an exception with a message string and a initial-cause exception.
	 * 
	 * @param msg
	 *            the message.
	 * @param t
	 *            the exception causing this exception.
	 */
	public WWUnrecognizedException(String msg, Throwable t) {
		super(msg, t);
	}
}
