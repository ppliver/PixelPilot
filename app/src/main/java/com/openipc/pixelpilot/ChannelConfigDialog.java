package com.openipc.pixelpilot;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.Serializable;

/**
 * 通道配置对话框
 */
public class ChannelConfigDialog {

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
        
        public ChannelConfig() {
            this.id = 0;
            this.name = "CH";
        }
        
        public ChannelConfig(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public static class AppSettings implements Serializable {
        public boolean throttleSticky = false;
        public int mode = 2;
    }

    private final Context mContext;
    private final ChannelConfig[] mChannels;
    private final AppSettings mSettings;
    private OnChannelConfigChanged mListener;

    public ChannelConfigDialog(Context context, ChannelConfig[] channels, AppSettings settings) {
        mContext = context;
        mChannels = channels != null ? channels : new ChannelConfig[0];
        mSettings = settings != null ? settings : new AppSettings();
    }

    public Dialog createDialog() {
        Dialog dialog = new Dialog(mContext, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        View contentView = LayoutInflater.from(mContext).inflate(R.layout.dialog_channel_config, null);
        dialog.setContentView(contentView);
        dialog.setTitle("通道配置");
        
        bindChannelViews(contentView);
        bindGlobalSettings(contentView);
        
        Button cancelButton = contentView.findViewById(R.id.cancel_button);
        if (cancelButton != null) {
            cancelButton.setOnClickListener(v -> dialog.dismiss());
        }
        
        return dialog;
    }

    private void bindChannelViews(View view) {
        for (int i = 0; i < 4; i++) {
            int chId = i + 1;
            ChannelConfig ch = i < mChannels.length ? mChannels[i] : new ChannelConfig(chId, "CH" + chId);
            
            TextView label = view.findViewById(view.getResources().getIdentifier("ch" + chId + "_label", "id", mContext.getPackageName()));
            if (label != null) {
                label.setText("CH" + chId + " " + ch.name);
            }
            
            CheckBox invert = view.findViewById(view.getResources().getIdentifier("ch" + chId + "_invert", "id", mContext.getPackageName()));
            if (invert != null) {
                invert.setChecked(ch.invert);
                invert.setOnCheckedChangeListener((btn, checked) -> {
                    ch.invert = checked;
                    if (mListener != null) mListener.onChannelConfigChanged(chId, ch);
                });
            }
            
            SeekBar trim = view.findViewById(view.getResources().getIdentifier("ch" + chId + "_trim", "id", mContext.getPackageName()));
            TextView trimText = view.findViewById(view.getResources().getIdentifier("ch" + chId + "_trim_text", "id", mContext.getPackageName()));
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
            
            EditText minEdit = view.findViewById(view.getResources().getIdentifier("ch" + chId + "_min", "id", mContext.getPackageName()));
            EditText maxEdit = view.findViewById(view.getResources().getIdentifier("ch" + chId + "_max", "id", mContext.getPackageName()));
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
        Button modeBtn = view.findViewById(R.id.mode_button);
        if (modeBtn != null) {
            modeBtn.setText("模式: Mode " + mSettings.mode);
            modeBtn.setOnClickListener(v -> {
                mSettings.mode = mSettings.mode == 1 ? 2 : 1;
                modeBtn.setText("模式: Mode " + mSettings.mode);
                if (mListener != null) mListener.onSettingsChanged(mSettings);
            });
        }
    }

    private String formatTrim(int trim) {
        return trim > 0 ? "+" + trim + " µs" : trim + " µs";
    }

    public void setOnChannelConfigChangedListener(OnChannelConfigChanged listener) {
        this.mListener = listener;
    }
}
