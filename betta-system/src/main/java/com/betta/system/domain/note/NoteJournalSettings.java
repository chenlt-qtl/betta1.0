package com.betta.system.domain.note;

/**
 * 日记功能设置对象。
 *
 * <p>用于在前后端之间传递当前用户的日记保存目录；目录是相对于用户 vault 的路径，空字符串表示 vault 根目录。</p>
 */
public class NoteJournalSettings
{
    /** 日记保存目录相对于当前用户 vault 的路径，空字符串表示根目录。 */
    private String directory;

    /**
     * 获取日记保存目录。
     *
     * @return 相对于当前用户 vault 的目录路径，空字符串表示根目录
     */
    public String getDirectory()
    {
        return directory;
    }

    /**
     * 设置日记保存目录。
     *
     * @param directory 相对于当前用户 vault 的目录路径，空字符串表示根目录
     */
    public void setDirectory(String directory)
    {
        this.directory = directory;
    }
}
