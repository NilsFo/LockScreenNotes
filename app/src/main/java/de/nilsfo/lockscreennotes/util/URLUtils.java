package de.nilsfo.lockscreennotes.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class URLUtils {

	//public static final String URL_REGEX = "(?:(?:https?|ftp|file):\\/\\/|www\\.|ftp\.)(?:\\([-A-Z0-9+&@#\\/%=~_|$?!:,.]*\\)|[-A-Z0-9+&@#\\/%=~_|$?!:,.])*(?:\([-A-Z0-9+&@#\\/%=~_|$?!:,.]*\)|[A-Z0-9+&@#\\/%=~_|$])";
	//private static final String URL_REGEX = "((?:https\\:\\/\\/)|(?:http\\:\\/\\/)|(?:www\\.))?([a-zA-Z0-9\\-\\.]+\\.[a-zA-Z]{2,3}(?:\\??)[a-zA-Z0-9\\-\\._\\?\\,\\'\\/\\\\\\+&%\\$#\\=~]+)";
	public static final String URL_PATTERN = "(https?:\\/\\/)?([\\da-z\\.-]+\\.[a-z\\.]{2,6}|[\\d\\.]+)([\\/:?=&#]{1}[\\da-z\\.-]+)*[\\/\\?]?";


	private Context context;

	public URLUtils(Context context) {
		this.context = context;
	}

	public static RegexManager getURLRegexManager() {
		return new RegexManager(URL_PATTERN);
	}

	public boolean containsSingleURL(String text) {
		return getURLRegexManager().containsSingleMatch(text);
	}

	public void browseURL(String url) {
		if (!url.toLowerCase().startsWith("http://") && !url.toLowerCase().startsWith("https://")) {
			url = "http://" + url;
		}

		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(url));
		context.startActivity(i);
	}


}
