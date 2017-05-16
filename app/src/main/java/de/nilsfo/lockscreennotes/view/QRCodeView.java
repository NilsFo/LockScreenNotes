package de.nilsfo.lockscreennotes.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.os.AsyncTaskCompat;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import de.nilsfo.lsn.R;
import timber.log.Timber;

/**
 * Created by Nils on 15.05.2017.
 */

public class QRCodeView extends LinearLayout {

	private ProgressBar bar;

	public QRCodeView(Context context, String textToDisplay, int size) {
		super(context);

		Resources resources = getResources();
		bar = new ProgressBar(getContext());
		bar.setIndeterminate(true);
		bar.setPadding((int) resources.getDimension(R.dimen.activity_horizontal_margin), (int) resources.getDimension(R.dimen.activity_vertical_margin), (int) resources.getDimension(R.dimen.activity_horizontal_margin), (int) resources.getDimension(R.dimen.activity_vertical_margin));

		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		lp.gravity = Gravity.CENTER_HORIZONTAL;
		bar.setLayoutParams(lp);
		addView(bar);

		QRTask task = new QRTask(textToDisplay, size);
		AsyncTaskCompat.executeParallel(task);
	}

	public void displayQRImage(Bitmap map) {
		Timber.i("Displaying QR Bitmap!");
		removeView(bar);

		Context context = getContext();
		Resources resources = getResources();
		ImageView imageView = new ImageView(context);
		imageView.setPadding((int) resources.getDimension(R.dimen.activity_horizontal_margin_small), (int) resources.getDimension(R.dimen.activity_vertical_margin_small), (int) resources.getDimension(R.dimen.activity_horizontal_margin_small), (int) resources.getDimension(R.dimen.activity_vertical_margin_small));

		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		lp.gravity = Gravity.CENTER_HORIZONTAL;
		imageView.setLayoutParams(lp);
		imageView.setImageBitmap(map);
		addView(imageView);
	}

	private class QRTask extends AsyncTask<Void, Void, Bitmap> {

		private String text;
		private int size;

		public QRTask(String text, int size) {
			this.text = text;
			this.size = size;
		}

		@Override
		protected Bitmap doInBackground(Void... params) {
			Timber.i("Convertion from '" + text + "' to Bitmap starts now.");
			QRCodeWriter writer = new QRCodeWriter();
			Bitmap bmp = null;

			try {
				BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size);
				int width = bitMatrix.getWidth();
				int height = bitMatrix.getHeight();
				bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
				for (int x = 0; x < width; x++) {
					for (int y = 0; y < height; y++) {
						bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
					}
				}

			} catch (WriterException e) {
				e.printStackTrace();
				Timber.e(e, "Error while creating the QR code for " + text);
				return null;
			}
			return bmp;
		}

		@Override
		protected void onPostExecute(Bitmap map) {
			Timber.i("Finished QR Bitmap creation!");

			if (map != null) {
				Timber.i("Parsing into Dialog...");
				displayQRImage(map);
			} else {
				Timber.w("QR Bitmap creation failed!");
				Toast.makeText(getContext(), R.string.error_qr_generation_failed,Toast.LENGTH_LONG).show();
			}
		}
	}

}
