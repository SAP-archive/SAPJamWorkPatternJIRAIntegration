package com.sap.jam.samples.jira.plugin.odata.client.models;

import java.util.Date;

public class FeedEntry {
    public String Id;
    public String Text;
    public String ActionOnly;
    public Boolean Liked;
    public int LikesCount;
    public int RepliesCount;
    public Date CreatedAt;
    public Member Creator;
    public ThumbnailImage ThumbnailImage;
}

