package com.kfast.uitest.util;

/**
 * Created by David on 2014-12-16.
 */
public class Config {
    public static int IDLE_TIME = 15;
    public static int NUM_STEPS_TO_TRIGGER_MORAL_LOSS = 30;
    public static int PROGRESS_BAR_STEPS = 200;

    public static int REFRESH_ANIM = 15;
    public static boolean HAS_EXIT_APP = false;

    public static class Action{
        public static final String ACTION_RESTORE_DEFAULT_CONFIG = "config_restore_default";
        public static final String ACTION_RESET_STEP_COUNT = "reset_step_count";
    }

    public static class Keys{
        public static final String KEY_IDLE_TIME = "idle_time";
        public static final String KEY_STEP_MORAL_LOSS = "steps_to_moral_loss";
        public static final String KEY_PROGRESS_STEPS = "progress_steps";
        public static final String KEY_RESET_STEP_COUNT = "reset_count";
        public static final String KEY_RESTORE_DEFAULT = "restore_default";
    }
}
