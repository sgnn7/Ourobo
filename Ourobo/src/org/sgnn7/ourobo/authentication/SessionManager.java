package org.sgnn7.ourobo.authentication;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.sgnn7.ourobo.PreferencesManager;
import org.sgnn7.ourobo.R;
import org.sgnn7.ourobo.data.AuthenticationResponse;
import org.sgnn7.ourobo.util.JsonUtils;
import org.sgnn7.ourobo.util.LogMe;

import android.content.Context;
import android.widget.Toast;

public class SessionManager {
	private final PreferencesManager preferencesManager;
	private final Context context;

	private String cookieData = null;

	public SessionManager(Context context, PreferencesManager preferencesManager) {
		this.context = context;
		this.preferencesManager = preferencesManager;
	}

	public boolean authenticateUser() {
		boolean isLoggedIn = false;

		String username = preferencesManager.getValue(R.string.preference_id_username, String.class);
		String password = preferencesManager.getValue(R.string.preference_id_password, String.class);

		if (isValidUsername(username) && isValidPassword(password)) {
			AuthenticationResponse response = sendAuthenticationRequest(username, password);
			if (response.getErrorMessages().isEmpty() && response.getCookie() != null) {
				showMessage(R.string.authentication_success, Toast.LENGTH_SHORT);
				cookieData = response.getCookie();
				isLoggedIn = cookieData != null;
				LogMe.e("Logged in (cookieData=" + cookieData + ")");
			} else {
				displayErrors(response.getErrorMessages());
			}
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
		HttpClient httpClient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost("http://json.reddit.com/api/login/" + username);

		String responseContent = null;
		try {
			List<NameValuePair> postParameters = new ArrayList<NameValuePair>();
			postParameters.add(new BasicNameValuePair("api_type", "json"));
			postParameters.add(new BasicNameValuePair("user", username));
			postParameters.add(new BasicNameValuePair("passwd", password));
			httpPost.setEntity(new UrlEncodedFormEntity(postParameters));

			HttpResponse response = httpClient.execute(httpPost);
			responseContent = IOUtils.toString(response.getEntity().getContent());
			LogMe.e("Response: " + responseContent);
		} catch (Exception e) {
			LogMe.e("Error posting auth credentials: " + e.getMessage());
			LogMe.e(e);
		}
		return responseContent;
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
		}

		LogMe.e("Cookie retrieval: " + authCookie.toString());
		return authCookie;
	}
}
