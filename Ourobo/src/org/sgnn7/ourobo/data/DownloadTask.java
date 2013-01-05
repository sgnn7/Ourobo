package org.sgnn7.ourobo.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.sgnn7.ourobo.authentication.SessionManager;
import org.sgnn7.ourobo.eventing.IChangeEventListener;
import org.sgnn7.ourobo.util.HttpUtils;
import org.sgnn7.ourobo.util.JsonUtils;
import org.sgnn7.ourobo.util.LogMe;

import android.os.AsyncTask;

public abstract class DownloadTask extends AsyncTask<String, Void, List<RedditPost>> {
	private final SessionManager sessionManager;

	private IChangeEventListener taskDoneListener;
	private String uri;
	private boolean isTaskDone = false;

	public DownloadTask(SessionManager sessionManager) {
		this.sessionManager = sessionManager;
	}

	@Override
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

	private List<RedditPost> getRedditPostsFromContent(String pageContent) throws IOException, JsonParseException,
			JsonMappingException {
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

	protected abstract void onDownloadComplete(List<RedditPost> results);

	@Override
	@Deprecated
	protected void onPostExecute(List<RedditPost> result) {
		onDownloadComplete(result);

		isTaskDone = true;
		if (taskDoneListener != null) {
			taskDoneListener.handle();
			LogMe.w("Task done - " + uri);
		}
	}

	@Override
	protected void onCancelled() {
		LogMe.w("Task canceled - " + uri);
		onPostExecute(new ArrayList<RedditPost>());
	}
}