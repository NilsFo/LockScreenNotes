package de.nilsfo.lockscreennotes.util;

import android.content.Context;

import static de.nilsfo.lockscreennotes.util.RegexManager.URL_PATTERN;

public class URLUtils {
	public static final int URL_MIN_SIZE = 4;
	private Context context;

	public URLUtils(Context context) {
		this.context = context;
	}

	public boolean containsSingleURL(String text) {
		return getURLRegexManager().containsSingleMatch(text);
	}

	public void browseURL(String url) {
		context.startActivity(new IntentUtils(context).getUrlIntent(url));
	}

	public static RegexManager getURLRegexManager() {
		return new RegexManager(URL_PATTERN, URL_MIN_SIZE, true);
	}
}
