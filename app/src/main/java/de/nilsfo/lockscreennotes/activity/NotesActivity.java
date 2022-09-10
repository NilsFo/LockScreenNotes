package de.nilsfo.lockscreennotes.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

/**
 * Created by Nils on 16.08.2016.
 */

public abstract class NotesActivity extends AppCompatActivity {

	private boolean showNotifications;

	public boolean isShowNotifications() {
		return showNotifications;
	}

	public void setShowNotifications(boolean showNotifications) {
		this.showNotifications = showNotifications;
	}

	@Override
	protected void onResume() {
		setShowNotifications(true);
		super.onResume();
	}
}
