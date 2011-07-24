package org.sgnn7.ourobo.data;

import org.sgnn7.ourobo.eventing.IImageLoadedListener;
import org.sgnn7.ourobo.util.ImageCacheManager;
import org.sgnn7.ourobo.util.LogMe;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.ViewSwitcher;

public class AsyncThumbnailLoader {
	private final ImageView thumbnail;
	private final String host;
	private final Activity activity;
	private final ViewSwitcher thumbnailHolder;
	private final View postHolder;

	public AsyncThumbnailLoader(Activity activity, View postHolder, ViewSwitcher thumbnailHolder, ImageView thumbnail,
			String host) {
		this.activity = activity;
		this.postHolder = postHolder;
		this.thumbnailHolder = thumbnailHolder;
		this.thumbnail = thumbnail;
		this.host = host;
	}

	public void loadImage(String imageUrl) {
		if (imageUrl != null && imageUrl.length() > 0) {
			IImageLoadedListener imageLoadedListener = new IImageLoadedListener() {
				public void finishedLoading(final Drawable drawable) {
					activity.runOnUiThread(new Runnable() {
						public void run() {
							if (drawable != null) {
								thumbnailHolder.setVisibility(View.VISIBLE);
								thumbnailHolder.reset();
								thumbnail.setImageDrawable(drawable);
								thumbnailHolder.showNext();
							} else {
								thumbnailHolder.setVisibility(View.GONE);
							}
						}
					});
				}
			};

			LogMe.e("Loading image " + imageUrl);
			ImageCacheManager.getImage(host, imageUrl, imageLoadedListener);
		} else {
			thumbnailHolder.setVisibility(View.GONE);
		}
		postHolder.requestLayout();
	}
}
