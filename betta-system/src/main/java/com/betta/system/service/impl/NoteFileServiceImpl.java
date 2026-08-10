package com.betta.system.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
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

/**
 * 笔记文件业务服务实现。
 *
 * <p>负责用户 vault 隔离、路径安全校验、Markdown 文件管理、收藏元数据持久化以及附件和同步处理。</p>
 */
@Service
public class NoteFileServiceImpl implements INoteFileService
{
    private static final String TYPE_FILE = "file";

    private static final String TYPE_DIRECTORY = "directory";

    /** 用户 vault 根目录下的隐藏收藏元数据文件，每行保存一个规范化的 Markdown 相对路径。 */
    private static final String FAVORITES_FILE_NAME = ".favorites";

    /**
     * 收藏元数据串行锁。
     *
     * <p>服务是 Spring 单例，收藏查询、设置以及重命名和删除联动统一通过该锁串行执行，避免读改写互相覆盖。</p>
     */
    private final Object favoriteMetadataLock = new Object();

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
        // 重命名和收藏路径映射必须处于同一串行区间，避免并发收藏请求重新写回旧路径。
        synchronized (favoriteMetadataLock)
        {
            return renameAndUpdateFavorites(userName, path, newPath);
        }
    }

    /**
     * 执行文件或目录重命名，并同步映射相关收藏路径。
     *
     * @param userName 当前登录用户名
     * @param path 原文件或目录相对路径
     * @param newPath 新文件或目录相对路径
     * @return 重命名后的树节点
     */
    private NoteTreeNode renameAndUpdateFavorites(String userName, String path, String newPath)
    {
        Path source = resolvePath(userName, path);
        Path target = resolvePath(userName, newPath);
        if (!Files.exists(source))
        {
            throw new ServiceException("文件或目录不存在");
        }
        // 保持历史判断顺序：先拒绝调用方原始目标已存在，再处理文件目标的 Markdown 后缀补全。
        if (Files.exists(target))
        {
            throw new ServiceException("目标文件或目录已存在");
        }
        if (Files.isRegularFile(source) && !isMarkdown(target))
        {
            // 前端标题展示时去掉 .md，后端兜底补回 Markdown 后缀，保证落盘文件仍是标准笔记。
            target = target.resolveSibling(target.getFileName().toString() + ".md");
            if (Files.exists(target))
            {
                // 补后缀后的最终文件同样不能碰撞，避免覆盖已经存在的 Markdown 笔记。
                throw new ServiceException("目标文件或目录已存在");
            }
        }
        Path root = vaultRoot(userName);
        boolean directory = Files.isDirectory(source);
        String sourcePath = toRelative(root, source);
        String targetPath = toRelative(root, target);
        // 文件移动前读取有效收藏，否则移动后原路径会被当成陈旧项清理，无法完成精确映射。
        Set<String> favoritePaths = loadValidFavoritePaths(userName);
        ensureDirectory(target.getParent());
        try
        {
            Files.move(source, target);
        }
        catch (IOException | RuntimeException e)
        {
            throw serviceException("重命名失败，文件未移动", e);
        }

        Set<String> renamedFavoritePaths = renameFavoritePaths(favoritePaths, sourcePath, targetPath, directory);
        try
        {
            if (!renamedFavoritePaths.equals(favoritePaths))
            {
                writeFavoritePaths(root, renamedFavoritePaths);
            }
        }
        catch (ServiceException e)
        {
            // 元数据替换失败时尽力恢复文件原路径；回滚结果会通过明确异常告知前端实际落盘状态。
            throw rollbackRenameAfterFavoriteFailure(source, target, e);
        }

        try
        {
            return toNode(root, target);
        }
        catch (IOException | RuntimeException e)
        {
            // 文件与收藏路径已经完成变更，此时不能再报告成普通“重命名失败”，避免前端继续使用旧路径。
            throw serviceException("重命名已完成，但读取新文件信息失败，请刷新笔记树", e);
        }
    }

    /**
     * 收藏元数据保存失败后，尽力将已经移动的文件或目录恢复到原路径。
     *
     * @param source 原文件或目录路径
     * @param target 已经移动到的新路径
     * @param metadataFailure 收藏元数据保存异常
     * @return 包含原始元数据异常及回滚结果的明确业务异常
     */
    private ServiceException rollbackRenameAfterFavoriteFailure(Path source, Path target,
            ServiceException metadataFailure)
    {
        try
        {
            Files.move(target, source);
            return serviceException("重命名失败：收藏信息保存失败，文件已恢复到原路径", metadataFailure);
        }
        catch (IOException | RuntimeException rollbackFailure)
        {
            ServiceException failure = serviceException(
                    "重命名部分失败：文件已移动到新路径，但收藏信息保存及文件回滚均失败，请刷新笔记树确认",
                    metadataFailure);
            // 原始 cause 保留收藏保存失败，回滚失败作为 suppressed 证据附加，便于日志完整定位两阶段故障。
            failure.addSuppressed(rollbackFailure);
            return failure;
        }
    }

    /**
     * 将多个普通文件或单个目录移动到指定目录，并同步映射收藏路径。
     *
     * @param userName 当前登录用户名，用于定位独立的用户 vault
     * @param paths 待移动的文件相对路径列表，或仅包含一个目录路径的列表
     * @param targetDirectory 目标目录相对于当前用户 vault 的路径
     * @return 与输入 paths 顺序一致的移动后相对路径列表
     */
    @Override
    public List<String> move(String userName, List<String> paths, String targetDirectory)
    {
        // 移动文件和收藏路径映射必须串行，避免并发收藏请求在移动期间重新写回旧路径。
        synchronized (favoriteMetadataLock)
        {
            return moveAndUpdateFavorites(userName, paths, targetDirectory);
        }
    }

    /**
     * 在完成全部路径、类型和冲突预检后执行移动，并在失败时尽力恢复已移动项目。
     *
     * @param userName 当前登录用户名
     * @param paths 待移动的相对路径列表
     * @param targetDirectory 目标目录相对路径
     * @return 与输入路径顺序一致的新相对路径列表
     */
    private List<String> moveAndUpdateFavorites(String userName, List<String> paths, String targetDirectory)
    {
        if (paths == null || paths.isEmpty())
        {
            throw new ServiceException("待移动路径不能为空");
        }

        Path root = vaultRoot(userName);
        Path targetDirectoryPath = resolveMoveTargetDirectory(userName, targetDirectory);
        if (!Files.exists(targetDirectoryPath, LinkOption.NOFOLLOW_LINKS)
                || !Files.isDirectory(targetDirectoryPath, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(targetDirectoryPath))
        {
            throw new ServiceException("移动目标必须是已存在的目录");
        }
        Path realRoot = toRealPath(root, "校验笔记根目录失败");
        Path realTargetDirectory = toRealPath(targetDirectoryPath, "校验移动目标目录失败");
        if (!realTargetDirectory.startsWith(realRoot))
        {
            throw new ServiceException("移动目标路径越界");
        }

        List<Path> sources = new ArrayList<>();
        List<Path> targets = new ArrayList<>();
        List<String> movedRelativePaths = new ArrayList<>();
        Set<Path> uniqueSources = new HashSet<>();
        Set<Path> uniqueTargets = new HashSet<>();
        boolean containsDirectory = false;
        for (String path : paths)
        {
            Path source = resolvePath(userName, path);
            if (!Files.exists(source, LinkOption.NOFOLLOW_LINKS))
            {
                throw new ServiceException("待移动文件或目录不存在");
            }
            if (Files.isSymbolicLink(source))
            {
                throw new ServiceException("不支持移动符号链接");
            }
            boolean directory = Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS);
            if (!directory && !Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS))
            {
                throw new ServiceException("仅支持移动普通文件或目录");
            }
            Path realSource = toRealPath(source, "校验待移动路径失败");
            if (!realSource.startsWith(realRoot))
            {
                throw new ServiceException("待移动路径越界");
            }
            if (directory && (realTargetDirectory.equals(realSource)
                    || realTargetDirectory.startsWith(realSource)))
            {
                throw new ServiceException("目录不能移动到自身或其子目录");
            }
            // 使用真实路径去重，避免同一项目经 vault 内符号链接父目录形成不同文本路径后被重复移动。
            if (!uniqueSources.add(realSource))
            {
                throw new ServiceException("待移动路径不能重复");
            }

            Path target = targetDirectoryPath.resolve(source.getFileName()).normalize();
            if (target.equals(source))
            {
                throw new ServiceException("文件或目录已位于目标目录");
            }
            if (!target.startsWith(root))
            {
                throw new ServiceException("移动目标路径越界");
            }
            if (!uniqueTargets.add(target))
            {
                throw new ServiceException("多个待移动项目在目标目录中重名");
            }
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS))
            {
                throw new ServiceException("目标目录中已存在同名文件或目录");
            }

            containsDirectory = containsDirectory || directory;
            sources.add(source);
            targets.add(target);
            movedRelativePaths.add(toRelative(root, target));
        }
        if (containsDirectory && paths.size() > 1)
        {
            throw new ServiceException("不能混合移动文件和目录，且一次只能移动一个目录");
        }

        // 收藏读取也属于移动前预检；如果元数据当前不可读，不能先移动文件再留下无法同步的旧收藏路径。
        Set<String> favoritePaths = loadValidFavoritePaths(userName);
        Set<String> movedFavoritePaths = new LinkedHashSet<>(favoritePaths);
        for (int index = 0; index < sources.size(); index++)
        {
            boolean directory = Files.isDirectory(sources.get(index), LinkOption.NOFOLLOW_LINKS);
            movedFavoritePaths = renameFavoritePaths(movedFavoritePaths, toRelative(root, sources.get(index)),
                    toRelative(root, targets.get(index)), directory);
        }

        int movedCount = 0;
        try
        {
            for (int index = 0; index < sources.size(); index++)
            {
                // 不使用覆盖选项，确保预检后若出现并发重名也会失败并进入统一回滚。
                Files.move(sources.get(index), targets.get(index));
                movedCount++;
            }
        }
        catch (IOException | RuntimeException e)
        {
            throw rollbackMovedPaths(sources, targets, movedCount, "移动失败", e);
        }

        try
        {
            if (!movedFavoritePaths.equals(favoritePaths))
            {
                writeFavoritePaths(root, movedFavoritePaths);
            }
        }
        catch (RuntimeException e)
        {
            // 收藏保存失败时恢复所有已移动项目，使文件位置与原收藏元数据继续保持一致。
            throw rollbackMovedPaths(sources, targets, movedCount, "移动失败：收藏信息保存失败", e);
        }
        return movedRelativePaths;
    }

    /**
     * 将已完成移动的项目按逆序恢复到原路径，并明确报告是否发生部分成功。
     *
     * @param sources 原路径列表
     * @param targets 新路径列表
     * @param movedCount 已成功移动的项目数量
     * @param message 原始失败阶段说明
     * @param cause 导致移动或收藏保存失败的异常
     * @return 描述回滚结果并保留底层异常证据的业务异常
     */
    private ServiceException rollbackMovedPaths(List<Path> sources, List<Path> targets, int movedCount,
            String message, Throwable cause)
    {
        Throwable rollbackFailure = null;
        for (int index = movedCount - 1; index >= 0; index--)
        {
            try
            {
                Files.move(targets.get(index), sources.get(index));
            }
            catch (IOException | RuntimeException e)
            {
                if (rollbackFailure == null)
                {
                    rollbackFailure = e;
                }
                else
                {
                    rollbackFailure.addSuppressed(e);
                }
            }
        }
        if (rollbackFailure == null)
        {
            return serviceException(message + "，已移动项目已恢复到原路径", cause);
        }
        ServiceException failure = serviceException(message + "，且部分项目回滚失败，请刷新笔记树确认实际位置", cause);
        failure.addSuppressed(rollbackFailure);
        return failure;
    }

    /**
     * 获取路径未跟随符号链接后的真实位置，并将读取失败转换为包含原因的业务异常。
     *
     * @param path 待读取真实位置的路径
     * @param message 读取失败时返回给调用方的业务提示
     * @return 文件系统解析后的真实绝对路径
     */
    private Path toRealPath(Path path, String message)
    {
        try
        {
            return path.toRealPath();
        }
        catch (IOException | RuntimeException e)
        {
            throw serviceException(message, e);
        }
    }

    /**
     * 解析移动目标目录，单独允许空字符串表示当前用户 vault 根目录。
     *
     * <p>该兼容仅用于移动目标目录选择器；其他文件接口仍统一通过 {@link #resolvePath(String, String)}
     * 拒绝空路径，避免意外操作 vault 根目录。</p>
     *
     * @param userName 当前登录用户名
     * @param targetDirectory 目标目录相对路径，空字符串表示 vault 根目录
     * @return 规范化后的目标目录绝对路径
     */
    private Path resolveMoveTargetDirectory(String userName, String targetDirectory)
    {
        if ("".equals(targetDirectory))
        {
            return vaultRoot(userName);
        }
        return resolvePath(userName, targetDirectory);
    }

    @Override
    public void delete(String userName, String path)
    {
        // 删除与收藏清理串行执行，保证已删除路径不会被并发请求重新写回收藏元数据。
        synchronized (favoriteMetadataLock)
        {
            deleteAndCleanFavorites(userName, path);
        }
    }

    /**
     * 删除指定文件或目录，并清理其精确路径及目录后代的收藏记录。
     *
     * @param userName 当前登录用户名
     * @param path 待删除文件或目录的相对路径
     */
    private void deleteAndCleanFavorites(String userName, String path)
    {
        Path target = resolvePath(userName, path);
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS))
        {
            try
            {
                // 即使目标已不存在，也读取一次收藏并清理所有陈旧项，保持删除接口幂等。
                loadValidFavoritePaths(userName);
            }
            catch (ServiceException e)
            {
                throw serviceException("目标文件已不存在，但收藏信息清理失败；查询收藏后可再次尝试自愈", e);
            }
            return;
        }

        boolean directory = Files.isDirectory(target, LinkOption.NOFOLLOW_LINKS);
        if (directory)
        {
            assertDirectoryContainsOnlyDirectories(target);
        }

        Throwable deleteFailure = null;
        List<Path> deletePaths = new ArrayList<>();
        try
        {
            // 目录在构建最终删除清单时再次校验类型，确保发现并发写入的文件后不会开始任何删除。
            deletePaths = directory ? listDirectoryDeletionPaths(target) : listDeletionPaths(target);
        }
        catch (ServiceException e)
        {
            // 类型复检失败表示目录中已经出现非目录项，必须在删除循环开始前直接拒绝。
            throw e;
        }
        catch (IOException e)
        {
            deleteFailure = e;
        }
        catch (UncheckedIOException e)
        {
            // Files.walk 在遍历阶段发生的 I/O 错误会包装为 UncheckedIOException，仍需进入后续收藏收敛流程。
            deleteFailure = e.getCause();
        }
        catch (SecurityException e)
        {
            deleteFailure = e;
        }
        catch (RuntimeException e)
        {
            deleteFailure = e;
        }
        for (Path deletePath : deletePaths)
        {
            try
            {
                if (directory && !Files.exists(deletePath, LinkOption.NOFOLLOW_LINKS))
                {
                    // 并发流程已删除的目录无需再次操作，避免对不存在的非目录目标调用删除方法。
                    continue;
                }
                if (directory && !Files.isDirectory(deletePath, LinkOption.NOFOLLOW_LINKS))
                {
                    // 清单完成后若目录被替换成文件或符号链接，绝不调用删除方法处理该非目录项。
                    throw new IOException("目录删除期间检测到非目录项");
                }
                Files.deleteIfExists(deletePath);
            }
            catch (IOException | RuntimeException e)
            {
                if (deleteFailure == null)
                {
                    deleteFailure = e;
                }
                else
                {
                    deleteFailure.addSuppressed(e);
                }
            }
        }

        RuntimeException favoriteCleanupFailure = null;
        try
        {
            // 无论完整或部分删除，都按磁盘上真实剩余的 Markdown 文件重新过滤收藏，收敛已删除项。
            loadValidFavoritePaths(userName);
        }
        catch (RuntimeException e)
        {
            favoriteCleanupFailure = e;
        }

        if (deleteFailure != null)
        {
            String message = favoriteCleanupFailure == null
                    ? "删除未完整完成，收藏信息已按磁盘剩余文件收敛，请刷新后重试"
                    : "删除未完整完成，且收藏信息收敛失败，请刷新后重试";
            ServiceException failure = serviceException(message, deleteFailure);
            if (favoriteCleanupFailure != null)
            {
                failure.addSuppressed(favoriteCleanupFailure);
            }
            throw failure;
        }
        if (favoriteCleanupFailure != null)
        {
            // 删除动作已经完整执行，仅元数据清理失败；明确告知实际状态，后续删除重试或收藏查询会继续自愈。
            throw serviceException("文件删除已完成，但收藏信息清理失败；重试删除或查询收藏可自动修复",
                    favoriteCleanupFailure);
        }
    }

    /**
     * 构建普通文件删除使用的反序路径清单，保持单文件删除的既有行为不变。
     *
     * @param target 待删除的普通文件路径
     * @return 按子项优先顺序排列的删除路径清单
     * @throws IOException 遍历目标失败时抛出
     */
    private List<Path> listDeletionPaths(Path target) throws IOException
    {
        try (Stream<Path> paths = Files.walk(target))
        {
            return paths.sorted(Comparator.reverseOrder()).toList();
        }
    }

    /**
     * 再次遍历待删除目录并构建仅包含目录的反序清单，发现任何非目录项时立即拒绝。
     *
     * <p>清单必须在删除动作开始前完整生成；生成后新建的文件不会进入清单，父目录删除将通过
     * {@code DirectoryNotEmptyException} 失败，从而保留并发写入的文件。</p>
     *
     * @param directory 待删除的目录路径
     * @return 仅包含目录且按子目录优先顺序排列的删除路径清单
     * @throws IOException 遍历目录失败时抛出
     */
    private List<Path> listDirectoryDeletionPaths(Path directory) throws IOException
    {
        List<Path> deletePaths = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(directory))
        {
            paths.forEach(path -> {
                if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
                {
                    throw new ServiceException("目录中仍有文件，请先移动或删除后再删除目录");
                }
                deletePaths.add(path);
            });
        }
        deletePaths.sort(Comparator.reverseOrder());
        return deletePaths;
    }

    /**
     * 递归确认待删除目录仅由目录组成，不包含任何深度的文件、符号链接或其他非目录项。
     *
     * @param directory 待检查的真实目录路径
     */
    private void assertDirectoryContainsOnlyDirectories(Path directory)
    {
        try (Stream<Path> paths = Files.walk(directory))
        {
            boolean containsNonDirectory = paths.skip(1)
                    .anyMatch(path -> !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS));
            if (containsNonDirectory)
            {
                throw new ServiceException("目录中仍有文件，请先移动或删除后再删除目录");
            }
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (IOException | UncheckedIOException | SecurityException e)
        {
            throw serviceException("检查目录内容失败，未执行删除", e);
        }
    }

    /**
     * 查询当前用户仍然有效的收藏笔记。
     *
     * @param userName 当前登录用户名
     * @return 有效收藏对应的笔记树节点列表，顺序与元数据中的收藏顺序一致
     */
    @Override
    public List<NoteTreeNode> favorites(String userName)
    {
        synchronized (favoriteMetadataLock)
        {
            Path root = vaultRoot(userName);
            Set<String> favoritePaths = loadValidFavoritePaths(userName);
            Set<String> readablePaths = new LinkedHashSet<>();
            List<NoteTreeNode> nodes = new ArrayList<>();
            for (String favoritePath : favoritePaths)
            {
                try
                {
                    // 再次使用受限路径解析构造返回节点，确保接口永远不暴露隐藏文件或 vault 外文件。
                    Path noteFile = resolveExistingFavoriteNote(userName, favoritePath);
                    nodes.add(toNode(root, noteFile));
                    readablePaths.add(toRelative(root, noteFile));
                }
                catch (IOException | ServiceException ignored)
                {
                    // 文件可能在校验与读取属性之间被外部程序删除，跳过并在下方清理该陈旧记录。
                }
            }
            if (!readablePaths.equals(favoritePaths))
            {
                writeFavoritePaths(root, readablePaths);
            }
            return nodes;
        }
    }

    /**
     * 设置指定 Markdown 笔记的收藏状态。
     *
     * @param userName 当前登录用户名
     * @param path 笔记相对于当前用户 vault 的路径
     * @param favorite true 表示收藏，false 表示取消收藏
     * @return 写入后的最终收藏状态
     */
    @Override
    public boolean favorite(String userName, String path, Boolean favorite)
    {
        if (favorite == null)
        {
            throw new ServiceException("收藏状态不能为空");
        }
        synchronized (favoriteMetadataLock)
        {
            Path root = vaultRoot(userName);
            // 只允许收藏当前用户 vault 中真实存在的 Markdown 文件，并将路径统一为正斜杠相对路径。
            Path noteFile = resolveExistingFavoriteNote(userName, path);
            String favoritePath = toRelative(root, noteFile);
            Set<String> favoritePaths = loadValidFavoritePaths(userName);
            boolean changed = favorite ? favoritePaths.add(favoritePath) : favoritePaths.remove(favoritePath);
            if (changed)
            {
                writeFavoritePaths(root, favoritePaths);
            }
            return favoritePaths.contains(favoritePath);
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

    /**
     * 读取并校验收藏元数据，返回去重后的有效 Markdown 相对路径集合。
     *
     * <p>调用方必须持有 {@link #favoriteMetadataLock}。无效、越界、隐藏或已不存在的记录会被过滤，
     * 并通过安全替换方式回写清理后的结果。</p>
     *
     * @param userName 当前登录用户名
     * @return 按原收藏顺序排列的有效相对路径集合
     */
    private Set<String> loadValidFavoritePaths(String userName)
    {
        Path root = vaultRoot(userName);
        ensureDirectory(root);
        List<String> storedPaths = readFavoritePaths(root);
        Set<String> validPaths = new LinkedHashSet<>();
        for (String storedPath : storedPaths)
        {
            try
            {
                Path noteFile = resolveExistingFavoriteNote(userName, storedPath);
                validPaths.add(toRelative(root, noteFile));
            }
            catch (ServiceException ignored)
            {
                // 元数据可能包含已删除、已越界或非 Markdown 路径，查询时统一过滤并在下方回写清理。
            }
        }
        if (!new ArrayList<>(validPaths).equals(storedPaths))
        {
            writeFavoritePaths(root, validPaths);
        }
        return validPaths;
    }

    /**
     * 从固定隐藏文件读取收藏路径，空行直接跳过。
     *
     * @param root 当前用户 vault 根目录
     * @return 元数据中非空的原始路径列表
     */
    private List<String> readFavoritePaths(Path root)
    {
        Path metadataFile = favoriteMetadataPath(root);
        try
        {
            if (!Files.exists(metadataFile, LinkOption.NOFOLLOW_LINKS))
            {
                return new ArrayList<>();
            }
            if (Files.isSymbolicLink(metadataFile) || !Files.isRegularFile(metadataFile, LinkOption.NOFOLLOW_LINKS))
            {
                throw new ServiceException("收藏元数据文件非法");
            }
            List<String> storedPaths = new ArrayList<>();
            // 通过 NOFOLLOW_LINKS 通道读取，避免校验后路径被替换为符号链接时跟随到 vault 外部。
            try (SeekableByteChannel channel = Files.newByteChannel(metadataFile, StandardOpenOption.READ,
                    LinkOption.NOFOLLOW_LINKS);
                    BufferedReader reader = new BufferedReader(Channels.newReader(channel, StandardCharsets.UTF_8)))
            {
                String line;
                while ((line = reader.readLine()) != null)
                {
                    if (StringUtils.isNotBlank(line))
                    {
                        storedPaths.add(line);
                    }
                }
            }
            return storedPaths;
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (IOException e)
        {
            throw serviceException("读取收藏失败", e);
        }
        catch (RuntimeException e)
        {
            throw serviceException("读取收藏失败", e);
        }
    }

    /**
     * 将收藏路径去重后先完整写入隐藏临时文件，再原子替换正式元数据文件。
     *
     * @param root 当前用户 vault 根目录
     * @param favoritePaths 待持久化的规范化 Markdown 相对路径集合
     */
    private void writeFavoritePaths(Path root, Set<String> favoritePaths)
    {
        ensureDirectory(root);
        Path metadataFile = favoriteMetadataPath(root);
        List<String> uniquePaths = new ArrayList<>(new LinkedHashSet<>(favoritePaths));
        Path temporaryFile = null;
        try
        {
            // createTempFile 在 vault 内原子创建不可预测名称，避免攻击者预置固定临时路径或符号链接。
            temporaryFile = Files.createTempFile(root, ".favorites-", ".tmp");
            if (Files.isSymbolicLink(temporaryFile)
                    || !Files.isRegularFile(temporaryFile, LinkOption.NOFOLLOW_LINKS))
            {
                throw new IOException("收藏临时文件不是普通文件");
            }
            Files.write(temporaryFile, uniquePaths, StandardCharsets.UTF_8, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING, LinkOption.NOFOLLOW_LINKS);
            if (Files.isSymbolicLink(temporaryFile)
                    || !Files.isRegularFile(temporaryFile, LinkOption.NOFOLLOW_LINKS))
            {
                throw new IOException("收藏临时文件写入后类型异常");
            }
            assertSafeFavoriteMetadataTarget(metadataFile);
            try
            {
                // 文件系统支持时使用原子替换，避免进程在正式文件写到一半时留下残缺元数据。
                Files.move(temporaryFile, metadataFile, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            }
            catch (AtomicMoveNotSupportedException e)
            {
                // 跨平台文件系统不一定支持原子移动，退化为同目录覆盖仍可避免直接写正式文件的部分写风险。
                Files.move(temporaryFile, metadataFile, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (IOException e)
        {
            throw serviceException("保存收藏失败", e);
        }
        catch (RuntimeException e)
        {
            throw serviceException("保存收藏失败", e);
        }
        finally
        {
            if (temporaryFile != null)
            {
                try
                {
                    Files.deleteIfExists(temporaryFile);
                }
                catch (IOException | RuntimeException ignored)
                {
                    // 清理失败不覆盖主流程结果；随机临时文件以点号开头，仍不会进入树、搜索和同步结果。
                }
            }
        }
    }

    /**
     * 替换收藏元数据前确认固定目标不存在，或仍是未跟随链接的普通文件。
     *
     * @param metadataFile vault 根目录下固定的 .favorites 路径
     */
    private void assertSafeFavoriteMetadataTarget(Path metadataFile)
    {
        if (Files.exists(metadataFile, LinkOption.NOFOLLOW_LINKS)
                && (Files.isSymbolicLink(metadataFile)
                        || !Files.isRegularFile(metadataFile, LinkOption.NOFOLLOW_LINKS)))
        {
            throw new ServiceException("收藏元数据文件非法，拒绝替换");
        }
    }

    /**
     * 获取当前用户 vault 中固定的收藏元数据文件路径。
     *
     * <p>该内部路径不经过 {@link #resolvePath(String, String)}，避免为了访问隐藏元数据而放宽对外路径校验。</p>
     *
     * @param root 当前用户 vault 根目录
     * @return vault 根目录下的 .favorites 固定路径
     */
    private Path favoriteMetadataPath(Path root)
    {
        return root.resolve(FAVORITES_FILE_NAME);
    }

    /**
     * 解析并验证可收藏的真实 Markdown 文件。
     *
     * @param userName 当前登录用户名
     * @param relativePath 待校验的 vault 相对路径
     * @return 通过存在性、类型、隐藏路径和真实路径边界校验的文件路径
     */
    private Path resolveExistingFavoriteNote(String userName, String relativePath)
    {
        // 收藏元数据采用逐行格式，专用入口必须拒绝 CR/LF，避免单个文件名被拆成多条收藏记录。
        if (relativePath != null && (relativePath.contains("\r") || relativePath.contains("\n")))
        {
            throw new ServiceException("收藏笔记路径不能包含换行符");
        }
        Path root = vaultRoot(userName);
        Path noteFile = resolveNoteFile(userName, relativePath, true);
        try
        {
            Path realRoot = root.toRealPath();
            Path realNoteFile = noteFile.toRealPath();
            // 对符号链接解析后的真实目标重新执行边界、普通文件、Markdown 与隐藏路径校验。
            if (!realNoteFile.startsWith(realRoot))
            {
                throw new ServiceException("收藏路径越界");
            }
            if (!Files.isRegularFile(realNoteFile, LinkOption.NOFOLLOW_LINKS) || !isMarkdown(realNoteFile)
                    || isIgnored(realRoot, realNoteFile))
            {
                throw new ServiceException("只能收藏真实存在的非隐藏 Markdown 文件");
            }
            return noteFile;
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (IOException e)
        {
            throw new ServiceException("笔记不存在");
        }
        catch (RuntimeException e)
        {
            throw serviceException("校验收藏笔记失败", e);
        }
    }

    /**
     * 按文件精确路径或目录后代关系映射重命名后的收藏路径。
     *
     * @param favoritePaths 重命名前的有效收藏路径
     * @param sourcePath 原文件或目录相对路径
     * @param targetPath 新文件或目录相对路径
     * @param directory true 表示目录，需要同步映射后代收藏；false 表示仅映射精确文件路径
     * @return 映射并去重后的新收藏路径集合
     */
    private Set<String> renameFavoritePaths(Set<String> favoritePaths, String sourcePath, String targetPath,
            boolean directory)
    {
        Set<String> renamedPaths = new LinkedHashSet<>();
        String descendantPrefix = sourcePath + "/";
        for (String favoritePath : favoritePaths)
        {
            if (favoritePath.equals(sourcePath))
            {
                renamedPaths.add(targetPath);
            }
            else if (directory && favoritePath.startsWith(descendantPrefix))
            {
                renamedPaths.add(targetPath + favoritePath.substring(sourcePath.length()));
            }
            else
            {
                renamedPaths.add(favoritePath);
            }
        }
        return renamedPaths;
    }

    /**
     * 构造保留底层异常证据的业务异常。
     *
     * @param message 对调用方说明实际完成状态的业务消息
     * @param cause 导致当前业务失败的原始异常
     * @return 已记录 cause 和调试明细的业务异常
     */
    private ServiceException serviceException(String message, Throwable cause)
    {
        ServiceException exception = new ServiceException(message);
        exception.setDetailMessage(cause == null ? null : cause.getMessage());
        if (cause != null)
        {
            exception.initCause(cause);
        }
        return exception;
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
