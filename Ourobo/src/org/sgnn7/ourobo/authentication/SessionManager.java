package org.sgnn7.ourobo.authentication;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.sgnn7.ourobo.PreferencesManager;
import org.sgnn7.ourobo.R;
import org.sgnn7.ourobo.data.AuthenticationResponse;
import org.sgnn7.ourobo.util.HttpUtils;
import org.sgnn7.ourobo.util.JsonUtils;
import org.sgnn7.ourobo.util.LogMe;

import android.content.Context;
import android.widget.Toast;

public class SessionManager {
	private final PreferencesManager preferencesManager;
	private final Context context;
	private final String baseUrl;

	private String cookieData = null;

	public SessionManager(Context context, String baseUrl, PreferencesManager preferencesManager) {
		this.context = context;
		this.baseUrl = baseUrl;
		this.preferencesManager = preferencesManager;
	}

	public boolean authenticateUser() {
		boolean isLoggedIn = false;

		if (getAuthenticationCookie() == null) {
			String username = preferencesManager.getString(R.string.preference_id_username);
			String password = preferencesManager.getString(R.string.preference_id_password);

			if (isValidUsername(username) && isValidPassword(password)) {
				AuthenticationResponse response = sendAuthenticationRequest(username, password);
				if (response.getErrorMessages().isEmpty() && response.getCookie() != null) {
					showMessage(R.string.authentication_success, Toast.LENGTH_SHORT);
					cookieData = response.getCookie();
					isLoggedIn = cookieData != null;
					LogMe.e("Logged in (cookieData=" + cookieData + ")");

					// only for debug
					LogMe.e("Checking authentication cookie");
					String pageContent = HttpUtils.getPageContent(this, "http://www.reddit.com/api/me.json");
					LogMe.e("Got response: " + pageContent);
				} else {
					displayErrors(response.getErrorMessages());
				}
			}
		} else {
			isLoggedIn = true;
		}

		return isLoggedIn;
	}

	private void displayErrors(List<String> errorMessages) {
		showMessage(R.string.authentication_failure, Toast.LENGTH_LONG);

		for (String errorMessage : errorMessages) {
			LogMe.e("Logging in failed: " + errorMessage);
			showMessage(errorMessage, Toast.LENGTH_LONG);
		}
	}

	private AuthenticationResponse sendAuthenticationRequest(String username, String password) {
		showMessage(R.string.authentication_start, Toast.LENGTH_SHORT);
		LogMe.e("Logging in with username: " + username);

		AuthenticationResponse authenticationResponse = new AuthenticationResponse();

		String responseContent = postLoginCredentials(username, password);
		if (responseContent != null) {
			JsonNode topNode = null;
			try {
				topNode = new ObjectMapper().readValue(responseContent, JsonNode.class);
			} catch (Exception e) {
				LogMe.e(e);
				e.printStackTrace();
			}

			List<JsonNode> errorJsonNodes = JsonUtils.getJsonChildren(topNode, "json/errors");
			if (!errorJsonNodes.isEmpty()) {
				for (JsonNode jsonNode : errorJsonNodes) {
					String readableErrorText = jsonNode.get(1).toString();
					authenticationResponse.getErrorMessages().add(readableErrorText);
				}
			} else {
				authenticationResponse = JsonUtils
						.convertJsonToBean(topNode, "json/data", AuthenticationResponse.class);
			}
		}

		return authenticationResponse;
	}

	private String postLoginCredentials(String username, String password) {
		Map<String, String> parameterMap = new HashMap<String, String>();
		parameterMap.put("api_type", "json");
		parameterMap.put("user", username);
		parameterMap.put("passwd", password);

		return HttpUtils.doPost(null, baseUrl, "api/login/", parameterMap);
	}

	private boolean isValidPassword(String password) {
		return isValueValid(password, R.string.bad_password);
	}

	private boolean isValidUsername(String username) {
		return isValueValid(username, R.string.bad_username);
	}

	private boolean isValueValid(String value, int errorMessageId) {
		boolean isValid = true;
		if (StringUtils.isEmpty(value)) {
			showMessage(errorMessageId, Toast.LENGTH_LONG);
			isValid = false;
		}

		return isValid;
	}

	private void showMessage(int messageId, int duration) {
		showMessage(context.getResources().getString(messageId), duration);
	}

	private void showMessage(String message, int duration) {
		Toast.makeText(context, message, duration).show();
	}

	public Cookie getAuthenticationCookie() {
		BasicClientCookie authCookie = null;

		if (cookieData != null) {
			authCookie = new BasicClientCookie("reddit_session", cookieData);
			authCookie.setDomain(".reddit.com");
			authCookie.setPath("/");
			LogMe.e("Using auth cookie: " + authCookie.toString());
		} else {
			LogMe.e("Not using auth cookie");
		}

		return authCookie;
	}
}
