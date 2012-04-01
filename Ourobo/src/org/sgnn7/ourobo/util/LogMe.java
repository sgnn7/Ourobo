package org.sgnn7.ourobo.util;

import org.sgnn7.ourobo.BuildConfig;

import android.util.Log;

public class LogMe {
	private static enum LogLevels {
		ERROR, WARN, INFO, DEBUG, VERBOSE
	}

	private static final LogLevels LOG_LEVEL = BuildConfig.DEBUG ? LogLevels.ERROR : LogLevels.ERROR;

	private static final String PREFIX = "Ourobo";

	public static void e(String value) {
		Log.e(PREFIX, value);
	}

	public static void e(Exception e) {
		String errorMessage = "NULL";
		if (e != null && e.getMessage() != null) {
			errorMessage = e.getMessage();
		}
		e(errorMessage);
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

	public static void v(String value) {
		if (logLevelAllows(LogLevels.VERBOSE)) {
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
