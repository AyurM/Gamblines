/**
 * Главная вьюшка. Аюр М., 05.07.2016.
 */

package com.ayur.gambles;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.ayur.gambles.utils.GamblesAnimation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GamblesView extends View{
    private static final String sTag = "GamblesView";              //метка для логов
    private static final String sSoundSetting = "SoundSetting";    //метка для сохранения настройки звука
    private static final String sDifficulty = "Difficulty";        //метка для сохранения текущей сложности игры
    private static final String sDiceStyle = "DiceStyle";          //метка для сохранения стиля оформления
    private static final String sBlockedCellMode = "BlockedCellMode";          //метка для сохранения стиля оформления
    private static final int sVibrationLength = 100;      //длительность вибрации, мс
    private static final int sCellToPaddingRatio = 12;    //отношение размера клетки поля к толщине разделительной линии
    public static final String BEST_SCORE_EASY = "BestScoreEasy";    //метки для сохранения рекорда
    public static final String BEST_SCORE_NORMAL = "BestScoreNormal";
    public static final String BEST_SCORE_HARD = "BestScoreHard";

    private final Paint mPaint = new Paint();
    private DiceField mDiceField = null;            //экземпляр игрового поля
    private Drawable mBackgroundRectangle = null;   //фон игрового поля
    private Drawable mScoreBackground = null;       //фон таблички с очками
    private Drawable[][] mCellsDrawable = null;     //рисунки для клеток игрового поля
    private final Typeface mTextFont = Typeface.SANS_SERIF;    //шрифт для вывода текста

    private int mScore = 0;         //набранные очки
    private int mPrevScore = 0;     //очки на предыдущем ходу
    private int mScoreGained = 0;   //очки, набранные на данном ходу
    private int mBestScore = 0;     //лучший результат
    public int diceSize;            //размер клетки поля
    private int mFieldSize;         //размер поля (поле является квадратом)
    private final int[] mMaxViewSize = new int[2];    //размеры вьюшки
    private int mScoreColor;          //цвет для отрисовки очков в зависимости от текущей сложности игры
    public int paddingDivider = 0;    //размер разделительной линии при отрисовке игрового поля
    public int startX, startY;        //границы для отрисовки поля
    private int mEndX, mEndY;
    private boolean mDiceMoveTriggered = false;      //флаг начала перемещения кубика
    private int[] mMovingDicePosition = {-1, -1};    //координаты кубика, подлежащего перемещению
    private List<int[]> mCellsToHighlight = new ArrayList<>();    //позиции клеток, которые нужно подсветить при перемещении кубика
    public final Bitmap[] diceBitmaps = new Bitmap[8];            //рисунки для клеток
    public final List<int[]> drawingOrder = new ArrayList<>();    //используется для корректировки порядка отрисовки клеток при анимации перемещения
    public boolean soundIsOn = true;    //вкл/выкл звук
    private int mDiceStyle = 0;         //стиль кубиков

    private final Vibrator mVibrator;    //отвечает за вибрацию устройства
    private MediaPlayer mMoveSound = null, mDeleteSound = null, mChangeSound = null;     //звуки


    public GamblesView(Context context){
        super(context);
        mDiceField = new DiceField();

        try{
            //подготовка фонов
            mBackgroundRectangle = ResourcesCompat.getDrawable(getResources(), R.drawable.background_rectangle, null);
            mScoreBackground = ResourcesCompat.getDrawable(getResources(), R.drawable.score_background, null);
            this.setBackgroundColor(ContextCompat.getColor(context, R.color.background));
            //параметры текста
            mPaint.setAntiAlias(true);
            mPaint.setTextAlign(Paint.Align.CENTER);
            //подготовка звуков
            mMoveSound = MediaPlayer.create(context, R.raw.dice_move_sound);        //звук перемещения кубика
            mDeleteSound = MediaPlayer.create(context, R.raw.dice_delete_sound);    //звук исчезновения кубиков
            mChangeSound = MediaPlayer.create(context, R.raw.dice_change_sound);    //звук изменения кубика
        } catch(Exception e){
            Log.e(sTag, "Error while loading resources", e);
        }

        setOnTouchListener(new GamblesListener(this));    //задаем обработчик кликов
        mVibrator = (Vibrator) this.getContext().getSystemService(Context.VIBRATOR_SERVICE);
        readDifficulty();         //загрузить настройку сложности игры
        readBestScore();          //загрузить сведения о рекорде
        readSoundSetting();       //загрузить настройку звука
        chooseScoreColor();       //выставить цвет для отрисовки очков
        readDiceStyle();          //загрузить настройку оформления кубиков
        readBlockedCellMode();    //загрузить настройку режима игрового поля
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mMaxViewSize[0] = this.getWidth();     //определяем максимальные размеры
        mMaxViewSize[1] = this.getHeight();
        this.mFieldSize = countFieldSize();    //вычисляем размеры игрового поля
        setBackgroundBounds();                 //задаем координаты для отрисовки фона игрового поля
        loadDiceDrawables();
        mDiceField.setGView(this);             //передаем ссылку на эту вьюшку
        mDiceField.prepareDiceDrawables();     //подготавливаем рисунки для клеток
        mCellsDrawable = mDiceField.getCellsDrawables();    //получаем ссылку на массив с рисунками для клеток
    }

    @Override
    public void onDraw(Canvas canvas) {
        if(mDiceField == null){
            return;
        }
        drawBackground(canvas);    //прорисовка фона игрового поля
        drawCells(canvas);         //прорисовка клеток игрового поля
        drawScore(canvas);         //прорисовка таблички с очками
    }

    /**
     * Рассчитывает размеры квадратного игрового поля, а также размеры клетки поля.
     * @return величина стороны игрового поля
     */
    private int countFieldSize(){
        //определить текущее соотношение размеров вьюшки
        double screenRatio = (double) Math.max(mMaxViewSize[0], mMaxViewSize[1]) /
                Math.min(mMaxViewSize[0], mMaxViewSize[1]);
        double screenRatioRequired = (double)(2*sCellToPaddingRatio - 4) /
                (DiceField.FIELD_SIZE + sCellToPaddingRatio * DiceField.FIELD_SIZE + 5) + 1;
        //рассчитать толщину разделительной линии в зависимости от соотношения размеров
        if(screenRatio < screenRatioRequired){
            //после отрисовки поля должно остаться 2 * diceSize пикселей свободного пространства в высоту
            paddingDivider = Math.max(mMaxViewSize[0], mMaxViewSize[1]) /
                    (2 * sCellToPaddingRatio + DiceField.FIELD_SIZE * (sCellToPaddingRatio + 1) + 1);
        } else {
            paddingDivider = (int) Math.round((double) Math.min(mMaxViewSize[0], mMaxViewSize[1]) /
                    (DiceField.FIELD_SIZE * (sCellToPaddingRatio + 1) + 5));
        }
        this.diceSize = paddingDivider * sCellToPaddingRatio;    //задаем размер клетки поля
        //возвращаем длину игрового поля (поле квадратное)
        return this.diceSize * DiceField.FIELD_SIZE + paddingDivider * (DiceField.FIELD_SIZE + 1);
    }

    /**
     * Устанавливает координаты границ для последующей отрисовки фона игрового поля
     */
    private void setBackgroundBounds(){
        startX = (mMaxViewSize[0] - mFieldSize) / 2;
        mEndX = startX + mFieldSize;
        startY = Math.max((int)(1.5 * diceSize),
                Math.abs(2 * (mMaxViewSize[1] - mMaxViewSize[0]) / 3));    //границы по Y зависят от соотношения сторон вьюшки
        mEndY = startY + mFieldSize;
    }

    /**
     * Загружает фоновые рисунки для клеток, используя сведения о размере клетки
     * игрового поля и выбранном стиле оформления
     */
    public void loadDiceDrawables(){
        for(int i = 0; i < 8; i++){
            Bitmap bitmap = null;
            switch (i){
                case 0:
                    bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.dice_empty);
                    break;
                case 1:
                    bitmap = BitmapFactory.decodeResource(getResources(),
                            mDiceStyle == 0 ? R.drawable.dice_one : R.drawable.number_one);
                    break;
                case 2:
                    bitmap = BitmapFactory.decodeResource(getResources(),
                            mDiceStyle == 0 ? R.drawable.dice_two : R.drawable.number_two);
                    break;
                case 3:
                    bitmap = BitmapFactory.decodeResource(getResources(),
                            mDiceStyle == 0 ? R.drawable.dice_three : R.drawable.number_three);
                    break;
                case 4:
                    bitmap = BitmapFactory.decodeResource(getResources(),
                            mDiceStyle == 0 ? R.drawable.dice_four : R.drawable.number_four);
                    break;
                case 5:
                    bitmap = BitmapFactory.decodeResource(getResources(),
                            mDiceStyle == 0 ? R.drawable.dice_five : R.drawable.number_five);
                    break;
                case 6:
                    bitmap = BitmapFactory.decodeResource(getResources(),
                            mDiceStyle == 0 ? R.drawable.dice_six : R.drawable.number_six);
                    break;
                case 7:
                    bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.dice_highlight);
                    break;
            }
            //контролируем размер загружаемых картинок
            bitmap = Bitmap.createScaledBitmap(bitmap, diceSize, diceSize, false);
            diceBitmaps[i] = bitmap;
        }
    }

    /**
     * Рисует фоновую подложку для игрового поля
     * @param canvas рисуем здесь
     */
    private void drawBackground(Canvas canvas){
        mBackgroundRectangle.setBounds(startX, startY, mEndX, mEndY);
        mBackgroundRectangle.draw(canvas);
    }

    /**
     * Задает цвет для отрисовки рекорда и набираемых очков в зависимости от текущей
     * сложности
     */
    private void chooseScoreColor(){
        switch (mDiceField.getDifficulty()){
            case 1:
                mScoreColor = ContextCompat.getColor(getContext(), R.color.normalText);
                break;
            case 2:
                mScoreColor = ContextCompat.getColor(getContext(), R.color.hardText);
                break;
            default:
                mScoreColor = ContextCompat.getColor(getContext(), R.color.easyText);
        }
    }

    /**
     * Выводит на экран табличку с очками
     * @param canvas рисуем здесь
     */
    private void drawScore(Canvas canvas){
        //параметры текста
        mPaint.setColor(ContextCompat.getColor(getContext(), R.color.whiteText));
        mPaint.setTypeface(mTextFont);      //задать шрифт для надписей
        mPaint.setTextSize((float) (diceSize * 0.3));  //размер шрифта
        //задать границы отрисовки для таблички со счетом
        String score = getResources().getString(R.string.score_text);
        float width = mPaint.measureText(score);
        int sX = (int) ((mMaxViewSize[0] / 2) - width);
        int sY = paddingDivider * 3;
        int eX = (int) ((mMaxViewSize[0] / 2) + width);
        int eY = sY + diceSize;
        int textShift = (int) (mPaint.descent() + mPaint.ascent()) / 2;
        mScoreBackground.setBounds(sX, sY, eX, eY);
        mScoreBackground.draw(canvas);  //рисуем фон таблички

        float x = (float) (mMaxViewSize[0] / 2);
        float y = sY + diceSize / 4;
        canvas.drawText(score, x, y - textShift, mPaint);   //надпись

        mPaint.setTextSize((float) (diceSize * 0.5));
        mPaint.setFakeBoldText(true);
        y = eY - diceSize / 4;
        canvas.drawText("" + mScore, x, y - textShift, mPaint);   //количество очков

        mPaint.setTextSize((float) (diceSize * 0.3));
        mPaint.setFakeBoldText(false);
        mPaint.setColor(mScoreColor);
        //сформировать строку для вывода рекорда на экран
        String bestScore = String.format(getResources().getString(R.string.best_score_text), mBestScore);
        y = mEndY + paddingDivider * 3;
        canvas.drawText(bestScore, x, y - textShift, mPaint);   //лучший результат
        //показать набранное на данном ходу количество очков ("анимация" увеличения результата)
        if(mScoreGained > 0){
            mPaint.setColor(mScoreColor);
            mPaint.setTextSize((float) (diceSize * 0.4));
            canvas.drawText("+" + mScoreGained, (float)(x + diceSize * 0.8), sY + diceSize / 4 - textShift, mPaint);
        }
    }

    /**
     * Выводит на экран клетки игрового поля
     * @param canvas рисуем здесь
     */
    private void drawCells(Canvas canvas){
        //если порядок прорисовки изменять не нужно, то рисуем клетки по порядку
        if(drawingOrder.isEmpty()){
            for(int i = 0; i < DiceField.FIELD_SIZE; i++) {
                for (int j = 0; j < DiceField.FIELD_SIZE; j++) {
                    if(mCellsDrawable[i][j] != null){
                        mCellsDrawable[i][j].draw(canvas);  //нарисовать текущую клетку
                    }
                }
            }
        } else {
            //вмешаться в порядок прорисовки клеток
            int[] drawFirst = drawingOrder.get(0);    //клетка с этой позиции должна быть отрисована раньше
            int[] drawSecond = drawingOrder.get(1);   //этой клетки
            for(int i = 0; i < DiceField.FIELD_SIZE; i++) {
                for (int j = 0; j < DiceField.FIELD_SIZE; j++) {
                    if (drawSecond[0] == i && drawSecond[1] == j) {
                        mCellsDrawable[drawFirst[0]][drawFirst[1]].draw(canvas); //поменять местами порядок
                    } else if (drawFirst[0] == i && drawFirst[1] == j) {
                        mCellsDrawable[drawSecond[0]][drawSecond[1]].draw(canvas);  //прорисовки клеток
                    } else {
                        if (mCellsDrawable[i][j] != null) {
                            mCellsDrawable[i][j].draw(canvas);  //нарисовать текущую клетку
                        }
                    }
                }
            }
        }
    }

    /**
     * Сбрасывает информацию о перемещении кубика
     */
    public void resetMoveInfo(){
        mDiceMoveTriggered = false;
        mMovingDicePosition[0] = -1; mMovingDicePosition[1] = -1;
        mCellsToHighlight.clear();
    }

    /**
     * Выполняет действия для начала новой игры
     */
    public void resetGame(){
        mScore = mPrevScore = 0;	//обнулить очки
        readBestScore();            //обновить рекорд для данной сложности
        chooseScoreColor();         //обновить цвет для отрисовки очков
        resetMoveInfo();
        if(mDiceField.isBlockedCellMode()){    //убрать рисунок для заблокированной клетки
            mCellsDrawable[DiceField.BLOCKED_CELL[0]][DiceField.BLOCKED_CELL[1]] = null;
        }
        mDiceField.resetField();	//сбросить игровое поле
        mDiceField.prepareDiceDrawables();	//подготовить рисунки для клеток
    }

    /**
     * Отменяет последний ход
     */
    public void undoLastTurn(){
        mDiceField.undoTurn();
        mScore = mPrevScore;
    }

    public DiceField getDiceField() {
        return mDiceField;
    }

    public Integer getScore() {
        return mScore;
    }

    public void resetScoreGained() {
        mScoreGained = 0;
    }

    /**
     * Загружает сохраненную игру
     * @param savedData строка с сохраненным состоянием игры
     */
    public void loadGambles(String savedData){
        mDiceField.setGView(this);		//передать ссылку на вьюшку
        mDiceField.setDices(mDiceField.loadFieldFromString(savedData));  //загрузить игровое поле
        String savedScore = savedData.substring(DiceField.FIELD_SIZE * DiceField.FIELD_SIZE);
        mScore = Integer.parseInt(savedScore);   //загрузить количество набранных очков
        mPrevScore = mScore;
        resetMoveInfo();
        mDiceField.prepareDiceDrawables();  //обновить рисунки для клеток
    }

    /**
     * Загружает лучший результат из настроек
     */
    private void readBestScore(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        //загрузить лучший результат для текущей сложности
        switch (mDiceField.getDifficulty()){
            case 1:
                mBestScore = preferences.getInt(BEST_SCORE_NORMAL, 0);
                break;
            case 2:
                mBestScore = preferences.getInt(BEST_SCORE_HARD, 0);
                break;
            default:
                mBestScore = preferences.getInt(BEST_SCORE_EASY, 0);
        }
    }

    /**
     * Сохраняет лучший набранный результат в настройках
     */
    private void saveBestScore(){
        if(mScore >= mBestScore){
            mBestScore = mScore;
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor editor = preferences.edit();
            //сохранить рекорд для текущей сложности
            switch (mDiceField.getDifficulty()){
                case 1:
                    editor.putInt(BEST_SCORE_NORMAL, mBestScore);
                    break;
                case 2:
                    editor.putInt(BEST_SCORE_HARD, mBestScore);
                    break;
                default:
                    editor.putInt(BEST_SCORE_EASY, mBestScore);
            }
            editor.apply();
        }
    }

    /**
     * Сохраняет сведения о текущей настройке звука
     */
    public void saveSoundSetting(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(sSoundSetting, soundIsOn);
        editor.apply();
    }

    /**
     * Загружает настройку звука
     */
    private void readSoundSetting(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        soundIsOn = preferences.getBoolean(sSoundSetting, true);
    }

    /**
     * Сохраняет сведения о текущей сложности игры
     */
    public void saveDifficulty(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(sDifficulty, mDiceField.getDifficulty());
        editor.apply();
    }

    /**
     * Загружает сведения о сложности игры из настроек
     */
    private void readDifficulty(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        mDiceField.setDifficulty(preferences.getInt(sDifficulty, 0));
    }

    public int getDiceStyle() {
        return mDiceStyle;
    }

    public void setDiceStyle(int diceStyle) {
        mDiceStyle = diceStyle;
    }

    /**
     * Сохраняет сведения о текущей сложности игры
     */
    public void saveDiceStyle(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(sDiceStyle, mDiceStyle);
        editor.apply();
    }

    /**
     * Загружает сведения о сложности игры из настроек
     */
    private void readDiceStyle(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        mDiceStyle = preferences.getInt(sDiceStyle, 0);
    }

    /**
     * Сохраняет сведения о режиме игрового поля
     */
    public void saveBlockedCellMode(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(sBlockedCellMode, mDiceField.isBlockedCellMode());
        editor.apply();
    }

    /**
     * Загружает сведения о режиме игрового поля из настроек
     */
    private void readBlockedCellMode(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        mDiceField.setBlockedCellMode(preferences.getBoolean(sBlockedCellMode, false));
    }

    /**
     * Обработчик кликов
     */
    private class GamblesListener implements View.OnTouchListener{
        private final GamblesView mGamblesView;   //ссылка на вьюшку
        private float mX, mY;
        private float mDownX, mDownY;
        private int[] mDownPosition = new int[2];
        private static final int sLongClickThreshold = 250;    //порог для распознавания долгого нажатия, мс
        private long mDownTime = 0;  //время нажатия
        private long mUpTime = 0;    //время отжатия

        public GamblesListener(GamblesView gamblesView){
            super();
            this.mGamblesView = gamblesView;
        }

        @Override
        public boolean onTouch(View view, MotionEvent event){
            switch (event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    mDownX = event.getX();    //координаты точки отжатия
                    mDownY = event.getY();
                    mDownPosition = findCellPosition(mDownX, mDownY);    //находим позицию кубика на поле по координатам касания
                    mDownTime = event.getEventTime();    //получаем время нажатия
                    if(verifyPosition(mDownPosition) && mDiceField
                            .getDices()[mDownPosition[0]][mDownPosition[1]] != 0){
                        GamblesAnimation.animateDicePressing(mDownPosition, mCellsDrawable, mGamblesView,
                                mDiceField.getGAListener());    //анимация нажатия на ненулевой кубик
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    mX = event.getX();    //координаты точки отжатия
                    mY = event.getY();
                    int[] position = findCellPosition(mX, mY);    //находим позицию кубика на поле по координатам касания
                    mUpTime = event.getEventTime();    //получаем время отжатия
                    //одиночное касание используется для перемещения кубика
                    if(Math.abs(mUpTime - mDownTime) < sLongClickThreshold){
                        if(verifyPosition(position)){
                            if(mDiceMoveTriggered){    //вторая фаза перемещения кубика
                                mDiceField.prepareCellsToHighlight(mCellsToHighlight, position, true);    //убрать подсветку клеток
                                //кубик перемещается только на пустые клетки (или выполнен клик по той же самой клетке)
                                if(mDiceField.getDices()[position[0]][position[1]] == 0 ||
                                        Arrays.equals(mMovingDicePosition, position)){
                                    mPrevScore = mScore; //обновить сведения об очках на случай отмены хода
                                    //обработать перемещение кубика
                                    boolean diceMoved = mDiceField.moveDice(mMovingDicePosition, position, mCellsToHighlight, getContext());
                                    List<int[]> winIndices = new ArrayList<>();
                                    //если кубик действительно был перемещен
                                    if(diceMoved){
                                        winIndices = mDiceField.checkField();   //проверить совпадения кубиков
                                        mScoreGained = mDiceField.removeWinIndices(winIndices, position, true, true); //обновить очки
                                        mScore += mScoreGained;
                                        saveBestScore();     //обновить сведения о рекорде очков
                                        //если произошло перемещение кубика, настроить порядок отрисовки клеток
                                        adjustDrawingOrder(mMovingDicePosition, position);
                                        if(soundIsOn){
                                            if(mScoreGained > 0){
                                                mDeleteSound.start();    //звук исчезновения кубиков
                                            } else {
                                                mMoveSound.start();      //звук перемещения кубика
                                            }
                                        }
                                    }
                                    GamblesAnimation.animateTurn(mMovingDicePosition, position, winIndices, diceMoved,
                                            mCellsDrawable, mGamblesView, mDiceField.getGAListener()); //воспроизвести анимацию
                                    if(!diceMoved){
                                    /*если перемещения кубика не было, сбросить информацию о ходе,
                                    т.к. в случае перемещения информация сбрасывается слушателем анимации*/
                                        GamblesAnimation.animateDiceReleasing(mDownPosition, mCellsDrawable, mGamblesView,
                                                mDiceField.getGAListener());    //анимация отжатия кубика
                                        resetMoveInfo();
                                    }
                                    invalidate();
                                } else {
                                    selectDice(position);    //сменить перемещаемый кубик
                                }
                            } else {
                                selectDice(position);    //1 фаза хода, выбор кубика для перемещения
                            }
                        } else {
                            animateUnusualDiceReleasing();     //анимация отжатия кубика
                        }
                    } else {
                        //долгое нажатие позволяет изменить значение кубика
                        if(verifyPosition(position) && Arrays.equals(mDownPosition, position)){
                            if(mDiceField.changeDice(position)){    //меняем случайным образом значение кубика
                                mPrevScore = mScore; //обновить сведения об очках на случай отмены хода
                                List<int[]> winIndices = mDiceField.checkField();   //проверить совпадения кубиков
                                mScoreGained = mDiceField.removeWinIndices(winIndices, position, true, false);   //обновить очки
                                mScore += mScoreGained;
                                saveBestScore();    //обновить сведения о рекорде очков
                                if(soundIsOn){
                                    if(mScoreGained > 0){
                                        mDeleteSound.start();    //звук исчезновения кубиков
                                    } else {
                                        mChangeSound.start();    //звук изменения кубика
                                    }
                                }
                                GamblesAnimation.animateTurn(position, winIndices, mCellsDrawable, mGamblesView,
                                        mDiceField.getGAListener());    //воспроизвести анимацию
                                GamblesAnimation.animateDiceReleasing(mDownPosition, mCellsDrawable, mGamblesView,
                                        mDiceField.getGAListener());    //анимация отжатия кубика
                                //убрать подсветку клеток
                                mDiceField.prepareCellsToHighlight(mCellsToHighlight, position, true);
                                resetMoveInfo();    //сброс сведений о перемещении
                                invalidate();
                                if(!soundIsOn){
                                    mVibrator.vibrate(sVibrationLength);    //вибросигнал об успешном изменении кубика
                                }
                            }
                        } else {
                            animateUnusualDiceReleasing();    //анимация отжатия кубика
                        }
                    }
                    return true;
            }
            return true;
        }

        /**
         * Выбирает кубик для его дальнейшего перемещения
         * @param position позиция выбираемого кубика
         */
        private void selectDice(int[] position){
            mMovingDicePosition = position;
            //определить номера клеток, которые нужно подсветить
            mCellsToHighlight = mDiceField.calculateCellsToHighlight(position);
            //задать рисунок для подсвечиваемых клеток
            mDiceField.prepareCellsToHighlight(mCellsToHighlight, position, false);
            GamblesAnimation.animateDiceReleasing(mDownPosition, mCellsDrawable, mGamblesView,
                    mDiceField.getGAListener());    //анимация отжатия кубика
            if(mCellsToHighlight.isEmpty()){    //если ходить некуда, сбросить сведения о ходе
                mDiceMoveTriggered = false;
                resetMoveInfo();
            } else {
                mDiceMoveTriggered = true;
            }
            invalidate();
        }

        /**
         * Определяет номер клетки поля по ее координатам
         * @param x координата x
         * @param y координата y
         * @return позицию клетки на игровом поле
         */
        private int[] findCellPosition(float x, float y){
            int[] position = {-1, -1};
            float relativeX = x - startX;
            float relativeY = y - startY;
            if(relativeX > 0 && relativeY > 0){
                position[0] = (int) (relativeY / (diceSize + paddingDivider));
                position[1] = (int) (relativeX / (diceSize + paddingDivider));
            }
            return position;
        }

        /**
         * Воспроизводит анимацию отжатия кубика, если было выполнено
         * нажатие на кубик, затем палец был перемещен и отпущен на другой клетке.
         */
        private void animateUnusualDiceReleasing(){
            if(verifyPosition(mDownPosition)){
                if(mDiceField.getDices()[mDownPosition[0]][mDownPosition[1]] != 0){
                    GamblesAnimation.animateDiceReleasing(mDownPosition, mCellsDrawable, mGamblesView,
                            mDiceField.getGAListener());
                }
            }
        }

        /**
         * Проверяет корректность позиции клетки на поле
         * @param position номера строки и столбца
         * @return true, если индексы находяттся в допустимых пределах
         */
        private boolean verifyPosition(int[] position){
            return (position[0] >= 0 && position[0] < DiceField.FIELD_SIZE) &&
                    (position[1] >= 0 && position[1] < DiceField.FIELD_SIZE) &&
                    (!mDiceField.isBlockedCellMode() ||
                            (mDiceField.isBlockedCellMode() && !Arrays.equals(position, DiceField.BLOCKED_CELL)));
        }

        /**
         * Используется для настройки правильного порядка прорисовки клеток
         * при анимации движения кубика справа налево или снизу вверх
         * (чтобы кубик перемещался поверх пустых клеток, а не под ними)
         * @param startPos начальная позиция кубика
         * @param endPos конечная позиция
         */
        private void adjustDrawingOrder(int[] startPos, int[] endPos){
            //если конечная позиция расположена после начальной (т.е. правее или ниже)
            if((endPos[0] * DiceField.FIELD_SIZE + endPos[1]) <
                    (startPos[0] * DiceField.FIELD_SIZE + startPos[1])){
                drawingOrder.add(new int[]{endPos[0], endPos[1]});
                drawingOrder.add(new int[]{startPos[0], startPos[1]});
            }
        }
    }
}
