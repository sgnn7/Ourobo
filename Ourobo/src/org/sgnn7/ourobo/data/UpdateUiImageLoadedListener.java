package org.sgnn7.ourobo.data;

import org.sgnn7.ourobo.eventing.IImageLoadedListener;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.ViewSwitcher;

public class UpdateUiImageLoadedListener implements IImageLoadedListener {
	private final ViewSwitcher thumbnailHolder;
	private final ImageView thumbnail;
	private final Activity activity;

	public UpdateUiImageLoadedListener(Activity activity, ViewSwitcher thumbnailHolder, ImageView thumbnail) {
		this.thumbnailHolder = thumbnailHolder;
		this.thumbnail = thumbnail;

		this.activity = activity;
	}

	public void finishedLoading(final Drawable thumbnailDrawable) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				if (thumbnailDrawable != null) {
					thumbnailHolder.setVisibility(View.VISIBLE);
					thumbnailHolder.reset();
					thumbnail.setImageDrawable(thumbnailDrawable);
					thumbnailHolder.showNext();
				} else {
					thumbnailHolder.setVisibility(View.GONE);
				}
			}
		});
	}
}
