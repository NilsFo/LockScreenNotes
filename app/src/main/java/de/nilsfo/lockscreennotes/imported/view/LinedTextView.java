package de.nilsfo.lockscreennotes.imported.view;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatTextView;
import de.nilsfo.lsn.R;
import timber.log.Timber;

public class LinedTextView extends AppCompatTextView {

	private final LinedTextHelper helper;

	// we need this constructor for LayoutInflater
	public LinedTextView(Context context, AttributeSet attrs) {
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

		if (helper == null) {
			String errorText = getContext().getString(R.string.error_line_rendering_contact_dev);
			Timber.w(errorText);
			Toast.makeText(getContext(), errorText, Toast.LENGTH_LONG).show();
		} else {
			Rect r = helper.getRect();
			Paint paint = helper.getPaint();
			int baseline = getLineBounds(0, r);//first line

			for (int i = 0; i < count; i++) {
				canvas.drawLine(r.left, baseline + 1, r.right, baseline + 1, paint);
				baseline += getLineHeight();//next line
			}
		}

		super.onDraw(canvas);
	}

	@Override
	public boolean isInEditMode() {
		if (helper == null) {
			Timber.e("NO LinedTextHelper DECLARED!");
			return false;
		}
		return helper.isInEditMode();
	}

	@Override
	public int getMinimumWidth() {
		if (helper == null) {
			return 1;
		}
		return helper.getMinimumWidth();
	}

}
