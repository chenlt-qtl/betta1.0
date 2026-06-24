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

public interface INoteFileService
{
    List<NoteTreeNode> tree(String userName);

    NoteContent readContent(String userName, String path);

    NoteContent saveContent(String userName, String path, String content, String lastKnownHash);

    NoteTreeNode create(String userName, String path, String type, String content);

    NoteTreeNode rename(String userName, String path, String newPath);

    void delete(String userName, String path);

    List<NoteSearchResult> search(String userName, String keyword);

    NoteImageUploadResult uploadImage(String userName, String notePath, MultipartFile file);

    List<NoteSyncEntry> manifest(String userName);

    NoteUploadResult syncUpload(String userName, String path, MultipartFile file, String lastKnownHash);

    void download(String userName, String path, HttpServletResponse response) throws IOException;
}
