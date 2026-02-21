package org.sgnn7.ourobo.util;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sgnn7.ourobo.data.UrlFileType;

public class HttpUtilsFileTypeTest {

	@Test
	public void detectsPngFile() {
		assertEquals(UrlFileType.IMAGE, HttpUtils.getFileType("https://example.com/image.png"));
	}

	@Test
	public void detectsJpgFile() {
		assertEquals(UrlFileType.IMAGE, HttpUtils.getFileType("https://example.com/photo.jpg"));
	}

	@Test
	public void detectsGifFile() {
		assertEquals(UrlFileType.IMAGE, HttpUtils.getFileType("https://example.com/anim.gif"));
	}

	@Test
	public void detectsBmpFile() {
		assertEquals(UrlFileType.IMAGE, HttpUtils.getFileType("https://example.com/bitmap.bmp"));
	}

	@Test
	public void detectsUnknownForHtml() {
		assertEquals(UrlFileType.UNKNOWN, HttpUtils.getFileType("https://example.com/page.html"));
	}

	@Test
	public void detectsUnknownForNoExtension() {
		assertEquals(UrlFileType.UNKNOWN, HttpUtils.getFileType("https://example.com/path"));
	}

	@Test
	public void handlesUppercaseExtension() {
		assertEquals(UrlFileType.IMAGE, HttpUtils.getFileType("https://example.com/image.PNG"));
	}

	@Test
	public void handlesQueryStringAfterExtension() {
		// Current implementation treats "png?w=100" as the extension, which won't match
		assertEquals(UrlFileType.UNKNOWN, HttpUtils.getFileType("https://example.com/image.png?w=100"));
	}
}
