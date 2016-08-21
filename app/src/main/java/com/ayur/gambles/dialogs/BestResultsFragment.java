/**
 * Диалоговое окно с лучшими результатами. Аюр М., 29.07.2016
 */

package com.ayur.gambles.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.ayur.gambles.GamblesView;
import com.ayur.gambles.R;

public class BestResultsFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v = LayoutInflater.from(getActivity())
                .inflate(R.layout.best_result_screen, null);
        //извлечь сведения о лучших результатах
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        int easy = preferences.getInt(GamblesView.BEST_SCORE_EASY, 0);
        int normal = preferences.getInt(GamblesView.BEST_SCORE_NORMAL, 0);
        int hard = preferences.getInt(GamblesView.BEST_SCORE_HARD, 0);
        //вывести результаты
        TextView easyTextView = (TextView) v.findViewById(R.id.easy_score_text_view);
        TextView normalTextView = (TextView) v.findViewById(R.id.normal_score_text_view);
        TextView hardTextView = (TextView) v.findViewById(R.id.hard_score_text_view);
        easyTextView.setText(String.valueOf(easy));
        normalTextView.setText(String.valueOf(normal));
        hardTextView.setText(String.valueOf(hard));
        //диалоговое окно с рекордами
        return new AlertDialog.Builder(getActivity())
                .setView(v)
                .setTitle(R.string.best_results_text)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dismiss();
                    }
                })
                .create();
    }

}
