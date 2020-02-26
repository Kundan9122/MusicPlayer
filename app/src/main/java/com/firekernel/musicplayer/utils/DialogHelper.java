package com.firekernel.musicplayer.utils;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Build;
import android.support.v7.app.AlertDialog;



public class DialogHelper {

    public static void showRationalePermissionDialog(final Activity activity, final OnPositiveButtonListener onPositiveButtonListener) {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(activity, android.R.style.Theme_Holo_Light_Dialog_NoActionBar);
        } else {
            builder = new AlertDialog.Builder(activity);
        }
        builder.setTitle("")
                .setMessage("App need storage permission.\nPlease allow the permission required.")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (onPositiveButtonListener != null) {
                            onPositiveButtonListener.onPositiveButtonClick();
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        activity.finish();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .show();
    }

    public interface OnPositiveButtonListener {
        void onPositiveButtonClick();
    }

    public interface OnNegationButtonListener {
        void onNegativeButtonClick();
    }
}
