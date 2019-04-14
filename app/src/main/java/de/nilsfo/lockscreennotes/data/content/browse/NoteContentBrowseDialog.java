package de.nilsfo.lockscreennotes.data.content.browse;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.util.SparseBooleanArray;
import android.widget.Toast;

import java.util.ArrayList;

import de.nilsfo.lockscreennotes.data.Note;
import de.nilsfo.lsn.R;
import timber.log.Timber;

public abstract class NoteContentBrowseDialog {

	protected Context context;

	public NoteContentBrowseDialog(Context context) {
		this.context = context;
	}

	public void displayDialog(Note note) {
		displayDialog(note.getText());
	}

	public void displayDialog(String text) {
		ArrayList<String> list = getMatchesInText(text);

		if (list == null) {
			return;
		}

		if (list.isEmpty()) {
			Toast.makeText(context, getErrorNothingFoundText(), Toast.LENGTH_LONG).show();
			return;
		}

		displayDialog(list);
	}

	public void displayDialog(final ArrayList<String> list) {
		if (list.size() == 1) {
			browseElement(list.get(0));
			return;
		}

		boolean[] sel = new boolean[list.size()];
		String[] data = new String[list.size()];
		for (int i = 0; i < list.size(); i++) {
			data[i] = list.get(i);
			sel[i] = false;
		}

		AlertDialog.Builder b = new AlertDialog.Builder(context);
		b.setTitle(getDialogTitle());
		b.setIcon(getDialogIcon());

		if (isSingleSelection()) {
			b.setSingleChoiceItems(data, 0, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					SparseBooleanArray sel = ((AlertDialog) dialog).getListView().getCheckedItemPositions();
					Timber.v("Elements selected: " + sel);
				}
			});
		} else {
			b.setMultiChoiceItems(data, sel, new DialogInterface.OnMultiChoiceClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which, boolean isChecked) {
					SparseBooleanArray sel = ((AlertDialog) dialog).getListView().getCheckedItemPositions();
					Timber.v("Elements selected: " + sel);
				}
			});
		}

		b.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});

		b.setPositiveButton(getDialogPositiveButtonName(), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				SparseBooleanArray sel = ((AlertDialog) dialog).getListView().getCheckedItemPositions();
				Timber.i("Multiple elements to browse selected: " + sel);

				int size = list.size();
				if (size == 0) {
					Toast.makeText(context, getErrorNothingSelectedText(), Toast.LENGTH_LONG).show();
					return;
				}

				for (int i = 0; i < size; i++) {
					if (sel.get(i)) {
						String element = list.get(i);
						browseElement(element);

						if (isSingleSelection()) {
							Timber.i("Browsing just a single element. Job done.");
							return;
						}

						Timber.i("Browsing Element '" + element + "'. " + (i + 1) + "/" + size);
					}
				}
			}
		});

		if (!isSingleSelection()) {
			b.setNeutralButton(getDialogBrowseAllButtonName(), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					for (String s : list) {
						browseElement(s);
					}
				}
			});
		}

		b.show();
	}

	protected abstract void browseElement(String element);

	protected abstract ArrayList<String> getMatchesInText(String text);

	protected abstract int getErrorNothingSelectedText();

	protected abstract int getDialogPositiveButtonName();

	protected abstract int getDialogBrowseAllButtonName();

	protected abstract int getErrorNothingFoundText();

	protected abstract int getDialogTitle();

	protected int getDialogIcon() {
		return R.mipmap.ic_launcher;
	}

	protected abstract boolean isSingleSelection();
}
