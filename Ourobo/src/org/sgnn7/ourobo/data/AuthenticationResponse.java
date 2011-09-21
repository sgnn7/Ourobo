package org.sgnn7.ourobo.data;

import java.util.ArrayList;
import java.util.List;

public class AuthenticationResponse {
	private String modhash;
	private String cookie;

	private final List<String> errorMessages = new ArrayList<String>();

	public String getModhash() {
		return modhash;
	}

	public void setModhash(String modhash) {
		this.modhash = modhash;
	}

	public String getCookie() {
		return cookie;
	}

	public void setCookie(String cookie) {
		this.cookie = cookie;
	}

	public List<String> getErrorMessages() {
		return errorMessages;
	}
}
