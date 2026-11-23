package com.betta.web.controller.menstrual;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import com.betta.common.core.domain.AjaxResult;
import com.betta.other.domain.menstrual.MenstrualRecord;
import com.betta.other.service.IMenstrualService;
import tools.jackson.databind.json.JsonMapper;

/** 使用应用运行时 JSON 实现验证日期、用户字段和接口转发。 */
public class MenstrualControllerTest
{
    /** 执行接口绑定回归。 */
    public static void main(String[] args) throws Exception
    {
        JsonMapper json = new JsonMapper();
        MenstrualRecord record = json.readValue("{\"startDate\":\"2026-01-01\",\"endDate\":null,\"note\":\"备注\",\"userId\":99}", MenstrualRecord.class);
        assertEquals(LocalDate.of(2026, 1, 1), record.getStartDate(), "开始日期绑定");
        assertEquals(null, record.getUserId(), "不能绑定用户ID");
        record.setUserId(1L);
        String rendered = json.writeValueAsString(record);
        if (rendered.contains("userId") || !rendered.contains("2026-01-01")) throw new AssertionError("日期或用户信息序列化错误: " + rendered);
        Object[][] captured = new Object[1][];
        IMenstrualService service = (IMenstrualService) Proxy.newProxyInstance(IMenstrualService.class.getClassLoader(),
                new Class<?>[]{IMenstrualService.class}, (proxy, method, arguments) -> {
                    captured[0] = arguments;
                    if ("listRecords".equals(method.getName())) return List.of(record);
                    if ("createRecord".equals(method.getName()) || "updateRecord".equals(method.getName())) return record;
                    return null;
                });
        MenstrualController controller = new MenstrualController(service);
        AjaxResult list = controller.records();
        assertEquals(List.of(record), list.get("data"), "列表使用 AjaxResult data");
        controller.update(123L, record);
        assertEquals(123L, captured[0][0], "路径 ID 转发");
        assertEquals(record, captured[0][1], "请求体转发");
        System.out.println("MenstrualControllerTest: all scenarios passed");
    }

    private static void assertEquals(Object expected, Object actual, String label)
    {
        if (!Objects.equals(expected, actual)) throw new AssertionError(label + ": " + expected + " != " + actual);
    }
}
