package org.sgnn7.ourobo;

import java.util.ArrayList;
import java.util.List;

import org.sgnn7.ourobo.eventing.IChangeEventListener;

import android.webkit.WebView;
import android.webkit.WebViewClient;

public class EventingWebViewClient extends WebViewClient {
	List<IChangeEventListener> listeners = new ArrayList<IChangeEventListener>();

	@Override
	public void onPageFinished(WebView view, String url) {
		notifyAllListeners();
		super.onPageFinished(view, url);
	}

	private void notifyAllListeners() {
		for (IChangeEventListener listener : listeners) {
			listener.handle();
		}
	}

	public void addPageLoadedListener(IChangeEventListener listener) {
		listeners.add(listener);
	}
}
