package de.nilsfo.lockscreennotes.io;

import static de.nilsfo.lockscreennotes.LockScreenNotes.REQUEST_CODE_PERMISSION_STORAGE;
import static de.nilsfo.lockscreennotes.io.FileManager.FOLDER_NAME_BACKUP;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;

import de.nilsfo.lsn.R;
import timber.log.Timber;

public class StoragePermissionManager {

	public static class InsufficientStoragePermissionException extends Exception {
		public InsufficientStoragePermissionException() {
			super();
		}

		public InsufficientStoragePermissionException(String message) {
			super(message);
		}

		public InsufficientStoragePermissionException(String message, Throwable cause) {
			super(message, cause);
		}

		public InsufficientStoragePermissionException(Throwable cause) {
			super(cause);
		}

	}

	public static enum StoragePermissionState {
		STATE_NO_PERMISSION,
		STATE_MEDIA_ONLY,
		STATE_FULL_ACCESS
	}

	private Context context;

	public StoragePermissionManager(Context context) {
		this.context = context;
	}

	public boolean hasAllCorrectPermissions() {
		return getPermissionState() == StoragePermissionState.STATE_FULL_ACCESS;
	}

	public boolean requiresExtendedPermissions() {
		StoragePermissionState currentState = getPermissionState();
		switch (currentState) {
			case STATE_MEDIA_ONLY:
				return true;
			case STATE_FULL_ACCESS:
				return false;
			case STATE_NO_PERMISSION:
				return true;
			default:
				// Do not forget to update this switch when a new state is defined
				throw new IllegalStateException("Unknown storage space: " + currentState);
		}
	}

	public boolean hasPermissionsTotallyDenied() {
		return getPermissionState() == StoragePermissionState.STATE_NO_PERMISSION;
	}

	public StoragePermissionState getPermissionState() {
		// If the version is greater than 'R', we have to check if we are the ExternalStorage Manager
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			if (isExternalStorageManager()) {
				return StoragePermissionState.STATE_FULL_ACCESS;
			}
			if (hasExternalStoragePermission()) {
				return StoragePermissionState.STATE_MEDIA_ONLY;
			} else return StoragePermissionState.STATE_NO_PERMISSION;
		}

		// If not, we only need to check for the external storage permission
		if (hasExternalStoragePermission()) {
			return StoragePermissionState.STATE_FULL_ACCESS;
		} else return StoragePermissionState.STATE_NO_PERMISSION;
	}

	public boolean hasExternalStoragePermission() {
		// If the version is greater than 'R', we have to check if we are the ExternalStorage Manager
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			if (isExternalStorageManager()) {
				return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
			}
			return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ||
					ContextCompat.checkSelfPermission(context, Manifest.permission.MANAGE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
		}

		// If not, we only need to check for the default storage permission
		return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
	}

	public void requestExternalStoragePermission(Activity activity) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

			// Setting up a dummy intent
			boolean storageIntentSent = true;
			Intent storageManagerIntent = null;
			try {
				storageManagerIntent = new Intent();
				storageManagerIntent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
				Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
				storageManagerIntent.setData(uri);
			} catch (Exception e) {
				Timber.e(e);
				Toast.makeText(activity, R.string.error_internal_error, Toast.LENGTH_LONG).show();
			}

			// Checking if the intent exists and firing it
			if (storageManagerIntent != null) {
				try {
					activity.startActivity(storageManagerIntent);
				} catch (Exception e) {
					Timber.e(e);
					Toast.makeText(activity, R.string.error_internal_error, Toast.LENGTH_LONG).show();
					storageIntentSent = false;
				}
			} else storageIntentSent = false;

			if (!storageIntentSent) {
				// If the intent has not been sent, try to use the compat to request permission
				ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.MANAGE_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSION_STORAGE);
			}
		} else {
			ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSION_STORAGE);
		}
	}

	public boolean isExternalStorageManager() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			return Environment.isExternalStorageManager();
		}
		return true;
	}

	public String getDocumentsFilePath(Context context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			return Environment.DIRECTORY_DOCUMENTS;
		}

		FileManager fileManager = new FileManager(context);
		File f = new File(fileManager.getExternalDir(), FOLDER_NAME_BACKUP);
		return f.getAbsolutePath();
	}

}
