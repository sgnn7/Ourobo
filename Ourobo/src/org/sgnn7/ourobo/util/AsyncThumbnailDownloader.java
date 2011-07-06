package org.sgnn7.ourobo.util;

import java.util.Set;

import org.sgnn7.ourobo.R;

import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;
import android.widget.ViewSwitcher;

public class AsyncThumbnailDownloader extends AsyncTask<Void, Void, Drawable> {
	private final ViewSwitcher viewSwitcher;

	private final Set<AsyncThumbnailDownloader> removalList;

	private final Drawable defaultDrawable;

	private final String host;
	private final String thumbnailUrl;
	private final String fullImageUrl;

	public AsyncThumbnailDownloader(String host, ViewSwitcher viewSwitcher, Set<AsyncThumbnailDownloader> removalList,
			Drawable defaultDrawable, String thumbnailUrl, String fullImageUrl) {
		this.host = host;
		this.viewSwitcher = viewSwitcher;
		this.removalList = removalList;
		this.defaultDrawable = defaultDrawable;
		this.thumbnailUrl = thumbnailUrl;
		this.fullImageUrl = fullImageUrl;
	}

	@Override
	protected Drawable doInBackground(Void... nothing) {
		Drawable thumbnail = null;
		if (fullImageUrl != null) {
			thumbnail = getImageThumbnailWithFailover();
		} else {
			thumbnail = getImage(thumbnailUrl);
		}

		if (thumbnail == null) {
			thumbnail = defaultDrawable;
		}

		return thumbnail;
	}

	@Override
	protected void onPostExecute(Drawable image) {
		removalList.remove(this);
		ImageView thumbnailView = (ImageView) viewSwitcher.findViewById(R.id.post_thumbnail);

		if (image != null) {
			thumbnailView.setImageDrawable(image);
		} else {
			viewSwitcher.setVisibility(View.GONE);
		}

		viewSwitcher.showNext();
	}

	private Drawable getImageThumbnailWithFailover() {
		Drawable thumbnail = getImage(thumbnailUrl);

		if (thumbnail == null) {
			thumbnail = getImage(fullImageUrl);
		}

		return thumbnail;
	}

	private Drawable getImage(String imageUrl) {
		Drawable thumbnail = null;

		if (imageUrl.length() > 0) {
			try {
				byte[] rawThumbnail = HttpUtils.getBinaryPageContent(host, imageUrl);
				thumbnail = new BitmapDrawable(BitmapFactory.decodeByteArray(rawThumbnail, 0, rawThumbnail.length));
			} catch (Exception e) {
				e.printStackTrace();
				LogMe.e(e);
			}
		}

		return thumbnail;
	}
}
