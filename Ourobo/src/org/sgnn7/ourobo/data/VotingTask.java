package org.sgnn7.ourobo.data;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.sgnn7.ourobo.authentication.SessionManager;
import org.sgnn7.ourobo.util.HttpUtils;
import org.sgnn7.ourobo.util.LogMe;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

public class VotingTask extends AsyncTask<String, Void, Boolean> {
	private static final String GOOD_RESPONSE = "{}";

	private final String baseUrl;
	private final RedditPost redditPost;
	private final SessionManager sessionManager;
	private final Context context;

	private final boolean isUpvote;

	public VotingTask(Context context, SessionManager sessionManager, String baseUrl, RedditPost redditPost,
			boolean isUpvote) {
		this.context = context;
		this.sessionManager = sessionManager;
		this.baseUrl = baseUrl;
		this.redditPost = redditPost;
		this.isUpvote = isUpvote;
	}

	@Override
	protected Boolean doInBackground(String... url) {
		boolean votingSuccess = false;

		Map<String, String> parameterMap = new HashMap<String, String>();
		parameterMap.put("id", redditPost.getName());
		parameterMap.put("dir", isUpvote ? "1" : "-1");
		// parameterMap.put("uh", modhash);

		String votingResult = HttpUtils.doPost(sessionManager, baseUrl, "api/login/", parameterMap);
		LogMe.e("Voting result: " + votingResult);

		if (StringUtils.equals(GOOD_RESPONSE, votingResult)) {
			votingSuccess = true;
		}

		return votingSuccess;
	}

	@Override
	protected void onPostExecute(Boolean votingSuccessful) {
		if (!votingSuccessful) {
			Toast.makeText(context, "Voting failed", Toast.LENGTH_SHORT);
		}
	}
}
