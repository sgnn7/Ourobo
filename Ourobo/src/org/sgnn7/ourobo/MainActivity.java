package org.sgnn7.ourobo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.sgnn7.ourobo.data.RedditPost;
import org.sgnn7.ourobo.data.UrlFileType;
import org.sgnn7.ourobo.util.HttpUtils;
import org.sgnn7.ourobo.util.JsonUtils;
import org.sgnn7.ourobo.util.LogMe;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private static final String REDDIT_API_URI = "http://api.reddit.com";

	private ProgressBar progressBar;
	private ImageView refreshButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.main);

		progressBar = (ProgressBar) findViewById(R.id.loading_view);

		refreshButton = (ImageView) findViewById(R.id.main_page);
		refreshButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				progressBar.setVisibility(View.VISIBLE);

				refreshButton.setImageDrawable(getResources().getDrawable(R.drawable.refresh_hilight));

				refreshButton.setEnabled(false);
				new DownloadTask().execute(REDDIT_API_URI);
			}
		});

		new DownloadTask().execute(REDDIT_API_URI);
	}

	private class DownloadTask extends AsyncTask<String, Void, List<RedditPost>> {
		@Override
		protected List<RedditPost> doInBackground(String... params) {
			List<RedditPost> posts = new ArrayList<RedditPost>();
			try {
				String pageContent = HttpUtils.getPageContent(params[0]);
				posts = getRedditPostsFromContent(pageContent);
				LogMe.e("Posts: " + posts.size());
			} catch (Exception e) {
				LogMe.e(e.getClass().getName() + ": " + e.getMessage());
				e.printStackTrace();
			}
			return posts;
		}

		private List<RedditPost> getRedditPostsFromContent(String pageContent) throws IOException, JsonParseException,
				JsonMappingException {
			JsonNode topNode = new ObjectMapper().readValue(pageContent, JsonNode.class);
			List<JsonNode> posts = JsonUtils.getJsonChildren(topNode, "data/children");
			LogMe.d("Json Size: " + posts.size());
			List<RedditPost> usablePosts = JsonUtils.convertJsonPostNodesToJavaBeans(posts);
			return usablePosts;
		}

		@Override
		protected void onPostExecute(List<RedditPost> results) {
			if (!results.isEmpty()) {
				addPostsToMainPage(results);
			} else {
				Toast.makeText(MainActivity.this, "Could not retrieve results from " + REDDIT_API_URI,
						Toast.LENGTH_LONG).show();
			}

			progressBar.setVisibility(View.INVISIBLE);
			refreshButton.setImageDrawable(getResources().getDrawable(R.drawable.refresh));
			refreshButton.setEnabled(true);
		}

	}

	private void addPostsToMainPage(List<RedditPost> results) {
		LayoutInflater layoutInflater = getLayoutInflater();
		LinearLayout mainView = (LinearLayout) findViewById(R.id.posts_list);
		mainView.removeAllViews();

		int index = 0;
		for (final RedditPost redditPost : results) {
			LogMe.d("Adding " + redditPost.getTitle());
			View postHolder = layoutInflater.inflate(R.layout.post_layout, mainView, false);

			TextView textView = (TextView) postHolder.findViewById(R.id.post_title);
			textView.setText(redditPost.getTitle());

			final UrlFileType fileType = HttpUtils.getFileType(redditPost.getUrl());

			postHolder.setBackgroundDrawable(getResources().getDrawable(
					getBackgroundIdBasedOnTypeAndIndex(fileType, index++)));
			postHolder.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					startActivity(getIntentBasedOnFileType(redditPost, fileType));
				}
			});

			ImageView thumbnail = (ImageView) postHolder.findViewById(R.id.post_thumbnail);
			if (fileType.equals(UrlFileType.IMAGE)) {
				thumbnail.setImageDrawable(getResources().getDrawable(R.drawable.view_image));
			} else {
				thumbnail.setImageDrawable(getResources().getDrawable(R.drawable.browse));
			}

			mainView.addView(postHolder);
		}

	}

	private Intent getIntentBasedOnFileType(final RedditPost redditPost, final UrlFileType fileType) {
		Intent targetIntent = null;
		switch (fileType) {
		case IMAGE:
			targetIntent = new Intent(MainActivity.this, ImageViewActivity.class);
			targetIntent.putExtra(ImageViewActivity.IMAGE_LOCATION, redditPost.getUrl().toExternalForm());
			break;
		default:
			targetIntent = new Intent("android.intent.action.VIEW", Uri.parse(redditPost.getUrl().toExternalForm()));
		}
		return targetIntent;
	}

	private int getBackgroundIdBasedOnTypeAndIndex(UrlFileType fileType, int index) {
		int backdgoundId = R.drawable.gray_post_style;
		// if (fileType.equals(UrlFileType.IMAGE)) {
		// backdgoundId = R.drawable.picture_post_style;
		// } else if (index % 2 != 0) {
		if (index % 2 != 0) {
			backdgoundId = R.drawable.blue_post_style;
		}
		return backdgoundId;
	}
}