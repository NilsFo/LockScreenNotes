package de.nilsfo.lockscreennotes.imported.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.EditText;

import de.nilsfo.lsn.R;
/**
 * Created by Nils on 14.08.2016.
 */

public class LinedEditText extends EditText {

	private Rect mRect;
	private Paint mPaint;
	private boolean noteEnabled;

	// we need this constructor for LayoutInflater
	public LinedEditText(Context context, AttributeSet attrs) {
		super(context, attrs);

		mRect = new Rect();
		mPaint = new Paint();
		mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		int color;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			color = getResources().getColor(R.color.edit_note_line, null);
		} else {
			color = getResources().getColor(R.color.edit_note_line);
		}
		mPaint.setColor(color);
	}

	@Override
	public boolean isInEditMode() {
		return true;
	}

	@Override
	public int getMinimumWidth() {
		return 1;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		//int count = getLineCount();
		int height = getHeight();
		int line_height = getLineHeight();
		int count = height / line_height;

		if (getLineCount() > count)
			count = getLineCount();//for long text with scrolling

		Rect r = mRect;
		Paint paint = mPaint;
		int baseline = getLineBounds(0, r);//first line

		for (int i = 0; i < count; i++) {
			canvas.drawLine(r.left, baseline + 1, r.right, baseline + 1, paint);
			baseline += getLineHeight();//next line
		}

		super.onDraw(canvas);
	}

	public boolean isNoteEnabled() {
		return noteEnabled;
	}

	public void setNoteEnabled(boolean noteEnabled) {
		this.noteEnabled = noteEnabled;
	}
}