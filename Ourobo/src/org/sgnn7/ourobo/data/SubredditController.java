package org.sgnn7.ourobo.data;

import java.util.ArrayList;
import java.util.List;

import org.sgnn7.ourobo.util.LogMe;

import android.app.Activity;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class SubredditController {
	private static final String REDDITS_URL = "/reddits/.json";
	private final Spinner subredditSpinnerView;
	private final String dataSourceUrl;
	private final Activity activity;

	public SubredditController(Activity activity, String dataSourceUrl, Spinner subredditSpinnerView) {
		this.activity = activity;
		this.dataSourceUrl = dataSourceUrl;
		this.subredditSpinnerView = subredditSpinnerView;
	}

	public void loadSubreddits() {
		DownloadTask downloadTask = new DownloadTask() {

			@Override
			protected void onPostExecute(List<RedditPost> results) {
				LogMe.e("Downloaded " + results.size() + " subreddit categories");
				// subredditSpinnerView.
				List<String> subredditList = new ArrayList<String>();
				for (RedditPost redditPost : results) {
					String subredditUrl = redditPost.getUrl().toString();
					if (subredditUrl.startsWith("/r/")) {
						subredditUrl = subredditUrl.substring(3);
					}
					subredditList.add(subredditUrl);
				}

				final ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity,
						android.R.layout.simple_spinner_item, subredditList);
				subredditSpinnerView.setAdapter(adapter);
			}
		};

		String subreditUrl = dataSourceUrl + REDDITS_URL;
		LogMe.e("Subreddits from: " + subreditUrl);
		downloadTask.execute(subreditUrl);
	}
}
