/**
 * Содержит методы для анимации. Аюр М., 26.07.2016.
 */
package com.ayur.gambles.utils;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import com.ayur.gambles.GamblesView;
import com.ayur.gambles.utils.BoundEvaluator;
import com.ayur.gambles.utils.GamblesAnimListener;

import java.util.Arrays;
import java.util.List;

public class GamblesAnimation {
    private static final int sAnimDurationShort = 90;   //длительность анимации, мс
    private static final int sAnimDurationLong = 125;

    /**
     * Воспроизводит анимацию перемещения и удаления кубиков
     * @param startPos исходная позиция перемещенного кубика
     * @param endPos конечная позиция перемещенного кубика
     * @param winIndices позиции кубиков, удаленных в результате успешного хода
     * @param diceMoved признак фактического изменения состояния кубиков на поле
     * @param cellsDrawables ссылка на массив рисунков для клеток
     * @param gamblesView ссылка на главную вьюшку
     * @param gamblesAnimListener ссылка на слушатель событий анимации
     */
    public static void animateTurn(int[] startPos, int[] endPos, List<int[]> winIndices, boolean diceMoved,
                                   Drawable[][] cellsDrawables, GamblesView gamblesView,
                                   GamblesAnimListener gamblesAnimListener){
        //если кубики не менялись, то ничего делать не нужно
        if(!diceMoved){
            return;
        }

        ObjectAnimator moveAnimation;
        AnimatorSet turnAnimation = new AnimatorSet();
        AnimatorSet removeAnimSet = new AnimatorSet();
        //если на данном ходу были убраны какие-то кубики
        if(!winIndices.isEmpty()){
            animateDiceDeleting(removeAnimSet, winIndices, cellsDrawables,
                    gamblesView, gamblesAnimListener); //создать анимацию удаления кубиков
        }
        //если на данном ходу произошло перемещение кубика
        if(!Arrays.equals(startPos, endPos)){
            moveAnimation = animateDiceMoving(startPos, endPos, cellsDrawables,
                    gamblesAnimListener);    //создать анимацию перемещения кубика
            //если на данном ходу были убраны какие-то кубики
            if(!winIndices.isEmpty()){
                turnAnimation.playSequentially(moveAnimation, removeAnimSet);    //последовательное воспроизведение
            } else {
                moveAnimation.addListener(gamblesAnimListener);
                turnAnimation.play(moveAnimation);
            }
            turnAnimation.start();  //начать воспроизведение анимации
        }
    }

    /**
     * Воспроизводит анимацию изменения и удаления кубиков
     * @param position позиция измененного кубика
     * @param winIndices позиции кубиков, удаленных в результате успешного хода
     * @param cellsDrawables ссылка на массив рисунков для клеток
     * @param gamblesView ссылка на главную вьюшку
     * @param gamblesAnimListener ссылка на слушатель событий анимации
     */
    public static void animateTurn(int[] position, List<int[]> winIndices, Drawable[][] cellsDrawables,
                                   GamblesView gamblesView, GamblesAnimListener gamblesAnimListener){
        //создать анимацию изменения кубика
        ObjectAnimator changeAnimation = animateDiceChanging(position, cellsDrawables);
        AnimatorSet turnAnimation = new AnimatorSet();
        AnimatorSet removeAnimSet = new AnimatorSet();
        //если на данном ходу были убраны какие-то кубики
        if(!winIndices.isEmpty()){
            changeAnimation.setDuration(sAnimDurationLong); //уменьшить длительность анимации
            animateDiceDeleting(removeAnimSet, winIndices, cellsDrawables,
                    gamblesView, gamblesAnimListener); //создать анимацию удаления кубиков
            turnAnimation.playSequentially(changeAnimation, removeAnimSet);   //последовательное воспроизведение
        } else {
            //если воспроизводится только анимация изменения кубика
            changeAnimation.addUpdateListener(gamblesAnimListener);
            changeAnimation.addListener(gamblesAnimListener);
            turnAnimation.play(changeAnimation);
        }
        turnAnimation.start();  //начать воспроизведение анимации
    }

    /**
     * Создает анимацию удаления совпавших кубиков с поля
     * @param removeAnimSet набор анимаций
     * @param winIndices позиции кубиков, подлежащих удалению
     * @param cellsDrawables ссылка на массив рисунков для клеток
     * @param gamblesView ссылка на главную вьюшку
     * @param gamblesAnimListener ссылка на слушатель событий анимации
     */
    private static void animateDiceDeleting(AnimatorSet removeAnimSet, List<int[]> winIndices,
                                           Drawable[][] cellsDrawables, GamblesView gamblesView,
                                           GamblesAnimListener gamblesAnimListener){
        ObjectAnimator[] removeAnimation = new ObjectAnimator[winIndices.size()];
        double animCoefficient = 0.25;  //коэффициент уменьшения размера кубиков при анимации
        //создать анимацию для удаляемых кубиков
        for(int i = 0; i < winIndices.size(); i++){
            int[] currIdx = winIndices.get(i);
            //удаляемые кубики исчезают с поля, уменьшаясь в размерах
            Rect startBounds = cellsDrawables[currIdx[0]][currIdx[1]].copyBounds();
            Rect endBounds = new Rect((int)(startBounds.left + gamblesView.diceSize * animCoefficient),
                    (int)(startBounds.top + gamblesView.diceSize * animCoefficient),
                    (int)(startBounds.right - gamblesView.diceSize * animCoefficient),
                    (int)(startBounds.bottom - gamblesView.diceSize * animCoefficient));
            //анимация удаления кубиков
            removeAnimation[i] = ObjectAnimator.ofObject(cellsDrawables[currIdx[0]][currIdx[1]],
                    "bounds", new BoundEvaluator(), startBounds, endBounds);
            removeAnimation[i].setDuration(sAnimDurationLong);
            if(i == winIndices.size() - 1){
                //слушатель событий анимации достаточно прикрепить к последнему кубику
                removeAnimation[i].addUpdateListener(gamblesAnimListener);
                removeAnimation[i].addListener(gamblesAnimListener);
            }
        }
        removeAnimSet.playTogether(removeAnimation); //воспроизвести одновременно
    }

    /**
     * Создает анимацию перемещения кубика
     * @param startPos исходная позиция
     * @param endPos конечная позиция
     * @param cellsDrawables ссылка на массив рисунков для клеток
     * @param gamblesAnimListener ссылка на слушатель событий анимации
     * @return анимация, готовая к воспроизведению
     */
    private static ObjectAnimator animateDiceMoving(int[] startPos, int[] endPos, Drawable[][] cellsDrawables,
                                                   GamblesAnimListener gamblesAnimListener){
        //анимируемые значения - границы отрисовки
        Rect startBounds = cellsDrawables[startPos[0]][startPos[1]].copyBounds();
        Rect endBounds = cellsDrawables[endPos[0]][endPos[1]].copyBounds();
        //вычислить кол-во клеток, на которое был перемещен кубик
        int cellsToMove = (startPos[0] == endPos[0]) ? Math.abs((startPos[1] - endPos[1]))
                : Math.abs((startPos[0] - endPos[0]));
        //длительность анимации зависит от расстояния (кол-ва клеток), на которое нужно переместить кубик
        int animDuration = cellsToMove > 2 ? sAnimDurationLong : sAnimDurationShort;
        //анимация кубика
        ObjectAnimator moveAnimation = ObjectAnimator.ofObject(cellsDrawables[endPos[0]][endPos[1]],
                "bounds", new BoundEvaluator(), startBounds, endBounds); //анимация перемещения
        moveAnimation.setDuration(animDuration);
        moveAnimation.setInterpolator(new AccelerateInterpolator());    //интерполяция с ускорением
        moveAnimation.addUpdateListener(gamblesAnimListener);
        return moveAnimation;
    }

    /**
     * Создает и воспроизводит анимацию появления кубика на поле
     * @param newIndices позиция, на которой появляется кубик
     * @param cellsDrawables ссылка на массив рисунков для клеток
     * @param gamblesView ссылка на главную вьюшку
     * @param gamblesAnimListener ссылка на слушатель событий анимации
     */
    public static void animateDiceSpawning(int[] newIndices, Drawable[][] cellsDrawables,
                                           GamblesView gamblesView, GamblesAnimListener gamblesAnimListener){
        //появляющийся кубик "вырастает" на игровом поле (начальный размер
        //равен animCoefficient*2 от конечного)
        double animCoefficient = 0.25;
        //анимируемыми параметрами являются границы отрисовки
        Rect endBounds = cellsDrawables[newIndices[0]][newIndices[1]].copyBounds();
        Rect startBounds = new Rect((int)(endBounds.left + gamblesView.diceSize * animCoefficient),
                (int)(endBounds.top + gamblesView.diceSize * animCoefficient),
                (int)(endBounds.right - gamblesView.diceSize * animCoefficient),
                (int)(endBounds.bottom - gamblesView.diceSize * animCoefficient));
        //анимация появления кубика
        ObjectAnimator anim = ObjectAnimator.ofObject(cellsDrawables[newIndices[0]][newIndices[1]],
                "bounds", new BoundEvaluator(), startBounds, endBounds);
        anim.setDuration(sAnimDurationLong * 2);
        anim.setInterpolator(new OvershootInterpolator(4f));
        anim.addUpdateListener(gamblesAnimListener);
        anim.start();
    }

    /**
     * Создает анимацию изменения значения кубика
     * @param position позиция изменяемого кубика
     * @param cellsDrawables ссылка на массив рисунков для клеток
     * @return анимация, готовая к воспроизведению
     */
    private static ObjectAnimator animateDiceChanging(int[] position, Drawable[][] cellsDrawables){
        //анимация изменения значения кубика
        ObjectAnimator anim = ObjectAnimator.ofInt(cellsDrawables[position[0]][position[1]],
                "alpha", 0, 255); //анимация прозрачности
        anim.setDuration(sAnimDurationLong * 2);
        return anim;
    }

    /**
     * Создает и воспроизводит анимацию нажатия на кубик
     * @param position позиция анимируемого кубика
     * @param cellsDrawables ссылка на массив рисунков для клеток
     * @param gamblesView ссылка на главную вьюшку
     * @param gamblesAnimListener ссылка на слушатель событий анимации
     */
    public static void animateDicePressing(int[] position, Drawable[][] cellsDrawables, GamblesView gamblesView,
                                           GamblesAnimListener gamblesAnimListener){
        //при нажатии на кубик он немного уменьшается в размере
        double animCoefficient = 0.08;
        //анимируемыми параметрами являются границы отрисовки
        Rect startBounds = cellsDrawables[position[0]][position[1]].copyBounds();
        Rect endBounds = new Rect((int)(startBounds.left + gamblesView.diceSize * animCoefficient),
                (int)(startBounds.top + gamblesView.diceSize * animCoefficient),
                (int)(startBounds.right - gamblesView.diceSize * animCoefficient),
                (int)(startBounds.bottom - gamblesView.diceSize * animCoefficient));
        //анимация прикосновения к кубику
        ObjectAnimator pressAnimation = ObjectAnimator.ofObject(cellsDrawables[position[0]][position[1]],
                "bounds", new BoundEvaluator(), startBounds, endBounds);
        pressAnimation.setDuration(sAnimDurationShort);
        pressAnimation.addUpdateListener(gamblesAnimListener);
        pressAnimation.start();
    }

    /**
     * Создает и воспроизводит анимацию отжатия кубика
     * @param pos позиция анимируемого кубика
     * @param cellsDrawables ссылка на массив рисунков для клеток
     * @param gView ссылка на главную вьюшку
     * @param gamblesAnimListener ссылка на слушатель событий анимации
     */
    public static void animateDiceReleasing(int[] pos, Drawable[][] cellsDrawables, GamblesView gView,
                                            GamblesAnimListener gamblesAnimListener){
        //границы отрисовки кубика возвращаются к нормальному значению
        Rect endBounds = new Rect(gView.startX + (pos[1]+1) * gView.paddingDivider + pos[1] * gView.diceSize,
                gView.startY + (pos[0]+1) * gView.paddingDivider + pos[0] * gView.diceSize,
                gView.startX + (pos[1]+1) * gView.paddingDivider + (pos[1]+1) * gView.diceSize,
                gView.startY + (pos[0]+1) * gView.paddingDivider + (pos[0]+1) * gView.diceSize);
        ObjectAnimator releaseAnimation = ObjectAnimator.ofObject(cellsDrawables[pos[0]][pos[1]],
                "bounds", new BoundEvaluator(), endBounds);
        releaseAnimation.setDuration(sAnimDurationShort);
        releaseAnimation.addUpdateListener(gamblesAnimListener);
        releaseAnimation.start();
    }
}
