package de.nilsfo.lockscreennotes.io.backups;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.List;

public interface BackupStorage {

	@NonNull
	BackupStorageState getState();

	@NonNull
	List<BackupFile> findBackups() throws IOException;

	@NonNull
	BackupFile writeBackup(
			@NonNull String filename,
			@NonNull String contents
	) throws IOException;

	@NonNull
	String readBackup(@NonNull BackupFile backup) throws IOException;

	boolean deleteBackup(@NonNull BackupFile backup) throws IOException;
}