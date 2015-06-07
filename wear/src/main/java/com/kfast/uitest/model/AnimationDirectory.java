package com.kfast.uitest.model;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by David on 5/22/2015.
 */
public class AnimationDirectory implements Serializable{

    private String animationGroup;
    private ArrayList<String> animationItem;

    public AnimationDirectory(String animationGroup, ArrayList<String> animationItem) {
        this.animationGroup = animationGroup;
        this.animationItem = animationItem;
    }

    public String getAnimationGroup() {
        return animationGroup;
    }

    public void setAnimationGroup(String animationGroup) {
        this.animationGroup = animationGroup;
    }

    public ArrayList<String> getAnimationItem() {
        return animationItem;
    }

    public void setAnimationItem(ArrayList<String> animationItem) {
        this.animationItem = animationItem;
    }
}
