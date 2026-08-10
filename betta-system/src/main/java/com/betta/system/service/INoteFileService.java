package com.betta.system.service;

import java.io.IOException;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import com.betta.system.domain.note.NoteContent;
import com.betta.system.domain.note.NoteImageUploadResult;
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

    List<NoteSearchResult> search(String userName, String keyword);

    NoteImageUploadResult uploadImage(String userName, String notePath, MultipartFile file);

    List<NoteSyncEntry> manifest(String userName);

    NoteUploadResult syncUpload(String userName, String path, MultipartFile file, String lastKnownHash);

    void download(String userName, String path, HttpServletResponse response) throws IOException;
}
