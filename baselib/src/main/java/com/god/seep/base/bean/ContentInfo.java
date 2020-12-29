package com.god.seep.base.bean;

import android.os.Parcel;
import android.os.Parcelable;

public class ContentInfo implements Parcelable {
    private String url;
    private int type;//类型：1、图片，0、视频

    public ContentInfo(String url, int type) {
        this.url = url;
        this.type = type;
    }

    protected ContentInfo(Parcel in) {
        url = in.readString();
        type = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(url);
        dest.writeInt(type);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ContentInfo> CREATOR = new Creator<ContentInfo>() {
        @Override
        public ContentInfo createFromParcel(Parcel in) {
            return new ContentInfo(in);
        }

        @Override
        public ContentInfo[] newArray(int size) {
            return new ContentInfo[size];
        }
    };

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
