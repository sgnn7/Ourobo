package org.sgnn7.ourobo.data;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class RedditPostTest {

	private RedditPost post;

	@Before
	public void setUp() {
		post = new RedditPost();
	}

	@Test
	public void scoreGetterSetter() {
		post.setScore(42);
		assertEquals(42, post.getScore());
	}

	@Test
	public void titleGetterSetter() {
		post.setTitle("Test Post");
		assertEquals("Test Post", post.getTitle());
	}

	@Test
	public void urlGetterSetter() {
		post.setUrl("https://example.com");
		assertEquals("https://example.com", post.getUrl());
	}

	@Test
	public void permalinkGetterSetter() {
		post.setPermalink("/r/test/comments/abc");
		assertEquals("/r/test/comments/abc", post.getPermalink());
	}

	@Test
	public void numCommentsGetterSetter() {
		post.setNum_comments(100);
		assertEquals(100, post.getNum_comments());
	}

	@Test
	public void thumbnailGetterSetter() {
		post.setThumbnail("https://thumb.example.com/img.jpg");
		assertEquals("https://thumb.example.com/img.jpg", post.getThumbnail());
	}

	@Test
	public void nameGetterSetter() {
		post.setName("t3_abc123");
		assertEquals("t3_abc123", post.getName());
	}

	@Test
	public void likesGetterSetterNull() {
		post.setLikes(null);
		assertNull(post.getLikes());
	}

	@Test
	public void likesGetterSetterTrue() {
		post.setLikes(true);
		assertTrue(post.getLikes());
	}

	@Test
	public void likesGetterSetterFalse() {
		post.setLikes(false);
		assertFalse(post.getLikes());
	}

	@Test
	public void defaultValuesAreNull() {
		assertNull(post.getTitle());
		assertNull(post.getUrl());
		assertNull(post.getPermalink());
		assertNull(post.getThumbnail());
		assertNull(post.getName());
		assertNull(post.getLikes());
		assertEquals(0, post.getScore());
		assertEquals(0, post.getNum_comments());
	}
}
