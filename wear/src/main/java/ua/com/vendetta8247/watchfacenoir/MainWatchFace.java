/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ua.com.vendetta8247.watchfacenoir;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.Gravity;
import android.view.SurfaceHolder;

import java.lang.ref.WeakReference;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


public class MainWatchFace extends CanvasWatchFaceService {

    int batteryPercent = 0;

    private int getBatteryInfoPhone()
    {
        float retVal = 0;

        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus =  registerReceiver(null, iFilter);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);

        retVal = level;

        return Math.round(retVal);
    }

    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        Paint mBackgroundPaint;
        Paint mHandPaint;
        Paint mSecondHandPaint;
        Paint mBackgrountCirclePaint;
        Paint mTickPaint;
        Paint outLiningHandPaint;

        Paint mTextPaint;
        Paint mDateTextPaint;
        boolean mAmbient;
        Time mTime;

        boolean clicked;
        int transparentColor = 0x00000000;

        final Handler mUpdateTimeHandler = new EngineHandler(this);

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        boolean mRegisteredTimeZoneReceiver = false;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;


        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case WatchFaceService.TAP_TYPE_TAP:
                    clicked = !clicked;
                    System.out.println("Tapped at " + x + ":" + y + " at " + eventTime + "\nClicked: " + clicked);
                    invalidate();
                    if(transparentColor==0xdd000000&&!clicked)
                        transparentColor = 0x00000000;
                    System.out.println(transparentColor);
                    break;
                default:
                    super.onTapCommand(tapType, x, y, eventTime);
                    break;
            }



        }


        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            setWatchFaceStyle(new WatchFaceStyle.Builder(MainWatchFace.this)
                    .setAcceptsTapEvents(true)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setStatusBarGravity(Gravity.BOTTOM | Gravity.CENTER)
                    .build());

            Resources resources = MainWatchFace.this.getResources();


            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.analog_background));

            mBackgrountCirclePaint = new Paint();
            mBackgrountCirclePaint.setColor(resources.getColor(R.color.oulined_hands));

            mBackgrountCirclePaint.setAntiAlias(true);

            mHandPaint = new Paint();
            mHandPaint.setColor(resources.getColor(R.color.analog_hands));
            mHandPaint.setStrokeWidth(resources.getDimension(R.dimen.analog_hand_stroke));
            mHandPaint.setAntiAlias(true);
            mHandPaint.setStrokeCap(Paint.Cap.ROUND);

            mSecondHandPaint = new Paint();
            mSecondHandPaint.setColor(resources.getColor(R.color.analog_seconds_hand));
            mSecondHandPaint.setStrokeWidth(resources.getDimension(R.dimen.analog_second_hand_stroke));
            mSecondHandPaint.setAntiAlias(true);
            mSecondHandPaint.setStrokeCap(Paint.Cap.ROUND);

            outLiningHandPaint = new Paint();
            outLiningHandPaint.setColor(resources.getColor(R.color.oulined_hands));
            outLiningHandPaint.setStrokeWidth(resources.getDimension(R.dimen.analog_second_hand_stroke));
            outLiningHandPaint.setAntiAlias(true);
            outLiningHandPaint.setStrokeCap(Paint.Cap.ROUND);

            mTickPaint = new Paint();
            mTickPaint.setColor(resources.getColor(R.color.light_gray));
            mTickPaint.setStrokeWidth(resources.getDimension(R.dimen.analog_tick_hand_stroke));
            mTickPaint.setAntiAlias(true);
            mTickPaint.setStrokeCap(Paint.Cap.ROUND);

            mTextPaint = new Paint();
            mTextPaint.setAntiAlias(true);
            mTextPaint.setTextSize(30);
            mTextPaint.setColor(resources.getColor(R.color.analog_color_white));
            CustomFont.BOBLIC_REGULAR.apply(getApplicationContext(), mTextPaint);
            mTextPaint.setStrokeWidth(1);

            mDateTextPaint = new Paint();
            mDateTextPaint.setAntiAlias(true);
            mDateTextPaint.setStyle(Paint.Style.FILL);
            mDateTextPaint.setTextSize(22);
            mDateTextPaint.setColor(resources.getColor(R.color.analog_seconds_hand));
            CustomFont.BSTYLE_REGULAR.apply(getApplicationContext(), mDateTextPaint);
            mDateTextPaint.setStrokeWidth(3);

            mTime = new Time();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mHandPaint.setAntiAlias(!inAmbientMode);
                }
                //TODO battery
                //batteryPercent = getBatteryInfoPhone();
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }



        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();

            int width = bounds.width();
            int height = bounds.height();
            if(mAmbient)
                mBackgroundPaint.setColor(0xff000000);
            else
            mBackgroundPaint.setColor(getResources().getColor(R.color.analog_background));
            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mBackgroundPaint);


            // Find the center. Ignore the window insets so that, on round watches with a
            // "chin", the watch face is centered on the entire screen, not just the usable
            // portion.
            float centerX = width / 2f;
            float centerY = height / 2f;


            int day = mTime.monthDay;

            Rect textBounds = new Rect();
            mDateTextPaint.getTextBounds(Integer.toString(day), 0, Integer.toString(day).length(), textBounds);

            float textWidth = textBounds.width();
            float textHeight = textBounds.height();

            float left = centerX + centerX/4;
            float top = centerY + centerY/3;

            float margin = 4;


            canvas.drawLine(left - margin,top + margin,left - margin,top-textHeight - margin,mSecondHandPaint);
            canvas.drawLine(left - margin,top-textHeight - margin,left + textWidth + margin,top-textHeight - margin,mSecondHandPaint);
            canvas.drawLine(left + textWidth + margin,top-textHeight - margin,left + textWidth +margin,top + margin,mSecondHandPaint);
            canvas.drawLine(left + textWidth + margin, top + margin, left - margin, top + margin, mSecondHandPaint);

            //canvas.drawRect(left, top, left+ textWidth, top - textHeight, mSecondHandPaint);
            canvas.drawText(Integer.toString(day), centerX + centerX / 4, centerY + centerY / 3, mDateTextPaint);


            /*if(!mAmbient) {
                canvas.drawCircle(centerX, centerY, centerX - 55, mBackgrountCirclePaint);
                mBackgrountCirclePaint.setShader(new LinearGradient(centerX, centerY - 50, centerX, centerY + 50, 0xff181818, 0xff292929, Shader.TileMode.MIRROR));
                canvas.drawCircle(centerX, centerY, centerX - 50, mBackgrountCirclePaint);
            }*/





            int minutes = mTime.minute;

            float minRot = minutes / 30f * (float) Math.PI;

            float hrRot = ((mTime.hour + (minutes / 60f)) / 6f) * (float) Math.PI;

            float secLength = centerX - 25;
            float minLength = centerX - 50;
            float hrLength = centerX - 90;





            double sinVal = 0, cosVal = 0, angle = 0;
            float length1 = 0, length2 = 0;
            float x1 = 0, y1 = 0, x2 = 0, y2 = 0;

            // draw ticks
            length1 = centerX - 3;
            length2 = centerX;
            if(!mAmbient)
            for (int i = 0; i < 60; i++) {
                angle = (i * Math.PI * 2 / 60);
                sinVal = Math.sin(angle);
                cosVal = Math.cos(angle);
                if(i % 5 == 0) continue;
                x1 = (float) (sinVal * length1);
                y1 = (float) (-cosVal * length1);
                x2 = (float) (sinVal * length2);
                y2 = (float) (-cosVal * length2);
                canvas.drawLine(centerX + x1, centerY + y1, centerX + x2,
                        centerY + y2, mTickPaint);

            }


            double sinVal1 = 0, cosVal1 = 0;
            double sinVal2 = 0, cosVal2 = 0;
            double sinVal3 = 0, cosVal3 = 0;


            length1 = centerX-20;
            length2 = centerX - 1;
            for (int i = 0; i < 240; i+=20) {
                angle = (i * Math.PI * 2 / 240);
                sinVal1 = Math.sin(angle-0.03);
                cosVal1 = Math.cos(angle-0.03);

                sinVal2 = Math.sin(angle+0.03);
                cosVal2 = Math.cos(angle+0.03);

                sinVal3 = Math.sin(angle);
                cosVal3 = Math.cos(angle);

               // Log.i("angle ",Double.toString(angle));

                float tick1X = (float) (sinVal1 * length2) + centerX;
                float tick1Y = (float) (-cosVal1 * length2) + centerY;

                float tick2X = (float) (sinVal2 * length2) + centerX;
                float tick2Y = (float) (-cosVal2 * length2) + centerY;

                float tick3X = (float) (sinVal3 * length1) + centerX;
                float tick3Y = (float) (-cosVal3 * length1) + centerY;



                PointF point1, point2, point3;

                point1 = new PointF();
                point1.set(tick1X,tick1Y);

                point2 = new PointF();
                point2.set(tick2X,tick2Y);

                point3 = new PointF();
                point3.set(tick3X,tick3Y);


                if(!mAmbient) {
                    Path path = new Path();

                    path.setFillType(Path.FillType.EVEN_ODD);
                    path.moveTo(point1.x, point1.y);
                    path.lineTo(point2.x, point2.y);
                    path.lineTo(point3.x, point3.y);
                    path.lineTo(point1.x, point1.y);
                    path.close();

                    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

                    paint.setStrokeWidth(1);
                    //paint.setColor(Color.GRAY);
                    paint.setStyle(Paint.Style.FILL_AND_STROKE);
                    paint.setAntiAlias(true);
                    paint.setShader(new LinearGradient(tick3X, tick3Y, (float) (sinVal3 * length2) + centerX, (float) (-cosVal3 * length2) + centerY, 0xfff0f0f0, 0xff777777, Shader.TileMode.MIRROR));

                    canvas.drawPath(path, paint);
                }
                else
                {
                    canvas.drawLine(tick1X,tick1Y,tick2X,tick2Y,mSecondHandPaint);
                    canvas.drawLine(tick2X,tick2Y,tick3X,tick3Y,mSecondHandPaint);
                    canvas.drawLine(tick3X,tick3Y,tick1X,tick1Y,mSecondHandPaint);
                }

            }

            // drawing numbers

            length2 = centerX - 50;

            //if(!mAmbient) {

                mTextPaint.setStyle(Paint.Style.FILL);


                String text = "12";
                canvas.drawText(text, centerX - 10, centerY - length2, mTextPaint);
                text = "3";
                canvas.drawText(text, centerX + length2+5, centerY+10, mTextPaint);
                text = "6";
                canvas.drawText(text, centerX-7, centerY + length2+20, mTextPaint);
                text = "9";
                canvas.drawText(text, centerX - length2 - 20, centerY + 10 , mTextPaint);
           // }


            // draw hours


            float hrX = (float) Math.sin(hrRot) * hrLength;
            float hrY = (float) -Math.cos(hrRot) * hrLength;

            float perp1 = hrRot+30;
            float perp2 = hrRot-30;
            if(perp1>6.2) perp1-=6.2;
            if(perp2<0) perp2+=6.2;



            float hr1X = (float) Math.sin(perp1) * 8;
            float hr1Y = (float) -Math.cos(perp1) * 8;

            float hr2X = (float) Math.sin(perp2) * 8;
            float hr2Y = (float) -Math.cos(perp2) * 8;

            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            paint.setStrokeWidth(1);
            //paint.setColor(Color.GRAY);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setAntiAlias(true);

            PointF point1_draw = new PointF(centerX + hr1X, centerY + hr1Y);
            PointF point2_draw = new PointF(centerX + hr2X, centerY + hr2Y);
            PointF point3_draw = new PointF(centerX + hrX, centerY + hrY);
            PointF point4_draw = new PointF(centerX - hrX/7, centerY - hrY/7);

            paint.setShader(new LinearGradient(point1_draw.x, point1_draw.y, point2_draw.x, point2_draw.y, 0xFF9a9a9a, Color.DKGRAY, Shader.TileMode.MIRROR));

            Path path = new Path();
            path.setFillType(Path.FillType.EVEN_ODD);
            path.moveTo(point4_draw.x, point4_draw.y);
            path.lineTo(point1_draw.x, point1_draw.y);
            path.lineTo(point3_draw.x, point3_draw.y);
            path.lineTo(point2_draw.x, point2_draw.y);
            path.lineTo(point4_draw.x, point4_draw.y);
            path.close();



            if (!mAmbient) {
                canvas.drawPath(path, paint);

                canvas.drawLine(point4_draw.x, point4_draw.y, point1_draw.x, point1_draw.y, outLiningHandPaint);
                canvas.drawLine(point1_draw.x,point1_draw.y, point3_draw.x,point3_draw.y,outLiningHandPaint);
                canvas.drawLine(point3_draw.x,point3_draw.y, point2_draw.x,point2_draw.y,outLiningHandPaint);
                canvas.drawLine(point2_draw.x,point2_draw.y, point4_draw.x,point4_draw.y,outLiningHandPaint);
            }
            else
            {
                canvas.drawLine(point4_draw.x, point4_draw.y, point1_draw.x, point1_draw.y, mSecondHandPaint);
                canvas.drawLine(point1_draw.x,point1_draw.y, point3_draw.x,point3_draw.y,mSecondHandPaint);
                canvas.drawLine(point3_draw.x,point3_draw.y, point2_draw.x,point2_draw.y,mSecondHandPaint);
                canvas.drawLine(point2_draw.x,point2_draw.y, point4_draw.x,point4_draw.y,mSecondHandPaint);
            }

             //draw minutes



            perp1 = minRot+30;
            perp2 = minRot-30;
            if(perp1>6.2) perp1-=6.2;
            if(perp2<0) perp2+=6.2;

            float minX = (float) Math.sin(minRot) * minLength;
            float minY = (float) -Math.cos(minRot) * minLength;

            float min1X = (float) Math.sin(perp1) * 8;
            float min1Y = (float) -Math.cos(perp1) * 8;

            float min2X = (float) Math.sin(perp2) * 8;
            float min2Y = (float) -Math.cos(perp2) * 8;


            Paint paint1 = new Paint(Paint.ANTI_ALIAS_FLAG);

            paint1.setStrokeWidth(1);
            //paint1.setColor(Color.GRAY);
            paint1.setStyle(Paint.Style.FILL_AND_STROKE);
            paint1.setAntiAlias(true);

            PointF point1_draw1 = new PointF(centerX + min1X, centerY + min1Y);
            PointF point2_draw1 = new PointF(centerX + min2X, centerY + min2Y);
            PointF point3_draw1 = new PointF(centerX + minX, centerY + minY);
            PointF point4_draw1 = new PointF(centerX - minX / 7, centerY - minY/7);

            paint1.setShader(new LinearGradient(point1_draw1.x, point1_draw1.y, point2_draw1.x, point2_draw1.y, Color.DKGRAY, 0xFFABABAB, Shader.TileMode.MIRROR));

            Path path1 = new Path();
            path1.setFillType(Path.FillType.EVEN_ODD);
            path1.moveTo(point4_draw1.x, point4_draw1.y);
            path1.moveTo(point1_draw1.x, point1_draw1.y);
            path1.lineTo(point3_draw1.x, point3_draw1.y);
            path1.lineTo(point2_draw1.x,point2_draw1.y);
            path1.lineTo(point4_draw1.x, point4_draw1.y);
            path1.close();


            if (!mAmbient)
            {
                canvas.drawPath(path1, paint1);


                canvas.drawLine(point4_draw1.x, point4_draw1.y, point1_draw1.x, point1_draw1.y, outLiningHandPaint);
                canvas.drawLine(point1_draw1.x,point1_draw1.y, point3_draw1.x,point3_draw1.y,outLiningHandPaint);
                canvas.drawLine(point3_draw1.x,point3_draw1.y, point2_draw1.x,point2_draw1.y,outLiningHandPaint);
                canvas.drawLine(point2_draw1.x,point2_draw1.y, point4_draw1.x,point4_draw1.y,outLiningHandPaint);
            }
            else {
                canvas.drawLine(point4_draw1.x, point4_draw1.y, point1_draw1.x, point1_draw1.y, mSecondHandPaint);
                canvas.drawLine(point1_draw1.x,point1_draw1.y, point3_draw1.x,point3_draw1.y,mSecondHandPaint);
                canvas.drawLine(point3_draw1.x,point3_draw1.y, point2_draw1.x,point2_draw1.y,mSecondHandPaint);
                canvas.drawLine(point2_draw1.x,point2_draw1.y, point4_draw1.x,point4_draw1.y,mSecondHandPaint);

            }


//draw seconds
            float secRot = mTime.second / 30f * (float) Math.PI;

            perp1 = secRot+30;
            perp2 = secRot-30;
            if(perp1>6.2) perp1-=6.2;
            if(perp2<0) perp2+=6.2;

            float secX = (float) Math.sin(secRot) * secLength;
            float secY = (float) -Math.cos(secRot) * secLength;

            float sec1X = (float) Math.sin(perp1) * 5;
            float sec1Y = (float) -Math.cos(perp1) * 5;

            float sec2X = (float) Math.sin(perp2) * 5;
            float sec2Y = (float) -Math.cos(perp2) * 5;


             paint1 = new Paint(Paint.ANTI_ALIAS_FLAG);

            paint1.setStrokeWidth(1);
            //paint1.setColor(Color.GRAY);
            paint1.setStyle(Paint.Style.FILL_AND_STROKE);
            paint1.setAntiAlias(true);

             point1_draw1 = new PointF(centerX + sec1X, centerY + sec1Y);
             point2_draw1 = new PointF(centerX + sec2X, centerY + sec2Y);
             point3_draw1 = new PointF(centerX + secX, centerY + secY);
             point4_draw1 = new PointF(centerX - secX/7, centerY - secY/7);

            paint1.setShader(new LinearGradient(point1_draw1.x, point1_draw1.y, point2_draw1.x, point2_draw1.y, Color.DKGRAY, Color.LTGRAY, Shader.TileMode.MIRROR));

             path1 = new Path();
            path1.setFillType(Path.FillType.EVEN_ODD);
            path1.moveTo(point4_draw1.x, point4_draw1.y);
            path1.moveTo(point1_draw1.x, point1_draw1.y);
            path1.lineTo(point3_draw1.x, point3_draw1.y);
            path1.lineTo(point2_draw1.x,point2_draw1.y);
            path1.lineTo(point4_draw1.x, point4_draw1.y);
            path1.close();


            if (!mAmbient) {

                canvas.drawPath(path1, paint1);
                canvas.drawLine(point4_draw1.x, point4_draw1.y, point1_draw1.x, point1_draw1.y, outLiningHandPaint);
                canvas.drawLine(point1_draw1.x,point1_draw1.y, point3_draw1.x,point3_draw1.y,outLiningHandPaint);
                canvas.drawLine(point3_draw1.x,point3_draw1.y, point2_draw1.x,point2_draw1.y,outLiningHandPaint);
                canvas.drawLine(point2_draw1.x,point2_draw1.y, point4_draw1.x,point4_draw1.y,outLiningHandPaint);
                canvas.drawLine(point1_draw1.x,point1_draw1.y, point2_draw1.x,point2_draw1.y,outLiningHandPaint);
            }





            if (!mAmbient) {
                canvas.drawCircle(centerX, centerY, 4, mBackgroundPaint);
                canvas.drawCircle(centerX, centerY, 3, mHandPaint);
                canvas.drawCircle(centerX, centerY, 1, mBackgroundPaint);
            }

            if(!mAmbient&&clicked)
            {
                Paint transPaint = new Paint();
                if(transparentColor!=0xbb000000)
                transparentColor += 0x11000000;
                invalidate();
                transPaint.setColor(transparentColor);
                canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), transPaint);
            }

        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MainWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MainWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<MainWatchFace.Engine> mWeakReference;

        public EngineHandler(MainWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            MainWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }
}
