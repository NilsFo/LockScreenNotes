package de.nilsfo.lockscreennotes.util;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

public class RegexManager {

	private String pattern;

	public RegexManager(String pattern) {
		this.pattern = pattern;
	}

	public ArrayList<String> findMatchesInText(String text) {
		Matcher m = applyRegexToText(text);
		if (m == null) {
			return null;
		}

		ArrayList<String> list = new ArrayList<>();
		while (m.find()) {
			String match = m.group();
			Timber.v("Found a match: " + m.group());

			boolean add = true;
			for (String s : list) {
				if (s.equals(match)) {
					add = false;
				}
			}

			if (add) {
				list.add(match);
			}
		}

		return list;
	}

	public boolean containsSingleMatch(String input) {
		ArrayList<String> list = findMatchesInText(input);
		return list != null && list.size() == 1;
	}

	public boolean isSingleMatch(String input){
		if (!containsSingleMatch(input)) return false;

		ArrayList<String> list = findMatchesInText(input);
		return list.get(0).equals(input);
	}

	public Matcher applyRegexToText(String input) {
		Timber.v("Applying Regex: " + pattern);
		Timber.v("Text to apply it to: " + input);

		if (input == null || input.trim().equals("")) return null;

		Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
		return p.matcher(input);
	}

}
