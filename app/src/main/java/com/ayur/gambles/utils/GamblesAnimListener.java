/**
 * Слушатель событий анимации. Аюр М., 13.07.2016.
 */
package com.ayur.gambles.utils;

import android.animation.Animator;
import android.animation.ValueAnimator;

import com.ayur.gambles.GamblesView;

public class GamblesAnimListener implements ValueAnimator.AnimatorUpdateListener,
        Animator.AnimatorListener{
    private final GamblesView mView;

    public GamblesAnimListener(GamblesView view){
        super();
        this.mView = view;
    }

    @Override
    public void onAnimationUpdate(ValueAnimator anim){
        mView.invalidate();
    }

    @Override
    public void onAnimationStart(Animator anim){
    }

    @Override
    public void onAnimationCancel(Animator anim){
    }

    @Override
    public void onAnimationEnd(Animator anim){
        mView.getDiceField().prepareDiceDrawables();    //обновить рисунки клеток после анимации
        mView.resetScoreGained();    //обнулить сведения о набранных на данному ходу очках
        mView.invalidate();
        mView.drawingOrder.clear(); //сбросить сведения о порядке отрисовки клеток
        mView.resetMoveInfo(); //сброс сведений о перемещении
        anim.removeAllListeners();
    }

    @Override
    public void onAnimationRepeat(Animator anim){
    }
}
