package com.example.rdhol.mineseeker;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

/**
 * Created by rdhol on 2017-02-18.
 */

public class WinDialog extends AppCompatDialogFragment {

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //create the view
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.win_layout, null);

        //create button listener
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        getActivity().finish();
                        break;
                }
                Log.i("Tag", "you clicked the dialog button");
            }
        };

        // build the alert dialog
        return new AlertDialog.Builder(getActivity()).setTitle("YOU WON")
                .setView(v)
                .setPositiveButton(android.R.string.ok, listener)
                .create();
    }
}