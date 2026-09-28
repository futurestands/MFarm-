package dev.mfarm.com.mfarm.intelligence;

import java.util.ArrayList;
import java.util.List;

public class WhatChanged {
    private final List<String> changesList;

    public WhatChanged(List<String> changesList) {
        this.changesList = changesList != null ? changesList : new ArrayList<String>();
    }

    public List<String> getChangesList() {
        return changesList;
    }

    public boolean hasChanges() {
        return !changesList.isEmpty();
    }
}
