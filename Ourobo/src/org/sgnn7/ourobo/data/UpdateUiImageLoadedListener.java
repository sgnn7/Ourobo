package org.sgnn7.ourobo.data;

import org.sgnn7.ourobo.eventing.IImageLoadedListener;
import org.sgnn7.ourobo.util.LogMe;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ViewSwitcher;

public class UpdateUiImageLoadedListener implements IImageLoadedListener {
	private final ViewSwitcher thumbnailHolder;
	private final ImageView thumbnail;
	private final Activity activity;
	private final String id;

	public UpdateUiImageLoadedListener(Activity activity, ViewSwitcher thumbnailHolder, ImageView thumbnail, String id) {
		this.thumbnailHolder = thumbnailHolder;
		this.thumbnail = thumbnail;
		this.id = id;

		this.activity = activity;
	}

	public void finishedLoading(final Drawable thumbnailDrawable) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				if (id.equals(thumbnail.getTag())) {
					if (thumbnailDrawable != null) {
						LogMe.i("Found image for " + id + ". Showing the image");
						thumbnail.setImageDrawable(thumbnailDrawable);
						thumbnailHolder.setDisplayedChild(1);
					} else {
						LogMe.i("Image for " + id + " was nothing. Hiding the image");
						LayoutParams layoutParams = new RelativeLayout.LayoutParams(thumbnailHolder.getLayoutParams());
						layoutParams.width = 0;
						layoutParams.height = 0;
						thumbnailHolder.setLayoutParams(layoutParams);
					}

					thumbnailHolder.forceLayout();
					thumbnailHolder.invalidate();
				} else {
					LogMe.w("Ignoring reused view drawing. Id = " + id + ", Current Tag = " + thumbnail.getTag());
				}
			}
		});
	}
}
