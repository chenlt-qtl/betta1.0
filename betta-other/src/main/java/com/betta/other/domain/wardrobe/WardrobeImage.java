package com.betta.other.domain.wardrobe;

import lombok.Data;

/** 私有图片的媒体类型和二进制内容。 */
@Data
public class WardrobeImage
{
    private String contentType;
    private byte[] content;
}
