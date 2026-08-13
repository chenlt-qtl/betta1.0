package com.betta.system.service;

import java.io.IOException;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import com.betta.system.domain.note.NoteContent;
import com.betta.system.domain.note.NoteImageUploadResult;
import com.betta.system.domain.note.NoteJournalSettings;
import com.betta.system.domain.note.NoteSearchResult;
import com.betta.system.domain.note.NoteSyncEntry;
import com.betta.system.domain.note.NoteTreeNode;
import com.betta.system.domain.note.NoteUploadResult;

/**
 * 笔记文件业务服务接口。
 *
 * <p>定义当前用户 vault 的树、正文、文件管理、收藏、搜索、附件和同步等业务能力。</p>
 */
public interface INoteFileService
{
    List<NoteTreeNode> tree(String userName);

    NoteContent readContent(String userName, String path);

    NoteContent saveContent(String userName, String path, String content, String lastKnownHash);

    NoteTreeNode create(String userName, String path, String type, String content);

    NoteTreeNode rename(String userName, String path, String newPath);

    /**
     * 将多个普通文件或单个目录移动到指定目录，并同步更新受影响的收藏路径。
     *
     * @param userName 当前登录用户名，用于定位独立的用户 vault
     * @param paths 待移动的文件相对路径列表，或仅包含一个目录路径的列表
     * @param targetDirectory 目标目录相对于当前用户 vault 的路径
     * @return 与输入 paths 顺序一致的移动后相对路径列表
     */
    List<String> move(String userName, List<String> paths, String targetDirectory);

    void delete(String userName, String path);

    /**
     * 查询当前用户有效的收藏笔记。
     *
     * @param userName 当前登录用户名，用于定位独立的用户 vault
     * @return 仍然真实存在且为 Markdown 文件的收藏节点列表
     */
    List<NoteTreeNode> favorites(String userName);

    /**
     * 设置当前用户指定笔记的收藏状态。
     *
     * @param userName 当前登录用户名，用于定位独立的用户 vault
     * @param path 笔记相对于当前用户 vault 的路径
     * @param favorite true 表示收藏，false 表示取消收藏
     * @return 持久化后的最终收藏状态
     */
    boolean favorite(String userName, String path, Boolean favorite);

    /**
     * 查询当前用户的日记保存目录设置。
     *
     * @param userName 当前登录用户名，用于定位独立的用户 vault
     * @return 日记设置；其中空目录表示日记保存在 vault 根目录
     */
    NoteJournalSettings journalSettings(String userName);

    /**
     * 更新当前用户的日记保存目录设置。
     *
     * @param userName 当前登录用户名，用于定位独立的用户 vault
     * @param settings 待保存的日记设置；目录必须是 vault 内真实存在的目录，空目录表示根目录
     * @return 经过规范化并成功持久化的日记设置
     */
    NoteJournalSettings updateJournalSettings(String userName, NoteJournalSettings settings);

    /**
     * 打开或创建当前日期对应的日记笔记。
     *
     * @param userName 当前登录用户名，用于定位独立的用户 vault
     * @return 已存在或新创建的今日日记正文及文件信息
     */
    NoteContent openTodayJournal(String userName);

    List<NoteSearchResult> search(String userName, String keyword);

    NoteImageUploadResult uploadImage(String userName, String notePath, MultipartFile file);

    List<NoteSyncEntry> manifest(String userName);

    NoteUploadResult syncUpload(String userName, String path, MultipartFile file, String lastKnownHash);

    void download(String userName, String path, HttpServletResponse response) throws IOException;
}
