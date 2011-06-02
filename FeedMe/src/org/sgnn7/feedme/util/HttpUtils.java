package org.sgnn7.feedme.util;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

public class HttpUtils {
	public static String getPageContent(String uri) {
		long startTime = System.currentTimeMillis();

		String pageContent = null;
		try {
			HttpGet page = new HttpGet(uri);
			HttpResponse response = new DefaultHttpClient().execute(page);
			pageContent = IOUtils.toString(response.getEntity().getContent());
			LogMe.d("Size: " + pageContent.length());
			LogMe.d(pageContent);
		} catch (Exception e) {
			e.printStackTrace();
		}

		LogMe.logTime(startTime, "retrieve the data from " + uri);

		return pageContent;
	}
}
