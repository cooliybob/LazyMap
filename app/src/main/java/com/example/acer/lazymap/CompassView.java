package com.example.acer.lazymap;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import java.text.DecimalFormat;


/**
 * Created by jinbangzhu on 12/22/15.
 */
public class CompassView extends View {

    private static final DecimalFormat df = new DecimalFormat("#.0");
    private float mDegree = 0;
    private float mRoll;
    private float mPitch;

    private Paint mPaintTriangle;
    private Paint mPaintDivision;
    private Paint mPaintDivisionBold;
    private Paint mPaintLabel;
    private Paint mPaintCross;
    private Paint mPaintGradientLine;
    private Paint mPaintNSWE, mPaintNSWE02;
    private Paint mPaintGradientCircle;
    private Paint mPaintText;


    private int divisionRadius;
    private final int defaultPadding = dp2px(5);
    private final int divisionStrokeWidth = dp2px(2);
    private final int gradientLineWidth = dp2px(20);
    private final int divisionLineWidth = dp2px(20);
    private final int crossStrokeWidth = dp2px(1);
    private final int gradientStrokeWidth = dp2px(1);
    private final int labelStrokeWidth = dp2px(1);
    private final int labelWidth = dp2px(40);
    private final int labelNSWEWidth = dp2px(25);

    private final int labelNSWEFontSize = sp2px(22);
    private final int labelNSWE02FontSize = sp2px(18);
    private final int labelFontSize = sp2px(16);

    private final int labelTextSize = dp2px(40);

    private String east, west, south, north, east_south, east_north, west_south, west_north;


    private int centerX, centerY;
    Path mPathDivision = new Path();


    public CompassView(Context context) {
        super(context);
        init();
    }

    private void init() {
        east = getContext().getString(R.string.compass_east);
        south = getContext().getString(R.string.compass_south);
        west = getContext().getString(R.string.compass_west);
        north = getContext().getString(R.string.compass_north);
        east_south = getContext().getString(R.string.compass_eastsouth);
        east_north = getContext().getString(R.string.compass_eastnorth);
        west_south = getContext().getString(R.string.compass_westsouth);
        west_north = getContext().getString(R.string.compass_westnorth);


        mPaintDivision = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintDivision.setStyle(Paint.Style.STROKE);
        mPaintDivision.setStrokeWidth(divisionStrokeWidth);
//        mPaintDivision.setPathEffect(new DashPathEffect(new float[]{5, 8}, 0));
        mPaintDivision.setColor(Color.parseColor("#979797"));

        mPaintDivisionBold = new Paint(mPaintDivision);
        mPaintDivisionBold.setColor(Color.parseColor("#FFFFFF"));
        mPaintDivisionBold.setStrokeWidth((float) (divisionStrokeWidth * 1.2));

        mPaintCross = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintCross.setColor(Color.parseColor("#88979797"));
        mPaintCross.setStyle(Paint.Style.STROKE);
        mPaintCross.setStrokeWidth(crossStrokeWidth);

        mPaintLabel = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintLabel.setColor(Color.parseColor("#AAFFFFFF"));
        mPaintLabel.setTextSize(labelFontSize);
        mPaintLabel.setTextAlign(Paint.Align.CENTER);

        mPaintNSWE = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintNSWE.setColor(Color.parseColor("#FFFFFF"));
        mPaintNSWE.setTextSize(labelNSWEFontSize);
        mPaintNSWE.setTextAlign(Paint.Align.CENTER);

        mPaintNSWE02 = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintNSWE02.setColor(Color.parseColor("#FFFFFF"));
        mPaintNSWE02.setTextSize(labelNSWE02FontSize);
        mPaintNSWE02.setTextAlign(Paint.Align.CENTER);

        mPaintTriangle = new Paint(mPaintDivisionBold);
        mPaintTriangle.setColor(Color.parseColor("#FF0000"));


        mPaintGradientCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintGradientCircle.setStyle(Paint.Style.FILL);
        mPaintGradientCircle.setColor(Color.parseColor("#33FFFFFF"));

        mPaintGradientLine = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintGradientLine.setColor(Color.parseColor("#FFFFFF"));
        mPaintGradientLine.setStyle(Paint.Style.STROKE);
        mPaintGradientLine.setStrokeWidth(gradientStrokeWidth);

        mPaintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintText.setColor(Color.parseColor("#FFFFFF"));
        mPaintText.setTextSize(labelTextSize);

//        mPaintTriangle.setStrokeWidth(150);


    }


    public CompassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CompassView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        centerX = getWidth() / 2;
        centerY = getHeight() / 2;
        divisionRadius = centerX - labelWidth - getPaddingLeft() - getPaddingRight();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

//        Log.d("onDraw", "degree= " + mDegree);
        canvas.save();

        float bigCrossLineWidth = divisionRadius - labelNSWEWidth * 2;
        float gradientCircleRadius = bigCrossLineWidth / 2;


        mPitch = mPitch > 90 ? 180 - mPitch : mPitch;
        mPitch = mPitch < -90 ? (180 + mPitch) * -1 : mPitch;

        int gradientCenterY = (int) (centerY + gradientCircleRadius * mPitch / 90);

        int gradientCenterX = (int) (centerX + gradientCircleRadius * mRoll / 90);


        canvas.drawCircle(gradientCenterX, gradientCenterY, gradientCircleRadius, mPaintGradientCircle);


        canvas.drawLine(gradientCenterX - gradientLineWidth, gradientCenterY, gradientCenterX + gradientLineWidth, gradientCenterY, mPaintGradientLine);
        canvas.drawLine(gradientCenterX, gradientCenterY - gradientLineWidth, gradientCenterX, gradientCenterY + gradientLineWidth, mPaintGradientLine);

        mPathDivision.addCircle(centerX, centerY, divisionRadius, Path.Direction.CCW);


        // Draw Top Line
        float[] ff = new float[4];
        ff[0] = centerX;
        ff[1] = centerY - getHeight() / 2;
        ff[2] = centerX;
        ff[3] = centerY - divisionRadius;
        canvas.drawLines(ff, mPaintDivision);


        for (int i = 0; i < 360; i++) {

            double cos = Math.cos(Math.toRadians(i - 90 + mDegree));
            double sin = Math.sin(Math.toRadians(i - 90 + mDegree));

            // draw 0 30 60 90 120 ...
            if (i % 3 == 0) {

                float lineFromX = (float) ((divisionRadius) * cos + centerX);
                float lineFromY = (float) ((divisionRadius) * sin + centerY);

                float lineToX = (float) ((divisionRadius - divisionLineWidth) * cos + centerX);
                float lineToY = (float) ((divisionRadius - divisionLineWidth) * sin + centerY);


                ff[0] = lineFromX;
                ff[1] = lineFromY;
                ff[2] = lineToX;
                ff[3] = lineToY;
                canvas.drawLines(ff, mPaintDivision);
            }


            //  draw 0 90 180 270
            if (i % 15 == 0) {

                float labelX = (float) ((divisionRadius + labelWidth / 2) * cos + centerX);
                float labelY = (float) ((divisionRadius + labelWidth / 2) * sin + centerY);

                float startY = centerY - (divisionRadius + labelWidth / 2);
                float totalYHeight = (divisionRadius + labelWidth / 2) * 2;

                float gapY = labelY - startY;
                labelY += labelFontSize * (gapY / totalYHeight);


                float lineFromX = (float) ((divisionRadius) * cos + centerX);
                float lineFromY = (float) ((divisionRadius) * sin + centerY);

                float lineToX = (float) ((divisionRadius - divisionLineWidth) * cos + centerX);
                float lineToY = (float) ((divisionRadius - divisionLineWidth) * sin + centerY);


//                    mPathDivision.moveTo(targetX, targetY);
                canvas.drawText(String.valueOf(i), labelX, labelY, mPaintLabel);

                ff[0] = lineFromX;
                ff[1] = lineFromY;
                ff[2] = lineToX;
                ff[3] = lineToY;
                canvas.drawLines(ff, mPaintDivisionBold);
            }


            // draw N W S E
            if (i % 45 == 0) {

                float targetX = (float) ((divisionRadius - divisionLineWidth - labelFontSize) * cos + centerX);
                float targetY = (float) ((divisionRadius - divisionLineWidth - labelFontSize) * sin + centerY);

                float startY = centerY - (divisionRadius - divisionLineWidth - labelFontSize);
                float totalYHeight = (divisionRadius - divisionLineWidth - labelFontSize) * 2;

                float gapY = targetY - startY;
                targetY += labelFontSize * (gapY / totalYHeight);


                String label =  i == 45 ? east_north : i == 90 ? east : i == 135 ? east_south : i == 180 ? south :
                        i == 225 ? west_south : i == 270 ? west : i == 315 ? west_north : i == 0 ? north : "";

                if(label== east_north || label== east_south || label== west_south || label== west_north){
                    canvas.drawText(label, targetX, targetY, mPaintNSWE02);
                }else {
                    canvas.drawText(label, targetX, targetY, mPaintNSWE);
                }

                // TODO: 12/23/15  draw red triangle
                // Draw red line for north
                if (i == 0) {

                    float fromX = (float) ((divisionRadius) * cos + centerX);
                    float fromY = (float) ((divisionRadius) * sin + centerY);


                    float toX = (float) ((divisionRadius - divisionLineWidth) * cos + centerX);
                    float toY = (float) ((divisionRadius - divisionLineWidth) * sin + centerY);


                    float[] triangle = new float[4];
                    triangle[0] = fromX;
                    triangle[1] = fromY;
                    triangle[2] = toX;
                    triangle[3] = toY;

                    canvas.drawLines(triangle, mPaintTriangle);

                }
            }

        }


        // draw big cross
        canvas.drawLine(centerX - bigCrossLineWidth, centerY, centerX + bigCrossLineWidth, centerY, mPaintCross);
        canvas.drawLine(centerX, centerY - bigCrossLineWidth, centerX, centerY + bigCrossLineWidth, mPaintCross);

        // draw text
        canvas.drawText(df.format(360-mDegree)+(char)0x00B0, getWidth()/2-gradientCircleRadius, getHeight()-gradientCircleRadius , mPaintText);

        canvas.restore();
    }

    public void updateDegree(float degree, float pitch, float roll) {
        this.mDegree = degree;
        this.mRoll = roll;
        this.mPitch = pitch;
        invalidate();
    }


    private int dp2px(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getContext().getResources().getDisplayMetrics());
    }

    private int sp2px(float sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getContext().getResources().getDisplayMetrics());
    }
}
