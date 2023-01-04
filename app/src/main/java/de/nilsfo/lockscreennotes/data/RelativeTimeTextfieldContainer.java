package de.nilsfo.lockscreennotes.data;

import android.content.Context;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import de.nilsfo.lockscreennotes.util.TimeUtils;
import de.nilsfo.lsn.R;
import timber.log.Timber;

/**
 * Created by Nils on 21.02.2017.
 */

public class RelativeTimeTextfieldContainer {

	private static RelativeTimeTextfieldContainer container;
	private ArrayList<TextView> viewList;
	private LinkedList<TextView> deleteList;
	private HashMap<TextView, Long> timestampMap;

	private RelativeTimeTextfieldContainer() {
		viewList = new ArrayList<TextView>();
		timestampMap = new HashMap<TextView, Long>();
		deleteList = new LinkedList<TextView>();
	}

	public void clear() {
		viewList.clear();
	}

	public void add(TextView view, long timestamp) {
		Timber.v("Requested to add view [" + view.hashCode() + "] to the list.");
		if (!viewList.contains(view)) {
			viewList.add(view);
			timestampMap.put(view, timestamp);
			Timber.v("... and it succeeded!");
		} else {
			timestampMap.put(view, timestamp);
			Timber.v("... but it failed. It was already contained.");
		}
	}

	private void remove(TextView view) {
		viewList.remove(view);
		timestampMap.remove(view);
	}

	public void requestDelete(TextView view) {
		deleteList.add(view);
	}

	public void updateText(Context context) {
		Timber.v("Updating relative texts. Registered Views: " + viewList.size() + ". Timestamps: " + timestampMap.keySet().size() + ". DeleteQ: " + deleteList.size());
		while (!deleteList.isEmpty()) {
			remove(deleteList.pop());
		}

		for (TextView v : viewList) {
			Timber.v("TextUpdater: Is Null? v " + (v == null) + "? Map? " + (timestampMap == null) + ". In Map? " + timestampMap.containsKey(v));
			if (v == null) {
				Timber.w("Failed to fetch a textview from the list!");
				continue;
			}
			boolean contains = timestampMap.containsKey(v);
			if (contains) {
				long timestamp = Note.DEFAULT_TIMESTAMP;
				try {
					timestamp = timestampMap.get(v);
				} catch (Exception e) {
					Timber.e(e);
					Timber.e("Failed to get timestamp for: " + v);
					Toast.makeText(context, R.string.error_internal_error, Toast.LENGTH_LONG).show();
				}

				if (v.isShown()) {
					v.setText(context.getString(R.string.last_edited, new TimeUtils(context).formatRelative(timestamp)));
				}
			} else {
				v.setText(context.getString(R.string.last_edited, context.getText(R.string.error_internal_error)));
				Timber.e("The timestamp map did a big oof.");
				deleteList.add(v);
			}
		}
		Timber.v("Updating displayed note times. Updated count: " + viewList.size() + " To be deleted: " + deleteList.size());
	}

	public static RelativeTimeTextfieldContainer getContainer() {
		if (container == null) container = new RelativeTimeTextfieldContainer();
		return container;
	}
}
