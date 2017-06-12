package com.example.yuri.awarennesexample;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.DetectedActivityFence;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.HeadphoneFence;
import com.google.android.gms.awareness.fence.LocationFence;
import com.google.android.gms.awareness.snapshot.DetectedActivityResult;
import com.google.android.gms.awareness.snapshot.HeadphoneStateResult;
import com.google.android.gms.awareness.snapshot.LocationResult;
import com.google.android.gms.awareness.snapshot.PlacesResult;
import com.google.android.gms.awareness.snapshot.WeatherResult;
import com.google.android.gms.awareness.state.HeadphoneState;
import com.google.android.gms.awareness.state.Weather;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.places.PlaceLikelihood;

import java.util.List;

public class MainActivity extends AppCompatActivity implements FenceDelegate {

    private static MainActivity ins;
    //Setup API Client
    private GoogleApiClient client;
    private static final String TAG = "Awareness";

    //Fences Setup
    private static final String FENCE_RECEIVER_ACTION = "FENCE_RECEIVE";

    private PendingIntent mFencePendingIntent;

    private FenceCreator creator;

    //Text views
    public TextView activityText;
    public TextView locationText;
    public TextView localText;
    public TextView weatherText;
    public TextView headphoneText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Create FenceCreator
        creator = new FenceCreator(MainActivity.this, MainActivity.this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ins = this;

        activityText = (TextView) findViewById(R.id.activityText);
        locationText = (TextView) findViewById(R.id.locationText);
        localText = (TextView) findViewById(R.id.localText);
        weatherText = (TextView) findViewById(R.id.weatherText);
        headphoneText = (TextView) findViewById(R.id.headphonesText);

        //Finish client setup
        client = new GoogleApiClient.Builder(MainActivity.this)
                .addApi(Awareness.API)
                .build();
        client.connect();

        initSnapshot();
    }

    //Update labels from broadcastReceiver
    public static MainActivity getInstance() {
        return ins;
    }

    public void updateHeadphoneText(String text) {
        headphoneText.setText(text);
    }

    public void updateActvityText(String text) {
        activityText.setText(text);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Using new fenceCreator
        registerFences();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterFences();
    }


    private void initSnapshot() {

        //Getting user activity
        Awareness.SnapshotApi.getDetectedActivity(client)
                .setResultCallback(new ResultCallback<DetectedActivityResult>() {
                    @Override
                    public void onResult(@NonNull DetectedActivityResult detectedActivityResult) {
                        if (!detectedActivityResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Erro ao identificar atividade atual");
                            return;
                        }
                        ActivityRecognitionResult resultado = detectedActivityResult.getActivityRecognitionResult();
                        DetectedActivity atividade = resultado.getMostProbableActivity();
                        activityText.setText(returnActivity(atividade.getType()));
                        Log.i(TAG, atividade.toString());
                    }
                });

        //Getting Headphones pluged

        Awareness.SnapshotApi.getHeadphoneState(client)
                .setResultCallback(new ResultCallback<HeadphoneStateResult>() {
                    @Override
                    public void onResult(@NonNull HeadphoneStateResult headphoneStateResult) {
                        if (!headphoneStateResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Erro ao identificar conexão com headphone");
                            return;
                        }
                        HeadphoneState state = headphoneStateResult.getHeadphoneState();
                        if (state.getState() == HeadphoneState.PLUGGED_IN) {
                            Log.i(TAG, "Headphones plugados");
                            headphoneText.setText("Plugados");
                        } else {
                            Log.i(TAG, "Headphones desplugados");
                            headphoneText.setText("Desplugados");
                        }
                    }
                });

        //Necessary after Android M
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    12345
            );
            return;
        }
        //Getting device coordinations latitude and longitude
        Awareness.SnapshotApi.getLocation(client)
                .setResultCallback(new ResultCallback<LocationResult>() {
                    @Override
                    public void onResult(@NonNull LocationResult locationResult) {
                        if (!locationResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Erro ao identificar locais");
                            return;
                        }
                        Location location = locationResult.getLocation();
                        locationText.setText("Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude());
                        Log.i(TAG, "Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude());
                    }
                });

        //Getting most likely place
        Awareness.SnapshotApi.getPlaces(client)
                .setResultCallback(new ResultCallback<PlacesResult>() {
                    @Override
                    public void onResult(@NonNull PlacesResult placesResult) {
                        if (!placesResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Erro ao buscar locais");
                            return;
                        }

                        List<PlaceLikelihood> placesList = placesResult.getPlaceLikelihoods();
                        PlaceLikelihood closestPlace = placesList.get(0);
                        if (placesList != null) {
                            for (PlaceLikelihood place : placesList) {
                                /*Place can provide:
                                * Address
                                * Phone Number
                                * Place Type
                                * WebSite
                                * Location
                                * Price Level
                                * Rating
                                * */
                                if (place.getLikelihood() > closestPlace.getLikelihood()) {
                                    closestPlace = place;
                                }
                                Log.i(TAG, place.getPlace().getName().toString() + ", probabilidade :" + place.getLikelihood());
                            }
                            localText.setText(closestPlace.getPlace().getName());
                        } else {
                            Log.e(TAG, "Nenhum local encontrado");
                        }

                    }
                });

        //Getting Weather
        Awareness.SnapshotApi.getWeather(client)
                .setResultCallback(new ResultCallback<WeatherResult>() {
                    @Override
                    public void onResult(@NonNull WeatherResult weatherResult) {
                        if (!weatherResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Erro ao buscar locais");
                            return;
                        }
                        Weather weather = weatherResult.getWeather();
                        /*You can get
                        *Temperature
                         * Fells Like Temperature
                          * Humidity
                          * Dew Point
                          * Conditions*/
                        Log.i(TAG, "Clima : " + weather);
                        weatherText.setText("Temp : " + weather.getTemperature(1) + " Fells Like : " + weather.getFeelsLikeTemperature(1));
                    }
                });

    }


    //Fences are a little more complicated, they need a lot more configuration


    /*Type of detected activities
   * atividade.getType() returns an integer that translates to the enum below
   * atividade.getConfidence returns 0 - 100 and will return the likelihood of being that activity
   * */


    public String returnActivity(int type) {
        switch (type) {
            case 0:
                return "InVehicle";
            case 1:
                return "OnBycicle";
            case 2:
                return "OnFoot";
            case 3:
                return "Still";
            case 4:
                return "Unknow";
            case 5:
                return "Tilting";
            case 7:
                return "Walking";
            case 8:
                return "Running";
            default:
                return "Unknow";
        }
    }


    //Fences with new Creator

    private void registerFences() {
        AwarenessFence headphone = creator.registeHeadphoneFence(true, "HeadphoneFenceKey");
        AwarenessFence walking = creator.registerActiviyFence(Activities.Walking, "ActivityFenceKey");
        AwarenessFence[] andFences = new AwarenessFence[]{walking,headphone};
        creator.registerAndFences(andFences, "ExercisingWithHeadphoneKey");
        creator.registerLocationFence(-3.7543518, -38.5268885, 20.0, "EnteringHome");
    }

    //To avoid memory leak we must unregister fences once we finished using them
    private void unregisterFences() {
        creator.unregisterFences();
    }

    //Handle changes on fence
    @Override
    public void fenceHasChanged(String title, Boolean state) {
        if(TextUtils.equals(title, "HeadphoneFenceKey")){
            updateHeadphoneText(state ? "Conectado" : "Desconectado");
        }else if (TextUtils.equals(title, "ActivityFenceKey")){
            updateActvityText(state ? "Tilt" : "Parado");
        }else if (TextUtils.equals(title, "ExercisingWithHeadphoneKey")) {
            updateActvityText(state ? "Exercising With Headphones" : "Exercising");
        }
    }
}