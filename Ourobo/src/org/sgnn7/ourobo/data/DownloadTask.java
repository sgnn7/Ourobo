package org.sgnn7.ourobo.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.sgnn7.ourobo.authentication.SessionManager;
import org.sgnn7.ourobo.eventing.IChangeEventListener;
import org.sgnn7.ourobo.util.HttpUtils;
import org.sgnn7.ourobo.util.JsonUtils;
import org.sgnn7.ourobo.util.LogMe;

import android.os.Handler;
import android.os.Looper;

public abstract class DownloadTask {
	private static final ExecutorService executor = Executors.newCachedThreadPool();
	private static final Handler mainHandler = new Handler(Looper.getMainLooper());

	private final SessionManager sessionManager;

	private IChangeEventListener taskDoneListener;
	private String uri;
	private volatile boolean isTaskDone = false;
	private final AtomicBoolean isCancelled = new AtomicBoolean(false);

	public DownloadTask(SessionManager sessionManager) {
		this.sessionManager = sessionManager;
	}

	public void execute(String... params) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				final List<RedditPost> posts = doInBackground(params);
				mainHandler.post(new Runnable() {
					@Override
					public void run() {
						if (!isCancelled.get()) {
							onPostExecute(posts);
						} else {
							isTaskDone = true;
							if (taskDoneListener != null) {
								taskDoneListener.handle();
							}
						}
					}
				});
			}
		});
	}

	protected List<RedditPost> doInBackground(String... params) {
		List<RedditPost> posts = new ArrayList<RedditPost>();
		try {
			String getParameters = params.length < 2 ? "" : params[1];
			uri = params[0] + getParameters;

			String pageContent = HttpUtils.getPageContent(sessionManager, uri);
			LogMe.d("Content: " + pageContent);
			posts = getRedditPostsFromContent(pageContent);
			LogMe.e("Posts: " + posts.size());
		} catch (Exception e) {
			LogMe.e("Failed to retrieve uri: " + uri);
			LogMe.e(e);
		}
		return posts;
	}

	private List<RedditPost> getRedditPostsFromContent(String pageContent) throws IOException {
		if (pageContent == null) {
			throw new IOException("Json was empty. Will not try to deserialize");
		}

		LogMe.d("Json: " + pageContent);
		JsonNode topNode = new ObjectMapper().readValue(pageContent, JsonNode.class);
		List<JsonNode> posts = JsonUtils.getJsonChildren(topNode, "data/children");
		LogMe.d("Json Size: " + posts.size());
		return JsonUtils.convertJsonPostsToObjects(posts);
	}

	public void addTaskDoneListener(IChangeEventListener listener) {
		this.taskDoneListener = listener;
	}

	public boolean isTaskDone() {
		return isTaskDone;
	}

	public void cancel(boolean mayInterruptIfRunning) {
		isCancelled.set(true);
		LogMe.w("Task canceled - " + uri);
	}

	public boolean isCancelled() {
		return isCancelled.get();
	}

	protected abstract void onDownloadComplete(List<RedditPost> results);

	protected void onPostExecute(List<RedditPost> result) {
		onDownloadComplete(result);

		isTaskDone = true;
		if (taskDoneListener != null) {
			taskDoneListener.handle();
			LogMe.w("Task done - " + uri);
		}
	}
}
