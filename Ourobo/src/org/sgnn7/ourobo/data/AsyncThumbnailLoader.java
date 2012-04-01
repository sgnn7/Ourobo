package org.sgnn7.ourobo.data;

import org.sgnn7.ourobo.util.ImageCacheManager;
import org.sgnn7.ourobo.util.LogMe;

import android.app.Activity;
import android.content.res.Resources;
import android.view.View;
import android.widget.ImageView;
import android.widget.ViewSwitcher;

public class AsyncThumbnailLoader {
	private final ViewSwitcher thumbnailHolder;
	private final ImageView thumbnail;
	private final View postHolder;
	private final String host;

	public AsyncThumbnailLoader(View postHolder, ViewSwitcher thumbnailHolder, ImageView thumbnail, String host) {
		this.postHolder = postHolder;
		this.thumbnailHolder = thumbnailHolder;
		this.thumbnail = thumbnail;
		this.host = host;
	}

	public void loadImage(Activity activity, String imageUrl) {
		Resources resources = activity.getResources();

		boolean isValidImageUrl = imageUrl != null && imageUrl.length() > 0;
		if (isValidImageUrl) {
			LogMe.i("Loading image " + imageUrl);

			ImageCacheManager.getImage(resources, host, imageUrl, new UpdateUiImageLoadedListener(activity,
					thumbnailHolder, thumbnail));
		} else {
			thumbnailHolder.setVisibility(View.GONE);
		}
		postHolder.requestLayout();
	}
}
