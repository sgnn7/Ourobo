package org.sgnn7.ourobo.data;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum UrlFileType {
	IMAGE("png", "bmp", "gif", "jpg"), UNKNOWN;

	private final Set<String> extensions = new HashSet<String>();

	private UrlFileType(String... extensions) {
		this.extensions.addAll(Arrays.asList(extensions));
	}

	public Set<String> getExtensions() {
		return extensions;
	}
}
