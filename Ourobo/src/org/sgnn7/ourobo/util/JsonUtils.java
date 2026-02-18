package org.sgnn7.ourobo.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sgnn7.ourobo.data.RedditPost;

public class JsonUtils {
	private final static ObjectMapper objectMapper;

	static {
		objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	public static List<RedditPost> convertJsonPostsToObjects(List<JsonNode> posts) {
		long startTime = System.currentTimeMillis();

		List<RedditPost> redditPosts = new ArrayList<RedditPost>();
		for (JsonNode post : posts) {
			RedditPost redditPost = convertJsonToBean(post.get("data"), RedditPost.class);
			redditPosts.add(redditPost);
		}

		LogMe.logTime(startTime, "deserialize the data");

		return redditPosts;
	}

	public static <T> T convertJsonToBean(JsonNode topNode, String path, Class<T> clazz) {
		return convertJsonToBean(traverseJsonTree(topNode, path), clazz);
	}

	public static <T> T convertJsonToBean(JsonNode json, Class<T> clazz) {
		T returnObject = null;
		try {
			Object obj = objectMapper.readValue(json.traverse(), clazz);
			returnObject = clazz.cast(obj);
		} catch (Exception e) {
			LogMe.e("Could not deserialize data " + e.getMessage());
			e.printStackTrace();
		}

		return returnObject;
	}

	public static List<JsonNode> getJsonChildren(JsonNode topNode, String fullPath) {
		List<JsonNode> children = new ArrayList<JsonNode>();
		JsonNode targetNode = traverseJsonTree(topNode, fullPath);

		Iterator<JsonNode> elements = targetNode.elements();
		while (elements.hasNext()) {
			children.add(elements.next());
		}
		return children;
	}

	private static JsonNode traverseJsonTree(JsonNode topNode, String path) {
		String[] paths = path.split("/");

		JsonNode targetNode = topNode;
		for (String pathPart : paths) {
			targetNode = targetNode.get(pathPart);
		}
		return targetNode;
	}
}
