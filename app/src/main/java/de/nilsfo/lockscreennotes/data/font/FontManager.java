package de.nilsfo.lockscreennotes.data.font;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by Nils on 30.11.2016.
 */

@Deprecated
public class FontManager {

	public static final String ROOT = "fonts/";
	@Deprecated
	public static final String FONTAWESOME = ROOT + "fontawesome-webfont.ttf";

	public static Typeface getTypeface(Context context, String font) {
		return Typeface.createFromAsset(context.getAssets(), font);
	}

	public static void markAsIconContainer(View v, Typeface typeface) {
		if (v instanceof ViewGroup) {
			ViewGroup vg = (ViewGroup) v;
			for (int i = 0; i < vg.getChildCount(); i++) {
				View child = vg.getChildAt(i);
				markAsIconContainer(child, typeface);
			}
		} else if (v instanceof TextView) {
			((TextView) v).setTypeface(typeface);
		}
	}
}