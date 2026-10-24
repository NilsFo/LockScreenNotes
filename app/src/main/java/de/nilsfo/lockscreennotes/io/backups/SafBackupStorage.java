package de.nilsfo.lockscreennotes.io.backups;

import android.content.ContentResolver;
import android.content.Context;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class SafBackupStorage implements BackupStorage {

	private static final String MIME_TYPE_JSON = "application/json";

	private final ContentResolver contentResolver;
	private final BackupLocationStore locationStore;

	public SafBackupStorage(@NonNull Context context) {
		Context appContext = context.getApplicationContext();
		contentResolver = appContext.getContentResolver();
		locationStore = new BackupLocationStore(appContext);
	}

	@NonNull
	@Override
	public BackupStorageState getState() {
		Uri treeUri = locationStore.getTreeUri();

		if (treeUri == null) {
			return BackupStorageState.NOT_CONFIGURED;
		}

		if (!hasReadPermission(treeUri)) {
			return BackupStorageState.PERMISSION_LOST;
		}

		try {
			Uri directoryUri = getRootDocumentUri(treeUri);

			int flags = queryDocumentFlags(directoryUri);

			if (flags == -1) {
				return BackupStorageState.LOCATION_UNAVAILABLE;
			}

			if (!hasWritePermission(treeUri)
					|| (flags & DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE) == 0) {
				return BackupStorageState.READ_ONLY;
			}

			return BackupStorageState.AVAILABLE;

		} catch (SecurityException e) {
			return BackupStorageState.PERMISSION_LOST;
		} catch (Exception e) {
			return BackupStorageState.LOCATION_UNAVAILABLE;
		}
	}

	@NonNull
	@Override
	public List<BackupFile> findBackups() throws IOException {
		List<BackupFile> result = new ArrayList<>();
		Uri directoryUri = getSelectedDirectoryUri();

		if (directoryUri == null) {
			return result;
		}

		String documentId = DocumentsContract.getDocumentId(directoryUri);
		Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
				directoryUri,
				documentId
		);

		String[] projection = {
				DocumentsContract.Document.COLUMN_DOCUMENT_ID,
				DocumentsContract.Document.COLUMN_DISPLAY_NAME,
				DocumentsContract.Document.COLUMN_MIME_TYPE,
				DocumentsContract.Document.COLUMN_LAST_MODIFIED
		};

		try (Cursor cursor = contentResolver.query(
				childrenUri,
				projection,
				null,
				null,
				null
		)) {
			if (cursor == null) {
				throw new IOException("Unable to query backup directory.");
			}

			int idColumn = cursor.getColumnIndexOrThrow(
					DocumentsContract.Document.COLUMN_DOCUMENT_ID
			);
			int nameColumn = cursor.getColumnIndexOrThrow(
					DocumentsContract.Document.COLUMN_DISPLAY_NAME
			);
			int mimeColumn = cursor.getColumnIndexOrThrow(
					DocumentsContract.Document.COLUMN_MIME_TYPE
			);
			int modifiedColumn = cursor.getColumnIndexOrThrow(
					DocumentsContract.Document.COLUMN_LAST_MODIFIED
			);

			while (cursor.moveToNext()) {
				String id = cursor.getString(idColumn);
				String name = cursor.getString(nameColumn);
				String mime = cursor.getString(mimeColumn);
				long lastModified = cursor.isNull(modifiedColumn)
						? 0L
						: cursor.getLong(modifiedColumn);

				if (!isRecognizedBackup(name, mime)) {
					continue;
				}

				Uri uri = DocumentsContract.buildDocumentUriUsingTree(
						directoryUri,
						id
				);

				result.add(new BackupFile(uri, name, lastModified));
			}

		} catch (SecurityException e) {
			throw new IOException("Backup storage permission is unavailable.", e);
		}

		return result;
	}

	@NonNull
	@Override
	public BackupFile writeBackup(
			@NonNull String filename,
			@NonNull String contents
	) throws IOException {

		if (getState() != BackupStorageState.AVAILABLE) {
			throw new IOException("Backup storage is not writable.");
		}

		Uri directoryUri = getSelectedDirectoryUri();

		if (directoryUri == null) {
			throw new IOException("Backup storage is not configured.");
		}

		Uri fileUri = DocumentsContract.createDocument(
				contentResolver,
				directoryUri,
				MIME_TYPE_JSON,
				filename
		);

		if (fileUri == null) {
			throw new IOException("Unable to create backup document.");
		}

		boolean completed = false;

		try {
			OutputStream stream = contentResolver.openOutputStream(fileUri, "wt");

			if (stream == null) {
				throw new IOException("Unable to open backup output stream.");
			}

			try (OutputStreamWriter writer =
						 new OutputStreamWriter(stream, StandardCharsets.UTF_8)) {
				writer.write(contents);
				writer.flush();
			}

			completed = true;

			return new BackupFile(
					fileUri,
					filename,
					System.currentTimeMillis()
			);

		} finally {
			if (!completed) {
				try {
					DocumentsContract.deleteDocument(contentResolver, fileUri);
				} catch (Exception ignored) {
					// Best effort cleanup of an incomplete backup.
				}
			}
		}
	}

	@NonNull
	@Override
	public String readBackup(@NonNull BackupFile backup) throws IOException {
		InputStream stream = contentResolver.openInputStream(backup.getUri());

		if (stream == null) {
			throw new IOException("Unable to open backup.");
		}

		StringBuilder result = new StringBuilder();

		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(stream, StandardCharsets.UTF_8)
		)) {
			char[] buffer = new char[4096];
			int count;

			while ((count = reader.read(buffer)) != -1) {
				result.append(buffer, 0, count);
			}
		}

		return result.toString();
	}

	@Override
	public boolean deleteBackup(@NonNull BackupFile backup) throws IOException {
		try {
			return DocumentsContract.deleteDocument(
					contentResolver,
					backup.getUri()
			);

		} catch (SecurityException e) {
			throw new IOException("Unable to delete backup.", e);
		}
	}

	@Nullable
	private Uri getSelectedDirectoryUri() {
		Uri treeUri = locationStore.getTreeUri();

		if (treeUri == null) {
			return null;
		}

		return getRootDocumentUri(treeUri);
	}

	@NonNull
	private Uri getRootDocumentUri(@NonNull Uri treeUri) {
		return DocumentsContract.buildDocumentUriUsingTree(
				treeUri,
				DocumentsContract.getTreeDocumentId(treeUri)
		);
	}

	private int queryDocumentFlags(@NonNull Uri documentUri) {
		String[] projection = {
				DocumentsContract.Document.COLUMN_FLAGS
		};

		try (Cursor cursor = contentResolver.query(
				documentUri,
				projection,
				null,
				null,
				null
		)) {
			if (cursor == null || !cursor.moveToFirst()) {
				return -1;
			}

			return cursor.getInt(0);
		}
	}

	private boolean hasReadPermission(@NonNull Uri uri) {
		for (UriPermission permission :
				contentResolver.getPersistedUriPermissions()) {

			if (uri.equals(permission.getUri())
					&& permission.isReadPermission()) {
				return true;
			}
		}

		return false;
	}

	private boolean hasWritePermission(@NonNull Uri uri) {
		for (UriPermission permission :
				contentResolver.getPersistedUriPermissions()) {

			if (uri.equals(permission.getUri())
					&& permission.isWritePermission()) {
				return true;
			}
		}

		return false;
	}

	private boolean isRecognizedBackup(
			@Nullable String name,
			@Nullable String mimeType
	) {
		if (name == null) {
			return false;
		}

		return name.startsWith(BackupManager.FILENAME_BACKUP_BASE_NAME)
				&& name.endsWith(BackupManager.FILENAME_BACKUP_EXTENSION)
				&& !DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType);
	}
}