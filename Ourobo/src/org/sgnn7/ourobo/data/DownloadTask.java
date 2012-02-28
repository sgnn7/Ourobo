package org.sgnn7.ourobo.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.sgnn7.ourobo.authentication.SessionManager;
import org.sgnn7.ourobo.util.HttpUtils;
import org.sgnn7.ourobo.util.JsonUtils;
import org.sgnn7.ourobo.util.LogMe;

import android.os.AsyncTask;

public abstract class DownloadTask extends AsyncTask<String, Void, List<RedditPost>> {
	private final SessionManager sessionManager;

	public DownloadTask(SessionManager sessionManager) {
		this.sessionManager = sessionManager;
	}

	@Override
	protected List<RedditPost> doInBackground(String... params) {
		List<RedditPost> posts = new ArrayList<RedditPost>();
		try {
			String getParameters = params.length < 2 ? "" : params[1];

			String pageContent = HttpUtils.getPageContent(sessionManager, params[0] + getParameters);
			LogMe.d("Content: " + pageContent);
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
		LogMe.d(pageContent);
		JsonNode topNode = new ObjectMapper().readValue(pageContent, JsonNode.class);
		List<JsonNode> posts = JsonUtils.getJsonChildren(topNode, "data/children");
		LogMe.d("Json Size: " + posts.size());
		List<RedditPost> usablePosts = JsonUtils.convertJsonPostsToObjects(posts);
		return usablePosts;
	}

	@Override
	protected abstract void onPostExecute(List<RedditPost> results);
}