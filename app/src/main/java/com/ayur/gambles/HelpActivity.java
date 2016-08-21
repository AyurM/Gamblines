/**
 * Показывает окно с помошью по игре. Аюр М.
 */

package com.ayur.gambles;

import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class HelpActivity extends AppCompatActivity {
    private int mPageNumber = 1;
    private ImageView mHelpImage;
    private TextView mHelpText;
    private TextView mBackText;
    private TextView mNextText;
    private TextView mPageNumText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help_screen);

        mHelpImage = (ImageView) findViewById(R.id.help_screen_image);
        mHelpText = (TextView) findViewById(R.id.help_screen_text);
        mBackText = (TextView) findViewById(R.id.help_screen_back_text);
        mNextText = (TextView) findViewById(R.id.help_screen_next_text);
        mPageNumText = (TextView) findViewById(R.id.help_screen_page_number);
        String text = String.format(getResources().getString(R.string.help_text_page_number), mPageNumber);
        mPageNumText.setText(text);

        mBackText.setVisibility(View.INVISIBLE);
        //кнопка "Далее" на экране справки
        mNextText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mPageNumber != 3){
                    //обновляем справочную информацию на экране
                    switch (mPageNumber){
                        case 1:
                            mHelpImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(),
                                    R.drawable.help_2, null));
                            mHelpText.setText(getResources().getString(R.string.help_text_two));
                            mBackText.setVisibility(View.VISIBLE);
                            mPageNumber++;
                            break;
                        case 2:
                            mHelpImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(),
                                    R.drawable.help_3, null));
                            mHelpText.setText(getResources().getString(R.string.help_text_three));
                            mBackText.setVisibility(View.VISIBLE);
                            mNextText.setVisibility(View.INVISIBLE);
                            mPageNumber++;
                            break;
                    }
                    String text = String.format(getResources().getString(R.string.help_text_page_number), mPageNumber);
                    mPageNumText.setText(text);
                }
            }
        });
        //кнопка "Назад" на экране справки
        mBackText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mPageNumber != 1){
                    //обновляем справочную информацию на экране
                    switch (mPageNumber){
                        case 3:
                            mHelpImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(),
                                    R.drawable.help_2, null));
                            mHelpText.setText(getResources().getString(R.string.help_text_two));
                            mNextText.setVisibility(View.VISIBLE);
                            mPageNumber--;
                            break;
                        case 2:
                            mHelpImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(),
                                    R.drawable.help_1, null));
                            mHelpText.setText(getResources().getString(R.string.help_text_one));
                            mNextText.setVisibility(View.VISIBLE);
                            mBackText.setVisibility(View.INVISIBLE);
                            mPageNumber--;
                            break;
                    }
                    String text = String.format(getResources().getString(R.string.help_text_page_number), mPageNumber);
                    mPageNumText.setText(text);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.help_menu, menu);    //меню
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //выбор пункта "Закрыть"
            case R.id.menu_item_close:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
