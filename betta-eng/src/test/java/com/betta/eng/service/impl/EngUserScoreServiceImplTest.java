package com.betta.eng.service.impl;

import com.betta.common.core.domain.entity.SysUser;
import com.betta.common.core.domain.model.LoginUser;
import com.betta.eng.domain.EngUserScore;
import com.betta.eng.mapper.EngUserScoreMapper;
import com.betta.eng.service.IEngSentenceService;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** 用户熟悉度服务的上下限与查询衰减回归入口。 */
public class EngUserScoreServiceImplTest {
    /** 执行新增、增量更新和只读衰减测试。 */
    public static void main(String[] args) {
        setLoginUser();
        try {
            shouldClampIncrementAndDecrement();
            shouldReturnEffectiveValueWithoutPersistingDecay();
            shouldKeepDialectAudioMapperContract();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private static void shouldClampIncrementAndDecrement() {
        EngUserScore stored = score(1L, 10, new Date());
        List<Integer> updates = new ArrayList<>();
        EngUserScoreMapper mapper = mapper(stored, updates, List.of());
        EngUserScoreServiceImpl service = new EngUserScoreServiceImpl(mapper, sentenceService());
        service.updateEngUserScore("apple", 1);
        stored.setFamiliarity(0);
        service.updateEngUserScore("apple", -1);
        assertEquals(List.of(10, 0), updates, "增量更新必须截断到零至十");
    }

    private static void shouldReturnEffectiveValueWithoutPersistingDecay() {
        long day = 24L * 60L * 60L * 1000L;
        EngUserScore queried = score(2L, 10, new Date(System.currentTimeMillis() - 365L * day));
        List<Integer> updates = new ArrayList<>();
        EngUserScoreServiceImpl service = new EngUserScoreServiceImpl(
                mapper(null, updates, List.of(queried)), sentenceService());
        EngUserScore condition = new EngUserScore();
        List<EngUserScore> result = service.selectEngUserScoreList(condition);
        assertEquals(9, result.get(0).getFamiliarity(), "查询应返回曲线衰减后的当前熟悉度");
        assertEquals(List.of(), updates, "查询衰减不得写回数据库");
    }

    /** Mapper 应同时映射并查询英式、美式音标与音频字段。 */
    private static void shouldKeepDialectAudioMapperContract() {
        String resource = "mapper/eng/EngUserScoreMapper.xml";
        try (var input = EngUserScoreServiceImplTest.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new AssertionError("未找到用户熟悉度 Mapper XML");
            }
            String xml = new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceAll("\\s+", " ").toLowerCase();
            assertContains(xml, "property=\"phoneticsuk\" column=\"phonetics_uk\"",
                    "结果映射必须包含英式音标");
            assertContains(xml, "property=\"phoneticsus\" column=\"phonetics_us\"",
                    "结果映射必须包含美式音标");
            assertContains(xml, "property=\"phmp3uk\" column=\"ph_mp3_uk\"",
                    "结果映射必须包含英式音频");
            assertContains(xml, "property=\"phmp3us\" column=\"ph_mp3_us\"",
                    "结果映射必须包含美式音频");
            assertContains(xml, "w.phonetics_uk", "文章单词查询必须返回英式音标");
            assertContains(xml, "w.phonetics_us", "文章单词查询必须返回美式音标");
            assertContains(xml, "w.ph_mp3_uk", "文章单词查询必须返回英式音频");
            assertContains(xml, "w.ph_mp3_us", "文章单词查询必须返回美式音频");
        } catch (java.io.IOException exception) {
            throw new AssertionError("读取用户熟悉度 Mapper XML 失败", exception);
        }
    }

    private static EngUserScoreMapper mapper(EngUserScore stored, List<Integer> updates,
            List<EngUserScore> queryResult) {
        return (EngUserScoreMapper) Proxy.newProxyInstance(EngUserScoreMapper.class.getClassLoader(),
                new Class<?>[] {EngUserScoreMapper.class}, (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "getByWordName" -> stored;
                        case "updateEngUserScore" -> {
                            updates.add(((EngUserScore) args[0]).getFamiliarity());
                            yield 1;
                        }
                        case "selectEngUserScoreList" -> queryResult;
                        default -> method.getReturnType().isPrimitive() ? 0 : null;
                    };
                });
    }

    private static IEngSentenceService sentenceService() {
        return (IEngSentenceService) Proxy.newProxyInstance(IEngSentenceService.class.getClassLoader(),
                new Class<?>[] {IEngSentenceService.class}, (proxy, method, args) -> null);
    }

    private static EngUserScore score(Long id, int familiarity, Date lastReviewTime) {
        EngUserScore score = new EngUserScore();
        score.setId(id);
        score.setUser("tester");
        score.setWordName("apple");
        score.setFamiliarity(familiarity);
        score.setUpdateTime(lastReviewTime);
        return score;
    }

    private static void setLoginUser() {
        SysUser user = new SysUser();
        user.setUserId(7L);
        user.setUserName("tester");
        LoginUser loginUser = new LoginUser();
        loginUser.setUser(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, List.of()));
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + "，expected=" + expected + "，actual=" + actual);
        }
    }

    private static void assertContains(String actual, String expected, String message) {
        if (!actual.contains(expected)) {
            throw new AssertionError(message + "，expected fragment=" + expected);
        }
    }
}
