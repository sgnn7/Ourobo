package org.sgnn7.ourobo.util;

import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import androidx.core.content.ContextCompat;

import org.sgnn7.ourobo.R;
import org.sgnn7.ourobo.eventing.IImageLoadedListener;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class ImageCacheManager {
	private static final int EXECUTOR_THREAD_COUNT = 20;

	private static final long DOWNLOAD_WAIT_TIMEOUT_SECONDS = 30;
	private final static String MSFW_THUMBNAIL_ID = "nsfw";

	private static Map<String, SoftReference<Drawable>> imageCache = new ConcurrentHashMap<String, SoftReference<Drawable>>();
	private static Map<String, CountDownLatch> downloadLatches = new ConcurrentHashMap<String, CountDownLatch>();

	private static ExecutorService executorService = null;

	static {
		createExecutorPool();
	}

	public static void getImage(final Context context, final String host, final String imageUrl,
			final IImageLoadedListener callback) {
		if (isImageInMap(imageUrl)) {
			LogMe.d("Cache hit on key: " + imageUrl);

			callback.finishedLoading(getDrawableFromCache(imageUrl));
		} else if (imageUrl.equals(MSFW_THUMBNAIL_ID)) {
			callback.finishedLoading(ContextCompat.getDrawable(context, R.drawable.nswf));
		} else {
			LogMe.d("Cache miss on key: " + imageUrl);

			executorService.execute(new Runnable() {
				public void run() {
					callback.finishedLoading(getImageSynced(context, host, imageUrl));
				}
			});
		}
	}

	private static Drawable getImageSynced(Context context, String host, String imageUrl) {
		CountDownLatch existingLatch = downloadLatches.get(imageUrl);
		if (existingLatch != null) {
			try {
				existingLatch.await(DOWNLOAD_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				LogMe.d("Interrupted waiting on image " + imageUrl);
			}
			return getDrawableFromCache(imageUrl);
		}

		if (!isImageInMap(imageUrl)) {
			CountDownLatch latch = new CountDownLatch(1);
			CountDownLatch previous = downloadLatches.putIfAbsent(imageUrl, latch);
			if (previous != null) {
				try {
					previous.await(DOWNLOAD_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					LogMe.d("Interrupted waiting on image " + imageUrl);
				}
			} else {
				try {
					Drawable image = downloadImage(context, host, imageUrl);
					if (image != null) {
						imageCache.put(imageUrl, new SoftReference<Drawable>(image));
					}
				} finally {
					latch.countDown();
					downloadLatches.remove(imageUrl);
				}
			}
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

	private static Drawable downloadImage(final Context context, final String host, final String imageUrl) {
		BitmapDrawable drawable = null;

		try {
			byte[] binaryContent = HttpUtils.getBinaryPageContent(null, host, imageUrl);
			if (binaryContent != null) {
				Bitmap bitmap = BitmapFactory.decodeByteArray(binaryContent, 0, binaryContent.length);
				if (bitmap != null) {
					drawable = new BitmapDrawable(context.getResources(), bitmap);
				}
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

	public static void clear() {
		stopDownloads();
		imageCache.clear();
	}

	public static void stopDownloads() {
		for (CountDownLatch latch : downloadLatches.values()) {
			latch.countDown();
		}
		downloadLatches.clear();
		createExecutorPool();
	}

	private static void createExecutorPool() {
		if (executorService != null) {
			executorService.shutdownNow();
		}

		executorService = Executors.newFixedThreadPool(EXECUTOR_THREAD_COUNT);
	}
}
