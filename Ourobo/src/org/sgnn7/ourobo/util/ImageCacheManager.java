package org.sgnn7.ourobo.util;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.sgnn7.ourobo.eventing.IImageLoadedListener;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class ImageCacheManager {
	private static Map<String, SoftReference<Drawable>> imageCache = new ConcurrentHashMap<String, SoftReference<Drawable>>();
	private static Set<String> downloadList = Collections.synchronizedSet(new HashSet<String>());

	private static ExecutorService executorService = Executors.newFixedThreadPool(10);

	public static void getImage(final String host, final String imageUrl, final IImageLoadedListener callback) {
		boolean isValidImageUrl = imageUrl != null && imageUrl.length() > 0;

		if (isValidImageUrl) {
			if (isImageInMap(imageUrl)) {
				LogMe.d("Cache hit on key: " + imageUrl);

				callback.finishedLoading(getDrawableFromCache(imageUrl));
			} else {
				LogMe.d("Cache miss on key: " + imageUrl);

				executorService.execute(new Runnable() {
					public void run() {
						callback.finishedLoading(getImageSynced(host, imageUrl));
					}
				});
			}
		}
	}

	private static Drawable getImageSynced(String host, String imageUrl) {
		waitUntilOtherThreadDownloadsImage(imageUrl);

		if (!isImageInMap(imageUrl)) {
			downloadList.add(imageUrl);

			Drawable image = downloadImage(host, imageUrl);
			if (image != null) {
				imageCache.put(imageUrl, new SoftReference<Drawable>(image));
			}
			downloadList.remove(imageUrl);
		}

		return getDrawableFromCache(imageUrl);
	}

	private static Drawable getDrawableFromCache(String imageUrl) {
		Drawable returnDrawable = null;
		SoftReference<Drawable> softReference = imageCache.get(imageUrl);
		if (softReference != null && softReference.get() != null) {
			returnDrawable = softReference.get();
		}
		return returnDrawable;
	}

	private static Drawable downloadImage(final String host, final String imageUrl) {
		BitmapDrawable drawable = null;

		try {
			byte[] binaryContent = HttpUtils.getBinaryPageContent(null, host, imageUrl);
			if (binaryContent != null) {
				Bitmap bitmap = BitmapFactory.decodeByteArray(binaryContent, 0, binaryContent.length);
				drawable = new BitmapDrawable(bitmap);
			}
		} catch (OutOfMemoryError oome) {
			System.gc();
			LogMe.w("Cleaned garbage");
		} catch (Exception e) {
			LogMe.e(e);
		}

		return drawable;
	}

	private static boolean isImageInMap(String key) {
		return imageCache.containsKey(key) && imageCache.get(key) != null && imageCache.get(key).get() != null;
	}

	private static void waitUntilOtherThreadDownloadsImage(String key) {
		while (downloadList.contains(key)) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static void stopDownloads() {
		clear();
	}

	public static void clear() {
		downloadList.clear();
		imageCache.clear();
	}
}
