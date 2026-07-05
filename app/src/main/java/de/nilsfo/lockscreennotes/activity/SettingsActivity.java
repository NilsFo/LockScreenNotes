package de.nilsfo.lockscreennotes.activity;

import static de.nilsfo.lockscreennotes.io.backups.BackupManager.AUTO_DELETE_MAX_FILE_COUNT;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.util.Date;
import java.util.List;

import de.nilsfo.lockscreennotes.LockScreenNotes;
import de.nilsfo.lockscreennotes.io.FileManager;
import de.nilsfo.lockscreennotes.receiver.alarms.LSNAlarmManager;
import de.nilsfo.lockscreennotes.util.NotesNotificationManager;
import de.nilsfo.lockscreennotes.util.TimeUtils;
import de.nilsfo.lockscreennotes.util.VersionManager;
import de.nilsfo.lockscreennotes.util.listener.SettingsBindPreferenceSummaryToValueListener;
import de.nilsfo.lsn.R;
import timber.log.Timber;

public class SettingsActivity extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

	public static final int LAST_UPDATE_DATE_UNKNOWN = -1;
	public static final SettingsBindPreferenceSummaryToValueListener defaultListener = new SettingsBindPreferenceSummaryToValueListener();
	private boolean showNotifications;
	private static final String TITLE_TAG = "settingsActivityTitle";
	private List<PreferenceActivity.Header> bufferedHeaders;

	/// ////////////////////////////////////////////////////////////////
	///
	/// Static helper functions
	///
	/// ////////////////////////////////////////////////////////////////

	public static void bindPreferenceURLAsAction(Preference preference, final Uri uri, final boolean chooser) {
		preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
				if (chooser) {
					browserIntent = Intent.createChooser(browserIntent, preference.getContext().getString(R.string.share_via));
				}

				preference.getContext().startActivity(browserIntent);
				return true;
			}
		});
	}

	public static void bindPreferenceURLAsAction(Preference preference, final Uri uri) {
		bindPreferenceURLAsAction(preference, uri, false);
	}

	public static void bindPreferenceURLAsAction(Preference preference) {
		String summary = preference.getSummary().toString();
		bindPreferenceURLAsAction(preference, Uri.parse(summary));
	}

	public static void bindPreferenceSummaryToValue(Preference preference) {
		bindPreferenceSummaryToValue(preference, null, null);
	}

	public static void bindPreferenceSummaryToValue(Preference preference, Runnable additionalAction) {
		bindPreferenceSummaryToValue(preference, null, additionalAction);
	}

	public static void bindPreferenceSummaryToValue(Preference preference, Integer resource) {
		bindPreferenceSummaryToValue(preference, resource, null);
	}

	public static void bindPreferenceSummaryToValue(Preference preference, Integer resource, Runnable additionalAction) {
		if (preference == null) return;

		SettingsBindPreferenceSummaryToValueListener listener = defaultListener;
		if (resource != null) {
			listener = new SettingsBindPreferenceSummaryToValueListener(resource);
		}

		preference.setOnPreferenceChangeListener(listener);

		if (additionalAction != null) {
			listener.setAdditionalAction(additionalAction);
		}

		listener.onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), preference.getContext().getString(R.string.error_unknown)));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction().replace(R.id.settings, new HeaderFragment()).commit();
		} else {
			setTitle(savedInstanceState.getCharSequence(TITLE_TAG));
		}
		getSupportFragmentManager().addOnBackStackChangedListener(() -> {
			int count = getSupportFragmentManager().getBackStackEntryCount();
			Timber.i("BackStack has changed! Current count: %d", count);
			if (count == 0) {
				updateTitle(getString(R.string.action_settings));
			} else {
				FragmentManager.BackStackEntry entry = getSupportFragmentManager().getBackStackEntryAt(count - 1);
				if (entry.getName() != null) {
					updateTitle(entry.getName());
				}
			}
		});

		getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				Timber.i("System back button pressed.");
				if (!getSupportFragmentManager().popBackStackImmediate()) {
					Timber.i("Nothing to pop from back stack. Finishing activity.");
					setEnabled(false);
					getOnBackPressedDispatcher().onBackPressed();
				}
			}
		});

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings_root_layout), (v, insets) -> {
			Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
			v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
			return insets;
		});

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}

	private void updateTitle(CharSequence title) {
		Timber.i("Updating title to: %s", title);
		setTitle(title);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setTitle(title);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		new NotesNotificationManager(this).hideAllNotifications();
		setShowNotifications(true);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (isShowNotifications())
			new NotesNotificationManager(this).showNoteNotifications();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// Save current activity title so we can set it again after a configuration change
		outState.putCharSequence(TITLE_TAG, getTitle());
	}

	@Override
	public boolean onSupportNavigateUp() {
		if (getSupportFragmentManager().popBackStackImmediate()) {
			return true;
		}
		return super.onSupportNavigateUp();
	}

	@Override
	public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, androidx.preference.Preference pref) {
		// Instantiate the new Fragment
		final Bundle args = pref.getExtras();
		final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(getClassLoader(), pref.getFragment());
		fragment.setArguments(args);
		fragment.setTargetFragment(caller, 0);

		// Replace the existing Fragment with the new Fragment
		String title = pref.getTitle() != null ? pref.getTitle().toString() : null;
		getSupportFragmentManager().beginTransaction().replace(R.id.settings, fragment).addToBackStack(title).commit();
		updateTitle(pref.getTitle());
		return true;
	}

	public boolean isShowNotifications() {
		return showNotifications;
	}

	public void setShowNotifications(boolean showNotifications) {
		this.showNotifications = showNotifications;
	}


	/// ////////////////////////////////////////////////////////////////
	///
	/// Home fragment
	///
	/// ////////////////////////////////////////////////////////////////

	public static class HeaderFragment extends PreferenceFragmentCompat {

		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			setPreferencesFromResource(R.xml.prefs_headers, rootKey);
			requestDarkThemeToBufferedHeaders();
		}

		@Override
		public void onConfigurationChanged(@NonNull Configuration newConfig) {
			super.onConfigurationChanged(newConfig);
			requestDarkThemeToBufferedHeaders();
		}

		private void requestDarkThemeToBufferedHeaders() {
			if (LockScreenNotes.isDarkMode(getContext())) {
				applyDarkThemeToBufferedHeaders();
			}
		}

		private void applyDarkThemeToBufferedHeaders() {
			// Accessing the preferences by their keys defined in header_preferences.xml
			Preference generalHeader = findPreference("general_header");
			Preference notificationsHeader = findPreference("notifications_header");
			Preference backupHeader = findPreference("backup_header");
			Preference dateAndTimeHeader = findPreference("date_and_time_header");
			Preference infoHeader = findPreference("info_and_about_header");

			if (generalHeader != null) {
				generalHeader.setIcon(R.drawable.baseline_build_white_24);
			}
			if (notificationsHeader != null) {
				notificationsHeader.setIcon(R.drawable.baseline_notifications_white_24);
			}
			if (backupHeader != null) {
				backupHeader.setIcon(R.drawable.baseline_save_alt_white_24);
			}
			if (dateAndTimeHeader != null) {
				dateAndTimeHeader.setIcon(R.drawable.baseline_access_time_white_24);
			}
			if (infoHeader != null) {
				infoHeader.setIcon(R.drawable.baseline_info_white_24);
			}
		}
	}

	/// ////////////////////////////////////////////////////////////////
	///
	/// Specialized fragments
	///
	/// ////////////////////////////////////////////////////////////////

	public static class GeneralPreferenceFragment extends PreferenceFragmentCompat {

		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

			setPreferencesFromResource(R.xml.prefs_general, rootKey);
			UpdatePermissionButtonTexts();

			final Preference resetTutorial = findPreference("prefs_reset_tutorial");
			if (resetTutorial != null) {
				resetTutorial.setOnPreferenceClickListener(preference -> {
					Context context = preference.getContext();
					resetTutorial(context);
					Toast.makeText(context, R.string.prefs_reset_tutorial_success, Toast.LENGTH_LONG).show();
					return true;
				});
			}

			final Preference permissionsHeader = findPreference("prefs_review_notifications_settings_header");
			if (permissionsHeader != null) {
				permissionsHeader.setSelectable(false);
			}

			final Preference permissionsSystem = findPreference("prefs_review_notifications_settings_system");
			if (permissionsSystem != null) {
				permissionsSystem.setOnPreferenceClickListener(preference -> {
					NotesNotificationManager notesNotificationManager = new NotesNotificationManager(getActivity());
					if (notesNotificationManager.shouldShowRequestPermissionRationale(getActivity())) {
						notesNotificationManager.requestPermissionRationale(getActivity());
					} else {
						try {
							Intent intent = LockScreenNotes.BuildPermissionIntentSystemSettings(getActivity());
							startActivity(intent);
						} catch (Exception e) {
							Timber.e(e);
							Toast.makeText(getActivity(), R.string.error_internal_error, Toast.LENGTH_LONG).show();
						}
					}
					return true;
				});
			}

			final Preference permissionsDrawer = findPreference("prefs_review_notifications_settings_drawer");
			if (permissionsDrawer != null) {
				permissionsDrawer.setOnPreferenceClickListener(preference -> {
					Intent intent = LockScreenNotes.BuildPermissionIntentNotificationDrawer(getActivity());
					try {
						startActivity(intent);
					} catch (Exception e) {
						Timber.e(e);
						Toast.makeText(getActivity(), R.string.error_internal_error, Toast.LENGTH_LONG).show();
					}
					return true;
				});
			}
		}

		private void UpdatePermissionButtonTexts() {
			final Preference permissionsHeader = findPreference("prefs_review_notifications_settings_header");

			if (permissionsHeader != null) {
				NotesNotificationManager notesNotificationManager = new NotesNotificationManager(getActivity());
				String headerStringState = getString(R.string.prefs_review_notifications_settings_header_summary_good);
				if (!notesNotificationManager.hasUserPermissionToDisplayNotifications()) {
					headerStringState = getString(R.string.prefs_review_notifications_settings_header_summary_insufficient);
				}
				Timber.i("Updating permission button texts! The text is now: '" + headerStringState + "'!");

				String headerString = getString(R.string.prefs_review_notifications_settings_header_summary, headerStringState);
				permissionsHeader.setSummary(headerString);
			}
		}

		private void resetTutorial(Context context) {
			SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
			editor.putBoolean("prefs_hide_tutorial", false);
			editor.putBoolean("prefs_ignore_tutorial_autosave", false);
			editor.putLong(VersionManager.PREFERENCE_APP_LAUNCHED_THIS_VERSION, 0);
			editor.apply();
		}

		@Override
		public void onResume() {
			super.onResume();
			UpdatePermissionButtonTexts();
		}
	}

	public static class NotificationsPreferenceFragment extends PreferenceFragmentCompat {

		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			setPreferencesFromResource(R.xml.prefs_notifications, rootKey);

			Preference reversePreference = findPreference("prefs_reverse_displayed_notifications");
			if (reversePreference != null) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
					reversePreference.setSummary(R.string.prefs_reverse_displayed_notifications_flavor_android7);
				}
			}
		}
	}

	public static class AutoBackupPreferenceFragment extends PreferenceFragmentCompat {

		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			setPreferencesFromResource(R.xml.prefs_auto_backup, rootKey);

			FileManager manager = new FileManager(getActivity());
			SharedPreferences preferences = android.preference.PreferenceManager.getDefaultSharedPreferences(getActivity());

			findPreference("pref_auto_backup_info").setSummary(getString(R.string.pref_auto_backup_info_summary, manager.getNoteBackupDir()));
			findPreference("pref_auto_backups_delete_old").setSummary(getString(R.string.pref_auto_backups_delete_old_summary, String.valueOf(AUTO_DELETE_MAX_FILE_COUNT)));
			findPreference("pref_auto_backups_enabled").setOnPreferenceChangeListener((preference, newValue) -> {
				boolean isEnabled = (Boolean) newValue;
				Timber.i("'auto backups' new value: " + isEnabled);
				return true;
			});

			Preference autoBackupInfo = findPreference("pref_auto_backup_info");
			autoBackupInfo.setSelectable(false);

			Preference schedulePreference = findPreference("pref_auto_backups_schedule_days");
			schedulePreference.setOnPreferenceChangeListener((preference, newValue) -> {
				String s = newValue.toString();
				Timber.i("Schedule of days changed. New value: " + s);
				preference.setSummary(preference.getContext().getString(R.string.pref_auto_backups_schedule_days_summary, s));

				return true;
			});
			schedulePreference.setSummary(getActivity().getString(R.string.pref_auto_backups_schedule_days_summary, preferences.getString("pref_auto_backups_schedule_days", "3")));
		}

		@Override
		public void onPause() {
			super.onPause();

			Timber.i("Stopping the Backup preference activity. Let's see if a alarm will be scheduled...");
			SharedPreferences preferences = android.preference.PreferenceManager.getDefaultSharedPreferences(getActivity());
			LSNAlarmManager manager = new LSNAlarmManager(getActivity());
			if (preferences.getBoolean("pref_auto_backups_enabled", false)) {
				Timber.i("Changes detected. Requesting a new backup timer!");
				manager.requestCancelAndReScheduleNextAutoBackup();
			} else {
				boolean nextBackupCanceled = manager.cancelNextAutoBackup();
				if (nextBackupCanceled) {
					Timber.i("A next auto backup alarm will not be scheduled and any old ones will be disabled.");
				} else {
					Timber.e("Failed to cancel next alarm!");
					Toast.makeText(getActivity(), R.string.error_failed_to_cancel_backup_alarm, Toast.LENGTH_LONG).show();
				}
			}
		}
	}

	public static class DateAndTimePreferenceFragment extends PreferenceFragmentCompat {

		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			setPreferencesFromResource(R.xml.prefs_time, rootKey);

			Runnable dateAndTimeUpdater = new Runnable() {
				@Override
				public void run() {
					Timber.i("Running nested Runnable!");
					updateTimeAndDatePreference();
				}
			};
			dateAndTimeUpdater.run();

			bindPreferenceSummaryToValue(findPreference("prefs_time_detail"), dateAndTimeUpdater);
			bindPreferenceSummaryToValue(findPreference("prefs_date_detail"), dateAndTimeUpdater);

			findPreference("prefs_time_preview").setOnPreferenceClickListener(preference -> {
				updateTimeAndDatePreference();
				return true;
			});
		}

		public void updateTimeAndDatePreference() {
			Timber.i("Updating Time and Date preview.");
			Preference preview = findPreference("prefs_time_preview");
			TimeUtils utils = new TimeUtils(getActivity());

			synchronized (preview) {
				String summary = getActivity().getString(R.string.error_unknown);
				try {
					int levelOfDetailDate = utils.getLoDviaPreference(((ListPreference) (findPreference("prefs_date_detail"))).getValue());
					int levelOfDetailTime = utils.getLoDviaPreference(((ListPreference) (findPreference("prefs_time_detail"))).getValue());

					Timber.i("Current Level of Detail Date: " + levelOfDetailDate);
					Timber.i("Current Level of Detail Time: " + levelOfDetailTime);

					summary = utils.formatDateAbsolute(new Date(), levelOfDetailTime, levelOfDetailDate);
				} catch (Exception e) {
					Timber.e(e);
				}

				Timber.i("Summary: " + summary);
				preview.setSummary(summary);
			}
		}
	}

	public static class InfoAndAboutPreferenceFragment extends PreferenceFragmentCompat {

		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			setPreferencesFromResource(R.xml.prefs_info, rootKey);

			int versionCode = 0;
			String versionName = getString(R.string.error_unknown);
			String appName = getString(R.string.app_name);

			PackageManager manager = getActivity().getPackageManager();
			try {
				PackageInfo info = manager.getPackageInfo(getActivity().getPackageName(), 0);
				versionName = info.versionName;
				versionCode = info.versionCode;
			} catch (PackageManager.NameNotFoundException e) {
				e.printStackTrace();
			}

			final int finalVersionCode = versionCode;
			Preference.OnPreferenceClickListener displayVersionUpdateNewsAction = new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(@NonNull Preference preference) {
					VersionManager.displayVersionUpdateNews(preference.getContext(), finalVersionCode);
					return true;
				}
			};

			Preference aboutPreference = findPreference("pref_about");
			aboutPreference.setSummary(String.format(getString(R.string.prefs_about_summary), appName, versionName));
			aboutPreference.setOnPreferenceClickListener(displayVersionUpdateNewsAction);

			Preference externalFeedPreference = findPreference("pref_github_releases_feed");
			externalFeedPreference.setSummary(getString(R.string.pref_github_releases_summary) + "\n" + getString(R.string.const_github_update_feed_url));
			externalFeedPreference.setOnPreferenceClickListener(preference -> {
				ClipboardManager clipboard = (ClipboardManager) preference.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
				ClipData clip = ClipData.newPlainText(getString(R.string.app_name), getString(R.string.const_github_update_feed_url));
				if (clipboard != null) {
					clipboard.setPrimaryClip(clip);
					Toast.makeText(preference.getContext(), R.string.action_github_url_success, Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(preference.getContext(), R.string.error_clipboard_unavailable, Toast.LENGTH_LONG).show();
				}
				return true;
			});

			bindPreferenceURLAsAction(findPreference("pref_view_on_github"), Uri.parse(getString(R.string.const_github_url)));
			bindPreferenceURLAsAction(findPreference("prefs_credits_text_drawable"));
			bindPreferenceURLAsAction(findPreference("prefs_credits_timber"));
			bindPreferenceURLAsAction(findPreference("prefs_credits_balloon"));
			bindPreferenceURLAsAction(findPreference("prefs_credits_debug_db"));
			bindPreferenceURLAsAction(findPreference("prefs_credits_zxing"));
			bindPreferenceURLAsAction(findPreference("prefs_credits_leakcanary"));
			bindPreferenceURLAsAction(findPreference("pref_view_on_play_store"), Uri.parse(getString(R.string.const_google_play_url)));

			findPreference("pref_share_app").setOnPreferenceClickListener(preference -> {
				String text = getString(R.string.pref_share_app_text, getString(R.string.app_name), getString(R.string.const_google_play_url));

				Intent sendIntent = new Intent();
				sendIntent.setAction(Intent.ACTION_SEND);
				sendIntent.putExtra(Intent.EXTRA_TEXT, text);
				sendIntent.setType("text/plain");
				startActivity(Intent.createChooser(sendIntent, getResources().getString(R.string.action_share)));
				return true;
			});

			Preference updateDayCounter = findPreference("pref_update_day_counter");
			int lastUpdateDays = getUpdateDays(getActivity());
			if (lastUpdateDays == LAST_UPDATE_DATE_UNKNOWN) {
				Timber.w("Failed to get the last date this app was updated! Doing nothing, as this will result in the text that an error occurred!");
			} else {
				SharedPreferences preferences = android.preference.PreferenceManager.getDefaultSharedPreferences(getActivity());
				long launchedCountAllTime = preferences.getLong(VersionManager.PREFERENCE_APP_LAUNCHED_ALL_TIME, 0);
				long launchedCountThisVersion = preferences.getLong(VersionManager.PREFERENCE_APP_LAUNCHED_THIS_VERSION, 0);

				String displayedText = getString(R.string.pref_update_day_counter_summary, String.valueOf(lastUpdateDays), String.valueOf(launchedCountThisVersion), String.valueOf(launchedCountAllTime));
				updateDayCounter.setSummary(displayedText);
			}
			updateDayCounter.setOnPreferenceClickListener(displayVersionUpdateNewsAction);
		}
	}

	private static int getUpdateDays(Context context) {
		Date now = new Date();
		Date lastUpdate = null;
		try {
			lastUpdate = VersionManager.getCurrentVersionDate(context);
		} catch (Exception e) {
			e.printStackTrace();
			Timber.e(e);
			return LAST_UPDATE_DATE_UNKNOWN;
		}

		long diffTime = now.getTime() - lastUpdate.getTime();
		long diffDays = diffTime / (1000 * 60 * 60 * 24);
		return (int) diffDays;
	}

}