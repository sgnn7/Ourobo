package org.sgnn7.ourobo.data;

import java.util.ArrayList;
import java.util.List;

import org.sgnn7.ourobo.eventing.ISubredditChangedListener;
import org.sgnn7.ourobo.util.LogMe;

import android.app.Activity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class SubredditController {
	private static final String REDDITS_URL = "/reddits/.json";
	private static final String SUBREDDIT_PREFIX = "/r/";
	private static final String DEFAULT_SUBREDDIT = "Main";

	private final Spinner subredditSpinnerView;
	private final String dataSourceUrl;
	private final Activity activity;

	public SubredditController(Activity activity, String dataSourceUrl, Spinner subredditSpinnerView) {
		this.activity = activity;
		this.dataSourceUrl = dataSourceUrl;
		this.subredditSpinnerView = subredditSpinnerView;
	}

	public void loadSubreddits(final ISubredditChangedListener subredditChangedListener) {
		final List<String> subredditList = new ArrayList<String>();
		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item,
				subredditList);

		DownloadTask downloadTask = new DownloadTask() {
			@Override
			protected void onPostExecute(List<RedditPost> results) {
				LogMe.e("Downloaded " + results.size() + " subreddit categories");
				// subredditSpinnerView.
				subredditList.add(DEFAULT_SUBREDDIT);
				for (RedditPost redditPost : results) {
					String subredditUrl = redditPost.getUrl().toString();
					if (subredditUrl.startsWith(SUBREDDIT_PREFIX)) {
						subredditUrl = subredditUrl.substring(3);
					}
					subredditList.add(subredditUrl);
				}

				subredditSpinnerView.setOnItemSelectedListener(new OnItemSelectedListener() {
					public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
						String subredditName = subredditList.get(position);
						if (subredditName.equals(DEFAULT_SUBREDDIT)) {
							subredditName = "";
						} else {
							subredditName = SUBREDDIT_PREFIX + subredditName;
						}

						LogMe.e("Subreddit selected: " + subredditName);
						subredditChangedListener.subredditChanged(subredditName);
					}

					public void onNothingSelected(AdapterView<?> parent) {
						subredditSpinnerView.setSelection(0);
					}
				});

				subredditSpinnerView.setAdapter(adapter);
				subredditSpinnerView.setSelection(0);
			}
		};

		String subreditUrl = dataSourceUrl + REDDITS_URL;
		LogMe.e("Subreddits from: " + subreditUrl);
		downloadTask.execute(subreditUrl);
	}
}
