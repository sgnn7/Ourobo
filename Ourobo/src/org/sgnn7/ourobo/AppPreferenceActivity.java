package org.sgnn7.ourobo;

import android.os.Bundle;
import android.text.InputType;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;

public class AppPreferenceActivity extends AppCompatActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			getSupportFragmentManager()
					.beginTransaction()
					.replace(android.R.id.content, new SettingsFragment())
					.commit();
		}
	}

	public static class SettingsFragment extends PreferenceFragmentCompat {
		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			setPreferencesFromResource(R.xml.preferences, rootKey);

			EditTextPreference passwordPref = findPreference(
					getString(R.string.preference_id_password));
			if (passwordPref != null) {
				passwordPref.setPreferenceDataStore(
						new EncryptedPreferenceDataStore(requireContext()));
				passwordPref.setOnBindEditTextListener(editText ->
						editText.setInputType(InputType.TYPE_CLASS_TEXT
								| InputType.TYPE_TEXT_VARIATION_PASSWORD));
			}
		}
	}
}
