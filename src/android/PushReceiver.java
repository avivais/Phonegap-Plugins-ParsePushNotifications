package com.stratogos.cordova.parsePushNotifications;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.parse.ParseAnalytics;
import com.parse.ParsePushBroadcastReceiver;

public class PushReceiver extends ParsePushBroadcastReceiver {
	public static final String TAG = "PushReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "onReceive() fired.");
		Log.d(TAG, "intent: " + intent.toString());
		Log.d(TAG, "extras: " + intent.getExtras().toString());
		super.onReceive(context, intent);
	}

	@Override
	protected void onPushReceive(Context context, Intent intent) {
		Log.d(TAG, "onPushReceive() fired.");
		if (ParsePushNotificationPlugin.isInForeground()) {
			JSONObject jsonObj = null;
			if (intent.getExtras().getString("com.parse.Data") != null) {
				try {
					jsonObj = new JSONObject(intent.getExtras().getString("com.parse.Data"));
					jsonObj.remove("title");
					jsonObj.remove("alert");
				}
				catch (JSONException e) {}
				Log.d(TAG, "ParsePushNotificationPlugin.isInForeground() = true, Removed title and alert from payload: " + jsonObj.toString());
			}
			intent.removeExtra("com.parse.Data");
			intent.putExtra("com.parse.Data", jsonObj.toString());
		}
		super.onPushReceive(context, intent);
	}

	@Override
	protected void onPushDismiss(Context context, Intent intent) {
		Log.d(TAG, "onPushDismiss() fired.");
		super.onPushDismiss(context, intent);
	}

	@Override
	public void onPushOpen(Context context, Intent intent) {
		boolean isPluginActive = ParsePushNotificationPlugin.isActive();

		Bundle extras = intent.getExtras();
		if (extras != null)	{
			ParsePushNotificationPlugin.NotificationReceived(extras.getString("com.parse.Data"), ParsePushNotificationPlugin.isInForeground(), !isPluginActive);
		}
		Log.v(TAG, "ACTIVITY: " + getActivity(context, intent));
		Intent i = new Intent(context, getActivity(context, intent));
		i.putExtras(extras);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		ParseAnalytics.trackAppOpenedInBackground(i);
		Log.v(TAG, "onPushOpen: " + extras.toString());
		context.startActivity(i);
	}
}
