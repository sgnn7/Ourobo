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
		viewSwitcher.addView(webView);
		// webView.setDownloadListener(new DownloadListener() {
		// public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType,
		// long contentLength) {
		// Intent viewIntent = new Intent(Intent.ACTION_VIEW);
		// viewIntent.setDataAndType(Uri.parse(url), mimeType);
		//
		// try {
		// startActivity(viewIntent);
		// } catch (ActivityNotFoundException ex) {
		// LogMe.e("Couldn't find activity to view mimetype: " + mimeType);
		// }
		//
		// }
		// });

		EventingWebViewClient client = new EventingWebViewClient(this);
		client.addPageLoadedListener(new IChangeEventListener() {
			public void handle() {
				if (!(viewSwitcher.getCurrentView() instanceof WebView)) {
					viewSwitcher.showNext();
				}
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

		webView.loadUrl(destinationUrl);
	}
}
