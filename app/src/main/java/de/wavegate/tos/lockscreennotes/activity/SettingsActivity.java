package de.wavegate.tos.lockscreennotes.activity;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.view.MenuItem;

import de.wavegate.tos.lockscreennotes.R;
import de.wavegate.tos.lockscreennotes.util.NotesNotificationManager;
import de.wavegate.tos.lockscreennotes.view.fragment.SettingsFragment;

import static de.wavegate.tos.lockscreennotes.MainActivity.LOGTAG;

/**
 * Created by Nils on 01.09.2016.
 */

public class SettingsActivity extends PreferenceActivity {

	private AppCompatDelegate mDelegate;
	private boolean showNotifications;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getDelegate().installViewFactory();
		getDelegate().onCreate(savedInstanceState);
		setShowNotifications(true);

		ActionBar bar = getDelegate().getSupportActionBar();

		if (bar != null) {
			bar.setDisplayHomeAsUpEnabled(true);
			bar.setTitle(R.string.action_settings);
		} else {
			Log.w(LOGTAG, "Wanted to set up the AppBar. But the bar didn't exist.");
		}

		getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new SettingsFragment())
				.commit();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			// Respond to the action bar's Up/Home button
			case android.R.id.home:
				setShowNotifications(false);
				NavUtils.navigateUpFromSameTask(this);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public boolean isShowNotifications() {
		return showNotifications;
	}

	public void setShowNotifications(boolean showNotifications) {
		this.showNotifications = showNotifications;
	}

	private AppCompatDelegate getDelegate() {
		if (mDelegate == null) {
			mDelegate = AppCompatDelegate.create(this, null);
		}
		return mDelegate;
	}

	@Override
	public void onBackPressed() {
		setShowNotifications(false);
		super.onBackPressed();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (isShowNotifications())
			new NotesNotificationManager(this).showNotifications();
	}

	@Override
	protected void onResume() {
		super.onResume();
		new NotesNotificationManager(this).hideNotifications();
		setShowNotifications(true);
	}
}
