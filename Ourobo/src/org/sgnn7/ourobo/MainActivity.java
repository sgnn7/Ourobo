package org.sgnn7.ourobo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.sgnn7.ourobo.data.RedditPost;
import org.sgnn7.ourobo.data.UrlFileType;
import org.sgnn7.ourobo.eventing.IChangeEventListener;
import org.sgnn7.ourobo.util.HttpUtils;
import org.sgnn7.ourobo.util.ImageCacheManager;
import org.sgnn7.ourobo.util.JsonUtils;
import org.sgnn7.ourobo.util.LogMe;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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

	// private static final String DEBUG_SERVER = "192.168.3.70:8080";
	// private static final String MAIN_URL = HTTP_PROTOCOL_PREFIX + DEBUG_SERVER + "/RedditService/RedditService";
	// private static final String JSON_URL = HTTP_PROTOCOL_PREFIX + DEBUG_SERVER + "/RedditService/RedditService";
	// private static final String MOBILE_URL = HTTP_PROTOCOL_PREFIX + DEBUG_SERVER + "/RedditService/RedditService";

	private ProgressBar progressBar;
	private ListView postView;

	private RedditPostAdapter redditPostAdapter;
	private LazyLoadingListener lazyLoadingListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.main);

		progressBar = (ProgressBar) findViewById(R.id.loading_view);

		postView = (ListView) findViewById(R.id.posts_list);

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

		redditPostAdapter.refreshViews();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_refresh:
			redditPostAdapter.refreshViews();
			stopAllDownloads();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
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
				redditPostAdapter.addPosts(results);
			} else {
				Toast.makeText(MainActivity.this, "Could not retrieve results from " + JSON_URL, Toast.LENGTH_LONG)
						.show();
			}

			progressBar.setVisibility(View.INVISIBLE);
			lazyLoadingListener.contentLoaded();
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
		ImageCacheManager.stopDownloads();
	}

	private String sanitizeString(String text) {
		String sanitizedText = "NULL";
		if (text != null && text.length() != 0) {
			sanitizedText = text;
		}
		return sanitizedText;
	}

	public class RedditPostAdapter extends BaseAdapter {
		final List<RedditPost> redditPosts = new ArrayList<RedditPost>();

		public int getCount() {
			int numberOfPosts;
			numberOfPosts = redditPosts.size();
			return numberOfPosts;
		}

		public Object getItem(int position) {
			return redditPosts.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			RedditPost redditPost = redditPosts.get(position);

			View redditPostHolder = null;
			// if (convertView == null) {
			LogMe.e("Creating view: " + redditPost.getTitle());
			redditPostHolder = getLayoutInflater().inflate(R.layout.post_layout, parent, false);
			// } else {
			// redditPostHolder = convertView;
			// }

			setPostHolderValues(position, redditPost, redditPostHolder);
			redditPostHolder.invalidate();
			return redditPostHolder;
		}

		public void addPosts(final List<RedditPost> newRedditPosts) {
			redditPosts.addAll(newRedditPosts);
			LogMe.e("Posts set. Add Size: " + newRedditPosts.size() + ". Total: " + redditPosts.size());
			notifyDataSetChanged();
		}

		public void refreshViews() {
			LogMe.e("Clearing view");
			redditPosts.clear();

			new DownloadTask().execute(JSON_URL, getParameterString(this));
		}

		public String getLastPostId() {
			return redditPosts.get(redditPosts.size() - 1).getName();
		}
	}

	private void setPostHolderValues(int index, final RedditPost redditPost, View postHolder) {
		TextView titleView = (TextView) postHolder.findViewById(R.id.post_title);
		String title = sanitizeString(redditPost.getTitle().trim());
		titleView.setText(title);

		final UrlFileType fileType = HttpUtils.getFileType(redditPost.getUrl());

		postHolder.setBackgroundDrawable(getResources()
				.getDrawable(getBackgroundIdBasedOnTypeAndIndex(fileType, index)));
		postHolder.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				stopAllDownloads();
				startActivity(getIntentBasedOnFileType(redditPost, fileType));
			}
		});

		boolean isImageUrl = fileType.equals(UrlFileType.IMAGE);

		ViewSwitcher thumbnailHolder = (ViewSwitcher) postHolder.findViewById(R.id.post_thumbnail_holder);
		final ImageView thumbnail = (ImageView) thumbnailHolder.findViewById(R.id.post_thumbnail);

		AsyncThumbnailLoader thumbnailLazyLoader = new AsyncThumbnailLoader(this, postHolder, thumbnailHolder,
				thumbnail, REDDIT_HOST);
		if (isImageUrl) {
			thumbnailLazyLoader.loadImage(redditPost.getUrl().toExternalForm());
		} else {
			thumbnailLazyLoader.loadImage(redditPost.getThumbnail());
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

		LogMe.d("Parameters: " + parameterString);

		return parameterString;
	}
}
