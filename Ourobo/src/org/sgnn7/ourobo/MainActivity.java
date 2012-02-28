package org.sgnn7.ourobo;

import org.sgnn7.ourobo.authentication.SessionManager;
import org.sgnn7.ourobo.data.RedditPostAdapter;
import org.sgnn7.ourobo.data.SubredditController;
import org.sgnn7.ourobo.eventing.IChangeEventListener;
import org.sgnn7.ourobo.eventing.ISubredditChangedListener;
import org.sgnn7.ourobo.eventing.LazyLoadingListener;
import org.sgnn7.ourobo.util.ImageCacheManager;
import org.sgnn7.ourobo.util.LogMe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;

public class MainActivity extends Activity {
	protected static final String HTTP_PROTOCOL_PREFIX = "http://";

	private static final String REDDIT_HOST = "reddit.com";

	private static final String MAIN_SUBDOMAIN = "www.";
	private static final String JSON_SUBDOMAIN = "www.";
	private static final String MOBILE_SUBDOMAIN = "i.";
	private static final String JSON_PATH_SUFFIX = "/.json";

	private static final String MAIN_URL = HTTP_PROTOCOL_PREFIX + MAIN_SUBDOMAIN + REDDIT_HOST;
	private static final String JSON_URL = HTTP_PROTOCOL_PREFIX + JSON_SUBDOMAIN + REDDIT_HOST;
	private static final String MOBILE_URL = HTTP_PROTOCOL_PREFIX + MOBILE_SUBDOMAIN + REDDIT_HOST;

	private ListView postView;
	private SubredditController subredditController;
	private RedditPostAdapter redditPostAdapter;

	private String currentSubreddit = null;

	private ISubredditChangedListener subredditChangedListener;
	private IChangeEventListener finishedLoadingListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.main);

		final SessionManager sessionManager = new SessionManager(this, MAIN_URL, new PreferencesManager(this));

		// TODO: make non-blocking
		// sessionManager.authenticateUser();

		attachListAdapterToListView(sessionManager, "");

		subredditController = new SubredditController(this, sessionManager, getJsonUrl(),
				(Spinner) findViewById(R.id.subreddit_spinner), (ProgressBar) findViewById(R.id.subreddit_progressbar));

		subredditChangedListener = new ISubredditChangedListener() {
			public void subredditChanged(String newSubreddit) {
				attachListAdapterToListView(sessionManager, newSubreddit);
			}
		};
		subredditController.reloadSubreddits(subredditChangedListener);
	}

	private void attachListAdapterToListView(SessionManager sessionManager, String newSubreddit) {
		if (!newSubreddit.equalsIgnoreCase(currentSubreddit)) {
			currentSubreddit = newSubreddit;

			collectGarbage();

			postView = (ListView) findViewById(R.id.posts_list);
			final LazyLoadingListener lazyLoadingListener = new LazyLoadingListener(5);

			finishedLoadingListener = new IChangeEventListener() {
				public void handle() {
					lazyLoadingListener.contentLoaded();
				}
			};

			String sanitizedSubreddit = newSubreddit.endsWith("/") ? newSubreddit.substring(0,
					newSubreddit.length() - 1) : newSubreddit;
			LogMe.e("Data location: " + getJsonUrl() + sanitizedSubreddit + JSON_PATH_SUFFIX);
			LogMe.e("Mobile Site location: " + getMobileUrl() + sanitizedSubreddit);

			redditPostAdapter = new RedditPostAdapter(this, sessionManager, getMainUrl(), getJsonUrl()
					+ sanitizedSubreddit + JSON_PATH_SUFFIX, getMobileUrl(), finishedLoadingListener);

			lazyLoadingListener.addLazyLoaderEventListener(new IChangeEventListener() {
				public void handle() {
					if (redditPostAdapter != null) {
						redditPostAdapter.downloadMoreContent();
					}
				}
			});

			postView.setAdapter(redditPostAdapter);
			postView.setOnScrollListener(lazyLoadingListener);

			redditPostAdapter.refreshViews();
		}
	}

	// XXX There must be a better way of doing something like this...
	private void collectGarbage() {
		redditPostAdapter = null;
		finishedLoadingListener = null;

		ImageCacheManager.clear();

		System.gc();
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
			if (redditPostAdapter != null) {
				redditPostAdapter.stopAllDownloads();
				redditPostAdapter.refreshViews();
			}

			if (subredditController != null) {
				subredditController.reloadSubreddits(subredditChangedListener);
			}
			return true;
		case R.id.menu_switch_subreddit:
			Spinner subredditSpinner = (Spinner) findViewById(R.id.subreddit_spinner);
			subredditSpinner.performClick();
			return true;
		case R.id.menu_preferences:
			startActivity(new Intent(this, AppPreferenceActivity.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onBackPressed() {
		AlertDialog alertDialog = new AlertDialog.Builder(this).setIcon(android.R.drawable.btn_dialog)
				.setTitle("Exit App").setMessage("Are you sure you want to exit the app?")
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						MainActivity.this.moveTaskToBack(true);
					}
				}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
					}
				}).create();

		alertDialog.show();
	}

	protected String getMainUrl() {
		return MAIN_URL;
	}

	protected String getJsonUrl() {
		return JSON_URL;
	}

	protected String getMobileUrl() {
		return MOBILE_URL;
	}
}
