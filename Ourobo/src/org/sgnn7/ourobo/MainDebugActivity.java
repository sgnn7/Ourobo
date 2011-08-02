package org.sgnn7.ourobo;

public class MainDebugActivity extends MainActivity {
	private static final String DEBUG_SERVER = "10.0.2.2:8080";
	private static final String MAIN_DEBUG_URL = HTTP_PROTOCOL_PREFIX + DEBUG_SERVER + "/RedditService/RedditService";
	private static final String JSON_DEBUG_URL = HTTP_PROTOCOL_PREFIX + DEBUG_SERVER + "/RedditService/RedditService";
	private static final String MOBILE_DEBUG_URL = HTTP_PROTOCOL_PREFIX + DEBUG_SERVER + "/RedditService/RedditService";

	@Override
	protected String getMainUrl() {
		return MAIN_DEBUG_URL;
	}

	@Override
	protected String getJsonUrl() {
		return JSON_DEBUG_URL;
	}

	@Override
	protected String getMobileUrl() {
		return MOBILE_DEBUG_URL;
	}
}
