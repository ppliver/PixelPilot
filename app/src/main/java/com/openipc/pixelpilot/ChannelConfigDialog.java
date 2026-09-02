package com.openipc.pixelpilot;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.io.Serializable;

/**
 * 通道配置对话框
 */
public class ChannelConfigDialog extends DialogFragment {

    public interface OnChannelConfigChanged {
        void onChannelConfigChanged(int channelId, ChannelConfig config);
        void onSettingsChanged(AppSettings settings);
    }

    public static class ChannelConfig implements Serializable {
        public int id;
        public String name;
        public int min = 1000;
        public int max = 2000;
        public int trim = 0;
        public boolean invert = false;
    }

    public static class AppSettings implements Serializable {
        public boolean throttleSticky = false;
        public int mode = 2;
    }

    private static final String ARG_CHANNELS = "channels";
    private static final String ARG_SETTINGS = "settings";

    private ChannelConfig[] mChannels;
    private AppSettings mSettings;
    private OnChannelConfigChanged mListener;

    public static ChannelConfigDialog newInstance(ChannelConfig[] channels, AppSettings settings) {
        ChannelConfigDialog dialog = new ChannelConfigDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_CHANNELS, channels);
        args.putSerializable(ARG_SETTINGS, settings);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new Dialog(requireContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_channel_config, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        mChannels = (ChannelConfig[]) getArguments().getSerializable(ARG_CHANNELS);
        mSettings = (AppSettings) getArguments().getSerializable(ARG_SETTINGS);
        
        if (mChannels == null) {
            mChannels = new ChannelConfig[4];
            String[] names = {"转向", "俯仰", "油门", "航向"};
            for (int i = 0; i < 4; i++) {
                mChannels[i] = new ChannelConfig();
                mChannels[i].id = i + 1;
                mChannels[i].name = names[i];
            }
        }
        
        if (mSettings == null) {
            mSettings = new AppSettings();
        }
        
        bindChannelViews(view);
        bindGlobalSettings(view);
    }

    private void bindChannelViews(View view) {
        for (int i = 0; i < 4; i++) {
            int chId = i + 1;
            ChannelConfig ch = mChannels[i];
            
            TextView label = view.findViewById(view.getResources().getIdentifier("ch" + chId + "_label", "id", getContext().getPackageName()));
            if (label != null) {
                label.setText("CH" + chId + " " + ch.name);
            }
            
            CheckBox invert = view.findViewById(view.getResources().getIdentifier("ch" + chId + "_invert", "id", getContext().getPackageName()));
            if (invert != null) {
                invert.setChecked(ch.invert);
                invert.setOnCheckedChangeListener((btn, checked) -> {
                    ch.invert = checked;
                    if (mListener != null) mListener.onChannelConfigChanged(chId, ch);
                });
            }
            
            SeekBar trim = view.findViewById(view.getResources().getIdentifier("ch" + chId + "_trim", "id", getContext().getPackageName()));
            TextView trimText = view.findViewById(view.getResources().getIdentifier("ch" + chId + "_trim_text", "id", getContext().getPackageName()));
            if (trim != null && trimText != null) {
                trim.setProgress(ch.trim + 200);
                trimText.setText(formatTrim(ch.trim));
                trim.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                        int t = progress - 200;
                        ch.trim = t;
                        trimText.setText(formatTrim(t));
                        if (mListener != null) mListener.onChannelConfigChanged(chId, ch);
                    }
                    @Override public void onStartTrackingTouch(SeekBar sb) {}
                    @Override public void onStopTrackingTouch(SeekBar sb) {}
                });
            }
            
            EditText minEdit = view.findViewById(view.getResources().getIdentifier("ch" + chId + "_min", "id", getContext().getPackageName()));
            EditText maxEdit = view.findViewById(view.getResources().getIdentifier("ch" + chId + "_max", "id", getContext().getPackageName()));
            if (minEdit != null) {
                minEdit.setText(String.valueOf(ch.min));
                minEdit.addTextChangedListener(new android.text.TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override public void afterTextChanged(android.text.Editable s) {
                        try { ch.min = Integer.parseInt(s.toString()); if (mListener != null) mListener.onChannelConfigChanged(chId, ch); } catch (NumberFormatException e) {}
                    }
                });
            }
            if (maxEdit != null) {
                maxEdit.setText(String.valueOf(ch.max));
                maxEdit.addTextChangedListener(new android.text.TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override public void afterTextChanged(android.text.Editable s) {
                        try { ch.max = Integer.parseInt(s.toString()); if (mListener != null) mListener.onChannelConfigChanged(chId, ch); } catch (NumberFormatException e) {}
                    }
                });
            }
        }
    }

    private void bindGlobalSettings(View view) {
        CheckBox stickyCheck = view.findViewById(view.getResources().getIdentifier("throttle_sticky_check", "id", getContext().getPackageName()));
        if (stickyCheck != null) {
            stickyCheck.setChecked(mSettings.throttleSticky);
            stickyCheck.setOnCheckedChangeListener((btn, checked) -> {
                mSettings.throttleSticky = checked;
                if (mListener != null) mListener.onSettingsChanged(mSettings);
            });
        }
        
        Button modeBtn = view.findViewById(view.getResources().getIdentifier("mode_button", "id", getContext().getPackageName()));
        if (modeBtn != null) {
            modeBtn.setText("模式: Mode " + mSettings.mode);
            modeBtn.setOnClickListener(v -> {
                mSettings.mode = mSettings.mode == 1 ? 2 : 1;
                modeBtn.setText("模式: Mode " + mSettings.mode);
                if (mListener != null) mListener.onSettingsChanged(mSettings);
            });
        }
        
        Button saveBtn = view.findViewById(view.getResources().getIdentifier("save_button", "id", getContext().getPackageName()));
        Button cancelBtn = view.findViewById(view.getResources().getIdentifier("cancel_button", "id", getContext().getPackageName()));
        
        if (saveBtn != null) saveBtn.setOnClickListener(v -> dismiss());
        if (cancelBtn != null) cancelBtn.setOnClickListener(v -> dismiss());
    }

    private String formatTrim(int trim) {
        return trim > 0 ? "+" + trim + " µs" : trim + " µs";
    }

    public void setOnChannelConfigChangedListener(OnChannelConfigChanged listener) {
        this.mListener = listener;
    }
}
