package de.wavegate.tos.lockscreennotes.data.font;

import android.content.Context;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.text.Layout;
import android.util.TypedValue;

import de.wavegate.tos.lockscreennotes.imported.TextDrawable;
import timber.log.Timber;


/**
 * Created by Nils on 03.01.2017.
 */

public abstract class FontAwesomeDrawableBuilder {

	public static final int DEFAULT__MENU_ICON_SCALE = 9;

	public static TextDrawable get(Context context, String text, int size, int color) {
		TextDrawable faIcon = new TextDrawable(context);
		faIcon.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
		faIcon.setTextAlign(Layout.Alignment.ALIGN_CENTER);
		faIcon.setTypeface(FontManager.getTypeface(context, FontManager.FONTAWESOME));
		faIcon.setText(text);
		faIcon.setTextColor(color);

		return faIcon;
	}

	public static TextDrawable getOptionsIcon(Context context, int text) {
		double density = context.getResources().getDisplayMetrics().density;
		Timber.i("Screen density value: " + density);

		int itemScale;
		String prefScale = PreferenceManager.getDefaultSharedPreferences(context).getString("prefs_action_bar_icon_scale", String.valueOf(DEFAULT__MENU_ICON_SCALE));
		try {
			itemScale = Integer.parseInt(prefScale);
		} catch (NumberFormatException e) {
			Timber.e(e, "Failed to parse the scale from the preferences! Reverting to default. Found in the prefs: " + prefScale);
			itemScale = DEFAULT__MENU_ICON_SCALE;
		}
		itemScale++;

		TextDrawable faIcon = get(context, text, (int) (itemScale * density), Color.WHITE);
		faIcon.setAlpha(153);
		return faIcon;
	}

	public static TextDrawable get(Context context, int text, int size, int color) {
		return get(context, context.getString(text), size, color);
	}
}
