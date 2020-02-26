package com.firekernel.musicplayer.utils;

import android.support.annotation.NonNull;

/**
 * Utility class to help on queue related tasks.
 */
public class MediaIDHelper {

    // Media IDs used on browse items of MediaBrowser
    public static final String MEDIA_ID_EMPTY_ROOT = "_EMPTY_ROOT_";
    public static final String MEDIA_ID_ROOT = "_ROOT_";
    public static final String MEDIA_ID_PLAYLIST = "_BY_PLAYLIST_";
    public static final String MEDIA_ID_TRACKS = "_BY_TRACK_"; // track is special category where no further subcategory
    public static final String MEDIA_ID_TRACKS_ALL = "_ALL_TRACK_"; // use this for subcategory for track
    public static final String MEDIA_ID_ALBUM = "_BY_ALBUM_";
    public static final String MEDIA_ID_ARTIST = "_BY_ARTIST_";
    public static final String MEDIA_ID_GENRE = "_BY_GENRE_";
    public static final String MEDIA_ID_FOLDER = "_BY_FOLDER_";

    public static final String MEDIA_ID_SEARCH = "_SEARCH_";

    private static final char CATEGORY_SEPARATOR = ',';
    private static final char LEAF_SEPARATOR = '|';

    /**
     * Create a String value that represents a playable or a browsable media.
     * <p>
     * Encode the media browseable categories, if any, and the unique music ID, if any,
     * into a single String mediaID.
     * <p>
     * MediaIDs are of the form <categoryType>,<categoryValue>|<musicUniqueId>, to make it easy
     * to find the category (like genre) that a music was selected from, so we
     * can correctly build the playing queue. This is specially useful when
     * one music can appear in more than one list.
     *
     * @param musicID    Unique music ID for playable items, or null for browseable items.
     * @param categories hierarchy of categories representing this item's browsing parents
     * @return a hierarchy-aware media ID
     */
    public static String createMediaID(String musicID, String... categories) {
        StringBuilder sb = new StringBuilder();
        if (categories != null) {
            for (int i = 0; i < categories.length; i++) {
                if (!isValidCategory(categories[i])) {
                    throw new IllegalArgumentException("Invalid category: " + categories[i]);
                }
                sb.append(categories[i]);
                if (i < categories.length - 1) {
                    sb.append(CATEGORY_SEPARATOR);
                }
            }
        }
        if (musicID != null) {
            sb.append(LEAF_SEPARATOR).append(musicID);
        }
        return sb.toString();
    }

    /**
     * A Category is valid only when there is no category separator and no leaf separator
     */
    private static boolean isValidCategory(String category) {
        return category == null || (category.indexOf(CATEGORY_SEPARATOR) < 0 && category.indexOf(LEAF_SEPARATOR) < 0);
    }

    /**
     * Extracts unique musicID from the mediaID. mediaID is, by this sample's convention, a
     * concatenation of category (eg "by_genre"), categoryValue (eg "Classical") and unique
     * musicID. This is necessary so we know where the user selected the music from, when the music
     * exists in more than one music list, and thus we are able to correctly build the playing queue.
     *
     * @param mediaID that contains the musicID
     * @return musicID
     */
    public static String extractMusicIDFromMediaID(@NonNull String mediaID) {
        int pos = mediaID.indexOf(LEAF_SEPARATOR);
        if (pos >= 0) {
            return mediaID.substring(pos + 1);
        }
        return null;
    }

    /**
     * Extracts category and categoryValue from the mediaID. mediaID is, by this sample's
     * convention, a concatenation of category (eg "by_genre"), categoryValue (eg "Classical") and
     * mediaID. This is necessary so we know where the user selected the music from, when the music
     * exists in more than one music list, and thus we are able to correctly build the playing queue.
     *
     * @param mediaID that contains a category and categoryValue.
     */
    public static
    @NonNull
    String[] getHierarchy(@NonNull String mediaID) {
        int pos = mediaID.indexOf(LEAF_SEPARATOR);
        if (pos >= 0) {
            mediaID = mediaID.substring(0, pos);
        }
        return mediaID.split(String.valueOf(CATEGORY_SEPARATOR));
    }

}
