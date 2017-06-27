package com.example.yuri.awarennesexample;

import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.example.yuri.awarennesexample.Fence.Activities;
import com.example.yuri.awarennesexample.Fence.FenceCreator;
import com.example.yuri.awarennesexample.Fence.FenceDelegate;
import com.example.yuri.awarennesexample.Snapshot.Listeners.ActivityListener;
import com.example.yuri.awarennesexample.Snapshot.Listeners.HeadphoneListener;
import com.example.yuri.awarennesexample.Snapshot.Listeners.PlaceListener;
import com.example.yuri.awarennesexample.Snapshot.Listeners.SnapLocationListener;
import com.example.yuri.awarennesexample.Snapshot.SnapshotManager;
import com.example.yuri.awarennesexample.Snapshot.Listeners.WeatherListener;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.state.Weather;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.PlaceLikelihood;

public class MainActivity extends AppCompatActivity implements FenceDelegate {

    //Setup API Client
    private GoogleApiClient client;
    private SnapshotManager snapCreator;
    private static final String TAG = "Awareness";

    //Fences Setup
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
        snapCreator = new SnapshotManager(MainActivity.this);
        creator = new FenceCreator(MainActivity.this, MainActivity.this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        snapCreator.getActivity(new ActivityListener() {
            @Override
            public void completion(String activity) {
                activityText.setText(activity);
            }
        });

        //Getting Headphones pluged
        snapCreator.getHeadphoneState(new HeadphoneListener() {
            @Override
            public void completion(Boolean state) {
                if (state){
                    Log.i(TAG, "Headphones plugados");
                    headphoneText.setText("Plugados");
                } else {
                    Log.i(TAG, "Headphones desplugados");
                    headphoneText.setText("Desplugados");
                }
            }
        });

        //Getting device coordinations latitude and longitude
        snapCreator.getLocation(new SnapLocationListener() {
            @Override
            public void completion(Location location) {
                locationText.setText("Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude());
            }
        });

        //Getting most likely place
        snapCreator.getPlaces(new PlaceListener() {
            @Override
            public void completion(PlaceLikelihood place) {
                localText.setText(place.getPlace().getName());
            }
        });

        //Getting Weather
        snapCreator.getWeather(new WeatherListener() {
            @Override
            public void completion(Weather weather) {
                weatherText.setText("Temp : " + weather.getTemperature(1) + " Fells Like : " + weather.getFeelsLikeTemperature(1));
            }
        });
    }
    //Fences with new Creator

    private void registerFences() {
        AwarenessFence headphone = creator.registerHeadphoneFence(true, "HeadphoneFenceKey");
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