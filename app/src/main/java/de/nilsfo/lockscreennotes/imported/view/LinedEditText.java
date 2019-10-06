package de.nilsfo.lockscreennotes.imported.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatEditText;

/**
 * Created by Nils on 14.08.2016.
 */

public class LinedEditText extends AppCompatEditText {

	private LinedTextHelper helper;

	// we need this constructor for LayoutInflater
	public LinedEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		helper = new LinedTextHelper(context);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		//int count = getLineCount();
		int height = getHeight();
		int line_height = getLineHeight();
		int count = height / line_height;

		if (getLineCount() > count) {
			count = getLineCount();//for long text with scrolling
		}

		Rect r = helper.getRect();
		Paint paint = helper.getPaint();
		int baseline = getLineBounds(0, r);//first line

		for (int i = 0; i < count; i++) {
			canvas.drawLine(r.left, baseline + 1, r.right, baseline + 1, paint);
			baseline += getLineHeight();//next line
		}

		super.onDraw(canvas);
	}

	@Override
	public boolean isInEditMode() {
		return helper.isInEditMode();
	}

	@Override
	public int getMinimumWidth() {
		return helper.getMinimumWidth();
	}

}