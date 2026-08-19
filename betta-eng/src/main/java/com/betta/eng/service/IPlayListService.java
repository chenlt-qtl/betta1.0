package com.betta.eng.service;
import com.betta.eng.domain.PlayList;
import java.util.List;
/** 播放列表业务接口，所有写操作按当前用户隔离。 */
public interface IPlayListService {
    /** 根据 id 查询当前用户播放列表。 */
    PlayList selectPlayListById(Long id);
    /** 根据 playList 条件查询列表。 */
    List<PlayList> selectPlayListList(PlayList playList);
    /** 查询当前用户的首条播放列表；不存在时返回 null。 */
    PlayList selectCurrentUserPlayList();
    /** 新增 playList 并返回影响行数。 */
    int insertPlayList(PlayList playList);
    /** 更新 playList 并返回影响行数。 */
    int updatePlayList(PlayList playList);
    /** 删除当前用户 ids 对应列表并返回影响行数。 */
    int deletePlayListByIds(Long[] ids);
}
