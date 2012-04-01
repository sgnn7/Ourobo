package org.sgnn7.ourobo;

import java.util.List;

import org.sgnn7.ourobo.eventing.IChangeEventListener;
import org.sgnn7.ourobo.util.LogMe;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class EventingWebViewClient extends WebViewClient {
	private static final String WWW_PREFIX = "www.";
	private static final String VIDEO_QUERY_PARAMETER = "v";
	private static final String HTTP_PROTOCOL_PREFIX = "http://";
	private static final String MOBILE_YOUTUBE_SITE = "m.youtube.com/";
	private static final String MAIN_YOUTUBE_SITE = "youtube.com/";

	private static final String YOUTUBE_LINK_PREFIX = "vnd.youtube://";
	private static final String NO_MATCHING_INTENT = "Cannot find an app to handle this kind of link";

	private final Activity activity;

	private IChangeEventListener pageLoadedListener;
	private IChangeEventListener errorOccuredListener;
	private IChangeEventListener pageStartedListener;

	public EventingWebViewClient(Activity activity) {
		this.activity = activity;
	}

	@Override
	public void onPageFinished(WebView view, String url) {
		pageLoadedListener.handle();
		super.onPageFinished(view, url);
	}

	public void setPageLoadedListener(IChangeEventListener listener) {
		pageLoadedListener = listener;
	}

	public void setPageStartedListener(IChangeEventListener listener) {
		pageStartedListener = listener;
	}

	public void setErrorOccuredListener(IChangeEventListener listener) {
		errorOccuredListener = listener;
	}

	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		LogMe.d("Loading: " + url);

		boolean overrideUrlLoading = false;
		if (url.startsWith(YOUTUBE_LINK_PREFIX)) {
			overrideUrlLoading = openYoutubeApp(url);
		} else if (isYoutubeLink(url) && extractVideoId(url) != null) {
			String modifiedUrl = YOUTUBE_LINK_PREFIX + extractVideoId(url);

			LogMe.i("Modified URL: " + modifiedUrl);

			overrideUrlLoading = openYoutubeApp(modifiedUrl);
		}

		return overrideUrlLoading;
	}

	@Override
	public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
		errorOccuredListener.handle();
	}

	@Override
	public void onPageStarted(WebView view, String url, Bitmap favicon) {
		pageStartedListener.handle();
	}

	private String extractVideoId(String url) {
		String videoId = Uri.parse(url).getQueryParameter(VIDEO_QUERY_PARAMETER);

		LogMe.d("Video ID: " + videoId);

		return videoId;
	}

	private boolean isYoutubeLink(String url) {
		boolean isYoutubeLink = false;
		if (isMobileYoutubeSite(url) || isMainYoutubeSite(url)) {
			isYoutubeLink = true;
		}

		LogMe.e("Is youtube link: " + isYoutubeLink);

		return isYoutubeLink;
	}

	private boolean isMobileYoutubeSite(String url) {
		return url.startsWith(HTTP_PROTOCOL_PREFIX + MOBILE_YOUTUBE_SITE) || url.startsWith(MOBILE_YOUTUBE_SITE);
	}

	private boolean isMainYoutubeSite(String url) {
		return url.startsWith(HTTP_PROTOCOL_PREFIX + MAIN_YOUTUBE_SITE)
				|| url.startsWith(HTTP_PROTOCOL_PREFIX + WWW_PREFIX + MAIN_YOUTUBE_SITE)
				|| url.startsWith(MAIN_YOUTUBE_SITE);
	}

	private boolean openYoutubeApp(String url) {
		LogMe.e("YouTube link: " + url);

		boolean overrideUrlLoading = false;
		if (canHandleYoutubeLinks(url)) {
			try {
				Intent videoIntent = new Intent(Intent.ACTION_VIEW);
				videoIntent.setData(Uri.parse(url));

				activity.startActivity(videoIntent);
				activity.finish();

				overrideUrlLoading = true;
			} catch (ActivityNotFoundException anfe) {
				displayAndLogIntentError(anfe);
			}
		} else {
			displayAndLogIntentError(new ActivityNotFoundException("YouTube intent search failed"));
		}

		return overrideUrlLoading;
	}

	private boolean canHandleYoutubeLinks(String url) {
		Intent urlIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		PackageManager packageManager = activity.getPackageManager();

		List<ResolveInfo> list = packageManager.queryIntentActivities(urlIntent, PackageManager.MATCH_DEFAULT_ONLY);
		return !list.isEmpty();
	}

	private void displayAndLogIntentError(ActivityNotFoundException anfe) {
		LogMe.e(anfe);
		LogMe.e(NO_MATCHING_INTENT);
		Toast.makeText(activity, NO_MATCHING_INTENT, Toast.LENGTH_LONG).show();
	}
}
