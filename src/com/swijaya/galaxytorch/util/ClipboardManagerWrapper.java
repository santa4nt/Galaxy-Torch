package com.swijaya.galaxytorch.util;

import android.annotation.SuppressLint;
import android.content.Context;
import com.swijaya.galaxytorch.R;

public class ClipboardManagerWrapper {

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public static void copyToClipboard(Context context, String text) {
        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context
                    .getSystemService(context.CLIPBOARD_SERVICE);
            clipboard.setText(text);
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context
                    .getSystemService(context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData
                    .newPlainText(
                            context.getResources().getString(
                                    R.string.label_donate_address), text);
            clipboard.setPrimaryClip(clip);
        }
    }

}
