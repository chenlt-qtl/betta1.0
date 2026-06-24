package com.betta.system.domain.note;

public class NoteUploadResult
{
    private String status;

    private String path;

    private String conflictPath;

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public String getConflictPath()
    {
        return conflictPath;
    }

    public void setConflictPath(String conflictPath)
    {
        this.conflictPath = conflictPath;
    }
}
