package com.oz.imagezoomcrop.imagecrop.cropoverlay;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

import com.oz.imagezoomcrop.R;
import com.oz.imagezoomcrop.imagecrop.cropoverlay.edge.Edge;
import com.oz.imagezoomcrop.imagecrop.cropoverlay.utils.PaintUtil;
import com.oz.imagezoomcrop.imagecrop.photoview.PhotoViewAttacher;


/**
 * @author GT
 *         Modified/stripped down Code from cropper library : https://github.com/edmodo/cropper
 */
public class CropOverlayView extends View implements PhotoViewAttacher.IGetImageBounds
{

  //Defaults
  private int DEFAULT_MARGINTOP = 100;
  private int DEFAULT_MARGINSIDE = 50;
  private int DEFAULT_MIN_WIDTH = 500;
  private int DEFAULT_MAX_WIDTH = 700;

  // we are croping square image so width and height will always be equal
  private int DEFAULT_CROPWIDTH = 600;

  // The Paint used to darken the surrounding areas outside the crop area.
  private Paint mBackgroundPaint;

  // The Paint used to draw the white rectangle around the crop area.
  private Paint mBorderPaint;

  // The Paint used to draw the guidelines within the crop area.
  //private Paint mGuidelinePaint;

  private int cropHeight = DEFAULT_CROPWIDTH;
  private int cropWidth = DEFAULT_CROPWIDTH;


  private int mMarginTop;
  private int mMarginSide;
  private int mMinWidth;
  private int mMaxWidth;
  private Context mContext;

  public CropOverlayView(Context context)
  {
    super(context);
    init(context);
    this.mContext = context;
  }

  public CropOverlayView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    this.mContext = context;
    TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.CropOverlayView, 0, 0);
    try
    {
      mMarginTop = ta.getDimensionPixelSize(R.styleable.CropOverlayView_marginTop, DEFAULT_MARGINTOP);
      mMarginSide = ta.getDimensionPixelSize(R.styleable.CropOverlayView_marginSide, DEFAULT_MARGINSIDE);
      mMinWidth = ta.getDimensionPixelSize(R.styleable.CropOverlayView_minWidth, DEFAULT_MIN_WIDTH);
      mMaxWidth = ta.getDimensionPixelSize(R.styleable.CropOverlayView_maxWidth, DEFAULT_MAX_WIDTH);
    }
    finally
    {
      ta.recycle();
    }

    init(context);
  }

  @Override
  protected void onDraw(Canvas canvas)
  {
    super.onDraw(canvas);
    //BUG FIX : Turn of hardware acceleration. Clip path doesn't work with hardware acceleration
    //BUG FIX : Will have to do it here @ View level. Activity level not working on HTC ONE X
    //http://stackoverflow.com/questions/8895677/work-around-canvas-clippath-that-is-not-supported-in-android-any-more/8895894#8895894
    setLayerType(View.LAYER_TYPE_SOFTWARE, null);

    Path clipPath = new Path();
    float radius = (cropHeight / 2) + mMarginSide;
    clipPath.addCircle(radius, radius, cropHeight / 2, Path.Direction.CW);
    canvas.clipPath(clipPath, Region.Op.DIFFERENCE);
    canvas.drawARGB(204, 0, 0, 0);
    canvas.save();
    canvas.restore();
    canvas.drawCircle(radius, radius, cropHeight / 2, mBorderPaint);
    //canvas.drawRoundRect(rectF, radius, radius, mBorderPaint);

    //GT :  Drop shadow not working right now. Commenting the code now
//        //Draw shadow
//        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        paint.setShadowLayer(12, 0, 0, Color.YELLOW);
//        paint.setAlpha(0);
    //drawRuleOfThirdsGuidelines(canvas);
  }

  public Rect getImageBounds()
  {
    return new Rect((int) Edge.LEFT.getCoordinate(), (int) Edge.TOP.getCoordinate(), (int) Edge.RIGHT.getCoordinate(), (int) Edge.BOTTOM.getCoordinate());
  }


  // Private Methods /////////////////////////////////////////////////////////
  private void init(Context context)
  {
    int w = context.getResources().getDisplayMetrics().widthPixels;
    cropWidth = w - 2 * mMarginSide;
    cropHeight = cropWidth;
    int edgeT = mMarginTop;
    int edgeB = mMarginTop + cropHeight;
    int edgeL = mMarginSide;
    int edgeR = mMarginSide + cropWidth;
    mBorderPaint = PaintUtil.newBorderPaint(context);
    Edge.TOP.setCoordinate(edgeT);
    Edge.BOTTOM.setCoordinate(edgeB);
    Edge.LEFT.setCoordinate(edgeL);
    Edge.RIGHT.setCoordinate(edgeR);
    new Rect(edgeL, edgeT, edgeR, edgeB);
  }
}
