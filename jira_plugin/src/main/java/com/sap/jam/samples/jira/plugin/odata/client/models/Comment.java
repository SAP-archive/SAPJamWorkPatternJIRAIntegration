package com.sap.jam.samples.jira.plugin.odata.client.models;

import java.util.Date;

public class Comment {
    public String Id;
    public String Text;
    public Date CreatedAt;
    public Boolean Liked;
    public int LikesCount;
    public Member Creator;
    public ThumbnailImage ThumbnailImage;
}

