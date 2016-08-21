/**
 * Диалоговое окно для начала новой игры. Аюр М., 09.07.2016.
 */

package com.ayur.gambles.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;

import com.ayur.gambles.GamblesActivity;
import com.ayur.gambles.R;

public class NewGameFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v = LayoutInflater.from(getActivity())
                .inflate(R.layout.start_new_game, null);
        //диалоговое окно для подтверждения запуска новой игры
        return new AlertDialog.Builder(getActivity())
                .setView(v)
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ((GamblesActivity) getActivity()).newGame();    //начать новую игру
                                dismiss();
                            }
                        })
                .create();
    }
}
