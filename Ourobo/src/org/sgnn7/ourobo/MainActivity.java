package org.sgnn7.ourobo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.sgnn7.ourobo.data.RedditPost;
import org.sgnn7.ourobo.data.UrlFileType;
import org.sgnn7.ourobo.eventing.IChangeEventListener;
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
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

public class MainActivity extends Activity {

	private static final String REDDIT_HOST = "reddit.com";

	private static final String PARAMETER_SEPARATOR = "&";

	private static final int DEFAULT_POST_COUNT = 12;
	private static final String DEFAULT_SORTING_TYPE = "new";

	private static final String HTTP_PROTOCOL_PREFIX = "http://";

	private static final String MAIN_SUBDOMAIN = "www.";
	private static final String JSON_SUBDOMAIN = "json.";
	private static final String MOBILE_SUBDOMAIN = "i.";
	private static final String JSON_PATH_SUFFIX = "/.json";

	private static final String MAIN_URL = HTTP_PROTOCOL_PREFIX + MAIN_SUBDOMAIN + REDDIT_HOST;
	private static final String JSON_URL = HTTP_PROTOCOL_PREFIX + JSON_SUBDOMAIN + REDDIT_HOST + JSON_PATH_SUFFIX;
	private static final String MOBILE_URL = HTTP_PROTOCOL_PREFIX + MOBILE_SUBDOMAIN + REDDIT_HOST;

	private ProgressBar progressBar;
	private ImageView refreshButton;

	private RedditPostAdapter redditPostAdapter;
	private LazyLoadingListener lazyLoadingListener;

	private final Set<AsyncThumbnailDownloader> runningDownloaders = new CopyOnWriteArraySet<AsyncThumbnailDownloader>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.main);

		progressBar = (ProgressBar) findViewById(R.id.loading_view);

		ListView postView = (ListView) findViewById(R.id.posts_list);

		redditPostAdapter = new RedditPostAdapter();
		postView.setAdapter(redditPostAdapter);

		lazyLoadingListener = new LazyLoadingListener(5);
		lazyLoadingListener.addLazyLoaderEventListener(new IChangeEventListener() {
			public void handle() {
				LogMe.i("Lazy loading more content...");
				new DownloadTask().execute(JSON_URL, getParameterString(redditPostAdapter));
			}
		});
		postView.setOnScrollListener(lazyLoadingListener);

		refreshButton = (ImageView) findViewById(R.id.main_page);
		refreshButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				stopAllDownloads();

				progressBar.setVisibility(View.VISIBLE);

				refreshButton.setImageDrawable(getResources().getDrawable(R.drawable.refresh_hilight));

				refreshButton.setEnabled(false);

				redditPostAdapter.refreshViews();
			}
		});

		redditPostAdapter.refreshViews();
	}

	private class DownloadTask extends AsyncTask<String, Void, List<RedditPost>> {
		@Override
		protected List<RedditPost> doInBackground(String... params) {
			List<RedditPost> posts = new ArrayList<RedditPost>();
			try {
				String getParameters = params.length < 2 ? "" : params[1];

				String pageContent = HttpUtils.getPageContent(params[0] + getParameters);
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
				Toast.makeText(MainActivity.this, "Could not retrieve results from " + JSON_URL, Toast.LENGTH_LONG)
						.show();
			}

			progressBar.setVisibility(View.INVISIBLE);
			refreshButton.setImageDrawable(getResources().getDrawable(R.drawable.refresh));
			refreshButton.setEnabled(true);

			lazyLoadingListener.contentLoaded();
		}

	}

	private void addPostsToMainPage(List<RedditPost> results) {
		LayoutInflater layoutInflater = getLayoutInflater();
		ListView mainView = (ListView) findViewById(R.id.posts_list);

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
				AsyncThumbnailDownloader downloader = new AsyncThumbnailDownloader(MAIN_URL, thumbnailHolder,
						runningDownloaders, viewImageThumbnail, redditPost.getThumbnail(), redditPost.getUrl()
								.toExternalForm());
				runningDownloaders.add(downloader);
				downloader.execute();
			} else {
				AsyncThumbnailDownloader downloader = new AsyncThumbnailDownloader(MAIN_URL, thumbnailHolder,
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

			redditPostAdapter.addView(postHolder, redditPost.getName());
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

	public class RedditPostAdapter extends BaseAdapter {
		List<String> redditPostIds = new ArrayList<String>();
		Map<String, View> postIdToViewMap = new HashMap<String, View>();

		public int getCount() {
			return redditPostIds.size();
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			return postIdToViewMap.get(redditPostIds.get(position));
		}

		public void addView(View view, String redditPostId) {
			redditPostIds.add(redditPostId);
			postIdToViewMap.put(redditPostId, view);
		}

		public void refreshViews() {
			redditPostIds.clear();
			postIdToViewMap.clear();

			new DownloadTask().execute(JSON_URL, getParameterString(this));
		}

		public String getLastPostId() {
			return redditPostIds.get(redditPostIds.size() - 1);
		}
	}

	private String getParameterString(RedditPostAdapter adapter) {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("limit", "" + DEFAULT_POST_COUNT);
		parameters.put("sort", DEFAULT_SORTING_TYPE);

		if (adapter.getCount() != 0) {
			parameters.put("after", adapter.getLastPostId());
		}

		String parameterString = "";
		for (String parameterKey : parameters.keySet()) {
			parameterString += PARAMETER_SEPARATOR + parameterKey + "=" + parameters.get(parameterKey);
		}
		parameterString = "?" + parameterString.substring(1);

		LogMe.e("Parameters: " + parameterString);

		return parameterString;
	}
}