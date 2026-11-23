package com.betta.other.service.impl;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.betta.common.core.domain.entity.SysUser;
import com.betta.common.core.domain.model.LoginUser;
import com.betta.common.exception.ServiceException;
import com.betta.other.domain.menstrual.MenstrualRecord;
import com.betta.other.domain.menstrual.MenstrualSummary;
import com.betta.other.mapper.MenstrualMapper;

/** 无外部数据库的经期业务回归入口。 */
public class MenstrualServiceImplTest
{
    /** 验证隔离、日期、统计、更新以及映射。 */
    public static void main(String[] args) throws Exception
    {
        FakeMapper mapper = new FakeMapper();
        MenstrualServiceImpl service = new MenstrualServiceImpl(mapper);
        try
        {
            login(1);
            assertEquals(null, service.summary().getAveragePeriodDays(), "空统计");
            assertError(() -> service.createRecord(null));
            assertError(() -> service.createRecord(record("2026-01-02", "2026-01-01")));
            MenstrualRecord first = service.createRecord(record("2026-01-01", null));
            assertEquals(null, service.summary().getPredictedStartDate(), "一条记录不预测");
            assertEquals(new BigDecimal("7.0"), service.summary().getAveragePeriodDays(), "空结束日期按7天");
            assertError(() -> service.createRecord(record("2026-01-01", null)));
            service.createRecord(record("2026-01-29", "2026-02-02"));
            service.createRecord(record("2026-02-28", null));
            service.createRecord(record("2026-03-27", null));
            service.createRecord(record("2026-04-24", null));
            MenstrualSummary summary = service.summary();
            assertEquals(28L, summary.getAverageCycleDays(), "最近三周期平均四舍五入");
            assertEquals(LocalDate.parse("2026-05-22"), summary.getPredictedStartDate(), "预测日期");
            assertEquals(new BigDecimal("6.6"), summary.getAveragePeriodDays(), "平均经期包含默认值");
            service.updateRecord(first.getId(), record("2026-01-01", "2026-01-03"));
            assertEquals(new BigDecimal("5.8"), service.summary().getAveragePeriodDays(), "补录后重新计算");
            assertError(() -> service.updateRecord(first.getId(), record("2026-01-29", null)));
            login(2);
            assertEquals(0, service.listRecords().size(), "用户隔离");
            assertError(() -> service.updateRecord(first.getId(), record("2026-01-01", null)));
            assertError(() -> service.deleteRecord(first.getId()));
            service.createRecord(record("2026-01-01", null));
            assertEquals(1, service.listRecords().size(), "不同用户可使用相同日期");
            login(1);
            service.deleteRecord(first.getId());
            assertEquals(4, service.summary().getRecordCount(), "删除后重新统计");
            try (InputStream stream = MenstrualServiceImplTest.class.getClassLoader().getResourceAsStream("mapper/other/MenstrualMapper.xml"))
            {
                Configuration config = new Configuration();
                new XMLMapperBuilder(stream, config, "mapper/other/MenstrualMapper.xml", config.getSqlFragments()).parse();
                assertEquals(true, config.hasStatement("com.betta.other.mapper.MenstrualMapper.selectRecords"), "XML映射解析");
            }
            System.out.println("MenstrualServiceImplTest: all scenarios passed");
        }
        finally
        {
            SecurityContextHolder.clearContext();
        }
    }

    private static MenstrualRecord record(String start, String end)
    {
        MenstrualRecord r = new MenstrualRecord();
        r.setStartDate(LocalDate.parse(start));
        r.setEndDate(end == null ? null : LocalDate.parse(end));
        return r;
    }

    private static void login(long id)
    {
        LoginUser user = new LoginUser(id, 1L, new SysUser(), Set.of());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private static void assertError(Runnable action)
    {
        try { action.run(); throw new AssertionError("预期业务异常"); }
        catch (ServiceException expected) { }
    }

    private static void assertEquals(Object expected, Object actual, String label)
    {
        if (!Objects.equals(expected, actual)) throw new AssertionError(label + ": " + expected + " != " + actual);
    }

    private static class FakeMapper implements MenstrualMapper
    {
        private final Map<Long, MenstrualRecord> data = new HashMap<>();
        private long nextId = 1;
        public List<MenstrualRecord> selectRecords(Long userId)
        {
            List<MenstrualRecord> result = new ArrayList<>();
            for (MenstrualRecord r : data.values()) if (r.getUserId().equals(userId)) result.add(r);
            result.sort(Comparator.comparing(MenstrualRecord::getStartDate).reversed());
            return result;
        }
        public MenstrualRecord selectRecord(Long userId, Long id)
        {
            MenstrualRecord r = data.get(id);
            return r != null && r.getUserId().equals(userId) ? r : null;
        }
        public int insertRecord(MenstrualRecord record)
        {
            unique(record);
            record.setId(nextId++);
            data.put(record.getId(), record);
            return 1;
        }
        public int updateRecord(MenstrualRecord record)
        {
            unique(record);
            if (selectRecord(record.getUserId(), record.getId()) == null) return 0;
            data.put(record.getId(), record);
            return 1;
        }
        public int deleteRecord(Long userId, Long id)
        {
            if (selectRecord(userId, id) == null) return 0;
            data.remove(id);
            return 1;
        }
        private void unique(MenstrualRecord record)
        {
            for (MenstrualRecord r : data.values())
            {
                if (r.getUserId().equals(record.getUserId()) && r.getStartDate().equals(record.getStartDate())
                        && !Objects.equals(r.getId(), record.getId())) throw new DuplicateKeyException("duplicate");
            }
        }
    }
}
