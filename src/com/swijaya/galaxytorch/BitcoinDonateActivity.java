package com.swijaya.galaxytorch;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class BitcoinDonateActivity extends Activity implements View.OnClickListener {

    private static final String TAG = BitcoinDonateActivity.class.getSimpleName();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.btcdonate);
    }

    @Override
    public void onClick(View v) {
        Log.i(TAG, "Cannot acquire camera. Closing activity.");
        Toast toast = Toast.makeText(getApplicationContext(),
                R.string.toast_address_copied, Toast.LENGTH_SHORT);
        toast.show();
    }
}