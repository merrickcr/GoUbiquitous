package com.example.android.sunshine.app;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends WearableActivity implements MessageApi.MessageListener, GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient
    .OnConnectionFailedListener{

    private GoogleApiClient client;
    private boolean isConnected = false;

    final static int TIMEOUT = 5;

    final static String PATH_DATA = "sunshine_wear/";


    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
        new SimpleDateFormat("HH:mm", Locale.US);

    private BoxInsetLayout mContainerView;

    private TextView mTextView;

    private TextView mClockView;


    @Override
    public void onConnected(Bundle bundle) {
        isConnected = true;
        Wearable.MessageApi.addListener(client, this);
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        client = new GoogleApiClient.Builder(this)
            .addApiIfAvailable(Wearable.API)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build();

        client.connect();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mTextView = (TextView) findViewById(R.id.text);
        mClockView = (TextView) findViewById(R.id.clock);
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
            mTextView.setTextColor(getResources().getColor(android.R.color.white));
            mClockView.setVisibility(View.VISIBLE);

            mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
        } else {
            mContainerView.setBackground(null);
            mTextView.setTextColor(getResources().getColor(android.R.color.black));
            mClockView.setVisibility(View.GONE);
        }
    }


    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {
        runOnUiThread( new Runnable() {
            @Override
            public void run() {
                if( messageEvent.getPath().equalsIgnoreCase( PATH_DATA ) ) {
                    Log.e("messageEventWear", messageEvent.getData().toString());
                }
            }
        });
    }
}
