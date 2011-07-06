package org.sgnn7.ourobo.util;

import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.sgnn7.ourobo.data.UrlFileType;

public class HttpUtils {
	public static String getPageContent(String uri) {
		return new String(getBinaryPageContent(uri));
	}

	public static byte[] getBinaryPageContent(String uri) {
		long startTime = System.currentTimeMillis();

		byte[] pageContent = null;
		try {
			HttpGet page = new HttpGet(uri);
			HttpResponse response = new DefaultHttpClient().execute(page);
			pageContent = IOUtils.toByteArray(response.getEntity().getContent());
			LogMe.d("Size: " + pageContent.length);
			LogMe.d(new String(pageContent));
		} catch (Exception e) {
			e.printStackTrace();
			LogMe.e("Error while trying to retrieve '" + uri + "'");
		}

		LogMe.logTime(startTime, "retrieve the data from " + uri);

		return pageContent;
	}

	public static UrlFileType getFileType(URL url) {
		UrlFileType targetType = UrlFileType.UNKNOWN;
		for (UrlFileType type : UrlFileType.values()) {
			String extension = getExtension(url.getFile());
			if (type.getExtensions().contains(extension)) {
				LogMe.d("Image found: " + url.getFile());
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

	public static byte[] getBinaryPageContent(String host, String urlOrPath) {
		byte[] content = null;
		if (urlOrPath.startsWith("/")) {
			content = getBinaryPageContent(host + urlOrPath);
		} else {
			content = getBinaryPageContent(urlOrPath);
		}

		return content;
	}
}
