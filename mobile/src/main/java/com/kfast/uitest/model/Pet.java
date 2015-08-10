package com.kfast.uitest.model;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by David on 8/8/2015.
 */
public class Pet implements Serializable{
    private String name;
    private double price;
    private Date expiryDate;
    private String description;
    private boolean enabled;
    private Date createDate;
    private Date updateDate;
    private String avatar;

    public Pet(String name, double price, Date expiryDate, String description, boolean enabled, Date createDate, Date updateDate, String avatar) {
        this.name = name;
        this.price = price;
        this.expiryDate = expiryDate;
        this.description = description;
        this.enabled = enabled;
        this.createDate = createDate;
        this.updateDate = updateDate;
        this.avatar = avatar;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}
