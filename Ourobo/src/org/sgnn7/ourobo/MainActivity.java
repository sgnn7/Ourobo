package org.sgnn7.ourobo;

import java.util.List;

import org.sgnn7.ourobo.data.DownloadTaskFactory;
import org.sgnn7.ourobo.data.RedditPost;
import org.sgnn7.ourobo.data.RedditPostAdapter;
import org.sgnn7.ourobo.eventing.IChangeEventListener;
import org.sgnn7.ourobo.eventing.LazyLoadingListener;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final String REDDIT_HOST = "reddit.com";

	private static final String HTTP_PROTOCOL_PREFIX = "http://";

	private static final String MAIN_SUBDOMAIN = "www.";
	private static final String JSON_SUBDOMAIN = "json.";
	private static final String MOBILE_SUBDOMAIN = "i.";
	private static final String JSON_PATH_SUFFIX = "/.json";

	// private static final String MAIN_URL = HTTP_PROTOCOL_PREFIX + MAIN_SUBDOMAIN + REDDIT_HOST;
	// private static final String JSON_URL = HTTP_PROTOCOL_PREFIX + JSON_SUBDOMAIN + REDDIT_HOST + JSON_PATH_SUFFIX;
	// private static final String MOBILE_URL = HTTP_PROTOCOL_PREFIX + MOBILE_SUBDOMAIN + REDDIT_HOST;

	private static final String DEBUG_SERVER = "192.168.3.70:8080";
	private static final String MAIN_URL = HTTP_PROTOCOL_PREFIX + DEBUG_SERVER + "/RedditService/RedditService";
	private static final String JSON_URL = HTTP_PROTOCOL_PREFIX + DEBUG_SERVER + "/RedditService/RedditService";
	private static final String MOBILE_URL = HTTP_PROTOCOL_PREFIX + DEBUG_SERVER + "/RedditService/RedditService";

	private ProgressBar progressBar;
	private ListView postView;

	private RedditPostAdapter redditPostAdapter;
	private LazyLoadingListener lazyLoadingListener;

	private DownloadTaskFactory downloadTaskFactory;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);

		downloadTaskFactory = new DownloadTaskFactory() {
			@Override
			protected void onPostExecuteDownloadTask(List<RedditPost> results) {
				if (!results.isEmpty()) {
					redditPostAdapter.addPosts(results);
				} else {
					Toast.makeText(MainActivity.this, "Could not retrieve json data", Toast.LENGTH_LONG).show();
				}

				progressBar.setVisibility(View.INVISIBLE);
				lazyLoadingListener.contentLoaded();
			}
		};

		progressBar = (ProgressBar) findViewById(R.id.loading_view);

		attachListAdapterToListView("");

		lazyLoadingListener = new LazyLoadingListener(5);
		lazyLoadingListener.addLazyLoaderEventListener(new IChangeEventListener() {
			public void handle() {
				redditPostAdapter.downloadMoreContent();
			}
		});
		postView.setOnScrollListener(lazyLoadingListener);

		redditPostAdapter.refreshViews();
	}

	private void attachListAdapterToListView(String subreddit) {
		postView = (ListView) findViewById(R.id.posts_list);
		redditPostAdapter = new RedditPostAdapter(this, downloadTaskFactory, JSON_URL, MAIN_URL, MOBILE_URL);
		postView.setAdapter(redditPostAdapter);
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
			redditPostAdapter.stopAllDownloads();
			return true;
		case R.id.menu_switch_subreddit:
			Toast.makeText(this, "Not implemented yet", Toast.LENGTH_LONG).show();
			// attachListAdapterToListView("xyz");
		case R.id.menu_preferences:
			Intent preferenceActivity = new Intent(getBaseContext(), AppPreferenceActivity.class);
			startActivity(preferenceActivity);
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
