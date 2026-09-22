package de.nilsfo.lockscreennotes.io.backups;

import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.Objects;

import timber.log.Timber;

/**
 * Reference to a backup document managed through Android's Storage Access Framework.
 */
public final class BackupFile {

	private final Uri uri;
	private final String name;
	private final long lastModified;

	public BackupFile(@NonNull Uri uri, @NonNull String name, long lastModified) {
		this.uri = Objects.requireNonNull(uri);
		this.name = Objects.requireNonNull(name);
		this.lastModified = lastModified;
	}

	@NonNull
	public Uri getUri() {
		return uri;
	}

	@NonNull
	public String getName() {
		return name;
	}

	public long getLastModified() {
		return lastModified;
	}

	@Override
	public String toString() {
		return name + " (" + uri + ")";
	}

	public boolean exists() {
		// TODO implement
		Timber.e("TODO: Implement");
		return true;
	}

}