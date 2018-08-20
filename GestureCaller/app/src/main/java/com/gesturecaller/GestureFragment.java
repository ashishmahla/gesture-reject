package com.gesturecaller;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gesturecaller.utils.PermissionUtils;
import com.gesturecaller.utils.SQLiteHandler;
import com.gesturecaller.utils.Sv;

import java.util.ArrayList;

import static com.gesturecaller.utils.PermissionUtils.permissionsList;
import static com.gesturecaller.utils.Sv.ACCEPT_CALL;
import static com.gesturecaller.utils.Sv.NONE;
import static com.gesturecaller.utils.Sv.REJECT_CALL;
import static com.gesturecaller.utils.Sv.REJECT_CALL_WITH_SMS;
import static com.gesturecaller.utils.Sv.SILENCE_CALL;

public class GestureFragment extends Fragment {

    // this is the gesture fragment shown in one of the tabs
    private static final int REQUEST_PERMISSION_CODE = 17;
    private static final String TAG = "GestureFragment";
    Context context;
    ArrayList<String> unAuthPermissionsList = new ArrayList<>();

    private static String getTextForAction(String ACTION) {
        String text = Sv.getSetting(ACTION, Sv.items[NONE]);
        if (text.equals(Sv.items[Sv.REJECT_CALL_WITH_SMS])) {
            text += Sv.getSetting(ACTION + "sms", "");
        }
        return text;
    }

    private static String getTextForAction(String ACTION, String normalColor, String specialColor) {
        String text = "<font color = " + normalColor + ">";
        text += Sv.getSetting(ACTION, Sv.items[NONE]);
        text += "</font>";
        if (Sv.getSetting(ACTION, Sv.items[NONE]).equals(Sv.items[Sv.REJECT_CALL_WITH_SMS])) {
            text += " <font color = " + specialColor + "> ";
            text += Sv.getSetting(ACTION + "sms", "(No SMS)");
            text += "</font>";
        }
        return text;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // this method is called when this view is created to be shown

        // we are inflating the layout on the base activity
        View parent = inflater.inflate(R.layout.frag_gesture, container, false);
        this.context = getActivity();

        // getting all settings from database
        SQLiteHandler db = new SQLiteHandler(context);
        try {
            Sv.settings = db.getAllSettings();
        } catch (Exception ignored) {
        } finally {
            db.close();
        }

        // checking if this is the first run or not
        // set first run to false, show introduction and reset settings if this is first run
        String launch = Sv.getSetting(context, Sv.FIRST_RUN);
        if (launch == null) {
            Sv.reset(context);
            Sv.setBooleanSetting(context, Sv.FIRST_RUN, false);
            startActivity(new Intent(context, Intro.class));
        } else {
            Sv.setBooleanSetting(context, Sv.FIRST_RUN, false);
        }

        // check if all permissions are provided in this method
        getPermissions();

        // initialise basic views and layout
        initBasics(parent);

        // initialize single gesture setting views shown in gestures tab
        initSingle(parent);

        return parent;
    }

    private void getPermissions() {
        // check if permissions are required and to be asked
        if (PermissionUtils.shouldAskPermission()) {
            // load required permissions list
            unAuthPermissionsList = PermissionUtils.getReqPermissionsList(context);

            // if permissions list is not empty, ask permissions
            if (!unAuthPermissionsList.isEmpty()) {
                // convert permissions list to string array
                final String[] permissions = new String[unAuthPermissionsList.size()];
                for (int i = 0; i < unAuthPermissionsList.size(); i++) {
                    permissions[i] = unAuthPermissionsList.get(i);
                }

                // show an alert dialog saying that permissions are required.
                AlertDialog.Builder adb = new AlertDialog.Builder(context);
                adb.setTitle("Permissions Required.");
                adb.setMessage("App cannot work without required permissions. " +
                        "We never ask for unwanted permissions." +
                        "\n\nPlease provide all permissions for the working of this app.");
                adb.setPositiveButton("Sure", new DialogInterface.OnClickListener() {
                    @TargetApi(Build.VERSION_CODES.M)
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        requestPermissions(permissions, REQUEST_PERMISSION_CODE);
                    }
                });
                adb.show();
            }
        }

        // ask for do not disturb permissions
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !notificationManager.isNotificationPolicyAccessGranted()) {

            Intent intent = new Intent(
                    android.provider.Settings
                            .ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);

            startActivity(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // this method is invoked when the permissions request returns its result
        switch (requestCode) {
            case REQUEST_PERMISSION_CODE:
                int counter = 0;
                // check which permissions are granted and which are not
                for (String permission : permissionsList) {
                    if (!PermissionUtils.hasPermission(context, permission)) {
                        Log.e(TAG, "Permissions rejected : " + permission);
                        counter++;
                    }
                }

                // if some permissions are not granted, show a toast as a warning
                if (counter > 0) {
                    Toast.makeText(context,
                            "All permissions are required for this app to work. (Unauthorized : " + counter + ")",
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Permissions rejected : " + counter);
                }
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void initBasics(View parent) {
        // finding "Activate All" checkbox and setting it up for on click functionality
        CheckBox activate_all = parent.findViewById(R.id.set_activate_all);
        activate_all.setChecked(Sv.getBooleanSetting(Sv.ACTIVATE_APP, true));
        activate_all.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                // when checked is changed, reflect settings to database
                Sv.setBooleanSetting(context, Sv.ACTIVATE_APP, b);
            }
        });

        // setup reset app button
        Button resetApp = parent.findViewById(R.id.reset_app);
        resetApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // on button click show a warning to make user certain of risks
                AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
                adb.setTitle("Really reset?");
                adb.setMessage("Are you sure you want to reset app settings. This will restore app back to default settings.");
                adb.setPositiveButton("Reset", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // if user is sure, reset app and restart
                        Sv.reset(context);
                        startActivity(new Intent(context, MainActivity.class));
                        getActivity().finish();
                    }
                });

                adb.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                });

                adb.show();
            }
        });

        // setup intro button click action
        Button showIntro = parent.findViewById(R.id.show_intro);
        showIntro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(context, Intro.class));
                /*final Intent intentPhoneCall = new Intent(getActivity(), CallerActivity.class);
                intentPhoneCall.putExtra(sINCOMING_NUMBER, "9414787140");
                intentPhoneCall.putExtra(sSTATE, TelephonyManager.CALL_STATE_OFFHOOK);
                intentPhoneCall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intentPhoneCall);*/
            }
        });
    }

    private void initSingle(View parent) {
        // initialize various checkboxes and on click listeners
        LinearLayout singleTap = parent.findViewById(R.id.set_single_tap);
        ((TextView) singleTap.getChildAt(1)).setText(
                Html.fromHtml(getTextForAction(Sv.sSINGLE_TAP, "#888888", "#52a317")));
        singleTap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setAction(view, Sv.sSINGLE_TAP, "Single Tap Action");
            }
        });

        LinearLayout doubleTap = parent.findViewById(R.id.set_double_tap);
        ((TextView) doubleTap.getChildAt(1)).setText(
                Html.fromHtml(getTextForAction(Sv.sDOUBLE_TAP, "#888888", "#52a317")));
        doubleTap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setAction(view, Sv.sDOUBLE_TAP, "Double Tap Action");
            }
        });

        LinearLayout tripleTap = parent.findViewById(R.id.set_triple_tap);
        ((TextView) tripleTap.getChildAt(1)).setText(
                Html.fromHtml(getTextForAction(Sv.sTRIPLE_TAP, "#888888", "#52a317")));
        tripleTap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setAction(view, Sv.sTRIPLE_TAP, "Triple Tap Action");
            }
        });

        LinearLayout single_long_press = parent.findViewById(R.id.set_single_long_press);
        ((TextView) single_long_press.getChildAt(1)).setText(
                Html.fromHtml(getTextForAction(Sv.sSINGLE_TAP_LONG_PRESS, "#888888", "#52a317")));
        single_long_press.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setAction(view, Sv.sSINGLE_TAP_LONG_PRESS, "Long Press Action");
            }
        });

        LinearLayout swipeUp = parent.findViewById(R.id.set_swipe_up);
        ((TextView) swipeUp.getChildAt(1)).setText(
                Html.fromHtml(getTextForAction(Sv.sSWIPE_UP, "#888888", "#52a317")));
        swipeUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setAction(view, Sv.sSWIPE_UP, "Swipe Up Action");
            }
        });

        LinearLayout swipeDown = parent.findViewById(R.id.set_swipe_down);
        ((TextView) swipeDown.getChildAt(1)).setText(
                Html.fromHtml(getTextForAction(Sv.sSWIPE_DOWN, "#888888", "#52a317")));
        swipeDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setAction(view, Sv.sSWIPE_DOWN, "Swipe Down Action");
            }
        });

        LinearLayout swipeLeft = parent.findViewById(R.id.set_swipe_left);
        ((TextView) swipeLeft.getChildAt(1)).setText(
                Html.fromHtml(getTextForAction(Sv.sSWIPE_LEFT, "#888888", "#52a317")));
        swipeLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setAction(view, Sv.sSWIPE_LEFT, "Swipe Left Action");
            }
        });

        LinearLayout swipeRight = parent.findViewById(R.id.set_swipe_right);
        ((TextView) swipeRight.getChildAt(1)).setText(
                Html.fromHtml(getTextForAction(Sv.sSWIPE_RIGHT, "#888888", "#52a317")));
        swipeRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setAction(view, Sv.sSWIPE_RIGHT, "Swipe Right Action");
            }
        });
    }

    private int getIndex(String string) {
        if (string.matches(Sv.items[NONE])) {
            return NONE;
        } else if (string.matches(Sv.items[ACCEPT_CALL])) {
            return ACCEPT_CALL;
        } else if (string.matches(Sv.items[REJECT_CALL])) {
            return REJECT_CALL;
        } else if (string.matches(Sv.items[REJECT_CALL_WITH_SMS])) {
            return REJECT_CALL_WITH_SMS;
        } else if (string.matches(Sv.items[SILENCE_CALL])) {
            return SILENCE_CALL;
        } else {
            return -1;
        }
    }

    private void setAction(final View view, final String prefKey, String title) {
        // setup action to the preferences like swipe up ( opens setup dialog )
        AlertDialog.Builder adb = new AlertDialog.Builder(context);
        adb.setTitle(title);
        adb.setSingleChoiceItems(Sv.items, getIndex(Sv.getSetting(prefKey, Sv.items[NONE])), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch (i) {
                    case NONE:
                        // set the action as NONE
                        Sv.setSetting(context, prefKey, Sv.items[i]);
                        break;
                    case Sv.ACCEPT_CALL:
                        Sv.setSetting(context, prefKey, Sv.items[i]);
                        break;

                    case Sv.REJECT_CALL:
                        Sv.setSetting(context, prefKey, Sv.items[i]);
                        break;

                    case Sv.REJECT_CALL_WITH_SMS:
                        // make another dialog box to take sms as input
                        AlertDialog.Builder adb = new AlertDialog.Builder(context);
                        adb.setTitle("Set SMS Text");
                        // add edit text to take input
                        final EditText et = new EditText(context);
                        adb.setView(et);
                        adb.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (!et.getText().toString().isEmpty()) {
                                    // if input text is not empty, setup action as REJECT CALL WITH SMS
                                    Sv.setSetting(context, prefKey, Sv.items[Sv.REJECT_CALL_WITH_SMS]);
                                    Sv.setSetting(context, prefKey + "sms", et.getText().toString());
                                } else {
                                    // if empty, setup action as reject call only
                                    Toast.makeText(context, "Empty SMS field. Setting as reject call only."
                                            , Toast.LENGTH_LONG).show();
                                    Sv.setSetting(context, prefKey, Sv.items[Sv.REJECT_CALL]);
                                }

                                // change the action shown in main layout
                                ((TextView) ((LinearLayout) view).getChildAt(1)).setText(
                                        Html.fromHtml(getTextForAction(prefKey, "#888888", "#52a317")));
                                dialogInterface.dismiss();
                            }
                        });
                        adb.setNegativeButton("Cancel", null);
                        adb.show();
                        break;

                    case Sv.SILENCE_CALL:
                        Sv.setSetting(context, prefKey, Sv.items[i]);
                        break;
                    default:
                }
                ((TextView) ((LinearLayout) view).getChildAt(1)).setText(
                        Html.fromHtml(getTextForAction(prefKey, "#888888", "#52a317")));

                String text = Sv.getSetting(prefKey, Sv.items[NONE]);
                if (Sv.getSetting(prefKey, Sv.items[NONE]).equals(Sv.items[Sv.REJECT_CALL_WITH_SMS])) {
                    text += Sv.getSetting(prefKey + "sms", "(No SMS)");
                }

                // show toast of set action

                Toast.makeText(context, "Action set as : " + text, Toast.LENGTH_SHORT).show();
                dialogInterface.dismiss();
            }
        });
        adb.setNegativeButton("Cancel", null);
        adb.show();
    }
}