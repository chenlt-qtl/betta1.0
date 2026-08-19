package com.betta.eng.service.impl;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.betta.common.exception.ServiceException;
import com.betta.common.utils.SecurityUtils;
import com.betta.common.utils.StringUtils;
import com.betta.eng.domain.EngSentence;
import com.betta.eng.domain.EngWord;
import com.betta.eng.domain.PlayList;
import com.betta.eng.domain.dojo.BatchAddSentences;
import com.betta.eng.domain.vo.SentenceVo;
import com.betta.eng.mapper.EngSentenceMapper;
import com.betta.eng.service.IEngSentenceService;
import com.betta.eng.service.IPlayListService;
import com.betta.eng.utils.EngUtils;

/** 文章句子业务实现，负责用户隔离、时间规范化和批量写入。 */
@Service
public class EngSentenceServiceImpl implements IEngSentenceService
{
    private final EngSentenceMapper mapper;
    private final IPlayListService playListService;

    /** 创建句子服务；参数分别负责句子与播放列表访问。 */
    public EngSentenceServiceImpl(EngSentenceMapper mapper, IPlayListService playListService)
    {
        this.mapper = mapper;
        this.playListService = playListService;
    }

    @Override
    public EngSentence selectEngSentenceById(Long id)
    {
        return mapper.selectEngSentenceById(id, SecurityUtils.getUsername());
    }

    @Override
    public List<EngSentence> selectEngSentenceList(EngSentence sentence)
    {
        sentence.setCreateBy(SecurityUtils.getUsername());
        return mapper.selectEngSentenceList(sentence);
    }

    @Override
    public int insertEngSentence(EngSentence sentence)
    {
        validate(sentence);
        EngUtils.genMp3Time(sentence);
        sentence.setCreateBy(SecurityUtils.getUsername());
        if (sentence.getIdx() == null)
        {
            sentence.setIdx(mapper.countByArticleId(sentence.getArticleId()) + 1);
        }
        return mapper.insertEngSentence(sentence);
    }

    @Override
    public int updateEngSentence(EngSentence sentence)
    {
        validate(sentence);
        EngUtils.genMp3Time(sentence);
        sentence.setUpdateBy(SecurityUtils.getUsername());
        sentence.setCreateBy(SecurityUtils.getUsername());
        return mapper.updateEngSentence(sentence);
    }

    @Override
    public int deleteEngSentenceByIds(Long[] ids)
    {
        return ids == null || ids.length == 0 ? 0 : mapper.deleteEngSentenceByIds(ids, SecurityUtils.getUsername());
    }

    @Override
    public int deleteByArticle(Long articleId)
    {
        return mapper.deleteByArticleId(articleId, SecurityUtils.getUsername());
    }

    @Override
    public List<SentenceVo> selectPlayList(EngSentence sentence, boolean inPlayList, String username)
    {
        // 播放列表查询始终以当前登录用户为数据所有者，忽略客户端传入的用户名。
        String currentUsername = SecurityUtils.getUsername();
        return mapper.selectPlayList(sentence, currentUsername, parseIds(findSentenceIds(currentUsername)), inPlayList);
    }

    @Override
    public List<SentenceVo> selectByWordTop10(EngWord word)
    {
        return mapper.selectByWordTop10(word.getWordName(), word.getPrototype(), SecurityUtils.getUsername());
    }

    @Override
    @Transactional
    public boolean insertEngSentenceBatch(BatchAddSentences request)
    {
        if (request == null || request.getArticleId() == null || StringUtils.isEmpty(request.getSentenceStr()))
        {
            throw new ServiceException("文章和句子内容不能为空");
        }
        long sequence = mapper.countByArticleId(request.getArticleId());
        int count = 0;
        for (String line : request.getSentenceStr().split("\\r?\\n"))
        {
            if (StringUtils.isEmpty(line.trim()))
            {
                continue;
            }
            EngSentence sentence = new EngSentence();
            sentence.setArticleId(request.getArticleId());
            sentence.setContent(line.trim());
            sentence.setIdx(++sequence);
            insertEngSentence(sentence);
            count++;
        }
        return count > 0;
    }

    /** 校验 sentence 的文章与内容必填项。 */
    private void validate(EngSentence sentence)
    {
        if (sentence == null || sentence.getArticleId() == null || StringUtils.isEmpty(sentence.getContent()))
        {
            throw new ServiceException("文章和句子内容不能为空");
        }
    }

    /** 查询 username 对应播放项文本。 */
    private String findSentenceIds(String username)
    {
        PlayList condition = new PlayList();
        condition.setUserName(username);
        List<PlayList> lists = playListService.selectPlayListList(condition);
        return lists.isEmpty() ? null : lists.get(0).getSentenceIds();
    }

    /** 将 value 解析为长整型集合并忽略历史无效值。 */
    private List<Long> parseIds(String value)
    {
        List<Long> ids = new ArrayList<>();
        if (StringUtils.isEmpty(value))
        {
            return ids;
        }
        for (String item : value.split(","))
        {
            try
            {
                ids.add(Long.valueOf(item.trim()));
            }
            catch (NumberFormatException ignored)
            {
                // 忽略无效播放项，避免历史数据阻断查询。
            }
        }
        return ids;
    }
}
