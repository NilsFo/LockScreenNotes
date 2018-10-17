package de.nilsfo.lockscreennotes.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import de.nilsfo.lockscreennotes.activity.NotificationDeleteRecieverDialogActivity;
import de.nilsfo.lsn.R;
import timber.log.Timber;

@Deprecated
public class NotificationDeleteReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

		Bundle extras = intent.getExtras();
		if (extras == null) {
			Timber.e("I just got a 'NoitificationDeleted' intent! But there were no extras! Could not perform any actions!");
			Toast.makeText(context, R.string.error_internal_error, Toast.LENGTH_LONG).show();
			return;
		}

		Intent dialogIntent = new Intent(context, NotificationDeleteRecieverDialogActivity.class);
		dialogIntent.putExtras(extras);
		context.startActivity(dialogIntent);
	}

}
