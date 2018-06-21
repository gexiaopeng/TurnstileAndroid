/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.aip.face.turnstile.model;

public class FaceVerifyResult {
    public double getLivenss() {
        return livenss;
    }

    public void setLivenss(double livenss) {
        this.livenss = livenss;
    }

    private double livenss;
}
