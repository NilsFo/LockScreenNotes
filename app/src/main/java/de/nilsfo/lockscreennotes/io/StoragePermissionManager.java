package de.nilsfo.lockscreennotes.io;

import static de.nilsfo.lockscreennotes.LockScreenNotes.REQUEST_CODE_PERMISSION_STORAGE;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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

	private final Context context;

	public StoragePermissionManager(Context context) {
		this.context = context;
	}

	public boolean hasExternalStoragePermission() {
		return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
				&&
				ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
	}

	public void requestExternalStoragePermission(Activity activity) {
		ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSION_STORAGE);
	}

}
