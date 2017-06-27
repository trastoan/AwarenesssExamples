package com.example.yuri.awarennesexample.Fence;

/**
 * Created by Yuri on 11/06/17.
 */

public enum Activities {
    Running(8),
    OnBicycle(1),
    InVehicle(0),
    OnFoot(2),
    Still(3),
    Walking(7);

    private final int value;

    Activities(int valueEn) {
        value = valueEn;
    }
    public int getValue() {
        return value;
    }
}