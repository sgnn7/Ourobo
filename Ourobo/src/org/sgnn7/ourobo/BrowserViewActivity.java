package org.sgnn7.ourobo;

import org.sgnn7.ourobo.eventing.IChangeEventListener;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

public class BrowserViewActivity extends Activity {
	public static final String LOCATION = "image.location";
	private WebView webView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.browser_page);

		final RelativeLayout browserLayout = (RelativeLayout) findViewById(R.id.main_browser_view);

		webView = new WebView(this);

		// Hack for faulty APIs
		webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT);
		browserLayout.addView(webView, 0, layoutParams);

		final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_meter);

		EventingWebViewClient client = new EventingWebViewClient(this);
		client.setPageLoadedListener(new IChangeEventListener() {
			public void handle() {
				progressBar.setVisibility(View.INVISIBLE);
			}
		});

		client.setPageStartedListener(new IChangeEventListener() {
			public void handle() {
				progressBar.setVisibility(View.VISIBLE);
			}
		});

		client.setErrorOccuredListener(new IChangeEventListener() {
			public void handle() {
				webView.setBackgroundColor(Color.WHITE);
			}
		});

		webView.setWebChromeClient(new WebChromeClient() {
			@Override
			public void onProgressChanged(WebView view, int progress) {
				progressBar.setProgress(progress);
			}

			@Override
			public void onReachedMaxAppCacheSize(long spaceNeeded, long totalUsedQuota,
					WebStorage.QuotaUpdater quotaUpdater) {
				quotaUpdater.updateQuota(spaceNeeded * 2);
			}
		});

		webView.setBackgroundColor(Color.BLACK);
		webView.setInitialScale(100);
		webView.setWebViewClient(client);

		WebSettings webSettings = webView.getSettings();
		webSettings.setBuiltInZoomControls(true);
		webSettings.setJavaScriptEnabled(true);
		webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
		webSettings.setLoadsImagesAutomatically(true);
		webSettings.setPluginsEnabled(true);
		webSettings.setSupportZoom(true);
		webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);

		webSettings.setAllowFileAccess(true);
		webSettings.setDomStorageEnabled(true);
		webSettings.setAppCacheMaxSize(16 * 1024 * 1024);
		webSettings.setAppCacheEnabled(true);
		webSettings.setAppCachePath("/data/data/org.sgnn7.ourobo/cache");
		webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

		String destinationUrl = getIntent().getStringExtra(LOCATION);
		webView.loadUrl(destinationUrl);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		webView.clearHistory();
		webView.setBackgroundColor(Color.BLACK);

		String destinationUrl = getIntent().getStringExtra(LOCATION);
		webView.loadUrl(destinationUrl);
	}

	@Override
	public void onBackPressed() {
		if (webView.canGoBack()) {

			webView.goBack();
		} else {
			super.onBackPressed();
		}
	}
}
