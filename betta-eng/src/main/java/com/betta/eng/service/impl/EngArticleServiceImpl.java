package com.betta.eng.service.impl;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.betta.common.utils.SecurityUtils;
import com.betta.common.utils.StringUtils;
import com.betta.eng.domain.EngArticle;
import com.betta.eng.domain.EngSentence;
import com.betta.eng.domain.PlayList;
import com.betta.eng.domain.vo.EngWordVo;
import com.betta.eng.mapper.EngArticleMapper;
import com.betta.eng.service.IEngArticleService;
import com.betta.eng.service.IEngArticleWordRelService;
import com.betta.eng.service.IEngSentenceService;
import com.betta.eng.service.IEngWordService;
import com.betta.eng.service.IPlayListService;

/** 文章业务实现，负责用户隔离、级联维护和文章数据聚合。 */
@Service
public class EngArticleServiceImpl implements IEngArticleService
{
    private static final int CURRENT_ARTICLE_DATA_TYPE = 2;
    private final EngArticleMapper mapper;
    private final IEngSentenceService sentenceService;
    private final IEngArticleWordRelService relService;
    private final IPlayListService playListService;
    private final IEngWordService wordService;

    /** 创建文章服务；参数依次负责文章、句子、关系、播放列表和单词访问。 */
    public EngArticleServiceImpl(EngArticleMapper mapper, IEngSentenceService sentenceService,
            IEngArticleWordRelService relService, IPlayListService playListService, IEngWordService wordService)
    {
        this.mapper = mapper;
        this.sentenceService = sentenceService;
        this.relService = relService;
        this.playListService = playListService;
        this.wordService = wordService;
    }

    @Override
    public EngArticle selectEngArticleById(Long id)
    {
        return mapper.selectEngArticleById(id, SecurityUtils.getUsername());
    }

    @Override
    public List<EngArticle> selectEngArticleList(EngArticle article)
    {
        article.setCreateBy(SecurityUtils.getUsername());
        return mapper.selectEngArticleList(article);
    }

    @Override
    public EngArticle insertEngArticle(EngArticle article)
    {
        article.setCreateBy(SecurityUtils.getUsername());
        mapper.insertEngArticle(article);
        return article;
    }

    @Override
    public int updateEngArticle(EngArticle article)
    {
        article.setUpdateBy(SecurityUtils.getUsername());
        article.setCreateBy(SecurityUtils.getUsername());
        return mapper.updateEngArticle(article);
    }

    @Override
    @Transactional
    public int deleteEngArticleById(Long id)
    {
        sentenceService.deleteByArticle(id);
        relService.deleteByArticle(id);
        return mapper.deleteEngArticleById(id, SecurityUtils.getUsername());
    }

    @Override
    public List<EngArticle> selectPlayList(EngArticle article, boolean inPlayList, String username)
    {
        // 播放列表查询始终以当前登录用户为数据所有者，忽略客户端传入的用户名。
        String currentUsername = SecurityUtils.getUsername();
        return mapper.selectPlayList(article, currentUsername, parseIds(findSentenceIds(currentUsername)), inPlayList);
    }

    @Override
    public EngArticle getCurrent()
    {
        return mapper.getCurrentArticle(SecurityUtils.getUsername(), CURRENT_ARTICLE_DATA_TYPE);
    }

    @Override
    public List<String> exportArticle(Long articleId)
    {
        List<String> result = new ArrayList<>();
        EngSentence condition = new EngSentence();
        condition.setArticleId(articleId);
        for (EngSentence sentence : sentenceService.selectEngSentenceList(condition))
        {
            result.add(sentence.getContent());
        }
        for (EngWordVo word : wordService.selectWordListByArticle(articleId))
        {
            result.add(word.getWordName());
        }
        return result;
    }

    /** 查询 username 对应播放项字符串并返回。 */
    private String findSentenceIds(String username)
    {
        PlayList condition = new PlayList();
        condition.setUserName(username);
        List<PlayList> lists = playListService.selectPlayListList(condition);
        return lists.isEmpty() ? null : lists.get(0).getSentenceIds();
    }

    /** 将 value 解析为主键集合，并忽略历史无效值。 */
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
                // 历史脏值不应阻断整个播放列表查询。
            }
        }
        return ids;
    }
}
