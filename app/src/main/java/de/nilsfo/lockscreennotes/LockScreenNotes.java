package de.nilsfo.lockscreennotes;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Locale;

import de.nilsfo.lockscreennotes.util.VersionManager;
import de.nilsfo.lsn.BuildConfig;
import de.nilsfo.lsn.R;
import timber.log.Timber;

//import com.amitshekhar.DebugDB;

/**
 * Created by Nils on 19.02.2017.
 */

public class LockScreenNotes extends Application {

	public static final String APP_TAG = "de.tos.lsn.";
	public static final String LOG_TAG = APP_TAG + "log.";
	public static final String PREFS_TAG = APP_TAG + "prefs_";

	public static final int REQUEST_CODE_PERMISSION_STORAGE = 1;
	public static final int REQUEST_CODE_INTENT_EXTERNAL_SEARCH = 2;

	@Override
	public void onCreate() {
		super.onCreate();

		if (isDebugBuild()) {
			Timber.plant(new DebugTree());
			//Timber.i("Debug-DB URL: " + DebugDB.getAddressLog());
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

		Timber.i("Started the app. Locale used: " + locale.getISO3Country() + " - " + locale.getCountry() + " - " + locale.getDisplayLanguage() + " - " + locale.getDisplayCountry());
		int currentVer = VersionManager.getCurrentVersion(this);
		Timber.i("App Version: " + currentVer + ". Device Android-Version Code: " + Build.VERSION.SDK_INT);
	}

	public static boolean isDebugBuild() {
		return BuildConfig.DEBUG;
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
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
