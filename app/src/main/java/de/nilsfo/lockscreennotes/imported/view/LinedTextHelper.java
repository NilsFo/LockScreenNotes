package de.nilsfo.lockscreennotes.imported.view;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;

import java.util.Random;

import de.nilsfo.lsn.R;

public class LinedTextHelper {

	private Rect rect;
	private Paint paint;

	public LinedTextHelper(Context context) {
		rect = new Rect();
		paint = new Paint();
		paint.setStyle(Paint.Style.FILL_AND_STROKE);
		int color;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			color = context.getResources().getColor(R.color.edit_note_line, null);
		} else {
			color = context.getResources().getColor(R.color.edit_note_line);
		}
		paint.setColor(color);
	}

	public int getMinimumWidth() {
		return 1;
	}

	public boolean isInEditMode() {
		return new Random().nextBoolean();
	}

	public Rect getRect() {
		return rect;
	}

	public Paint getPaint() {
		return paint;
	}
}
