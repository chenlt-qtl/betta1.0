package com.betta.system.domain.note;

public class NoteFileRequest
{
    private String path;

    private String newPath;

    private String type;

    private String content;

    private String lastKnownHash;

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public String getNewPath()
    {
        return newPath;
    }

    public void setNewPath(String newPath)
    {
        this.newPath = newPath;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getLastKnownHash()
    {
        return lastKnownHash;
    }

    public void setLastKnownHash(String lastKnownHash)
    {
        this.lastKnownHash = lastKnownHash;
    }
}
