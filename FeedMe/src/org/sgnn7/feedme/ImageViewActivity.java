package org.sgnn7.feedme;

import org.sgnn7.feedme.util.HttpUtils;
import org.sgnn7.feedme.util.LogMe;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

public class ImageViewActivity extends Activity {
	public static final String IMAGE_LOCATION = "image.location";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.image_view);

		ImageView imageLoader = (ImageView) findViewById(R.id.image_loader);

		String imageLocation = getIntent().getStringExtra(IMAGE_LOCATION);
		LogMe.d("Image Location: " + imageLocation);
		loadImage(imageLoader, imageLocation);
	}

	private void loadImage(ImageView view, String uri) {
		byte[] imageContent = HttpUtils.getBinaryPageContent(uri);
		Bitmap bitmap = BitmapFactory.decodeByteArray(imageContent, 0, imageContent.length);

		view.setImageBitmap(bitmap);
	}
}
