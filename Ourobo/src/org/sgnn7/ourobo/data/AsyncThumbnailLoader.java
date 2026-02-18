package org.sgnn7.ourobo.data;

import org.apache.commons.text.StringEscapeUtils;
import org.sgnn7.ourobo.util.ImageCacheManager;
import org.sgnn7.ourobo.util.LogMe;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ViewSwitcher;

public class AsyncThumbnailLoader {
	private final ViewSwitcher thumbnailHolder;
	private final ImageView thumbnail;
	private final View postHolder;
	private final String host;
	private final String id;

	public AsyncThumbnailLoader(View postHolder, ViewSwitcher thumbnailHolder, ImageView thumbnail, String host,
			String id) {
		this.postHolder = postHolder;
		this.thumbnailHolder = thumbnailHolder;
		this.thumbnail = thumbnail;
		this.host = host;
		this.id = id;
	}

	public void loadImage(Activity activity, String imageUrl) {
		boolean isValidImageUrl = imageUrl != null && imageUrl.startsWith("http");
		if (isValidImageUrl) {
			String sanitizedUrl = StringEscapeUtils.unescapeHtml4(imageUrl);
			if (sanitizedUrl.startsWith("http://")) {
				sanitizedUrl = "https" + sanitizedUrl.substring(4);
			}
			LogMe.i("Loading image " + sanitizedUrl);

			ImageCacheManager.getImage(activity, host, sanitizedUrl, new UpdateUiImageLoadedListener(activity,
					thumbnailHolder, thumbnail, id));
		} else if (thumbnail.getTag().equals(id)) {
			LayoutParams layoutParams = new RelativeLayout.LayoutParams(thumbnailHolder.getLayoutParams());
			layoutParams.width = 0;
			layoutParams.height = 0;

			thumbnailHolder.setLayoutParams(layoutParams);

			postHolder.forceLayout();
			postHolder.invalidate();
		}
	}
}
