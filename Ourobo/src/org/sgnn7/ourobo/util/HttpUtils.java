package org.sgnn7.ourobo.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.sgnn7.ourobo.authentication.SessionManager;
import org.sgnn7.ourobo.data.UrlFileType;

public class HttpUtils {
	private static final int DOWNLOAD_TIMEOUT = 20000;
	private static final int CONNECTION_TIMEOUT = 5000;

	public static String getPageContent(SessionManager sessionManager, String uri) {
		return new String(getBinaryPageContent(sessionManager, uri));
	}

	public static byte[] getBinaryPageContent(SessionManager sessionManager, String host, String urlOrPath) {
		boolean isRelativeLink = urlOrPath.startsWith("/");

		return getBinaryPageContent(sessionManager, isRelativeLink ? host + urlOrPath : urlOrPath);
	}

	public static byte[] getBinaryPageContent(SessionManager sessionManager, String uri) {
		long startTime = System.currentTimeMillis();

		byte[] pageContent = null;
		try {
			HttpGet page = new HttpGet(uri);

			DefaultHttpClient httpClient = new DefaultHttpClient();
			setAuthCookie(sessionManager, httpClient);

			HttpResponse response = httpClient.execute(page);
			pageContent = IOUtils.toByteArray(response.getEntity().getContent());
			LogMe.d("Size: " + pageContent.length);
			LogMe.d(new String(pageContent));
		} catch (OutOfMemoryError oome) {
			System.gc();
			LogMe.e("OOM. Cleaned garbage");
		} catch (Exception e) {
			e.printStackTrace();
			LogMe.e("Error while trying to retrieve '" + uri + "'");
		}

		LogMe.logTime(startTime, "retrieve the data from " + uri);

		return pageContent;
	}

	private static void setAuthCookie(SessionManager sessionManager, DefaultHttpClient httpClient) {
		if (sessionManager != null) {
			Cookie authenticationCookie = sessionManager.getAuthenticationCookie();
			if (authenticationCookie != null) {
				httpClient.getCookieStore().addCookie(authenticationCookie);
			}
		}
	}

	public static UrlFileType getFileType(String url) {
		UrlFileType targetType = UrlFileType.UNKNOWN;
		for (UrlFileType type : UrlFileType.values()) {
			String extension = getExtension(url);
			if (type.getExtensions().contains(extension)) {
				LogMe.d("Image found: " + url);
				targetType = type;
				break;
			}
		}

		return targetType;
	}

	private static String getExtension(String filename) {
		String extension = "";
		int indexOfDelimiter = filename.lastIndexOf(".");
		if (indexOfDelimiter > 0) {
			extension = filename.substring(indexOfDelimiter + 1);
		}

		LogMe.d("Extension for " + filename + " is: " + extension);
		return extension.toLowerCase();
	}

	public static String doPost(SessionManager sessionManager, String baseUrl, String path,
			Map<String, String> parameterMap) {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		setAuthCookie(sessionManager, httpClient);

		HttpPost httpPost = new HttpPost(baseUrl + "/" + path);

		String responseContent = null;
		try {
			LogMe.e("Doing POST to " + baseUrl);

			List<NameValuePair> postParameters = new ArrayList<NameValuePair>();
			for (String key : parameterMap.keySet()) {
				postParameters.add(new BasicNameValuePair(key, parameterMap.get(key)));
			}
			httpPost.setEntity(new UrlEncodedFormEntity(postParameters, HTTP.UTF_8));

			HttpParams params = httpPost.getParams();
			HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
			HttpConnectionParams.setSoTimeout(params, DOWNLOAD_TIMEOUT);

			HttpResponse response = httpClient.execute(httpPost);
			responseContent = IOUtils.toString(response.getEntity().getContent());

			LogMe.e("Response: " + responseContent);
		} catch (Exception e) {
			LogMe.e("Error posting values: " + e.getMessage());
			LogMe.e(e);
		}

		return responseContent;
	}
}
