/**
 * Игровая механика. Аюр М., 05.07.2016.
 */

package com.ayur.gambles;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.app.FragmentManager;
import android.widget.Toast;

import com.ayur.gambles.dialogs.GameOverFragment;
import com.ayur.gambles.utils.GamblesAnimListener;
import com.ayur.gambles.utils.GamblesAnimation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DiceField{
    public static final int FIELD_SIZE = 5;             //размер игрового поля
    public static final int[] BLOCKED_CELL = {2, 2};    //позиция блокированной клетки для усложненного поля
    private static final int sDicesAtStart = 5;         //количество ненулевых кубиков на старте
    private static final int sDicesToCollect = 3;       //минимальная длина группы одинаковых кубиков
    private static final int sMinDiceValue = 1;         //минимальное значение кубика
    private static final int sMaxDiceValue = 6;         //максимальное значение кубика
    private static final int sHardFieldMultiplier = 2;         //множитель очков для сложного игрового поля
    private static final String sGameOverDialog = "GAME_OVER";

    private final int[][] mDices = new int[FIELD_SIZE][FIELD_SIZE];        //игровое поле
    private final int[][] mUndoDices = new int[FIELD_SIZE][FIELD_SIZE];    //копия игрового поля для отмены хода
    private final ArrayList<int[]> mFreeCells = new ArrayList<>();         //координаты пустых клеток игрового поля
    private final ArrayList<int[]> mUndoFreeCells = new ArrayList<>();     //координаты пустых клеток игрового поля
    private final Drawable[][] mCellsDrawables = new Drawable[FIELD_SIZE][FIELD_SIZE]; //рисунки для клеток игрового поля
    private int mDifficulty = 0;          //сложность игры (0-2)
    private boolean mBlockedCellMode = false;    //поле повышенной сложности вкл/выкл
    private int mScoreMultiplier = 1;     //текущий множитель зарабатываемых очков (1 - для обычного поля, sHardFieldMultiplier - для сложного)
    private GamblesView mGView = null;    //ссылка на главную вьюшку
    private GamblesAnimListener mGAListener = null;

    public DiceField(){
        resetField();
    }

    /**
     * Генерирует случайные координаты клетки из списка свободных клеток.
     * Полученные координаты удаляются из списка.
     * @return координаты случайной свободной клетки
     */
    private int[] generateRandomCoordinates(){
        if(mFreeCells.size() == 0){
            return new int[]{-1, -1};
        }
        int x = (int)(Math.random() * mFreeCells.size());
        return mFreeCells.remove(x);
    }

    /**
     * Убирает номер заданной клетки из списка свободных клеток
     * @param index номер строки и столбца
     */
    private void removeCoordinatesFromFreeCells(int[] index){
        if(!mFreeCells.isEmpty()){
            for(int i = 0; i < mFreeCells.size(); i++){
                int[] currIdx = mFreeCells.get(i);
                if(Arrays.equals(currIdx, index)){
                    mFreeCells.remove(i);
                    return;
                }
            }
        }
    }

    /**
     * Присваивает кубику случайное значение
     * @param position позиция изменяемого кубика
     */
    private void randomize(int[] position){
        mDices[position[0]][position[1]] = sMinDiceValue +
                (int)(Math.random() * ((sMaxDiceValue - sMinDiceValue) + 1));
    }

    /**
     * Изменяет значение кубика на указанной позиции
     * @param position искомые строка и столбец
     * @return true, если значение было изменено
     */
    public boolean changeDice(int[] position){
        int dice = mDices[position[0]][position[1]];
        if(dice != 0){
            refreshUndoDices();     //обновить сведения для отмены хода
            randomize(position);    //изменить значение кубика случайным образом
            mCellsDrawables[position[0]][position[1]] = findDiceDrawable(mDices[position[0]][position[1]],
                    position[0], position[1]);    //обновить рисунок кубика
            return true;
        }
        return false;
    }

    /**
     * Определяет позиции клеток, на которые может быть перемещен кубик
     * @param position исходная позиция перемещаемого кубика
     * @return список доступных для хода клеток
     */
    public List<int[]> calculateCellsToHighlight(int[] position) {
        int value = mDices[position[0]][position[1]];    //область хода определяется текущим значением кубика
        List<int[]> indices = new ArrayList<>();

        if(value == 0){
            return indices;
        }
        //сверху от клетки
        for(int i = -1; i >= -value; i--){
            int[] xy1 = {i + position[0], position[1]};
            if(xy1[0] >= 0 && xy1[0] < FIELD_SIZE){
                if(mDices[xy1[0]][xy1[1]] == 0 &&
                        (!mBlockedCellMode || (mBlockedCellMode && !Arrays.equals(xy1, BLOCKED_CELL)))){
                    indices.add(xy1);
                } else {
                    break;	//ненулевой кубик блокирует дальнейшее перемещение
                }
            }
        }
        //слева от клетки
        for(int i = -1; i >= -value; i--){
            int[] xy2 = {position[0], i + position[1]};
            if(xy2[1] >= 0 && xy2[1] < FIELD_SIZE){
                if(mDices[xy2[0]][xy2[1]] == 0 &&
                        (!mBlockedCellMode || (mBlockedCellMode && !Arrays.equals(xy2, BLOCKED_CELL)))){
                    indices.add(xy2);
                } else {
                    break;	//ненулевой кубик блокирует дальнейшее перемещение
                }
            }
        }
        //снизу от клетки
        for(int i = 1; i <= value; i++){
            int[] xy1 = {i + position[0], position[1]};
            if(xy1[0] >= 0 && xy1[0] < FIELD_SIZE){
                if(mDices[xy1[0]][xy1[1]] == 0 &&
                        (!mBlockedCellMode || (mBlockedCellMode && !Arrays.equals(xy1, BLOCKED_CELL)))){
                    indices.add(xy1);
                } else {
                    break;	//ненулевой кубик блокирует дальнейшее перемещение
                }
            }
        }
        //справа от клетки
        for(int i = 1; i <= value; i++){
            int[] xy2 = {position[0], i + position[1]};
            if(xy2[1] >= 0 && xy2[1] < FIELD_SIZE){
                if(mDices[xy2[0]][xy2[1]] == 0 &&
                        (!mBlockedCellMode || (mBlockedCellMode && !Arrays.equals(xy2, BLOCKED_CELL)))){
                    indices.add(xy2);
                } else {
                    break;	//ненулевой кубик блокирует дальнейшее перемещение
                }
            }
        }
        return indices;
    }

    /**
     * Перемещает кубик на указанную позицию
     * @param startPos исходная позиция
     * @param endPos выбранная позиция
     * @param validCells список доступных для хода клеток
     * @param context контекст для тоаста
     * @return true, если кубик был перемещен
     */
    public boolean moveDice(int[] startPos, int[] endPos, List<int[]> validCells, Context context){
        boolean endPosIsFree = false;
        //на клик по одной и той же клетке реакция отсутствует
        if(Arrays.equals(startPos, endPos)){
            return false;
        }
        /*проверка правильности конечной позиции кубика
        (перемещать кубик можно только на разрешенные, т.е. подсвеченные клетки)*/
        for(int[] currPos : validCells){
            if(Arrays.equals(currPos, endPos)){
                endPosIsFree = true;
                break;
            }
        }

        int tempValue = mDices[startPos[0]][startPos[1]];
        //перемещать следует только ненулевые кубики
        if(tempValue != 0){
            //перемещение кубика в случае корректного хода
            if(endPosIsFree){
                refreshUndoDices();    //обновить сведения для отмены хода
                //обнулить исходную позицию
                mDices[startPos[0]][startPos[1]] = 0;
                mCellsDrawables[startPos[0]][startPos[1]] = findDiceDrawable(0, startPos[0], startPos[1]);
                //обновить конечную позицию
                mDices[endPos[0]][endPos[1]] = tempValue;
                mCellsDrawables[endPos[0]][endPos[1]] = findDiceDrawable(tempValue, endPos[0], endPos[1]);
                //обновить список свободных клеток
                mFreeCells.add(new int[]{startPos[0], startPos[1]});    //копируем значение
                removeCoordinatesFromFreeCells(endPos);
            } else {
                //сообщение об ошибке в случае неправильного хода
                String wrongTurn = context.getResources().getString(R.string.wrong_turn);
                Toast toast = Toast.makeText(context, wrongTurn, Toast.LENGTH_SHORT);
                toast.show();
            }
        }
        return endPosIsFree;
    }

    /**
     * Создает случайным образом новые кубики на поле
     * @param value значение кубика
     * @param afterMove true, если обработка выполняется после перемещения кубика; false - если
     *                  после изменения кубика
     */
    private void generateNewDice(int value, boolean afterMove){
        if(value == 0){
            return;
        }
        //количество появляющихся кубиков зависит от текущей сложности игры
        int dicesToSpawn;
        switch (mDifficulty){
            case 1:
                //на нормальной сложности: 2 кубика после перемещения, 1 после изменения
                dicesToSpawn = afterMove ? 2 : 1;
                break;
            case 2:
                dicesToSpawn = 2;   //на тяжелой: всегда появляются 2 кубика
                break;
            default:
                dicesToSpawn = 1;   //на легкой: всегда появляется 1 кубик
        }
        //создать нужное кол-во кубиков
        for(int i = 0; i < dicesToSpawn; i++){
            int[] newIndices = generateRandomCoordinates();  //получить случайные координаты для создания кубика
            if(newIndices[0] != -1){
                randomize(newIndices);   //задать случайное значение для кубика
                mCellsDrawables[newIndices[0]][newIndices[1]] = findDiceDrawable(mDices[newIndices[0]][newIndices[1]], newIndices[0], newIndices[1]); //обновить рисунок кубика
                GamblesAnimation.animateDiceSpawning(newIndices, mCellsDrawables, mGView,
                        mGAListener);    //анимация появления кубика
                if(mFreeCells.isEmpty()){
                    return;    //прекратить генерацию новых кубиков, если на поле не осталось места
                }
            }
        }
    }

    /**
     * Выполняет поиск совпадающих кубиков на игровом поле
     * @return список найденных совпадений
     */
    public List<int[]> checkField(){
        List<int[]> winIndices = new ArrayList<>();
        //просмотреть игровое поле в поисках совпадений
        for(int i = 0; i < FIELD_SIZE; i++){
            for(int j = 0; j < FIELD_SIZE; j++){
                List<int[]> currIndices;
                currIndices = checkDice(mDices[i][j], i, j);    //выполнить поиск совпадений для данной клетки
                //если совпадения найдены, добавить их в возвращаемый список
                if(!winIndices.isEmpty()){    //если на данном ходу уже были найдены совпавшие клетки
                    for(int[] idxToAdd : currIndices){
                        boolean newIdx = true;
                        for(int[] existingIdx : winIndices){
                            if(Arrays.equals(idxToAdd, existingIdx)){    //убедиться в отсутствии повторяющихся клеток
                                newIdx = false;
                                break;
                            }
                        }
                        if(newIdx){
                            winIndices.add(idxToAdd);
                        }
                    }
                } else {
                    winIndices.addAll(currIndices);    //если других совпавших клеток нет
                }
            }
        }
        return winIndices;
    }

    /**
     * Выполняет поиск совпадающих кубиков для данной клетки
     * @param dice текущая клетка
     * @param x номер строки для данной клетки
     * @param y номер столбца для данной клетки
     * @return список позиций кубиков, совпадающих с текущим (необходимо превысить порог
     * sDicesToCollect)
     */
    private List<int[]> checkDice(int dice, int x, int y){
        List<int[]> winIndices = new ArrayList<>();
        //рассматриваются только ненулевые кубики
        if(dice != 0){
            int dicesVertical = 1, dicesHorizontal = 1;
            //поиск совпадающих кубиков в вертикальном направлении (вниз)
            for(int i = 1; i < FIELD_SIZE - x; i++){
                try{
                    if(mDices[x + i][y] == dice){
                        dicesVertical++;
                    } else {
                        break;
                    }
                } catch (IndexOutOfBoundsException exc){
                    break;
                }
            }
            //поиск совпадающих кубиков в горизонтальном направлении (вправо)
            for(int i = 1; i < FIELD_SIZE - y; i++){
                try{
                    if(mDices[x][y + i] == dice){
                        dicesHorizontal++;
                    } else {
                        break;
                    }
                } catch (IndexOutOfBoundsException exc){
                    break;
                }
            }
            //обработка результатов поиска
            if(dicesVertical >= sDicesToCollect){
                for(int i = 0; i < dicesVertical; i++){
                    int[] currIdx = {x + i, y};
                    winIndices.add(currIdx);
                }
            }
            if(dicesHorizontal >= sDicesToCollect){
                //не добавлять стартовую клетку еще раз, если есть совпадения в вертикальном направлении
                for(int i = dicesVertical >= sDicesToCollect ? 1 : 0 ; i < dicesHorizontal; i++){
                    int[] currIdx = {x, y + i};
                    winIndices.add(currIdx);
                }
            }
        }
        return winIndices;
    }

    /**
     * Убирает совпавшие кубики с поля, если таковые имеются
     * @param winIndices позиции совпавших кубиков (совпавших кубиков может не быть)
     * @param newPosition конечная позиция хода
     * @param diceMoved true, если положение/значение кубиков на поле действительно изменились
     * @param afterMove true, если обработка выполняется после перемещения кубика; false - если
     *                  после изменения кубика
     * @return количество очков, заработанных на данном ходу
     */
    public int removeWinIndices(List<int[]> winIndices, int[] newPosition, boolean diceMoved, boolean afterMove){
        int score = 0;
        //после хода не было найдено совпадающих кубиков
        if(winIndices.isEmpty()){
            //новый кубик добавляется, только если было изменение на поле
            if(diceMoved){
                generateNewDice(mDices[newPosition[0]][newPosition[1]], afterMove); //добавить на поле новый кубик
                /*последовательность совпадающих кубиков может появиться и после хода,
                в результате появления нового кубика*/
                List<int[]> secondIdxCheck = checkField();    //после добавления кубика проверить совпадения еще раз
                if(!secondIdxCheck.isEmpty()){
                    score = removeWinIndices(secondIdxCheck, newPosition, false, afterMove); //убрать совпавшие кубики
                    winIndices.addAll(secondIdxCheck);    //обновить сведения о кубиках, подлежащих удалению
                }
                checkGameOver();    //проверка условия гейм-овера
            }
            return score;
        }
        //совпадающие кубики были найдены
        for(int i = 0; i < winIndices.size(); i++){
            int[] currIdx = winIndices.get(i);
            score += mDices[currIdx[0]][currIdx[1]] * mScoreMultiplier;    //подсчитать очки
            mDices[currIdx[0]][currIdx[1]] = 0;         //убрать совпадающие кубики
            mFreeCells.add(currIdx);                    //обновить список свободных клеток
        }
        //нельзя допустить, чтобы на поле не осталось ни одного кубика
        if(mFreeCells.size() == FIELD_SIZE * FIELD_SIZE){
            addStartingDices();        //добавить ненулевые кубики
            prepareDiceDrawables();    //обновить рисунки клеток
        }
        return score;
    }

    /**
     * Создает поле для новой игры
     */
    public void resetField(){
        mFreeCells.clear();
        //Создаем новое поле, заполненное нулями
        for(int i = 0; i < FIELD_SIZE; i++){
            for(int j = 0; j < FIELD_SIZE; j++){
                int[] indices = {i, j};
                if(mBlockedCellMode){    //для поля повышенной сложности
                    if(!(i == BLOCKED_CELL[0] && j == BLOCKED_CELL[1])){
                        mDices[i][j] = 0;
                        mUndoDices[i][j] = 0;
                        mFreeCells.add(indices);    //вносим сведения о координатах пустых клеток
                    }
                } else {
                    mDices[i][j] = 0;
                    mUndoDices[i][j] = 0;
                    mFreeCells.add(indices);    //вносим сведения о координатах пустых клеток
                }
            }
        }
        addStartingDices();    //добавляем стартовые ненулевые кубики
        refreshUndoDices();    //обновляем бэкап-поле
    }

    /**
     * Обновляет поле, сохраняющее предыдущее состояние на случай отмены хода
     */
    private void refreshUndoDices(){
        //Копируем значения кубиков из основного игрового поля
        for(int i = 0; i < FIELD_SIZE; i++){
            if(mBlockedCellMode){    //для поля повышенной сложности
                if(i != BLOCKED_CELL[0]){
                    System.arraycopy(mDices[i], 0, mUndoDices[i], 0, FIELD_SIZE);
                } else {
                    for(int j = 0; j < FIELD_SIZE; j++){
                        if(j != BLOCKED_CELL[1]){
                            mUndoDices[i][j] = mDices[i][j];
                        } else {
                            mUndoDices[i][j] = 0;
                        }
                    }
                }
            } else {    //обычное поле
                System.arraycopy(mDices[i], 0, mUndoDices[i], 0, FIELD_SIZE);
            }
        }
        //Копируем список свободных клеток
        mUndoFreeCells.clear();
        for(int i = 0; i < mFreeCells.size(); i++){
            int[] tempIdx = mFreeCells.get(i);
            int[] currIdx = {tempIdx[0], tempIdx[1]};
            mUndoFreeCells.add(currIdx);
        }
    }

    /**
     * Отменяет последний ход
     */
    public void undoTurn(){
        //Копируем значения кубиков из запасного игрового поля
        for(int i = 0; i < FIELD_SIZE; i++){
            if(mBlockedCellMode){    //для поля повышенной сложности
                if(i != BLOCKED_CELL[0]){
                    System.arraycopy(mUndoDices[i], 0, mDices[i], 0, FIELD_SIZE);
                } else {
                    for(int j = 0; j < FIELD_SIZE; j++){
                        if(j != BLOCKED_CELL[1]){
                            mDices[i][j] = mUndoDices[i][j];
                        } else {
                            mDices[i][j] = 0;
                        }
                    }
                }
            } else {    //обычное поле
                System.arraycopy(mUndoDices[i], 0, mDices[i], 0, FIELD_SIZE);
            }
        }
        prepareDiceDrawables();    //обновить рисунки клеток
        //Копируем список свободных клеток
        mFreeCells.clear();
        for(int i = 0; i < mUndoFreeCells.size(); i++){
            int[] tempIdx = mUndoFreeCells.get(i);
            int[] currIdx = {tempIdx[0], tempIdx[1]};
            mFreeCells.add(currIdx);
        }
    }

    /**
     * Воссоздает игровое поле по строке из файла сохранения
     * @param savedData сохраненная строка
     * @return игровое поле
     */
    public int[][] loadFieldFromString(String savedData){
        int[][] result = new int[FIELD_SIZE][FIELD_SIZE];
        for(int i = 0; i < FIELD_SIZE; i++){
            for(int j = 0 ; j < FIELD_SIZE; j++){
                if(mBlockedCellMode){    //поле повышенной сложности
                    if(!(i == BLOCKED_CELL[0] && j == BLOCKED_CELL[1])){
                        String s = "" + savedData.charAt(i * FIELD_SIZE + j);
                        result[i][j] = Integer.parseInt(s);
                    } else {
                        result[i][j] = 0;
                    }
                } else {    //обычное поле
                    String s = "" + savedData.charAt(i * FIELD_SIZE + j);
                    result[i][j] = Integer.parseInt(s);
                }
            }
        }
        return result;
    }

    /**
     * Копирует переданное игровое поле
     * @param dices игровое поле, которое нужно воссоздать
     */
    public void setDices(int[][] dices){
        mFreeCells.clear(); //очистить список свободных клеток
        for(int i = 0; i < FIELD_SIZE; i++){
            for(int j = 0 ; j < FIELD_SIZE; j++){
                //обновить клетки поля
                mDices[i][j] = dices[i][j];
                int[] currIdx = {i, j};
                //обновить список свободных клеток
                if(mDices[i][j] == 0 && (!mBlockedCellMode ||
                        (mBlockedCellMode && !Arrays.equals(currIdx, BLOCKED_CELL)) )){
                    mFreeCells.add(currIdx);
                }
            }
        }
        refreshUndoDices();
    }

    /**
     * Задает фоновые рисунки для всех клеток поля в зависимости
     * от их текущих значений
     */
    public void prepareDiceDrawables(){
        for(int i = 0; i < FIELD_SIZE; i++){
            for(int j = 0 ; j < FIELD_SIZE; j++){
                if(mBlockedCellMode){    //поле повышенной сложности
                    if(!(i == BLOCKED_CELL[0] && j == BLOCKED_CELL[1])){
                        //подобрать рисунок для текущей клетки
                        mCellsDrawables[i][j] = findDiceDrawable(mDices[i][j], i, j);
                    }
                } else {    //обычное поле
                    //подобрать рисунок для текущей клетки
                    mCellsDrawables[i][j] = findDiceDrawable(mDices[i][j], i, j);
                }
            }
        }
    }

    /**
     * Обновляет фоновые рисунки для подсвечиваемых клеток поля
     * @param indices список клеток, для которых нужно включить/снять подсветку
     * @param position клетка, на которую был перемещен кубик, не обрабатывается
     * @param removeHighlight признак снятия/установки подсветки клеток
     */
    public void prepareCellsToHighlight(List<int[]> indices, int[] position, boolean removeHighlight){
        if(indices.isEmpty()){
            return;
        }
        for(int i = 0; i < indices.size(); i++){
            int[] currentIndex = indices.get(i);
            if(!removeHighlight){
                //рисунок для подсвечиваемой клетки
                mCellsDrawables[currentIndex[0]][currentIndex[1]] = findDiceDrawable(7,
                        currentIndex[0], currentIndex[1]);
            } else {
                //снять подсветку для требуемых клеток
                if((currentIndex[0] != position[0] || currentIndex[1] != position[1]) &&
                        mDices[currentIndex[0]][currentIndex[1]] == 0){
                    mCellsDrawables[currentIndex[0]][currentIndex[1]] = findDiceDrawable(0,
                            currentIndex[0], currentIndex[1]);
                }
            }
        }
    }

    /**
     * Подбирает подходящий фоновый рисунок для клетки
     * @param value текущее значение клетки (от 0 до 7)
     * @param x номер строки на поле
     * @param y номер столбца на поле
     * @return рисунок для клетки
     */
    private Drawable findDiceDrawable(int value, int x, int y){
        Drawable drawable = new BitmapDrawable(mGView.getResources(), mGView.diceBitmaps[value]);
        setDrawableBounds(drawable, x , y);
        return drawable;
    }

    /**
     * Задает область прорисовки для фонового рисунка клетки
     * @param drawable фоновый рисунок
     * @param x номер строки на поле
     * @param y номер столбца на поле
     */
    private void setDrawableBounds(Drawable drawable, int x, int y){
        drawable.setBounds(mGView.startX + (y+1) * mGView.paddingDivider + y * mGView.diceSize,
                mGView.startY + (x+1) * mGView.paddingDivider + x * mGView.diceSize,
                mGView.startX + (y+1) * mGView.paddingDivider + (y+1) * mGView.diceSize,
                mGView.startY + (x+1) * mGView.paddingDivider + (x+1) * mGView.diceSize);
    }

    /**
     * Добавляет sDicesAtStart начальных кубиков на поле
     */
    private void addStartingDices(){
        //Случайным образом размещаем стартовые кубики
        for(int i = 0; i < sDicesAtStart; i++){
            int[] indices = generateRandomCoordinates();    //генерация случайных координат клетки
            randomize(indices);    //задаем случайное стартовое значение для кубика
        }
    }

    /**
     * Завершает игру, если на поле не осталось свободных клеток
     */
    private void checkGameOver(){
        if(mFreeCells.isEmpty()){
            FragmentManager manager = ((GamblesActivity) mGView.getContext()).getSupportFragmentManager();
            GameOverFragment dialog = new GameOverFragment();    //диалоговое окно об окончании игры
            dialog.show(manager, sGameOverDialog);
        }
    }

    public Drawable[][] getCellsDrawables() {
        return mCellsDrawables;
    }

    public GamblesAnimListener getGAListener() {
        return mGAListener;
    }

    public int[][] getDices() {
        return mDices;
    }

    public void setGView(GamblesView GView) {
        mGView = GView;
        mGAListener = new GamblesAnimListener(mGView);
    }

    public int getDifficulty() {
        return mDifficulty;
    }

    public void setDifficulty(int difficulty) {
        mDifficulty = difficulty;
    }

    public boolean isBlockedCellMode() {
        return mBlockedCellMode;
    }

    public void setBlockedCellMode(boolean blockedCellMode) {
        mBlockedCellMode = blockedCellMode;
        mScoreMultiplier = blockedCellMode ? sHardFieldMultiplier : 1;  //изменить множитель очков
    }
}
