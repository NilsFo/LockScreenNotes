package de.nilsfo.lockscreennotes.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import de.nilsfo.lockscreennotes.LockScreenNotes;

/**
 * Created by Nils on 16.08.2016.
 */

public abstract class NotesActivity extends AppCompatActivity {

	private boolean showNotifications;

	public boolean isShowNotifications() {
		return showNotifications;
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		WindowCompat.enableEdgeToEdge(getWindow());

		// Ensure system bar icons have enough contrast
		WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
		boolean isDarkMode = LockScreenNotes.isDarkMode(this);
		controller.setAppearanceLightStatusBars(false); // We have a dark/yellow Toolbar, so always use light status bar icons
		controller.setAppearanceLightNavigationBars(!isDarkMode); // Dark icons in light mode (on white bg), Light icons in dark mode
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
