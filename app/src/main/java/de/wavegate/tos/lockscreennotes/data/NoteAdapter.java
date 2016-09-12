package de.wavegate.tos.lockscreennotes.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.wavegate.tos.lockscreennotes.MainActivity;
import de.wavegate.tos.lockscreennotes.R;
import de.wavegate.tos.lockscreennotes.view.fragment.SettingsFragment;

import static de.wavegate.tos.lockscreennotes.MainActivity.LOGTAG;

/**
 * Created by Nils on 13.08.2016.
 */

public class NoteAdapter extends ArrayAdapter<Note> {

	public static final String PREFERENCE_ALLOW_EDIT_NOTE_ACTIVITY = "prefs_allow_edit_note_activity";
	public static final int DEFAULT_LEVEL_OF_DETAIL = DateFormat.MEDIUM;
	private static final int DEFAULT_MIN_LINES = 5;

	public NoteAdapter(Context context, int resource, List<Note> objects) {
		super(context, resource, objects);
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(getContext());
		//View view;
		//if (convertView == null)
		//	view = inflater.inflate(R.layout.note_row, parent, false);
		//else
		//	view = convertView;
		//
		Note note = getItem(position);
		View view = inflater.inflate(R.layout.note_row, parent, false);

		TextView text = (TextView) view.findViewById(R.id.note_row_timestamp);
		TextView positionTF = (TextView) view.findViewById(R.id.note_row_position);
		ImageButton enabledBT = (ImageButton) view.findViewById(R.id.note_row_image_bt);
		ImageButton deleteBT = (ImageButton) view.findViewById(R.id.image_row_deleteBT);
		EditText editText = (EditText) view.findViewById(R.id.note_row_text_tf);

		//	editText.setMinHeight(editText.getLineHeight() * getMinLines());
		editText.setMinLines(getMinLines());
		editText.setText(note.getText());
		editText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				getItem(position).setText(editable.toString());
			}
		});

		if (isFullscreenEditMode()) {
			editText.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					Log.i(LOGTAG, "Turns out... its a click on the Note.");
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
			Log.i(LOGTAG, "Seems that it is fullscreenEditMoode... So all listeners have been added.");
		}

		enabledBT.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Note n = getItem(position);
				n.setEnabled(!n.isEnabled());

				notifyDataSetChanged();
			}
		});
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

		enabledBT.setImageResource(getEnabledIcon(note.isEnabled()));
		text.setText(String.format(getContext().getString(R.string.last_edited), getNoteTime(note)));
		positionTF.setText(String.format(getContext().getString(R.string.note_position), String.valueOf(position + 1)));
		deleteBT.setBackground(view.getBackground());

		return view;
	}

	private int getMinLines() {
		String lines = PreferenceManager.getDefaultSharedPreferences(getContext()).getString(SettingsFragment.HOME_SCREEN_LINES_KEY, String.valueOf(DEFAULT_MIN_LINES));
		try {
			return Integer.parseInt(lines);
		} catch (NumberFormatException e) {
			return DEFAULT_MIN_LINES;
		}
	}

	private boolean requestEditNoteActivity(Note note) {
		if (!isFullscreenEditMode()) {
			Log.i(LOGTAG, "A request was made to edit via fullscreen. But it was not enabled in the preferences.");
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

	private String getNoteTime(Note note) {
		Date date = note.getTimestampAsDate();
		Locale locale = Locale.getDefault();
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
		String date_lod = preferences.getString("prefs_date_detail", "?");
		String time_lod = preferences.getString("prefs_time_detail", "?");

		DateFormat f = DateFormat.getDateTimeInstance(getLoDviaPreference(date_lod), getLoDviaPreference(time_lod), locale);
		String formattedDate = f.format(date);
		//Log.i(LOGTAG, "Parsed date: " + formattedDate + " from locale: " + locale.getCountry());
		return formattedDate;
	}

	private int getLoDviaPreference(String lod) {
		int i = 0;
		try {
			i = Integer.parseInt(lod);
		} catch (NumberFormatException e) {
			Log.w(LOGTAG, "Warning: Could not interpret this as a number: " + lod);
		}

		switch (i) {
			case 0:
				return DateFormat.FULL;
			case 1:
				return DateFormat.LONG;
			case 2:
				return DateFormat.MEDIUM;
			case 3:
				return DateFormat.SHORT;
		}

		//if (lod.equals(res.getString(R.string.prefs_full))) return DateFormat.FULL;
		//if (lod.equals(res.getString(R.string.prefs_long))) return DateFormat.LONG;
		//if (lod.equals(res.getString(R.string.prefs_medium))) return DateFormat.MEDIUM;
		//if (lod.equals(res.getString(R.string.prefs_short))) return DateFormat.SHORT;

		Log.w(LOGTAG, "Warning: Level of Detail not found, reverting to default. Input: " + lod);
		return DEFAULT_LEVEL_OF_DETAIL;
	}
}
