package com.gesturecaller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.gesturecaller.utils.SQLiteHandler;
import com.gesturecaller.utils.Sv;

@SuppressWarnings({"FieldCanBeLocal", "ConstantConditions", "unused"})
public class MyPhoneBroadcastReceiver extends BroadcastReceiver {
    public static final String sSTATE = "state";
    public static final String sINCOMING_NUMBER = "incoming_number";
    private static final String TAG = "PhoneBroadcastReceiver";
    int delay = 100;
    Context c;
    private String outgoing;

    @Override
    public void onReceive(Context context, Intent intent) {
        // this method is invoked on new calls
        this.c = context;
        // check if application is activated before further action
        SQLiteHandler db = new SQLiteHandler(c);
        boolean isActivated = true;
        try {
            isActivated = Boolean.parseBoolean(db.getPref(Sv.ACTIVATE_APP, "true"));
        } catch (Exception ignored) {
            Toast.makeText(c, "Some Error Occurred", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error : " + ignored);
        }

        // if app is activated, start CallerActivity for further actions
        if (isActivated) {
            if (intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL")) {
                // get phone number
                outgoing = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
                int state = TelephonyManager.CALL_STATE_OFFHOOK;
                // create new intent
                final Intent intentPhoneCall = new Intent(c, CallerActivity.class);
                // put mobile number in the intent as extra data
                intentPhoneCall.putExtra(sINCOMING_NUMBER, outgoing);
                // put call state as extra
                intentPhoneCall.putExtra(sSTATE, state);
                intentPhoneCall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Handler callActionHandler = new Handler();
                Runnable runRingingActivity = new Runnable() {
                    @Override
                    public void run() {
                        c.startActivity(intentPhoneCall);
                    }
                };
                callActionHandler.postDelayed(runRingingActivity, delay);
            }

            // initialize call listener on telephone manager
            try {
                TelephonyManager tmgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                MyPhoneStateListener PhoneListener = new MyPhoneStateListener();
                tmgr.listen(PhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
            } catch (Exception e) {
                Log.e("Phone Receive Error", " " + e);
            }
        }
    }

    private class MyPhoneStateListener extends PhoneStateListener {
        // phone state change listener is invoked when call state changes
        @Override
        public void onCallStateChanged(final int state, final String incomingNumber) {
            SQLiteHandler db = new SQLiteHandler(c);
            boolean isActivated = true;
            try {
                isActivated = Boolean.parseBoolean(db.getPref(Sv.ACTIVATE_APP, "true"));
            } catch (Exception ignored) {
                Toast.makeText(c, "Some Error Occurred", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error : " + ignored);
            }
            if (isActivated) {
                Handler callActionHandler = new Handler();
                Runnable runRingingActivity = new Runnable() {
                    @Override
                    public void run() {
                        if (state == 1) {
                            Intent intentPhoneCall = new Intent(c, CallerActivity.class);
                            intentPhoneCall.putExtra(sINCOMING_NUMBER, incomingNumber);
                            intentPhoneCall.putExtra(sSTATE, state);
                            intentPhoneCall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            c.startActivity(intentPhoneCall);
                        }
                    }
                };

                // if call is ringing
                if (state == TelephonyManager.CALL_STATE_RINGING) {
                    callActionHandler.postDelayed(runRingingActivity, delay);
                }

                // if call terminated
                if (state == TelephonyManager.CALL_STATE_IDLE) {
                    callActionHandler.removeCallbacks(runRingingActivity);
                }
            }
        }
    }
}