package de.nilsfo.lockscreennotes.io.backups;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

public final class BackupLocationStore {

	private static final String PREF_BACKUP_TREE_URI =
			"backup_storage_tree_uri";
	private Context context;

	public BackupLocationStore(@NonNull Context context) {
		this.context = context;
	}

	@Nullable
	public Uri getTreeUri() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(
				context.getApplicationContext()
		);
		String value = preferences.getString(PREF_BACKUP_TREE_URI, null);

		if (value == null || value.isEmpty()) {
			return null;
		}

		return Uri.parse(value);
	}

	public void setTreeUri(@NonNull Uri uri) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(
				context.getApplicationContext()
		);
		preferences.edit()
				.putString(PREF_BACKUP_TREE_URI, uri.toString())
				.apply();
	}

	public void clearTreeUri() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(
				context.getApplicationContext()
		);
		preferences.edit()
				.remove(PREF_BACKUP_TREE_URI)
				.apply();
	}
}