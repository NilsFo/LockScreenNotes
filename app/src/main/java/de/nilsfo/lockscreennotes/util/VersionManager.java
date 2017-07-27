package de.nilsfo.lockscreennotes.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
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
import java.util.Iterator;

import de.nilsfo.lsn.R;
import timber.log.Timber;

/**
 * Created by Nils on 13.05.2017.
 */

public abstract class VersionManager {

	public static void onVersionChange(Context context, int oldVersion, int newVersion) {
		Timber.i("App version changed! " + oldVersion + " -> " + newVersion);

		switch (newVersion) {
			default:
				Timber.w("This version has no special changes!");
				break;
		}

		displayVersionUpdateNews(context, newVersion);
	}

	public static void displayVersionUpdateNews(final Context context, final int version) {
		InputStream inputStream = context.getResources().openRawResource(R.raw.version_changelog);
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

		String str = "";
		StringBuilder builder = new StringBuilder();
		try {
			while ((str = reader.readLine()) != null) {
				builder.append(str);
			}
		} catch (IOException e) {
			e.printStackTrace();
			Timber.e(e);
			//TODO handle error
		}

		Timber.i("Looking for version changelog for entry: " + version);
		Timber.i("Read changelog JSON: " + builder.toString());
		String versionName = null, changelog = null, wholeChangelog = null;
		try {
			JSONObject versionList = new JSONObject(builder.toString());
			if (versionList.has(String.valueOf(version))) {
				JSONObject currentVersion = versionList.getJSONObject(String.valueOf(version));
				versionName = currentVersion.getString("version");
				changelog = currentVersion.getString("text");
			}

			builder = new StringBuilder();
			Iterator<String> it = versionList.keys();
			while (it.hasNext()) {
				JSONObject currentVersion = versionList.getJSONObject(it.next());
				builder.append(currentVersion.getString("version"));
				builder.append("\n");
				builder.append(currentVersion.getString("text"));
				builder.append("\n\n");
			}
			wholeChangelog = builder.toString().trim();
		} catch (JSONException e) {
			e.printStackTrace();
			Timber.e(e);
			//TODO handle error
		}

		Timber.i("Version info found: " + versionName + " -> " + changelog.replace("\n", "") + " everything: " + wholeChangelog.replace("\n", ""));
		AlertDialog dialog = buildChangelogDialog(context, context.getString(R.string.info_changelog_version, versionName), changelog, true, wholeChangelog);
		dialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		dialog.show();
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