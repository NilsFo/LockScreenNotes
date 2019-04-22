package de.nilsfo.lockscreennotes.data.content.browse;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.util.ArrayList;

import de.nilsfo.lockscreennotes.data.content.NoteContentAnalyzer;
import de.nilsfo.lsn.R;

public class NoteContentBrowseDialogMail extends NoteContentBrowseDialog {

	public NoteContentBrowseDialogMail(Context context) {
		super(context);
	}

	@Override
	protected void browseElement(String element) {
		String title = context.getString(R.string.share_via);
		Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", element, null));
		emailIntent.putExtra(Intent.EXTRA_EMAIL, element);

		context.startActivity(Intent.createChooser(emailIntent, title));
	}

	@Override
	protected ArrayList<String> getMatchesInText(String text) {
		return new NoteContentAnalyzer(text).getMails();
	}

	@Override
	protected int getErrorNothingSelectedText() {
		return R.string.error_nothing_selected;
	}

	@Override
	protected int getDialogPositiveButtonName() {
		return R.string.action_write_mail;
	}

	@Override
	protected int getDialogBrowseAllButtonName() {
		return -1;
	}

	@Override
	protected int getErrorNothingFoundText() {
		return R.string.error_no_mail;
	}

	@Override
	protected int getDialogTitle() {
		return R.string.info_choose_mail;
	}

	@Override
	protected int getDialogIcon() {
		return R.drawable.baseline_email_black_48;
	}

	@Override
	protected boolean isSingleSelection() {
		return true;
	}
}
