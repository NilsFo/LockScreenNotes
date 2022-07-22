package de.nilsfo.lockscreennotes;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.util.Locale;

import de.nilsfo.lockscreennotes.util.VersionManager;
import de.nilsfo.lsn.BuildConfig;
import de.nilsfo.lsn.R;
import timber.log.Timber;

// TODO For debugging purposes: Enable / Disable this line for debugDB
// import com.amitshekhar.DebugDB;

/**
 * Created by Nils on 19.02.2017.
 */

public class LockScreenNotes extends Application {

	/**
	 * DO NOT EDIT BECAUSE OF LEGACY SUPPORT!
	 * The DB ID is dependant on this App tag, so it should not be changed!
	 */
	public static final String APP_TAG = "de.tos.lsn.";
	public static final String LOG_TAG = APP_TAG + "log.";
	public static final String PREFS_TAG = APP_TAG + "prefs_";

	public static final int REQUEST_CODE_PERMISSION_STORAGE = 1;
	public static final int REQUEST_CODE_INTENT_EXTERNAL_SEARCH = 2;
	public static final int REQUEST_CODE_INTENT_AUTO_BACKUP_ALARM = 3;
	public static final int REQUEST_CODE_INTENT_OPEN_APP = 4;
	public static final int REQUEST_CODE_INTENT_OPEN_APP_EDIT_NOTE = 5;

	public static boolean isDarkMode(Context context) {
		return isDarkMode(context.getResources().getConfiguration());
	}

	public static boolean isDarkMode(Configuration configuration) {
		int currentNightMode = configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK;
		switch (currentNightMode) {
			case Configuration.UI_MODE_NIGHT_NO:
				return false;
			case Configuration.UI_MODE_NIGHT_YES:
				return true;
		}
		throw new IllegalStateException("Failed to obtain the state of the dark mode");
	}

	@Override
	public void onCreate() {
		super.onCreate();

		if (isDebugBuild()) {
			Timber.plant(new DebugTree());

			// TODO For debugging purposes: Enable / Disable this line for debugDB
			// Timber.i("Debug-DB URL: " + DebugDB.getAddressLog());
		} else {
			Timber.plant(new ReleaseTree());
		}

		Locale locale = Locale.getDefault();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (prefs.getString("prefs_time_locale_default_value", getString(R.string.error_unknown)).equals(getString(R.string.prefs_time_locale_default_value))) {
			String iso = locale.getISO3Country();
			Timber.i("Turns out the user wants the default time locale to be the current locale: " + iso);
			prefs.edit().putString("prefs_time_locale_default_value", iso).apply();
		}

		PreferenceManager.setDefaultValues(this, R.xml.prefs_general, false);
		PreferenceManager.setDefaultValues(this, R.xml.prefs_notifications, false);
		PreferenceManager.setDefaultValues(this, R.xml.prefs_info, false);
		PreferenceManager.setDefaultValues(this, R.xml.prefs_time, false);
		PreferenceManager.setDefaultValues(this, R.xml.prefs_auto_backup, false);

		Timber.i("Started the app. Locale used: " + locale.getISO3Country() + " - " + locale.getCountry() + " - " + locale.getDisplayLanguage() + " - " + locale.getDisplayCountry());
		int currentVer = VersionManager.getCurrentVersion(this);
		Timber.i("App Version: " + currentVer + ". Device Android-Version Code: " + Build.VERSION.SDK_INT);

		// Incrementing launch timer
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		long launchedCountAllTime = 0;
		long launchedCountThisVersion = 0;
		try {
			launchedCountAllTime = preferences.getLong(VersionManager.PREFERENCE_APP_LAUNCHED_ALL_TIME, 0);
			launchedCountThisVersion = preferences.getLong(VersionManager.PREFERENCE_APP_LAUNCHED_THIS_VERSION, 0);
		} catch (Exception e) {
			Timber.w("Failed to read app launch counts. Resetting!");
			Toast.makeText(this, R.string.error_internal_error, Toast.LENGTH_LONG).show();
		}
		Timber.i("App previously launched count all time: " + launchedCountAllTime + ".");
		Timber.i("App previously launched count this version: " + launchedCountThisVersion + ".");

		SharedPreferences.Editor editor = preferences.edit();
		editor.putLong(VersionManager.PREFERENCE_APP_LAUNCHED_ALL_TIME, launchedCountAllTime + 1);
		editor.putLong(VersionManager.PREFERENCE_APP_LAUNCHED_THIS_VERSION, launchedCountThisVersion + 1);
		editor.apply();
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
	}

	public static boolean isDebugBuild() {
		return BuildConfig.DEBUG;
	}

	private class DebugTree extends Timber.DebugTree {
		@Override
		protected String createStackElementTag(StackTraceElement element) {
			return LOG_TAG + super.createStackElementTag(element) + ":" + element.getLineNumber();
		}
	}

	private class ReleaseTree extends DebugTree {
		@Override
		protected boolean isLoggable(String tag, int priority) {
			return !(priority == Log.VERBOSE || priority == Log.DEBUG || priority == Log.INFO);
		}
	}

}
