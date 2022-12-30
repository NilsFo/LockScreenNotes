package de.nilsfo.lockscreennotes.data.content;

import static de.nilsfo.lockscreennotes.util.RegexManager.MAIL_PATTERN;
import static de.nilsfo.lockscreennotes.util.RegexManager.PHONE_NUMBER_PATTERN;

import java.util.ArrayList;

import de.nilsfo.lockscreennotes.data.Note;
import de.nilsfo.lockscreennotes.util.RegexManager;
import de.nilsfo.lockscreennotes.util.URLUtils;
import timber.log.Timber;

public class NoteContentAnalyzer {

	public static final int DEFAULT_PHONE_NUMBER_MIN_LENGTH = 5;
	private String inputText;
	private ArrayList<String> urlList;
	private ArrayList<String> mailList;
	private ArrayList<String> phoneList;

	public NoteContentAnalyzer(String inputText) {
		if (inputText == null) {
			Timber.e("Text to analyze is null!!");
			inputText = "";
		}
		this.inputText = inputText;
	}

	public NoteContentAnalyzer(Note source) {
		this(source.getText());
	}

	public boolean containsURL() {
		return !getURLs().isEmpty();
	}

	public boolean containsEMail() {
		return !getMails().isEmpty();
	}

	public boolean containsPhoneNumber() {
		return !getPhoneNumbers().isEmpty();
	}

	public ArrayList<String> getPhoneNumbers(int minimumCharacterLength) {
		if (minimumCharacterLength <= 0) {
			throw new IllegalArgumentException("Minimum phone number character [" + minimumCharacterLength + "] must not be zero or negative!");
		}

		if (phoneList == null) {
			ArrayList<String> tempList = new RegexManager(PHONE_NUMBER_PATTERN).findMatchesInText(inputText);
			phoneList = new ArrayList<>();
			for (String s : tempList) {
				if (s.length() >= minimumCharacterLength) {
					phoneList.add(s.trim());
				}
			}
		}
		return phoneList;
	}

	public boolean containsAnything() {
		return containsURL() || containsEMail() || containsPhoneNumber();
	}

	public ArrayList<String> getPhoneNumbers() {
		return getPhoneNumbers(DEFAULT_PHONE_NUMBER_MIN_LENGTH);
	}

	public ArrayList<String> getURLs() {
		if (urlList == null) {
			urlList = URLUtils.getURLRegexManager().findMatchesInText(inputText);
		}
		return urlList;
	}

	public ArrayList<String> getMails() {
		if (mailList == null) {
			mailList = new RegexManager(MAIL_PATTERN).findMatchesInText(inputText);
		}
		return mailList;
	}
}
