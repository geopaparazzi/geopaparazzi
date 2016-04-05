/* Copyright (C) 2001, 2012 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
 */
package gov.nasa.worldwind.exception;

/**
 * Thrown when a World Wind operation times out.
 * 
 * @author tag
 * @version $Id: WWTimeoutException.java 804 2012-09-26 01:46:04Z dcollins $
 */
public class WWTimeoutException extends WWRuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1247567988322973160L;

	/**
	 * Construct an exception with a message string.
	 * 
	 * @param msg
	 *            the message.
	 */
	public WWTimeoutException(String msg) {
		super(msg);
	}
}
