package com.betta.other.domain.wardrobe;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/** 衣橱单品及列表筛选条件。 */
@Data
public class WardrobeItem
{
    private Long id;
    private String name;
    private String category;
    private String color;
    private List<String> seasons;
    private List<String> occasions;
    private String note;

    @JsonIgnore
    private Long userId;
    @JsonIgnore
    private String seasonsJson;
    @JsonIgnore
    private String occasionsJson;
    @JsonIgnore
    private String keyword;
    @JsonIgnore
    private String season;
    @JsonIgnore
    private String occasion;
}
