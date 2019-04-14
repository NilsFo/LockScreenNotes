package de.nilsfo.lockscreennotes.data.content.browse;

import android.content.Context;

import java.util.ArrayList;

import de.nilsfo.lockscreennotes.data.content.NoteContentAnalyzer;
import de.nilsfo.lockscreennotes.util.URLUtils;
import de.nilsfo.lsn.R;

public class NoteContentBrowseDialogURLs extends NoteContentBrowseDialog {
	public NoteContentBrowseDialogURLs(Context context) {
		super(context);
	}

	@Override
	protected void browseElement(String element) {
		URLUtils urlUtils = new URLUtils(context);
		urlUtils.browseURL(element);
	}

	@Override
	protected ArrayList<String> getMatchesInText(String text) {
		return new NoteContentAnalyzer(text).getURLs();
	}

	@Override
	protected int getErrorNothingSelectedText() {
		return R.string.error_nothing_selected;
	}

	@Override
	protected int getDialogPositiveButtonName() {
		return R.string.action_browse;
	}

	@Override
	protected int getDialogBrowseAllButtonName() {
		return R.string.action_browse_all;
	}

	@Override
	protected int getErrorNothingFoundText() {
		return R.string.error_no_weblings;
	}

	@Override
	protected int getDialogTitle() {
		return R.string.info_choose_url;
	}

	@Override
	protected boolean isSingleSelection() {
		return false;
	}
}
