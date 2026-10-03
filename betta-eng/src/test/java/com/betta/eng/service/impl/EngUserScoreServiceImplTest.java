package com.betta.eng.service.impl;

import com.betta.common.core.domain.entity.SysUser;
import com.betta.common.core.domain.model.LoginUser;
import com.betta.eng.domain.EngUserScore;
import com.betta.eng.mapper.EngUserScoreMapper;
import com.betta.eng.service.IEngSentenceService;
import java.lang.reflect.Proxy;
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
}
