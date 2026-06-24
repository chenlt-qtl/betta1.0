package com.betta.system.domain.note;

public class NoteContent
{
    private String path;

    private String content;

    private Long updateTime;

    private String hash;

    private String resourceBase;

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public Long getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Long updateTime)
    {
        this.updateTime = updateTime;
    }

    public String getHash()
    {
        return hash;
    }

    public void setHash(String hash)
    {
        this.hash = hash;
    }

    public String getResourceBase()
    {
        return resourceBase;
    }

    public void setResourceBase(String resourceBase)
    {
        this.resourceBase = resourceBase;
    }
}
