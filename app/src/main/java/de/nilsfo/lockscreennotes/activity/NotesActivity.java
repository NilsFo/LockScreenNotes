package de.nilsfo.lockscreennotes.activity;

import androidx.appcompat.app.AppCompatActivity;

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
