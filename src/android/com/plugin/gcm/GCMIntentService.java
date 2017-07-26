package com.plugin.gcm;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;
import com.inet.evernet.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Random;

@SuppressLint("NewApi")
public class GCMIntentService extends GCMBaseIntentService {

	private static final String TAG = "GCMIntentService";
	
	public GCMIntentService() {
		super("GCMIntentService");
	}

	@Override
	public void onRegistered(Context context, String regId) {

		Log.v(TAG, "onRegistered: "+ regId);

		JSONObject json;

		try
		{
			json = new JSONObject().put("event", "registered");
			json.put("regid", regId);

			Log.v(TAG, "onRegistered: " + json.toString());

			// Send this JSON data to the JavaScript application above EVENT should be set to the msg type
			// In this case this is the registration ID
			PushPlugin.sendJavascript( json );

		}
		catch( JSONException e)
		{
			// No message to the user is sent, JSON failed
			Log.e(TAG, "onRegistered: JSON exception");
		}
	}

	@Override
	public void onUnregistered(Context context, String regId) {
		Log.d(TAG, "onUnregistered - regId: " + regId);
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
		Log.d(TAG, "onMessage - context: " + context);

		// Extract the payload from the message
		Bundle extras = intent.getExtras();
		if (extras != null)
		{
			/*
			 * Structure payload received:
			 * {
			 *		alert: "Message here",
			 *		sender: "sender" 		
			 * }
			 * Content was encode by url encode
			 * So that need decode to make sure message is correct
			 */
			String message = "";
			if (extras.getString("alert") != null && extras.getString("alert").length() != 0) {
				try {
					message = java.net.URLDecoder.decode(extras.getString("alert"), "UTF-8");
					extras.putString("alert", message);	
					extras.putString("sender", java.net.URLDecoder.decode(extras.getString("sender"), "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
            }
			
			// if we are in the foreground, just surface the payload, else post it to the statusbar
            if (PushPlugin.isInForeground()) {
				extras.putBoolean("foreground", true);
                PushPlugin.sendExtras(extras);
			}
			else {
				extras.putBoolean("foreground", false);
				if(message.length() != 0)
					extras.putString("message", message);
                // Send a notification if there is a message
                if (extras.getString("message") != null && extras.getString("message").length() != 0) {
    				extras.putString("title", "EverNet");
                    createNotification(context, extras);
                }
            }
        }
	}

	public void createNotification(Context context, Bundle extras)
	{
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		Intent notificationIntent = new Intent(this, PushHandlerActivity.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notificationIntent.putExtra("pushBundle", extras);

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		int defaults = Notification.DEFAULT_ALL;

		if (extras.getString("defaults") != null) {
			try {
				defaults = Integer.parseInt(extras.getString("defaults"));
			} catch (NumberFormatException e) {}
		}
		
		NotificationCompat.Builder mBuilder =
			new NotificationCompat.Builder(context)
				.setDefaults(defaults)
				.setSmallIcon(context.getApplicationInfo().icon)
				.setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
						R.mipmap.ic_launcher))
				.setWhen(System.currentTimeMillis())
				.setContentTitle(extras.getString("title"))
				.setTicker(extras.getString("title"))
				.setContentIntent(contentIntent)
				.setAutoCancel(true);

		String message = extras.getString("message");
		if (message != null) {
			mBuilder.setContentText(message);
		} else {
			mBuilder.setContentText("<missing message content>");
		}

		String msgcnt = extras.getString("msgcnt");
		if (msgcnt != null) {
			mBuilder.setNumber(Integer.parseInt(msgcnt));
		}
		
//		int notId = 0;
//		
//		try {
//			notId = Integer.parseInt(extras.getString("notId"));
//		}
//		catch(NumberFormatException e) {
//			Log.e(TAG, "Number format exception - Error parsing Notification ID: " + e.getMessage());
//		}
//		catch(Exception e) {
//			Log.e(TAG, "Number format exception - Error parsing Notification ID" + e.getMessage());
//		}
		mNotificationManager.notify(new Random().nextInt(101), mBuilder.build());
	}
	
//	private static String getAppName(Context context)
//	{
//		CharSequence appName = 
//				context
//					.getPackageManager()
//					.getApplicationLabel(context.getApplicationInfo());
//		
//		return (String)appName;
//	}
	
	@Override
	public void onError(Context context, String errorId) {
		Log.e(TAG, "onError - errorId: " + errorId);
	}
}
