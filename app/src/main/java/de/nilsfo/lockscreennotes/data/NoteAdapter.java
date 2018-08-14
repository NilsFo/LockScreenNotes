package de.nilsfo.lockscreennotes.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import de.nilsfo.lockscreennotes.activity.MainActivity;
import de.nilsfo.lockscreennotes.util.TimeUtils;
import de.nilsfo.lsn.R;
import timber.log.Timber;

/**
 * Created by Nils on 13.08.2016.
 */

public class NoteAdapter extends ArrayAdapter<Note> {

	public static final String PREFERENCE_ALLOW_EDIT_NOTE_ACTIVITY = "prefs_allow_edit_note_activity";
	private static final int DEFAULT_MIN_LINES = 5;
	public static final int DELETE_BT_SIZE = 36;
	public static final int DELETE_BT_COLOR = Color.GRAY;

	public NoteAdapter(Context context, int resource, List<Note> objects) {
		super(context, resource, objects);
	}

	@NonNull
	@Override
	public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(getContext());
		//View view;
		//if (convertView == null)
		//	view = inflater.inflate(R.layout.note_row, parent, false);
		//else
		//	view = convertView;
		//
		Note note = getItem(position);
		@SuppressLint("ViewHolder") View view = inflater.inflate(R.layout.note_row, parent, false);

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
		TextView timestampTF = (TextView) view.findViewById(R.id.note_row_timestamp);
		TextView positionTF = (TextView) view.findViewById(R.id.note_row_position);
		ImageButton enabledBT = (ImageButton) view.findViewById(R.id.note_row_image_bt);
		ImageButton deleteBT = (ImageButton) view.findViewById(R.id.image_row_deleteBT);
		EditText editText = (EditText) view.findViewById(R.id.note_row_text_tf);
		View root = enabledBT.getRootView();

		editText.setMinLines(getMinLines());
		editText.setText(note != null ? note.getText() : getContext().getText(R.string.error_unknown));
		editText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				Note n = getItem(position);
				if (n != null) {
					n.setText(editable.toString());
				}
			}
		});

		if (isFullscreenEditMode()) {
			editText.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					Timber.i("Turns out... its a click on the Note.");
					requestEditNoteActivity(getItem(position));
				}
			});
			//editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			//
			//	@Override
			//	public void onFocusChange(View v, boolean hasFocus) {
			//		if (getCount() == 0) return;
			//
			//		if (hasFocus) {
			//			Log.i(LOGTAG, "Turns out... its a focus gain on the note.");
			//			requestEditNoteActivity(getItem(position));
			//		}
			//	}
			//});
			editText.setFocusable(false);
			editText.setClickable(true);
			Timber.i("Seems that it is fullscreenEditMoode... So all listeners have been added.");
		}

		enabledBT.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Note n = getItem(position);
				if (n == null) {
					return;
				}
				n.setEnabled(!n.isEnabled());

				notifyDataSetChanged();
			}
		});
		enabledBT.setBackground(root.getBackground());

		deleteBT.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				notifyDataSetChanged();

				Context context = getContext();
				if (context instanceof MainActivity) {
					MainActivity activity = (MainActivity) context;
					activity.requestDeleteNote(getItem(position));
				}
			}
		});

		TimeUtils utils = new TimeUtils(getContext());

		long time = note.getTimestamp();
		if (utils.isRelativeTimePrefered()) {
			timestampTF.setText(getContext().getString(R.string.last_edited,utils.formatRelative(time)));
			RelativeTimeTextfieldContainer.getContainer().add(timestampTF, time);
		} else
			timestampTF.setText(getContext().getString(R.string.last_edited, utils.formatAbsolute(time)));
		enabledBT.setImageResource(getEnabledIcon(note != null && note.isEnabled()));
		positionTF.setText(String.format(getContext().getString(R.string.note_position), String.valueOf(position + 1)));
		deleteBT.setBackground(view.getBackground());

		return view;
	}



	private int getMinLines() {
		String lines = PreferenceManager.getDefaultSharedPreferences(getContext()).getString("prefs_homescreen_lines", String.valueOf(DEFAULT_MIN_LINES));
		try {
			return Integer.parseInt(lines);
		} catch (NumberFormatException e) {
			return DEFAULT_MIN_LINES;
		}
	}

	private boolean requestEditNoteActivity(Note note) {
		if (!isFullscreenEditMode()) {
			Timber.i("A request was made to edit via fullscreen. But it was not enabled in the prefs_general.");
			return false;
		}

		Context context = getContext();
		if (context instanceof MainActivity) {
			MainActivity activity = (MainActivity) context;
			activity.requestEditNote(note);
		}
		return true;
	}

	public boolean isFullscreenEditMode() {
		//boolean b = PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(PREFERENCE_ALLOW_EDIT_NOTE_ACTIVITY, true);
		//Log.i(LOGTAG, "A check was made if its fullscreen edit mode. Result: " + b);
		//return b;

		return true;
	}

	private int getEnabledIcon(boolean enabled) {
		if (enabled) return R.drawable.enabled_button;
		return R.drawable.disabled_button;
	}

	@Deprecated
	private int getNoteTextColor(boolean enabled) {
		if (enabled) return android.R.color.black;
		return android.R.color.holo_red_dark;
	}

}
