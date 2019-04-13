package de.nilsfo.lockscreennotes.data.font;

import java.util.ArrayList;

import de.nilsfo.lockscreennotes.data.Note;
import de.nilsfo.lockscreennotes.util.RegexManager;
import de.nilsfo.lockscreennotes.util.URLUtils;
import timber.log.Timber;

import static de.nilsfo.lockscreennotes.util.RegexManager.MAIL_PATTERN;
import static de.nilsfo.lockscreennotes.util.RegexManager.PHONE_NUMBER_PATTERN;

public class NoteContentAnalyzer {

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

	public ArrayList<String> getPhoneNumbers() {
		if (phoneList == null) {
			phoneList = new RegexManager(PHONE_NUMBER_PATTERN).findMatchesInText(inputText);
		}
		return phoneList;
	}

}
