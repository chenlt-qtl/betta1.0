package com.betta.other.service.impl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.betta.common.core.domain.entity.SysUser;
import com.betta.common.core.domain.model.LoginUser;
import com.betta.common.exception.ServiceException;
import com.betta.other.domain.wardrobe.WardrobeImage;
import com.betta.other.domain.wardrobe.WardrobeItem;
import com.betta.other.domain.wardrobe.WardrobeOutfit;
import com.betta.other.mapper.WardrobeMapper;

/** 无外部数据库的衣橱业务回归入口；SQL 并发约束仍需数据库集成验证。 */
public class WardrobeServiceImplTest
{
    private final FakeMapper mapper = new FakeMapper();
    private final WardrobeServiceImpl service = new WardrobeServiceImpl(mapper);
    private byte[] png;
    private byte[] jpg;

    /** 执行输入、用户隔离、效果照、排序和推荐业务回归。 */
    public static void main(String[] args) throws Exception
    {
        WardrobeServiceImplTest test = new WardrobeServiceImplTest();
        test.png = image("png");
        test.jpg = image("jpg");
        try
        {
            test.shouldValidateItemsAndProtectImages();
            test.shouldRejectHugeImageHeadersBeforeDecoding();
            test.shouldManageOutfitImagesAndReferences();
            test.shouldMoveOutfitsAcrossFullList();
            test.shouldRecommendByTemperatureOccasionAndCreatedTime();
            test.shouldDeclareTransactionsAndParseMapper();
        }
        finally
        {
            SecurityContextHolder.clearContext();
        }
        System.out.println("WardrobeServiceImplTest: all scenarios passed");
    }

    private void shouldValidateItemsAndProtectImages()
    {
        login(1);
        assertError(() -> service.createItem(item("无效", "春"), file(png)), "请选择有效的单品分类");
        assertError(() -> service.createItem(item("家居服", "春"), null), "请选择图片");
        assertError(() -> service.createItem(item("家居服", "春"), file(new byte[]{1, 2, 3})), "仅支持有效的 JPG 或 PNG 图片");
        assertError(() -> service.createItem(item("家居服", "春"), file(png, 5L * 1024 * 1024 + 1)), "图片不能为空且不能超过5MB");
        WardrobeItem invalidSeason = item("家居服", "全年");
        assertError(() -> service.createItem(invalidSeason, file(png)), "季节无效");
        WardrobeItem created = service.createItem(item("家居服", "春"), file(png));
        assertEquals("image/png", service.getItemImage(created.getId()).getContentType(), "识别图片内容");
        created.setName("居家上衣");
        service.updateItem(created.getId(), created, null);
        assertEquals("image/png", service.getItemImage(created.getId()).getContentType(), "不传图片应保留原图");
        login(2);
        assertEquals(null, service.getItemImage(created.getId()), "其他用户不得读取图片");
        assertError(() -> service.getItem(created.getId()), "单品不存在");
        assertError(() -> service.deleteItem(created.getId()), "单品不存在");
    }

    private void shouldRejectHugeImageHeadersBeforeDecoding()
    {
        login(1);
        String message = "图片尺寸过大，最长边不能超过16384像素且总像素不能超过4000万";
        assertError(() -> service.createItem(item("上衣"), file(pngWithSize(16385, 1))), message);
        assertError(() -> service.createItem(item("上衣"), file(pngWithSize(8000, 5001))), message);
    }

    private byte[] pngWithSize(int width, int height)
    {
        // 修改 PNG 的 IHDR 并更新 CRC，不需要构造解码会耗尽内存的真实巨图。
        byte[] result = png.clone();
        ByteBuffer.wrap(result).putInt(16, width).putInt(20, height);
        CRC32 crc = new CRC32();
        crc.update(result, 12, 17);
        ByteBuffer.wrap(result).putInt(29, (int) crc.getValue());
        return result;
    }

    private void shouldManageOutfitImagesAndReferences()
    {
        login(1);
        WardrobeItem item = service.createItem(item("上衣", "春"), file(png));
        WardrobeOutfit invalid = outfit("重复", item.getId(), item.getId());
        assertError(() -> service.createOutfit(invalid, null), "请选择不重复的单品");
        WardrobeOutfit created = service.createOutfit(outfit("通勤", item.getId()), file(png));
        Long id = created.getId();
        assertTrue(created.getHasImage(), "新增效果照应被检测");
        assertEquals("image/png", service.getOutfitImage(id).getContentType(), "可读取本人的效果照");
        assertError(() -> service.deleteItem(item.getId()), "该单品已用于搭配，请先从搭配中移除");
        WardrobeOutfit edit = outfit("通勤", item.getId());
        edit.setImageAction("KEEP");
        service.updateOutfit(id, edit, null);
        assertEquals("image/png", service.getOutfitImage(id).getContentType(), "KEEP 不应清除原照片");
        edit.setImageAction("REPLACE");
        assertError(() -> service.updateOutfit(id, edit, null), "请选择图片");
        service.updateOutfit(id, edit, file(jpg));
        assertEquals("image/jpeg", service.getOutfitImage(id).getContentType(), "REPLACE 应更换照片");
        edit.setImageAction("REMOVE");
        service.updateOutfit(id, edit, null);
        assertEquals(null, service.getOutfitImage(id), "REMOVE 应移除照片");
        login(2);
        assertEquals(null, service.getOutfitImage(id), "其他用户不得读取效果照");
        assertError(() -> service.createOutfit(outfit("跨用户", item.getId()), null), "单品不存在");
        assertError(() -> service.updateOutfit(id, edit, null), "搭配不存在");
        assertError(() -> service.deleteOutfit(id), "搭配不存在");
        login(1);
        service.deleteOutfit(id);
        assertEquals(null, service.getOutfitImage(id), "删除搭配应清除效果照");
        service.deleteItem(item.getId());
        assertEquals(null, service.getItemImage(item.getId()), "解除搭配引用后可删除单品");
    }

    private void shouldMoveOutfitsAcrossFullList()
    {
        login(4);
        WardrobeItem item = service.createItem(item("上衣", "春"), file(png));
        for (int i = 0; i < 15; i++)
        {
            service.createOutfit(outfit("搭配" + i, item.getId()), null);
        }
        List<WardrobeOutfit> before = service.listOutfits();
        Long first = before.get(0).getId();
        Long last = before.get(before.size() - 1).getId();
        assertTrue(!service.moveOutfit(first, "up"), "首项不能继续上移");
        assertTrue(!service.moveOutfit(last, "down"), "末项不能继续下移");
        assertError(() -> service.moveOutfit(first, null), "排序方向只能为 up 或 down");
        assertError(() -> service.moveOutfit(first, "left"), "排序方向只能为 up 或 down");
        assertError(() -> service.moveOutfit(Long.MAX_VALUE, "up"), "搭配不存在");
        Long pageOneLast = before.get(11).getId();
        Long pageTwoFirst = before.get(12).getId();
        assertTrue(service.moveOutfit(pageOneLast, "down"), "允许跨页下移");
        List<WardrobeOutfit> after = service.listOutfits();
        assertEquals(pageTwoFirst, after.get(11).getId(), "第二页首项应上移到第一页");
        assertEquals(pageOneLast, after.get(12).getId(), "第一页末项应下移到第二页");
        List<Long> locks = mapper.lastLockedIds;
        assertEquals(locks.stream().sorted().toList(), locks, "必须按 ID 固定顺序加锁");
        assertTrue(service.moveOutfit(pageOneLast, "up"), "允许跨页上移");
        assertEquals(pageOneLast, service.listOutfits().get(11).getId(), "上移后恢复原位置");
        WardrobeOutfit newlyCreated = service.createOutfit(outfit("新搭配", item.getId()), null);
        assertEquals(newlyCreated.getId(), service.listOutfits().get(0).getId(), "新搭配默认排最前");
        WardrobeOutfit newer = service.createOutfit(outfit("更新搭配", item.getId()), null);
        assertEquals(newer.getId(), service.listOutfits().get(0).getId(), "相同默认值按 ID 倒序");
        Long beforeEditOrder = service.getOutfit(pageOneLast).getSortOrder();
        LocalDateTime beforeEditTime = service.getOutfit(pageOneLast).getCreateTime();
        service.updateOutfit(pageOneLast, outfit("修改名称", item.getId()), null);
        assertEquals(beforeEditOrder, service.getOutfit(pageOneLast).getSortOrder(), "编辑不能覆盖排序值");
        assertEquals(beforeEditTime, service.getOutfit(pageOneLast).getCreateTime(), "编辑不能覆盖创建时间");
        service.deleteOutfit(newer.getId());
        assertEquals(newlyCreated.getId(), service.listOutfits().get(0).getId(), "删除后顺序保持");
        login(5);
        assertError(() -> service.moveOutfit(pageOneLast, "up"), "搭配不存在");
        assertError(() -> service.moveOutfit(1L, "down"), "搭配不存在");
        WardrobeItem own = service.createItem(item("家居服"), file(png));
        Long only = service.createOutfit(outfit("单条", own.getId()), null).getId();
        assertTrue(!service.moveOutfit(only, "up") && !service.moveOutfit(only, "down"), "单条记录为首尾");
    }

    private void shouldRecommendByTemperatureOccasionAndCreatedTime()
    {
        login(3);
        WardrobeItem winter = service.createItem(item("外套", "冬"), file(png));
        WardrobeItem spring = service.createItem(item("上衣", "春"), file(png));
        WardrobeItem summer = service.createItem(item("上衣", "夏"), file(png));
        WardrobeItem neutral = service.createItem(item("鞋"), file(png));
        WardrobeOutfit winterOutfit = service.createOutfit(outfit("冬季", winter.getId()), null);
        WardrobeOutfit springOutfit = service.createOutfit(outfit("春季", spring.getId()), null);
        WardrobeOutfit summerOutfit = service.createOutfit(outfit("夏季", summer.getId()), null);
        WardrobeOutfit universal = outfit("通用", neutral.getId());
        universal.setFavorite(true);
        universal = service.createOutfit(universal, null);
        assertNames(service.recommend(11.9, null), "通用", "冬季");
        assertNames(service.recommend(12.0, null), "通用", "春季");
        assertNames(service.recommend(23.99, null), "通用", "春季");
        assertNames(service.recommend(24.0, null), "通用", "夏季");
        WardrobeOutfit changed = outfit("春季", spring.getId());
        changed.setOccasions(List.of("通勤"));
        service.updateOutfit(springOutfit.getId(), changed, null);
        assertNames(service.recommend(12.0, "居家"), "通用");
        assertNames(service.recommend(12.0, "通勤"), "通用", "春季");
        LocalDateTime time = LocalDateTime.of(2026, 1, 1, 12, 0);
        mapper.outfits.get(winterOutfit.getId()).setCreateTime(time.plusSeconds(20));
        mapper.outfits.get(springOutfit.getId()).setCreateTime(time.plusSeconds(10));
        mapper.outfits.get(summerOutfit.getId()).setCreateTime(time.plusSeconds(10));
        List<WardrobeOutfit> all = service.recommend(null, null);
        assertNames(all, "通用", "冬季", "夏季", "春季");
        assertTrue(service.moveOutfit(winterOutfit.getId(), "up"), "可以手动移动搭配");
        assertNames(service.recommend(null, null), "通用", "冬季", "夏季", "春季");
        assertError(() -> service.recommend(Double.NaN, null), "温度无效");
        assertTrue(summerOutfit.getId() != null && universal.getId() != null, "推荐数据应正常保存");
    }

    private void shouldDeclareTransactionsAndParseMapper() throws Exception
    {
        for (Method method : WardrobeServiceImpl.class.getDeclaredMethods())
        {
            if (Set.of("createItem", "updateItem", "deleteItem", "createOutfit", "updateOutfit", "deleteOutfit", "moveOutfit").contains(method.getName()))
            {
                assertTrue(method.isAnnotationPresent(Transactional.class), method.getName() + " 必须在事务内执行");
            }
        }
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("mapper/other/WardrobeMapper.xml"))
        {
            assertTrue(in != null, "Mapper XML 资源存在");
            Configuration config = new Configuration();
            new XMLMapperBuilder(in, config, "mapper/other/WardrobeMapper.xml", config.getSqlFragments()).parse();
            assertTrue(config.hasStatement("com.betta.other.mapper.WardrobeMapper.lockUserOutfits"), "排序 Mapper 可解析");
            assertTrue(config.hasStatement("com.betta.other.mapper.WardrobeMapper.updateOutfitSortOrder"), "排序更新可解析");
            String sql = config.getMappedStatement("com.betta.other.mapper.WardrobeMapper.selectOutfits")
                    .getBoundSql(Map.of("userId", 1L)).getSql();
            assertTrue(sql.contains("o.sort_order ASC, o.id DESC"), "分页前应在 SQL 中排序");
            assertTrue(!sql.contains("wardrobe_calendar"), "查询不能依赖日历表");
        }
    }

    private static void assertNames(List<WardrobeOutfit> actual, String... expected)
    {
        assertEquals(List.of(expected), actual.stream().map(WardrobeOutfit::getName).toList(), "推荐顺序及筛选");
    }

    private static WardrobeItem item(String category, String... seasons)
    {
        WardrobeItem item = new WardrobeItem();
        item.setCategory(category);
        item.setSeasons(List.of(seasons));
        return item;
    }

    private static WardrobeOutfit outfit(String name, Long... itemIds)
    {
        WardrobeOutfit outfit = new WardrobeOutfit();
        outfit.setName(name);
        outfit.setItemIds(List.of(itemIds));
        return outfit;
    }

    private static byte[] image(String format) throws Exception
    {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, format, output), "测试图片编码成功");
        return output.toByteArray();
    }

    private static MultipartFile file(byte[] data)
    {
        return file(data, data.length);
    }

    private static MultipartFile file(byte[] data, long reportedSize)
    {
        return (MultipartFile) Proxy.newProxyInstance(MultipartFile.class.getClassLoader(), new Class<?>[]{MultipartFile.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getBytes" -> data;
                    case "getSize" -> reportedSize;
                    case "isEmpty" -> data.length == 0;
                    case "getName" -> "image";
                    case "getOriginalFilename" -> "test.jpg";
                    case "getContentType" -> "image/jpeg";
                    default -> null;
                });
    }

    private static void login(long userId)
    {
        LoginUser user = new LoginUser(userId, 1L, new SysUser(), Set.of());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private static void assertError(Runnable action, String message)
    {
        try
        {
            action.run();
            throw new AssertionError("应抛出异常: " + message);
        }
        catch (ServiceException e)
        {
            assertEquals(message, e.getMessage(), "错误信息");
        }
    }

    private static void assertTrue(boolean value, String message)
    {
        if (!value) throw new AssertionError(message);
    }

    private static void assertEquals(Object expected, Object actual, String message)
    {
        if (!Objects.equals(expected, actual)) throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
    }

    /** 模拟持久层隔离与记录关系，避免把独立业务测试当成数据库并发测试。 */
    private static class FakeMapper implements WardrobeMapper
    {
        private long itemSeq;
        private long outfitSeq;
        private final Map<Long, WardrobeItem> items = new HashMap<>();
        private final Map<Long, WardrobeOutfit> outfits = new HashMap<>();
        private final Map<Long, WardrobeImage> itemImages = new HashMap<>();
        private final Map<Long, WardrobeImage> outfitImages = new HashMap<>();
        private final Map<Long, List<Long>> relations = new HashMap<>();
        private List<Long> lastLockedIds = List.of();

        @Override public List<WardrobeItem> selectItems(WardrobeItem q)
        {
            return items.values().stream().filter(i -> i.getUserId().equals(q.getUserId())).sorted(Comparator.comparing(WardrobeItem::getId).reversed()).toList();
        }
        @Override public WardrobeItem selectItem(Long userId, Long id)
        {
            WardrobeItem item = items.get(id);
            return item != null && item.getUserId().equals(userId) ? item : null;
        }
        @Override public Long lockItem(Long userId, Long id) { return selectItem(userId, id) == null ? null : id; }
        @Override public int insertItem(WardrobeItem item) { item.setId(++itemSeq); items.put(item.getId(), item); return 1; }
        @Override public int updateItem(WardrobeItem item) { items.put(item.getId(), item); return 1; }
        @Override public int deleteItem(Long userId, Long id) { items.remove(id); itemImages.remove(id); return 1; }
        @Override public int countItemReferences(Long id) { return (int) relations.values().stream().filter(ids -> ids.contains(id)).count(); }
        @Override public WardrobeImage selectItemImage(Long userId, Long id) { return selectItem(userId, id) == null ? null : itemImages.get(id); }
        @Override public int upsertItemImage(Long id, WardrobeImage image) { itemImages.put(id, image); return 1; }
        @Override public List<WardrobeOutfit> selectOutfits(Long userId)
        {
            return outfits.values().stream().filter(o -> o.getUserId().equals(userId))
                    .sorted(Comparator.comparing(WardrobeOutfit::getSortOrder).thenComparing(WardrobeOutfit::getId, Comparator.reverseOrder()))
                    .map(o -> selectOutfit(userId, o.getId())).toList();
        }
        @Override public WardrobeOutfit selectOutfit(Long userId, Long id)
        {
            WardrobeOutfit outfit = outfits.get(id);
            if (outfit == null || !outfit.getUserId().equals(userId)) return null;
            outfit.setHasImage(outfitImages.containsKey(id));
            return outfit;
        }
        @Override public Long lockOutfit(Long userId, Long id) { return selectOutfit(userId, id) == null ? null : id; }
        @Override public List<WardrobeItem> selectOutfitItems(Long userId, Long id)
        {
            if (selectOutfit(userId, id) == null) return new ArrayList<>();
            return relations.getOrDefault(id, List.of()).stream().map(itemId -> selectItem(userId, itemId)).filter(Objects::nonNull).toList();
        }
        @Override public int insertOutfit(WardrobeOutfit outfit)
        {
            outfit.setId(++outfitSeq);
            outfit.setSortOrder(0L);
            outfit.setCreateTime(LocalDateTime.of(2026, 1, 1, 0, 0).plusSeconds(outfitSeq));
            outfits.put(outfit.getId(), outfit);
            return 1;
        }
        @Override public int updateOutfit(WardrobeOutfit outfit)
        {
            WardrobeOutfit old = outfits.get(outfit.getId());
            outfit.setSortOrder(old.getSortOrder());
            outfit.setCreateTime(old.getCreateTime());
            outfits.put(outfit.getId(), outfit);
            return 1;
        }
        @Override public int updateFavorite(Long userId, Long id, Boolean favorite) { selectOutfit(userId, id).setFavorite(favorite); return 1; }
        @Override public int deleteOutfit(Long userId, Long id) { outfits.remove(id); relations.remove(id); outfitImages.remove(id); return 1; }
        @Override public int deleteOutfitItems(Long id) { relations.remove(id); return 1; }
        @Override public int insertOutfitItem(Long outfitId, Long itemId, int sortOrder) { relations.computeIfAbsent(outfitId, k -> new ArrayList<>()).add(itemId); return 1; }
        @Override public WardrobeImage selectOutfitImage(Long userId, Long id) { return selectOutfit(userId, id) == null ? null : outfitImages.get(id); }
        @Override public int upsertOutfitImage(Long id, WardrobeImage image) { outfitImages.put(id, image); return 1; }
        @Override public int deleteOutfitImage(Long id) { outfitImages.remove(id); return 1; }
        @Override public List<WardrobeOutfit> lockUserOutfits(Long userId)
        {
            List<WardrobeOutfit> locked = new ArrayList<>(outfits.values().stream()
                    .filter(o -> o.getUserId().equals(userId)).sorted(Comparator.comparing(WardrobeOutfit::getId)).toList());
            lastLockedIds = locked.stream().map(WardrobeOutfit::getId).toList();
            return locked;
        }
        @Override public int updateOutfitSortOrder(Long userId, Long id, long sortOrder)
        {
            WardrobeOutfit outfit = selectOutfit(userId, id);
            if (outfit == null) return 0;
            outfit.setSortOrder(sortOrder);
            return 1;
        }
    }
}
