/*
 * Copyright (C) 2019 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_android_sdk.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.bandyer.demo_android_sdk.BuildConfig;
import com.bandyer.demo_android_sdk.R;
import com.bandyer.demo_android_sdk.custom_views.SummaryCheckBoxPreference;
import com.bandyer.demo_android_sdk.custom_views.SummaryEditTextPreference;
import com.bandyer.demo_android_sdk.custom_views.SummaryImageEditTextPreference;
import com.bandyer.demo_android_sdk.custom_views.SummaryListPreference;
import com.bandyer.demo_android_sdk.custom_views.SummaryPreference;
import com.bandyer.demo_android_sdk.mock.MockUserProviderMode;
import com.bandyer.demo_android_sdk.utils.LeakCanaryManager;
import com.bandyer.demo_android_sdk.utils.activities.ImageTextEditActivity;
import com.bandyer.demo_android_sdk.utils.activities.MockUserDetailsSettingsActivity;
import com.bandyer.demo_android_sdk.utils.storage.ConfigurationPrefsManager;
import com.bandyer.demo_android_sdk.utils.storage.MediaStorageUtils;

import static com.bandyer.demo_android_sdk.notification.NotificationProxy.FCM_PROVIDER;
import static com.bandyer.demo_android_sdk.utils.activities.ImageTextEditActivity.PRESET_TEXT_PARAM;
import static com.bandyer.demo_android_sdk.utils.activities.ImageTextEditActivity.PRESET_URI_PARAM;
import static com.bandyer.demo_android_sdk.utils.storage.ConfigurationPrefsManager.MY_CREDENTIAL_PREFS_NAME;

public class ConfigurationFragment extends PreferenceFragmentCompat {

    private boolean credentialsChanged = false;

    public static final int BRAND_IMAGE_TEXT_REQUEST = 111;
    public static final int MOCK_USER_DETAILS_REQUEST = 222;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    }

    private SummaryCheckBoxPreference leakCanary;
    private SummaryEditTextPreference apiKey;
    private SummaryEditTextPreference appId;
    private SummaryEditTextPreference projectNumber;
    private SummaryListPreference environment;
    private SummaryListPreference pushProvider;
    private SummaryListPreference mockUserDetails;
    private SummaryImageEditTextPreference watermarkPreference;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hideBackActionFromToolbar(ConfigurationPrefsManager.hasMockCredentials(getActivity()));

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setSharedPreferencesName(MY_CREDENTIAL_PREFS_NAME);

        addPreferencesFromResource(R.xml.pref_configuration);

        environment = findPreference(getString(R.string.pref_environment));
        appId = findPreference(getString(R.string.pref_app_id));
        apiKey = findPreference(getString(R.string.pref_api_key));
        projectNumber = findPreference(getString(R.string.pref_project_number));
        pushProvider = findPreference(getString(R.string.pref_pushProvider));
        mockUserDetails = findPreference(getString(R.string.pref_mockUserDetails));

        environment.setValue(ConfigurationPrefsManager.getEnvironmentName(getActivity()));
        setupPreferenceView(environment,
                0,
                R.string.summary_environment,
                ConfigurationPrefsManager.getEnvironmentName(getActivity()));

        setupPreferenceView(apiKey,
                R.string.pref_hint_api_key,
                R.string.summary_api_key,
                ConfigurationPrefsManager.getApiKey(getActivity()));

        setupPreferenceView(appId,
                R.string.pref_hint_app_id,
                R.string.summary_app_id,
                ConfigurationPrefsManager.getAppId(getActivity()));

        setupPreferenceView(projectNumber,
                R.string.pref_hint_project_number,
                R.string.summary_project_number,
                ConfigurationPrefsManager.getFirebaseProjectNumber(getActivity()));

        setupPreferenceView(pushProvider,
                0,
                R.string.summary_pushProvider,
                ConfigurationPrefsManager.getPushProvider(getActivity()));

        updateMockUserDetailsPreferenceView();

        String currentPushProvider = ConfigurationPrefsManager.getPushProvider(getActivity());
        pushProvider.setValue(currentPushProvider);
        projectNumber.setVisible(currentPushProvider.equals(FCM_PROVIDER));

        leakCanary = findPreference(getString(R.string.leak_canary));
        if (!BuildConfig.DEBUG) {
            PreferenceScreen preferenceScreen = findPreference("preferenceScreen");
            PreferenceCategory debugCategory = findPreference(getString(R.string.pref_debug_options));
            if (preferenceScreen != null && debugCategory != null) {
                preferenceScreen.removePreference(debugCategory);
            }
        }

        if (leakCanary != null) {
            leakCanary.setChecked(ConfigurationPrefsManager.isLeakCanaryEnabled(getActivity()));
            leakCanary.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean value = ((boolean) newValue);
                setCredentialsChanged(((TwoStatePreference) preference).isChecked() != value);
                ConfigurationPrefsManager.setLeakCanaryEnabled(getActivity(), value);
                LeakCanaryManager.enableLeakCanary(value);
                return true;
            });

            leakCanary.setSecondarySummaryText(getResources().getString(R.string.summary_leak_canary));
        }

        watermarkPreference = findPreference(getString(R.string.pref_watermark_id));
        watermarkPreference.setImageUri(ConfigurationPrefsManager.getWatermarkUri(getContext()));
        watermarkPreference.setText(ConfigurationPrefsManager.getWatermarkText(getContext()));
        watermarkPreference.setSecondarySummaryText(getResources().getString(R.string.summary_watermark));
        watermarkPreference.setOnPreferenceClickListener(preference -> {
            ImageTextEditActivity.showForResult(
                    this,
                    ConfigurationPrefsManager.getWatermarkUri(getContext()),
                    ConfigurationPrefsManager.getWatermarkText(getContext()),
                    BRAND_IMAGE_TEXT_REQUEST);
            return true;
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BRAND_IMAGE_TEXT_REQUEST && resultCode == 2 && data.getExtras() != null) {
            String url = data.getStringExtra(PRESET_URI_PARAM);
            String text = data.getStringExtra(PRESET_TEXT_PARAM);
            Uri uri = MediaStorageUtils.getUriFromString(url);
            setCredentialsChanged(!equals(uri, watermarkPreference.getImageUri()) || !equals(text, watermarkPreference.getText()));
            watermarkPreference.setText(text);
            watermarkPreference.setImageUri(uri);
            ConfigurationPrefsManager.setWatermarkUri(getActivity(), url);
            ConfigurationPrefsManager.setWatermarkText(getActivity(), text);
            credentialsChanged = true;
        } else if (requestCode == MOCK_USER_DETAILS_REQUEST && resultCode == 2) {
            String customUserImageUrl = data.getStringExtra(PRESET_URI_PARAM);
            String customDisplayName = data.getStringExtra(PRESET_TEXT_PARAM);
            ConfigurationPrefsManager.setCustomUserDetailsDisplayName(getActivity(), customDisplayName);
            ConfigurationPrefsManager.setCustomUserDetailsImageUri(getActivity(), customUserImageUrl);
            updateMockUserDetailsPreferenceView();
            credentialsChanged = true;
        }
    }

    private void updateMockUserDetailsPreferenceView() {
        setupPreferenceView(mockUserDetails,
                0,
                R.string.summary_use_mock_user_details_provider,
                ConfigurationPrefsManager.getMockedUserDetailsMode(getActivity()));
        mockUserDetails.setOnPreferenceClickListener(preference -> {
            MockUserDetailsSettingsActivity.showForResult(
                    this,
                    ConfigurationPrefsManager.getCustomUserDetailsImageUri(getContext()),
                    ConfigurationPrefsManager.getCustomUserDetailsDisplayName(getContext()),
                    MOCK_USER_DETAILS_REQUEST);
            return true;
        });
    }

    @SuppressWarnings("EqualsReplaceableByObjectsCall")
    private static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }

    private void setupPreferenceView(Preference preferenceView,
                                     @StringRes int hint,
                                     @StringRes int description,
                                     String defaultValue) {

        if (preferenceView instanceof EditTextPreference)
            ((EditTextPreference) preferenceView).setOnBindEditTextListener(editText -> editText.setHint(hint));

        if (preferenceView instanceof SummaryPreference)
            ((SummaryPreference) preferenceView).setSecondarySummaryText(getResources().getString(description));

        preferenceView.setDefaultValue(defaultValue);

        preferenceView.setOnPreferenceClickListener(preference -> false);

        setSummary(preferenceView, defaultValue);

        preferenceView.setOnPreferenceChangeListener((preference, newValue) -> {
            if (newValue.toString().isEmpty()) return false;
            setCredentialsChanged(!preference.getSummary().equals(newValue.toString()));
            setSummary(preference, newValue.toString());
            if (preference.equals(pushProvider)) {
                projectNumber.setVisible(newValue.equals(FCM_PROVIDER));
                getListView().smoothScrollToPosition(getListView().getChildCount());
            }
            return true;
        });
    }

    void updateAllCredentials() {
        if (getActivity() == null) return;

        boolean hasApiKeyChanged = false;
        boolean hasAppIdChanged = false;
        boolean hasFirebaseProjectNumberChanged = false;
        boolean hasEnvironmentChanged = false;
        boolean hasPushProviderChanged = false;
        boolean hasLeakCanaryChanged = false;
        boolean hasWatermarkChanged = false;
        boolean hasMockUserProviderChanged = false;

        if (apiKey != null) {
            String defaultValue = ConfigurationPrefsManager.getApiKey(getActivity());
            hasApiKeyChanged = !defaultValue.equals(apiKey.getSummary().toString());
            setSummary(apiKey, defaultValue);
            apiKey.setText(defaultValue);
        }

        if (appId != null) {
            String defaultValue = ConfigurationPrefsManager.getAppId(getActivity());
            hasAppIdChanged = !defaultValue.equals(appId.getSummary().toString());
            setSummary(appId, defaultValue);
            appId.setText(defaultValue);
        }

        if (projectNumber != null) {
            String defaultValue = ConfigurationPrefsManager.getFirebaseProjectNumber(getActivity());
            hasFirebaseProjectNumberChanged = !defaultValue.equals(projectNumber.getSummary().toString());
            setSummary(projectNumber, defaultValue);
            projectNumber.setText(defaultValue);
        }

        if (environment != null) {
            String defaultValue = ConfigurationPrefsManager.getEnvironmentName(getActivity());
            hasEnvironmentChanged = !defaultValue.equals(environment.getSummary().toString());
            setSummary(environment, defaultValue);
            environment.setValue(defaultValue);
        }

        if (pushProvider != null) {
            String defaultValue = ConfigurationPrefsManager.getPushProvider(getActivity());
            hasPushProviderChanged = !defaultValue.equals(pushProvider.getSummary().toString());
            setSummary(pushProvider, defaultValue);
            pushProvider.setValue(defaultValue);
        }

        if (leakCanary != null) {
            boolean defaultValue = ConfigurationPrefsManager.isLeakCanaryEnabled(getActivity());
            hasLeakCanaryChanged = defaultValue != leakCanary.isChecked();
            leakCanary.setChecked(defaultValue);
        }

        if (watermarkPreference != null) {
            Uri oldUriValue = ConfigurationPrefsManager.getWatermarkUri(getActivity());
            String oldTextValue = ConfigurationPrefsManager.getWatermarkText(getActivity());

            hasWatermarkChanged = !oldUriValue.equals(watermarkPreference.getImageUri()) || !oldTextValue.equals(watermarkPreference.getText());

            watermarkPreference.setImageUri(oldUriValue);
            watermarkPreference.setText(oldTextValue);
        }

        if (mockUserDetails != null) {
            MockUserProviderMode currentMode = MockUserProviderMode.valueOf(mockUserDetails.getValue());
            if (currentMode != MockUserProviderMode.NONE)
                hasMockUserProviderChanged = true;
        }

        setCredentialsChanged(
                hasApiKeyChanged ||
                        hasAppIdChanged ||
                        hasFirebaseProjectNumberChanged ||
                        hasEnvironmentChanged ||
                        hasPushProviderChanged ||
                        hasLeakCanaryChanged ||
                        hasWatermarkChanged ||
                        hasMockUserProviderChanged
        );
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView recyclerView = getView().findViewById(R.id.recycler_view);
        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));
    }

    private void hideBackActionFromToolbar(boolean hide) {
        if (getActivity() == null) return;
        if (((AppCompatActivity) getActivity()).getSupportActionBar() == null) return;
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(!hide);
    }

    private void setCredentialsChanged(boolean changed) {
        credentialsChanged = credentialsChanged || changed;
    }

    private void setSummary(Preference preference, String string) {
        Spannable summary = new SpannableString(string);
        summary.setSpan(new ForegroundColorSpan(ContextCompat.getColor(getActivity(), R.color.colorPrimary)), 0, summary.length(), 0);
        preference.setSummary(summary);
    }

    boolean didChangeSettings() {
        return credentialsChanged;
    }
}
