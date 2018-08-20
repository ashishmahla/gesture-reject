package com.gesturecaller;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.gesture.GestureOverlayView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.gesturecaller.models.MyLocation;
import com.gesturecaller.utils.PermissionUtils;
import com.gesturecaller.utils.Statics;
import com.gesturecaller.utils.Sv;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.gesturecaller.utils.Sv.NONE;

public class CallerActivity extends AppCompatActivity implements View.OnClickListener
        , SensorEventListener {
    private static final String TAG = "CallerActivity";
    private static final int SENSOR_SENSITIVITY = 4;
    public final CallerActivity context = CallerActivity.this;
    FloatingActionButton answerButton;
    FloatingActionButton rejectButton;
    LinearLayout timerLayout;
    TextView contactName;
    TextView contactNumber;
    ImageView profile;
    String name = "Unknown Number";
    String contactId = null;
    InputStream photoStream;
    TextView callType;
    TextView timerValue;
    int tick = 20;
    int delay = 0;
    int maxDelay = 2000;
    Button cancel;
    TextView status;
    ProgressBar progress;
    boolean clicked = false;
    vector p1 = new vector();
    int maxTouchCount = 0;
    String incomingNumber = "";
    long gestureStartTimeInMilli = 0;
    boolean proximityAlert = true;
    int timesClicked = 0;
    int ms = 0;
    boolean threadRunning = false;
    private SensorManager mSensorManager;
    private Sensor mProximity;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // get extra data put in the intent
        Bundle bundle = getIntent().getExtras();
        // check that the data exists
        if (bundle != null) {
            // check if we got all the required permissions
            if (PermissionUtils.hasAllPermissions(context)) {
                // get incoming number form the bundle put there by the broadcast receiver
                incomingNumber = bundle.getString(MyPhoneBroadcastReceiver.sINCOMING_NUMBER);
            } else {
                // if permissions are missing, exit
                Log.e("CallerActivity", "Missing required permissions.");
                finish();
            }
        } else {
            // if bundle is missing, terminate
            Log.e("CallerActivity", "Bundle is empty.");
            finish();
        }

        // set the layout of the activity
        setContentView(R.layout.activity_caller);
        // get sensor manager from system ( for proximity sensor )
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        assert mSensorManager != null;

        // get proximity sensor form the sensor manager to use it
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        // set up the activity to be shown on lockscreen, turn screen on and dismiss the keyguard
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        // initialize the views in the activity
        answerButton = findViewById(R.id.callReceive);
        answerButton.setOnClickListener(this);
        rejectButton = findViewById(R.id.callReject);
        rejectButton.setOnClickListener(this);
        timerLayout = findViewById(R.id.timerLayout);
        contactName = findViewById(R.id.contactName);
        contactNumber = findViewById(R.id.contactNumber);
        callType = findViewById(R.id.callType);
        timerValue = findViewById(R.id.timerValue);
        profile = findViewById(R.id.contactPhoto);

        cancel = findViewById(R.id.btn_cancel);
        status = findViewById(R.id.tv_status);
        progress = findViewById(R.id.pb_progress);

        // look up contact details for incoming number
        contactsLookUp(incomingNumber);
        // setup name and number to show in activity
        contactName.setText(name);
        contactNumber.setText(incomingNumber);

        // initialize a phone state listener to synchronize activity life cycle with the call cycle
        PhoneStateListener phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                // action for when phone is in ringing state
                if (state == TelephonyManager.CALL_STATE_RINGING) {
                    String placeholder = "Incoming Call";
                    callType.setText(placeholder);
                    Log.e("CALL_STATE_RINGING", "CALL_STATE_RINGING");
                }

                // actions after call cut (ended)
                else if (state == TelephonyManager.CALL_STATE_IDLE) {
                    String placeholder = "Disconnected";
                    callType.setText(placeholder);
                    finish();
                }

                // when speaking / outgoing call
                else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                    String placeholder = "";
                    callType.setText(placeholder);
                    Log.e("CALL_STATE_OFFHOOK", "CALL_STATE_OFFHOOK");
                    finish();
                }
                super.onCallStateChanged(state, incomingNumber);
            }
        };

        // get up telephony manager
        TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (mgr != null) {
            // if manager is not null, register the listener with phone manager
            mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }

        // if bike mode is active and action is taken, terminate activity
        if (Sv.getBooleanSetting(Sv.BIKE_MODE, false) && bikeModeAction()) {
            finish();
        }

        // check for location mode
        if (Sv.getBooleanSetting(Sv.LOCATION_MODE, false)) {
            // check if within any location bounds
            MyLocation currActiveLocation = Sv.getCurrActiveLocation(context);
            if (currActiveLocation != null) {
                // if some location from active location list is active, take corresponding action
                if (currActiveLocation.getMessage() != null && !currActiveLocation.getMessage().isEmpty() && incomingNumber != null) {
                    // send message if any
                    Statics.sendSMS(context, incomingNumber, currActiveLocation.getMessage());
                }
                // reject call
                rejectCall();
                finish();
            }
        }

        // find gesture overlay view and set on touch listener on it to track touch events
        // any view can be used but gesture overlay shows pointer track
        final GestureOverlayView gestures = findViewById(R.id.gestures);
        gestures.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                // handle touch if proximity alert is not alerting
                if (!proximityAlert) {
                    try {
                        handleTouch(motionEvent);
                    } catch (Exception ignored) {
                        Log.e(TAG, "Exception");
                    }
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

    private void contactsLookUp(String number) {
        // look up the number and get corresponding details from user's contacts
        Log.v("CallerActivity", "Fetching contacts information and pic...");

        // define the columns I want the query to return
        String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup._ID};

        // encode the phone number and build the filter URI
        Uri contactUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));

        // query time
        Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null);
        assert cursor != null;
        if (cursor.moveToFirst()) {
            // Get values from contacts database:
            contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup._ID));
            name = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
        } else {
            return; // contact not found
        }

        // when api is > 14 (ours is always > 19)
        Uri my_contact_Uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.parseLong(contactId));
        // get contact photo
        photoStream = ContactsContract.Contacts.openContactPhotoInputStream(getContentResolver(), my_contact_Uri, true);

        // if photo input stream is not null
        if (photoStream != null) {
            BufferedInputStream buffIS = new BufferedInputStream(photoStream);
            // create a bitmap from input stream to use in profile image view
            Bitmap mBitmap = BitmapFactory.decodeStream(buffIS);
            profile.setImageBitmap(mBitmap);
        } else {
            // if no contact image, set default image
            profile.setImageResource(R.drawable.ic_person);
        }
        // close cursor resource
        cursor.close();
    }

    @Override
    public void onClick(View v) {
        // method is called with view's id thats been clicked
        if (v.getId() == answerButton.getId()) {
            // if answer button clicked, pickCall
            pickUpCall();
        }

        if (v.getId() == rejectButton.getId()) {
            // if reject button clicked, reject call
            rejectCall();
        }
    }

    private void pickUpCall() {
        // call picking not allowed by system security,
        // close this activity and show default dialer instead
        finish();
    }

    @SuppressLint("PrivateApi")
    private void rejectCall() {
        // call management not allowed by android due to security reasons
        // this is the method reflection, a workaround.

        // get telephony manager to manage call
        TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        Method m1;
        try {
            assert tm != null;
            // get method getITelephony form telephony manager
            m1 = tm.getClass().getDeclaredMethod("getITelephony");
            // set accessibility to true to access method
            m1.setAccessible(true);
            // try to invoke method and get returned object
            Object iTelephony = m1.invoke(tm);

            // get endCall method from object
            Method m3 = iTelephony.getClass().getDeclaredMethod("endCall");

            // invoke endCall method, (method reflection not actual execution)
            m3.invoke(iTelephony);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }

        /* this is real code but it only works in system apps due to android system security. */

        /*TelephonyManager telephony = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            // Java reflection to gain access to TelephonyManager's
            // ITelephony getter
            Class c = Class.forName(telephony.getClass().getName());
            Method m = c.getDeclaredMethod("getITelephony");
            m.setAccessible(true);
            ITelephony telephonyService = (ITelephony) m.invoke(telephony);
            telephonyService.endCall();
            finish();

            timeSwapBuff += timeInMilliseconds;
            customHandler.removeCallbacks(updateTimerThread);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("Error", "FATAL ERROR: could not connect to telephony subsystem");
            Log.e("Error", "Exception object: " + e);
        }*/
    }

    private boolean bikeModeAction() {
        // check if contact is exceptional from database
        if (Sv.isContactExceptional(context, incomingNumber)) {
            // if contact is exceptional, do nothing, warn about exceptional contact
            Toast.makeText(context, "Exceptional contact", Toast.LENGTH_SHORT).show();
            return false;
        }

        // contact is not exceptional, continue

        // reload all settings from database
        Sv.refreshSettings(context);
        // get message to send for bike mode
        String bikeModeMsg = Sv.getSetting(Sv.BIKE_MODE_MSG, null);
        if (bikeModeMsg != null && !bikeModeMsg.isEmpty()) {
            // if message is not empty, send message to incoming number
            if (incomingNumber != null && !incomingNumber.isEmpty() && Sv.refreshSettings(context)) {
                Statics.sendSMS(context, incomingNumber, bikeModeMsg);
            }
        }

        // create notification to notify user that a call was rejected by bike mode for later
        createNotification("Bike mode rejected a call from " + incomingNumber);
        // reject incoming call
        rejectCall();
        // return that action was taken
        return true;
    }

    private void createNotification(String msg) {
        // create notification stating that a call was rejected.

        // get notification manager from system
        final NotificationManager mgr =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        // build a notification
        Notification.Builder builder = new Notification.Builder(context);
        // set title and other details of notification
        builder.setContentTitle("Call Rejected").setSmallIcon(R.drawable.ic_call)
                .setContentText(msg);

        // create a notification from notification builder
        Notification notification = builder.build();

        assert mgr != null;
        // tell notification manager to show the notification
        mgr.notify((int) System.currentTimeMillis(), notification);
    }

    private void performAction(final String ACTION) {
        // perform some action
        String actionWarning;
        // refresh settings
        if (Sv.refreshSettings(context)) {
            // get action type
            final String action = Sv.getSetting(ACTION, Sv.items[NONE]);
            // get action code from settings database
            int actionCode = Sv.getItemCode(action);
            // switch action code for different actions
            switch (actionCode) {
                case NONE: // when no action is set for gesture
                    Log.e(TAG, "No action defined for gesture.");
                    break;

                case Sv.ACCEPT_CALL: // when accept call action is set to gesture
                    finish();
                    break;

                case Sv.REJECT_CALL: // when call rejection action is set to gesture
                    // warn user about call rejection action before performing it
                    // make colored text by using html tags
                    actionWarning = "<font color = #d50000> Reject call";
                    // get html formatted text and show it as status
                    status.setText(Html.fromHtml(actionWarning));
                    // create action handler to perform action after a little delay
                    new ActionHandler() {
                        @Override
                        public void onFinish() {
                            rejectCall();
                        }
                    }.runHandler();
                    break;

                case Sv.REJECT_CALL_WITH_SMS: // when reject call action is setup along with reply message
                    // get message for this action
                    String msg = Sv.getSetting(ACTION + "sms", "");
                    // warn user for the action and give time to revert it
                    actionWarning = "<font color = #d50000> Reject call with sms </font>" +
                            "<font color = #444444>" + msg + "</font>";
                    status.setText(Html.fromHtml(actionWarning));
                    // take action after a little delay
                    new ActionHandler() {
                        @Override
                        public void onFinish() {
                            rejectCallWithSMS(ACTION);
                        }
                    }.runHandler();
                    break;

                case Sv.SILENCE_CALL: // when silent call action is setup
                    AudioManager am;
                    // get audio manager service from system
                    am = (AudioManager) getBaseContext().getSystemService(Context.AUDIO_SERVICE);

                    if (am != null) {
                        //For Normal mode
                        //am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);

                        //For Silent mode
                        am.setRingerMode(AudioManager.RINGER_MODE_SILENT);

                        //For Vibrate mode
                        //am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                    }
                    break;

                default:
                    Log.e(TAG, "No such action : " + action);
            }
        } else {
            Log.e(TAG, "Unable to refresh settings.");
        }
    }

    private void rejectCallWithSMS(final String ACTION) {
        // this method rejects the call with a message to the caller

        // get message set for the action
        String msg = Sv.getSetting(ACTION + "sms", "");
        // if incoming number is not empty, send message to the number
        if (incomingNumber != null && !incomingNumber.isEmpty() && Sv.refreshSettings(context)) {
            Statics.sendSMS(context, incomingNumber, msg);
        }
        // reject the call
        rejectCall();
    }

    void handleTouch(MotionEvent m) throws Exception {
        // this method handles the touches and tracks the pointer, taking relevant actions

        // get the number of touches on the screen from motion even
        int pointerCount = m.getPointerCount();
        // get type of action being performed like touching the screen
        int action = m.getActionMasked();

        // switch number of touches on the screen
        switch (pointerCount) {
            case 1: // when single point of touch, single finger gestures
                switch (action) {
                    case MotionEvent.ACTION_DOWN: // action down means when finger starts touching(finger down)
                        // set that click has started
                        clicked = true;
                        // set that touch count is 1
                        maxTouchCount = 1;
                        // reset the pointer for first finger values
                        p1.reset();
                        // set the pointer values to touch coordinates x and y
                        p1.init(m.getX(), m.getY());
                        // set the time when gesture was started, to track the time of gesture to track long press
                        gestureStartTimeInMilli = System.currentTimeMillis();
                        break;

                    case MotionEvent.ACTION_UP: // action up means finger is removed from screen on gesture complete
                        // set that click has ended
                        clicked = false;
                        // invoke single finger gesture method
                        if (maxTouchCount == 1) {
                            singleTouchActions();
                        }
                        break;

                    case MotionEvent.ACTION_MOVE: // action move means finger is moved, track finger position
                        // set new coordinates to pointer
                        p1.mov(m.getX(), m.getY());
                        break;

                    default:
                        break;
                }
                break;
        }
    }

    private void singleTouchActions() {
        // single finger pointer values recorded previously are used to detect gesture type
        // check if pointer was a tap on screen or was pointer moving
        if (!p1.isTap()) {
            // if pointer was not a tap, stop counting number of taps
            timesClicked = 0;
            // set counter thread as stopped
            threadRunning = false;

            // if pointer moved more in x direction
            if (p1.isXMov()) {
                // check if the pointer moved in positive x or negative x direction to detect left or right
                if (p1.xMov > 0) {
                    // x moved in positive direction, hence left swipe
                    performAction(Sv.sSWIPE_RIGHT);
                } else {
                    // pointer moved in -x direction, left swipe
                    performAction(Sv.sSWIPE_LEFT);
                }
            } else {
                // pointer moved more in y direction, check +y or -y for down or top actions
                if (p1.yMov > 0) {
                    // +y direction, swipe down detected
                    performAction(Sv.sSWIPE_DOWN);
                } else {
                    // -y direction, swipe up detected
                    performAction(Sv.sSWIPE_UP);
                }
            }

        } else { // action was a tap
            // check pointer start and end time to check if long press or tap
            if (System.currentTimeMillis() - gestureStartTimeInMilli < 500) {
                // down time less than 500 milli seconds, its a tap
                // increase the count of taps
                timesClicked++;

                if (timesClicked == 3) { // if number of taps gets to 3, perform triple tap action
                    performAction(Sv.sTRIPLE_TAP);
                    // reinitialized number of taps on screen
                    timesClicked = 0;
                    // set counter thread as not running
                    threadRunning = false;

                } else {
                    // reset the next tap waiting time
                    ms = 1000;
                    // if counter thread is not already running, run it
                    if (!threadRunning) {
                        try {
                            // provide thread specification and its action s
                            Thread th = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    // when tap waiting has not timed out, wait for it
                                    while (ms > 0) {
                                        try {
                                            Thread.sleep(50);
                                            ms -= 50;
                                        } catch (InterruptedException ignored) {
                                        }
                                    }
                                    // on tap waiting timeout, get the times user clicked
                                    int taps = timesClicked;
                                    // reset times clicked count and set thread as not running
                                    timesClicked = 0;
                                    threadRunning = false;

                                    // take action for different count of taps
                                    String action = null;
                                    switch (taps) {
                                        case 1: // single tap
                                            action = Sv.sSINGLE_TAP;
                                            break;
                                        case 2: // double tap
                                            action = Sv.sDOUBLE_TAP;
                                            break;
                                        case 3: // triple tap
                                            action = Sv.sTRIPLE_TAP;
                                            break;
                                    }
                                    final String ACTION = action;
                                    // run the action on main UI thread
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            performAction(ACTION);
                                        }
                                    });
                                }
                            });

                            // all thread specification are finalized, now run it and set it as running
                            threadRunning = true;
                            th.start();
                        } catch (IllegalThreadStateException ignored) {
                            // if some error encountered, reset settings
                            ms = 0;
                            timesClicked = 0;
                            threadRunning = false;
                        }
                    }
                }
            } else {
                // if it was not a tap, perform long press action
                performAction(Sv.sSINGLE_TAP_LONG_PRESS);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // override pressing home or back button to not allow user to go back from this screen
        return !(KeyEvent.KEYCODE_BACK == event.getKeyCode()
                || KeyEvent.KEYCODE_HOME == event.getKeyCode()) && super.onKeyDown(keyCode, event);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // this method listens for any changes in proximity
        // check only proximity sensor
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            // check if the user is too close to mobile device to throw proximity alert
            if (event.values[0] >= -SENSOR_SENSITIVITY && event.values[0] <= SENSOR_SENSITIVITY) {
                //near
                findViewById(R.id.rl_proximity_alert).setVisibility(View.VISIBLE);
                proximityAlert = true;
            } else {
                //far
                findViewById(R.id.rl_proximity_alert).setVisibility(View.GONE);
                proximityAlert = false;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        // register sensor utilization when activity starts into action
        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // release sensor when not in use
        mSensorManager.unregisterListener(this);
    }

    private abstract class ActionHandler {
        // create a class to handle actions, show warnings and timer
        final Handler handler = new Handler();
        Runnable actionCountDown = null;

        void runHandler() {
            // setup cancel on click listener to perform tasks when user cancels the action
            // show that the user has cancelled the action
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    handler.removeCallbacks(actionCountDown);
                    delay = 0;
                    String placeholder = "Action Cancelled.";
                    status.setText(placeholder);
                    progress.setVisibility(View.GONE);
                    progress.setProgress(0);
                    cancel.setVisibility(View.GONE);
                    cancel.setOnClickListener(null);
                }
            });
            progress.setVisibility(View.VISIBLE);
            progress.setProgress(0);
            cancel.setVisibility(View.VISIBLE);

            // show timeout to user after which action will be performed
            actionCountDown = new Runnable() {
                public void run() {
                    progress.setMax(maxDelay);
                    if ((delay += tick) < maxDelay) {
                        progress.setProgress(delay);
                        handler.postDelayed(this, tick);
                    } else {
                        delay = 0;
                        progress.setVisibility(View.GONE);
                        progress.setProgress(0);
                        cancel.setVisibility(View.GONE);
                        onFinish();
                    }
                }
            };
            handler.post(actionCountDown);
        }

        // onFinish is abstract to allow us to define action later on in program by overriding it
        public abstract void onFinish();
    }

    // custom vector class to manage pointers
    class vector {
        float x; // pointer start x coord
        float y; // pointer start y coord
        float xMov; // pointer moved to x pos
        float yMov; // pointer moved to y pos

        // on reset, sets all values to zero
        void reset() {
            x = 0;
            y = 0;
            xMov = 0;
            yMov = 0;
        }

        // set pointer start coords
        void init(float x, float y) {
            this.x = x;
            this.y = y;
        }

        // set the distance pointer has moved (displacement)
        void mov(float xm, float ym) {
            xMov = xm - x;
            yMov = ym - y;
        }

        // tap is recorded when pointer moves more than 60 points on screen
        boolean isTap() {
            return !(Math.abs(xMov) > 60 || Math.abs(yMov) > 60);
        }

        // if total x movement is more than total y movement, it is called x movement
        boolean isXMov() {
            return Math.abs(xMov) > Math.abs(yMov);
        }

        /*boolean isYMov() {
            return Math.abs(xMov) > Math.abs(yMov);
        }
        */
    }
}