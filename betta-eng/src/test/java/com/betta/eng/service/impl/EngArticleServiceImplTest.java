package com.betta.eng.service.impl;

import com.betta.common.core.domain.entity.SysUser;
import com.betta.common.core.domain.model.LoginUser;
import com.betta.common.exception.ServiceException;
import com.betta.eng.domain.EngArticle;
import com.betta.eng.mapper.EngArticleMapper;
import com.betta.eng.service.IEngArticleWordRelService;
import com.betta.eng.service.IEngSentenceService;
import com.betta.eng.service.IEngWordService;
import com.betta.eng.service.IPlayListService;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前词书切换的无第三方依赖回归测试，验证用户隔离、原子写入和异常边界。
 */
public class EngArticleServiceImplTest
{
    /** 依次执行全部词书切换回归场景。 */
    public static void main(String[] args)
    {
        EngArticleServiceImplTest test = new EngArticleServiceImplTest();
        test.shouldSwitchCurrentArticleForOwner();
        test.shouldUpsertWhenSelectingRepeatedlyOrChangingArticle();
        test.shouldRejectMissingOrUnauthorizedArticleWithoutWriting();
        test.shouldIsolateCurrentArticleByUserId();
    }

    /** 正常选择时按用户主键原子写入当前文章。 */
    private void shouldSwitchCurrentArticleForOwner()
    {
        Harness harness = new Harness(article(10L, "alice", "四级词书"));
        loginAs(1L, "alice");
        try
        {
            EngArticle selected = harness.service.setCurrent(10L);

            assertEquals(10L, selected.getId(), "应返回已选词书");
            assertEquals(1, harness.upsertCalls.get(), "正常切换应执行一次原子写入");
            assertEquals(10L, harness.currentByUserId.get(1L), "新记录应按用户主键关联选中文章");
            assertEquals("alice", harness.lastQueriedUsername, "写入前应按登录用户校验文章归属");
            assertEquals(1L, harness.lastWrittenUserId, "当前词书应使用登录用户主键");
            assertEquals("alice", harness.lastWrittenUsername, "审计字段应使用登录用户名");
        }
        finally
        {
            SecurityContextHolder.clearContext();
        }
    }

    /** 重复选择和更换文章均应走 upsert，且每个用户只保留一个当前值。 */
    private void shouldUpsertWhenSelectingRepeatedlyOrChangingArticle()
    {
        Harness harness = new Harness(
                article(10L, "alice", "四级词书"),
                article(11L, "alice", "六级词书"));
        loginAs(1L, "alice");
        try
        {
            harness.service.setCurrent(10L);
            harness.service.setCurrent(10L);
            harness.service.setCurrent(11L);

            assertEquals(3, harness.upsertCalls.get(), "每次选择都应执行一次 upsert");
            assertEquals(1, harness.currentByUserId.size(), "重复或更换选择后每个用户只保留一个当前值");
            assertEquals(11L, harness.currentByUserId.get(1L), "更换后应保留最后选中的文章");
        }
        finally
        {
            SecurityContextHolder.clearContext();
        }
    }

    /** 不存在和他人文章均应拒绝，且不得破坏旧选择。 */
    private void shouldRejectMissingOrUnauthorizedArticleWithoutWriting()
    {
        Harness harness = new Harness(article(20L, "bob", "Bob 的词书"));
        harness.currentByUserId.put(1L, 9L);
        loginAs(1L, "alice");
        try
        {
            assertServiceException(() -> harness.service.setCurrent(999L), "不存在的文章应被拒绝");
            assertServiceException(() -> harness.service.setCurrent(20L), "他人文章应被拒绝");

            assertEquals(0, harness.upsertCalls.get(), "校验失败时不得写入当前文章");
            assertEquals(9L, harness.currentByUserId.get(1L), "校验失败时应保留原有词书");
        }
        finally
        {
            SecurityContextHolder.clearContext();
        }
    }

    /** 当前文章记录必须以 userId 隔离，同时保留 username 的文章归属校验。 */
    private void shouldIsolateCurrentArticleByUserId()
    {
        Harness harness = new Harness(
                article(10L, "alice", "Alice 的词书"),
                article(20L, "bob", "Bob 的词书"));
        harness.currentByUserId.put(1L, 10L);
        harness.currentByUserId.put(2L, 20L);
        loginAs(1L, "alice");
        try
        {
            List<EngArticle> options = harness.service.selectCurrentOptions();
            EngArticle aliceCurrent = harness.service.getCurrent();

            assertEquals(1, options.size(), "候选列表只能包含当前用户文章");
            assertEquals("alice", options.get(0).getCreateBy(), "候选词书所有者应为当前用户");
            assertEquals(10L, aliceCurrent.getId(), "Alice 应读取自己 userId 下的当前文章");
            assertEquals(1L, harness.lastCurrentUserId, "当前文章查询应使用登录用户主键");
            assertEquals("alice", harness.lastCurrentUsername, "当前文章查询应保留用户名归属校验");

            loginAs(2L, "bob");
            EngArticle bobCurrent = harness.service.getCurrent();
            assertEquals(20L, bobCurrent.getId(), "Bob 应读取自己 userId 下的当前文章");
        }
        finally
        {
            SecurityContextHolder.clearContext();
        }
    }

    /** 设置固定登录用户，供 SecurityUtils 获取当前用户名。 */
    private static void loginAs(Long userId, String username)
    {
        SysUser user = new SysUser();
        user.setUserName(username);
        LoginUser loginUser = new LoginUser(userId, 1L, user, Set.of());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                loginUser, null, loginUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static EngArticle article(Long id, String username, String title)
    {
        EngArticle article = new EngArticle();
        article.setId(id);
        article.setCreateBy(username);
        article.setTitle(title);
        return article;
    }

    private static void assertServiceException(Runnable action, String message)
    {
        try
        {
            action.run();
            throw new AssertionError(message);
        }
        catch (ServiceException exception)
        {
            assertEquals("词书不存在或无权选择", exception.getMessage(), "业务异常提示应统一");
        }
    }

    private static void assertEquals(Object expected, Object actual, String message)
    {
        if (expected == null ? actual != null : !expected.equals(actual))
        {
            throw new AssertionError(message + "，期望=" + expected + "，实际=" + actual);
        }
    }

    /** 通过内存 Mapper 桩记录查询边界和当前词书写入。 */
    private static final class Harness
    {
        private final Map<Long, EngArticle> articles = new HashMap<>();
        private final Map<Long, Long> currentByUserId = new HashMap<>();
        private final AtomicInteger upsertCalls = new AtomicInteger();
        private String lastQueriedUsername;
        private String lastWrittenUsername;
        private Long lastWrittenUserId;
        private Long lastCurrentUserId;
        private String lastCurrentUsername;
        private final EngArticleServiceImpl service;

        private Harness(EngArticle... initialArticles)
        {
            for (EngArticle article : initialArticles)
            {
                articles.put(article.getId(), article);
            }
            service = new EngArticleServiceImpl(mapper(), proxy(IEngSentenceService.class),
                    proxy(IEngArticleWordRelService.class), proxy(IPlayListService.class), proxy(IEngWordService.class));
        }

        private EngArticleMapper mapper()
        {
            return (EngArticleMapper) Proxy.newProxyInstance(EngArticleMapper.class.getClassLoader(),
                    new Class<?>[] { EngArticleMapper.class }, (proxy, method, args) -> switch (method.getName())
                    {
                        case "selectEngArticleById" -> {
                            Long id = (Long) args[0];
                            String username = (String) args[1];
                            lastQueriedUsername = username;
                            EngArticle article = articles.get(id);
                            yield article != null && username.equals(article.getCreateBy()) ? article : null;
                        }
                        case "selectEngArticleList" -> {
                            EngArticle condition = (EngArticle) args[0];
                            List<EngArticle> result = new ArrayList<>();
                            for (EngArticle article : articles.values())
                            {
                                if (condition.getCreateBy().equals(article.getCreateBy()))
                                {
                                    result.add(article);
                                }
                            }
                            yield result;
                        }
                        case "getCurrentArticle" -> {
                            Long userId = (Long) args[0];
                            String username = (String) args[1];
                            lastCurrentUserId = userId;
                            lastCurrentUsername = username;
                            Long articleId = currentByUserId.get(userId);
                            EngArticle article = articles.get(articleId);
                            yield article != null && username.equals(article.getCreateBy()) ? article : null;
                        }
                        case "upsertCurrentArticle" -> {
                            Long userId = (Long) args[0];
                            Long articleId = (Long) args[1];
                            String username = (String) args[2];
                            lastWrittenUserId = userId;
                            lastWrittenUsername = username;
                            currentByUserId.put(userId, articleId);
                            yield upsertCalls.incrementAndGet();
                        }
                        default -> defaultValue(method.getReturnType());
                    });
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type)
    {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
                (proxy, method, args) -> defaultValue(method.getReturnType()));
    }

    private static Object defaultValue(Class<?> type)
    {
        if (type == boolean.class)
        {
            return false;
        }
        if (type == int.class)
        {
            return 0;
        }
        if (type == long.class)
        {
            return 0L;
        }
        return null;
    }
}
