/*
 * Copyright (C) 2020 Team Gateship-One
 * (Hendrik Borghorst & Frederik Luetkes)
 *
 * The AUTHORS.md file contains a detailed contributors list:
 * <https://github.com/gateship-one/odyssey/blob/master/AUTHORS.md>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.gateshipone.odyssey.fragments;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.activities.GenericActivity;
import org.gateshipone.odyssey.listener.OnRecentAlbumsSelectedListener;
import org.gateshipone.odyssey.listener.ToolbarAndFABCallback;
import org.gateshipone.odyssey.utils.ThemeUtils;
import org.gateshipone.odyssey.viewmodels.SearchViewModel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

public class MyMusicFragment extends Fragment {

    /**
     * Callback to open the recent albums fragment
     */
    private OnRecentAlbumsSelectedListener mRecentAlbumsSelectedListener;

    /**
     * Callback to setup toolbar and fab
     */
    protected ToolbarAndFABCallback mToolbarAndFABCallback;

    /**
     * Save the viewpager for later usage
     */
    private ViewPager2 mMyMusicViewPager;

    private ViewPager2PageChangeCallback mViewPagerCallback;

    /**
     * Save the pageradapter for later usage
     */
    private MyMusicPagerAdapter mMyMusicPagerAdapter;

    /**
     * Save the optionsmenu for later usage
     */
    private Menu mOptionMenu;

    /**
     * Saved search string when user rotates devices
     */
    private String mSearchString;

    private int mCurrentTab = -1;

    /**
     * Constant for state saving
     */
    public final static String MYMUSICFRAGMENT_SAVED_INSTANCE_SEARCH_STRING = "MyMusicFragment.SearchString";
    public final static String MYMUSICFRAGMENT_SAVED_INSTANCE_CURRENT_TAB = "MyMusicFragment.CurrentTab";

    /**
     * key value for arguments of the fragment
     */
    private final static String ARG_REQUESTED_TAB = "requested_tab";

    /**
     * enum for the default tab
     */
    public enum DEFAULTTAB {
        ARTISTS, ALBUMS, TRACKS
    }

    public static MyMusicFragment newInstance(final DEFAULTTAB defaulttab) {
        final Bundle args = new Bundle();
        args.putInt(ARG_REQUESTED_TAB, defaulttab.ordinal());

        final MyMusicFragment fragment = new MyMusicFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_music, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // create tabs
        final TabLayout tabLayout = view.findViewById(R.id.my_music_tab_layout);

        // setup viewpager
        mMyMusicViewPager = view.findViewById(R.id.my_music_viewpager);
        mMyMusicPagerAdapter = new MyMusicPagerAdapter(this);
        mMyMusicViewPager.setAdapter(mMyMusicPagerAdapter);

        mViewPagerCallback = new ViewPager2PageChangeCallback();

        mMyMusicViewPager.registerOnPageChangeCallback(mViewPagerCallback);

        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        final ColorStateList tabColors = tabLayout.getTabTextColors();

        new TabLayoutMediator(tabLayout, mMyMusicViewPager,
                (tab, position) -> {
                    // setup icon
                    final Resources res = getResources();
                    Drawable drawable = null;

                    switch (position) {
                        case 0:
                            drawable = ResourcesCompat.getDrawable(res, R.drawable.ic_recent_actors_24dp, null);
                            break;
                        case 1:
                            drawable = ResourcesCompat.getDrawable(res, R.drawable.ic_album_24dp, null);
                            break;
                        case 2:
                            drawable = ResourcesCompat.getDrawable(res, R.drawable.ic_my_library_music_24dp, null);
                            break;
                    }

                    if (drawable != null) {
                        Drawable icon = DrawableCompat.wrap(drawable);
                        DrawableCompat.setTintList(icon, tabColors);
                        tab.setIcon(icon);
                    }
                }
        ).attach();

        // try to resume the saved search string
        if (savedInstanceState != null) {
            mSearchString = savedInstanceState.getString(MYMUSICFRAGMENT_SAVED_INSTANCE_SEARCH_STRING);
            mCurrentTab = savedInstanceState.getInt(MYMUSICFRAGMENT_SAVED_INSTANCE_CURRENT_TAB);
            mMyMusicViewPager.setCurrentItem(mCurrentTab, false);
        }

        // activate options menu in toolbar
        setHasOptionsMenu(true);

        // set start page
        final Bundle args = getArguments();

        // only set requested tab if no state was saved
        if (args != null && savedInstanceState == null && mCurrentTab == -1) {
            final DEFAULTTAB tab = DEFAULTTAB.values()[args.getInt(ARG_REQUESTED_TAB)];

            switch (tab) {
                case ARTISTS:
                    mMyMusicViewPager.setCurrentItem(0, false);
                    break;
                case ALBUMS:
                    mMyMusicViewPager.setCurrentItem(1, false);
                    break;
                case TRACKS:
                    mMyMusicViewPager.setCurrentItem(2, false);
                    break;
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // save the already typed search string (or null if nothing is entered)
        outState.putString(MYMUSICFRAGMENT_SAVED_INSTANCE_SEARCH_STRING, mSearchString);
        outState.putInt(MYMUSICFRAGMENT_SAVED_INSTANCE_CURRENT_TAB, mCurrentTab);
    }

    /**
     * Called when the fragment is first attached to its context.
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interfaces. If not, it throws an exception

        try {
            mToolbarAndFABCallback = (ToolbarAndFABCallback) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement ToolbarAndFABCallback");
        }

        try {
            mRecentAlbumsSelectedListener = (OnRecentAlbumsSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnRecentAlbumsSelectedListener");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mMyMusicViewPager.unregisterOnPageChangeCallback(mViewPagerCallback);
    }

    /**
     * Setup toolbar and button callback in last creation state.
     */
    @Override
    public void onResume() {
        super.onResume();

        if (mToolbarAndFABCallback != null) {
            // set up play button
            mToolbarAndFABCallback.setupFAB(getPlayButtonListener(mMyMusicViewPager.getCurrentItem()));
            // set toolbar behaviour and title
            mToolbarAndFABCallback.setupToolbar(getString(R.string.fragment_title_my_music), true, true, false);
        }
    }

    /**
     * Create a ClickListener for the play button if needed.
     */
    private View.OnClickListener getPlayButtonListener(int tab) {
        switch (tab) {
            case 0:
            case 1:
                // add logic here if necessary
                return null;
            case 2:
                return v -> {
                    // play all tracks on device
                    try {
                        ((GenericActivity) requireActivity()).getPlaybackService().playAllTracks(mSearchString);
                    } catch (RemoteException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                };
            default:
                return null;
        }
    }

    /**
     * Method to reload the fragments.
     */
    public void refresh() {
        // reload tabs
        mMyMusicPagerAdapter.notifyDataSetChanged();
    }

    /**
     * Initialize the options menu.
     * Be sure to call {@link #setHasOptionsMenu} before.
     *
     * @param menu         The container for the custom options menu.
     * @param menuInflater The inflater to instantiate the layout.
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.options_menu_my_music, menu);

        mOptionMenu = menu;

        // get tint color
        final int tintColor = ThemeUtils.getThemeColor(requireContext(), R.attr.odyssey_color_text_accent);

        Drawable drawable = mOptionMenu.findItem(R.id.action_search).getIcon();
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, tintColor);
        mOptionMenu.findItem(R.id.action_search).setIcon(drawable);

        final SearchView searchView = (SearchView) mOptionMenu.findItem(R.id.action_search).getActionView();

        // Check if a search string is saved from before
        if (mSearchString != null) {
            // Expand the view
            searchView.setIconified(false);
            mOptionMenu.findItem(R.id.action_search).expandActionView();
            // Set the query string
            searchView.setQuery(mSearchString, true);
        }

        searchView.setOnQueryTextListener(new SearchTextObserver());

        // show recents options only for the albums fragment
        mOptionMenu.findItem(R.id.action_show_recent_albums).setVisible(mMyMusicViewPager.getCurrentItem() == 1);

        super.onCreateOptionsMenu(menu, menuInflater);
    }

    /**
     * Hook called when an menu item in the options menu is selected.
     *
     * @param item The menu item that was selected.
     * @return True if the hook was consumed here.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_show_recent_albums) {
            mRecentAlbumsSelectedListener.onRecentAlbumsSelected();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Custom pager adapter to retrieve already registered fragments.
     */
    private static class MyMusicPagerAdapter extends FragmentStateAdapter {
        static final int NUMBER_OF_PAGES = 3;

        public MyMusicPagerAdapter(Fragment parent) {
            super(parent);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return ArtistsFragment.newInstance();
                case 1:
                    return AlbumsFragment.newInstance();
                case 2:
                    return AllTracksFragment.newInstance();
                default:
                    // should not happen throw exception
                    throw new IllegalStateException("No fragment defined to return");
            }
        }

        @Override
        public int getItemCount() {
            return NUMBER_OF_PAGES;
        }
    }

    private class ViewPager2PageChangeCallback extends ViewPager2.OnPageChangeCallback {

        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);

            mCurrentTab = position;

            if (mToolbarAndFABCallback != null) {
                // show fab only for AllTracksFragment
                View.OnClickListener listener = getPlayButtonListener(position);

                // set up play button
                mToolbarAndFABCallback.setupFAB(listener);
            }

            if (mOptionMenu != null) {
                // show recents options only for the albums fragment
                final MenuItem item = mOptionMenu.findItem(R.id.action_show_recent_albums);
                if (item != null) {
                    item.setVisible(position == 1);
                }
            }
        }
    }

    /**
     * Observer class to apply a filter to the current fragment in the viewpager.
     */
    private class SearchTextObserver implements SearchView.OnQueryTextListener {

        @Override
        public boolean onQueryTextSubmit(String query) {
            applyFilter(query);

            return true;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            applyFilter(newText);

            return true;
        }

        private void applyFilter(String filter) {

            SearchViewModel searchViewModel = new ViewModelProvider(requireActivity()).get(SearchViewModel.class);

            if (filter.isEmpty()) {
                mSearchString = null;
                searchViewModel.clearSearchString();
            } else {
                mSearchString = filter;
                searchViewModel.setSearchString(filter);
            }
        }
    }
}
