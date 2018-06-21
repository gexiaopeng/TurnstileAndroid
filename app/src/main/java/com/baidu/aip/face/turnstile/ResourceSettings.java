/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.aip.face.turnstile;

import com.baidu.aip.face.LivenessDetector;
import com.shinesun.face.R;

public class ResourceSettings {

    public static void init() {

    }

    static {
        LivenessDetector.getInstance().setTip(LivenessDetector.HINT_MOVE_INTO_FRAME, R.string.hint_move_into_frame);
        LivenessDetector.getInstance().setTip(LivenessDetector.HINT_HEAD_HIGHER, R.string.hint_head_higher);
        LivenessDetector.getInstance().setTip(LivenessDetector.HINT_HEAD_DOWN, R.string.hint_head_down);
        LivenessDetector.getInstance().setTip(LivenessDetector.HINT_COME_CLOSER, R.string.hint_come_closer);
        LivenessDetector.getInstance().setTip(LivenessDetector.HINT_MOVE_FURTHER, R.string.hint_move_further);
        LivenessDetector.getInstance().setTip(LivenessDetector.HINT_TURN_LEFT, R.string.hint_turn_left);
        LivenessDetector.getInstance().setTip(LivenessDetector.HINT_TURN_RIGHT, R.string.hint_turn_right);

        LivenessDetector.getInstance().setTip(LivenessDetector.HINT_MOVE_LEFT, R.string.hint_move_left);
        LivenessDetector.getInstance().setTip(LivenessDetector.HINT_MOVE_UP, R.string.hint_move_up);
        LivenessDetector.getInstance().setTip(LivenessDetector.HINT_MOVE_RIGHT, R.string.hint_move_right);
        LivenessDetector.getInstance().setTip(LivenessDetector.HINT_MOVE_DOWN, R.string.hint_move_down);

        LivenessDetector.getInstance()
                .setTip(LivenessDetector.HINT_OCCLUSION_IN_LEFT_EYE, R.string.hint_occlusion_in_left_eye);
        LivenessDetector.getInstance()
                .setTip(LivenessDetector.HINT_OCCLUSION_IN_RIGHT_EYE, R.string.hint_occlusion_in_right_eye);
        LivenessDetector.getInstance()
                .setTip(LivenessDetector.HINT_OCCLUSION_IN_NOSE, R.string.hint_occlusion_in_right_eye);
        LivenessDetector.getInstance()
                .setTip(LivenessDetector.HINT_OCCLUSION_IN_MOUTH, R.string.hint_occlusion_in_mouth);
        LivenessDetector.getInstance()
                .setTip(LivenessDetector.HINT_OCCLUSION_IN_LEFT_CHEEK, R.string.hint_occlusion_in_left_cheek);
        LivenessDetector.getInstance()
                .setTip(LivenessDetector.HINT_OCCLUSION_IN_RIGHT_CHEEK, R.string.hint_occlusion_in_right_eye);
        LivenessDetector.getInstance().setTip(LivenessDetector.HINT_OCCLUSION_IN_CHIN, R.string.hint_occlusion_in_chin);
        LivenessDetector.getInstance().setTip(LivenessDetector.HINT_OCCLUSION_IN_FACE, R.string.hint_occlusion_in_face);
        LivenessDetector.getInstance().setTip(LivenessDetector.HINT_LIGHT_LOW, R.string.hint_light_low);
        LivenessDetector.getInstance().setTip(LivenessDetector.HINT_KEEP_STILL, R.string.hint_keep_still);
        LivenessDetector.getInstance().setTip(LivenessDetector.HINT_TIMEOUT, R.string.hint_timeout);
        LivenessDetector.getInstance().setTip(LivenessDetector.HINT_SUCCESS, R.string.hint_success);
        LivenessDetector.getInstance().setTip(LivenessDetector.HINT_VERY_GOOD, R.string.hint_very_good);

        LivenessDetector.getInstance().setTip(LivenessDetector.ACTION_BLINK, R.string.action_blink);
        LivenessDetector.getInstance().setTip(LivenessDetector.ACTION_BLINK_LEFT_EYE, R.string.action_blink_left_eye);
        LivenessDetector.getInstance().setTip(LivenessDetector.ACTION_BLINK_RIGHT_EYE, R.string.action_blink_right_eye);
        LivenessDetector.getInstance().setTip(LivenessDetector.ACTION_OPEN_MOUTH, R.string.action_open_mouth);
        LivenessDetector.getInstance().setTip(LivenessDetector.ACTION_LEAN_HEAD_LEFT, R.string.action_lean_head_left);
        LivenessDetector.getInstance().setTip(LivenessDetector.ACTION_LEAN_HEAD_RIGHT, R.string.action_lean_head_right);
        LivenessDetector.getInstance().setTip(LivenessDetector.ACTION_HEAD_UP, R.string.action_head_up);
        LivenessDetector.getInstance().setTip(LivenessDetector.ACTION_HEAD_DOWN, R.string.action_head_down);
    }
}
