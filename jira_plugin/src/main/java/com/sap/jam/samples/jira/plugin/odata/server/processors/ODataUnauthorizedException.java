package com.sap.jam.samples.jira.plugin.odata.server.processors;

import org.apache.olingo.odata2.api.commons.HttpStatusCodes;
import org.apache.olingo.odata2.api.exception.MessageReference;
import org.apache.olingo.odata2.api.exception.ODataHttpException;

// Olingo, strangely, does not have this exception.  We will need such a thing, so we'll make our own...
public class ODataUnauthorizedException extends ODataHttpException {

	private static final long serialVersionUID = 1L;

	public static final MessageReference COMMON = createMessageReference(ODataHttpException.class, "COMMON");

	public ODataUnauthorizedException(final MessageReference messageReference) {
		super(messageReference, HttpStatusCodes.UNAUTHORIZED);
	}

	public ODataUnauthorizedException(final MessageReference messageReference, final String errorCode) {
		super(messageReference, HttpStatusCodes.UNAUTHORIZED, errorCode);
	}

	public ODataUnauthorizedException(final MessageReference messageReference, final Throwable cause) {
		super(messageReference, cause, HttpStatusCodes.UNAUTHORIZED);
	}

	public ODataUnauthorizedException(final MessageReference messageReference, final Throwable cause, final String errorCode) {
		super(messageReference, cause, HttpStatusCodes.UNAUTHORIZED, errorCode);
	}
	
	public ODataUnauthorizedException() {
		this(COMMON);
	}
}
