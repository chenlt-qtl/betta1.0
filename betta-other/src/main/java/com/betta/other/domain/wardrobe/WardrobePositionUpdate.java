package com.betta.other.domain.wardrobe;

import lombok.Data;

/** 每次将当前用户的一套搭配上移或下移一位。 */
@Data
public class WardrobePositionUpdate
{
    private String direction;
}
