package com.kfast.uitest.util;

/**
 * Created by David on 2014-12-16.
 */
public class Config {
    public static int IDLE_TIME = 15;
    public static int NUM_STEPS_TO_TRIGGER_MORAL_LOSS = 30;
    public static int PROGRESS_BAR_STEPS = 200;

    public static int REFRESH_ANIM = 5;
    public static boolean HAS_EXIT_APP = false;

    public static class Action{
        public static final String ACTION_RESTORE_DEFAULT_CONFIG = "config_restore_default";
        public static final String ACTION_RESET_STEP_COUNT = "reset_step_count";
    }
}
