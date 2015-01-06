package com.stratogos.cordova.parsePushNotifications;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.parse.ParseAnalytics;
import com.parse.ParsePushBroadcastReceiver;

public class PushReceiver extends ParsePushBroadcastReceiver {
	public static final String TAG = "PushReceiver";
    @Override
    public void onPushOpen(Context context, Intent intent) {
        boolean isPluginActive = ParsePushNotificationPlugin.isActive();

		Bundle extras = intent.getExtras();
        if (extras != null)	{
            ParsePushNotificationPlugin.NotificationReceived(extras.getString("com.parse.Data"), false, !isPluginActive);
        }
        Log.v(TAG, "onPushOpen: " + extras.toString());
        Intent i = new Intent(context, getActivity(context, intent));
        i.putExtras(extras);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ParseAnalytics.trackAppOpenedInBackground(i);
        Log.v(TAG, "onPushOpen: " + extras.toString());
        context.startActivity(i);
    }
}