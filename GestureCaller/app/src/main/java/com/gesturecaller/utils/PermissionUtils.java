package com.gesturecaller.utils;

import android.Manifest.permission;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;

public class PermissionUtils {

    // list of permissions required by app
    public static final String[] permissionsList = new String[]{
            permission.READ_CONTACTS,
            permission.READ_PHONE_STATE,
            permission.CALL_PHONE,
            permission.SEND_SMS,
            permission.PROCESS_OUTGOING_CALLS,
            permission.WRITE_EXTERNAL_STORAGE,
            permission.ACCESS_FINE_LOCATION,
            permission.ACCESS_COARSE_LOCATION,
    };

    public static boolean hasPermission(Context context, String permission) {
        // checks if a certain permission is granted to the app
        return context.checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean shouldAskPermission() {
        // checks if dynamic permissions must be asked
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1;
    }

    public static boolean hasAllPermissions(Context context) {
        // checks if all the required permissions are granted to the app
        ArrayList<String> unAuthPermissionsList = new ArrayList<>();
        for (String permission : permissionsList) {
            if (!PermissionUtils.hasPermission(context, permission)) {
                unAuthPermissionsList.add(permission);
            }
        }
        return unAuthPermissionsList.isEmpty();
    }

    public static ArrayList<String> getReqPermissionsList(Context context) {
        // returns the non-granted list of permissions after checking all permissions
        ArrayList<String> unAuthPermissionsList = new ArrayList<>();
        for (String permission : permissionsList) {
            if (!PermissionUtils.hasPermission(context, permission)) {
                unAuthPermissionsList.add(permission);
            }
        }
        return unAuthPermissionsList;
    }
}