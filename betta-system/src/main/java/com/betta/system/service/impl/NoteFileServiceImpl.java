package com.betta.system.service.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.betta.common.config.RuoYiConfig;
import com.betta.common.constant.Constants;
import com.betta.common.exception.ServiceException;
import com.betta.common.utils.StringUtils;
import com.betta.system.domain.note.NoteContent;
import com.betta.system.domain.note.NoteImageUploadResult;
import com.betta.system.domain.note.NoteSearchResult;
import com.betta.system.domain.note.NoteSyncEntry;
import com.betta.system.domain.note.NoteTreeNode;
import com.betta.system.domain.note.NoteUploadResult;
import com.betta.system.service.INoteFileService;

@Service
public class NoteFileServiceImpl implements INoteFileService
{
    private static final String TYPE_FILE = "file";

    private static final String TYPE_DIRECTORY = "directory";

    /** 默认附件目录与 Obsidian Custom Attachment Location 保持一致，当配置文件没有配置时使用此配置。 */
    private static final String DEFAULT_ATTACHMENT_LOCATION = "999.图片/${noteFileName}";

    /** 默认附件文件名模板，精确到毫秒，避免同一笔记内连续上传图片发生重名。当配置文件没有配置时使用此配置 */
    private static final String DEFAULT_ATTACHMENT_FILE_NAME = "file-${date:{momentJsFormat:'YYYYMMDDHHmmssSSS'}}";

    private static final DateTimeFormatter CONFLICT_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private static final Pattern DATE_TOKEN_PATTERN = Pattern.compile("\\$\\{date:\\{momentJsFormat:'([^']+)'}}");

    private static final Pattern SIMPLE_DATE_TOKEN_PATTERN = Pattern.compile("\\{date:([^}]+)}");

    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(
            List.of("jpg", "jpeg", "png", "gif", "webp", "svg"));

    private static final Set<String> SYNC_EXTENSIONS = new HashSet<>(
            List.of("md", "jpg", "jpeg", "png", "gif", "webp", "svg"));

    @Override
    public List<NoteTreeNode> tree(String userName)
    {
        // 每个用户一个独立 vault，树接口只暴露普通目录和 .md 笔记，附件图片目录在 listTree 中隐藏。
        Path root = vaultRoot(userName);
        ensureDirectory(root);
        try
        {
            return listTree(root, root);
        }
        catch (IOException e)
        {
            throw new ServiceException("读取笔记树失败");
        }
    }

    @Override
    public NoteContent readContent(String userName, String path)
    {
        Path file = resolveNoteFile(userName, path, true);
        try
        {
            NoteContent content = new NoteContent();
            content.setPath(toRelative(vaultRoot(userName), file));
            content.setContent(Files.readString(file, StandardCharsets.UTF_8));
            content.setUpdateTime(Files.getLastModifiedTime(file).toMillis());
            content.setHash(hash(file));
            content.setResourceBase(Constants.RESOURCE_PREFIX + "/notes/" + userName + "/");
            return content;
        }
        catch (IOException e)
        {
            throw new ServiceException("读取笔记失败");
        }
    }

    @Override
    public NoteContent saveContent(String userName, String path, String content, String lastKnownHash)
    {
        Path file = resolveNoteFile(userName, path, false);
        ensureDirectory(file.getParent());
        // Web 端保存前带上上次读取的 hash；如果服务端文件已被同步脚本或其他端改动，则拒绝覆盖。
        assertNoHashConflict(file, lastKnownHash);
        try
        {
            Files.writeString(file, StringUtils.nvl(content, ""), StandardCharsets.UTF_8);
            return readContent(userName, toRelative(vaultRoot(userName), file));
        }
        catch (IOException e)
        {
            throw new ServiceException("保存笔记失败");
        }
    }

    @Override
    public NoteTreeNode create(String userName, String path, String type, String content)
    {
        Path target = TYPE_DIRECTORY.equals(type) ? resolvePath(userName, path) : resolveNoteFile(userName, path, false);
        if (Files.exists(target))
        {
            throw new ServiceException("文件或目录已存在");
        }
        try
        {
            if (TYPE_DIRECTORY.equals(type))
            {
                Files.createDirectories(target);
            }
            else
            {
                ensureDirectory(target.getParent());
                Files.writeString(target, StringUtils.nvl(content, ""), StandardCharsets.UTF_8);
            }
            return toNode(vaultRoot(userName), target);
        }
        catch (IOException e)
        {
            throw new ServiceException("创建笔记失败");
        }
    }

    @Override
    public NoteTreeNode rename(String userName, String path, String newPath)
    {
        Path source = resolvePath(userName, path);
        Path target = resolvePath(userName, newPath);
        if (!Files.exists(source))
        {
            throw new ServiceException("文件或目录不存在");
        }
        if (Files.exists(target))
        {
            throw new ServiceException("目标文件或目录已存在");
        }
        if (Files.isRegularFile(source) && !isMarkdown(target))
        {
            // 前端标题展示时去掉 .md，后端兜底补回 Markdown 后缀，保证落盘文件仍是标准笔记。
            target = target.resolveSibling(target.getFileName().toString() + ".md");
        }
        try
        {
            ensureDirectory(target.getParent());
            Files.move(source, target);
            return toNode(vaultRoot(userName), target);
        }
        catch (IOException e)
        {
            throw new ServiceException("重命名失败");
        }
    }

    @Override
    public void delete(String userName, String path)
    {
        Path target = resolvePath(userName, path);
        if (!Files.exists(target))
        {
            return;
        }
        try (Stream<Path> paths = Files.walk(target))
        {
            // 删除目录时需要先删子文件再删父目录，因此按路径反序执行。
            paths.sorted(Comparator.reverseOrder()).forEach(item -> {
                try
                {
                    Files.deleteIfExists(item);
                }
                catch (IOException e)
                {
                    throw new ServiceException("删除失败");
                }
            });
        }
        catch (IOException e)
        {
            throw new ServiceException("删除失败");
        }
    }

    @Override
    public List<NoteSearchResult> search(String userName, String keyword)
    {
        if (StringUtils.isBlank(keyword))
        {
            return new ArrayList<>();
        }
        String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
        Path root = vaultRoot(userName);
        ensureDirectory(root);
        List<NoteSearchResult> results = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(root))
        {
            // 搜索仅针对 Markdown 正文和文件名，不搜索图片附件；隐藏目录同样不会被访问。
            paths.filter(Files::isRegularFile).filter(this::isMarkdown).filter(path -> !isIgnored(root, path)).forEach(path -> {
                try
                {
                    String name = path.getFileName().toString();
                    String text = Files.readString(path, StandardCharsets.UTF_8);
                    String lowerText = text.toLowerCase(Locale.ROOT);
                    if (name.toLowerCase(Locale.ROOT).contains(lowerKeyword) || lowerText.contains(lowerKeyword))
                    {
                        NoteSearchResult result = new NoteSearchResult();
                        result.setName(name);
                        result.setPath(toRelative(root, path));
                        result.setSnippet(snippet(text, lowerText.indexOf(lowerKeyword)));
                        result.setUpdateTime(Files.getLastModifiedTime(path).toMillis());
                        results.add(result);
                    }
                }
                catch (IOException ignored)
                {
                }
            });
        }
        catch (IOException e)
        {
            throw new ServiceException("搜索笔记失败");
        }
        return results;
    }

    @Override
    public NoteImageUploadResult uploadImage(String userName, String notePath, MultipartFile file)
    {
        Path noteFile = resolveNoteFile(userName, notePath, false);
        String noteFileName = FilenameUtils.getBaseName(noteFile.getFileName().toString());
        String extension = extension(file.getOriginalFilename());
        if (!IMAGE_EXTENSIONS.contains(extension))
        {
            throw new ServiceException("图片格式不支持");
        }
        String attachmentDir = renderAttachmentTemplate(RuoYiConfig.getNoteAttachmentLocation(),
                DEFAULT_ATTACHMENT_LOCATION, noteFileName);
        String imageBaseName = renderAttachmentTemplate(RuoYiConfig.getNoteAttachmentFileName(),
                DEFAULT_ATTACHMENT_FILE_NAME, noteFileName);
        String imageName = imageBaseName + "." + extension;
        Path imagePath = resolvePath(userName, attachmentDir + "/" + imageName);
        ensureDirectory(imagePath.getParent());
        try
        {
            file.transferTo(imagePath);
            Path root = vaultRoot(userName);
            String relativeImagePath = toRelative(root, imagePath);
            // 写回 Markdown 的图片链接使用相对路径，保证同一份 .md 在 Obsidian 和 betta 中都能正确显示。
            String markdownPath = toRelativeMarkdownPath(noteFile.getParent(), imagePath);
            NoteImageUploadResult result = new NoteImageUploadResult();
            result.setPath(relativeImagePath);
            result.setUrl(Constants.RESOURCE_PREFIX + "/notes/" + userName + "/" + relativeImagePath);
            result.setMarkdown("![" + FilenameUtils.getBaseName(imageName) + "](" + markdownPath + ")");
            return result;
        }
        catch (IOException e)
        {
            throw new ServiceException("上传图片失败");
        }
    }

    @Override
    public List<NoteSyncEntry> manifest(String userName)
    {
        Path root = vaultRoot(userName);
        ensureDirectory(root);
        List<NoteSyncEntry> entries = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(root))
        {
            // manifest 是本机同步脚本的对账清单，包含 .md 与图片附件，忽略 Obsidian 配置目录。
            paths.filter(Files::isRegularFile).filter(path -> isSyncFile(root, path)).forEach(path -> {
                try
                {
                    NoteSyncEntry entry = new NoteSyncEntry();
                    entry.setPath(toRelative(root, path));
                    entry.setType(TYPE_FILE);
                    entry.setSize(Files.size(path));
                    entry.setUpdateTime(Files.getLastModifiedTime(path).toMillis());
                    entry.setHash(hash(path));
                    entries.add(entry);
                }
                catch (IOException ignored)
                {
                }
            });
        }
        catch (IOException e)
        {
            throw new ServiceException("生成同步清单失败");
        }
        return entries;
    }

    @Override
    public NoteUploadResult syncUpload(String userName, String path, MultipartFile file, String lastKnownHash)
    {
        Path target = resolveSyncFile(userName, path);
        ensureDirectory(target.getParent());
        String finalPath = toRelative(vaultRoot(userName), target);
        if (Files.exists(target) && StringUtils.isNotBlank(lastKnownHash))
        {
            try
            {
                String serverHash = hash(target);
                if (!serverHash.equals(lastKnownHash))
                {
                    // 双端都改过同一文件时保留本次上传为冲突副本，不静默覆盖服务端现有文件。
                    target = conflictPath(target, "local");
                    finalPath = toRelative(vaultRoot(userName), target);
                }
            }
            catch (IOException e)
            {
                throw new ServiceException("检测同步冲突失败");
            }
        }
        try
        {
            file.transferTo(target);
            NoteUploadResult result = new NoteUploadResult();
            result.setPath(toRelative(vaultRoot(userName), resolveSyncFile(userName, path)));
            result.setConflictPath(finalPath.equals(result.getPath()) ? null : finalPath);
            result.setStatus(result.getConflictPath() == null ? "saved" : "conflict");
            return result;
        }
        catch (IOException e)
        {
            throw new ServiceException("上传同步文件失败");
        }
    }

    @Override
    public void download(String userName, String path, HttpServletResponse response) throws IOException
    {
        Path file = resolveSyncFile(userName, path);
        if (!Files.exists(file) || !Files.isRegularFile(file))
        {
            throw new ServiceException("文件不存在");
        }
        String fileName = file.getFileName().toString();
        response.setContentType("application/octet-stream");
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-disposition", "attachment; filename=" + encoded + ";filename*=utf-8''" + encoded);
        response.setHeader("download-filename", encoded);
        try (OutputStream outputStream = response.getOutputStream())
        {
            Files.copy(file, outputStream);
        }
    }

    private List<NoteTreeNode> listTree(Path root, Path directory) throws IOException
    {
        List<NoteTreeNode> nodes = new ArrayList<>();
        try (Stream<Path> paths = Files.list(directory))
        {
            // 左侧树只展示用户需要直接管理的笔记结构：隐藏 .obsidian/.trash/临时文件和附件图片目录。
            paths.filter(path -> !isIgnored(root, path))
                    .filter(path -> !isAttachmentDirectory(root, path))
                    .filter(path -> Files.isDirectory(path) || isMarkdown(path))
                    .sorted(this::compareTreePath)
                    .forEach(path -> {
                        try
                        {
                            NoteTreeNode node = toNode(root, path);
                            if (Files.isDirectory(path))
                            {
                                node.setChildren(listTree(root, path));
                            }
                            nodes.add(node);
                        }
                        catch (IOException ignored)
                        {
                        }
                    });
        }
        return nodes;
    }

    private NoteTreeNode toNode(Path root, Path path) throws IOException
    {
        NoteTreeNode node = new NoteTreeNode();
        node.setName(path.getFileName().toString());
        node.setPath(toRelative(root, path));
        node.setType(Files.isDirectory(path) ? TYPE_DIRECTORY : TYPE_FILE);
        node.setSize(Files.isDirectory(path) ? 0L : Files.size(path));
        node.setUpdateTime(Files.getLastModifiedTime(path).toMillis());
        return node;
    }

    private Path vaultRoot(String userName)
    {
        // vault 放在 ruoyi.profile/notes/{userName} 下，后续所有路径解析都必须限制在该目录内。
        return Path.of(RuoYiConfig.getProfile(), "notes", String.valueOf(userName)).normalize().toAbsolutePath();
    }

    private Path resolvePath(String userName, String relativePath)
    {
        if (StringUtils.isBlank(relativePath))
        {
            throw new ServiceException("路径不能为空");
        }
        String normalized = relativePath.replace("\\", "/");
        if (normalized.startsWith("/") || normalized.contains("\0"))
        {
            throw new ServiceException("路径非法");
        }
        Path root = vaultRoot(userName);
        Path target = root.resolve(normalized).normalize().toAbsolutePath();
        // normalize 后再次校验前缀，防止 ../ 之类路径穿越访问其他用户或系统文件。
        if (!target.startsWith(root))
        {
            throw new ServiceException("路径越界");
        }
        if (isIgnored(root, target))
        {
            throw new ServiceException("路径不允许访问");
        }
        return target;
    }

    private Path resolveNoteFile(String userName, String relativePath, boolean mustExist)
    {
        Path file = resolvePath(userName, relativePath);
        if (!isMarkdown(file))
        {
            file = file.resolveSibling(file.getFileName().toString() + ".md");
        }
        if (mustExist && (!Files.exists(file) || !Files.isRegularFile(file)))
        {
            throw new ServiceException("笔记不存在");
        }
        return file;
    }

    private Path resolveSyncFile(String userName, String relativePath)
    {
        Path file = resolvePath(userName, relativePath);
        if (!isSyncFile(vaultRoot(userName), file))
        {
            throw new ServiceException("文件类型不允许同步");
        }
        return file;
    }

    private boolean isSyncFile(Path root, Path path)
    {
        return !isIgnored(root, path) && SYNC_EXTENSIONS.contains(extension(path.getFileName().toString()));
    }

    private boolean isIgnored(Path root, Path path)
    {
        Path relative = root.relativize(path.normalize().toAbsolutePath());
        for (Path part : relative)
        {
            String name = part.toString();
            // 不同步也不展示 Obsidian 内部目录、回收站、隐藏文件和编辑器临时文件。
            if (name.startsWith(".") || ".obsidian".equals(name) || ".trash".equals(name)
                    || name.endsWith(".tmp") || name.endsWith(".swp") || name.endsWith("~"))
            {
                return true;
            }
        }
        return false;
    }

    private boolean isAttachmentDirectory(Path root, Path path)
    {
        if (!Files.isDirectory(path))
        {
            return false;
        }
        String attachmentRoot = attachmentRootPath();
        if (StringUtils.isBlank(attachmentRoot))
        {
            return false;
        }
        String relative = toRelative(root, path);
        // 附件根目录及其子目录都从笔记树隐藏，但 manifest 仍会同步其中的图片文件。
        return relative.equals(attachmentRoot) || relative.startsWith(attachmentRoot + "/");
    }

    private String attachmentRootPath()
    {
        // 从可配置的附件模板中提取稳定根目录；例如 999.图片/{noteFileName} 的根目录是 999.图片。
        String template = StringUtils.nvl(RuoYiConfig.getNoteAttachmentLocation(), DEFAULT_ATTACHMENT_LOCATION)
                .replace("\\", "/");
        String stablePrefix = template.split("\\$\\{noteFileName}|\\{noteFileName}|\\$\\{date:|\\{date:", 2)[0];
        if (StringUtils.isBlank(stablePrefix))
        {
            return "";
        }
        String[] parts = stablePrefix.split("/");
        List<String> pathParts = new ArrayList<>();
        for (String part : parts)
        {
            if (StringUtils.isNotBlank(part))
            {
                pathParts.add(part);
            }
        }
        return pathParts.isEmpty() ? "" : pathParts.get(0);
    }

    private boolean isMarkdown(Path path)
    {
        return "md".equals(extension(path.getFileName().toString()));
    }

    private String extension(String fileName)
    {
        return FilenameUtils.getExtension(StringUtils.nvl(fileName, "")).toLowerCase(Locale.ROOT);
    }

    private String toRelative(Path root, Path path)
    {
        return root.relativize(path.normalize().toAbsolutePath()).toString().replace("\\", "/");
    }

    private String toRelativeMarkdownPath(Path noteParent, Path imagePath)
    {
        // 计算图片相对当前笔记所在目录的路径，与 Obsidian 的 Markdown 图片引用方式一致。
        String relative = noteParent.normalize().toAbsolutePath().relativize(imagePath.normalize().toAbsolutePath())
                .toString().replace("\\", "/");
        return encodeMarkdownPath(relative);
    }

    private String encodeMarkdownPath(String path)
    {
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length; i++)
        {
            if (!"..".equals(parts[i]) && !".".equals(parts[i]))
            {
                parts[i] = parts[i].replace(" ", "%20");
            }
        }
        return String.join("/", parts);
    }

    private String hash(Path path) throws IOException
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = Files.readAllBytes(path);
            byte[] hash = digest.digest(bytes);
            StringBuilder builder = new StringBuilder();
            for (byte item : hash)
            {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        }
        catch (Exception e)
        {
            throw new IOException("计算文件Hash失败", e);
        }
    }

    private void assertNoHashConflict(Path file, String lastKnownHash)
    {
        if (!Files.exists(file) || StringUtils.isBlank(lastKnownHash))
        {
            return;
        }
        try
        {
            if (!hash(file).equals(lastKnownHash))
            {
                throw new ServiceException("笔记已被其他地方修改，请刷新后再保存");
            }
        }
        catch (IOException e)
        {
            throw new ServiceException("检测笔记冲突失败");
        }
    }

    private Path conflictPath(Path target, String side)
    {
        String fileName = target.getFileName().toString();
        String baseName = FilenameUtils.getBaseName(fileName);
        String extension = FilenameUtils.getExtension(fileName);
        String timestamp = LocalDateTime.now().format(CONFLICT_NAME_FORMATTER);
        String conflictName = baseName + ".conflict-" + side + "-" + timestamp
                + (StringUtils.isBlank(extension) ? "" : "." + extension);
        return target.resolveSibling(conflictName);
    }

    private String renderAttachmentTemplate(String template, String defaultTemplate, String noteFileName)
    {
        // 同时兼容 Obsidian 插件风格 ${noteFileName}/${date:{momentJsFormat:'...'}} 和简化配置 {noteFileName}/{date:...}。
        String value = StringUtils.isBlank(template) ? defaultTemplate : template;
        value = value.replace("${noteFileName}", sanitizePathPart(noteFileName));
        value = value.replace("{noteFileName}", sanitizePathPart(noteFileName));
        Matcher matcher = DATE_TOKEN_PATTERN.matcher(value);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find())
        {
            String javaPattern = toJavaDatePattern(matcher.group(1));
            String formatted = LocalDateTime.now().format(DateTimeFormatter.ofPattern(javaPattern));
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(formatted));
        }
        matcher.appendTail(buffer);
        String rendered = replaceSimpleDateTokens(buffer.toString()).replace("\\", "/");
        if (rendered.startsWith("/") || rendered.contains(".."))
        {
            // 模板渲染结果仍然必须是 vault 内相对路径，防止配置错误导致附件写到 vault 外。
            throw new ServiceException("笔记附件模板渲染后路径非法");
        }
        return rendered;
    }

    private String replaceSimpleDateTokens(String value)
    {
        Matcher matcher = SIMPLE_DATE_TOKEN_PATTERN.matcher(value);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find())
        {
            String javaPattern = toJavaDatePattern(matcher.group(1));
            String formatted = LocalDateTime.now().format(DateTimeFormatter.ofPattern(javaPattern));
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(formatted));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String sanitizePathPart(String value)
    {
        return StringUtils.nvl(value, "").replace("/", "_").replace("\\", "_").replace(":", "_");
    }

    private String toJavaDatePattern(String momentPattern)
    {
        return momentPattern.replace("YYYY", "yyyy").replace("DD", "dd");
    }

    private String snippet(String text, int index)
    {
        if (index < 0)
        {
            return text.length() > 120 ? text.substring(0, 120) : text;
        }
        int start = Math.max(0, index - 40);
        int end = Math.min(text.length(), index + 80);
        return text.substring(start, end).replaceAll("\\s+", " ");
    }

    private int compareTreePath(Path left, Path right)
    {
        if (Files.isDirectory(left) && !Files.isDirectory(right))
        {
            return -1;
        }
        if (!Files.isDirectory(left) && Files.isDirectory(right))
        {
            return 1;
        }
        return left.getFileName().toString().compareToIgnoreCase(right.getFileName().toString());
    }

    private void ensureDirectory(Path path)
    {
        try
        {
            Files.createDirectories(path);
        }
        catch (IOException e)
        {
            throw new ServiceException("创建目录失败");
        }
    }
}
