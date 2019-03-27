package de.nilsfo.lockscreennotes.concurrent;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.ArrayList;

import de.nilsfo.lsn.R;
import timber.log.Timber;

public class QRTask extends AsyncTask<Void, Integer, Bitmap> {

	private ArrayList<QRTaskListener> listeners;
	private String text;
	private int size;

	public QRTask(String text, int size) {
		if (text.equals("")) {
			text = " ";
		}

		this.text = text;
		this.size = size;
		listeners = new ArrayList<>();
	}

	@Override
	protected Bitmap doInBackground(Void... params) {
		Timber.i("Convertion from '" + text + "' to Bitmap starts now.");
		QRCodeWriter writer = new QRCodeWriter();
		Bitmap bmp = null;

		try {
			Timber.i("QR - Preparations");
			BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size);
			Timber.i("QR - Encoding finished");

			int width = bitMatrix.getWidth();
			int height = bitMatrix.getHeight();
			bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
				}
			}
			Timber.i("QR - Bitmatp available");

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

			for (QRTaskListener listener : listeners) {
				listener.onSuccess(map);
			}
		} else {
			Timber.w("QR Bitmap creation failed!");
			for (QRTaskListener listener : listeners) {
				listener.onFailure(R.string.error_qr_generation_failed);
			}
		}
	}

	public boolean addListener(QRTaskListener listener) {
		return listeners.add(listener);
	}

	public QRTaskListener remove(int index) {
		return listeners.remove(index);
	}

	public void clearListeners() {
		listeners.clear();
	}

	public interface QRTaskListener {
		public void onSuccess(Bitmap image);

		public void onFailure(int reason);
	}

}
