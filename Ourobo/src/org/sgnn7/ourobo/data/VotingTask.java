package org.sgnn7.ourobo.data;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.StringUtils;
import org.sgnn7.ourobo.authentication.SessionManager;
import org.sgnn7.ourobo.util.HttpUtils;
import org.sgnn7.ourobo.util.LogMe;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class VotingTask {
	private static final String GOOD_RESPONSE = "{}";
	private static final ExecutorService executor = Executors.newCachedThreadPool();
	private static final Handler mainHandler = new Handler(Looper.getMainLooper());

	private final String baseUrl;
	private final RedditPost redditPost;
	private final SessionManager sessionManager;
	private final Context context;
	private final boolean isUpvote;
	private final VoteResultListener listener;

	public interface VoteResultListener {
		void onVoteSuccess(boolean isUpvote);
	}

	public VotingTask(Context context, SessionManager sessionManager, String baseUrl, RedditPost redditPost,
			boolean isUpvote, VoteResultListener listener) {
		this.context = context;
		this.sessionManager = sessionManager;
		this.baseUrl = baseUrl;
		this.redditPost = redditPost;
		this.isUpvote = isUpvote;
		this.listener = listener;
	}

	public void execute() {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				final boolean votingSuccess = doInBackground();
				mainHandler.post(new Runnable() {
					@Override
					public void run() {
						onPostExecute(votingSuccess);
					}
				});
			}
		});
	}

	private boolean doInBackground() {
		try {
			boolean isAuthenticated = sessionManager.authenticateUser();
			if (!isAuthenticated) {
				return false;
			}

			Map<String, String> parameterMap = new HashMap<String, String>();
			parameterMap.put("id", redditPost.getName());
			parameterMap.put("dir", isUpvote ? "1" : "-1");

			String votingResult = HttpUtils.doPost(sessionManager, baseUrl, "api/vote/", parameterMap);
			LogMe.e("Voting result: " + votingResult);

			return StringUtils.equals(GOOD_RESPONSE, votingResult);
		} catch (Exception e) {
			LogMe.e("Voting failed: " + e.getMessage());
			return false;
		}
	}

	private void onPostExecute(boolean votingSuccessful) {
		if (votingSuccessful) {
			if (listener != null) {
				listener.onVoteSuccess(isUpvote);
			}
		} else {
			Toast.makeText(context, "Voting failed", Toast.LENGTH_SHORT).show();
		}
	}
}
