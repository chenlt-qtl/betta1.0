package com.betta.eng.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import com.betta.common.utils.SecurityUtils;
import com.betta.eng.domain.PlayList;
import com.betta.eng.mapper.PlayListMapper;
import com.betta.eng.service.IPlayListService;

/** 播放列表业务实现，强制所有读写使用当前用户身份。 */
@Service
public class PlayListServiceImpl implements IPlayListService
{
    private final PlayListMapper mapper;

    /** 创建播放列表服务；mapper 负责数据库操作。 */
    public PlayListServiceImpl(PlayListMapper mapper)
    {
        this.mapper = mapper;
    }

    @Override
    public PlayList selectPlayListById(Long id)
    {
        return mapper.selectPlayListById(id, SecurityUtils.getUsername());
    }

    @Override
    public List<PlayList> selectPlayListList(PlayList playList)
    {
        playList.setUserName(SecurityUtils.getUsername());
        return mapper.selectPlayListList(playList);
    }

    @Override
    public PlayList selectCurrentUserPlayList()
    {
        List<PlayList> lists = selectPlayListList(new PlayList());
        return lists.isEmpty() ? null : lists.get(0);
    }

    @Override
    public int insertPlayList(PlayList playList)
    {
        String username = SecurityUtils.getUsername();
        playList.setUserName(username);
        playList.setCreateBy(username);
        return mapper.insertPlayList(playList);
    }

    @Override
    public int updatePlayList(PlayList playList)
    {
        String username = SecurityUtils.getUsername();
        playList.setUserName(username);
        playList.setUpdateBy(username);
        return mapper.updatePlayList(playList);
    }

    @Override
    public int deletePlayListByIds(Long[] ids)
    {
        return mapper.deletePlayListByIds(ids, SecurityUtils.getUsername());
    }
}
