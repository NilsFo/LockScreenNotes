package de.nilsfo.lockscreennotes;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Locale;

import de.nilsfo.lsn.BuildConfig;
import de.nilsfo.lsn.R;
import timber.log.Timber;

/**
 * Created by Nils on 19.02.2017.
 */

public class LockScreenNotes extends Application {

	public static final String LOGTAG = "de.tos.lsn.";

	@Override
	public void onCreate() {
		super.onCreate();

		if (BuildConfig.DEBUG) {
			Timber.plant(new DebugTree());
		} else {
			Timber.plant(new ReleaseTree());
		}
		Timber.i("Application started via TIMBER!");

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
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
	}

	private class DebugTree extends Timber.DebugTree{
		@Override
		protected String createStackElementTag(StackTraceElement element) {
			return LOGTAG + super.createStackElementTag(element) + ":" + element.getLineNumber();
		}
	}

	private class ReleaseTree extends DebugTree {

		@Override
		protected boolean isLoggable(String tag, int priority) {
			return !(priority == Log.VERBOSE || priority == Log.DEBUG || priority == Log.INFO);
		}

		//@Override
		//protected void log(int priority, String tag, String message, Throwable t) {
		//	if (isLoggable(tag, priority)) {
		//
		//		if (message.length() < MAX_LOG_LENGTH) {
		//			if (priority == Log.ASSERT) {
		//				Log.wtf(tag, message);
		//			} else {
		//				Log.println(priority, tag, message);
		//			}
		//			return;
		//		}
		//
		//		for (int i = 0, length = message.length(); i < length; i++) {
		//			int newLine = message.indexOf('\n', i);
		//			newLine = newLine != -1 ? newLine : length;
		//			do {
		//				int end = Math.min(newLine, i + MAX_LOG_LENGTH);
		//				String part = message.substring(i, end);
		//
		//				if (priority == Log.ASSERT) {
		//					Log.wtf(tag, part);
		//				} else {
		//					Log.println(priority, tag, part);
		//				}
		//				i = end;
		//			} while (i < newLine);
		//		}
		//	}
		//}
	}

}
