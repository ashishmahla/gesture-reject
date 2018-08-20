package com.gesturecaller.utils;

import android.content.Context;
import android.telephony.SmsManager;
import android.widget.Toast;

@SuppressWarnings("unused")
public class Statics {

    public static void sendSMS(Context context, String phoneNo, String msg) {
        try {
            // get sms manager
            SmsManager smsManager = SmsManager.getDefault();
            // send text
            smsManager.sendTextMessage(phoneNo, null, msg, null, null);
            // notify user
            Toast.makeText(context, "Messaged : " + msg, Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            Toast.makeText(context, ex.getMessage(), Toast.LENGTH_LONG).show();
            ex.printStackTrace();
        }
    }
}
