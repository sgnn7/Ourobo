package org.sgnn7.ourobo;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

public class PreferencesManager {
	private final Resources resources;
	private final SharedPreferences preferences;

	public PreferencesManager(Context context) {
		this.resources = context.getResources();
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
	}

	public String getString(int preferenceKey) {
		return preferences.getString(resources.getString(preferenceKey), null);
	}
}
