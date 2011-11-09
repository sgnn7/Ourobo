package org.sgnn7.ourobo;

import org.sgnn7.ourobo.eventing.IChangeEventListener;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
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

		String destinationUrl = getIntent().getStringExtra(LOCATION);

		setContentView(R.layout.browser_page);

		final RelativeLayout browserLayout = (RelativeLayout) findViewById(R.id.main_browser_view);

		webView = new WebView(this);

		// Hack for faulty APIs
		webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT);
		browserLayout.addView(webView, 0, layoutParams);

		EventingWebViewClient client = new EventingWebViewClient(this);
		client.addPageLoadedListener(new IChangeEventListener() {
			public void handle() {
				View progressIndicator = BrowserViewActivity.this.findViewById(R.id.progress_meter);
				progressIndicator.setVisibility(View.INVISIBLE);
			}
		});

		webView.setWebChromeClient(new WebChromeClient() {
			@Override
			public void onProgressChanged(WebView view, int progress) {
				ProgressBar progressMeter = (ProgressBar) BrowserViewActivity.this.findViewById(R.id.progress_meter);
				progressMeter.setProgress(progress);
			}
		});

		webView.setBackgroundColor(R.color.black);
		webView.setInitialScale(100);
		webView.setWebViewClient(client);

		WebSettings webSettings = webView.getSettings();
		webSettings.setBuiltInZoomControls(true);
		webSettings.setJavaScriptEnabled(true);
		webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
		webSettings.setLoadsImagesAutomatically(true);
		webSettings.setPluginsEnabled(true);
		webSettings.setSupportZoom(true);
		webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
		webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);

		webView.loadUrl(destinationUrl);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
			webView.goBack();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}
