package de.nilsfo.lockscreennotes.util;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

public class RegexManager {

	//public static final String URL_REGEX = "(?:(?:https?|ftp|file):\\/\\/|www\\.|ftp\.)(?:\\([-A-Z0-9+&@#\\/%=~_|$?!:,.]*\\)|[-A-Z0-9+&@#\\/%=~_|$?!:,.])*(?:\([-A-Z0-9+&@#\\/%=~_|$?!:,.]*\)|[A-Z0-9+&@#\\/%=~_|$])";
	//private static final String URL_REGEX = "((?:https\\:\\/\\/)|(?:http\\:\\/\\/)|(?:www\\.))?([a-zA-Z0-9\\-\\.]+\\.[a-zA-Z]{2,3}(?:\\??)[a-zA-Z0-9\\-\\._\\?\\,\\'\\/\\\\\\+&%\\$#\\=~]+)";
	public static final String URL_PATTERN = "(https?:\\/\\/)?([\\da-z\\.-]+\\.[a-z\\.]{2,6}|[\\d\\.]+)([\\/:?=&#]{1}[\\da-z\\.-]+)*[\\/\\?]?";
	public static final String PHONE_NUMBER_PATTERN = "[+]*[(]{0,1}[0-9]{1,4}[)]{0,1}[-\\s\\./0-9]*";
	public static final String MAIL_PATTERN = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";

	public static final int ENTRY_DEFAULT_MIN_SIZE = 0;

	private String pattern;
	private int minimumLength;
	private boolean nonFullNumeric;

	public RegexManager(String pattern) {
		this(pattern, ENTRY_DEFAULT_MIN_SIZE, false);
	}

	public RegexManager(String pattern, int minimumLength, boolean nonFullNumeric) {
		this.pattern = pattern;
		this.minimumLength = minimumLength;
		this.nonFullNumeric = nonFullNumeric;
	}

	public ArrayList<String> findMatchesInText(String text) {
		Matcher m = applyRegexToText(text);
		if (m == null) {
			return new ArrayList<>();
		}

		ArrayList<String> list = new ArrayList<>();
		while (m.find()) {
			String match = m.group();
			Timber.v("Found a match: " + m.group());

			boolean add = true;
			for (String s : list) {
				if (s.equals(match)) {
					add = false;
					break;
				}
			}

			if (add) {
				if (match.length() < getMinimumLength()) {
					continue;
				}
				if (isNonFullNumeric() && isNumeric(match)) {
					continue;
				}

				list.add(match);
			}
		}

		return list;
	}

	private boolean isNumeric(String text) {
		Timber.i("Checking if '" + text + "' is a number.");
		try {
			Double.parseDouble(text);
			return true;
		} catch (Exception e1) {
			try {
				Integer.parseInt(text);
				return true;
			} catch (Exception e2) {
				return false;
			}
		}
	}

	public boolean containsSingleMatch(String input) {
		ArrayList<String> list = findMatchesInText(input);
		return list != null && list.size() == 1;
	}

	public boolean isSingleMatch(String input) {
		if (!containsSingleMatch(input)) return false;

		ArrayList<String> list = findMatchesInText(input);
		return list.get(0).equals(input);
	}

	public boolean isMatch(String text) {
		return !findMatchesInText(text).isEmpty();
	}

	public Matcher applyRegexToText(String input) {
		Timber.v("Applying Regex: " + pattern);
		Timber.v("Text to apply it to: " + input);

		if (input == null || input.trim().equals("")) return null;

		Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
		return p.matcher(input);
	}

	public String getPattern() {
		return pattern;
	}

	public int getMinimumLength() {
		return minimumLength;
	}

	public void setMinimumLength(int minimumLength) {
		this.minimumLength = minimumLength;
	}

	public boolean isNonFullNumeric() {
		return nonFullNumeric;
	}

	public void setNonFullNumeric(boolean nonFullNumeric) {
		this.nonFullNumeric = nonFullNumeric;
	}

}
