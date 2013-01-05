package org.sgnn7.ourobo.eventing;

import org.sgnn7.ourobo.util.LogMe;

import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;

public class LazyLoadingListener extends EventManager implements OnScrollListener {
	private final int lazyLoaderThreshold;
	private boolean isLoading;

	public LazyLoadingListener(int lazyLoaderThreshold) {
		this.lazyLoaderThreshold = lazyLoaderThreshold;
		this.isLoading = false;
	}

	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		int lastVisibleItem = firstVisibleItem + visibleItemCount;

		if (isLoading) {
			LogMe.i("Loading already. Ignoring multiple request");
			return;
		} else if (lastVisibleItem + lazyLoaderThreshold >= totalItemCount) {
			LogMe.e("Notifying listeners that we should download more data");
			notifyManagedListeners();
			isLoading = true;
		}
	}

	public void addLazyLoaderEventListener(IChangeEventListener listener) {
		addListenerToManager(listener);
	}

	public void contentLoaded() {
		LogMe.w("Content Loaded. Resetting 'isLoaded' flag");
		isLoading = false;
	}

	public void onScrollStateChanged(AbsListView view, int scrollState) {
	}
}
