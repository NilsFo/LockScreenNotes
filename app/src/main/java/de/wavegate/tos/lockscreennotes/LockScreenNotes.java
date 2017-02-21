package de.wavegate.tos.lockscreennotes;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Locale;
import java.util.Timer;

import timber.log.Timber;

import static de.wavegate.tos.lockscreennotes.activity.MainActivity.LOGTAG;

/**
 * Created by Nils on 19.02.2017.
 */

public class LockScreenNotes extends Application {

	public static final String LOGTAG = "de.tos.lsn.";

	@Override
	public void onCreate() {
		super.onCreate();

		if (BuildConfig.DEBUG) {
			Timber.plant(new Timber.DebugTree() {
				@Override
				protected String createStackElementTag(StackTraceElement element) {
					return LOGTAG + super.createStackElementTag(element) + ":" + element.getLineNumber();
				}
			});
		} else {
			//Timber.plant(new Timber.);
		}
		Timber.i("Application started via TIMBER!");
		Log.i(LOGTAG, "Application started via TINDER!");

		Locale locale = Locale.getDefault();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (prefs.getString("prefs_time_locale_default_value", getString(R.string.error_unknown)).equals(getString(R.string.prefs_time_locale_default_value))) {
			String iso = locale.getISO3Country();
			Timber.i("Turns out the user wants the default time locale to be the current locale: " + iso);

			prefs.edit().putString("prefs_time_locale_default_value", iso).apply();
		}

		Timber.i("Started the app. Locale used: " + locale.getISO3Country() +" - " + locale.getCountry() + " - " + locale.getDisplayLanguage() + " - " + locale.getDisplayCountry());
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
	}

}
