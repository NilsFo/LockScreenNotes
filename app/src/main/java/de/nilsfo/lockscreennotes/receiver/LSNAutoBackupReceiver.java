package de.nilsfo.lockscreennotes.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import de.nilsfo.lockscreennotes.io.backups.BackupManager;
import de.nilsfo.lockscreennotes.util.NotesNotificationManager;
import de.nilsfo.lsn.R;
import timber.log.Timber;

import static de.nilsfo.lockscreennotes.io.backups.BackupManager.AUTO_DELETE_MAX_FILE_COUNT;

public class LSNAutoBackupReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Timber.i("LSNAutoBackupReceiver: Alarm received! It's about: " + intent.getAction());

		if (intent.getAction().equals(LSNAutoBackupReceiver.class.getCanonicalName())) {
			NotesNotificationManager notificationManager = new NotesNotificationManager(context);

			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			boolean deleteOld = preferences.getBoolean("pref_auto_backups_delete_old", true);
			boolean showNotification = preferences.getBoolean("pref_auto_backups_notification_enabled", true);

			Timber.i("LSNAutoBackupReceiver: Alarm received! [Confirmed AutoBackupIntent] - Params: deleteOld: " + deleteOld + " notification: " + showNotification);

			BackupManager backupManager = new BackupManager(context);
			if (backupManager.hasExternalStoragePermission()) {
				File f = null;
				try {
					f = backupManager.completeBackup();
				} catch (Exception e) {
					e.printStackTrace();
					Timber.e(e);

					notificationManager.displayNotificationAutomaticBackup(false, context.getString(R.string.error_internal_error));
					return;
				}

				if (f == null || !f.exists()) {
					notificationManager.displayNotificationAutomaticBackup(false, context.getString(R.string.error_internal_error));
					return;
				}

				if (deleteOld) {
					ArrayList<File> backups = backupManager.findBackupFiles();
					Timber.i("Checking if old files should be deleted. File count: " + backups.size());
					if (backups.size() > AUTO_DELETE_MAX_FILE_COUNT) {
						Collections.sort(backups, new Comparator<File>() {
							@Override
							public int compare(File file, File t1) {
								return String.valueOf(file.lastModified()).compareTo(String.valueOf(t1.lastModified()));
							}
						});

						Timber.i("Too many files! Deleting...");
						boolean success = true;
						int deletedCount = 0;

						while (backups.size() > AUTO_DELETE_MAX_FILE_COUNT) {
							File current = backups.get(0);
							backups.remove(0);
							success &= current.delete();
							deletedCount++;
							Timber.i("In an attempt to auto-clean up old backup files, this file was attempted to be deleted: " + f.getAbsolutePath());
						}
						Timber.i("Deleted old files. Count: " + deletedCount + " All successfull: " + success);

						if (!success) {
							notificationManager.displayNotificationAutomaticBackup(true, context.getString(R.string.error_no_old_backup_files_deleted));
						}
					}

					if (showNotification) {
						notificationManager.displayNotificationAutomaticBackup(true, f.getAbsolutePath());
					}

				} else {
					Timber.w("Schedule recieved, but the user has not granted the permission to write the external storage!");
					notificationManager.displayNotificationAutomaticBackup(false, context.getString(R.string.error_no_external_storage));
				}
			}

			notificationManager.showNoteNotifications();
		}
	}
}
