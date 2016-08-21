package com.ayur.gambles;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.Toast;

import com.ayur.gambles.dialogs.BestResultsFragment;
import com.ayur.gambles.dialogs.ChangeDifficultyFragment;
import com.ayur.gambles.dialogs.ChangeFieldModeFragment;
import com.ayur.gambles.dialogs.NewGameFragment;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class GamblesActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {
    private static final String sTag = "Gambles";
    private static final String sNewGameDialog = "NEW_GAME";
    private static final String sChangeDifficulty = "ChangeDifficulty";
    private static final String sChangeFieldMode = "ChangeFieldMode";
    private static final String sBestResults = "sBestResults";
    private static final String sSaveFileName = "gambles_save";

    private GamblesView mGamblesView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGamblesView = new GamblesView(this);
        setContentView(mGamblesView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.gambles_menu, menu);    //меню
        return true;
    }

    @Override
    public void onPause(){
        super.onPause();
        saveGame();    //сохранить текущую игру
    }

    @Override
    public void onResume(){
        super.onResume();
        String savedData = readSaveFile();
        if(!savedData.isEmpty()){
            mGamblesView.loadGambles(savedData);    //возобновить сохраненную игру
            mGamblesView.invalidate();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //выбор пункта "Новая игра"
            case R.id.menu_item_new_game:
                FragmentManager manager = getSupportFragmentManager();
                NewGameFragment dialog = new NewGameFragment();    //диалоговое окно для старта новой игры
                dialog.show(manager, sNewGameDialog);
                return true;
            //выбор пункта "Отменить ход"
            case R.id.menu_item_undo:
                mGamblesView.undoLastTurn();    //отмена последнего хода
                mGamblesView.invalidate();
                return true;
            //выбор пункта "Еще"
            case R.id.menu_item_more:
                View menuItemView = findViewById(R.id.menu_item_more);
                PopupMenu popupMenu = new PopupMenu(this, menuItemView);
                popupMenu.setOnMenuItemClickListener(this);
                popupMenu.inflate(R.menu.additional_menu);
                //настройка заголовка пункта "Звук"
                MenuItem soundItem = popupMenu.getMenu().findItem(R.id.menu_item_sound);
                String soundStatus;
                if(mGamblesView.soundIsOn){
                    soundStatus = getResources().getString(R.string.on_text);
                } else {
                    soundStatus = getResources().getString(R.string.off_text);
                }
                String soundTitle = String.format(getResources().getString(R.string.menu_sound_text), soundStatus);
                soundItem.setTitle(soundTitle);
                //настройка заголовка пункта "Поле"
                MenuItem fieldItem = popupMenu.getMenu().findItem(R.id.menu_item_field);
                String fieldStatus;
                if(mGamblesView.getDiceField().isBlockedCellMode()){
                    fieldStatus = getResources().getString(R.string.hard_field_text);
                } else {
                    fieldStatus = getResources().getString(R.string.normal_field_text);
                }
                String fieldTitle = String.format(getResources().getString(R.string.menu_field_text),
                        fieldStatus);
                fieldItem.setTitle(fieldTitle);
                //настройка заголовка пункта "Сложность"
                MenuItem difficultyItem = popupMenu.getMenu().findItem(R.id.menu_item_difficulty);
                String difficultyStatus;
                switch (mGamblesView.getDiceField().getDifficulty()){
                    case 1:
                        difficultyStatus = getResources().getString(R.string.normal_text);
                        break;
                    case 2:
                        difficultyStatus = getResources().getString(R.string.hard_text);
                        break;
                    default:
                        difficultyStatus = getResources().getString(R.string.easy_text);
                }
                String difficultyTitle = String.format(getResources().getString(R.string.menu_difficulty_text),
                        difficultyStatus);
                difficultyItem.setTitle(difficultyTitle);
                //настройка заголовка пункта "Стиль"
                MenuItem styleItem = popupMenu.getMenu().findItem(R.id.menu_item_style);
                String styleStatus;
                switch (mGamblesView.getDiceStyle()){
                    case 1:
                        styleStatus = getResources().getString(R.string.numbers_text);
                        break;
                    default:
                        styleStatus = getResources().getString(R.string.classic_text);
                }
                String styleTitle = String.format(getResources().getString(R.string.menu_style_text),
                        styleStatus);
                styleItem.setTitle(styleTitle);
                popupMenu.show();    //показать всплывающее меню
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item){
        //обработка выбора пунктов всплывающего меню
        switch (item.getItemId()){
            //выбор пункта "Звук"
            case R.id.menu_item_sound:
                mGamblesView.soundIsOn = !mGamblesView.soundIsOn;    //вкл/выкл звук
                mGamblesView.saveSoundSetting();    //сохранить выбранную настройку
                //подготовка оповещения
                String soundStatus;
                if(mGamblesView.soundIsOn){
                    soundStatus = getResources().getString(R.string.on_text);
                } else {
                    soundStatus = getResources().getString(R.string.off_text);
                }
                String soundState = String.format(getResources().getString(R.string.sound_change_text), soundStatus);
                Toast toast = Toast.makeText(this, soundState, Toast.LENGTH_SHORT);
                toast.show();   //показать оповещение о включении/отключении звука
                return true;
            //выбор пункта "Поле"
            case R.id.menu_item_field:
                //убрать заголовок подменю выбора поля
                SubMenu fieldSubMenu = item.getSubMenu();
                fieldSubMenu.clearHeader();
                //задать корректное положение переключателя поля
                if(mGamblesView.getDiceField().isBlockedCellMode()){
                    fieldSubMenu.findItem(R.id.menu_item_hard_field).setChecked(true);
                } else {
                    fieldSubMenu.findItem(R.id.menu_item_normal_field).setChecked(true);
                }
                return true;
            case R.id.menu_item_normal_field:    //обычное игровое поле
                showChangeFieldModeDialog(false);
                return true;
            case R.id.menu_item_hard_field:      //игровое поле повышенной сложности
                showChangeFieldModeDialog(true);
                return true;
            //выбор пункта "Сложность"
            case R.id.menu_item_difficulty:
                //убрать заголовок подменю выбора сложности
                SubMenu difficultySubMenu = item.getSubMenu();
                difficultySubMenu.clearHeader();
                //задать корректное положение переключателя сложности
                switch (mGamblesView.getDiceField().getDifficulty()){
                    case 1:
                        difficultySubMenu.findItem(R.id.menu_item_normal).setChecked(true);
                        break;
                    case 2:
                        difficultySubMenu.findItem(R.id.menu_item_hard).setChecked(true);
                        break;
                    default:
                        difficultySubMenu.findItem(R.id.menu_item_easy).setChecked(true);
                }
                return true;
            case R.id.menu_item_easy:      //легкая сложность
                showChangeDifficultyDialog(0);
                return true;
            case R.id.menu_item_normal:    //нормальная сложность
                showChangeDifficultyDialog(1);
                return true;
            case R.id.menu_item_hard:      //сложная сложность!
                showChangeDifficultyDialog(2);
                return true;
            //смена оформления
            case R.id.menu_item_style:
                //убрать заголовок подменю выбора оформления
                SubMenu styleSubMenu = item.getSubMenu();
                styleSubMenu.clearHeader();
                //задать корректное положение переключателя оформления
                switch (mGamblesView.getDiceStyle()){
                    case 1:
                        styleSubMenu.findItem(R.id.menu_item_numbers).setChecked(true);
                        break;
                    default:
                        styleSubMenu.findItem(R.id.menu_item_classic).setChecked(true);
                }
                return true;
            case R.id.menu_item_classic:    //кубики
                changeDiceStyle(0);
                return true;
            case R.id.menu_item_numbers:    //цифры
                changeDiceStyle(1);
                return true;
            //выбор пункта "Как играть"
            case R.id.menu_item_help:
                Intent i = new Intent(GamblesActivity.this, HelpActivity.class);
                startActivity(i);       //показать окно со справочной информацией
                return true;
            //выбор пункта "Лучшие результаты"
            case R.id.menu_item_show_records:
                FragmentManager manager = getSupportFragmentManager();
                BestResultsFragment dialog = new BestResultsFragment(); //диалоговое окно с рекордами
                dialog.show(manager, sBestResults);
                return true;
            default:
                return true;
        }
    }

    /**
     * Начинает новую игру
     */
    public void newGame(){
        mGamblesView.resetGame();
        mGamblesView.invalidate();
    }

    /**
     * Показывает диалог перезапуска игры для смены режима игрового поля
     */
    private void showChangeFieldModeDialog(boolean hardMode){
        //если выбираемый режим отличается от текущего
        if(hardMode != mGamblesView.getDiceField().isBlockedCellMode()){
            FragmentManager manager = getSupportFragmentManager();
            //диалоговое окно для подтверждения смены режима игрового поля
            ChangeFieldModeFragment dialog = ChangeFieldModeFragment.newInstance(hardMode);
            dialog.show(manager, sChangeFieldMode);
        }
    }

    /**
     * Начинает новую игру с выбранным режимом игрового поля
     * @param hardMode выбранный режим поля
     */
    public void changeFieldMode(boolean hardMode){
        mGamblesView.getDiceField().setBlockedCellMode(hardMode);    //сменить режим поля
        mGamblesView.saveBlockedCellMode();    //запомнить настройку
        newGame();
    }

    /**
     * Показывает диалог перезапуска игры для смены сложности
     */
    private void showChangeDifficultyDialog(int difficulty){
        //если выбираемая сложность отличается от текущей
        if(mGamblesView.getDiceField().getDifficulty() != difficulty){
            FragmentManager manager = getSupportFragmentManager();
            //диалоговое окно для подтверждения смены сложности
            ChangeDifficultyFragment dialog = ChangeDifficultyFragment.newInstance(difficulty);
            dialog.show(manager, sChangeDifficulty);
        }
    }

    /**
     * Начинает новую игру на выбранной сложности
     * @param difficulty выбранная сложность
     */
    public void changeDifficulty(int difficulty){
        mGamblesView.getDiceField().setDifficulty(difficulty);    //сменить сложность
        mGamblesView.saveDifficulty();    //запомнить выбранную сложность
        newGame();
    }

    /**
     * Изменяет визуальный стиль оформления
     * @param style номер выбранного стиля
     */
    private void changeDiceStyle(int style){
        if(mGamblesView.getDiceStyle() != style){
            mGamblesView.setDiceStyle(style);
            mGamblesView.saveDiceStyle();    //запомнить настройку оформления
            mGamblesView.loadDiceDrawables();    //загрузить новые рисунки для кубиков
            mGamblesView.getDiceField().prepareDiceDrawables();    //обновить текущие рисунки кубиков
            mGamblesView.invalidate();
        }
    }

    /**
     * Выполняет сохранение игры
     */
    private void saveGame(){
        writeSaveFile(mGamblesView.getDiceField());
    }

    /**
     * Сохраняет состояние поля в файл
     * @param diceField сохраняемое поле
     */
    private void writeSaveFile(DiceField diceField){
        FileOutputStream outputStream;
        String dataToSave = "";
        int[][] dices = diceField.getDices();
        //подготовка к записи
        for(int i = 0; i < DiceField.FIELD_SIZE; i++){
            for(int j = 0; j < DiceField.FIELD_SIZE; j++){
                dataToSave += Integer.toString(dices[i][j]);
            }
        }
        dataToSave += Integer.toString(mGamblesView.getScore());
        //запись в файл
        try{
            outputStream = openFileOutput(sSaveFileName, Context.MODE_PRIVATE);
            outputStream.write(dataToSave.getBytes());
            outputStream.close();
        } catch (Exception e){
            Log.e(sTag, "Error while saving game!", e);
        }
    }

    /**
     * Считывает файл сохранения
     * @return строка с информацией о сохраненном состоянии поля
     */
    private String readSaveFile(){
        String result = "";

        try{
            InputStream inputStream = openFileInput(sSaveFileName);    //открыть сэйв-файл
            //прочитать данные
            if(inputStream != null){
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString;
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                result = stringBuilder.toString();
            }
        } catch (FileNotFoundException e){
            Log.i(sTag, "Save file not found!"); //файл сохранения отсутствует (скорее всего, это первый запуск игры на устройстве)
        } catch (IOException e){
            Log.e(sTag, "Error while reading save file!");
        }
        return result;
    }
}
