package org.sgnn7.ourobo;

import org.sgnn7.ourobo.eventing.IChangeEventListener;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ViewSwitcher;

public class BrowserViewActivity extends Activity {
	public static final String LOCATION = "image.location";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String destinationUrl = getIntent().getStringExtra(LOCATION);

		setContentView(R.layout.browser_page);

		final ViewSwitcher viewSwitcher = (ViewSwitcher) findViewById(R.id.main_browser_view);

		WebView webView = new WebView(this);
		webView.setBackgroundColor(R.color.black);
		viewSwitcher.addView(webView);

		EventingWebViewClient client = new EventingWebViewClient();
		client.addPageLoadedListener(new IChangeEventListener() {
			public void handle() {
				if (!(viewSwitcher.getCurrentView() instanceof WebView)) {
					viewSwitcher.showNext();
				}
			}
		});

		webView.setWebViewClient(client);

		WebSettings webSettings = webView.getSettings();
		webSettings.setBuiltInZoomControls(true);
		webSettings.setJavaScriptEnabled(true);
		webSettings.setSupportZoom(true);
		webView.setInitialScale(100);

		webView.loadUrl(destinationUrl);
	}
}
