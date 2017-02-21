package de.wavegate.tos.lockscreennotes.util.listener;

import android.content.Context;
import android.preference.ListPreference;
import android.preference.Preference;

import de.wavegate.tos.lockscreennotes.R;

/**
 * Created by Nils on 21.02.2017.
 */

public class SettingsBindPreferenceSummaryToValueListener implements Preference.OnPreferenceChangeListener {

	private Integer stringResource = null;

	public SettingsBindPreferenceSummaryToValueListener() {
	}

	public SettingsBindPreferenceSummaryToValueListener(int stringResource) {
		this.stringResource = stringResource;
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object value) {
		String stringValue = value.toString();
		Context context = preference.getContext();

		if (preference instanceof ListPreference) {
			// For list prefs_general, look up the correct display value in
			// the preference's 'entries' list.
			ListPreference listPreference = (ListPreference) preference;
			int index = listPreference.findIndexOfValue(stringValue);

			// Set the summary to reflect the new value.

			String text = index >= 0 ? listPreference.getEntries()[index].toString() : context.getString(R.string.error_unknown);
			if (stringResource != null) {
				text = context.getString(stringResource, text);
			}

			preference.setSummary(text);
		} else {
			// For all other prefs_general, set the summary to the value's
			// simple string representation.
			if (stringResource != null) {
				stringValue = context.getString(stringResource, stringValue);
			}

			preference.setSummary(stringValue);
		}
		return true;
	}
}
