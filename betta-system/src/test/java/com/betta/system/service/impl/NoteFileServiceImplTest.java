package com.betta.system.service.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import com.betta.common.config.RuoYiConfig;

/** 笔记图片定时标记清理的无第三方依赖回归测试。 */
public class NoteFileServiceImplTest
{
    private static final String USER = "cleanup-test";
    private static final DateTimeFormatter BATCH_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /** 依次执行全部回归场景，无异常返回表示测试通过。 */
    public static void main(String[] args) throws Exception
    {
        NoteFileServiceImplTest test = new NoteFileServiceImplTest();
        test.shouldNotRecycleDuringSaveAndShouldApplyGracePeriod();
        test.shouldRecycleOldOrphanAndKeepReferencedImages();
        test.shouldRestoreLatestReferencedVersionWithoutOverwrite();
        test.shouldKeepRecentAndDeleteExpiredRecycledImages();
        test.shouldSkipUserWhenMarkdownReadFailsOrReferenceIsAmbiguous();
        test.shouldIsolateUsersAndRemainIdempotent();
    }

    private void shouldNotRecycleDuringSaveAndShouldApplyGracePeriod() throws Exception
    {
        withFixture(fixture -> {
            Path image = fixture.image(USER, "save/old.png", 48);
            fixture.note(USER, "save.md", "![图片](999.图片/save/old.png)");
            fixture.service.saveContent(USER, "save.md", "", null);
            assertTrue(Files.exists(image), "保存正文不得即时移动图片");
            Path recent = fixture.image(USER, "save/recent.png", 1);
            fixture.service.cleanupExpiredNoteImages(30);
            assertTrue(Files.exists(recent), "最近24小时内的孤立附件必须保留");
        });
    }

    private void shouldRecycleOldOrphanAndKeepReferencedImages() throws Exception
    {
        withFixture(fixture -> {
            Path orphan = fixture.image(USER, "orphan/old.png", 48);
            Path markdown = fixture.image(USER, "used/markdown.png", 48);
            Path obsidian = fixture.image(USER, "used/obsidian.jpg", 48);
            Path html = fixture.image(USER, "used/html.webp", 48);
            fixture.note(USER, "used.md", "![标准](999.图片/used/markdown.png)\n![[obsidian.jpg]]\n"
                    + "<img src=\"999.图片/used/html.webp\">");
            fixture.service.cleanupExpiredNoteImages(30);
            assertFalse(Files.exists(orphan), "超过24小时的孤立附件应进入系统回收区");
            assertTrue(Files.exists(markdown) && Files.exists(obsidian) && Files.exists(html), "三类引用图片均应保留");
            assertEquals(1, fixture.recycledVersions(USER, "999.图片/orphan/old.png").size(),
                    "回收路径应保留原 vault 相对路径");
        });
    }

    private void shouldRestoreLatestReferencedVersionWithoutOverwrite() throws Exception
    {
        withFixture(fixture -> {
            fixture.recycled(USER, 10, "999.图片/restore/picture.png", "older");
            Path latest = fixture.recycled(USER, 2, "999.图片/restore/picture.png", "latest");
            fixture.note(USER, "restore.md", "![恢复](999.图片/restore/picture.png)");
            fixture.service.cleanupExpiredNoteImages(30);
            Path original = fixture.root(USER).resolve("999.图片/restore/picture.png");
            assertEquals("latest", Files.readString(original), "应恢复最新回收版本");
            assertFalse(Files.exists(latest), "已恢复版本应离开回收区");
            Files.writeString(original, "existing", StandardCharsets.UTF_8);
            fixture.recycled(USER, 1, "999.图片/restore/picture.png", "must-not-overwrite");
            fixture.service.cleanupExpiredNoteImages(30);
            assertEquals("existing", Files.readString(original), "原位置已有文件时不得覆盖");
        });
    }

    private void shouldKeepRecentAndDeleteExpiredRecycledImages() throws Exception
    {
        withFixture(fixture -> {
            Path recent = fixture.recycled(USER, 20, "999.图片/trash/recent.png", "recent");
            Path expired = fixture.recycled(USER, 31, "999.图片/trash/expired.png", "expired");
            fixture.service.cleanupExpiredNoteImages(30);
            assertTrue(Files.exists(recent), "30天保留期内的回收图片必须保留");
            assertFalse(Files.exists(expired), "超过30天且无需恢复的图片应永久删除");
        });
    }

    private void shouldSkipUserWhenMarkdownReadFailsOrReferenceIsAmbiguous() throws Exception
    {
        withFixture(fixture -> {
            Path orphan = fixture.image(USER, "broken/orphan.png", 48);
            Path expired = fixture.recycled(USER, 31, "999.图片/broken/expired.png", "expired");
            Files.write(fixture.notePath(USER, "broken.md"), new byte[] {(byte) 0xC3});
            fixture.service.cleanupExpiredNoteImages(30);
            assertTrue(Files.exists(orphan) && Files.exists(expired), "读取失败时不得移动或永久删除图片");
        });
        withFixture(fixture -> {
            fixture.image(USER, "a/same.png", 48);
            fixture.image(USER, "b/same.png", 48);
            Path orphan = fixture.image(USER, "ambiguous/orphan.png", 48);
            fixture.note(USER, "ambiguous.md", "![[same.png]]");
            fixture.service.cleanupExpiredNoteImages(30);
            assertTrue(Files.exists(orphan), "短链接歧义时应跳过该用户清理");
        });
    }

    private void shouldIsolateUsersAndRemainIdempotent() throws Exception
    {
        withFixture(fixture -> {
            Path first = fixture.image(USER, "isolated/first.png", 48);
            Path second = fixture.image("other-user", "isolated/second.png", 48);
            fixture.note("other-user", "used.md", "![保留](999.图片/isolated/second.png)");
            fixture.service.cleanupExpiredNoteImages(30);
            fixture.service.cleanupExpiredNoteImages(30);
            assertFalse(Files.exists(first), "当前用户孤图应正常回收");
            assertTrue(Files.exists(second), "跨用户引用和文件必须隔离");
            assertEquals(1, fixture.recycledVersions(USER, "999.图片/isolated/first.png").size(),
                    "重复执行不得产生重复版本");
        });
    }

    private void withFixture(FixtureAction action) throws Exception
    {
        Path profile = Files.createTempDirectory("betta-note-cleanup-");
        RuoYiConfig config = new RuoYiConfig();
        config.setProfile(profile.toString());
        config.setNoteAttachmentLocation("999.图片/${noteFileName}");
        try { action.run(new Fixture(profile)); }
        finally { deleteRecursively(profile); }
    }

    private void deleteRecursively(Path path) throws IOException
    {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return;
        try (Stream<Path> paths = Files.walk(path))
        {
            for (Path item : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(item);
        }
    }

    private static void assertTrue(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void assertFalse(boolean condition, String message) { assertTrue(!condition, message); }
    private static void assertEquals(Object expected, Object actual, String message)
    {
        if (expected == null ? actual != null : !expected.equals(actual))
            throw new AssertionError(message + "，expected=" + expected + "，actual=" + actual);
    }

    @FunctionalInterface
    private interface FixtureAction { void run(Fixture fixture) throws Exception; }

    private static class Fixture
    {
        private final Path profile;
        private final NoteFileServiceImpl service = new NoteFileServiceImpl();
        Fixture(Path profile) { this.profile = profile; }
        Path root(String user) { return profile.resolve("notes").resolve(user); }
        Path notePath(String user, String path) throws IOException
        {
            Path note = root(user).resolve(path); Files.createDirectories(note.getParent()); return note;
        }
        Path note(String user, String path, String content) throws IOException
        {
            Path note = notePath(user, path); Files.writeString(note, content, StandardCharsets.UTF_8); return note;
        }
        Path image(String user, String path, int ageHours) throws IOException
        {
            Path image = root(user).resolve("999.图片").resolve(path); Files.createDirectories(image.getParent());
            Files.writeString(image, "image", StandardCharsets.UTF_8);
            Files.setLastModifiedTime(image, FileTime.from(Instant.now().minusSeconds(ageHours * 3600L))); return image;
        }
        Path recycled(String user, int ageDays, String originalPath, String content) throws IOException
        {
            String batch = LocalDateTime.now().minusDays(ageDays).format(BATCH_FORMAT);
            Path recycled = root(user).resolve(".trash/note-images").resolve(batch).resolve(originalPath);
            Files.createDirectories(recycled.getParent()); Files.writeString(recycled, content, StandardCharsets.UTF_8);
            return recycled;
        }
        List<Path> recycledVersions(String user, String originalPath) throws IOException
        {
            Path trash = root(user).resolve(".trash/note-images"); if (!Files.exists(trash)) return List.of();
            try (Stream<Path> batches = Files.list(trash))
            { return batches.map(batch -> batch.resolve(originalPath)).filter(Files::exists).toList(); }
        }
    }
}
