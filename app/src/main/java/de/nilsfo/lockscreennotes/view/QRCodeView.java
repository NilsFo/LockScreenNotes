package de.nilsfo.lockscreennotes.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;

import de.nilsfo.lockscreennotes.activity.EditNoteActivity;
import de.nilsfo.lockscreennotes.concurrent.QRTask;
import de.nilsfo.lsn.R;
import timber.log.Timber;

/**
 * Created by Nils on 15.05.2017.
 */

public class QRCodeView extends LinearLayout implements QRTask.QRTaskListener {

	protected ArrayList<QRFinishListener> listeners;
	private ProgressBar bar;
	private Bitmap qrImage;

	public QRCodeView(Context context) {
		this(context, "", EditNoteActivity.QR_IMAGE_SIZE);
	}

	public QRCodeView(Context context, String textToDisplay, int size) {
		super(context);

		listeners = new ArrayList<>();
		Resources resources = getResources();
		bar = new ProgressBar(getContext());
		bar.setIndeterminate(true);
		bar.setMax(size * size);
		bar.setProgress(0);
		bar.setPadding((int) resources.getDimension(R.dimen.activity_horizontal_margin), (int) resources.getDimension(R.dimen.activity_vertical_margin), (int) resources.getDimension(R.dimen.activity_horizontal_margin), (int) resources.getDimension(R.dimen.activity_vertical_margin));

		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		lp.gravity = Gravity.CENTER_HORIZONTAL;
		bar.setLayoutParams(lp);
		addView(bar);

		QRTask task = new QRTask(textToDisplay, size);
		task.addListener(this);
		task.execute();
		//AsyncTaskCompat.executeParallel(task);
	}

	public void displayQRImage(Bitmap map) {
		Timber.i("Displaying QR Bitmap!");
		qrImage = map;
		removeView(bar);

		Context context = getContext();
		Resources resources = getResources();
		ImageView imageView = new ImageView(context);
		imageView.setPadding((int) resources.getDimension(R.dimen.activity_horizontal_margin_small), (int) resources.getDimension(R.dimen.activity_vertical_margin_small), (int) resources.getDimension(R.dimen.activity_horizontal_margin_small), (int) resources.getDimension(R.dimen.activity_vertical_margin_small));

		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		lp.gravity = Gravity.CENTER_HORIZONTAL;
		imageView.setLayoutParams(lp);
		imageView.setImageBitmap(qrImage);
		addView(imageView);

		for (QRFinishListener listener : listeners) {
			Timber.i("Informing a listener!");
			listener.onFinished(qrImage);
		}
	}

	public boolean hasFinishedRenderingQRCode() {
		return qrImage != null;
	}

	public void addListener(QRFinishListener listener) {
		listeners.add(listener);
	}

	public boolean removeListener(QRFinishListener listener) {
		return listeners.remove(listener);
	}

	@Override
	public void onSuccess(Bitmap image) {
		displayQRImage(image);
	}

	@Override
	public void onFailure(int reason) {
		Toast.makeText(getContext(), reason, Toast.LENGTH_LONG).show();
	}

	public Bitmap getQrImage() {
		return qrImage;
	}

	public interface QRFinishListener {
		public void onFinished(Bitmap image);
	}
}
