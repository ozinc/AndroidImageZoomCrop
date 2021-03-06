/*******************************************************************************
 * Copyright 2011, 2012 Chris Banes.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.oz.imagezoomcrop.imagecrop.photoview.gestures;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import com.oz.imagezoomcrop.imagecrop.photoview.Compat;

@TargetApi(8)
public class FroyoGestureDetector implements GestureDetector
{

  protected final ScaleGestureDetector mDetector;

  private static final int INVALID_POINTER_ID = -1;
  private int mActivePointerId = INVALID_POINTER_ID;
  private int mActivePointerIndex = 0;

  protected OnGestureListener mListener;
  private static final String LOG_TAG = "CupcakeGestureDetector";
  float mLastTouchX;
  float mLastTouchY;
  final float mTouchSlop;
  final float mMinimumVelocity;

  private VelocityTracker mVelocityTracker;
  private boolean mIsDragging;

  @Override
  public void setOnGestureListener(OnGestureListener listener)
  {
    this.mListener = listener;
  }

  public FroyoGestureDetector(Context context)
  {
    final ViewConfiguration configuration = ViewConfiguration
        .get(context);
    mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
    mTouchSlop = configuration.getScaledTouchSlop();

    ScaleGestureDetector.OnScaleGestureListener mScaleListener = new ScaleGestureDetector.OnScaleGestureListener()
    {

      @Override
      public boolean onScale(ScaleGestureDetector detector)
      {
        float scaleFactor = detector.getScaleFactor();

        if (Float.isNaN(scaleFactor) || Float.isInfinite(scaleFactor))
        {
          return false;
        }

        mListener.onScale(scaleFactor,
            detector.getFocusX(), detector.getFocusY());
        return true;
      }

      @Override
      public boolean onScaleBegin(ScaleGestureDetector detector)
      {
        return true;
      }

      @Override
      public void onScaleEnd(ScaleGestureDetector detector)
      {
        // NO-OP
      }
    };
    mDetector = new ScaleGestureDetector(context, mScaleListener);
  }

  float getActiveX(MotionEvent ev)
  {
    try
    {
      return ev.getX(mActivePointerIndex);
    }
    catch (Exception e)
    {
      return ev.getX();
    }
  }

  float getActiveY(MotionEvent ev)
  {
    try
    {
      return ev.getY(mActivePointerIndex);
    }
    catch (Exception e)
    {
      return ev.getY();
    }
  }

  @Override
  public boolean isScaling()
  {
    return mDetector.isInProgress();
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev)
  {
    mDetector.onTouchEvent(ev);

    final int action = ev.getAction();
    switch (action & MotionEvent.ACTION_MASK)
    {
      case MotionEvent.ACTION_DOWN:
        mActivePointerId = ev.getPointerId(0);
        break;
      case MotionEvent.ACTION_CANCEL:
      case MotionEvent.ACTION_UP:
        mActivePointerId = INVALID_POINTER_ID;
        break;
      case MotionEvent.ACTION_POINTER_UP:
        // Ignore deprecation, ACTION_POINTER_ID_MASK and
        // ACTION_POINTER_ID_SHIFT has same value and are deprecated
        // You can have either deprecation or lint target api warning
        final int pointerIndex = Compat.getPointerIndex(ev.getAction());
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId)
        {
          // This was our active pointer going up. Choose a new
          // active pointer and adjust accordingly.
          final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
          mActivePointerId = ev.getPointerId(newPointerIndex);
          mLastTouchX = ev.getX(newPointerIndex);
          mLastTouchY = ev.getY(newPointerIndex);
        }
        break;
    }

    mActivePointerIndex = ev
        .findPointerIndex(mActivePointerId != INVALID_POINTER_ID ? mActivePointerId
            : 0);

    switch (ev.getAction())
    {
      case MotionEvent.ACTION_DOWN:
      {
        mVelocityTracker = VelocityTracker.obtain();
        if (null != mVelocityTracker)
        {
          mVelocityTracker.addMovement(ev);
        }
        else
        {
          Log.i(LOG_TAG, "Velocity tracker is null");
        }

        mLastTouchX = getActiveX(ev);
        mLastTouchY = getActiveY(ev);
        mIsDragging = false;
        break;
      }

      case MotionEvent.ACTION_MOVE:
      {
        final float x = getActiveX(ev);
        final float y = getActiveY(ev);
        final float dx = x - mLastTouchX, dy = y - mLastTouchY;

        if (!mIsDragging)
        {
          // Use Pythagoras to see if drag length is larger than
          // touch slop
          mIsDragging = Math.sqrt((dx * dx) + (dy * dy)) >= mTouchSlop;
        }

        if (mIsDragging)
        {
          mListener.onDrag(dx, dy);
          mLastTouchX = x;
          mLastTouchY = y;

          if (null != mVelocityTracker)
          {
            mVelocityTracker.addMovement(ev);
          }
        }
        break;
      }

      case MotionEvent.ACTION_CANCEL:
      {
        // Recycle Velocity Tracker
        if (null != mVelocityTracker)
        {
          mVelocityTracker.recycle();
          mVelocityTracker = null;
        }
        break;
      }

      case MotionEvent.ACTION_UP:
      {
        if (mIsDragging)
        {
          if (null != mVelocityTracker)
          {
            mLastTouchX = getActiveX(ev);
            mLastTouchY = getActiveY(ev);

            // Compute velocity within the last 1000ms
            mVelocityTracker.addMovement(ev);
            mVelocityTracker.computeCurrentVelocity(1000);

            final float vX = mVelocityTracker.getXVelocity(), vY = mVelocityTracker
                .getYVelocity();

            // If the velocity is greater than minVelocity, call
            // listener
            if (Math.max(Math.abs(vX), Math.abs(vY)) >= mMinimumVelocity)
            {
              mListener.onFling(mLastTouchX, mLastTouchY, -vX,
                  -vY);
            }
          }
        }

        // Recycle Velocity Tracker
        if (null != mVelocityTracker)
        {
          mVelocityTracker.recycle();
          mVelocityTracker = null;
        }
        break;
      }
    }

    return true; //super.onTouchEvent(ev);
  }

}
