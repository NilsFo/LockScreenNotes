package de.wavegate.tos.lockscreennotes.view.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import de.wavegate.tos.lockscreennotes.R;

import static de.wavegate.tos.lockscreennotes.MainActivity.LOGTAG;
import static de.wavegate.tos.lockscreennotes.MainActivity.PREFS_HIDE_TUTORIAL;

/**
 * Created by Nils on 01.09.2016.
 */

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

	public static final String GITHUB_URL = "https://github.com/NilsFo/LockScreenNotes";
	public static final String HOME_SCREEN_LINES_KEY = "prefs_homescreen_lines";
	public static final String LOD_DATE_KEY = "prefs_date_detail";
	public static final String LOD_TIME_KEY = "prefs_time_detail";
	public static final int MIN_HOME_SCREEN_LINES = 5;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		Preference myPref = findPreference("pref_share_app");
		myPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				shareApp();
				return false;
			}
		});

		myPref = findPreference("pref_view_on_github");
		myPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				viewOnGithub();
				return false;
			}
		});

		myPref = findPreference("prefs_reset_tutorial");
		myPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				resetTutorial();
				return false;
			}
		});

		updatePreferenceSummaries();
	}

	private void resetTutorial() {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
		editor.putBoolean(PREFS_HIDE_TUTORIAL, false);
		editor.apply();
		Log.i(LOGTAG, "Request to reset tutorial recieved. Shared pref 'hide_tutorial' is now: " + PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(PREFS_HIDE_TUTORIAL, false));

		Toast.makeText(getActivity(), R.string.prefs_reset_tutorial_success, Toast.LENGTH_LONG).show();

		updatePreferenceSummaries();
	}

	private void shareApp() {
		//TODO add android store URL later
		Toast.makeText(getActivity(), R.string.error_not_in_android_store_yet, Toast.LENGTH_LONG).show();
	}

	private void viewOnGithub() {
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(GITHUB_URL));
		startActivity(i);
	}

	@Override
	public void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Log.i(LOGTAG, "Preference changed: " + sharedPreferences + " key: " + key + " is now: " + getValue(sharedPreferences, key));

		if (key.equals(HOME_SCREEN_LINES_KEY)) {
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
			int lines = 0;
			try {
				lines = Integer.parseInt(preferences.getString(HOME_SCREEN_LINES_KEY, "?"));
			} catch (NumberFormatException e) {
				preferences.edit().putString(HOME_SCREEN_LINES_KEY, String.valueOf(MIN_HOME_SCREEN_LINES)).apply();
				Toast.makeText(getActivity(), R.string.error_invalid_value, Toast.LENGTH_LONG).show();
			}

			if (lines < MIN_HOME_SCREEN_LINES) {
				preferences.edit().putString(HOME_SCREEN_LINES_KEY, String.valueOf(MIN_HOME_SCREEN_LINES)).apply();
				Toast.makeText(getActivity(), R.string.home_screen_lines_too_small, Toast.LENGTH_LONG).show();
			}

			updatePreferenceSummaries();
		}

		if (key.equals(LOD_TIME_KEY) || key.equals(LOD_DATE_KEY))
			updatePreferenceSummaries();
	}

	private void updatePreferenceSummaries() {
		Preference pref = findPreference(HOME_SCREEN_LINES_KEY);
		String lod_date = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(LOD_DATE_KEY, getString(R.string.error_unknown));
		String lod_time = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(LOD_TIME_KEY, getString(R.string.error_unknown));
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

		try {
			int lod = Integer.parseInt(lod_date);
			lod_date = getResources().getStringArray(R.array.DateDetails)[lod];
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			int lod = Integer.parseInt(lod_time);
			lod_time = getResources().getStringArray(R.array.DateDetails)[lod];
		} catch (Exception e) {
			e.printStackTrace();
		}

		pref.setSummary(String.format(getString(R.string.prefs_homescreen_lines_summary),
				PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(HOME_SCREEN_LINES_KEY, "5")));

		pref = findPreference(LOD_DATE_KEY);
		pref.setSummary(String.format(getString(R.string.prefs_currently_selected), lod_date));

		pref = findPreference(LOD_TIME_KEY);
		pref.setSummary(String.format(getString(R.string.prefs_currently_selected), lod_time));

		boolean dont_show_tutorial = prefs.getBoolean(PREFS_HIDE_TUTORIAL, false);
		Log.i(LOGTAG, "PreferenceFragment here. Don't show tutorial? " + dont_show_tutorial);
		if (!dont_show_tutorial) {
			PreferenceCategory mCategory = (PreferenceCategory) findPreference("pref_cat_advanced");
			pref = findPreference("prefs_reset_tutorial");
			mCategory.removePreference(pref);
		}
	}

	public String getValue(SharedPreferences sharedPreferences, String key) {
		if (!sharedPreferences.contains(key)) {
			return "<Unexisting Value>";
		}

		try {
			return String.valueOf(sharedPreferences.getBoolean(key, false));
		} catch (ClassCastException e) {
		}
		try {
			return String.valueOf(sharedPreferences.getFloat(key, 0f));
		} catch (ClassCastException e) {
		}
		try {
			return String.valueOf(sharedPreferences.getInt(key, 0));
		} catch (ClassCastException e) {
		}
		try {
			return String.valueOf(sharedPreferences.getLong(key, 0));
		} catch (ClassCastException e) {
		}
		try {
			return String.valueOf(sharedPreferences.getString(key, "<Missing String>"));
		} catch (ClassCastException e) {
		}

		return "<Unknwon Value>";
	}
}
