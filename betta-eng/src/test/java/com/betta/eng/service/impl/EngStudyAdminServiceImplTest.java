package com.betta.eng.service.impl;

import com.betta.eng.domain.EngStudyRecord;
import com.betta.eng.domain.vo.EngStudyAdminUserVo;
import com.betta.eng.mapper.EngStudyAdminMapper;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** 管理员积分查询服务的无第三方依赖回归测试。 */
public class EngStudyAdminServiceImplTest
{
    /** 依次验证汇总条件转发、零积分契约和指定用户历史查询。 */
    public static void main(String[] args)
    {
        EngStudyAdminServiceImplTest test = new EngStudyAdminServiceImplTest();
        test.shouldForwardFiltersAndKeepZeroScoreUser();
        test.shouldQueryHistoryForSpecifiedUser();
        test.shouldKeepAdminMapperQueryContract();
    }

    /** 汇总查询应完整转发筛选条件，并保留 Mapper 返回的零积分用户。 */
    private void shouldForwardFiltersAndKeepZeroScoreUser()
    {
        AtomicReference<EngStudyAdminUserVo> captured = new AtomicReference<>();
        EngStudyAdminUserVo zeroScoreUser = new EngStudyAdminUserVo();
        zeroScoreUser.setUserId(7L);
        zeroScoreUser.setUserName("unused");
        zeroScoreUser.setNickName("未学习用户");
        zeroScoreUser.setTotalScore(0L);
        zeroScoreUser.setStudyCount(0L);

        EngStudyAdminMapper mapper = mapperProxy((method, arguments) ->
        {
            if ("selectUserScoreList".equals(method))
            {
                captured.set((EngStudyAdminUserVo) arguments[0]);
                return List.of(zeroScoreUser);
            }
            return List.of();
        });
        EngStudyAdminServiceImpl service = new EngStudyAdminServiceImpl(mapper);
        EngStudyAdminUserVo condition = new EngStudyAdminUserVo();
        condition.setUserName("use");
        condition.setNickName("学习");

        List<EngStudyAdminUserVo> result = service.selectUserScoreList(condition);

        assertSame(condition, captured.get(), "服务应原样转发用户名和昵称查询条件");
        assertEquals(1, result.size(), "零积分用户不应被业务层过滤");
        assertEquals(0L, result.get(0).getTotalScore(), "零记录用户累计积分应保持为零");
        assertEquals(0L, result.get(0).getStudyCount(), "零记录用户闯关次数应保持为零");
    }

    /** 历史查询应使用指定用户主键，并完整返回该用户记录。 */
    private void shouldQueryHistoryForSpecifiedUser()
    {
        AtomicReference<Long> capturedUserId = new AtomicReference<>();
        EngStudyRecord record = new EngStudyRecord();
        record.setId(21L);
        record.setUserId(9L);
        record.setScore(88);

        EngStudyAdminMapper mapper = mapperProxy((method, arguments) ->
        {
            if ("selectUserScoreHistory".equals(method))
            {
                capturedUserId.set((Long) arguments[0]);
                return List.of(record);
            }
            return List.of();
        });
        EngStudyAdminServiceImpl service = new EngStudyAdminServiceImpl(mapper);

        List<EngStudyRecord> result = service.selectUserScoreHistory(9L);

        assertEquals(9L, capturedUserId.get(), "历史查询应使用接口传入的用户主键");
        assertEquals(21L, result.get(0).getId(), "服务应完整返回 Mapper 查询结果");
        assertEquals(88, result.get(0).getScore(), "积分历史得分不应被业务层改写");
    }

    /** Mapper SQL 应保留未删除的零记录用户，并按用户主键倒序查询完整历史。 */
    private void shouldKeepAdminMapperQueryContract()
    {
        String resource = "mapper/eng/EngStudyAdminMapper.xml";
        try (var input = EngStudyAdminServiceImplTest.class.getClassLoader().getResourceAsStream(resource))
        {
            if (input == null)
            {
                throw new AssertionError("未找到管理员积分 Mapper XML");
            }
            String xml = new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceAll("\\s+", " ").toLowerCase();
            assertContains(xml, "from sys_user u left join eng_study_record r",
                    "汇总查询必须以用户表左连接学习记录以保留零记录用户");
            assertContains(xml, "where u.del_flag = '0'", "汇总查询必须排除已删除用户");
            assertContains(xml, "coalesce(sum(r.score), 0) total_score", "零记录用户累计积分必须返回零");
            assertContains(xml, "u.user_name like", "汇总查询必须支持用户名筛选");
            assertContains(xml, "u.nick_name like", "汇总查询必须支持昵称筛选");
            assertContains(xml, "where r.user_id = #{userid}", "历史查询必须限定指定用户主键");
            assertContains(xml, "order by r.create_time desc, r.id desc", "积分历史必须按时间和主键倒序");
        }
        catch (java.io.IOException exception)
        {
            throw new AssertionError("读取管理员积分 Mapper XML 失败", exception);
        }
    }

    /** 创建只处理两个查询方法的 Mapper 动态代理。 */
    private EngStudyAdminMapper mapperProxy(Invocation invocation)
    {
        return (EngStudyAdminMapper) Proxy.newProxyInstance(EngStudyAdminMapper.class.getClassLoader(),
                new Class<?>[] {EngStudyAdminMapper.class},
                (proxy, method, args) -> invocation.invoke(method.getName(), args));
    }

    /** 简单查询代理回调。 */
    @FunctionalInterface
    private interface Invocation
    {
        Object invoke(String method, Object[] arguments);
    }

    private static void assertSame(Object expected, Object actual, String message)
    {
        if (expected != actual)
        {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message)
    {
        if (expected == null ? actual != null : !expected.equals(actual))
        {
            throw new AssertionError(message + "，期望=" + expected + "，实际=" + actual);
        }
    }

    private static void assertContains(String text, String fragment, String message)
    {
        if (!text.contains(fragment))
        {
            throw new AssertionError(message);
        }
    }
}
