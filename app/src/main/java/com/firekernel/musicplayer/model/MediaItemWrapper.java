package com.firekernel.musicplayer.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.media.MediaBrowserCompat;



public class MediaItemWrapper implements Parcelable {
    public static final Creator<MediaItemWrapper> CREATOR = new Creator<MediaItemWrapper>() {
        @Override
        public MediaItemWrapper createFromParcel(Parcel in) {
            return new MediaItemWrapper(in);
        }

        @Override
        public MediaItemWrapper[] newArray(int size) {
            return new MediaItemWrapper[size];
        }
    };
    private MediaBrowserCompat.MediaItem mediaItem;
    private String typeTitle;
    private int category;

    public MediaItemWrapper(String typeTitle) {
        this.typeTitle = typeTitle;
    }

    public MediaItemWrapper(int category, MediaBrowserCompat.MediaItem mediaItem) {
        this.category = category;
        this.mediaItem = mediaItem;
    }

    protected MediaItemWrapper(Parcel in) {
        mediaItem = in.readParcelable(MediaBrowserCompat.MediaItem.class.getClassLoader());
        typeTitle = in.readString();
        category = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mediaItem, flags);
        dest.writeString(typeTitle);
        dest.writeInt(category);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public MediaBrowserCompat.MediaItem getMediaItem() {
        return mediaItem;
    }


    public String getTypeTitle() {
        return typeTitle;
    }

    public int getCategory() {
        return category;
    }

}
