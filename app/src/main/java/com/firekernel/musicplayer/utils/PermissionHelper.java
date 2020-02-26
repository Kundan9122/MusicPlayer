package com.firekernel.musicplayer.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;



public class PermissionHelper {
    private static final int REQUEST_CODE = 1 << 2;

    public static void requestPermission(final Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return;
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale
                    (activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                DialogHelper.showRationalePermissionDialog(activity, new DialogHelper.OnPositiveButtonListener() {
                    @Override
                    public void onPositiveButtonClick() {
                        ActivityCompat.requestPermissions(activity,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                REQUEST_CODE);
                    }
                });
//                Snackbar.make(activity.findViewById(android.R.id.content),
//                        "Please Grant Permissions", Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
//                        new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                                ActivityCompat.requestPermissions(activity,
//                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                                        REQUEST_CODE);
//                            }
//                        }).show();
            } else {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE);
            }
        } else {
            //Call whatever you want
        }
    }

    public static void onRequestPermissionsResult(final Activity activity, int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    activity.recreate();
                } else {
                    activity.finish();
//                    Snackbar.make(activity.findViewById(android.R.id.content), "Enable Permissions from settings",
//                            Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
//                            new View.OnClickListener() {
//                                @Override
//                                public void onClick(View v) {
//                                    Intent intent = new Intent();
//                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//                                    intent.addCategory(Intent.CATEGORY_DEFAULT);
//                                    intent.setData(Uri.parse("package:" + activity.getPackageName()));
//                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
//                                    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
//                                    activity.startActivity(intent);
//                                }
//                            }).show();
                }
                return;
            }
        }
    }

}
