package de.nilsfo.lockscreennotes.io;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.nilsfo.lsn.R;
import timber.log.Timber;

public class FileManager {

	public static final String NO_MEDIA = ".nomedia";
	public static final String FOLDERNAME_BACKUP = "backups";

	public static final String FILENAME_BACKUP_BASE_NAME = "lsn_backup_";
	public static final String FILENAME_TIMESTAMP_FORMATTER = "yyyy_MM_dd-hh_mm_ss";
	public static final String FILENAME_BACKUP_EXTENSION = ".json";

	private Context context;

	public FileManager(Context context) {
		this.context = context;
	}

	public File getExternalDir() {
		File f = Environment.getExternalStorageDirectory();
		//TODO does this parent file thing work?

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
			f = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
		}
		f = new File(f, context.getString(R.string.app_name));

		Timber.i("Requested external home directory. Location: " + f.getAbsolutePath());

		/*
		if (!f.exists()) {
			if (!f.mkdirs()) {
				Timber.e("Directory not created");
			} else {
				Timber.i("External origin dir was created without problems.");
			}
		} else {
			Timber.i("External origin dir already exists. No need to create.");
		}
		createNoMediaFile(f);
		*/
		createDirectory(f, true);

		return f;
	}

	public File getInternalDir() {
		File f = context.getExternalFilesDir(null);
		createDirectory(f, true);
		return f;
	}

	@Deprecated
	public void deleteCache() {
		File dir = getCacheDir();
		try {
			deleteDir(dir);
		} catch (Exception e) {
			e.printStackTrace();
			Timber.e(e, "Failed to delete the cache dir: " + dir.getAbsolutePath());
		}
	}

	public File getCacheDir() {
		return context.getCacheDir();
	}

	public File getNoteBackupDir() {
		File f = new File(getExternalDir(), FOLDERNAME_BACKUP);
		createDirectory(f, true);
		return f;
	}

	public String getDynamicNoteBackupFilename() {
		SimpleDateFormat sdf = new SimpleDateFormat(FILENAME_TIMESTAMP_FORMATTER);
		String date = sdf.format(new Date());
		return FILENAME_BACKUP_BASE_NAME + date + FILENAME_BACKUP_EXTENSION;
	}

	/*
	public File getNameGeneratorCache() {
		File f = new File(getCacheDir(), DIR_NAME_GENERATOR_SOURCES);

		if (!f.exists()) {
			boolean works = f.mkdirs();
			works |= createNoMediaFile(f);

			if (!works) {
				Timber.i("Errors regarding the cache dir. Either it could not have been created, or it had problems with its NOMEDIA file.");
			}
		}
		return f;
	}

	public File getCampaignBackupDir() {
		File f = new File(getOriginDir(), CAMPAIGN_BACKUP_DIR_NAME);
		if (!f.exists()) f.mkdirs();
		createNoMediaFile(f);

		return f;
	}

	public File getNameGeneratorSources() {
		File f = new File(getOriginDir(), DIR_NAME_GENERATOR_SOURCES);
		if (!f.exists()) f.mkdirs();
		createNoMediaFile(f);

		return f;
	}
	*/

	public void browseFolder(File file) {
		/*
		Log.i(LOGTAG, "Requesting to browse file: " + file.getAbsolutePath());
		//Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		Intent intent = new Intent(Intent.ACTION_VIEW);
		//Uri uri = Uri.parse(Environment.getExternalStorageDirectory().getPath()
		//		+ "/myFolder/");

		intent.setDataAndType(Uri.parse(file.getAbsolutePath()), "resource/folder");
		context.startActivity(Intent.createChooser(intent, "Open folder"));
		Uri selectedUri = Uri.parse(Environment.getExternalStorageDirectory() + "/myFolder/");
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(selectedUri, "resource/folder");
		context.startActivity(Intent.createChooser(intent, "Open folder"));
		*/
		String folderPath = file.getAbsolutePath();

		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_GET_CONTENT);
		Uri myUri = Uri.parse(folderPath);
		intent.setDataAndType(myUri, "file/*");
		context.startActivity(intent);
	}

	public boolean createDirectory(File f, boolean createNoMedia) {
		boolean works;
		if (!f.exists()) {
			if (!f.mkdirs()) {
				Timber.e("Directory not created");
				works = false;
			} else {
				Timber.i("External origin dir was created without problems.");
				works = true;
			}
		} else {
			Timber.i("External origin dir already exists. No need to create.");
			works = true;
		}

		Timber.i("File? " + f.exists() + " - " + f.isDirectory());

		if (createNoMedia) works &= createNoMediaFile(f);
		notifyMediaScanner(f);
		return works;
	}

	public boolean createNoMediaFile(File parent) {
		if (!parent.exists() || !parent.isDirectory()) return false;

		File f = new File(parent, NO_MEDIA);
		if (f.exists()) return true;

		boolean created;
		try {
			created = f.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			Timber.e(e, "Wanted to create 'NomediaFile' in " + parent.getAbsolutePath() + " but it failed!");
			return false;
		}
		if (created) {
			notifyMediaScanner(f);
		}
		Timber.i("Creating a .nomedia file at: " + f.getAbsolutePath() + " resulted in a success? -> " + created);

		return created;
	}

	public boolean deleteDir(File dir) {
		if (dir != null && dir.isDirectory()) {
			String[] children = dir.list();
			for (String aChildren : children) {
				boolean success = deleteDir(new File(dir, aChildren));
				if (!success) {
					return false;
				}
			}
			return dir.delete();
		} else
			return dir != null && dir.isFile() && dir.delete();
	}

	public void notifyMediaScanner(File file) {
		if (file.isDirectory()) {
			Timber.w("Wanted to scan an (empty) directory. Scan interrupted.");
			return;
		}
		MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()}, null, null);
	}

	public boolean isEmptyDirectory(File file) {
		if (!file.isDirectory()) return false;

		File[] files = file.listFiles();
		return files == null || file.length() > 0;
	}

	public static String readFile(File file) throws IOException {
		StringBuilder builder = new StringBuilder();
		BufferedReader br = new BufferedReader(new FileReader(file));
		for (String line; (line = br.readLine()) != null; ) {
			builder.append(line);
		}
		return builder.toString();
	}

	/**
	 * Copy & Paste by https://stackoverflow.com/questions/7856959/android-file-chooser
	 */
	public String getPath(Uri uri) throws URISyntaxException {
		if ("content".equalsIgnoreCase(uri.getScheme())) {
			String[] projection = {"_data"};
			Cursor cursor = null;

			try {
				cursor = context.getContentResolver().query(uri, projection, null, null, null);
				int column_index = cursor.getColumnIndexOrThrow("_data");
				if (cursor.moveToFirst()) {
					return cursor.getString(column_index);
				}
			} catch (Exception e) {
				// Eat it
			}
		} else if ("file".equalsIgnoreCase(uri.getScheme())) {
			return uri.getPath();
		}

		return null;
	}

	//public void notifyMediaScanner(File file) {
	//	Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
	//	intent.setData(Uri.fromFile(file));
	//	context.sendBroadcast(intent);
	//}

}