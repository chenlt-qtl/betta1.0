package com.betta.other.service.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.fastjson2.JSON;
import com.betta.common.exception.ServiceException;
import com.betta.common.utils.SecurityUtils;
import com.betta.other.domain.wardrobe.WardrobeImage;
import com.betta.other.domain.wardrobe.WardrobeItem;
import com.betta.other.domain.wardrobe.WardrobeOutfit;
import com.betta.other.mapper.WardrobeMapper;
import com.betta.other.service.IWardrobeService;

/** 衣橱数据及图片始终由登录用户限定归属。 */
@Service
public class WardrobeServiceImpl implements IWardrobeService
{
    private static final long IMAGE_LIMIT = 5L * 1024 * 1024;
    private static final int IMAGE_MAX_EDGE = 16384;
    private static final long IMAGE_MAX_PIXELS = 40_000_000L;
    private static final Set<String> CATEGORIES = Set.of("上衣", "下装", "外套", "连衣裙", "家居服", "鞋", "包", "配饰");
    private static final Set<String> SEASONS = Set.of("春", "夏", "秋", "冬");
    private final WardrobeMapper mapper;

    /** 创建衣橱业务服务。 */
    public WardrobeServiceImpl(WardrobeMapper mapper)
    {
        this.mapper = mapper;
    }

    @Override
    public List<WardrobeItem> listItems(WardrobeItem query)
    {
        WardrobeItem filter = query == null ? new WardrobeItem() : query;
        filter.setUserId(SecurityUtils.getUserId());
        List<WardrobeItem> result = mapper.selectItems(filter);
        result.forEach(this::hydrateItem);
        return result;
    }

    @Override
    public WardrobeItem getItem(Long id)
    {
        WardrobeItem item = mapper.selectItem(SecurityUtils.getUserId(), id);
        if (item == null)
        {
            throw new ServiceException("单品不存在");
        }
        return hydrateItem(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WardrobeItem createItem(WardrobeItem item, MultipartFile file)
    {
        validateItem(item);
        WardrobeImage image = validateImage(file, true);
        item.setId(null);
        item.setUserId(SecurityUtils.getUserId());
        mapper.insertItem(item);
        mapper.upsertItemImage(item.getId(), image);
        return hydrateItem(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WardrobeItem updateItem(Long id, WardrobeItem item, MultipartFile file)
    {
        validateItem(item);
        WardrobeImage image = validateImage(file, false);
        Long userId = SecurityUtils.getUserId();
        requireItemLock(userId, id);
        item.setId(id);
        item.setUserId(userId);
        mapper.updateItem(item);
        if (image != null)
        {
            mapper.upsertItemImage(id, image);
        }
        return hydrateItem(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteItem(Long id)
    {
        Long userId = SecurityUtils.getUserId();
        requireItemLock(userId, id);
        if (mapper.countItemReferences(id) > 0)
        {
            throw new ServiceException("该单品已用于搭配，请先从搭配中移除");
        }
        mapper.deleteItem(userId, id);
    }

    @Override
    public WardrobeImage getItemImage(Long id)
    {
        return mapper.selectItemImage(SecurityUtils.getUserId(), id);
    }

    @Override
    public List<WardrobeOutfit> listOutfits()
    {
        Long userId = SecurityUtils.getUserId();
        List<WardrobeOutfit> outfits = mapper.selectOutfits(userId);
        outfits.forEach(outfit -> hydrateOutfit(userId, outfit));
        return outfits;
    }

    @Override
    public WardrobeOutfit getOutfit(Long id)
    {
        Long userId = SecurityUtils.getUserId();
        WardrobeOutfit outfit = mapper.selectOutfit(userId, id);
        if (outfit == null)
        {
            throw new ServiceException("搭配不存在");
        }
        return hydrateOutfit(userId, outfit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WardrobeOutfit createOutfit(WardrobeOutfit outfit, MultipartFile file)
    {
        Long userId = SecurityUtils.getUserId();
        validateOutfit(outfit);
        String action = imageAction(outfit, file, true);
        WardrobeImage image = "REPLACE".equals(action) ? validateImage(file, true) : null;
        lockItems(userId, outfit.getItemIds());
        outfit.setId(null);
        outfit.setUserId(userId);
        outfit.setFavorite(Boolean.TRUE.equals(outfit.getFavorite()));
        mapper.insertOutfit(outfit);
        saveOutfitItems(outfit);
        if (image != null)
        {
            mapper.upsertOutfitImage(outfit.getId(), image);
        }
        return hydrateOutfit(userId, mapper.selectOutfit(userId, outfit.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WardrobeOutfit updateOutfit(Long id, WardrobeOutfit outfit, MultipartFile file)
    {
        Long userId = SecurityUtils.getUserId();
        validateOutfit(outfit);
        String action = imageAction(outfit, file, false);
        WardrobeImage image = "REPLACE".equals(action) ? validateImage(file, true) : null;
        requireOutfitLock(userId, id);
        WardrobeOutfit existing = mapper.selectOutfit(userId, id);
        lockItems(userId, outfit.getItemIds());
        outfit.setId(id);
        outfit.setUserId(userId);
        if (outfit.getFavorite() == null)
        {
            outfit.setFavorite(Boolean.TRUE.equals(existing.getFavorite()));
        }
        mapper.updateOutfit(outfit);
        mapper.deleteOutfitItems(id);
        saveOutfitItems(outfit);
        if (image != null)
        {
            mapper.upsertOutfitImage(id, image);
        }
        else if ("REMOVE".equals(action))
        {
            mapper.deleteOutfitImage(id);
        }
        return hydrateOutfit(userId, mapper.selectOutfit(userId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOutfit(Long id)
    {
        Long userId = SecurityUtils.getUserId();
        requireOutfitLock(userId, id);
        mapper.deleteOutfit(userId, id);
    }

    @Override
    public void updateFavorite(Long id, Boolean favorite)
    {
        if (id == null || favorite == null)
        {
            throw new ServiceException("收藏参数无效");
        }
        Long userId = SecurityUtils.getUserId();
        if (mapper.selectOutfit(userId, id) == null)
        {
            throw new ServiceException("搭配不存在");
        }
        mapper.updateFavorite(userId, id, favorite);
    }

    @Override
    public WardrobeImage getOutfitImage(Long id)
    {
        return mapper.selectOutfitImage(SecurityUtils.getUserId(), id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean moveOutfit(Long id, String direction)
    {
        if (!"up".equals(direction) && !"down".equals(direction))
        {
            throw new ServiceException("排序方向只能为 up 或 down");
        }
        Long userId = SecurityUtils.getUserId();
        List<WardrobeOutfit> outfits = mapper.lockUserOutfits(userId);
        // 统一按 ID 加锁后，再按展示顺序计算相邻项，跨页操作也使用全量顺序。
        outfits.sort(Comparator.comparing((WardrobeOutfit outfit) -> outfit.getSortOrder() == null ? 0L : outfit.getSortOrder())
                .thenComparing(WardrobeOutfit::getId, Comparator.reverseOrder()));
        int index = -1;
        for (int i = 0; i < outfits.size(); i++)
        {
            if (Objects.equals(outfits.get(i).getId(), id))
            {
                index = i;
                break;
            }
        }
        if (index < 0)
        {
            throw new ServiceException("搭配不存在");
        }
        int target = "up".equals(direction) ? index - 1 : index + 1;
        if (target < 0 || target >= outfits.size())
        {
            return false;
        }
        Collections.swap(outfits, index, target);
        for (int i = 0; i < outfits.size(); i++)
        {
            WardrobeOutfit outfit = outfits.get(i);
            long order = (long) i + 1;
            if (!Objects.equals(outfit.getSortOrder(), order)
                    && mapper.updateOutfitSortOrder(userId, outfit.getId(), order) != 1)
            {
                throw new ServiceException("调整搭配顺序失败，请重试");
            }
        }
        return true;
    }

    @Override
    public List<WardrobeOutfit> recommend(Double temperature, String occasion)
    {
        if (temperature != null && !Double.isFinite(temperature))
        {
            throw new ServiceException("温度无效");
        }
        Set<String> suitable = temperature == null ? null : temperature < 12 ? Set.of("冬")
                : temperature < 24 ? Set.of("春", "秋") : Set.of("夏");
        String requestedOccasion = occasion == null ? "" : occasion.trim();
        return listOutfits().stream()
                .filter(outfit -> requestedOccasion.isEmpty() || outfit.getOccasions().isEmpty()
                        || outfit.getOccasions().contains(requestedOccasion))
                .filter(outfit -> suitable == null || outfit.getItems().stream().allMatch(item ->
                        item.getSeasons().isEmpty() || item.getSeasons().stream().anyMatch(suitable::contains)))
                .sorted(Comparator.comparing((WardrobeOutfit outfit) -> Boolean.TRUE.equals(outfit.getFavorite())).reversed()
                        .thenComparing(WardrobeOutfit::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(WardrobeOutfit::getId, Comparator.reverseOrder()))
                .limit(6).toList();
    }

    private WardrobeItem hydrateItem(WardrobeItem item)
    {
        item.setSeasons(parseTags(item.getSeasonsJson()));
        item.setOccasions(parseTags(item.getOccasionsJson()));
        return item;
    }

    private WardrobeOutfit hydrateOutfit(Long userId, WardrobeOutfit outfit)
    {
        outfit.setOccasions(parseTags(outfit.getOccasionsJson()));
        List<WardrobeItem> items = mapper.selectOutfitItems(userId, outfit.getId());
        items.forEach(this::hydrateItem);
        outfit.setItems(items);
        outfit.setItemIds(items.stream().map(WardrobeItem::getId).toList());
        outfit.setHasImage(Boolean.TRUE.equals(outfit.getHasImage()));
        return outfit;
    }

    private List<String> parseTags(String json)
    {
        return json == null || json.isBlank() ? new ArrayList<>() : JSON.parseArray(json, String.class);
    }

    private void validateItem(WardrobeItem item)
    {
        if (item == null || item.getCategory() == null || !CATEGORIES.contains(item.getCategory()))
        {
            throw new ServiceException("请选择有效的单品分类");
        }
        item.setName(text(item.getName(), 100, "名称"));
        item.setColor(text(item.getColor(), 40, "颜色"));
        item.setNote(text(item.getNote(), 500, "备注"));
        item.setSeasons(tags(item.getSeasons(), true));
        item.setOccasions(tags(item.getOccasions(), false));
        item.setSeasonsJson(JSON.toJSONString(item.getSeasons()));
        item.setOccasionsJson(JSON.toJSONString(item.getOccasions()));
    }

    private void validateOutfit(WardrobeOutfit outfit)
    {
        if (outfit == null)
        {
            throw new ServiceException("搭配不能为空");
        }
        outfit.setName(text(outfit.getName(), 100, "搭配名称"));
        if (outfit.getName().isEmpty())
        {
            throw new ServiceException("搭配名称不能为空");
        }
        List<Long> ids = outfit.getItemIds();
        if (ids == null || ids.isEmpty() || ids.stream().anyMatch(id -> id == null || id <= 0)
                || new LinkedHashSet<>(ids).size() != ids.size())
        {
            throw new ServiceException("请选择不重复的单品");
        }
        outfit.setOccasions(tags(outfit.getOccasions(), false));
        outfit.setOccasionsJson(JSON.toJSONString(outfit.getOccasions()));
    }

    private String text(String value, int max, String label)
    {
        String result = value == null ? "" : value.trim();
        if (result.length() > max)
        {
            throw new ServiceException(label + "过长");
        }
        return result;
    }

    private List<String> tags(List<String> values, boolean season)
    {
        Set<String> result = new LinkedHashSet<>();
        if (values != null)
        {
            for (String value : values)
            {
                String cleaned = text(value, 40, season ? "季节" : "场合");
                if (cleaned.isEmpty() || (season && !SEASONS.contains(cleaned)))
                {
                    throw new ServiceException(season ? "季节无效" : "场合不能为空");
                }
                result.add(cleaned);
            }
        }
        return new ArrayList<>(result);
    }

    private void requireItemLock(Long userId, Long id)
    {
        if (id == null || mapper.lockItem(userId, id) == null)
        {
            throw new ServiceException("单品不存在");
        }
    }

    private void requireOutfitLock(Long userId, Long id)
    {
        if (id == null || mapper.lockOutfit(userId, id) == null)
        {
            throw new ServiceException("搭配不存在");
        }
    }

    private void lockItems(Long userId, List<Long> ids)
    {
        // 固定锁顺序，同时阻止关联单品被并发删除。
        ids.stream().sorted().forEach(id -> requireItemLock(userId, id));
    }

    private void saveOutfitItems(WardrobeOutfit outfit)
    {
        for (int i = 0; i < outfit.getItemIds().size(); i++)
        {
            mapper.insertOutfitItem(outfit.getId(), outfit.getItemIds().get(i), i);
        }
    }

    private String imageAction(WardrobeOutfit outfit, MultipartFile file, boolean create)
    {
        String action = outfit.getImageAction();
        if (action == null || action.isBlank())
        {
            action = file == null ? "KEEP" : "REPLACE";
        }
        if (!Set.of("KEEP", "REPLACE", "REMOVE").contains(action) || (create && "REMOVE".equals(action))
                || (!"REPLACE".equals(action) && file != null))
        {
            throw new ServiceException("效果照操作无效");
        }
        return action;
    }

    private WardrobeImage validateImage(MultipartFile file, boolean required)
    {
        if (file == null)
        {
            if (required)
            {
                throw new ServiceException("请选择图片");
            }
            return null;
        }
        if (file.isEmpty() || file.getSize() > IMAGE_LIMIT)
        {
            throw new ServiceException("图片不能为空且不能超过5MB");
        }
        try
        {
            byte[] bytes = file.getBytes();
            if (bytes.length == 0 || bytes.length > IMAGE_LIMIT)
            {
                throw new ServiceException("图片不能为空且不能超过5MB");
            }
            try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes)))
            {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
                if (!readers.hasNext())
                {
                    throw new ServiceException("仅支持有效的 JPG 或 PNG 图片");
                }
                ImageReader reader = readers.next();
                try
                {
                    String format = reader.getFormatName();
                    if (!"JPEG".equalsIgnoreCase(format) && !"JPG".equalsIgnoreCase(format) && !"PNG".equalsIgnoreCase(format))
                    {
                        throw new ServiceException("仅支持有效的 JPG 或 PNG 图片");
                    }
                    reader.setInput(stream, true, true);
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    if (width <= 0 || height <= 0)
                    {
                        throw new ServiceException("图片内容无效");
                    }
                    // 在解码前检查原始尺寸，避免极宽图片分配过大的解码行缓冲。
                    if (width > IMAGE_MAX_EDGE || height > IMAGE_MAX_EDGE || (long) width * height > IMAGE_MAX_PIXELS)
                    {
                        throw new ServiceException("图片尺寸过大，最长边不能超过16384像素且总像素不能超过4000万");
                    }
                    // 缩小解码输出，减少校验时的内存占用。
                    int sample = (int) Math.max(1L, ((long) Math.max(width, height) + 1023) / 1024);
                    ImageReadParam param = reader.getDefaultReadParam();
                    param.setSourceSubsampling(sample, sample, 0, 0);
                    if (reader.read(0, param) == null)
                    {
                        throw new ServiceException("图片内容无效");
                    }
                    WardrobeImage result = new WardrobeImage();
                    result.setContentType("PNG".equalsIgnoreCase(format) ? "image/png" : "image/jpeg");
                    result.setContent(bytes);
                    return result;
                }
                finally
                {
                    reader.dispose();
                }
            }
        }
        catch (IOException | IllegalArgumentException e)
        {
            throw new ServiceException("图片内容无效");
        }
    }
}
