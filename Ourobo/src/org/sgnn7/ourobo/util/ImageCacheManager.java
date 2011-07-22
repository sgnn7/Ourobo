package org.sgnn7.ourobo.util;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.sgnn7.ourobo.eventing.IImageLoadedListener;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class ImageCacheManager {

	private static Map<String, WeakReference<Drawable>> imageCacheMap = new ConcurrentHashMap<String, WeakReference<Drawable>>();
	private static Set<String> downloadList = Collections.synchronizedSet(new HashSet<String>());

	public static void getImage(final String host, final String imageUrl, final IImageLoadedListener callback) {
		new Thread(new Runnable() {
			public void run() {
				Drawable image = getImageSync(host, imageUrl);
				callback.finishedLoading(image);
			}
		}).start();
	}

	private static Drawable getImageSync(String host, String imageUrl) {
		final String key = imageUrl;
		if (!isImageInMap(key)) {
			waitUntilOtherThreadDownloadsImage(key);

			if (!isImageInMap(key)) {
				downloadList.add(key);
				LogMe.e("Cache miss on key: " + key);

				Drawable image = downloadImage(host, imageUrl);
				if (image != null) {
					WeakReference<Drawable> weakBitmapReference = new WeakReference<Drawable>(image);
					imageCacheMap.put(key, weakBitmapReference);
				}
				downloadList.remove(key);
			}
		} else {
			LogMe.e("Cache hit on key: " + key);
		}

		Drawable returnDrawable = null;
		WeakReference<Drawable> weakReference = imageCacheMap.get(key);
		if (weakReference != null && weakReference.get() != null) {
			returnDrawable = weakReference.get();
		}

		return returnDrawable;
	}

	private static Drawable downloadImage(final String host, final String imageUrl) {
		BitmapDrawable drawable = null;

		if (imageUrl != null && imageUrl.length() > 0) {
			try {
				byte[] binaryContent = HttpUtils.getBinaryPageContent(host, imageUrl);
				if (binaryContent != null) {
					Bitmap bitmap = BitmapFactory.decodeByteArray(binaryContent, 0, binaryContent.length);
					drawable = new BitmapDrawable(bitmap);
				}
			} catch (OutOfMemoryError oome) {
				System.gc();
				LogMe.e("Cleaned garbage");
			} catch (Exception e) {
				LogMe.e(e);
			}
		}

		return drawable;
	}

	private static boolean isImageInMap(String key) {
		return imageCacheMap.containsKey(key) && imageCacheMap.get(key) != null && imageCacheMap.get(key).get() != null;
		// return imageCacheMap.containsKey(key) && imageCacheMap.get(key) != null;
	}

	private static void waitUntilOtherThreadDownloadsImage(String key) {
		while (downloadList.contains(key)) {
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static void stopDownloads() {
		// TODO finish me
	}
}
