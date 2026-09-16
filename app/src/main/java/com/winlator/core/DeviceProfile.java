package com.winlator.core;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;

import com.winlator.box64.Box64Preset;
import com.winlator.container.Container;
import com.winlator.container.GraphicsDrivers;

import java.util.Locale;

/** Runtime defaults for the HONOR 200 / Snapdragon 7 Gen 3 target. */
public abstract class DeviceProfile {
    public static final String HONOR_200_SCREEN_SIZE = "1280x720";
    public static final String HONOR_200_CPU_LIST = "4,5,6,7";

    private static boolean isHonor200Model() {
        String manufacturer = Build.MANUFACTURER != null ? Build.MANUFACTURER.toUpperCase(Locale.ENGLISH) : "";
        String model = Build.MODEL != null ? Build.MODEL.toUpperCase(Locale.ENGLISH) : "";
        return manufacturer.contains("HONOR") &&
               (model.startsWith("ELP-") || model.equals("HONOR 200"));
    }

    public static boolean isHonor200(Context context) {
        return isHonor200Model() || GPUHelper.getAdrenoModelId(context) == 720;
    }

    public static String getName(Context context) {
        return isHonor200(context) ? "HONOR 200 / Snapdragon 7 Gen 3" : "Generic ARM64";
    }

    public static String getDefaultScreenSize(Context context) {
        return isHonor200(context) ? HONOR_200_SCREEN_SIZE : Container.DEFAULT_SCREEN_SIZE;
    }

    public static String getDefaultGraphicsDriver(Context context) {
        if (isHonor200(context)) {
            return GraphicsDrivers.TURNIP + "," + GraphicsDrivers.DEFAULT_OPENGL_DRIVER;
        }
        return GraphicsDrivers.getDefaultDriver(context);
    }

    public static String getDefaultGraphicsDriverConfig(Context context) {
        if (!isHonor200(context)) return "";
        return "version=" + DefaultVersion.TURNIP +
               ",maxDeviceMemory=" + getRecommendedVideoMemory(context) +
               ",useHWBuf=1,forceWaitForFences=0";
    }

    public static String getDefaultDXWrapperConfig(Context context) {
        if (!isHonor200(context)) return "";
        return "version=" + DefaultVersion.DXVK(GraphicsDrivers.TURNIP) +
               ",framerate=30,maxDeviceMemory=" + getRecommendedVideoMemory(context);
    }

    public static String getDefaultEnvVars(Context context) {
        String defaults = Container.DEFAULT_ENV_VARS;
        if (!isHonor200(context)) return defaults;
        return defaults.replace("TU_DEBUG=noconform", "TU_DEBUG=noconform,gmem");
    }

    public static String getDefaultBox64Preset(Context context) {
        return isHonor200(context) ? Box64Preset.HONOR_200_BALANCED : Box64Preset.DEFAULT;
    }

    public static String getDefaultCPUList(Context context) {
        int processors = Runtime.getRuntime().availableProcessors();
        return isHonor200(context) && processors >= 8
            ? HONOR_200_CPU_LIST
            : Container.getFallbackCPUList();
    }

    private static String getRecommendedVideoMemory(Context context) {
        ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) return "2048";

        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        long totalMemoryGB = memoryInfo.totalMem / (1024L * 1024L * 1024L);
        return totalMemoryGB >= 10 ? "4096" : "2048";
    }
}
