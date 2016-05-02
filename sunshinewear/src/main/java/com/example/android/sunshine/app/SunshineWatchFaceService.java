package com.example.android.sunshine.app;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class SunshineWatchFaceService extends CanvasWatchFaceService {

    Bitmap weatherIcon;

    private Paint backgroundPaint;

    private Paint clockPaint;

    private static final Typeface BOLD_TYPEFACE =
        Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
    private static final Typeface NORMAL_TYPEFACE =
        Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    private String currentTemp;

    private String tempHigh;

    private String tempLow;

    private Paint datePaint;

    private Paint tempPaint;

    private float clockTextSize = 75f;

    private float tempTextSize = 40f;

    private float dateTextSize = 25f;

    private int weatherResourceId = -1;

    private Engine engine;

    private String weatherId;

    private String metric;

    @Override
    public Engine onCreateEngine() {
        engine = new Engine();

        return engine;
    }

    private class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener,
    GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(SunshineWatchFaceService.this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(Wearable.API)
            .build();

        private static final String TAG = "SunshineWatchFaceEng";

        int backgroundColor;
        int clockColor;


        @Override
        public void onDataChanged(DataEventBuffer dataEventBuffer) {
            Log.e("asdf", "asdf");
            int i = 0;
            i++;

            for (DataEvent event : dataEventBuffer) {
                if (event.getType() == DataEvent.TYPE_CHANGED) {
                    // DataItem changed
                    DataItem item = event.getDataItem();
                    if (item.getUri().getPath().compareTo("/sunshine_wear") == 0) {
                        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                        //update current weather
                        String weatherJson = dataMap.getString("data");

                        processWeatherJson(weatherJson);
                    }
                } else if (event.getType() == DataEvent.TYPE_DELETED) {
                    // DataItem deleted
                }
            }
        }

        @Override  // GoogleApiClient.ConnectionCallbacks
        public void onConnected(Bundle connectionHint) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onConnected: " + connectionHint);
            }

            Wearable.DataApi.addListener(mGoogleApiClient, Engine.this);
        }

        @Override  // GoogleApiClient.ConnectionCallbacks
        public void onConnectionSuspended(int cause) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onConnectionSuspended: " + cause);
            }
        }

        @Override  // GoogleApiClient.OnConnectionFailedListener
        public void onConnectionFailed(ConnectionResult result) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onConnectionFailed: " + result);
            }
        }

        private class LoadWeatherDataTask extends AsyncTask<Void, Void, String> {

            @Override
            protected String doInBackground(Void... params) {

                //TODO
                //fetch weather data from sunshine content provider
                //Uri.Builder builder = WeatherProvider

                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
            }
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            mGoogleApiClient = new GoogleApiClient.Builder(SunshineWatchFaceService.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

            mGoogleApiClient.connect();

            Resources resources = SunshineWatchFaceService.this.getResources();

            backgroundColor = resources.getColor(R.color.light_blue);
            clockColor = resources.getColor(R.color.white);

            backgroundPaint = new Paint();
            backgroundPaint.setColor(backgroundColor);

            clockPaint = new Paint();
            clockPaint.setColor(Color.WHITE);
            clockPaint.setTextSize(clockTextSize);
            clockPaint.setShadowLayer(5,1,1, R.color.black);
            clockPaint.setAntiAlias(true);

            datePaint = new Paint();
            datePaint.setColor(Color.WHITE);
            datePaint.setTextSize(dateTextSize);
            datePaint.setShadowLayer(5,1,1, R.color.black);
            datePaint.setAntiAlias(true);
            datePaint.setAlpha(190);

            tempPaint = new Paint();
            tempPaint.setColor(Color.WHITE);
            tempPaint.setTextSize(tempTextSize);
            tempPaint.setShadowLayer(5,1,1, R.color.black);
            tempPaint.setAntiAlias(true);

            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchFaceService.this)
                .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                .setShowSystemUiTime(false)
                .build());


            tempLow = "--";
            tempHigh = "--";


        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);

            long now = System.currentTimeMillis();
            Calendar mCalendar = Calendar.getInstance();

            clockTextSize = 75f;
            mCalendar.setTimeInMillis(now);

            String timeString = buildTimeString();

            canvas.drawRect(0,0,canvas.getWidth(),canvas.getHeight(), backgroundPaint);

            float clockStartX = bounds.centerX() - SunshineWatchFaceService.this.clockPaint.measureText(timeString)/2;
            float clockStartY = canvas.getHeight()/3 - 20;
            canvas.drawText(timeString, clockStartX, clockStartY, SunshineWatchFaceService.this.clockPaint);

            //Draw Date
            String dateString = new SimpleDateFormat("EEE, MMM dd yyyy").format(new Date());

            int dateTextOffsetY = -45;
            float dateStartX = bounds.centerX() - datePaint.measureText(dateString)/2;
            float dateStartY = canvas.getHeight()/2+dateTextOffsetY;

            canvas.drawText(dateString, dateStartX, dateStartY, datePaint);

            int tempOffsetX = 50;
            int tempOffsetY = 10;

            Paint minTempPaint = new Paint(tempPaint);
            minTempPaint.setAlpha(190);

            float tempHighStartX = bounds.centerX() - tempPaint.measureText(tempHigh)/2 -tempOffsetX;
            float tempLowStartX = bounds.centerX() - tempPaint.measureText(tempLow)/2 +tempOffsetX;

            canvas.drawText(tempHigh, tempHighStartX, canvas.getHeight()*5/6 + tempOffsetY, tempPaint);
            canvas.drawText(tempLow, tempLowStartX, canvas.getHeight()*5/6 + tempOffsetY, minTempPaint);


            //draw weather bitmap

            Paint weatherImagePaint = new Paint();

            if(weatherResourceId <= 0){
                weatherResourceId = R.drawable.art_fog;
            }

            Bitmap weatherImage = BitmapFactory.decodeResource(getResources(), weatherResourceId);

            float weatherImageStartX = bounds.centerX() - weatherImage.getWidth()/2;
            float weatherImageStartY = bounds.centerY() - weatherImage.getHeight()/3 + 5;

            weatherImagePaint.setAntiAlias(true);

            canvas.drawBitmap(weatherImage,weatherImageStartX, weatherImageStartY, weatherImagePaint);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
        }
    }

    private void processWeatherJson(String data) {

        try {
            JSONObject weatherJson = new JSONObject(data);
            tempHigh = weatherJson.optString("max");
            tempLow = weatherJson.optString("min");
            weatherResourceId = weatherJson.optInt("art");
            metric = weatherJson.optString("metric");
            weatherId = weatherJson.optString("weatherId");

            engine.postInvalidate();

        }
        catch(JSONException e){
            Log.e("TAG", e.toString());
        }

    }

    private String buildTimeString() {

        Calendar calendar = Calendar.getInstance();

        int minute = calendar.get(Calendar.MINUTE);

        Format format = new SimpleDateFormat("h:mm");

        String timeString = format.format(calendar.getTime());

        return timeString;

    }

    private Paint createTextPaint(int defaultInteractiveColor) {
        return createTextPaint(defaultInteractiveColor, NORMAL_TYPEFACE);
    }

    private Paint createTextPaint(int defaultInteractiveColor, Typeface typeface) {
        Paint paint = new Paint();
        paint.setColor(defaultInteractiveColor);
        paint.setTypeface(typeface);
        paint.setAntiAlias(true);
        return paint;
    }
}
