package de.nilsfo.lockscreennotes.io.backups;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import timber.log.Timber;

public final class BackupLocationStore {

	private static final String PREF_BACKUP_TREE_URI = "backup_storage_tree_uri";
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
		Timber.i("External Storage URI saved: " + uri.getPath());
	}

	public void clearTreeUri() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(
				context.getApplicationContext()
		);
		preferences.edit()
				.remove(PREF_BACKUP_TREE_URI)
				.apply();
		Timber.i("External Storage URI cleared!");
	}

	public boolean hastTree() {
		return getTreeUri() != null;
	}

	public String getHumanReadableURI() {
		String folderName = null;
		Uri uri = getTreeUri();

		DocumentFile directory = DocumentFile.fromTreeUri(context, uri);
		if (directory != null) {
			folderName = directory.getName();
		}

		String providerName = null;

		String authority = uri.getAuthority();
		if (authority != null) {
			PackageManager pm = context.getPackageManager();

			ProviderInfo provider = pm.resolveContentProvider(authority, 0);
			if (provider != null) {
				CharSequence label = provider.loadLabel(pm);
				if (label != null) {
					providerName = label.toString();
				}
			}
		}

		if (providerName != null && folderName != null) {
			return providerName + "/.../" + folderName;
		}

		if (folderName != null) {
			return folderName;
		}

		return uri.toString();
	}
}