package com.firekernel.musicplayer.source;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.media.MediaMetadataCompat;

import com.firekernel.musicplayer.FireApplication;
import com.firekernel.musicplayer.utils.FireLog;
import com.firekernel.musicplayer.utils.MediaIDHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.firekernel.musicplayer.utils.MediaIDHelper.MEDIA_ID_ALBUM;
import static com.firekernel.musicplayer.utils.MediaIDHelper.MEDIA_ID_ARTIST;
import static com.firekernel.musicplayer.utils.MediaIDHelper.MEDIA_ID_FOLDER;
import static com.firekernel.musicplayer.utils.MediaIDHelper.MEDIA_ID_GENRE;
import static com.firekernel.musicplayer.utils.MediaIDHelper.MEDIA_ID_PLAYLIST;
import static com.firekernel.musicplayer.utils.MediaIDHelper.MEDIA_ID_ROOT;
import static com.firekernel.musicplayer.utils.MediaIDHelper.MEDIA_ID_TRACKS;



public class LocalSource implements MusicProviderSource {
    private static final String TAG = FireLog.makeLogTag(LocalSource.class);

    public LocalSource() {
    }

    @Override
    public Iterator<MediaMetadataCompat> iterator(String mediaId) {
        FireLog.d(TAG, "(++) iterator, mediaId=" + mediaId);

        if (MEDIA_ID_ROOT.equals(mediaId)) {
            return new ArrayList<MediaMetadataCompat>().iterator();

        } else if (MEDIA_ID_TRACKS.equals(mediaId)) {
            List<MediaMetadataCompat> tracks = getLocalTracks();
            return tracks.iterator();
        } else if (MEDIA_ID_PLAYLIST.equals(mediaId)) {
            return getLocalPlayLists().iterator();
        } else if (MEDIA_ID_ALBUM.equals(mediaId)) {
            return getLocalAlbums().iterator();
        } else if (MEDIA_ID_ARTIST.equals(mediaId)) {
            return getLocalArtists().iterator();
        } else if (MEDIA_ID_GENRE.equals(mediaId)) {
            return getLocalGenres().iterator();
        } else if (MEDIA_ID_FOLDER.equals(mediaId)) {
            return getLocalFolders().iterator();
        } else if (mediaId.startsWith(MEDIA_ID_PLAYLIST) || mediaId.startsWith(MEDIA_ID_ALBUM)
                || mediaId.startsWith(MEDIA_ID_ARTIST) || mediaId.startsWith(MEDIA_ID_GENRE)
                || mediaId.startsWith(MEDIA_ID_FOLDER)) {
            String category = MediaIDHelper.getHierarchy(mediaId)[0];
            String subCategory = MediaIDHelper.getHierarchy(mediaId)[1];
            return getTracksBySubCategory(category, subCategory).iterator();
        } else {
            FireLog.w(TAG, "unmatched mediaId: " + mediaId);
            return new ArrayList<MediaMetadataCompat>().iterator();
        }
    }

    private List<MediaMetadataCompat> getLocalPlayLists() {
        FireLog.d(TAG, "(++) getLocalPlayLists");

        List<MediaMetadataCompat> playlists = new ArrayList<>();
        Uri playlistUri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
        String[] projection = {"*"};
        String selection = null;
        String[] selectionArgs = null;
        String sortOrder = MediaStore.Audio.Playlists.NAME + " ASC";

        Cursor cursor = FireApplication.getInstance().getContentResolver()
                .query(playlistUri, projection, selection, selectionArgs, sortOrder);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    MediaMetadataCompat mediaMetadataCompat = new MediaMetadataCompat.Builder()
                            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Playlists._ID)))
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Playlists.NAME)))
                            .build();
                    playlists.add(mediaMetadataCompat);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return playlists;
    }

    private List<MediaMetadataCompat> getLocalTracks() {
        FireLog.d(TAG, "(++) getLocalTracks");

        return getTracksBySubCategory(MediaIDHelper.MEDIA_ID_TRACKS, MediaIDHelper.MEDIA_ID_TRACKS_ALL);
    }


    private List<MediaMetadataCompat> getLocalAlbums() {
        FireLog.d(TAG, "(++) getLocalAlbums");

        List<MediaMetadataCompat> albums = new ArrayList<>();
        Uri albumUri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
        String[] projection = {"*"};
        String selection = null;
        String[] selectionArgs = null;
        String sortOrder = MediaStore.Audio.Albums.ALBUM + " ASC";

        Cursor cursor = FireApplication.getInstance().getContentResolver()
                .query(albumUri, projection, selection, selectionArgs, sortOrder);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    MediaMetadataCompat mediaMetadataCompat = new MediaMetadataCompat.Builder()
                            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums._ID)))
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM)))
                            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART)))
                            .build();
                    albums.add(mediaMetadataCompat);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return albums;
    }

    private List<MediaMetadataCompat> getLocalArtists() {
        FireLog.d(TAG, "(++) getLocalArtists");
        List<MediaMetadataCompat> artists = new ArrayList<>();
        Uri albumUri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
        String[] projection = {"*"};
        String selection = null;
        String[] selectionArgs = null;
        String sortOrder = MediaStore.Audio.Artists.ARTIST + " ASC";

        Cursor cursor = FireApplication.getInstance().getContentResolver()
                .query(albumUri, projection, selection, selectionArgs, sortOrder);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    MediaMetadataCompat mediaMetadataCompat = new MediaMetadataCompat.Builder()
                            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists._ID)))
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST)))
                            .build();
                    artists.add(mediaMetadataCompat);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        return artists;
    }

    private List<MediaMetadataCompat> getLocalGenres() {
        FireLog.d(TAG, "(++) getLocalGenres");

        List<MediaMetadataCompat> genres = new ArrayList<>();
        Uri albumUri = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI;
        String[] projection = {"*"};
        String selection = null;
        String[] selectionArgs = null;
        String sortOrder = MediaStore.Audio.Genres.NAME + " ASC";

        Cursor cursor = FireApplication.getInstance().getContentResolver()
                .query(albumUri, projection, selection, selectionArgs, sortOrder);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    MediaMetadataCompat mediaMetadataCompat = new MediaMetadataCompat.Builder()
                            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Genres._ID)))
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Genres.NAME)))
                            .build();
                    genres.add(mediaMetadataCompat);
                } while (cursor.moveToNext());
            }
            cursor.close();

        }
        return genres;
    }

    // filter genre to remove genre with no playable media item
//    private List<MediaMetadataCompat> filterGenres(List<MediaMetadataCompat> genres) {
//        FireLog.d(TAG, "(++) filterGenres");
//
//        if (genres == null) {
//            return null;
//        }
//        Iterator iterator = genres.iterator();
//        while (iterator.hasNext()) {
//            MediaMetadataCompat genre = (MediaMetadataCompat) iterator.next();
//            Uri songsUri = getUri(genre);
//            String[] projection = {
//                    MediaStore.Audio.Media._ID
//            };
//            String selection = null;
//            String[] selectionArgs = null;
//            String sortOrder = null;
//
//            Cursor cursor = FireApplication.getInstance().getContentResolver()
//                    .query(songsUri, projection, selection, selectionArgs, sortOrder);
//
//            if (cursor != null) {
//                if (!cursor.moveToFirst()) {
//                    iterator.remove();
//                }
//                cursor.close();
//            }
//        }
//        return genres;
//    }

    private List<MediaMetadataCompat> getLocalFolders() {
        FireLog.d(TAG, "(++) getLocalFolders");
        List<MediaMetadataCompat> folders = new ArrayList<>();

        Uri songsUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {"*"};
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != ?";
        String[] selectionArgs = {"0"};
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        Cursor cursor = FireApplication.getInstance().getContentResolver()
                .query(songsUri, projection, selection, selectionArgs, sortOrder);

        // Using set for finding distinct folder
        Set<String> set = new HashSet<>();
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                    String path = data.substring(0, data.lastIndexOf("/"));
                    if (set.add(path)) {
                        MediaMetadataCompat mediaMetadataCompat = new MediaMetadataCompat.Builder()
                                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                                        path)
                                .putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                                        path.substring(path.lastIndexOf("/") + 1, path.length()))
                                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                                        getAlbumArt(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))))
                                .build();
                        folders.add(mediaMetadataCompat);
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return folders;
    }

    private List<MediaMetadataCompat> getTracksBySubCategory(String category, String subCategory) {
        FireLog.d(TAG, "(++) getTracksBySubCategory, category=" + category + ", subCategory=" + subCategory);

        if (category == null || subCategory == null) {
            return null;
        }

        List<MediaMetadataCompat> medias = new ArrayList<>();

        Uri songsUri = getUri(category, subCategory);
        String[] projection = getProjection(category, subCategory);
        String selection = getSelection(category, subCategory);
        String[] selectionArgs = null;
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        String idColumnName = get_IDColumnName(category);

        Cursor cursor = FireApplication.getInstance().getContentResolver()
                .query(songsUri, projection, selection, selectionArgs, sortOrder);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    MediaMetadataCompat mediaMetadataCompat = new MediaMetadataCompat.Builder()
                            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                                    cursor.getString(cursor.getColumnIndex(idColumnName)))
                            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI,
                                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)))
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)))
                            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)))
                            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION,
                                    cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)))
                            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                                    getAlbumArt(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))))
                            .build();
                    medias.add(mediaMetadataCompat);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        return medias;
    }

    // uri and selection combination filters the media by subcategory
    private Uri getUri(String category, String subCategory) {
        FireLog.d(TAG, "(++) getUri, category=" + category + ", subCategory=" + subCategory);
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        if (category.equals(MEDIA_ID_PLAYLIST)) {
            long playListId = Long.parseLong(subCategory);
            uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playListId);
        } else if (category.equals(MEDIA_ID_GENRE)) {
            long genreId = Long.parseLong(subCategory);
            uri = MediaStore.Audio.Genres.Members.getContentUri("external", genreId);
        }
        return uri;
    }

    private String[] getProjection(String category, String subCategory) {

        if (category.equals(MEDIA_ID_PLAYLIST)) {
            String[] projection = {
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Playlists.Members.AUDIO_ID,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.DATE_ADDED,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.ALBUM_ID,
                    MediaStore.Audio.Media.YEAR,
                    MediaStore.Audio.Media.ARTIST};
            return projection;
        } else {
            String[] projection = {
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.DATE_ADDED,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.ALBUM_ID,
                    MediaStore.Audio.Media.YEAR,
                    MediaStore.Audio.Media.ARTIST};
            return projection;
        }

    }

    private String getSelection(String category, String subCategory) {
        FireLog.d(TAG, "(++) getSelection, category= " + category + ", subCategory=" + subCategory);
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        if (category.equals(MEDIA_ID_ALBUM)) {
            selection += " AND " + MediaStore.Audio.Media.ALBUM_ID + " == " + subCategory;
        } else if (category.equals(MEDIA_ID_ARTIST)) {
            selection += " AND " + MediaStore.Audio.Media.ARTIST_ID + " == " + subCategory;
        } else if (category.equals(MEDIA_ID_FOLDER)) {
            // in case of folder subcategory = path
            selection += " AND " + MediaStore.Audio.Media.DATA + " LIKE '" + subCategory + "%'";
        }
        return selection;
    }

    private String get_IDColumnName(String category) {
        String columnName;
        if (category.equals(MEDIA_ID_PLAYLIST)) {
            columnName = MediaStore.Audio.Playlists.Members.AUDIO_ID;
        } else {
            columnName = MediaStore.Audio.Media._ID;
        }
        return columnName;
    }

    private String getAlbumArt(int albumId) {
        FireLog.d(TAG, "(++) getAlbumArt, albumArt= " + albumId);

        return ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId).toString();
//        String albumArt = null;
//        Uri albumUri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
//        String[] projection = {MediaStore.Audio.Albums.ALBUM_ART};
//        String selection = MediaStore.Audio.Albums._ID + " == ?";
//        String[] selectionArgs = {albumId + ""};
//
//        Cursor cursor = FireApplication.getInstance().getContentResolver()
//                .query(albumUri, projection, selection, selectionArgs, null);
//        if (cursor != null) {
//            if (cursor.moveToFirst()) {
//                albumArt = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
//            }
//            cursor.close();
//        }
//
//        return albumArt;
    }


    public List<MediaMetadataCompat> searchTracks(String query, int limit) {
        FireLog.d(TAG, "(++) searchTracks");

        List<MediaMetadataCompat> medias = new ArrayList<>();
        Uri songsUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {"*"};
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != ? AND " + MediaStore.Audio.Media.TITLE + " LIKE ?";
        String[] selectionArgs = {"0", "%" + query + "%"};
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC LIMIT " + limit;

        Cursor cursor = FireApplication.getInstance().getContentResolver()
                .query(songsUri, projection, selection, selectionArgs, sortOrder);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {

                    MediaMetadataCompat mediaMetadataCompat = new MediaMetadataCompat.Builder()
                            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID)))
                            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI,
                                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)))
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)))
                            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)))
                            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION,
                                    cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)))
                            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                                    getAlbumArt(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))))
                            .build();
                    medias.add(mediaMetadataCompat);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        return medias;
    }

    public List<MediaMetadataCompat> searchAlbums(String query, int limit) {
        FireLog.d(TAG, "(++) searchAlbums");
        List<MediaMetadataCompat> albums = new ArrayList<>();

        Uri albumUri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
        String[] projection = {"*"};
        String selection = MediaStore.Audio.Albums.ALBUM + " LIKE ?";
        String[] selectionArgs = {"%" + query + "%"};
        String sortOrder = MediaStore.Audio.Albums.ALBUM + " ASC LIMIT " + limit;

        Cursor cursor = FireApplication.getInstance().getContentResolver().
                query(albumUri, projection, selection, selectionArgs, sortOrder);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    MediaMetadataCompat mediaMetadataCompat = new MediaMetadataCompat.Builder()
                            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums._ID)))
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM)))
                            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)))
                            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART)))
                            .build();
                    albums.add(mediaMetadataCompat);
                } while (cursor.moveToNext());
            }
            cursor.close();

        }
        return albums;
    }

    public List<MediaMetadataCompat> searchArtists(String query, int limit) {
        FireLog.d(TAG, "(++) searchArtists");
        List<MediaMetadataCompat> artists = new ArrayList<>();

        Uri albumUri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
        String[] projection = {"*"};
        String selection = MediaStore.Audio.Artists.ARTIST + " LIKE ?";
        String[] selectionArgs = {"%" + query + "%"};
        String sortOrder = MediaStore.Audio.Artists.ARTIST + " ASC LIMIT " + limit;

        Cursor cursor = FireApplication.getInstance().getContentResolver().
                query(albumUri, projection, selection, selectionArgs, sortOrder);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    MediaMetadataCompat mediaMetadataCompat = new MediaMetadataCompat.Builder()
                            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists._ID)))
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST)))
                            .build();
                    artists.add(mediaMetadataCompat);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        return artists;
    }
}
