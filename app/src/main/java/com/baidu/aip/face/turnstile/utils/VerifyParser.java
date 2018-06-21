/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.aip.face.turnstile.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.baidu.aip.face.turnstile.exception.FaceError;
import com.baidu.aip.face.turnstile.model.FaceModel;

public class VerifyParser implements Parser<FaceModel> {
    @Override
    public FaceModel parse(String json) throws FaceError {
        FaceModel faceModel = null;
        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONArray resultArray = jsonObject.optJSONArray("result");
            if (resultArray != null) {
                faceModel = new FaceModel();
                JSONObject faceObject = resultArray.getJSONObject(0);
                faceModel.setUid(faceObject.getString("uid"));
                JSONArray scroeArray = faceObject.optJSONArray("scores");
                if (scroeArray != null) {
                    faceModel.setScore(scroeArray.getDouble(0));
                }
                faceModel.setGroupID(faceObject.getString("group_id"));
                faceModel.setUserInfo(faceObject.getString("user_info"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return faceModel;
    }
}
