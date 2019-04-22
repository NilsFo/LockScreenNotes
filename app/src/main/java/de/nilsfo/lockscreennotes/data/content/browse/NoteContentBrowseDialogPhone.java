package de.nilsfo.lockscreennotes.data.content.browse;

import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;

import de.nilsfo.lockscreennotes.data.content.NoteContentAnalyzer;
import de.nilsfo.lockscreennotes.util.IntentUtils;
import de.nilsfo.lsn.R;

public class NoteContentBrowseDialogPhone extends NoteContentBrowseDialog {
	public NoteContentBrowseDialogPhone(Context context) {
		super(context);
	}

	@Override
	protected void browseElement(String element) {
		Intent intent = new IntentUtils(context).getPhoneNumberIntent(element);
		context.startActivity(intent);
	}

	@Override
	protected ArrayList<String> getMatchesInText(String text) {
		return new NoteContentAnalyzer(text).getPhoneNumbers();
	}

	@Override
	protected int getErrorNothingSelectedText() {
		return R.string.error_nothing_selected;
	}

	@Override
	protected int getDialogPositiveButtonName() {
		return R.string.action_dial;
	}

	@Override
	protected int getDialogBrowseAllButtonName() {
		return -1;
	}

	@Override
	protected int getErrorNothingFoundText() {
		return R.string.error_no_numbers;
	}

	@Override
	protected int getDialogTitle() {
		return R.string.info_choose_phone_number;
	}

	@Override
	protected int getDialogIcon() {
		return R.drawable.baseline_phone_black_48;
	}

	@Override
	protected boolean isSingleSelection() {
		return true;
	}
}
