package com.winlator;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.core.AppUtils;
import com.winlator.core.DeviceProfile;
import com.winlator.core.PreloaderDialog;
import com.winlator.core.WineThemeManager;
import com.winlator.widget.FrameRating;
import com.winlator.xenvironment.RootFS;
import com.winlator.xenvironment.RootFSInstaller;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/** Minimal, low-overhead front end that boots directly into Steam. */
public class SteamLauncherFragment extends Fragment {
    private static final String CONTAINER_NAME = "Steam H200";
    private static final String STEAM_INSTALLER = "Z:\\opt\\apps\\winaddons.exe";
    private static final String STEAM_INSTALLER_ARGS = "-n \"Steam (Legacy)\" -d \"Steam\" -e \"steam.exe\"";
    private static final String STEAM_ARGS = "-nochatui -nofriendsui";

    private TextView statusView;
    private Button launchButton;
    private PreloaderDialog preloaderDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.steam_launcher_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AppCompatActivity activity = (AppCompatActivity)requireActivity();
        activity.getSupportActionBar().setTitle(R.string.steam_launcher);

        statusView = view.findViewById(R.id.TVSteamStatus);
        launchButton = view.findViewById(R.id.BTLaunchSteam);
        preloaderDialog = new PreloaderDialog(requireActivity());
        launchButton.setOnClickListener((v) -> prepareAndLaunch());

        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .edit()
            .putBoolean("save_mem_on_run_from_steam", true)
            .apply();
        updateStatus();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (statusView != null) updateStatus();
    }

    private void updateStatus() {
        Context context = requireContext();
        if (!RootFS.find(context).isValid()) {
            statusView.setText(R.string.steam_preparing_engine);
            launchButton.setText(R.string.steam_open);
            return;
        }

        Container container = findSteamContainer(new ContainerManager(context));
        boolean installed = container != null && findSteamExecutable(container) != null;
        statusView.setText(installed ? R.string.steam_ready : R.string.steam_not_installed);
        launchButton.setText(installed ? R.string.steam_open : R.string.steam_install);
    }

    private void prepareAndLaunch() {
        MainActivity activity = (MainActivity)requireActivity();
        if (!RootFS.find(activity).isValid()) {
            RootFSInstaller.installIfNeeded(activity);
            AppUtils.showToast(activity, R.string.steam_preparing_hint);
            return;
        }

        ContainerManager manager = new ContainerManager(activity);
        Container container = findSteamContainer(manager);
        if (container != null) {
            launchSteam(container);
            return;
        }

        launchButton.setEnabled(false);
        statusView.setText(R.string.steam_creating_container);
        preloaderDialog.show(R.string.creating_container);
        try {
            manager.createContainerAsync(createContainerData(activity), (createdContainer) -> {
                preloaderDialog.close();
                launchButton.setEnabled(true);
                if (createdContainer != null) launchSteam(createdContainer);
                else {
                    statusView.setText(R.string.steam_container_error);
                    AppUtils.showToast(activity, R.string.steam_container_error);
                }
            });
        }
        catch (JSONException e) {
            preloaderDialog.close();
            launchButton.setEnabled(true);
            statusView.setText(R.string.steam_container_error);
            AppUtils.showToast(activity, R.string.steam_container_error);
        }
    }

    private JSONObject createContainerData(Context context) throws JSONException {
        JSONObject data = new JSONObject();
        data.put("name", CONTAINER_NAME);
        data.put("screenSize", DeviceProfile.getDefaultScreenSize(context));
        data.put("envVars", DeviceProfile.getDefaultEnvVars(context));
        String cpuList = DeviceProfile.getDefaultCPUList(context);
        data.put("cpuList", cpuList);
        data.put("cpuListWoW64", cpuList);
        data.put("graphicsDriver", DeviceProfile.getDefaultGraphicsDriver(context));
        data.put("dxwrapper", Container.DEFAULT_DXWRAPPER);
        data.put("dxwrapperConfig", DeviceProfile.getDefaultDXWrapperConfig(context));
        data.put("graphicsDriverConfig", DeviceProfile.getDefaultGraphicsDriverConfig(context));
        data.put("audioDriver", Container.DEFAULT_AUDIO_DRIVER);
        data.put("audioDriverConfig", "");
        data.put("wincomponents", Container.DEFAULT_WINCOMPONENTS);
        data.put("drives", Container.DEFAULT_DRIVES);
        data.put("hudMode", FrameRating.Mode.DISABLED.ordinal());
        data.put("startupSelection", Container.STARTUP_SELECTION_ESSENTIAL);
        data.put("box64Preset", DeviceProfile.getDefaultBox64Preset(context));
        data.put("desktopTheme", WineThemeManager.DEFAULT_DESKTOP_THEME);
        return data;
    }

    private Container findSteamContainer(ContainerManager manager) {
        for (Container container : manager.getContainers()) {
            if (CONTAINER_NAME.equals(container.getName())) return container;
        }
        return null;
    }

    private File findSteamExecutable(Container container) {
        String[] relativePaths = {
            ".wine/drive_c/Program Files (x86)/Steam/steam.exe",
            ".wine/drive_c/Program Files (x86)/Steam/Steam.exe",
            ".wine/drive_c/Program Files/Steam/steam.exe",
            ".wine/drive_c/Program Files/Steam/Steam.exe",
            ".wine/drive_c/Steam/steam.exe"
        };
        for (String relativePath : relativePaths) {
            File executable = new File(container.getRootDir(), relativePath);
            if (executable.isFile()) return executable;
        }
        return null;
    }

    private void launchSteam(Container container) {
        File steamExecutable = findSteamExecutable(container);
        Intent intent = new Intent(requireContext(), XServerDisplayActivity.class);
        intent.putExtra("container_id", container.id);
        if (steamExecutable != null) {
            intent.putExtra("exec_path", steamExecutable.getPath());
            intent.putExtra("exec_args", STEAM_ARGS);
        }
        else {
            intent.putExtra("exec_dos_path", STEAM_INSTALLER);
            intent.putExtra("exec_args", STEAM_INSTALLER_ARGS);
        }
        startActivity(intent);
    }
}
