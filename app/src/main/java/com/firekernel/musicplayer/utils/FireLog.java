package com.firekernel.musicplayer.utils;

import android.content.Context;
import android.util.Log;

import com.firekernel.musicplayer.BuildConfig;
import com.firekernel.musicplayer.FireApplication;

public class FireLog {
    private static final String LOG_PREFIX = "f_";
    private static final int LOG_PREFIX_LENGTH = LOG_PREFIX.length();
    private static final int MAX_LOG_TAG_LENGTH = 23;
    private static boolean isVerboseLogOn = true;
    private static boolean isDebugLogOn = true;
    private static boolean isInfoLogOn = true;

    static {
        enableDisableDebugMode(isDebuggable(FireApplication.getInstance()));
    }

    public static String makeLogTag(String str) {
        if (str.length() > MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH) {
            return LOG_PREFIX + str.substring(0, MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH - 1);
        }

        return LOG_PREFIX + str;
    }

    public static String makeLogTag(Class cls) {
        return makeLogTag(cls.getSimpleName());
    }

    private static void enableDisableDebugMode(boolean enable) {
        if (enable) {
            FireLog.isVerboseLogOn = true;
            FireLog.isDebugLogOn = true;
            FireLog.isInfoLogOn = true;
        } else {
            FireLog.isVerboseLogOn = false;
            FireLog.isDebugLogOn = false;
            FireLog.isInfoLogOn = false;
        }
    }

    private static boolean isDebuggable(Context context) {
        return BuildConfig.DEBUG;
    }

//    private static boolean isDebuggable(Context ctx) {
//        final X500Principal DEBUG_DN = new X500Principal("CN=Android Debug,O=Android,C=US");
//        boolean debuggable = false;
//
//        try {
//            PackageInfo pinfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), PackageManager.GET_SIGNATURES);
//            Signature signatures[] = pinfo.signatures;
//
//            CertificateFactory cf = CertificateFactory.getInstance("X.509");
//
//            for (Signature signature : signatures) {
//                ByteArrayInputStream stream = new ByteArrayInputStream(signature.toByteArray());
//                X509Certificate cert = (X509Certificate) cf.generateCertificate(stream);
//                debuggable = cert.getSubjectX500Principal().equals(DEBUG_DN);
//                if (debuggable)
//                    break;
//            }
//        } catch (NameNotFoundException | CertificateException e) {
//            e.printStackTrace();
//        }
//        return debuggable;
//    }

    public static void v(String tag, String msg) {
        if (isVerboseLogOn) {
            Log.v(tag, msg);
        }
    }

    public static void d(String tag, String msg) {
        if (isDebugLogOn) {
            Log.d(tag, msg);
        }
    }

    public static void i(String tag, String msg) {
        if (isInfoLogOn) {
            Log.i(tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        Log.w(tag, msg);
    }


    public static void e(String tag, String msg) {
        Log.e(tag, msg);
    }

    public static void e(String tag, String msg, Throwable exception) {
        Log.e(tag, msg, exception);
    }

}
