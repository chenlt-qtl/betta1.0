package com.betta.eng.mapper;

import com.betta.eng.domain.PlayList;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 播放列表数据访问接口。 */
public interface PlayListMapper {
    /** 按主键和用户查询；id 为主键、username 为用户名，返回播放列表。 */
    PlayList selectPlayListById(@Param("id") Long id, @Param("username") String username);
    /** 查询播放列表；playList 为筛选条件，返回列表集合。 */
    List<PlayList> selectPlayListList(PlayList playList);
    /** 新增播放列表；playList 为待写入实体，返回影响行数。 */
    int insertPlayList(PlayList playList);
    /** 修改当前用户播放列表；playList 为待更新实体，返回影响行数。 */
    int updatePlayList(PlayList playList);
    /** 批量删除当前用户播放列表；ids 为主键数组、username 为用户名，返回影响行数。 */
    int deletePlayListByIds(@Param("ids") Long[] ids, @Param("username") String username);
}
