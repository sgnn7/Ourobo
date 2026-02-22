package org.sgnn7.ourobo;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceDataStore;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class EncryptedPreferenceDataStore extends PreferenceDataStore {
	private final SharedPreferences encryptedPrefs;

	public EncryptedPreferenceDataStore(Context context) {
		try {
			String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
			encryptedPrefs = EncryptedSharedPreferences.create(
					"encrypted_credentials",
					masterKeyAlias,
					context,
					EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
					EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
			);
		} catch (GeneralSecurityException | IOException e) {
			throw new RuntimeException("Failed to create encrypted preferences", e);
		}
	}

	@Override
	public void putString(String key, @Nullable String value) {
		encryptedPrefs.edit().putString(key, value).apply();
	}

	@Nullable
	@Override
	public String getString(String key, @Nullable String defValue) {
		return encryptedPrefs.getString(key, defValue);
	}

	public SharedPreferences getEncryptedPrefs() {
		return encryptedPrefs;
	}
}
