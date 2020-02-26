package com.firekernel.musicplayer.source;

import android.support.v4.media.MediaMetadataCompat;

import java.util.Iterator;

public interface MusicProviderSource {

    Iterator<MediaMetadataCompat> iterator(String mediaId);
}
