package org.sgnn7.ourobo.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.DeserializationProblemHandler;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.sgnn7.ourobo.data.RedditPost;

public class JsonUtils {
	private final static ObjectMapper objectMapper;

	static {
		objectMapper = new ObjectMapper();
		objectMapper.getDeserializationConfig().addHandler(new IgnoreUnusedFieldsHandler());
	}

	public static List<RedditPost> convertJsonPostNodesToJavaBeans(List<JsonNode> posts) {
		long startTime = System.currentTimeMillis();

		List<RedditPost> redditPosts = new ArrayList<RedditPost>();
		for (JsonNode post : posts) {
			try {
				RedditPost redditPost = objectMapper.readValue(post.get("data"), RedditPost.class);
				redditPosts.add(redditPost);
			} catch (Exception e) {
				LogMe.e("Could not deserialize data " + e.getMessage());
				e.printStackTrace();
			}
		}

		LogMe.logTime(startTime, "deserialize the data");

		return redditPosts;
	}

	public static List<JsonNode> getJsonChildren(JsonNode topNode, String fullPath) {
		List<JsonNode> children = new ArrayList<JsonNode>();
		String[] paths = fullPath.split("/");

		JsonNode targetNode = topNode;
		for (String pathPart : paths) {
			targetNode = targetNode.get(pathPart);
		}

		Iterator<JsonNode> elements = targetNode.getElements();
		while (elements.hasNext()) {
			children.add(elements.next());
		}
		return children;
	}

	private static class IgnoreUnusedFieldsHandler extends DeserializationProblemHandler {
		@Override
		public boolean handleUnknownProperty(DeserializationContext ctxt, JsonDeserializer<?> deserializer,
				Object beanOrClass, String propertyName) throws IOException, JsonProcessingException {
			ctxt.getParser().skipChildren();
			LogMe.w("Unknown property " + propertyName + " skipped");
			return true;
		}
	}

}
