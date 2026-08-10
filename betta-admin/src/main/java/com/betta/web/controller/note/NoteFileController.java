package com.betta.web.controller.note;

import java.io.IOException;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.betta.common.annotation.Log;
import com.betta.common.core.controller.BaseController;
import com.betta.common.core.domain.AjaxResult;
import com.betta.common.enums.BusinessType;
import com.betta.system.domain.note.NoteContent;
import com.betta.system.domain.note.NoteFileRequest;
import com.betta.system.domain.note.NoteSearchResult;
import com.betta.system.domain.note.NoteSyncEntry;
import com.betta.system.domain.note.NoteTreeNode;
import com.betta.system.domain.note.NoteUploadResult;
import com.betta.system.service.INoteFileService;

@RestController
@RequestMapping("/system/note")
/**
 * Markdown 笔记接口入口。
 *
 * <p>这里不直接处理磁盘路径细节，所有路径校验、用户 vault 隔离、附件模板、同步冲突处理都放在
 * {@link INoteFileService} 实现中。Controller 只负责权限控制、操作日志和把当前登录用户传给服务层。</p>
 *
 * <p>注意：所有接口都使用 {@code getUsername()} 定位当前用户的 vault，避免前端传 userId/userName 后出现跨用户访问风险。</p>
 */
public class NoteFileController extends BaseController
{
    @Autowired
    private INoteFileService noteFileService;

    /**
     * 获取左侧笔记树。
     *
     * <p>返回普通目录和 Markdown 文件；图片附件目录、.obsidian、.trash 等内部目录由服务层隐藏。</p>
     */
    @PreAuthorize("@ss.hasPermi('system:note:list')")
    @GetMapping("/tree")
    public AjaxResult tree()
    {
        List<NoteTreeNode> tree = noteFileService.tree(getUsername());
        return success(tree);
    }

    /**
     * 获取当前登录用户的有效收藏笔记。
     *
     * <p>服务层会过滤并清理已删除、非 Markdown 或非法路径，仅返回仍真实存在的笔记节点。</p>
     *
     * @return 包含收藏笔记节点列表的统一响应
     */
    @PreAuthorize("@ss.hasPermi('system:note:list')")
    @GetMapping("/favorites")
    public AjaxResult favorites()
    {
        List<NoteTreeNode> favorites = noteFileService.favorites(getUsername());
        return success(favorites);
    }

    /**
     * 设置指定笔记的收藏状态。
     *
     * <p>请求中的 path 表示当前用户 vault 内的笔记路径，favorite 表示期望状态；业务校验和幂等持久化由服务层完成。</p>
     *
     * @param request 包含笔记相对路径和期望收藏状态的请求对象
     * @return 包含持久化后最终 boolean 收藏状态的统一响应
     */
    @PreAuthorize("@ss.hasPermi('system:note:edit')")
    @Log(title = "笔记收藏", businessType = BusinessType.UPDATE)
    @PutMapping("/favorite")
    public AjaxResult favorite(@RequestBody NoteFileRequest request)
    {
        boolean favorite = noteFileService.favorite(getUsername(), request.getPath(), request.getFavorite());
        return success(favorite);
    }

    /**
     * 读取单篇笔记正文。
     *
     * <p>返回内容、文件 hash 和图片资源前缀。前端保存时会带回 hash，用于检测是否被同步脚本或其他端修改。</p>
     */
    @PreAuthorize("@ss.hasPermi('system:note:query')")
    @GetMapping("/content")
    public AjaxResult getContent(String path)
    {
        return success(noteFileService.readContent(getUsername(), path));
    }

    /**
     * 保存笔记正文。
     *
     * <p>{@code lastKnownHash} 用于乐观冲突检测：如果服务端文件已变化，服务层会拒绝覆盖，提醒用户刷新。</p>
     */
    @PreAuthorize("@ss.hasPermi('system:note:edit')")
    @Log(title = "笔记内容", businessType = BusinessType.UPDATE)
    @PutMapping("/content")
    public AjaxResult saveContent(@RequestBody NoteFileRequest request)
    {
        NoteContent content = noteFileService.saveContent(getUsername(), request.getPath(), request.getContent(),
                request.getLastKnownHash());
        return success(content);
    }

    /**
     * 新建笔记文件或目录。
     *
     * <p>文件类型由 request.type 决定；新建文件时服务层会确保落盘为 .md。</p>
     */
    @PreAuthorize("@ss.hasPermi('system:note:add')")
    @Log(title = "笔记文件", businessType = BusinessType.INSERT)
    @PostMapping("/file")
    public AjaxResult create(@RequestBody NoteFileRequest request)
    {
        return success(noteFileService.create(getUsername(), request.getPath(), request.getType(), request.getContent()));
    }

    /**
     * 重命名笔记文件或目录。
     *
     * <p>前端标题输入框失焦时会调用该接口；文件如果没有 .md 后缀，服务层会自动补齐。</p>
     */
    @PreAuthorize("@ss.hasPermi('system:note:edit')")
    @Log(title = "笔记文件", businessType = BusinessType.UPDATE)
    @PutMapping("/file")
    public AjaxResult rename(@RequestBody NoteFileRequest request)
    {
        return success(noteFileService.rename(getUsername(), request.getPath(), request.getNewPath()));
    }

    /**
     * 删除笔记文件或目录。
     *
     * <p>目录删除会递归删除其子项；具体递归顺序和安全路径校验由服务层完成。</p>
     */
    @PreAuthorize("@ss.hasPermi('system:note:remove')")
    @Log(title = "笔记文件", businessType = BusinessType.DELETE)
    @DeleteMapping("/file")
    public AjaxResult delete(String path)
    {
        noteFileService.delete(getUsername(), path);
        return success();
    }

    /**
     * 下载单个笔记或附件文件。
     *
     * <p>该接口主要给 Web 端“下载”按钮使用，允许下载 .md 和图片等同步文件类型。</p>
     */
    @PreAuthorize("@ss.hasPermi('system:note:download')")
    @GetMapping("/file/download")
    public void download(String path, HttpServletResponse response) throws IOException
    {
        noteFileService.download(getUsername(), path, response);
    }

    /**
     * 搜索笔记。
     *
     * <p>搜索范围是 Markdown 文件名和正文，不搜索图片附件内容。</p>
     */
    @PreAuthorize("@ss.hasPermi('system:note:list')")
    @GetMapping("/search")
    public AjaxResult search(String keyword)
    {
        List<NoteSearchResult> results = noteFileService.search(getUsername(), keyword);
        return success(results);
    }

    /**
     * 上传笔记内图片。
     *
     * <p>图片保存目录和文件名由配置模板决定，返回的 Markdown 图片链接使用相对路径，兼容 Obsidian。</p>
     */
    @PreAuthorize("@ss.hasPermi('system:note:upload')")
    @Log(title = "笔记图片", businessType = BusinessType.INSERT)
    @PostMapping("/upload-image")
    public AjaxResult uploadImage(@RequestParam("file") MultipartFile file, @RequestParam("notePath") String notePath)
    {
        return success(noteFileService.uploadImage(getUsername(), notePath, file));
    }

    /**
     * 同步清单接口。
     *
     * <p>本机同步脚本先拉取 manifest，对比本地文件 hash 后再决定上传、下载或保留冲突副本。</p>
     */
    @PreAuthorize("@ss.hasPermi('system:note:list')")
    @GetMapping("/sync/manifest")
    public AjaxResult manifest()
    {
        List<NoteSyncEntry> entries = noteFileService.manifest(getUsername());
        return success(entries);
    }

    /**
     * 同步脚本上传本地文件。
     *
     * <p>如果上传时发现服务端文件 hash 与 lastKnownHash 不一致，服务层会生成 conflict 副本，不覆盖服务端原文件。</p>
     */
    @PreAuthorize("@ss.hasPermi('system:note:edit')")
    @PostMapping("/sync/upload")
    public AjaxResult syncUpload(@RequestParam("file") MultipartFile file, @RequestParam("path") String path,
            @RequestParam(value = "lastKnownHash", required = false) String lastKnownHash)
    {
        NoteUploadResult result = noteFileService.syncUpload(getUsername(), path, file, lastKnownHash);
        return success(result);
    }

    /**
     * 同步脚本下载服务端文件。
     *
     * <p>复用普通下载逻辑，确保同步脚本和 Web 下载走同一套路径与类型校验。</p>
     */
    @PreAuthorize("@ss.hasPermi('system:note:download')")
    @GetMapping("/sync/download")
    public void syncDownload(String path, HttpServletResponse response) throws IOException
    {
        noteFileService.download(getUsername(), path, response);
    }

    /**
     * 同步脚本删除服务端文件。
     *
     * <p>用于本地删除后同步到服务器；仍然只作用于当前登录用户自己的 vault。</p>
     */
    @PreAuthorize("@ss.hasPermi('system:note:remove')")
    @DeleteMapping("/sync/delete")
    public AjaxResult syncDelete(String path)
    {
        noteFileService.delete(getUsername(), path);
        return success();
    }
}
