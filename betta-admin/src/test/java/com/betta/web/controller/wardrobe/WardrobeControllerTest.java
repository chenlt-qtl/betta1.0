package com.betta.web.controller.wardrobe;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.betta.common.core.domain.AjaxResult;
import com.betta.common.exception.ServiceException;
import com.betta.other.domain.wardrobe.WardrobeItem;
import com.betta.other.domain.wardrobe.WardrobeOutfit;
import com.betta.other.domain.wardrobe.WardrobePositionUpdate;
import com.betta.other.service.IWardrobeService;
import tools.jackson.databind.json.JsonMapper;

/** 使用应用运行时的 Jackson 3 检查排序接口绑定及隐藏内部字段。 */
public class WardrobeControllerTest
{
    /** 执行排序接口的参数绑定与响应回归。 */
    public static void main(String[] args) throws Exception
    {
        JsonMapper json = new JsonMapper();
        Object[][] lastCall = new Object[1][];
        int[] calls = new int[1];
        IWardrobeService service = (IWardrobeService) Proxy.newProxyInstance(IWardrobeService.class.getClassLoader(),
                new Class<?>[]{IWardrobeService.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("moveOutfit"))
                    {
                        calls[0]++;
                        lastCall[0] = arguments;
                        if (!"up".equals(arguments[1]) && !"down".equals(arguments[1]))
                        {
                            throw new ServiceException("排序方向只能为 up 或 down");
                        }
                        return "up".equals(arguments[1]);
                    }
                    return null;
                });
        WardrobeController controller = new WardrobeController(service);
        Method endpoint = WardrobeController.class.getMethod("moveOutfit", Long.class, WardrobePositionUpdate.class);
        assertEquals("/outfits/{id}/position", endpoint.getAnnotation(PutMapping.class).value()[0], "排序接口路径");
        WardrobePositionUpdate up = json.readValue("{\"direction\":\"up\"}", WardrobePositionUpdate.class);
        WardrobePositionUpdate down = json.readValue("{\"direction\":\"down\"}", WardrobePositionUpdate.class);
        AjaxResult moved = controller.moveOutfit(42L, up);
        assertEquals(true, moved.get("data"), "已移动应返回 true");
        assertEquals(42L, lastCall[0][0], "搭配 ID 应正确传给服务");
        assertEquals("up", lastCall[0][1], "上移方向应正确绑定");
        AjaxResult boundary = controller.moveOutfit(42L, down);
        assertEquals(false, boundary.get("data"), "到达边界应返回 false");
        assertEquals("down", lastCall[0][1], "下移方向应正确绑定");
        assertError(() -> controller.moveOutfit(42L, json.readValue("{}", WardrobePositionUpdate.class)));
        assertError(() -> controller.moveOutfit(42L, null));
        assertEquals(4, calls[0], "每次请求交由服务校验");

        WardrobeOutfit outfit = new WardrobeOutfit();
        outfit.setSortOrder(10L);
        outfit.setCreateTime(LocalDateTime.of(2026, 1, 1, 0, 0));
        outfit.setUserId(123L);
        String rendered = json.writeValueAsString(outfit);
        assertTrue(!rendered.contains("sortOrder") && !rendered.contains("createTime") && !rendered.contains("userId"),
                "排序值、创建时间和用户 ID 不应暴露");
        WardrobeOutfit forged = json.readValue("{\"sortOrder\":99,\"userId\":456}", WardrobeOutfit.class);
        assertEquals(null, forged.getSortOrder(), "请求不能覆盖排序值");
        assertEquals(null, forged.getUserId(), "请求不能覆盖用户 ID");
        String item = json.writeValueAsString(new WardrobeItem());
        assertTrue(!item.contains("brand") && !item.contains("size") && !item.contains("purchaseDate") && !item.contains("price"),
                "单品接口不应返回已移除字段");
        shouldIgnoreOutfitSortParameters();
        System.out.println("WardrobeControllerTest: all scenarios passed");
    }

    private static void shouldIgnoreOutfitSortParameters()
    {
        Page<?>[] captured = new Page<?>[1];
        IWardrobeService service = (IWardrobeService) Proxy.newProxyInstance(IWardrobeService.class.getClassLoader(),
                new Class<?>[]{IWardrobeService.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("listOutfits") || method.getName().equals("listItems"))
                    {
                        captured[0] = PageHelper.getLocalPage();
                        return List.of();
                    }
                    return null;
                });
        WardrobeController controller = new WardrobeController(service);
        RequestAttributes previous = RequestContextHolder.getRequestAttributes();
        try
        {
            setRequest(Map.of("pageNum", "0", "pageSize", "12", "orderByColumn", "id", "isAsc", "asc"));
            // 同线程若残留仅排序上下文，搭配分页也必须清空该排序。
            PageHelper.orderBy("id asc");
            controller.outfits();
            assertEquals(1, captured[0].getPageNum(), "搭配页码应保持默认合理化");
            assertEquals(12, captured[0].getPageSize(), "搭配页大小应来自请求");
            assertEquals(true, captured[0].getReasonable(), "默认启用合理化");
            assertEquals("", captured[0].getOrderBy(), "客户端排序及残留排序均不得覆盖搭配顺序");
            PageHelper.clearPage();

            setRequest(Map.of("pageNum", "0", "pageSize", "7", "reasonable", "false", "orderByColumn", "name", "isAsc", "desc"));
            controller.outfits();
            assertEquals(0, captured[0].getPageNum(), "可按现有约定关闭页码合理化");
            assertEquals(7, captured[0].getPageSize(), "关闭合理化时保留页大小");
            assertEquals(false, captured[0].getReasonable(), "合理化参数应按现有行为传递");
            assertEquals("", captured[0].getOrderBy(), "任意客户端排序均应忽略");
            PageHelper.clearPage();

            setRequest(Map.of("pageNum", "1", "pageSize", "12", "orderByColumn", "id", "isAsc", "asc"));
            controller.items(new WardrobeItem());
            assertEquals("id asc", captured[0].getOrderBy(), "单品接口仍使用通用分页排序");
            PageHelper.clearPage();

            setRequest(Map.of());
            controller.outfits();
            assertEquals(1, captured[0].getPageNum(), "默认页码不变");
            assertEquals(10, captured[0].getPageSize(), "默认页大小不变");
            assertEquals(true, captured[0].getReasonable(), "默认合理化不变");
        }
        finally
        {
            PageHelper.clearPage();
            RequestContextHolder.setRequestAttributes(previous);
        }
    }

    private static void setRequest(Map<String, String> parameters)
    {
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("getParameter"))
                    {
                        return parameters.get(arguments[0]);
                    }
                    return null;
                });
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private static void assertError(Runnable call)
    {
        try
        {
            call.run();
            throw new AssertionError("缺少方向应被拒绝");
        }
        catch (ServiceException e)
        {
            assertEquals("排序方向只能为 up 或 down", e.getMessage(), "无效排序方向");
        }
    }

    private static void assertTrue(boolean value, String message)
    {
        if (!value) throw new AssertionError(message);
    }

    private static void assertEquals(Object expected, Object actual, String message)
    {
        if (!Objects.equals(expected, actual))
        {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }
}
