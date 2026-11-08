package com.betta.other.domain.wardrobe;

import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/** 已保存的搭配与展示所需的有序单品。 */
@Data
public class WardrobeOutfit
{
    private Long id;
    private String name;
    private List<String> occasions;
    private Boolean favorite;
    private List<Long> itemIds;
    private List<WardrobeItem> items;
    private Boolean hasImage;
    private String imageAction;

    @JsonIgnore
    private Long userId;
    @JsonIgnore
    private String occasionsJson;
    /** 排序仅允许通过专用接口调整。 */
    @JsonIgnore
    private Long sortOrder;
    @JsonIgnore
    private LocalDateTime createTime;
}
