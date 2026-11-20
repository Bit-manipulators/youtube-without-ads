package org.schabi.newpipe.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.preference.Preference;

public class AboutSettingsFragment extends BasePreferenceFragment {

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        addPreferencesFromResourceRegistry();

        final Preference orgPref = findPreference("about_organization");
        if (orgPref != null) {
            orgPref.setOnPreferenceClickListener(preference -> {
                openUrl("https://github.com/Bit-manipulators");
                return true;
            });
        }

        final Preference creatorPref = findPreference("about_creator_maintainer");
        if (creatorPref != null) {
            creatorPref.setOnPreferenceClickListener(preference -> {
                openUrl("https://github.com/amitpadhan525");
                return true;
            });
        }

        final Preference repoPref = findPreference("about_github_repo");
        if (repoPref != null) {
            repoPref.setOnPreferenceClickListener(preference -> {
                openUrl("https://github.com/Bit-manipulators/youtube-without-ads");
                return true;
            });
        }
    }

    private void openUrl(final String url) {
        try {
            final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (final Exception ignored) {
        }
    }
}
