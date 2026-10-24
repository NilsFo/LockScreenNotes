package de.nilsfo.lockscreennotes.receiver;

import static de.nilsfo.lockscreennotes.io.backups.BackupManager.AUTO_DELETE_MAX_FILE_COUNT;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import de.nilsfo.lockscreennotes.io.StoragePermissionManager;
import de.nilsfo.lockscreennotes.io.backups.BackupFile;
import de.nilsfo.lockscreennotes.io.backups.BackupManager;
import de.nilsfo.lockscreennotes.receiver.alarms.LSNAlarmManager;
import de.nilsfo.lockscreennotes.util.NotesNotificationManager;
import de.nilsfo.lockscreennotes.util.TimeUtils;
import de.nilsfo.lsn.R;
import timber.log.Timber;

@Deprecated
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
			StoragePermissionManager storagePermissionManager = new StoragePermissionManager(context);
			if (storagePermissionManager.hasExternalStoragePermission()) {
				BackupFile f = null;
				try {
					f = backupManager.createAndWriteBackup();
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
					ArrayList<BackupFile> backups = new ArrayList<>();
					try {
						backups = backupManager.findBackupFiles();
					} catch (IOException e) {
						e.printStackTrace();
						Timber.e(e);
					}

					Timber.i("Checking if old files should be deleted. File count: " + backups.size());
					if (backups.size() > AUTO_DELETE_MAX_FILE_COUNT) {
						Collections.sort(backups, new Comparator<BackupFile>() {
							@Override
							public int compare(BackupFile file, BackupFile t1) {
								return String.valueOf(file.getLastModified()).compareTo(String.valueOf(t1.getLastModified()));
							}
						});

						Timber.i("Too many files! Deleting...");
						boolean success = true;
						int deletedCount = 0;

						while (backups.size() > AUTO_DELETE_MAX_FILE_COUNT) {
							BackupFile current = backups.get(0);
							backups.remove(0);

							// success &= current.delete();
							Timber.e("TODO: Delete");
							success = false;

							deletedCount++;
							Timber.i("In an attempt to auto-clean up old backup files, this file was attempted to be deleted: " + f.getName());
						}
						Timber.i("Deleted old files. Count: " + deletedCount + " All successfull: " + success);

						if (!success) {
							notificationManager.displayNotificationAutomaticBackup(true, context.getString(R.string.error_no_old_backup_files_deleted));
						}
					}

					if (showNotification) {
						LSNAlarmManager alarmManager = new LSNAlarmManager(context);

						int days = Integer.valueOf(preferences.getString("pref_auto_backups_schedule_days", "3"));
						days = Math.max(days, 1);
						long interval = AlarmManager.INTERVAL_DAY * days;
						Date d = new Date(new Date().getTime() + alarmManager.getTimeUntilNextNewAutoBackup() + interval);
						String formattedDate = new TimeUtils(context).formatDateAbsolute(d, DateFormat.FULL);

						String text = context.getString(R.string.notification_auto_backup_next, formattedDate);
						notificationManager.displayNotificationAutomaticBackup(true, text);
					}
				}
			} else {
				Timber.w("Schedule received, but the user has not granted the permission to write the external storage!");
				String errorText = context.getString(R.string.error_backup_directory_access_failed);
				notificationManager.displayNotificationAutomaticBackup(false, errorText);
			}

			notificationManager.showNoteNotifications();
		}
	}
}
