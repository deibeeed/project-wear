package com.kfast.uitest.model;

import java.io.Serializable;

/**
 * Created by David on 2015-03-14.
 */
public class UnsentSteps implements Serializable{

    private String date;
    private int stepCount;

    public UnsentSteps(String date, int stepCount) {
        this.date = date;
        this.stepCount = stepCount;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getStepCount() {
        return stepCount;
    }

    public void setStepCount(int stepCount) {
        this.stepCount = stepCount;
    }
}
