package org.sgnn7.ourobo.eventing;

import org.sgnn7.ourobo.util.LogMe;

import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;

public class LazyLoadingListener extends SimpleEventManager implements OnScrollListener {
	private final int lazyLoaderThreshold;
	private boolean isLoading;

	public LazyLoadingListener(int lazyLoaderThreshold) {
		this.lazyLoaderThreshold = lazyLoaderThreshold;
		this.isLoading = false;
	}

	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		int lastVisibleItem = firstVisibleItem + visibleItemCount;
		LogMe.d("Last index: " + (lastVisibleItem + lazyLoaderThreshold));
		LogMe.d("Threshold: " + totalItemCount);

		if (isLoading || totalItemCount == 0) {
			return;
		} else if (lastVisibleItem + lazyLoaderThreshold >= totalItemCount) {
			LogMe.e("Notifying listeners");
			notifyManagedListeners();
			isLoading = true;
		}
	}

	public void addLazyLoaderEventListener(IChangeEventListener listener) {
		addListenerToManager(listener);
	}

	public void contentLoaded() {
		isLoading = false;
	}

	public void onScrollStateChanged(AbsListView view, int scrollState) {
	}
}
