package com.example.yuri.awarennesexample.Fence;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.DetectedActivityFence;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.HeadphoneFence;
import com.google.android.gms.awareness.fence.LocationFence;
import com.google.android.gms.awareness.state.HeadphoneState;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Status;

import java.util.ArrayList;

/**
 * Created by Yuri on 11/06/17.
 */

public class FenceCreator extends BroadcastReceiver {
    private Intent intent;
    private Context context;
    private FenceDelegate delegate;
    private ArrayList<String> fences;
    private PendingIntent mFencePendingIntent;
    private GoogleApiClient client;
    private static final String FENCE_RECEIVER_ACTION = "FENCE_RECEIVE";
    private static final String TAG = "Awareness";

    //Fence Settings

    public FenceCreator(Context context, FenceDelegate delegate) {
        this.delegate = delegate;
        this.context = context;
        client = new GoogleApiClient.Builder(context)
                .addApi(Awareness.API)
                .build();
        client.connect();
        fences = new ArrayList<>();
    }

    private void updateFence(AwarenessFence fence, final String title) {
        Awareness.FenceApi.updateFences(client, new FenceUpdateRequest.Builder()
                .addFence(title, fence, mFencePendingIntent).build())
        .setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                if (status.isSuccess()) {
                    Log.i(TAG, "Fence " + title + " Registrada com sucesso");
                    fences.add(title);
                } else {
                    Log.e(TAG, "Um erro ocorreu durante o registro de " + title +":" + status);
                }
            }
        });
    }

    public void unregisterFences() {
        for(final String fence : fences) {
            Awareness.FenceApi.updateFences(
                    client,
                    new FenceUpdateRequest.Builder()
                            .removeFence(fence)
                            .build()).setResultCallback(new ResultCallbacks<Status>() {
                @Override
                public void onSuccess(@NonNull Status status) {
                    Log.i(TAG, fence + " Fence removida com sucesso");
                }

                @Override
                public void onFailure(@NonNull Status status) {
                    Log.e(TAG, fence + " Fence não pode ser removida");
                }
            });
        }
    }

    //Create fences
    public AwarenessFence registerLocationFence(double latitude, double longitude, double radius, String title) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    (Activity) context,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    12345
            );
            return null;
        }
        AwarenessFence location = LocationFence.entering(latitude, longitude, radius);
        updateFence(location, title);
        return location;
    }

    public AwarenessFence registerHeadphoneFence(Boolean plugged, String title) {
        int headphoneState = plugged ? HeadphoneState.PLUGGED_IN : HeadphoneState.UNPLUGGED;
        AwarenessFence headphoneFence = HeadphoneFence.during(headphoneState);
        updateFence(headphoneFence, title);
        return headphoneFence;
    }

    public AwarenessFence registerActiviyFence(Activities activity, String title) {
        AwarenessFence exercisingFence = DetectedActivityFence.during(activity.getValue());
        updateFence(exercisingFence, title);
        return exercisingFence;
    }

    public AwarenessFence registerAndFences(AwarenessFence[] fences, String title) {
        AwarenessFence andFence = AwarenessFence.and(fences);
        updateFence(andFence, title);
        return andFence;
    }

    public AwarenessFence registerOrFences(AwarenessFence[] fences, String title) {
        AwarenessFence orFence = AwarenessFence.or(fences);
        updateFence(orFence, title);
        return orFence;
    }

    //BroadCast Receiver

    public void onReceive(Context context, Intent intent) {
        FenceState fenceState = FenceState.extract(intent);
        switch (fenceState.getCurrentState()) {
            case FenceState.TRUE:
                delegate.fenceHasChanged(fenceState.getFenceKey(), true);
                break;
            case FenceState.FALSE:
                delegate.fenceHasChanged(fenceState.getFenceKey(), false);
                break;
            case FenceState.UNKNOWN:
                Log.i(TAG, "Fence > A Fence possui um estado desconhecido.");
                break;
        }
    }
}
