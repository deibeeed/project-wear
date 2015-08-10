package com.kfast.uitest.model;

import java.util.ArrayList;

/**
 * Created by David on 8/8/2015.
 */
public class PetSkill {
    private ArrayList<String> listImgs;
    private String skillName;

    public PetSkill(ArrayList<String> listImgs, String skillName) {
        this.listImgs = listImgs;
        this.skillName = skillName;
    }

    public ArrayList<String> getListImgs() {
        return listImgs;
    }

    public void setListImgs(ArrayList<String> listImgs) {
        this.listImgs = listImgs;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }
}
