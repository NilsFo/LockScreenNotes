package de.nilsfo.lockscreennotes.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.nilsfo.lockscreennotes.data.Note;
import de.nilsfo.lockscreennotes.sql.DBAdapter;
import de.nilsfo.lockscreennotes.util.NotesNotificationManager;
import de.nilsfo.lockscreennotes.view.QRCodeView;
import de.nilsfo.lsn.R;
import timber.log.Timber;

public class EditNoteActivity extends NotesActivity {

	//public static final String URL_REGEX = "(?:(?:https?|ftp|file):\\/\\/|www\\.|ftp\.)(?:\\([-A-Z0-9+&@#\\/%=~_|$?!:,.]*\\)|[-A-Z0-9+&@#\\/%=~_|$?!:,.])*(?:\([-A-Z0-9+&@#\\/%=~_|$?!:,.]*\)|[A-Z0-9+&@#\\/%=~_|$])";
	//private static final String URL_REGEX = "((?:https\\:\\/\\/)|(?:http\\:\\/\\/)|(?:www\\.))?([a-zA-Z0-9\\-\\.]+\\.[a-zA-Z]{2,3}(?:\\??)[a-zA-Z0-9\\-\\._\\?\\,\\'\\/\\\\\\+&%\\$#\\=~]+)";
	private static final String URL_REGEX = "(https?:\\/\\/)?([\\da-z\\.-]+\\.[a-z\\.]{2,6}|[\\d\\.]+)([\\/:?=&#]{1}[\\da-z\\.-]+)*[\\/\\?]?";

	public static final String NOTE_ACTIVITY_NOTE_ID = "EditNoteActivity_note_id";
	public static final int QR_IMAGE_SIZE = 512;
	public static final long ILLEGAL_NOTE_ID = -1;

	private Note myNote;
	private boolean canceled;
	private DBAdapter databaseAdapter;
	private EditText noteTF;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		canceled = false;
		setContentView(R.layout.activity_edit_note);
		ActionBar bar = getSupportActionBar();
		if (bar != null) bar.setDisplayHomeAsUpEnabled(true);

		SharedPreferences preferencses = PreferenceManager.getDefaultSharedPreferences(this);

		long id = getIntent().getExtras().getLong(NOTE_ACTIVITY_NOTE_ID, ILLEGAL_NOTE_ID);
		if (id == ILLEGAL_NOTE_ID) {
			handleIllegalNote();
			return;
		}

		databaseAdapter = new DBAdapter(this);
		databaseAdapter.open();

		myNote = Note.getNoteFromDB(id, databaseAdapter);
		if (myNote == null) {
			handleIllegalNote();
			return;
		}

		setShowNotifications(true);
		noteTF = (EditText) findViewById(R.id.enditNoteFullscreenTF);
		noteTF.setText(myNote.getText());

		if (!preferencses.getBoolean("prefs_ignore_tutorial_autosave", false)) {
			Timber.i("Displaying the auto-save tutorial now.");

			Snackbar snackbar = Snackbar.make(noteTF, R.string.tutorial_autosave, Snackbar.LENGTH_INDEFINITE);
			snackbar.setAction(R.string.ok_got_it, new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Timber.i("Will not display this tutorial again.");
					PreferenceManager.getDefaultSharedPreferences(v.getContext()).edit().putBoolean("prefs_ignore_tutorial_autosave", true).apply();
				}
			});
			snackbar.setActionTextColor(getResources().getColor(R.color.colorPrimary));

			View snackbarView = snackbar.getView();
			TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
			textView.setMaxLines(4);
			textView.setMinLines(2);

			snackbar.show();
		}

		if (savedInstanceState == null) {
			actionMoveToBottom();
		}
	}

	private void actionClear() {
		new AlertDialog.Builder(this)
				.setTitle(R.string.action_clear)
				.setMessage(R.string.action_clear_confirm)
				.setPositiveButton(R.string.action_clear, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						noteTF.setText("");
						actionMoveToBottom();
					}
				})
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				})
				.setIcon(android.R.drawable.ic_dialog_alert)
				.show();
	}

	private void actionMoveToBottom() {
		noteTF.requestFocus();
		noteTF.setSelection(noteTF.getText().length());
		requestKeyBoard();
	}

	private void requestKeyBoard() {
		requestKeyBoard(noteTF);
	}

	public synchronized void requestKeyBoard(View view) {
		InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
	}

	private void actionShare() {
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, noteTF.getText().toString());
		sendIntent.setType("text/plain");
		startActivity(Intent.createChooser(sendIntent, getResources().getString(R.string.share_using)));
		//startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_using)));
	}

	private void actionCopyText() {
		String text = noteTF.getText().toString();

		ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		ClipData clip = ClipData.newPlainText(getString(R.string.app_name), text);
		clipboard.setPrimaryClip(clip);

		Toast.makeText(this, R.string.action_copy_text_success, Toast.LENGTH_LONG).show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.edit_note_menu, menu);
		return true;
	}

	@Override
	protected void onPause() {
		super.onPause();
		Timber.i("EditNoteFrame: Paused");
		if (!canceled) saveNote();
		databaseAdapter.close();

		if (isShowNotifications())
			new NotesNotificationManager(this).showNotifications();
	}

	@Override
	protected void onResume() {
		super.onResume();
		databaseAdapter.open();
		new NotesNotificationManager(this).hideNotifications();
		setShowNotifications(true);
	}

	public void saveNote() {
		String oldText = myNote.getText();
		String text = noteTF.getText().toString();
		boolean changed = !oldText.equals(text);

		Timber.i("EditNoteFrame: Saving the note. Has something changed? " + changed);

		if (changed) {
			myNote.setText(text);
			myNote.setTimestamp(new Date());

			databaseAdapter.updateRow(myNote.getDatabaseID(), myNote.getText(), myNote.isEnabledSQL(), myNote.getTimestamp());
		}
	}

	public void actionCancelEdit() {
		new AlertDialog.Builder(this)
				.setTitle(R.string.action_cancel_edit)
				.setMessage(R.string.action_cancel_edit_confirm)
				.setPositiveButton(R.string.action_cancel_edit, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								canceled = true;
								finish();
							}
						});
					}
				})
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				})
				.setIcon(android.R.drawable.ic_dialog_alert)
				.show();
	}

	public void actionToQR() {
		String qrcode = noteTF.getText().toString();
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.app_name);
		builder.setIcon(R.mipmap.ic_launcher);

		final QRCodeView qrCodeView = new QRCodeView(this, qrcode, QR_IMAGE_SIZE);
		builder.setPositiveButton(R.string.action_dismiss, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.setNeutralButton(R.string.action_share, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (qrCodeView.hasFinishedRenderingQRCode()) {
					shareQRImage(qrCodeView.getQrImage());
				} else {
					Toast.makeText(EditNoteActivity.this, R.string.error_qr_not_rendered, Toast.LENGTH_LONG).show();
				}
			}
		});

		builder.setView(qrCodeView);
		builder.show();
	}

	public ArrayList<String> getURLsInNote() {
		Matcher m = applyRegexToNote(URL_REGEX);
		if (m == null) {
			return null;
		}

		ArrayList<String> list = new ArrayList<>();
		while (m.find()) {
			String match = m.group();
			Timber.i("Found a match: " + m.group());

			boolean add = true;
			for (String s : list) {
				if (s.equals(match)) {
					add=false;
				}
			}

			if (add){
				list.add(match);
			}
		}

		return list;
	}

	public void actionOpenWeblinks() {
		final ArrayList<String> list = getURLsInNote();
		if (list == null) {
			return;
		}

		if (list.isEmpty()) {
			Toast.makeText(this, R.string.error_no_weblings, Toast.LENGTH_LONG).show();
			return;
		}

		if (list.size() == 1) {
			browseURL(list.get(0));
			return;
		}

		boolean[] sel = new boolean[list.size()];
		String[] urls = new String[list.size()];
		for (int i = 0; i < list.size(); i++) {
			urls[i] = list.get(i);
			sel[i] = false;
		}
		AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setTitle(R.string.info_choose_url);
		b.setIcon(R.mipmap.ic_launcher);
		b.setMultiChoiceItems(urls, sel, new DialogInterface.OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				SparseBooleanArray sel = ((AlertDialog) dialog).getListView().getCheckedItemPositions();
				Timber.v("URLs selected: " + sel);
			}
		});
		b.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		b.setPositiveButton(R.string.action_browse_selected, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				SparseBooleanArray sel = ((AlertDialog) dialog).getListView().getCheckedItemPositions();
				Timber.i("URLs to browse selected: " + sel);
				for (int i = 0; i < list.size(); i++) {
					if (sel.get(i)) {
						String url = list.get(i);
						browseURL(url);
						Timber.i("Browsing URL '" + url + "'. " + (i + 1) + "/" + list.size());
					}
				}
			}
		});
		b.setNeutralButton(R.string.action_browse_all, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				for (String s : list) {
					browseURL(s);
				}
			}
		});

		b.show();
	}

	private void browseURL(String url) {
		if (!url.toLowerCase().startsWith("http://") && !url.toLowerCase().startsWith("https://")) {
			url = "http://" + url;
		}

		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(url));
		startActivity(i);
	}

	private Matcher applyRegexToNote(String regex) {
		String text = String.valueOf(noteTF.getText());
		Timber.i("Applying Regex: " + regex);
		Timber.i("Text to apply it to: " + text);

		if (text == null || text.trim().equals("")) return null;

		Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
		return p.matcher(text);
	}

	private void shareQRImage(Bitmap image) {
		File imageFile = null;
		try {
			File cachePath = new File(getCacheDir(), "images");
			cachePath.mkdirs();
			imageFile = new File(cachePath + File.separator + getString(R.string.qr_cache_name) + ".png");
			FileOutputStream stream = new FileOutputStream(imageFile); // overwrites this image every time
			image.compress(Bitmap.CompressFormat.PNG, 100, stream);
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
			Timber.e(e, "Failed to share QR Code!");

			Toast.makeText(this, R.string.error_qr_generation_failed, Toast.LENGTH_LONG).show();
			return;
		}

		if (imageFile == null || !imageFile.exists()) {
			Toast.makeText(this, R.string.error_qr_generation_failed, Toast.LENGTH_LONG).show();
			return;
		}

		//File imagePath = new File(getCacheDir(), "images");
		//File newFile = new File(imagePath, "image.png");
		Timber.i("Shared QR-Imagefile info: " + imageFile);
		Uri contentUri = FileProvider.getUriForFile(this, "de.nilsfo.lockscreennotes.LockScreenNotes.fileprovider", imageFile);

		if (contentUri != null) {
			Intent shareIntent = new Intent();
			shareIntent.setAction(Intent.ACTION_SEND);
			shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
			shareIntent.setDataAndType(contentUri, getContentResolver().getType(contentUri));
			shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
			startActivity(Intent.createChooser(shareIntent, getString(R.string.share_using)));
		} else {
			Toast.makeText(this, R.string.error_qr_generation_failed, Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			// Respond to the action bar's Up/Home button
			case android.R.id.home:
				setShowNotifications(false);
				NavUtils.navigateUpFromSameTask(this);
				return true;

			case R.id.action_clear:
				actionClear();
				return true;

			case R.id.action_move_to_bottom:
				actionMoveToBottom();
				return true;

			case R.id.action_share:
				actionShare();
				return true;

			case R.id.action_copy_note:
				actionCopyText();
				return true;

			case R.id.action_cancel_edit:
				actionCancelEdit();
				return true;

			case R.id.action_to_qr_code:
				actionToQR();
				return true;

			case R.id.action_open_weblinks:
				actionOpenWeblinks();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {
		setShowNotifications(false);
		super.onBackPressed();
	}

	private void handleIllegalNote() {
		Toast.makeText(this, R.string.error_internal_error, Toast.LENGTH_LONG).show();
	}

	@Override
	protected void onStop() {
		super.onStop();
		databaseAdapter.close();
	}
}
