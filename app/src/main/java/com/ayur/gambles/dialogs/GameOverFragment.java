/**
 * Диалоговое окно окончания игры. Аюр М., 17.07.2016.
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

public class GameOverFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v = LayoutInflater.from(getActivity())
                .inflate(R.layout.game_over, null);
        setCancelable(false);   //нельзя убрать кнопкой "Назад"
        //диалоговое окно о завершении игры
        return new AlertDialog.Builder(getActivity())
                .setView(v)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ((GamblesActivity) getActivity()).newGame();    //начать новую игру
                        dismiss();
                    }
                })
                .create();
    }
}
