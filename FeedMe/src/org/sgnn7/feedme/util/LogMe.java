package org.sgnn7.feedme.util;

import android.util.Log;

public class LogMe {
	private static enum LogLevels {
		ERROR, WARN, INFO, DEBUG
	}

	private static final String PREFIX = "FeedMe";
	private static final LogLevels LOG_LEVEL = LogLevels.ERROR;

	public static void e(String value) {
		Log.e(PREFIX, value);
	}

	public static void i(String value) {
		if (logLevelAllows(LogLevels.INFO)) {
			Log.i(PREFIX, value);
		}
	}

	public static void d(String value) {
		if (logLevelAllows(LogLevels.DEBUG)) {
			Log.d(PREFIX, value);
		}
	}

	public static void logTime(long startTime, String postfix) {
		Log.e(PREFIX, "Took " + (System.currentTimeMillis() - startTime) + "ms to " + postfix);
	}

	public static void w(String value) {
		if (logLevelAllows(LogLevels.WARN)) {
			Log.w(PREFIX, value);
		}
	}

	private static boolean logLevelAllows(LogLevels level) {
		return LOG_LEVEL.compareTo(level) >= 0;
	}
}
