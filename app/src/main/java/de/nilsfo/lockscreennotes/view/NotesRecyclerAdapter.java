package de.nilsfo.lockscreennotes.view;

import static de.nilsfo.lockscreennotes.util.NotesNotificationManager.PREFERENCE_REVERSE_ORDERING;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;

import de.nilsfo.lockscreennotes.LockScreenNotes;
import de.nilsfo.lockscreennotes.data.Note;
import de.nilsfo.lockscreennotes.data.RelativeTimeTextfieldContainer;
import de.nilsfo.lockscreennotes.data.content.NoteContentAnalyzer;
import de.nilsfo.lockscreennotes.imported.view.LinedTextView;
import de.nilsfo.lockscreennotes.sql.DBAdapter;
import de.nilsfo.lockscreennotes.util.TimeUtils;
import de.nilsfo.lsn.R;
import timber.log.Timber;

public class NotesRecyclerAdapter extends RecyclerView.Adapter<NotesRecyclerAdapter.ViewHolder> {

	private DBAdapter adapter;
	private Context context;
	private ArrayList<Long> noteIDs;
	private boolean reversed;
	private NotesRecyclerAdapterListener listener;

	public NotesRecyclerAdapter(DBAdapter adapter, Context context) {
		this.adapter = adapter;
		this.context = context;

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		reversed = preferences.getBoolean(PREFERENCE_REVERSE_ORDERING, false);

		Timber.i("Requested a new notes recycler adapter for notes!");
		refreshNotesList();
	}

	@NotNull
	@Override
	public ViewHolder onCreateViewHolder(@NotNull ViewGroup viewGroup, int i) {
		Timber.i("Creating view holder - " + i + ". ViewGroup: " + viewGroup);
		View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.card_note_layout, viewGroup, false);
		ViewHolder holder = new ViewHolder(v, context);
		holder.setDefault();
		return holder;
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
		long id = noteIDs.get(i);
		Timber.i("Binding viewHolder for index " + i + " -> Note ID: " + id);

		Note note = null;
		try {
			note = Note.getNoteFromDB(id, adapter);
		} catch (Exception e) {
			Timber.e(e);
			Toast.makeText(context, R.string.error_internal_error, Toast.LENGTH_LONG).show();
		}

		if (note == null) {
			Timber.e("Failed to retrieve a note with ID " + id);
			viewHolder.setDefault();
			return;
		}

		Timber.i("Retrieved a note for position " + i + ": " + note.getTextPreview());
		Timber.i("Changing Note IDs: " + viewHolder.getNoteID() + " -> " + note.getDatabaseID());

		viewHolder.setNote(note, i, context);
		if (hasListener()) {
			viewHolder.setListener(listener);
		}
	}

	@Override
	public int getItemCount() {
		return noteIDs.size();
	}

	public boolean hasListener() {
		return getListener() != null;
	}

	public Long getItemAt(int index) {
		return noteIDs.get(index);
	}

	public void refreshNotesList() {
		Timber.i("Updating recycler adapter items.");
		noteIDs = new ArrayList<>();
		ArrayList<Long> timestamps = new ArrayList<>();

		Cursor IDcursor = adapter.getAllIDsSorted();
		if (IDcursor.moveToFirst()) {
			do {
				long id = IDcursor.getLong(DBAdapter.COL_ROWID);
				noteIDs.add(id);
			} while (IDcursor.moveToNext());
		}
		Timber.i("Finished updating items.");

		if (!reversed) {
			Collections.reverse(noteIDs);
		}

		notifyDataSetChanged();
		Timber.i("Notified change in dataset.");
	}

	public NotesRecyclerAdapterListener getListener() {
		return listener;
	}

	public void setListener(NotesRecyclerAdapterListener listener) {
		this.listener = listener;
	}

	public boolean isReversed() {
		return reversed;
	}

	public boolean isEmpty() {
		return noteIDs.isEmpty();
	}

	public interface NotesRecyclerAdapterListener {
		public void onCardNotePressed(long noteID);

		public void onCardNotePressedLong(long noteID);

		public void onCardNoteMenuPressed(long noteID, MenuItem itemID);

		public void onCardNoteToggleImagePressed(long noteID);
	}

	class ViewHolder extends RecyclerView.ViewHolder implements PopupMenu.OnMenuItemClickListener {

		private long noteID;
		private ImageView noteImageIM;
		private TextView statusbarLB;
		private LinedTextView noteText;
		private TextView timestampLB;
		private ImageView menu;
		private View itemView;
		private NotesRecyclerAdapterListener listener;
		private NoteContentAnalyzer contentAnalyzer;

		public ViewHolder(final View itemView, Context context) {
			super(itemView);
			this.itemView = itemView;

			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			Configuration configuration = context.getResources().getConfiguration();

			noteID = -1; //TODO reference a static final field?
			Timber.e("A new ViewHolder has been created!");

			noteImageIM = (ImageView) itemView.findViewById(R.id.card_note_image);
			statusbarLB = (TextView) itemView.findViewById(R.id.note_status_bar);
			timestampLB = (TextView) itemView.findViewById(R.id.note_timestamp);
			menu = (ImageView) itemView.findViewById(R.id.card_menu_more);
			noteText = (LinedTextView) itemView.findViewById(R.id.note_text); //TODO make this not focusable!

			noteText.setTextAppearance(context, android.R.style.TextAppearance_Medium);
			if (preferences.getBoolean("prefs_large_font", false)) {
				noteText.setTextAppearance(context, android.R.style.TextAppearance_Large);
			}

			itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					int position = getAdapterPosition();
					Timber.i("Ayy, there was an item press! Pos.: " + position + ". Note ID: " + noteID);

					if (hasListener()) {
						listener.onCardNotePressed(noteID);
					}
				}
			});
			itemView.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					int position = getAdapterPosition();
					Timber.i("Ayy, there was an item long press! Pos.: " + position + ". Note ID: " + noteID);

					if (noteID != -1) { //TODO hardcoded
						showPopupMenu(itemView);
						if (hasListener()) {
							listener.onCardNotePressedLong(noteID);
						}
					}
					return true;
				}
			});
			noteImageIM.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Timber.i("Note toggle item pressed: " + getItemId());

					if (noteID != -1) {
						if (hasListener()) {
							listener.onCardNoteToggleImagePressed(noteID);
						}
					}
				}
			});

			menu.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showPopupMenu(menu);
				}
			});
			if (LockScreenNotes.isDarkMode(configuration)) {
				menu.setImageResource(R.drawable.baseline_more_vert_white_24);
			}
		}

		public void setNote(Note note, int index, Context context) {
			if (note == null || note.getDatabaseID() == -1) { //TODO fix hardcode ID?
				setDefault();
				return;
			}

			contentAnalyzer = new NoteContentAnalyzer(note);
			noteImageIM.setClickable(true);
			menu.setClickable(true);
			itemView.setClickable(true);

			TimeUtils utils = new TimeUtils(context);
			noteID = note.getDatabaseID();
			int imageID = R.drawable.disabled_button;
			if (note.isEnabled()) {
				imageID = R.drawable.enabled_button;
			}
			noteImageIM.setImageResource(imageID);
			noteText.setText(note.getText().trim());

			long time = note.getTimestamp();
			if (utils.isRelativeTimePrefered()) {
				timestampLB.setText(context.getString(R.string.last_edited, utils.formatRelative(time)));
				RelativeTimeTextfieldContainer.getContainer().add(timestampLB, time);
			} else {
				timestampLB.setText(context.getString(R.string.last_edited, utils.formatAbsolute(time)));
			}

			statusbarLB.setText(context.getString(R.string.note_position, String.valueOf(index + 1)));
			Timber.i("Note content status check: URL: " + contentAnalyzer.containsURL() + ". Number: " + contentAnalyzer.containsPhoneNumber() + ". E-Mail: " + contentAnalyzer.containsEMail());
		}

		public void setDefault() {
			noteImageIM.setClickable(false);
			menu.setClickable(false);
			itemView.setClickable(false);
			contentAnalyzer = null;

			noteImageIM.setImageResource(R.drawable.baseline_error_outline_black_18);
			statusbarLB.setText(R.string.error_internal_error);
			timestampLB.setText(R.string.error_internal_error);
			noteText.setText(R.string.error_internal_error);

			//RelativeTimeTextfieldContainer.getContainer().requestDelete(timestampLB);
		}

		private void showPopupMenu(View source) {
			Timber.i("Showing popup menu for note ID: " + noteID + ". Source: " + source.toString());
			PopupMenu popupMenu = new PopupMenu(context, source);
			popupMenu.inflate(R.menu.note_card_menu);
			popupMenu.setOnMenuItemClickListener(this);

			Menu menu = popupMenu.getMenu();
			MenuItem menuURL = menu.findItem(R.id.action_card_open_url);
			MenuItem menuPhone = menu.findItem(R.id.action_card_open_phone);
			MenuItem menuMail = menu.findItem(R.id.action_card_open_mail);

			if (!contentAnalyzer.containsURL()) {
				Timber.i("Disabling URL menu item.");
				menuURL.setVisible(false);
			}
			if (!contentAnalyzer.containsPhoneNumber()) {
				Timber.i("Disabling phone menu item.");
				menuPhone.setVisible(false);
			}
			if (!contentAnalyzer.containsEMail()) {
				Timber.i("Disabling mail menu item.");
				menuMail.setVisible(false);
			}

			popupMenu.show();
		}

		@Override
		public boolean onMenuItemClick(MenuItem menuItem) {
			if (hasListener()) {
				getListener().onCardNoteMenuPressed(noteID, menuItem);
			}

			return true;
		}

		public void removeListener() {
			listener = null;
		}

		public boolean hasListener() {
			return getListener() != null;
		}

		public NotesRecyclerAdapterListener getListener() {
			return listener;
		}

		public void setListener(NotesRecyclerAdapterListener listener) {
			this.listener = listener;
		}

		public long getNoteID() {
			return noteID;
		}
	}
}
