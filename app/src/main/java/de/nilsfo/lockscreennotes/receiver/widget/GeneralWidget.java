package de.nilsfo.lockscreennotes.receiver.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.Arrays;

import de.nilsfo.lockscreennotes.activity.MainActivity;
import timber.log.Timber;

@Deprecated
public class GeneralWidget extends AppWidgetProvider {

	@Override
	public void onReceive(Context context, Intent intent) {
		//Called on every intent ever sent to this widget
		super.onReceive(context, intent);

		Timber.i("GeneralWidget got a general intent: " + intent.getDataString());
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		//Called when the auto timer is triggered
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		Timber.w("LSN General-Widget: OnUpdate() called!");

		final int N = appWidgetIds.length;

		// Perform this loop procedure for each App Widget that belongs to this provider
		for (int i = 0; i < N; i++) {
			int appWidgetId = appWidgetIds[i];

			// Create an Intent to launch ExampleActivity
			Intent intent = new Intent(context, MainActivity.class);
			PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

			// Get the layout for the App Widget and attach an on-click listener
			// to the button

			//RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.appwidget_provider_layout);
			//views.setOnClickPendingIntent(R.id.button, pendingIntent);

			// Tell the AppWidgetManager to perform an update on the current app widget
			//appWidgetManager.updateAppWidget(appWidgetId, views);
		}
	}

	@Override
	public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
		//Called on resize
		super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

		Bundle b = appWidgetManager.getAppWidgetOptions(appWidgetId);
		Timber.i("GeneralWidget options changed:");
		Timber.i("Old: " + Arrays.toString(b.keySet().toArray()));
		Timber.i("New: " + Arrays.toString(newOptions.keySet().toArray()));
	}

	@Override
	public void onEnabled(Context context) {
		//Called when widget added
		super.onEnabled(context);
		Timber.i("LSN General-Widget added to host pane!");
	}

	@Override
	public void onDisabled(Context context) {
		//Called when widget is removed
		super.onDisabled(context);
		Timber.w("LSN General-Widget removed from host pane!");
	}
}
