/**
 * Показывает диалог рестарта игры при смене сложности. Аюр М., 29.07.2016.
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

public class ChangeDifficultyFragment extends DialogFragment {
    private static final String SELECTED_DIFFICULTY = "Selected_Difficulty";

    private int mDifficulty = 0;

    public static ChangeDifficultyFragment newInstance(int difficulty){
        ChangeDifficultyFragment fragment = new ChangeDifficultyFragment();
        Bundle args = new Bundle();
        args.putInt(SELECTED_DIFFICULTY, difficulty);   //сохранить выбранную сложность
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        mDifficulty = getArguments().getInt(SELECTED_DIFFICULTY);   //получить сведения о выбранной сложности
        View v = LayoutInflater.from(getActivity())
                .inflate(R.layout.change_difficulty, null);
        //диалоговое окно для подтверждения смены сложности и начала новой игры
        return new AlertDialog.Builder(getActivity())
                .setView(v)
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //сменить сложность
                                ((GamblesActivity) getActivity()).changeDifficulty(mDifficulty);
                                dismiss();
                            }
                        })
                .create();
    }
}
