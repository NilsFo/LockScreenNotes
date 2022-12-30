package de.nilsfo.lockscreennotes.util;

import static androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC;
import static de.nilsfo.lockscreennotes.LockScreenNotes.APP_TAG;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import de.nilsfo.lsn.R;
import timber.log.Timber;

public class NotificationChannelManager {

	public static final String CHANNEL_TAG = APP_TAG + "notification_channel.";
	public static final String CHANNEL_GROUPS_TAG = CHANNEL_TAG + "groups.";

	public static final String CHANNEL_GROUPS_NOTES_ID = CHANNEL_GROUPS_TAG + "notes";
	public static final String CHANNEL_GROUPS_MISC_ID = CHANNEL_GROUPS_TAG + "miscellaneous";

	public static final String CHANNEL_ID_NOTES_LOW = CHANNEL_TAG + "notes_low";
	public static final String CHANNEL_ID_NOTES_MEDIUM = CHANNEL_TAG + "notes_medium";
	public static final String CHANNEL_ID_NOTES_HIGH = CHANNEL_TAG + "notes_high";

	public static final String CHANNEL_ID_AUTO_BACKUP_CHANNEL = CHANNEL_TAG + "auto_backups";

	private Context context;

	public NotificationChannelManager(Context context) {
		this.context = context;
	}

	public boolean requestSetUpChannels() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			if (notificationManager == null) {
				Timber.w("Failed to get the notification manager. Notifications not supported on this device!");
				return false;
			}

			notificationManager.createNotificationChannelGroup(new NotificationChannelGroup(CHANNEL_GROUPS_NOTES_ID, context.getString(R.string.info_channel_groups_notes)));
			notificationManager.createNotificationChannelGroup(new NotificationChannelGroup(CHANNEL_GROUPS_MISC_ID, context.getString(R.string.info_channel_groups_misc)));

			//Notes channel
			//Notes - Priority: Low
			NotificationChannel channelNotesLow = new NotificationChannel(CHANNEL_ID_NOTES_LOW, context.getString(R.string.info_channel_notes_low_name), NotificationManager.IMPORTANCE_MIN);
			channelNotesLow.setImportance(NotificationManager.IMPORTANCE_MIN);
			channelNotesLow.setDescription(context.getString(R.string.info_channel_notes_description));
			channelNotesLow.setGroup(CHANNEL_GROUPS_NOTES_ID);
			channelNotesLow.setBypassDnd(false);
			channelNotesLow.setShowBadge(false);
			channelNotesLow.setLockscreenVisibility(VISIBILITY_PUBLIC);

			//Notes - Priority: Medium
			NotificationChannel channelNotesMedium = new NotificationChannel(CHANNEL_ID_NOTES_MEDIUM, context.getString(R.string.info_channel_notes_medium_name), NotificationManager.IMPORTANCE_LOW);
			channelNotesMedium.setImportance(NotificationManager.IMPORTANCE_LOW);
			channelNotesMedium.setDescription(context.getString(R.string.info_channel_notes_description));
			channelNotesMedium.setGroup(CHANNEL_GROUPS_NOTES_ID);
			channelNotesMedium.setBypassDnd(false);
			channelNotesMedium.setShowBadge(false);
			channelNotesMedium.setLockscreenVisibility(VISIBILITY_PUBLIC);

			//Notes - Priority: High
			NotificationChannel channelNotesHigh = new NotificationChannel(CHANNEL_ID_NOTES_HIGH, context.getString(R.string.info_channel_notes_high_name), NotificationManager.IMPORTANCE_LOW);
			channelNotesHigh.setImportance(NotificationManager.IMPORTANCE_LOW);
			channelNotesHigh.setDescription(context.getString(R.string.info_channel_notes_description));
			channelNotesHigh.setGroup(CHANNEL_GROUPS_NOTES_ID);
			channelNotesHigh.setBypassDnd(true);
			channelNotesHigh.setShowBadge(true);
			channelNotesHigh.setLockscreenVisibility(VISIBILITY_PUBLIC);
			channelNotesHigh.setLightColor(context.getColor(R.color.colorNotificationLight));

			notificationManager.createNotificationChannel(channelNotesLow);
			notificationManager.createNotificationChannel(channelNotesMedium);
			notificationManager.createNotificationChannel(channelNotesHigh);

			//Miscellaneous channel group:
			//Automatic backups channel:
			NotificationChannel channelAutoBackups = new NotificationChannel(CHANNEL_ID_AUTO_BACKUP_CHANNEL, context.getString(R.string.info_channel_auto_backup_name), NotificationManager.IMPORTANCE_LOW);
			channelAutoBackups.setImportance(NotificationManager.IMPORTANCE_LOW);
			channelAutoBackups.setDescription(context.getString(R.string.info_channel_auto_backup_description));
			channelAutoBackups.setGroup(CHANNEL_GROUPS_MISC_ID);
			channelAutoBackups.setBypassDnd(false);
			channelAutoBackups.setShowBadge(true);
			channelAutoBackups.setLockscreenVisibility(VISIBILITY_PUBLIC);
			channelAutoBackups.setLightColor(context.getColor(R.color.colorNotificationLight));
			notificationManager.createNotificationChannel(channelAutoBackups);
		}
		return false;
	}

	public String getNoteChannel(int priority) {
		String name;
		switch (priority) {
			case NotificationCompat.PRIORITY_MIN:
				name = CHANNEL_ID_NOTES_LOW;
				break;
			case NotificationCompat.PRIORITY_MAX:
				name = CHANNEL_ID_NOTES_HIGH;
				break;
			case NotificationCompat.PRIORITY_DEFAULT:
				name = CHANNEL_ID_NOTES_MEDIUM;
				break;
			default:
				Timber.w("Illiegal argument to parse priority to importance!");
				name = CHANNEL_ID_NOTES_MEDIUM;
				break;
		}
		return name;
	}

}
