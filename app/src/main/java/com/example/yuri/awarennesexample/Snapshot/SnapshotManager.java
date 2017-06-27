package com.example.yuri.awarennesexample.Snapshot;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.example.yuri.awarennesexample.Snapshot.Listeners.ActivityListener;
import com.example.yuri.awarennesexample.Snapshot.Listeners.HeadphoneListener;
import com.example.yuri.awarennesexample.Snapshot.Listeners.PlaceListener;
import com.example.yuri.awarennesexample.Snapshot.Listeners.SnapLocationListener;
import com.example.yuri.awarennesexample.Snapshot.Listeners.WeatherListener;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.snapshot.DetectedActivityResult;
import com.google.android.gms.awareness.snapshot.HeadphoneStateResult;
import com.google.android.gms.awareness.snapshot.LocationResult;
import com.google.android.gms.awareness.snapshot.PlacesResult;
import com.google.android.gms.awareness.snapshot.WeatherResult;
import com.google.android.gms.awareness.state.HeadphoneState;
import com.google.android.gms.awareness.state.Weather;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.*;
import com.google.android.gms.location.places.PlaceLikelihood;

import java.util.List;

/**
 * Created by Yuri on 19/06/17.
 */

public class SnapshotManager {
    private Context context;
    private GoogleApiClient client;
    private static final String TAG = "Awareness";

    public SnapshotManager(Context context) {
        client = new GoogleApiClient.Builder(context)
                .addApi(Awareness.API)
                .build();
        client.connect();
    }

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


    public void getActivity(final ActivityListener listener) {
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
                        Log.i(TAG, atividade.toString());
                        String activity = returnActivity(atividade.getType());
                        listener.completion(activity);
                    }
                });

    }

    public void getHeadphoneState(final HeadphoneListener listener) {
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
                            listener.completion(true);
                        } else {
                            Log.i(TAG, "Headphones desplugados");
                            listener.completion(false);
                        }
                    }
                });
    }

    public void getLocation(final SnapLocationListener listener) {
        if (!checkPermission()) {
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
                        listener.completion(location);
                        Log.i(TAG, "Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude());
                    }
                });
    }


    public void getPlaces(final PlaceListener listener) {
        if (!checkPermission()) {
            return;
        }
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
                                listener.completion(place);
                            }
//                            localText.setText(closestPlace.getPlace().getName());
                        } else {
                            Log.e(TAG, "Nenhum local encontrado");
                        }

                    }
                });
    }

    public void getWeather(final WeatherListener listener) {
        if (!checkPermission()) {
            return;
        }
        Awareness.SnapshotApi.getWeather(client)
                .setResultCallback(new ResultCallback<WeatherResult>() {
                    @Override
                    public void onResult(@NonNull WeatherResult weatherResult) {
                        if (!weatherResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Erro ao buscar locais");
                            return;
                        }
                        Weather weather = weatherResult.getWeather();
                        listener.completion(weather);
                        /*You can get
                        *Temperature
                         * Fells Like Temperature
                          * Humidity
                          * Dew Point
                          * Conditions*/
                        Log.i(TAG, "Clima : " + weather);
//                        weatherText.setText("Temp : " + weather.getTemperature(1) + " Fells Like : " + weather.getFeelsLikeTemperature(1));
                    }
                });
    }



    public boolean checkPermission() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    (Activity) context,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    12345
            );
            return false;
        }else {
            return true;
        }
    }
}
