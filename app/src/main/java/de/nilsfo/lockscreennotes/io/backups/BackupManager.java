package de.nilsfo.lockscreennotes.io.backups;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import de.nilsfo.lockscreennotes.data.Note;
import de.nilsfo.lockscreennotes.io.FileManager;
import de.nilsfo.lockscreennotes.util.TimeUtils;
import de.nilsfo.lockscreennotes.util.VersionManager;
import de.nilsfo.lsn.R;
import timber.log.Timber;

import static de.nilsfo.lockscreennotes.io.backups.NoteJSONUtils.VERSION_NOT_AVAILABLE;

public class BackupManager {

	public static final int AUTO_DELETE_MAX_FILE_COUNT = 20;

	private Context context;
	private FileManager manager;

	public BackupManager(Context context) {
		this.context = context;
		manager = new FileManager(context);
	}

	public File completeBackup() throws JSONException, IOException {
		JSONObject data;
		data = NoteJSONUtils.toJSON(context);
		Timber.i("Transforming notes to JSON: " + data.toString());

		manager = new FileManager(context);
		File f = new File(manager.getNoteBackupDir(), manager.getDynamicNoteBackupFilename());
		Timber.i("Exporting JSON notes to: " + f.getAbsolutePath());

		FileWriter fw = new FileWriter(f);
		fw.write(data.toString());
		fw.close();

		manager.notifyMediaScanner(f);
		return f;
	}

	public ArrayList<File> findBackupFiles() {
		ArrayList<File> list = new ArrayList<>();

		File dir = manager.getNoteBackupDir();
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

	public boolean hasBackupsMade() {
		return !findBackupFiles().isEmpty();
	}

	public BackupMetaData getMetaData(File file) throws IOException, JSONException {
		JSONObject data = new JSONObject(FileManager.readFile(file));
		return new BackupMetaData(data, context);
	}

	public ArrayList<Note> readBackupFile(File file) throws IOException, JSONException {
		JSONObject data = new JSONObject(FileManager.readFile(file));
		BackupMetaData metaData = new BackupMetaData(data, context);

		JSONArray notes = data.getJSONArray("notes");
		return NoteJSONUtils.toNotes(notes, metaData.version, VersionManager.getCurrentVersion(context));
	}

	public boolean hasExternalStoragePermission() {
		return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
	}

	@Nullable
	public File getLatestBackupFile() {
		ArrayList<File> list = findBackupFiles();
		Collections.sort(list, new Comparator<File>() {
			@Override
			public int compare(File f1, File f2) {
				long lastModifiedF1 = f1.lastModified();
				long lastModifiedF2 = f2.lastModified();

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
					return Long.compare(lastModifiedF2, lastModifiedF1);
				}

				String s1 = String.valueOf(f2);
				String s2 = String.valueOf(f1);
				return s1.compareTo(s2);
			}
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

	public class BackupMetaData {
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
