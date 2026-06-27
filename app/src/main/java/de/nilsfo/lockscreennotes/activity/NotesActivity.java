package de.nilsfo.lockscreennotes.activity;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import de.nilsfo.lockscreennotes.LockScreenNotes;
import de.nilsfo.lsn.R;

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

		// Status bar color matching the ActionBar (yellow in light, dark in dark mode)
		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
		window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));

		// Dark icons in light mode, light icons in dark mode
		WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
		if (controller != null) {
			controller.setAppearanceLightStatusBars(!LockScreenNotes.isDarkMode(this));
		}

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
