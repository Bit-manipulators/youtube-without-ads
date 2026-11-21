package org.schabi.newpipe.fragments;

import static android.widget.RelativeLayout.ABOVE;
import static android.widget.RelativeLayout.ALIGN_PARENT_BOTTOM;
import static android.widget.RelativeLayout.ALIGN_PARENT_TOP;
import static android.widget.RelativeLayout.BELOW;
import static com.google.android.material.tabs.TabLayout.INDICATOR_GRAVITY_BOTTOM;
import static com.google.android.material.tabs.TabLayout.INDICATOR_GRAVITY_TOP;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapterMenuWorkaround;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.schabi.newpipe.BaseFragment;
import org.schabi.newpipe.MainActivity;
import org.schabi.newpipe.R;
import org.schabi.newpipe.databinding.FragmentMainBinding;
import org.schabi.newpipe.error.ErrorInfo;
import org.schabi.newpipe.error.ErrorUtil;
import org.schabi.newpipe.error.UserAction;
import org.schabi.newpipe.local.playlist.LocalPlaylistFragment;
import org.schabi.newpipe.settings.tabs.Tab;
import org.schabi.newpipe.settings.tabs.TabsManager;
import org.schabi.newpipe.util.NavigationHelper;
import org.schabi.newpipe.util.ServiceHelper;
import org.schabi.newpipe.util.ThemeHelper;
import org.schabi.newpipe.views.ScrollableTabLayout;

import java.util.ArrayList;
import java.util.List;

public class MainFragment extends BaseFragment implements TabLayout.OnTabSelectedListener {
    private static int pendingTabPosition = -1;

    public static void setPendingTab(final int position) {
        pendingTabPosition = position;
    }

    private FragmentMainBinding binding;
    private SelectedTabsPagerAdapter pagerAdapter;

    private final List<Tab> tabsList = new ArrayList<>();
    private TabsManager tabsManager;

    private boolean hasTabsChanged = false;

    private SharedPreferences prefs;
    private boolean youtubeRestrictedModeEnabled;
    private String youtubeRestrictedModeEnabledKey;
    private boolean mainTabsPositionBottom;
    private String mainTabsPositionKey;

    /*//////////////////////////////////////////////////////////////////////////
    // Fragment's LifeCycle
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        tabsManager = TabsManager.getManager(activity);
        tabsManager.setSavedTabsListener(() -> {
            if (DEBUG) {
                Log.d(TAG, "TabsManager.SavedTabsChangeListener: "
                        + "onTabsChanged called, isResumed = " + isResumed());
            }
            if (isResumed()) {
                setupTabs();
            } else {
                hasTabsChanged = true;
            }
        });

        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        youtubeRestrictedModeEnabledKey = getString(R.string.youtube_restricted_mode_enabled);
        youtubeRestrictedModeEnabled = prefs.getBoolean(youtubeRestrictedModeEnabledKey, false);
        mainTabsPositionKey = getString(R.string.main_tabs_position_key);
        mainTabsPositionBottom = true;
        prefs.edit().putBoolean(mainTabsPositionKey, true).apply();
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Nullable
    private ScrollableTabLayout getTabLayout() {
        if (activity instanceof MainActivity) {
            return ((MainActivity) activity).getMainTabLayout();
        }
        return null;
    }

    @Override
    protected void initViews(final View rootView, final Bundle savedInstanceState) {
        super.initViews(rootView, savedInstanceState);

        binding = FragmentMainBinding.bind(rootView);
        binding.pager.setOffscreenPageLimit(4);

        binding.pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(final int position) {
                updateTitleForTab(position);
                if (activity instanceof MainActivity) {
                    ((MainActivity) activity).setBarsVisible(true);
                }
                final ScrollableTabLayout tl = getTabLayout();
                if (tl != null) {
                    final TabLayout.Tab tab = tl.getTabAt(position);
                    if (tab != null && !tab.isSelected()) {
                        tab.select();
                    }
                }
            }
        });

        setupTabs();
        updateTabLayoutPosition();
    }

    @Override
    public void onResume() {
        super.onResume();

        final boolean newYoutubeRestrictedModeEnabled =
                prefs.getBoolean(youtubeRestrictedModeEnabledKey, false);
        if (youtubeRestrictedModeEnabled != newYoutubeRestrictedModeEnabled || hasTabsChanged) {
            youtubeRestrictedModeEnabled = newYoutubeRestrictedModeEnabled;
            setupTabs();
        }

        mainTabsPositionBottom = true;
        prefs.edit().putBoolean(mainTabsPositionKey, true).apply();
        updateTabLayoutPosition();
        final ScrollableTabLayout tabLayout = getTabLayout();
        if (tabLayout != null) {
            updateTabsIconAndDescription();
            if (binding != null && binding.pager != null) {
                if (pendingTabPosition >= 0) {
                    final int target = pendingTabPosition;
                    pendingTabPosition = -1;
                    setSelectedTab(target);
                    final TabLayout.Tab tab = tabLayout.getTabAt(target);
                    if (tab != null && !tab.isSelected()) {
                        tab.select();
                    }
                } else {
                    final TabLayout.Tab tab = tabLayout.getTabAt(binding.pager.getCurrentItem());
                    if (tab != null && !tab.isSelected()) {
                        tab.select();
                    }
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        tabsManager.unsetSavedTabsListener();
        if (binding != null) {
            binding.pager.setAdapter(null);
            binding = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null && binding.pager != null) {
            binding.pager.setAdapter(null);
        }
        pagerAdapter = null;
        binding = null;
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Menu
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void onCreateOptionsMenu(@NonNull final Menu menu,
                                    @NonNull final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (DEBUG) {
            Log.d(TAG, "onCreateOptionsMenu() called with: "
                    + "menu = [" + menu + "], inflater = [" + inflater + "]");
        }
        inflater.inflate(R.menu.menu_main_fragment, menu);

        final ActionBar supportActionBar = activity.getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.action_search) {
            try {
                NavigationHelper.openSearchFragment(getFM(),
                        ServiceHelper.getSelectedServiceId(activity), "");
            } catch (final Exception e) {
                ErrorUtil.showUiErrorSnackbar(this, "Opening search fragment", e);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Tabs
    //////////////////////////////////////////////////////////////////////////*/

    private void setupTabs() {
        tabsList.clear();
        tabsList.addAll(tabsManager.getTabs());

        pagerAdapter = new SelectedTabsPagerAdapter(requireContext(),
                getChildFragmentManager(), tabsList);
        binding.pager.setAdapter(pagerAdapter);

        final ScrollableTabLayout tabLayout = getTabLayout();
        if (tabLayout != null) {
            if (tabLayout.getTabCount() != tabsList.size()) {
                tabLayout.removeAllTabs();
                for (int i = 0; i < tabsList.size(); i++) {
                    final Tab tab = tabsList.get(i);
                    final TabLayout.Tab newTab = tabLayout.newTab();
                    newTab.setIcon(tab.getTabIconRes(requireContext()));
                    newTab.setContentDescription(tab.getTabName(requireContext()));
                    tabLayout.addTab(newTab, i == binding.pager.getCurrentItem());
                }
            } else {
                updateTabsIconAndDescription();
            }
        }

        updateTitleForTab(binding.pager.getCurrentItem());
        hasTabsChanged = false;
    }

    private void updateTabsIconAndDescription() {
        final ScrollableTabLayout tabLayout = getTabLayout();
        if (tabLayout == null) {
            return;
        }
        for (int i = 0; i < tabsList.size(); i++) {
            final TabLayout.Tab tabToSet = tabLayout.getTabAt(i);
            if (tabToSet != null) {
                final Tab tab = tabsList.get(i);
                tabToSet.setIcon(tab.getTabIconRes(requireContext()));
                tabToSet.setText(null); // Remove text labels for polished icon-only dock
                tabToSet.setContentDescription(tab.getTabName(requireContext()));
            }
        }
    }

    private void updateTitleForTab(final int tabPosition) {
        final ActionBar supportActionBar = activity.getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayShowTitleEnabled(false);
            supportActionBar.setTitle(null);
        }
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).updateToolbarBranding(isHistoryTab(tabPosition));
        }
    }

    public boolean isCurrentTabHistory() {
        if (binding == null || binding.pager == null) {
            return false;
        }
        return isHistoryTab(binding.pager.getCurrentItem());
    }

    public boolean isHistoryTab(final int tabPosition) {
        if (tabsList != null && tabPosition >= 0 && tabPosition < tabsList.size()) {
            final Tab tab = tabsList.get(tabPosition);
            return tab instanceof Tab.HistoryTab || tab.getTabId() == Tab.HistoryTab.ID;
        }
        return false;
    }

    @Nullable
    public View getBottomBar() {
        return activity instanceof MainActivity ? ((MainActivity) activity).getBottomDockContainer() : null;
    }

    public void setSelectedTab(final int position) {
        if (binding != null && binding.pager != null && pagerAdapter != null
                && position >= 0 && position < pagerAdapter.getCount()) {
            binding.pager.setCurrentItem(position, false);
            updateTitleForTab(position);
        }
    }

    public void commitPlaylistTabs() {
        pagerAdapter.getLocalPlaylistFragments()
                .stream()
                .forEach(LocalPlaylistFragment::saveImmediate);
    }

    private void updateTabLayoutPosition() {
        final ScrollableTabLayout tabLayout = getTabLayout();
        if (tabLayout == null) {
            return;
        }
        mainTabsPositionBottom = true;

        tabLayout.setSelectedTabIndicatorGravity(TabLayout.INDICATOR_GRAVITY_CENTER);
        final int indicatorHeight = (int) android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP, 44, getResources().getDisplayMetrics());
        tabLayout.setSelectedTabIndicatorHeight(indicatorHeight);

        // Keep tab layout transparent so the glass dock background shows through:
        tabLayout.setBackgroundColor(Color.TRANSPARENT);

        final int selectedColor = ThemeHelper.resolveColorFromAttr(requireContext(),
                android.R.attr.textColorPrimary);
        final int unselectedColor = ThemeHelper.resolveColorFromAttr(requireContext(),
                android.R.attr.textColorSecondary);
        final int[][] states = new int[][]{
                new int[]{android.R.attr.state_selected},
                new int[]{}
        };
        final int[] colors = new int[]{selectedColor, unselectedColor};
        final ColorStateList tabColors = new ColorStateList(states, colors);
        tabLayout.setTabIconTint(tabColors);
        tabLayout.setTabTextColors(unselectedColor, selectedColor);
        tabLayout.setTabRippleColor(null);
    }

    @Override
    public void onTabSelected(final TabLayout.Tab selectedTab) {
        if (DEBUG) {
            Log.d(TAG, "onTabSelected() called with: selectedTab = [" + selectedTab + "]");
        }
        updateTitleForTab(selectedTab.getPosition());
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).setBarsVisible(true);
        }
    }

    @Override
    public void onTabUnselected(final TabLayout.Tab tab) { }

    @Override
    public void onTabReselected(final TabLayout.Tab tab) {
        if (DEBUG) {
            Log.d(TAG, "onTabReselected() called with: tab = [" + tab + "]");
        }
        updateTitleForTab(tab.getPosition());
    }

    public static final class SelectedTabsPagerAdapter
            extends FragmentStatePagerAdapterMenuWorkaround {
        private final Context context;
        private final List<Tab> internalTabsList;
        /**
         * Keep reference to LocalPlaylistFragments, because their data can be modified by the user
         * during runtime and changes are not committed immediately. However, in some cases,
         * the changes need to be committed immediately by calling
         * {@link LocalPlaylistFragment#saveImmediate()}.
         * The fragments are removed when {@link LocalPlaylistFragment#onDestroy()} is called.
         */
        private final List<LocalPlaylistFragment> localPlaylistFragments = new ArrayList<>();

        private SelectedTabsPagerAdapter(final Context context,
                                         final FragmentManager fragmentManager,
                                         final List<Tab> tabsList) {
            super(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            this.context = context;
            this.internalTabsList = new ArrayList<>(tabsList);
        }

        @NonNull
        @Override
        public Fragment getItem(final int position) {
            final Tab tab = internalTabsList.get(position);

            final Fragment fragment;
            try {
                fragment = tab.getFragment(context);
            } catch (final Throwable t) {
                return new BlankFragment(new ErrorInfo(t, UserAction.GETTING_MAIN_SCREEN_TAB,
                        "Tab " + tab.getClass().getSimpleName() + ":" + tab.getTabName(context)));
            }

            if (fragment instanceof BaseFragment) {
                ((BaseFragment) fragment).useAsFrontPage(true);
            }

            if (fragment instanceof LocalPlaylistFragment) {
                localPlaylistFragments.add((LocalPlaylistFragment) fragment);
            }

            return fragment;
        }

        public List<LocalPlaylistFragment> getLocalPlaylistFragments() {
            return localPlaylistFragments;
        }

        @Override
        public int getItemPosition(@NonNull final Object object) {
            // Causes adapter to reload all Fragments when
            // notifyDataSetChanged is called
            return POSITION_NONE;
        }

        @Override
        public int getCount() {
            return internalTabsList.size();
        }

        public boolean sameTabs(final List<Tab> tabsToCompare) {
            return internalTabsList.equals(tabsToCompare);
        }

        @Override
        public void restoreState(@Nullable final Parcelable state,
                                 @Nullable final ClassLoader loader) {
            try {
                super.restoreState(state, loader);
            } catch (final Exception e) {
                Log.w(TAG, "Failed to restore state for SelectedTabsPagerAdapter", e);
            }
        }
    }
}
