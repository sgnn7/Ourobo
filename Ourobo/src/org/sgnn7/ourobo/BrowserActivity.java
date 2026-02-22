package org.sgnn7.ourobo;

import org.sgnn7.ourobo.eventing.IChangeEventListener;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

public class BrowserActivity extends AppCompatActivity {
	public static final String URL_PARAMETER_KEY = "image.location";

	private WebView webView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.browser_page);

		final RelativeLayout browserLayout = (RelativeLayout) findViewById(R.id.main_browser_view);

		webView = new WebView(this);
		webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

		browserLayout.addView(webView, 0, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

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
		});

		WebSettings webSettings = webView.getSettings();
		webSettings.setBuiltInZoomControls(true);
		webSettings.setDisplayZoomControls(false);
		webSettings.setJavaScriptEnabled(true);
		webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
		webSettings.setLoadsImagesAutomatically(true);
		webSettings.setSupportZoom(true);
		webView.setInitialScale(1);
		webSettings.setUseWideViewPort(true);
		webSettings.setLoadWithOverviewMode(true);

		webSettings.setAllowFileAccess(true);
		webSettings.setDomStorageEnabled(true);
		webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
		webSettings.setLayoutAlgorithm(LayoutAlgorithm.TEXT_AUTOSIZING);

		webView.setWebViewClient(client);

		getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				if (webView.canGoBack()) {
					webView.goBack();
				} else {
					webView.stopLoading();
					webView.loadUrl("about:blank");
					finish();
				}
			}
		});

		loadIntentUrl();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		super.onNewIntent(intent);

		loadIntentUrl();
	}

	private void loadIntentUrl() {
		webView.clearHistory();
		webView.setBackgroundColor(Color.WHITE);

		webView.loadUrl(getIntent().getStringExtra(URL_PARAMETER_KEY));
	}

	@Override
	protected void onPause() {
		webView.stopLoading();

		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();

		webView.resumeTimers();
	}
}
