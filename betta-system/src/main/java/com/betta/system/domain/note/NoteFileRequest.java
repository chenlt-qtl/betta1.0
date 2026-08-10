package com.betta.system.domain.note;

import java.util.List;

/**
 * 笔记文件操作请求对象。
 *
 * <p>用于承载笔记路径、重命名目标、文件类型、正文、版本哈希和收藏状态等接口参数。</p>
 */
public class NoteFileRequest
{
    private String path;

    private String newPath;

    /** 待移动的文件相对路径列表，或仅包含一个目录相对路径的列表。 */
    private List<String> paths;

    /** 移动目标目录相对于当前用户 vault 的路径。 */
    private String targetDirectory;

    private String type;

    private String content;

    /** 前端上次读取到的文件哈希，用于保存时检测并发冲突。 */
    private String lastKnownHash;

    /** 期望设置的收藏状态，true 表示收藏，false 表示取消收藏。 */
    private Boolean favorite;

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

    /**
     * 获取待移动的相对路径列表。
     *
     * @return 文件相对路径列表，或仅包含一个目录路径的列表
     */
    public List<String> getPaths()
    {
        return paths;
    }

    /**
     * 设置待移动的相对路径列表。
     *
     * @param paths 文件相对路径列表，或仅包含一个目录路径的列表
     */
    public void setPaths(List<String> paths)
    {
        this.paths = paths;
    }

    /**
     * 获取移动目标目录。
     *
     * @return 目标目录相对于当前用户 vault 的路径
     */
    public String getTargetDirectory()
    {
        return targetDirectory;
    }

    /**
     * 设置移动目标目录。
     *
     * @param targetDirectory 目标目录相对于当前用户 vault 的路径
     */
    public void setTargetDirectory(String targetDirectory)
    {
        this.targetDirectory = targetDirectory;
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

    public Boolean getFavorite()
    {
        return favorite;
    }

    public void setFavorite(Boolean favorite)
    {
        this.favorite = favorite;
    }
}
