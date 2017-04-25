package com.example.kaloqn.mediaplayer;

import java.util.Observable;

/**
 * Created by Kaloyan on 25/04/2017.
 */

public class ObservableObject extends Observable {
    private static ObservableObject instance = new ObservableObject();

    public static ObservableObject getInstance() {
        return instance;
    }

    private ObservableObject() {
    }

    public void updateValue(Object data) {
        synchronized (this) {
            setChanged();
            notifyObservers(data);
        }
    }
}