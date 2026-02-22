package org.sgnn7.ourobo;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import androidx.preference.PreferenceManager;

public class PreferencesManager {
	private final Resources resources;
	private final SharedPreferences preferences;
	private final SharedPreferences encryptedPrefs;
	private final String passwordKey;

	public PreferencesManager(Context context) {
		this.resources = context.getResources();
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
		passwordKey = resources.getString(R.string.preference_id_password);

		EncryptedPreferenceDataStore encryptedStore =
				new EncryptedPreferenceDataStore(context);
		encryptedPrefs = encryptedStore.getEncryptedPrefs();

		migratePlainTextPassword();
	}

	public String getString(int preferenceKey) {
		String key = resources.getString(preferenceKey);
		if (passwordKey.equals(key)) {
			return encryptedPrefs.getString(key, null);
		}
		return preferences.getString(key, null);
	}

	private void migratePlainTextPassword() {
		String plainPassword = preferences.getString(passwordKey, null);
		if (plainPassword != null) {
			encryptedPrefs.edit().putString(passwordKey, plainPassword).apply();
			preferences.edit().remove(passwordKey).apply();
		}
	}
}
