package com.stratogos.cordova.parsePushNotifications;

import java.util.Set;

import java.util.ArrayList;
import com.parse.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import android.content.pm.ActivityInfo;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.Collection;

public class ParsePushNotificationPlugin extends CordovaPlugin {
    public static final String TAG = "ParsePushNotificationPlugin";
	public static final String STORAGE_KEY = "com.ftwinteractive.smartdirect.storage";


    private static CordovaWebView gWebView;

    private static boolean isInForeground = false;
    private static boolean canDeliverNotifications = false;
    private static ArrayList<String> callbackQueue = new ArrayList<String>();

    /**
     * Gets the application context from cordova's main activity.
     * @return the application context
     */
    private Context getApplicationContext() {
        return this.cordova.getActivity().getApplicationContext();
    }
	
	private Activity getActivity() {
		return this.cordova.getActivity();
	}

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {

        Log.v(TAG, "execute: action=" + action);

        if (action.equalsIgnoreCase("register")){

            JSONObject params = args.optJSONObject(0);

			if(params != null)
			{
				Parse.initialize(getApplicationContext(), params.optString("appId",""), params.optString("clientKey", ""));
				PushService.setDefaultPushCallback(getApplicationContext(), PushHandlerActivity.class);
				ParseInstallation currentInstallation = ParseInstallation.getCurrentInstallation();

				currentInstallation.put("endUserId", getUserId());
				currentInstallation.saveInBackground();
			}
			
            callbackContext.success();

            canDeliverNotifications = true;

            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    flushCallbackQueue();
                }
            });


            return true;
        }
        else if (action.equalsIgnoreCase("unregister")){

            ParseInstallation.getCurrentInstallation().deleteInBackground();

            callbackContext.success();

            return true;
        }
        else if (action.equalsIgnoreCase("getInstallationId")){

            // no installation tokens on android
			String parseInstallId = ParseInstallation.getCurrentInstallation().getInstallationId();
            callbackContext.success(parseInstallId);

            return true;
        }
        else if (action.equalsIgnoreCase("getSubscriptions")){

            Set<String> channels = PushService.getSubscriptions(getApplicationContext());

            JSONArray subscriptions = new JSONArray();

            for(String c:channels){
                subscriptions.put(c);
            }

            callbackContext.success(subscriptions);

            return true;
        }
        else if (action.equalsIgnoreCase("subscribeToChannel")){

            String channel = args.optString(0);

            PushService.subscribe(getApplicationContext(),channel, PushHandlerActivity.class);

            callbackContext.success();

            return true;
        }
        else if (action.equalsIgnoreCase("unsubscribeFromChannel")){

            String channel = args.optString(0);

            PushService.unsubscribe(getApplicationContext(), channel);

            callbackContext.success();

            return true;
        }
        else if (action.equalsIgnoreCase("getEndUserId")){

            // no installation tokens on android
			String endUserId = getUserId();
            callbackContext.success(endUserId);

            return true;
        }

        return false;
    }

    /*
     * Sends a json object to the client as parameter to a method which is defined in gECB.
     */
    public static void NotificationReceived(String json, boolean receivedInForeground) {

        String state = receivedInForeground ? "foreground" : "background";

        Log.v(TAG, "state: " + state + ", json:" + json);


        /*

         THE following is the comment from the iOS version explaining the motivation for copying the 'alert'
         files into data.message in case there is no explicit one set.

         on Android this isn't really needed but we keep it so the behavior is identical on both platforms.

         -------------------

         on iOS we must have the alert field set on the wrapping aps hash. in addition as we have severe
         limitation on the size of the payload we would normally avoid duplicating the notification text
         in both the aps wrapper and the payload object itself.

         in order to keep the interface identical between platforms
         the aps.alert value is required in order for the ios notification center to have something to show
         or else it wouls show the full JSON payload.

         however on the js side we want to access all the properties for this notification inside a single
         object and care not for ios specific implemenataion such as the aps wrapper

         we could just duplicate the text and have it in both *aps.alert* and inside data.message but as the
         payload size limit is only 256 bytes it is better to check if an explicit data.message value exists
         and if not just copy aps.alert into it

         */

        try
        {
            JSONObject wrapper = new JSONObject(json);
            JSONObject data = wrapper.getJSONObject("data");

            if(data != null){
                if(data.has("message") == false){
                    if(wrapper.has("alert")){
                        data.put("message", wrapper.getString("alert"));
                    }
                }
            }

            json = data.toString();

        }catch(JSONException e){}

        String js = "javascript:setTimeout(function(){window.plugin.parse_push.ontrigger('" + state + "',"+ json +")},0)";

        if (canDeliverNotifications) {
            gWebView.sendJavascript(js);
        }else{
            callbackQueue.add(js);
        }

    }


    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        gWebView = webView;
        isInForeground = true;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        gWebView = null;
        isInForeground = false;
    }

    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        isInForeground = false;
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        isInForeground = true;
    }

    private void flushCallbackQueue(){
        for(String js : callbackQueue){
            gWebView.sendJavascript(js);
        }

        callbackQueue.clear();
    }

    public static boolean isActive()
    {
        return gWebView != null;
    }

    public static boolean isInForeground()
    {
        return isInForeground;
    }
	
	
	public boolean hasTelephony(Context mContext)
    {
        TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm == null)
            return false;

        //devices below are phones only
        if (Build.VERSION.SDK_INT < 5)
            return true;

        PackageManager pm = mContext.getPackageManager();

        if (pm == null)
            return false;

        boolean retval = false;
        try
        {
            retval = pm.hasSystemFeature("android.hardware.telephony");
        }
        catch (Exception e)
        {
            retval = false;
        }

        return retval;
    }
	
	public String getUserId(){

        LocalStorage storage = new LocalStorage(this.cordova.getActivity(), ParsePushNotificationPlugin.STORAGE_KEY);
        String androidId = storage.getItem("userId");

        if(androidId != null){
            return androidId;
        }

        androidId = Settings.Secure.getString(getActivity().getContentResolver(),Settings.Secure.ANDROID_ID);
        // Another option is TechoTony's answer here: http://stackoverflow.com/questions/2322234/how-to-find-serial-number-of-android-device#2322494

        Log.d(TAG, "Android id string = " + androidId);
        if(androidId == null || androidId.equals("9774d56d682e549c")){
            /* Many devices (and any emulator) such as the Droid 2 and Galaxy Tab report the same ID, so we need to construct a different one.
                Phones (and possibly tablets with mobile data, though I'm not sure) will report a unique
                id via the telephony provider.  Devices without telephony report a unique serial number.
             */

            Log.d(TAG,"Constructing new id");
			 TelephonyManager telMgr = (TelephonyManager) getActivity().getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);

			 if(telMgr != null && hasTelephony(getActivity())){

                androidId = telMgr.getDeviceId();
            }
            else{

                /*
                Serial Number
                    Since Android 2.3 (“Gingerbread”) this is available via android.os.Build.SERIAL.
                    Devices without telephony are required to report a unique device ID here; some phones may do so also.
                 */
                androidId = android.os.Build.SERIAL;
            }

            // We need to combine the device's id with the user's so that if the device is transferred
            // to a different person we will get a new id.
            /*
            TODO this would be a better way to get the account,
            but I couldn't quickly get google play services working,
            which is required to use GoogleAuthUtil

            Account[] accounts = mAccountManager.getAccountsByType(
                    GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);

             */
			AccountManager mAccountManager = AccountManager.get(getActivity());
            Account[] accounts = mAccountManager.getAccountsByType("com.google");
            Account account = accounts[0];

            try {
                MessageDigest digester = MessageDigest.getInstance("SHA-256");
                digester.update(androidId.getBytes());
                digester.update(account.name.getBytes());
                byte[] hash = digester.digest();
                // This has all kinds of unprintable characters, but it works
                androidId = new String(hash);
                Log.d(TAG,"Generated id " + androidId);
            } catch (NoSuchAlgorithmException e1) {
                // TODO automatically generated catch block
                e1.printStackTrace();
            }
        }

        storage.setItem("userId",androidId);
        return androidId;
    }
}

class LocalStorage {
	
	public static final String FIRST_RUN = "First_Run";
	public static final String IS_DEVICE_REGISTERED = "Is_Device_Registered";
	public static final String LOCATIONS_REVISION = "Locations_Revision";
	public static final String IGNORED_LOCATIONS = "Ignored_Locations";
	public static final String FAV_LOCATIONS = "Fav_Locations";
	
	public String prefName;
	public SharedPreferences prefs;

	public LocalStorage(Activity app, String prefName) {
		this.prefName = prefName;
		prefs = app.getSharedPreferences(prefName, 0);
	}

	public void setItem(String name, String value) {
		SharedPreferences.Editor editor = prefs.edit();
		if (value == null || value.length() == 0) {
			editor.remove(name);
		}
		editor.putString(name, value);
		editor.commit();
	}

	public String getItem(String name) {
		return prefs.getString(name, null);
	}

	public void removeItem(String name) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.remove(name);
		editor.commit();
	}
}
