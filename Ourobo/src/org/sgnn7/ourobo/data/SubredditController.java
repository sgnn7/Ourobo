package org.sgnn7.ourobo.data;

import java.util.ArrayList;
import java.util.List;

import org.sgnn7.ourobo.authentication.SessionManager;
import org.sgnn7.ourobo.eventing.ISubredditChangedListener;
import org.sgnn7.ourobo.util.LogMe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;

public class SubredditController {
	private static final int SPINNER_DISPLAY_LAYOUT = android.R.layout.select_dialog_item;

	private static final String REDDITS_URL = "/reddits/.json";
	private static final String SUBREDDIT_PREFIX = "/r/";
	private static final String DEFAULT_SUBREDDIT = "home/";

	private final SessionManager sessionManager;

	private final Spinner subredditSpinnerView;
	private final String dataSourceUrl;
	private final Activity activity;
	private final List<String> subredditList = new ArrayList<String>();

	private final ProgressBar progressBar;

	public SubredditController(Activity activity, SessionManager sessionManager, String dataSourceUrl,
			Spinner subredditSpinnerView, ProgressBar progressBar) {
		this.activity = activity;
		this.sessionManager = sessionManager;
		this.dataSourceUrl = dataSourceUrl;
		this.subredditSpinnerView = subredditSpinnerView;
		this.progressBar = progressBar;

		subredditList.add(DEFAULT_SUBREDDIT);
		subredditSpinnerView.setAdapter(createNewSubredditAdapter());
	}

	public void reloadSubreddits(final ISubredditChangedListener subredditChangedListener) {
		DownloadTask downloadTask = new DownloadTask(sessionManager) {
			@Override
			protected void onDownloadComplete(List<RedditPost> results) {
				LogMe.e("Downloaded " + results.size() + " subreddit categories");

				subredditList.clear();
				subredditList.add(DEFAULT_SUBREDDIT);
				for (RedditPost redditPost : results) {
					String subredditUrl = redditPost.getUrl().toString();
					if (subredditUrl.startsWith(SUBREDDIT_PREFIX)) {
						subredditUrl = subredditUrl.substring(3);
					}
					subredditList.add(subredditUrl);
				}

				progressBar.setVisibility(View.INVISIBLE);

				subredditSpinnerView.setOnItemSelectedListener(new OnItemSelectedListener() {
					public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
						String subredditName = subredditList.get(position);
						changeSubreddit(subredditChangedListener, subredditName);
					}

					public void onNothingSelected(AdapterView<?> parent) {
						subredditSpinnerView.setSelection(0);
					}
				});

				subredditSpinnerView.setOnLongClickListener(new OnLongClickListener() {
					public boolean onLongClick(View view) {
						final EditText editText = new EditText(SubredditController.this.activity);

						new AlertDialog.Builder(SubredditController.this.activity)
								//
								.setTitle("Enter subreddit").setView(editText)
								.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
										String newSubreddit = editText.getText().toString();
										addNewSubreddit(subredditChangedListener, newSubreddit);
									}

								}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
										// Do nothing.
									}
								}).show();

						return true;
					}
				});

				subredditSpinnerView.setAdapter(createNewSubredditAdapter());
				subredditSpinnerView.setSelection(0);
			}
		};

		String subredditUrl = dataSourceUrl + REDDITS_URL;
		LogMe.e("Subreddits from: " + subredditUrl);
		downloadTask.execute(subredditUrl);
	}

	private void addNewSubreddit(final ISubredditChangedListener subredditChangedListener, String newSubreddit) {
		newSubreddit = newSubreddit.trim().toLowerCase();
		if (newSubreddit.length() > 0) {
			if (!newSubreddit.endsWith("/")) {
				newSubreddit += "/";
			}

			changeSubreddit(subredditChangedListener, newSubreddit);
			subredditList.add(1, newSubreddit);
			subredditSpinnerView.setAdapter(createNewSubredditAdapter());
			subredditSpinnerView.setSelection(1, true);
		}
	}

	private void changeSubreddit(final ISubredditChangedListener subredditChangedListener, String subredditName) {
		if (subredditName.equals(DEFAULT_SUBREDDIT)) {
			subredditName = "";
		} else {
			subredditName = SUBREDDIT_PREFIX + subredditName;
		}

		LogMe.e("Subreddit selected: " + subredditName);
		subredditChangedListener.subredditChanged(subredditName);
	}

	private ArrayAdapter<String> createNewSubredditAdapter() {
		return new ArrayAdapter<String>(activity, SPINNER_DISPLAY_LAYOUT, subredditList);
	}
}
