/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.aip.face.turnstile.utils;

import org.json.JSONException;
import org.json.JSONObject;

import com.baidu.aip.face.turnstile.exception.FaceError;
import com.baidu.aip.face.turnstile.model.AccessToken;

public class AccessTokenParser implements Parser<AccessToken> {
    @Override
    public AccessToken parse(String json) throws FaceError {
        try {
            AccessToken accessToken = new AccessToken();
            accessToken.setJson(json);
            JSONObject jsonObject = new JSONObject(json);
            accessToken.setAccessToken(jsonObject.optString("access_token"));
            accessToken.setExpiresIn(jsonObject.optInt("expires_in"));
            return accessToken;
        } catch (JSONException e) {
            e.printStackTrace();
            throw new FaceError(FaceError.ErrorCode.JSON_PARSE_ERROR, "Json parse error", e);
        }
    }
}
