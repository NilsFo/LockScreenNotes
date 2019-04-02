package de.nilsfo.lockscreennotes.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import timber.log.Timber;

import static de.nilsfo.lockscreennotes.util.RegexManager.URL_PATTERN;

public class URLUtils {
	private Context context;

	public URLUtils(Context context) {
		this.context = context;
	}

	public boolean containsSingleURL(String text) {
		return getURLRegexManager().containsSingleMatch(text);
	}

	public void browseURL(String url) {
		if (!url.toLowerCase().startsWith("http://") && !url.toLowerCase().startsWith("https://")) {
			url = "http://" + url;
		}
		Timber.i("Sending out URL browse intent: " + url);

		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(url));
		context.startActivity(i);
	}

	public static RegexManager getURLRegexManager() {
		return new RegexManager(URL_PATTERN);
	}


}
