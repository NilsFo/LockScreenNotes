package de.nilsfo.lockscreennotes.io.backups;

import static de.nilsfo.lockscreennotes.io.backups.NoteJSONUtils.VERSION_NOT_AVAILABLE;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.nilsfo.lockscreennotes.data.Note;
import de.nilsfo.lockscreennotes.util.VersionManager;
import de.nilsfo.lsn.R;
import timber.log.Timber;

public class BackupManager {

	public static final int AUTO_DELETE_MAX_FILE_COUNT = 20;

	public static final String FILENAME_BACKUP_BASE_NAME = "lsn_backup_";
	public static final String FILENAME_BACKUP_EXTENSION = ".json";

	private static final String FILENAME_TIMESTAMP_FORMATTER = "yyyy_MM_dd-HH_mm_ss";

	private final Context context;
	private final BackupStorage storage;

	public BackupManager(@NonNull Context context) {
		this(
				context,
				new SafBackupStorage(context.getApplicationContext())
		);
	}

	private BackupManager(@NonNull Context context, @NonNull BackupStorage storage
	) {
		this.context = context.getApplicationContext();
		this.storage = storage;
	}

	@NonNull
	public BackupStorageState getStorageState() {
		return storage.getState();
	}

	@NonNull
	public BackupFile createAndWriteBackup()
			throws JSONException, IOException {

		JSONObject data = NoteJSONUtils.toJSON(context);

		Timber.i("Transforming notes to JSON: %s", data);

		BackupFile backup = storage.writeBackup(
				createBackupFilename(),
				data.toString()
		);

		/*
		 * Only prune AFTER the new backup has been completely written.
		 * A failed backup must never destroy an existing recovery point.
		 */
		pruneOldBackups();

		return backup;
	}

	@NonNull
	public ArrayList<BackupFile> findBackupFiles() throws IOException {
		ArrayList<BackupFile> result =
				new ArrayList<>(storage.findBackups());

		Timber.i("Backup count: %d", result.size());

		return result;
	}

	public boolean hasBackupsMade() throws IOException {
		return !findBackupFiles().isEmpty();
	}

	@NonNull
	public BackupMetaData getMetaData(@NonNull BackupFile file)
			throws IOException, JSONException {

		JSONObject data = readBackupJson(file);

		return new BackupMetaData(data, context);
	}

	@NonNull
	public ArrayList<Note> readBackupFile(@NonNull BackupFile file)
			throws IOException, JSONException {

		JSONObject data = readBackupJson(file);
		BackupMetaData metaData = new BackupMetaData(data, context);

		JSONArray notes = data.getJSONArray("notes");

		return NoteJSONUtils.toNotes(
				notes,
				metaData.version,
				VersionManager.getCurrentVersion(context)
		);
	}

	@Nullable
	public BackupFile getLatestBackupFile() throws IOException {
		List<BackupFile> backups = storage.findBackups();

		BackupFile latest = null;
		long latestTimestamp = Long.MIN_VALUE;

		for (BackupFile backup : backups) {
			long timestamp = getBackupTimestamp(backup);

			if (latest == null || timestamp > latestTimestamp) {
				latest = backup;
				latestTimestamp = timestamp;
			}
		}

		if (latest != null) {
			Timber.i("Latest backup: %s", latest.getName());
		}

		return latest;
	}

	private void pruneOldBackups() throws IOException {
		List<BackupFile> backups = storage.findBackups();

		if (backups.size() <= AUTO_DELETE_MAX_FILE_COUNT) {
			return;
		}

		// Compare backups based on LastModified.
		// Depending on android version this might be more or less different / efficient
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			backups.sort(
					Comparator.comparingLong(this::getBackupTimestamp)
							.reversed()
			);
		} else {
			Collections.sort(backups, new Comparator<BackupFile>() {
				@Override
				public int compare(BackupFile file1, BackupFile file2) {
					return Long.compare(file1.getLastModified(), file2.getLastModified());
					//return String.valueOf(file1.getLastModified()).compareTo(String.valueOf(file2.getLastModified()));
				}
			});
		}

		for (int i = AUTO_DELETE_MAX_FILE_COUNT; i < backups.size(); i++) {
			BackupFile oldBackup = backups.get(i);

			if (!storage.deleteBackup(oldBackup)) {
				Timber.w(
						"Unable to delete old backup: %s",
						oldBackup.getName()
				);
			}
		}
	}

	private long getBackupTimestamp(@NonNull BackupFile backup) {
		try {
			return getMetaData(backup).getTimestamp();
		} catch (IOException | JSONException e) {
			Timber.w(e, "Unable to read timestamp from %s; using provider timestamp.",
					backup.getName()
			);

			return backup.getLastModified();
		}
	}

	@NonNull
	private JSONObject readBackupJson(@NonNull BackupFile backup)
			throws IOException, JSONException {
		return new JSONObject(storage.readBackup(backup));
	}

	@NonNull
	private String createBackupFilename() {
		SimpleDateFormat format = new SimpleDateFormat(
				FILENAME_TIMESTAMP_FORMATTER,
				Locale.US
		);

		return FILENAME_BACKUP_BASE_NAME
				+ format.format(new Date())
				+ FILENAME_BACKUP_EXTENSION;
	}

	public static class BackupMetaData {

		private long timestamp;
		private String count;
		private int version;

		public BackupMetaData(JSONObject data, Context context) {
			try {
				timestamp = data.getLong("timestamp");
			} catch (JSONException e) {
				Timber.e(e);
				timestamp = 0;
			}

			try {
				count = String.valueOf(data.getInt("count"));
			} catch (JSONException e) {
				Timber.e(e);
				count = context.getString(R.string.error_unknown);
			}

			try {
				version = data.getInt("version");
			} catch (JSONException e) {
				Timber.e(e);
				version = VERSION_NOT_AVAILABLE;
			}
		}

		public long getTimestamp() {
			return timestamp;
		}

		public String getCount() {
			return count;
		}

		public int getVersion() {
			return version;
		}
	}
}