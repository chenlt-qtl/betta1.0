package com.betta.system.domain.note;

import java.util.ArrayList;
import java.util.List;

/**
 * Obsidian compatible note tree node.
 */
public class NoteTreeNode
{
    private String name;

    private String path;

    private String type;

    private Long size;

    private Long updateTime;

    private List<NoteTreeNode> children = new ArrayList<>();

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public Long getSize()
    {
        return size;
    }

    public void setSize(Long size)
    {
        this.size = size;
    }

    public Long getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Long updateTime)
    {
        this.updateTime = updateTime;
    }

    public List<NoteTreeNode> getChildren()
    {
        return children;
    }

    public void setChildren(List<NoteTreeNode> children)
    {
        this.children = children;
    }
}
