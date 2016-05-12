package com.example.android.sunshine.app.wear;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;

import com.example.android.sunshine.app.ForecastFragment;
import com.example.android.sunshine.app.Utility;
import com.example.android.sunshine.app.data.WeatherContract;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

public class SunshineWearableService extends WearableListenerService implements Loader.OnLoadCompleteListener<Cursor>, GoogleApiClient
    .ConnectionCallbacks, GoogleApiClient
    .OnConnectionFailedListener, DataApi.DataListener{

    private GoogleApiClient client;
    private boolean isConnected = false;

    final static int TIMEOUT = 5;

    final static String PATH_DATA = "/sunshine_wear";

    Loader<Cursor> cursorLoader;

    private Cursor weatherCursor;

//    @Override
//    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
//        // This is called when a new Loader needs to be created.  This
//        // fragment only uses one loader, so we don't care about checking the id.
//
//        // To only show current and future dates, filter the query to return weather only for
//        // dates after or including today.
//
//        // Sort order:  Ascending, by date.
//        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
//
//        String locationSetting = Utility.getPreferredLocation(getApplicationContext());
//        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
//            locationSetting, System.currentTimeMillis());
//
//        return new CursorLoader(getApplicationContext(),
//            weatherForLocationUri,
//            ForecastFragment.FORECAST_COLUMNS,
//            null,
//            null,
//            sortOrder);
//    }

    private void sendWeatherData(Cursor data) {
        //get todays weather
        data.moveToFirst();
        //get necessary data

        int weatherId = data.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID);
        String minTemp = String.valueOf(data.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP));
        String maxTemp = String.valueOf(data.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP));
        int weatherImage = Utility.getArtResourceForWeatherCondition(weatherId);

        try {
            JSONObject weatherJson = new JSONObject();
            weatherJson.put("min", minTemp);
            weatherJson.put("max", maxTemp);
            weatherJson.put("art", weatherImage);
            weatherJson.put("weatherId", weatherId);

            //TODO load metric from shared preferences
            String metric = "imperial";

            weatherJson.put("metric", metric);

            sendData(weatherJson.toString());
        } catch (JSONException e) {
            Log.e("JSON", e.toString());
        }


    }

    @Override
    public void onCreate() {
        super.onCreate();

        client = new GoogleApiClient.Builder(this)
            .addApiIfAvailable(Wearable.API)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build();

        client.connect();

        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";

        String locationSetting = Utility.getPreferredLocation(getApplicationContext());
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
            locationSetting, System.currentTimeMillis());

        cursorLoader = new CursorLoader(getApplicationContext(),
            weatherForLocationUri,
            ForecastFragment.FORECAST_COLUMNS,
            null,
            null,
            sortOrder);

        cursorLoader.registerListener(0, this);

        cursorLoader.startLoading();
    }

    @Override
    public void onConnected(Bundle bundle) {
        isConnected = true;

        //todo send weather data to Watch

        Wearable.DataApi.addListener(client, SunshineWearableService.this);

        if(weatherCursor != null && client.isConnected()){
            sendWeatherData(weatherCursor);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        isConnected = false;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        isConnected = false;
    }

    public void sendData(final String data){

        new Thread(new Runnable(){

            @Override
            public void run() {
                //if not connected, try to reconnect?
                if(!isConnected){
                    client.blockingConnect(TIMEOUT, TimeUnit.SECONDS);
                }

                //if not connected after connect above
                if(!isConnected){
                    Log.e("SUNSHINE WEAR", "Failed to connect to android wear");
                }

                if(client.isConnected()){
                    PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATH_DATA);
                    putDataMapRequest.getDataMap().putString("data", data);
                    putDataMapRequest.getDataMap().putDouble("timestamp", System.currentTimeMillis());

                    PutDataRequest request = putDataMapRequest.asPutDataRequest();

                    PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(client, request);

                    pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(DataApi.DataItemResult dataItemResult) {
                            Log.e("SUNSHINE_WEAR", "RESULT RECEIVED");
                        }
                    });
                }
            }
        }).start();

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        Log.e("WEAR_SERVICE", "ON MESSAGE RECEIVE");
    }

    @Override
    public void onLoadComplete(Loader<Cursor> loader, Cursor data) {

        weatherCursor = data;
        sendWeatherData(data);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {

        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo("/sunshine_wear") == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    //update current weather
                    String data = dataMap.getString("data");

                    if(data.equals("update")) {
                        sendWeatherData(weatherCursor);
                    }
                }

            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
            }
        }
    }
}
