package de.nilsfo.lockscreennotes.util;

import android.content.Context;
import android.view.MenuItem;
import android.view.View;

import de.nilsfo.lsn.R;

public class PermissionBalloonTimer {

	public static final long PERMISSION_BALLOON_DELAY = 250;

	private boolean balloonShownOnce;
	private boolean enabled;
	private final Context context;

	public PermissionBalloonTimer(Context context) {
		this.context = context;
		setEnabled(true);
		balloonShownOnce = false;
	}

	public boolean isEnabled() {
		return enabled;
	}

	private void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void disable() {
		setEnabled(false);
	}

	public boolean checkPermissionBalloonCondition(View checkNotificationPermissionItemView,
												   MenuItem checkNotificationPermissionItem) {
		NotesNotificationManager notesNotificationManager = new NotesNotificationManager(context);
		return isEnabled() &&
				//!notesNotificationManager.hasUserPermissionToDisplayNotifications() &&
				checkNotificationPermissionItemView != null &&
				checkNotificationPermissionItem.isVisible();
	}

	public boolean hasBalloonShownOnce() {
		return balloonShownOnce;
	}

	public void markBalloonShownOnce() {
		this.balloonShownOnce = true;
	}
}
