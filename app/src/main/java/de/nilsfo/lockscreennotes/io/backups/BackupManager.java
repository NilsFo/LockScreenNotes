package de.nilsfo.lockscreennotes.io.backups;

import static de.nilsfo.lockscreennotes.io.backups.NoteJSONUtils.VERSION_NOT_AVAILABLE;

import android.content.Context;
import android.os.Build;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import de.nilsfo.lockscreennotes.data.Note;
import de.nilsfo.lockscreennotes.io.FileManager;
import de.nilsfo.lockscreennotes.io.StoragePermissionManager;
import de.nilsfo.lockscreennotes.io.StoragePermissionManager.InsufficientStoragePermissionException;
import de.nilsfo.lockscreennotes.util.TimeUtils;
import de.nilsfo.lockscreennotes.util.VersionManager;
import de.nilsfo.lsn.R;
import timber.log.Timber;

public class BackupManager {

	public static final int AUTO_DELETE_MAX_FILE_COUNT = 20;

	private final Context context;
	private final FileManager fileManager;
	private final StoragePermissionManager storagePermissionManager;

	public BackupManager(Context context) {
		this.context = context;
		fileManager = new FileManager(context);
		storagePermissionManager = new StoragePermissionManager(context);
	}

	public File createAndWriteBackup() throws JSONException, IOException, InsufficientStoragePermissionException {
		if (!storagePermissionManager.hasExternalStoragePermission()) {
			throw new InsufficientStoragePermissionException("Insufficient app permissions.");
		}

		JSONObject data = NoteJSONUtils.toJSON(context);
		Timber.i("Transforming notes to JSON: " + data.toString());

		File f = new File(fileManager.getNoteBackupDir(), fileManager.getDynamicNoteBackupFilename());
		Timber.i("Exporting JSON notes to: " + f.getAbsolutePath());

		FileWriter fw = new FileWriter(f);
		fw.write(data.toString());
		fw.close();

		fileManager.notifyMediaScanner(f);
		return f;
	}

	public ArrayList<File> findBackupFiles() throws InsufficientStoragePermissionException {
		if (!storagePermissionManager.hasExternalStoragePermission()) {
			throw new InsufficientStoragePermissionException("Insufficient app permissions.");
		}
		ArrayList<File> list = new ArrayList<>();

		File dir = fileManager.getNoteBackupDir();
		File[] files = dir.listFiles();
		if (files == null) {
			return list;
		}

		for (File f : files) {
			String name = f.getAbsolutePath().toLowerCase();
			if (name.endsWith(FileManager.FILENAME_BACKUP_EXTENSION)) {
				list.add(f);
			}
		}

		Timber.i("List of backup Files found: " + Arrays.toString(list.toArray()));
		Timber.i("Count: " + list.size());
		return list;
	}

	public boolean hasBackupsMade() throws InsufficientStoragePermissionException {
		return !findBackupFiles().isEmpty();
	}

	public BackupMetaData getMetaData(File file) throws IOException, JSONException, InsufficientStoragePermissionException {
		if (!storagePermissionManager.hasExternalStoragePermission()) {
			throw new InsufficientStoragePermissionException("Insufficient app permissions.");
		}

		JSONObject data = new JSONObject(FileManager.readFile(file));
		return new BackupMetaData(data, context);
	}

	public ArrayList<Note> readBackupFile(File file) throws IOException, JSONException, InsufficientStoragePermissionException {
		if (!storagePermissionManager.hasExternalStoragePermission()) {
			throw new InsufficientStoragePermissionException("Insufficient app permissions.");
		}

		JSONObject data = new JSONObject(FileManager.readFile(file));
		BackupMetaData metaData = new BackupMetaData(data, context);

		JSONArray notes = data.getJSONArray("notes");
		return NoteJSONUtils.toNotes(notes, metaData.version, VersionManager.getCurrentVersion(context));
	}

	@Nullable
	public File getLatestBackupFile() throws InsufficientStoragePermissionException {
		ArrayList<File> list = findBackupFiles();
		Collections.sort(list, (file1, file2) -> {
			long lastModifiedF1 = file1.lastModified();
			long lastModifiedF2 = file2.lastModified();

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				return Long.compare(lastModifiedF2, lastModifiedF1);
			}

			String s1 = String.valueOf(file2);
			String s2 = String.valueOf(file1);
			return s1.compareTo(s2);
		});

		Timber.i("Here's the reversed list state:");
		TimeUtils timeUtils = new TimeUtils(context);
		for (File f : list) {
			long date = f.lastModified();
			Timber.i(f.getName() + " -> " + timeUtils.formatDateAccordingToPreferences(date));
		}

		if (list.isEmpty()) return null;
		File f = list.get(0);
		Timber.i("Assumed latest Backup File: " + f.getAbsolutePath());
		return f;
	}

	public static class BackupMetaData {
		private long timestamp;
		private String count;
		private int version;

		public BackupMetaData(JSONObject data, Context context) {
			try {
				timestamp = data.getLong("timestamp");
			} catch (JSONException e) {
				e.printStackTrace();
				Timber.e(e);
				timestamp = 0;
			}
			try {
				count = String.valueOf(data.getInt("count"));
			} catch (JSONException e) {
				e.printStackTrace();
				Timber.e(e);
				count = context.getString(R.string.error_unknown);
			}
			try {
				version = data.getInt("version");
			} catch (JSONException e) {
				e.printStackTrace();
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
