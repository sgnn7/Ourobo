package org.sgnn7.ourobo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.sgnn7.ourobo.data.RedditPost;
import org.sgnn7.ourobo.data.UrlFileType;
import org.sgnn7.ourobo.util.AsyncThumbnailDownloader;
import org.sgnn7.ourobo.util.HttpUtils;
import org.sgnn7.ourobo.util.JsonUtils;
import org.sgnn7.ourobo.util.LogMe;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

public class MainActivity extends Activity {
	private static final String REDDIT_HOST = "reddit.com";

	private static final String HTTP_PROTOCOL_PREFIX = "http://";

	private static final String MAIN_SUBDOMAIN = "www.";
	private static final String API_SUBDOMAIN = "api.";
	private static final String MOBILE_SUBDOMAIN = "i.";

	private static final String MAIN_URL = HTTP_PROTOCOL_PREFIX + MAIN_SUBDOMAIN + REDDIT_HOST;
	private static final String API_URL = HTTP_PROTOCOL_PREFIX + API_SUBDOMAIN + REDDIT_HOST;
	private static final String MOBILE_URL = HTTP_PROTOCOL_PREFIX + MOBILE_SUBDOMAIN + REDDIT_HOST;

	private ProgressBar progressBar;
	private ImageView refreshButton;

	private final Set<AsyncThumbnailDownloader> runningDownloaders = new CopyOnWriteArraySet<AsyncThumbnailDownloader>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.main);

		progressBar = (ProgressBar) findViewById(R.id.loading_view);

		refreshButton = (ImageView) findViewById(R.id.main_page);
		refreshButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				stopAllDownloads();

				progressBar.setVisibility(View.VISIBLE);

				refreshButton.setImageDrawable(getResources().getDrawable(R.drawable.refresh_hilight));

				refreshButton.setEnabled(false);
				new DownloadTask().execute(API_URL);
			}
		});

		new DownloadTask().execute(API_URL);
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
				Toast.makeText(MainActivity.this, "Could not retrieve results from " + API_URL, Toast.LENGTH_LONG)
						.show();
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

		Drawable viewImageThumbnail = getResources().getDrawable(R.drawable.view_image);

		int index = 0;
		for (final RedditPost redditPost : results) {
			LogMe.d("Adding " + redditPost.getTitle());
			View postHolder = layoutInflater.inflate(R.layout.post_layout, mainView, false);

			TextView textView = (TextView) postHolder.findViewById(R.id.post_title);
			String title = sanitizeString(redditPost.getTitle().trim());
			textView.setText(title);

			final UrlFileType fileType = HttpUtils.getFileType(redditPost.getUrl());

			postHolder.setBackgroundDrawable(getResources().getDrawable(
					getBackgroundIdBasedOnTypeAndIndex(fileType, index++)));
			postHolder.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					stopAllDownloads();
					startActivity(getIntentBasedOnFileType(redditPost, fileType));
				}
			});

			ViewSwitcher thumbnailHolder = (ViewSwitcher) postHolder.findViewById(R.id.post_thumbnail_holder);

			boolean isImageUrl = fileType.equals(UrlFileType.IMAGE);
			if (isImageUrl) {
				AsyncThumbnailDownloader downloader = new AsyncThumbnailDownloader(API_URL, thumbnailHolder,
						runningDownloaders, viewImageThumbnail, redditPost.getThumbnail(), redditPost.getUrl()
								.toExternalForm());
				runningDownloaders.add(downloader);
				downloader.execute();
			} else {
				AsyncThumbnailDownloader downloader = new AsyncThumbnailDownloader(API_URL, thumbnailHolder,
						runningDownloaders, null, redditPost.getThumbnail(), null);
				runningDownloaders.add(downloader);
				downloader.execute();
			}

			RelativeLayout scoreHolder = (RelativeLayout) postHolder.findViewById(R.id.post_score_holder);
			scoreHolder.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					stopAllDownloads();

					String commentsUrl = MOBILE_URL + redditPost.getPermalink();
					LogMe.e("Opening comments at: " + commentsUrl);

					Intent targetIntent = new Intent(MainActivity.this, BrowserViewActivity.class);
					targetIntent.putExtra(BrowserViewActivity.LOCATION, commentsUrl);
					startActivity(targetIntent);
				}
			});

			TextView scoreView = (TextView) scoreHolder.findViewById(R.id.post_score);
			scoreView.setText("" + redditPost.getScore());

			mainView.addView(postHolder);
		}
	}

	private Intent getIntentBasedOnFileType(final RedditPost redditPost, final UrlFileType fileType) {
		Intent targetIntent = new Intent(MainActivity.this, BrowserViewActivity.class);
		targetIntent.putExtra(BrowserViewActivity.LOCATION, forceMobileSiteUrl(redditPost.getUrl().toExternalForm()));
		return targetIntent;
	}

	private String forceMobileSiteUrl(String url) {
		return url.replace(MAIN_URL, MOBILE_URL);
	}

	private int getBackgroundIdBasedOnTypeAndIndex(UrlFileType fileType, int index) {
		int backdgoundId = R.drawable.gray_post_style;
		if (index % 2 != 0) {
			backdgoundId = R.drawable.blue_post_style;
		}
		return backdgoundId;
	}

	private void stopAllDownloads() {
		for (AsyncThumbnailDownloader downloader : runningDownloaders) {
			downloader.cancel(true);
		}
	}

	private String sanitizeString(String text) {
		String sanitizedText = "NULL";
		if (text != null && text.length() != 0) {
			sanitizedText = text;
		}
		return sanitizedText;
	}
}