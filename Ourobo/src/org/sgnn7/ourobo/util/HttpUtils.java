package org.sgnn7.ourobo.util;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.sgnn7.ourobo.authentication.SessionManager;
import org.sgnn7.ourobo.data.UrlFileType;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HttpUtils {
	private static final int DOWNLOAD_TIMEOUT = 20;
	private static final int CONNECTION_TIMEOUT = 5;

	private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Mobile Safari/537.36";

	private static final OkHttpClient client = new OkHttpClient.Builder()
			.connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
			.readTimeout(DOWNLOAD_TIMEOUT, TimeUnit.SECONDS)
			.addInterceptor(chain -> chain.proceed(
					chain.request().newBuilder()
							.header("User-Agent", USER_AGENT)
							.build()))
			.build();

	public static String getPageContent(SessionManager sessionManager, String uri) {
		byte[] bytes = getBinaryPageContent(sessionManager, uri);
		return bytes != null ? new String(bytes) : null;
	}

	public static byte[] getBinaryPageContent(SessionManager sessionManager, String host, String urlOrPath) {
		boolean isRelativeLink = urlOrPath.startsWith("/");
		return getBinaryPageContent(sessionManager, isRelativeLink ? host + urlOrPath : urlOrPath);
	}

	public static byte[] getBinaryPageContent(SessionManager sessionManager, String uri) {
		long startTime = System.currentTimeMillis();

		byte[] pageContent = null;
		try {
			LogMe.d("Loading page " + uri);

			Request.Builder requestBuilder = new Request.Builder().url(uri);
			addAuthCookie(sessionManager, requestBuilder);

			try (Response response = client.newCall(requestBuilder.build()).execute()) {
				if (!response.isSuccessful()) {
					LogMe.e("HTTP " + response.code() + " for " + uri);
				} else {
					ResponseBody body = response.body();
					if (body != null) {
						pageContent = body.bytes();
					}
					LogMe.d("Loaded (" + (pageContent != null ? pageContent.length : 0) + ") " + uri);
				}
			}
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

	private static void addAuthCookie(SessionManager sessionManager, Request.Builder requestBuilder) {
		if (sessionManager != null) {
			String cookieValue = sessionManager.getAuthenticationCookieValue();
			if (cookieValue != null) {
				requestBuilder.addHeader("Cookie", "reddit_session=" + cookieValue);
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
		String responseContent = null;
		try {
			LogMe.e("Doing POST to " + baseUrl);

			FormBody.Builder formBuilder = new FormBody.Builder();
			for (Map.Entry<String, String> entry : parameterMap.entrySet()) {
				formBuilder.add(entry.getKey(), entry.getValue());
			}

			Request.Builder requestBuilder = new Request.Builder()
					.url(baseUrl + "/" + path)
					.post(formBuilder.build());
			addAuthCookie(sessionManager, requestBuilder);

			try (Response response = client.newCall(requestBuilder.build()).execute()) {
				ResponseBody body = response.body();
				if (body != null) {
					responseContent = body.string();
				}
			}

			LogMe.e("Response: " + responseContent);
		} catch (Exception e) {
			LogMe.e("Error posting values: " + e.getMessage());
			LogMe.e(e);
		}

		return responseContent;
	}
}
