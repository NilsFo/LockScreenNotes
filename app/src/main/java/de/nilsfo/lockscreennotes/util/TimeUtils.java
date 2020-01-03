package de.nilsfo.lockscreennotes.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import de.nilsfo.lsn.R;
import timber.log.Timber;

public class TimeUtils {

	private static final int DEFAULT_LEVEL_OF_DETAIL = DateFormat.MEDIUM;
	private SharedPreferences preferences;
	private Context context;

	public TimeUtils(Context context) {
		this.context = context;
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
	}

	public String formatDateAccordingToPreferences(Date date) {
		return formatDateAccordingToPreferences(date.getTime());
	}

	public String formatDateAccordingToPreferences(long time) {
		if (isRelativeTimePrefered()) {
			return formatRelative(time);
		} else {
			return formatAbsolute(time);
		}
	}

	public String formatRelative(Date date) {
		return formatRelative(date.getTime());
	}

	public String formatRelative(long timestamp) {
		return DateUtils.getRelativeTimeSpanString(timestamp, new Date().getTime(), 0L, DateUtils.FORMAT_ABBREV_ALL).toString();
	}

	public String formatAbsolute(long time) {
		return context.getString(R.string.concat_dash, formatDateAbsolute(time), formatTimeAbsolute(time));
	}

	public String formatAbsolute(Date time, int levelOfDetailTime, int levelOfDetailDate) {
		return context.getString(R.string.concat_dash, formatDateAbsolute(time, levelOfDetailTime), formatTimeAbsolute(time, levelOfDetailDate));
	}

	public String formatAbsolute(Date date) {
		return formatAbsolute(date.getTime());
	}

	public String formatDateAbsolute(long time) {
		return formatDateAbsolute(new Date(time));
	}

	public String formatTimeAbsolute(long time) {
		return formatTimeAbsolute(new Date(time));
	}

	public String formatTimeAbsolute(Date date, int levelOfDetail) {
		return DateFormat.getTimeInstance(levelOfDetail, getLocale()).format(date);
	}

	public String formatTimeAbsolute(Date date) {
		String lod = preferences.getString("prefs_time_detail", context.getString(R.string.error_unknown));
		return formatTimeAbsolute(date, getLoDviaPreference(lod));
	}

	public String formatDateAbsolute(Date date, int levelOfDetail) {
		return DateFormat.getDateInstance(levelOfDetail, getLocale()).format(date);
	}

	public String formatDateAbsolute(long date, int levelOfDetailTime, int levelOfDetailDate) {
		return formatDateAbsolute(new Date(date), levelOfDetailTime, levelOfDetailDate);
	}

	public String formatDateAbsolute(Date date, int levelOfDetailTime, int levelOfDetailDate) {
		return formatTimeAbsolute(date, levelOfDetailTime) + " " + formatDateAbsolute(date, levelOfDetailDate);
	}

	public String formatDateAbsolute(Date date) {
		String lod = preferences.getString("prefs_date_detail", context.getString(R.string.error_unknown));
		return formatDateAbsolute(date, getLoDviaPreference(lod));
	}

	public int getLoDviaPreference(String lod) {
		int i = -1;
		try {
			i = Integer.parseInt(lod);
		} catch (NumberFormatException e) {
			Timber.i("Warning: Could not interpret this as a number: " + lod);
		}

		switch (i) {
			case 0:
				return DateFormat.FULL;
			case 1:
				return DateFormat.LONG;
			case 2:
				return DateFormat.MEDIUM;
			case 3:
				return DateFormat.SHORT;
		}

		Timber.i("Warning: Level of Detail not found, reverting to default. Input: " + lod);
		return DEFAULT_LEVEL_OF_DETAIL;
	}

	public boolean isRelativeTimePrefered() {
		return preferences.getBoolean("prefs_time_relative", true);
	}

	private Locale getLocale() {
		return context.getResources().getConfiguration().locale;
	}

}
