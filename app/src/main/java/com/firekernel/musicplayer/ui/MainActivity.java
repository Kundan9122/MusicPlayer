package com.firekernel.musicplayer.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.firekernel.musicplayer.FirePopupMenuSelectedListener;
import com.firekernel.musicplayer.R;
import com.firekernel.musicplayer.ui.fragment.CategoryFragment;
import com.firekernel.musicplayer.ui.fragment.MediaListFragment;
import com.firekernel.musicplayer.ui.fragment.PlaybackControlsFragment;
import com.firekernel.musicplayer.ui.widget.AboutMe;
import com.firekernel.musicplayer.utils.ActionHelper;
import com.firekernel.musicplayer.utils.FireLog;
import com.firekernel.musicplayer.utils.ImageHelper;
import com.firekernel.musicplayer.utils.MediaIDHelper;

public class MainActivity extends PlaybackBaseActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        CategoryFragment.OnCategorySelectedListener,
        MediaListFragment.OnMediaItemSelectedListener,
        FirePopupMenuSelectedListener {

    private static final String TAG = FireLog.makeLogTag(MainActivity.class);

    private ImageView headerBgView;
    private ImageView bgView;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private View headerView;
    private String title;
    private int itemId;
    private String mArtUrl = ""; //do not set null
    private final MediaControllerCompat.Callback mediaControllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            FireLog.d(TAG, "(++) onPlaybackStateChanged state= " + state.getState());
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (metadata == null) {
                FireLog.e(TAG, "(++) MediaController.Callback.onMetadataChanged: metadata is null");
                return;
            }
            MainActivity.this.onMetadataChanged(metadata); // always use this context
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        bgView = (ImageView) findViewById(R.id.bgView);
//        ImageHelper.loadBlurBg(this, bgView);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.nav_drawer_open, R.string.nav_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        headerView = navigationView.getHeaderView(0);
        headerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNavigationItemSelected(v.getId());
            }
        });

        loadNavigationHeaderView(headerView);
        if (savedInstanceState == null) {
            // Set the default view when activity is launched on the first time
            loadPrimaryView();
            // Only check if a Now Playing is needed on the first time
            ActionHelper.startNowPlayingActivityIfNeeded(this, getIntent());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        FireLog.d(TAG, "(++) onStart");
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(this);
        if (controller != null) {
            onConnected();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        FireLog.d(TAG, "(++) onNewIntent, intent=" + intent);
        ActionHelper.startNowPlayingActivityIfNeeded(this, intent);
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_search:
                ActionHelper.startSearchActivity(this);
                return true;
            case R.id.action_audio_effects:
                ActionHelper.startAudioEffectActivity(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        FireLog.d(TAG, "(++) onStop");
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(this);
        if (controller != null) {
            FireLog.d(TAG, "Unregister callback=" + mediaControllerCallback);
            controller.unregisterCallback(mediaControllerCallback);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onMediaControllerConnected() {

        // connect CategoryFragment
        Fragment fragmentCategory = getCategoryFragment();
        if (fragmentCategory != null) {
            ((CategoryFragment) fragmentCategory).onConnected();
        }

        // connect MediaListFragment
        Fragment fragment = getMediaListFragment();
        if (fragment != null) {
            ((MediaListFragment) fragment).onConnected();
        }

        Fragment fragmentControl = getControlFragment();
        if (fragmentControl != null) {
            ((PlaybackControlsFragment) fragmentControl).onConnected();
        }

        // connect activity to receive callback
        this.onConnected();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        onNavigationItemSelected(id);
        return true;
    }

    @Override
    public void onCategorySelected(MediaBrowserCompat.MediaItem mediaItem) {
        FireLog.d(TAG, "(++) onCategorySelected, mediaItem= " + mediaItem);
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(CategoryFragment.TAG);
        if (fragment != null && fragment instanceof CategoryFragment) {
            title = mediaItem.getDescription().getTitle() + "";
            fragment = MediaListFragment.newInstance(title, mediaItem.getMediaId());
            String tag = MediaListFragment.TAG;

            getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(
                            R.anim.slide_in_from_right, R.anim.slide_out_to_left,
                            R.anim.slide_in_from_left, R.anim.slide_out_to_right)
                    .replace(R.id.flContent, fragment, tag)
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    public void onMediaItemSelected(MediaBrowserCompat.MediaItem item) {
        FireLog.d(TAG, "(++) onMediaItemSelected, mediaitem=" + item);
        if (item.isPlayable()) {
            MediaControllerCompat.getMediaController(this).getTransportControls()
                    .playFromMediaId(item.getMediaId(), null);
        }
    }

    @Override
    public void onPlaySelected(MediaBrowserCompat.MediaItem item) {
        FireLog.d(TAG, "(++) onPlaySelected");
        if (item.isPlayable()) {
            MediaControllerCompat.getMediaController(this).getTransportControls()
                    .playFromMediaId(item.getMediaId(), null);
        }
    }

    @Override
    public void onShareSelected(MediaBrowserCompat.MediaItem item) {
        ActionHelper.shareTrack(this, item.getDescription());
//        ActionHelper.shareTrack(this, item.getDescription().getMediaId());
    }

    private CategoryFragment getCategoryFragment() {
        return (CategoryFragment) getSupportFragmentManager().findFragmentByTag(CategoryFragment.TAG);
    }

    private MediaListFragment getMediaListFragment() {
        return (MediaListFragment) getSupportFragmentManager().findFragmentByTag(MediaListFragment.TAG);
    }

    private PlaybackControlsFragment getControlFragment() {
        return (PlaybackControlsFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_playback_controls);
    }

    private void loadPrimaryView() {
        FireLog.d(TAG, "(++) loadPrimaryView");
        onNavigationItemSelectedForFragment(R.id.nav_tracks);
    }

    /**
     * Responsible for showing appropriate content in navigation header view ie current user
     *
     * @param headerView
     */
    private void loadNavigationHeaderView(final View headerView) {
        FireLog.d(TAG, "(++) loadNavigationHeaderView");

        if (headerView == null) {
            return;
        }

        headerBgView = (ImageView) headerView.findViewById(R.id.bgView);
    }

    private void onNavigationItemSelected(final int id) {
        FireLog.d(TAG, "(++) onNavigationItemSelected, id=" + id);
        drawer.closeDrawer(GravityCompat.START);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                switch (id) {
                    case R.id.nav_header:
                    case R.id.nav_playlist:
                    case R.id.nav_tracks:
                    case R.id.nav_albums:
                    case R.id.nav_artists:
                    case R.id.nav_genres:
                    case R.id.nav_folders:
                    case R.id.about_us:

                        onNavigationItemSelectedForFragment(id);
                        break;
                }
            }
        }, 300);
    }

    private void onNavigationItemSelectedForFragment(int id) {
        if (id == itemId) {
            return;
        }
        itemId = id;
        Fragment fragment = null;
        String tag = null;

        switch (id) {
            case R.id.nav_header:
                title = getResources().getString(R.string.nav_menu_login);
                fragment = MediaListFragment.newInstance(title, MediaIDHelper.MEDIA_ID_TRACKS);
                unCheckAllMenuItems(navigationView);

                break;
            case R.id.nav_playlist:
                title = getResources().getString(R.string.nav_menu_playlists);
                fragment = CategoryFragment.newInstance(title, MediaIDHelper.MEDIA_ID_PLAYLIST);
                tag = CategoryFragment.TAG;
                break;
            case R.id.nav_tracks:
                title = getResources().getString(R.string.nav_menu_tracks);
                fragment = MediaListFragment.newInstance(title, MediaIDHelper.MEDIA_ID_TRACKS);
                tag = MediaListFragment.TAG;
                break;
            case R.id.nav_albums:
                title = getResources().getString(R.string.nav_menu_albums);
                fragment = CategoryFragment.newInstance(title, MediaIDHelper.MEDIA_ID_ALBUM);
                tag = CategoryFragment.TAG;
                break;
            case R.id.nav_artists:
                title = getResources().getString(R.string.nav_menu_artists);
                fragment = CategoryFragment.newInstance(title, MediaIDHelper.MEDIA_ID_ARTIST);
                tag = CategoryFragment.TAG;
                break;
            case R.id.nav_genres:
                title = getResources().getString(R.string.nav_menu_genre);
                fragment = CategoryFragment.newInstance(title, MediaIDHelper.MEDIA_ID_GENRE);
                tag = CategoryFragment.TAG;
                break;
            case R.id.nav_folders:
                title = getResources().getString(R.string.nav_menu_folders);
                fragment = CategoryFragment.newInstance(title, MediaIDHelper.MEDIA_ID_FOLDER);
                tag = CategoryFragment.TAG;
                break;
            case R.id.about_us:
                title = "About Me";
                Intent intObj = new Intent(MainActivity.this, AboutMe.class);
                startActivity(intObj);
                //fragment = CategoryFragment.newInstance(title, MediaIDHelper.MEDIA_ID_EMPTY_ROOT);
                tag = CategoryFragment.TAG;
                break;

        }

        for (int i = 0; i < getSupportFragmentManager().getBackStackEntryCount(); i++) {
            getSupportFragmentManager().popBackStack();
        }
        // Insert the fragment by replacing any existing fragment
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.flContent, fragment, tag)
                .commit();
    }

    private void onConnected() {
        FireLog.d(TAG, "onConnected");
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(this);
        if (controller != null) {
            MainActivity.this.onMetadataChanged(controller.getMetadata());
            FireLog.d(TAG, "Register callback=" + mediaControllerCallback);
            controller.registerCallback(mediaControllerCallback);
        }
    }

    private void onMetadataChanged(MediaMetadataCompat metadata) {
        FireLog.d(TAG, "(++) onMetadataChanged " + metadata);

        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (metadata == null) {
            ImageHelper.loadBlurBg(this, headerBgView);
            ImageHelper.loadBlurBg(this, bgView);
            return;
        }

        // metadata change is called so frequent// preventing image loading
        String artUrl = null;
        if (metadata.getDescription().getIconUri() != null) {
            artUrl = metadata.getDescription().getIconUri().toString();
        }
        FireLog.d(TAG, "mArtUrl=" + mArtUrl + ", artUrl=" + artUrl);
        if (!TextUtils.equals(artUrl, mArtUrl)) {
            mArtUrl = artUrl;
            ImageHelper.loadBlurBg(this, bgView, metadata.getDescription());
            ImageHelper.loadBlurBg(this, headerBgView, metadata.getDescription());
        }
    }

    private void unCheckAllMenuItems(NavigationView navigationView) {
        final Menu menu = navigationView.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
//            if (item.hasSubMenu()) {
//                SubMenu subMenu = item.getSubMenu();
//                for (int j = 0; j < subMenu.size(); j++) {
//                    MenuItem subMenuItem = subMenu.getItem(j);
//                    subMenuItem.setChecked(false);
//                }
//            } else {
            item.setChecked(false);
//            }
        }
    }
}

