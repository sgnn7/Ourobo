package org.sgnn7.ourobo;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

public class PreferencesManager {
	private final Context context;
	private final Resources resources;

	public PreferencesManager(Context context) {
		this.context = context;
		this.resources = context.getResources();
	}

	public <T> T getValue(int preferenceKey, Class<T> clazz) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		return clazz.cast(preferences.getAll().get(resources.getString(preferenceKey)));
	}
}
