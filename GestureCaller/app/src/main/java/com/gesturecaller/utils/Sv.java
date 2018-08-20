package com.gesturecaller.utils;

import android.content.Context;

import com.gesturecaller.models.ExceptionContact;
import com.gesturecaller.models.MyLocation;

import java.util.ArrayList;
import java.util.HashMap;

@SuppressWarnings({"unused", "WeakerAccess"})
public class Sv {
    public static final String ACTIVATE_APP = "activate_app";

    public static final String BIKE_MODE = "bike_mode";
    public static final String LOCATION_MODE = "location_mode";

    public static final String BIKE_MODE_MSG = "bike_mode_msg";

    public static final String FIRST_RUN = "first_run";
    public static final String sSINGLE_TAP = "s_single_tap";
    public static final String sDOUBLE_TAP = "s_double_tap";
    public static final String sTRIPLE_TAP = "s_triple_tap";
    public static final String sSINGLE_TAP_LONG_PRESS = "s_single_long_press";
    public static final String sSWIPE_UP = "s_swipe_up";
    public static final String sSWIPE_DOWN = "s_swipe_down";
    public static final String sSWIPE_LEFT = "s_swipe_left";
    public static final String sSWIPE_RIGHT = "s_swipe_right";

    public static final String[] items = new String[]{
            "None",
            "Hide",
            "Reject Call",
            "Reject call with sms...",
            "Silence Call"
    };

    public static final int NONE = 0;
    public static final int ACCEPT_CALL = 1;
    public static final int REJECT_CALL = 2;
    public static final int REJECT_CALL_WITH_SMS = 3;
    public static final int SILENCE_CALL = 4;

    public static HashMap<String, String> settings = new HashMap<>();

    public static String getSetting(String key, String defaultValue) {
        if (settings.containsKey(key))
            try {
                return String.valueOf(settings.get(key));
            } catch (Exception e) {
                return defaultValue;
            }
        return defaultValue;
    }

    public static boolean getBooleanSetting(String key, boolean defaultValue) {
        if (settings.containsKey(key))
            try {
                return Boolean.parseBoolean(String.valueOf(settings.get(key)));
            } catch (Exception e) {
                return defaultValue;
            }
        return defaultValue;
    }

    public static void setIntSetting(Context context, String key, int value) {
        setSetting(context, key, String.valueOf(value));
    }

    public static int getIntSetting(String key, int defaultValue) {
        if (settings.containsKey(key))
            try {
                return Integer.parseInt(String.valueOf(settings.get(key)));
            } catch (Exception e) {
                return defaultValue;
            }
        return defaultValue;
    }

    public static void setBooleanSetting(Context context, String key, boolean value) {
        setSetting(context, key, String.valueOf(value));
    }

    public static void setSetting(Context context, String key, String value) {
        SQLiteHandler db = new SQLiteHandler(context);
        db.setPref(key, value);
        db.close();
        settings.put(key, value);
    }

    public static boolean refreshSettings(Context context) {
        boolean successfulRefresh;
        SQLiteHandler db = new SQLiteHandler(context);
        try {
            settings = db.getAllSettings();
            successfulRefresh = true;
        } catch (Exception ignored) {
            successfulRefresh = false;
        } finally {
            db.close();
        }

        return successfulRefresh;
    }

    public static int getItemCode(String item) {
        for (int i = 0; i < items.length; i++) {
            if (items[i].equals(item))
                return i;
        }
        return -1;
    }

    public static boolean isContactExceptional(Context context, String contact) {
        boolean isContactExceptional = false;
        SQLiteHandler db = new SQLiteHandler(context);
        try {
            isContactExceptional = db.isContactExceptional(contact);
        } catch (Exception ignored) {
        } finally {
            db.close();
        }

        return isContactExceptional;
    }

    public static boolean addExceptionalContact(Context context, ExceptionContact ec) {
        SQLiteHandler db = new SQLiteHandler(context);
        boolean success = false;
        try {
            success = db.addExceptionContact(ec);
        } catch (Exception ignored) {
        } finally {
            db.close();
        }

        return success;
    }

    public static boolean deleteExceptionalContactByContact(Context context, String contact) {
        SQLiteHandler db = new SQLiteHandler(context);
        boolean success = false;
        try {
            db.deleteExceptionContactWithContact(contact);
            success = true;
        } catch (Exception ignored) {
        } finally {
            db.close();
        }

        return success;
    }

    public static boolean deleteExceptionalContactById(Context context, int id) {
        SQLiteHandler db = new SQLiteHandler(context);
        boolean success = false;
        try {
            db.deleteExceptionContactWithId(id);
            success = true;
        } catch (Exception ignored) {
        } finally {
            db.close();
        }

        return success;
    }

    public static ArrayList<ExceptionContact> getAllExceptionalContacts(Context context) {
        ArrayList<ExceptionContact> list = new ArrayList<>();
        SQLiteHandler db = new SQLiteHandler(context);
        try {
            list = db.getAllExceptionContacts();
        } catch (Exception ignored) {
        } finally {
            db.close();
        }

        return list;
    }

    public static boolean addLocation(Context context, MyLocation location) {
        SQLiteHandler db = new SQLiteHandler(context);
        boolean success = false;
        try {
            success = db.addLocation(location);
        } catch (Exception ignored) {
        } finally {
            db.close();
        }

        return success;
    }

    public static boolean deleteLocationById(Context context, int id) {
        SQLiteHandler db = new SQLiteHandler(context);
        boolean success = false;
        try {
            db.deleteLocationWithId(id);
            success = true;
        } catch (Exception ignored) {
        } finally {
            db.close();
        }

        return success;
    }

    public static ArrayList<MyLocation> getAllLocations(Context context) {
        ArrayList<MyLocation> list = new ArrayList<>();
        SQLiteHandler db = new SQLiteHandler(context);
        try {
            list = db.getAllLocations();
        } catch (Exception ignored) {
        } finally {
            db.close();
        }

        return list;
    }

    public static boolean setLocationEnabled(Context context, int id, boolean enabled) {
        boolean success = false;
        SQLiteHandler db = new SQLiteHandler(context);
        try {
            db.setLocationEnabled(id, enabled);
            success = true;
        } catch (Exception ignored) {
        } finally {
            db.close();
        }
        return success;
    }

    public static MyLocation getCurrActiveLocation(Context context) {
        MyLocation activeLocation = null;
        ArrayList<MyLocation> list = getAllLocations(context);
        for (MyLocation location : list) {
            if (location.isActive(context)) {
                activeLocation = location;
                break;
            }
        }
        return activeLocation;
    }

    public static boolean reset(Context context) {
        boolean success = false;
        SQLiteHandler db = new SQLiteHandler(context);
        try {
            db.reset();
            success = true;
        } catch (Exception ignored) {
        } finally {
            db.close();
        }

        // set default settings
        Sv.setSetting(context, Sv.sSWIPE_UP, Sv.items[Sv.ACCEPT_CALL]);
        Sv.setSetting(context, Sv.sSWIPE_DOWN, Sv.items[REJECT_CALL]);

        Sv.setSetting(context, Sv.sSWIPE_LEFT, Sv.items[Sv.REJECT_CALL_WITH_SMS]);
        Sv.setSetting(context, Sv.sSWIPE_LEFT + "sms", "I am busy. Call you later.");

        Sv.setSetting(context, Sv.sSWIPE_RIGHT, Sv.items[Sv.REJECT_CALL_WITH_SMS]);
        Sv.setSetting(context, Sv.sSWIPE_RIGHT + "sms", "I am in a meeting.");

        Sv.setSetting(context, Sv.sDOUBLE_TAP, Sv.items[Sv.REJECT_CALL_WITH_SMS]);
        Sv.setSetting(context, Sv.sDOUBLE_TAP + "sms", "Give me 5 minutes.");

        Sv.setSetting(context, Sv.sTRIPLE_TAP, Sv.items[Sv.REJECT_CALL_WITH_SMS]);
        Sv.setSetting(context, Sv.sTRIPLE_TAP + "sms", "Can't talk. Text only.");
        return success;
    }

    public static String getSetting(Context context, String key) {
        String value;
        SQLiteHandler db = new SQLiteHandler(context);
        try {
            value = db.getPref(key);
        } catch (Exception e) {
            value = "error";
        }
        db.close();
        return value;
    }
}