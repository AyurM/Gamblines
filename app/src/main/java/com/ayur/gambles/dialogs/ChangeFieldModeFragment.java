/**
 * Диалоговое окно для смена режима игрового поля. Аюр М., 03.08.2016.
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

public class ChangeFieldModeFragment extends DialogFragment {
    private static final String BLOCKED_CELL_MODE = "Blocked_Cell_Mode";
    private boolean mBlockedCellMode;


    public static ChangeFieldModeFragment newInstance(boolean hardMode){
        ChangeFieldModeFragment fragment = new ChangeFieldModeFragment();
        Bundle args = new Bundle();
        args.putBoolean(BLOCKED_CELL_MODE, hardMode);   //сохранить режим игрового поля
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        mBlockedCellMode = getArguments().getBoolean(BLOCKED_CELL_MODE);   //получить сведения о выбранном режиме поля
        View v = LayoutInflater.from(getActivity())
                .inflate(R.layout.change_field_mode, null);
        //диалоговое окно для подтверждения смены режима поля и начала новой игры
        return new AlertDialog.Builder(getActivity())
                .setView(v)
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //сменить режим поля
                                ((GamblesActivity) getActivity()).changeFieldMode(mBlockedCellMode);
                                dismiss();
                            }
                        })
                .create();
    }
}
