package de.nilsfo.lockscreennotes.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import de.nilsfo.lsn.R;
import timber.log.Timber;

import static de.nilsfo.lockscreennotes.util.NoteSharer.INTENT_TYPE;

public class IntentUtils {

	private Context context;

	public IntentUtils(Context context) {
		this.context = context;
	}

	public Intent getNoteShareIntent(String text) {
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, text);
		sendIntent.setType(INTENT_TYPE);
		return Intent.createChooser(sendIntent, context.getString(R.string.share_using));
	}

	public Intent getUrlIntent(String url) {
		if (!url.toLowerCase().startsWith("http://") && !url.toLowerCase().startsWith("https://")) {
			url = "http://" + url;
		}
		Timber.i("Sending out URL browse intent: " + url);

		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(url));
		return intent;
	}

	public Intent getPhoneNumberIntent(String number) {
		String uri = "tel:" + number;
		Intent intent = new Intent(Intent.ACTION_DIAL);
		intent.setData(Uri.parse(uri));
		return intent;
	}

	public Intent getMailIntent(String mail, boolean chooser) {
		Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", mail, null));
		emailIntent.putExtra(Intent.EXTRA_EMAIL, mail);

		if (chooser) {
			String title = context.getString(R.string.mail_via);
			return Intent.createChooser(emailIntent, title);
		}
		return emailIntent;
	}
}
