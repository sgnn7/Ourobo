package org.sgnn7.ourobo;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;

public class BrowserViewActivity extends Activity {
	public static final String LOCATION = "image.location";

	private static final FrameLayout.LayoutParams ZOOM_PARAMS = new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String imageLocation = getIntent().getStringExtra(LOCATION);
		WebView webView = new WebView(this);

		setContentView(webView);

		FrameLayout webViewContent = (FrameLayout) getWindow().getDecorView().findViewById(android.R.id.content);
		final View zoomControlsView = webView.getZoomControls();
		webViewContent.addView(zoomControlsView, ZOOM_PARAMS);

		WebSettings webSettings = webView.getSettings();
		webSettings.setBuiltInZoomControls(true);
		webSettings.setJavaScriptEnabled(true);
		webSettings.setSupportZoom(true);
		webView.setInitialScale(100);

		webView.loadUrl(imageLocation);

	}
}
