package org.sgnn7.ourobo.data;

import static org.junit.Assert.*;
import org.junit.Test;

public class UrlFileTypeTest {

	@Test
	public void imageTypeContainsPng() {
		assertTrue(UrlFileType.IMAGE.getExtensions().contains("png"));
	}

	@Test
	public void imageTypeContainsJpg() {
		assertTrue(UrlFileType.IMAGE.getExtensions().contains("jpg"));
	}

	@Test
	public void imageTypeContainsGif() {
		assertTrue(UrlFileType.IMAGE.getExtensions().contains("gif"));
	}

	@Test
	public void imageTypeContainsBmp() {
		assertTrue(UrlFileType.IMAGE.getExtensions().contains("bmp"));
	}

	@Test
	public void unknownTypeHasNoExtensions() {
		assertTrue(UrlFileType.UNKNOWN.getExtensions().isEmpty());
	}
}
