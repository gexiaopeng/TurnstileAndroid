/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.aip.face.turnstile.utils;

import com.baidu.aip.face.turnstile.exception.FaceError;

/**
 * JSON解析器
 * @param <T> 泛型，解析结果类
 */
public interface Parser<T> {
    T parse(String json) throws FaceError;
}
