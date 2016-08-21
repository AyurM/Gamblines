/**
 * Используется для интерполяции свойства "bounds" при анимации клеток. Аюр М., 20.07.2016.
 */

package com.ayur.gambles.utils;

import android.animation.TypeEvaluator;
import android.graphics.Rect;

class BoundEvaluator implements TypeEvaluator<Rect> {
    private Rect mRect;

    public BoundEvaluator() {
    }

    public BoundEvaluator(Rect reuseRect) {
        mRect = reuseRect;
    }

    @Override
    public Rect evaluate(float fraction, Rect startValue, Rect endValue){
        int left = startValue.left + (int) ((endValue.left - startValue.left) * fraction);
        int top = startValue.top + (int) ((endValue.top - startValue.top) * fraction);
        int right = startValue.right + (int) ((endValue.right - startValue.right) * fraction);
        int bottom = startValue.bottom + (int) ((endValue.bottom - startValue.bottom) * fraction);
        if (mRect == null) {
            return new Rect(left, top, right, bottom);
        } else {
            mRect.set(left, top, right, bottom);
            return mRect;
        }
    }
}
