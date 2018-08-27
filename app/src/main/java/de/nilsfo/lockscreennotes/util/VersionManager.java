package de.nilsfo.lockscreennotes.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import de.nilsfo.lockscreennotes.receiver.alarms.LSNAlarmManager;
import de.nilsfo.lsn.R;
import timber.log.Timber;

/**
 * Created by Nils on 13.05.2017.
 */

public abstract class VersionManager {

	public static final String VERSION_RELEASE_DATE_PATTERN = "dd.MM.yyyy";
	public static final int CURRENT_VERSION_UNKNOWN = -1;

	private static JSONObject changeLog;

	public static void onVersionChange(Context context, int oldVersion, int newVersion) {
		Timber.i("App version changed! " + oldVersion + " -> " + newVersion);

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

		switch (newVersion) {
			default:
				Timber.w("This version has no special changes!");
				break;
		}

		displayVersionUpdateNews(context, newVersion);

		LSNAlarmManager alarmManager = new LSNAlarmManager(context);
		if (preferences.getBoolean("pref_auto_backups_enabled", false)) {
			alarmManager.cancelNextAutoBackup();
			alarmManager.scheduleNextAutoBackup();
		}
	}

	public static void displayVersionUpdateNews(final Context context, final int version) {
		JSONObject versionList;
		try {
			versionList = getChanceLog(context);
		} catch (IOException | JSONException e) {
			e.printStackTrace();
			Timber.e(e);
			Toast.makeText(context, R.string.error_internal_error, Toast.LENGTH_LONG).show();
			return;
		}

		SimpleDateFormat jasonDatePattern = new SimpleDateFormat(VERSION_RELEASE_DATE_PATTERN);
		Timber.i("Looking for version changelog for entry: " + version);
		Timber.i("Read changelog JSON: " + versionList.toString());

		String versionName = null, changelog = null, wholeChangelog = null;
		try {
			if (versionList.has(String.valueOf(version))) {
				JSONObject currentVersion = versionList.getJSONObject(String.valueOf(version));
				Timber.i("Reading Version file for " + currentVersion + " (" + version + ")");
				versionName = currentVersion.getString("version");
				changelog = currentVersion.getString("text");
			}

			StringBuilder builder = new StringBuilder();
			Iterator<String> it = versionList.keys();
			TimeUtils utils = new TimeUtils(context);
			while (it.hasNext()) {
				JSONObject currentVersion = versionList.getJSONObject(it.next());
				builder.append(currentVersion.getString("version"));

				String dateText = null;
				if (currentVersion.has("date")) {
					dateText = currentVersion.getString("date");
				} else {
					dateText = jasonDatePattern.format(new Date(0));
					//dateText=context.getString(R.string.error_unknown);
				}

				try {
					Date d = jasonDatePattern.parse(dateText);
					builder.append(" (");
					builder.append(utils.formatDateAccordingToPreferences(d));
					builder.append(")");
				} catch (ParseException e) {
					e.printStackTrace();
				}

				builder.append("\n");
				builder.append(currentVersion.getString("text"));
				builder.append("\n\n");
			}
			wholeChangelog = builder.toString().trim();
		} catch (JSONException e) {
			e.printStackTrace();
			Timber.e(e);
			Toast.makeText(context, R.string.error_internal_error, Toast.LENGTH_LONG).show();
			return;
		}

		Timber.i("Version info found: " + versionName + " -> " + changelog.replace("\n", "") + " everything: " + wholeChangelog.replace("\n", ""));
		AlertDialog dialog = buildChangelogDialog(context, context.getString(R.string.info_changelog_version, versionName), changelog, true, wholeChangelog);
		dialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		dialog.show();
	}

	public static JSONObject getChanceLog(Context context) throws IOException, JSONException {
		if (changeLog != null) {
			return changeLog;
		}

		InputStream inputStream = context.getResources().openRawResource(R.raw.version_changelog);
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

		String str = "";
		StringBuilder builder = new StringBuilder();
		while ((str = reader.readLine()) != null) {
			builder.append(str);
		}

		changeLog = new JSONObject(builder.toString());
		return getChanceLog(context);
	}

	public static int getCurrentVersion(Context context) {
		try {
			return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
			Timber.e(e);
		}
		return CURRENT_VERSION_UNKNOWN;
	}

	public static Date getCurrentVersionDate(Context context) throws IllegalStateException, IOException, JSONException, ParseException {
		int version = getCurrentVersion(context);
		if (version == CURRENT_VERSION_UNKNOWN)
			throw new IllegalStateException("Current version unknown.");

		JSONObject versionList = getChanceLog(context);
		String s = versionList.getJSONObject(String.valueOf(version)).getString("date");
		SimpleDateFormat sdf = new SimpleDateFormat(VERSION_RELEASE_DATE_PATTERN);
		Date d = sdf.parse(s);

		Timber.i("Formating the update counter string. Read JSON date: " + s + ". Expected pattern: " + VERSION_RELEASE_DATE_PATTERN + " -> " + d.getTime());

		return d;
	}

	private static AlertDialog buildChangelogDialog(final Context context, final String title, final String text, boolean showAllButton, final String allText) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.dialog_changelog, null, false);

		TextView nameTF = (TextView) v.findViewById(R.id.dialog_changelog_fragment_name);
		TextView changelogTF = (TextView) v.findViewById(R.id.dialog_changelog_fragment_changelog);
		nameTF.setText(title);
		changelogTF.setText(text);

		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
		dialogBuilder.setIcon(R.mipmap.ic_launcher);
		dialogBuilder.setTitle(R.string.update_news);
		dialogBuilder.setView(v);
		//dialogBuilder.setMessage("nuls tests");
		//dialogBuilder.setPositiveButton(R.string.action_changelog_highlight,
		//		new DialogInterface.OnClickListener() {
		//			public void onClick(DialogInterface dialog, int whichButton) {
		//				dialog.dismiss();
		//				displayVersionHighlight(context, version);
		//			}
		//		}
		//);
		if (showAllButton) {
			dialogBuilder.setNegativeButton(R.string.action_full_changelog,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							dialog.dismiss();
							buildChangelogDialog(context, context.getString(R.string.action_full_changelog), allText, false, allText).show();
						}
					}
			);
		}
		dialogBuilder.setPositiveButton(R.string.action_dismiss,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.dismiss();
					}
				}
		);
		dialogBuilder.setNeutralButton(R.string.action_feedback, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				String packageName = context.getPackageName();
				Uri uri = Uri.parse("market://details?id=" + packageName);
				Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
				// To count with Play market backstack, After pressing back button,
				// to taken back to our application, we need to add following flags to intent.
				goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
						Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					goToMarket.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
				}
				try {
					context.startActivity(goToMarket);
				} catch (ActivityNotFoundException e) {
					Timber.e(e);
					context.startActivity(new Intent(Intent.ACTION_VIEW,
							Uri.parse("http://play.google.com/store/apps/details?id=" + packageName)));
				}
			}
		});
		return dialogBuilder.create();
	}

	@Deprecated
	public static void displayVersionHighlight(Context context, int version) {
		switch (version) {
			case 6:
				displayVersionUpdateNews(context, version);
				break;
			default:
				Toast.makeText(context, R.string.error_no_update_highlight, Toast.LENGTH_LONG).show();
		}

	}
}