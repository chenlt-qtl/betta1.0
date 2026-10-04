package com.betta.eng.service.impl;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.betta.common.exception.ServiceException;
import com.betta.common.utils.SecurityUtils;
import com.betta.common.utils.StringUtils;
import com.betta.eng.domain.EngArticleWordRel;
import com.betta.eng.domain.EngArticle;
import com.betta.eng.domain.EngSentence;
import com.betta.eng.domain.EngSentenceWordRel;
import com.betta.eng.domain.EngWord;
import com.betta.eng.domain.PlayList;
import com.betta.eng.domain.dojo.BatchAddSentences;
import com.betta.eng.domain.dto.EngSentenceWordUpdateDto;
import com.betta.eng.domain.vo.EngSentenceSegmentVo;
import com.betta.eng.domain.vo.EngSentenceWordOptionsVo;
import com.betta.eng.domain.vo.EngWordFormMatchVo;
import com.betta.eng.domain.vo.SentenceVo;
import com.betta.eng.mapper.EngArticleMapper;
import com.betta.eng.mapper.EngArticleWordRelMapper;
import com.betta.eng.mapper.EngSentenceMapper;
import com.betta.eng.mapper.EngSentenceWordRelMapper;
import com.betta.eng.mapper.EngWordMapper;
import com.betta.eng.service.IEngSentenceService;
import com.betta.eng.service.IEngArticleWordRelService;
import com.betta.eng.service.IPlayListService;
import com.betta.eng.utils.EngUtils;

/** 文章句子业务实现，负责用户隔离、时间规范化和批量写入。 */
@Service
public class EngSentenceServiceImpl implements IEngSentenceService
{
    /** 英文词片段；边界与测试挖空规则一致，避免 apple 命中 pineapple。 */
    private static final Pattern WORD_PATTERN = Pattern.compile("\\p{IsLatin}+(?:['’]\\p{IsLatin}+)*");
    private final EngSentenceMapper mapper;
    private final IPlayListService playListService;
    private final EngSentenceWordRelMapper sentenceWordRelMapper;
    private final EngWordMapper wordMapper;
    private final EngArticleWordRelMapper articleWordRelMapper;
    private final EngArticleMapper articleMapper;
    private final IEngArticleWordRelService articleWordRelService;

    /** 创建句子服务；参数分别负责句子与播放列表访问。 */
    public EngSentenceServiceImpl(EngSentenceMapper mapper, IPlayListService playListService,
            EngSentenceWordRelMapper sentenceWordRelMapper, EngWordMapper wordMapper,
            EngArticleWordRelMapper articleWordRelMapper, EngArticleMapper articleMapper,
            IEngArticleWordRelService articleWordRelService)
    {
        this.mapper = mapper;
        this.playListService = playListService;
        this.sentenceWordRelMapper = sentenceWordRelMapper;
        this.wordMapper = wordMapper;
        this.articleWordRelMapper = articleWordRelMapper;
        this.articleMapper = articleMapper;
        this.articleWordRelService = articleWordRelService;
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
        requireOwnedArticle(sentence.getArticleId());
        EngUtils.genMp3Time(sentence);
        sentence.setCreateBy(SecurityUtils.getUsername());
        if (sentence.getIdx() == null)
        {
            sentence.setIdx(mapper.countByArticleId(sentence.getArticleId()) + 1);
        }
        return mapper.insertEngSentence(sentence);
    }

    @Override
    @Transactional
    public int updateEngSentence(EngSentence sentence)
    {
        validate(sentence);
        EngSentence existing = requireSentence(sentence.getId());
        requireOwnedArticle(sentence.getArticleId());
        List<EngSentenceWordRel> relations = sentenceWordRelMapper.selectBySentenceId(
                sentence.getId(), SecurityUtils.getUsername());
        EngUtils.genMp3Time(sentence);
        sentence.setUpdateBy(SecurityUtils.getUsername());
        sentence.setCreateBy(SecurityUtils.getUsername());
        int rows = mapper.updateEngSentence(sentence);
        if (rows == 0)
        {
            throw new ServiceException("句子不存在或无权操作");
        }
        reconcileRelationsAfterUpdate(existing.getArticleId(), sentence, relations);
        return rows;
    }

    @Override
    @Transactional
    public int deleteEngSentenceByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0)
        {
            return 0;
        }
        String username = SecurityUtils.getUsername();
        Map<Long, List<EngSentenceWordRel>> relationsBySentence = new LinkedHashMap<>();
        for (Long id : ids)
        {
            EngSentence sentence = mapper.selectEngSentenceById(id, username);
            if (sentence != null)
            {
                relationsBySentence.put(id, sentenceWordRelMapper.selectBySentenceId(id, username));
            }
        }
        sentenceWordRelMapper.deleteBySentenceIds(ids, username);
        int rows = mapper.deleteEngSentenceByIds(ids, username);
        for (List<EngSentenceWordRel> relations : relationsBySentence.values())
        {
            for (EngSentenceWordRel relation : relations)
            {
                removeArticleRelationIfUnused(relation.getArticleId(), relation.getWordId(), username);
            }
        }
        return rows;
    }

    @Override
    @Transactional
    public int deleteByArticle(Long articleId)
    {
        String username = SecurityUtils.getUsername();
        EngSentence condition = new EngSentence();
        condition.setArticleId(articleId);
        condition.setCreateBy(username);
        Set<Long> wordIds = new LinkedHashSet<>();
        for (EngSentence sentence : mapper.selectEngSentenceList(condition))
        {
            for (EngSentenceWordRel relation : sentenceWordRelMapper.selectBySentenceId(sentence.getId(), username))
            {
                wordIds.add(relation.getWordId());
            }
        }
        sentenceWordRelMapper.deleteByArticle(articleId, username);
        int rows = mapper.deleteByArticleId(articleId, username);
        for (Long wordId : wordIds)
        {
            removeArticleRelationIfUnused(articleId, wordId, username);
        }
        return rows;
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
        if (word == null || word.getId() == null)
        {
            return List.of();
        }
        return mapper.selectByWordTop10(word.getId(), SecurityUtils.getUsername());
    }

    @Override
    public EngSentenceWordOptionsVo selectWordOptions(Long sentenceId)
    {
        EngSentence sentence = requireSentence(sentenceId);
        List<TokenMatch> matches = resolveTokenMatches(sentence.getContent());
        return buildWordOptions(sentence,
                sentenceWordRelMapper.selectBySentenceId(sentenceId, SecurityUtils.getUsername()), matches);
    }

    @Override
    @Transactional
    public EngSentenceWordOptionsVo updateWordOptions(Long sentenceId, EngSentenceWordUpdateDto request)
    {
        EngSentence sentence = requireSentence(sentenceId);
        requireOwnedArticle(sentence.getArticleId());
        Set<Long> requestedWordIds = validateRequestedWordIds(request);
        List<TokenMatch> matches = resolveTokenMatches(sentence.getContent());
        Map<Long, TokenMatch> matchByWordId = new LinkedHashMap<>();
        for (TokenMatch match : matches)
        {
            if (match.word() != null)
            {
                matchByWordId.putIfAbsent(match.word().getId(), match);
            }
        }
        if (!matchByWordId.keySet().containsAll(requestedWordIds))
        {
            throw new ServiceException("所选单词未在句子中出现或词形存在歧义");
        }

        String username = SecurityUtils.getUsername();
        List<EngSentenceWordRel> existingRelations = sentenceWordRelMapper.selectBySentenceId(sentenceId, username);
        List<Long> removedWordIds = existingRelations.stream().map(EngSentenceWordRel::getWordId)
                .filter(wordId -> !requestedWordIds.contains(wordId)).distinct().toList();
        if (!removedWordIds.isEmpty())
        {
            sentenceWordRelMapper.deleteBySentenceAndWordIds(sentenceId, removedWordIds, username);
        }

        List<Long> orderedWordIds = new ArrayList<>();
        List<EngSentenceWordRel> selectedRelations = new ArrayList<>();
        for (TokenMatch match : matches)
        {
            if (match.word() != null && requestedWordIds.contains(match.word().getId())
                    && !orderedWordIds.contains(match.word().getId()))
            {
                orderedWordIds.add(match.word().getId());
                selectedRelations.add(relation(sentenceId, match.word().getId(), match.text(), username));
            }
        }
        if (!selectedRelations.isEmpty())
        {
            sentenceWordRelMapper.upsertBatch(selectedRelations);
            articleWordRelService.insertMissingByWordIds(sentence.getArticleId(), orderedWordIds);
        }
        if (!removedWordIds.isEmpty())
        {
            articleWordRelMapper.deleteUnusedByWordIds(sentence.getArticleId(), removedWordIds, username);
        }
        return buildWordOptions(sentence, selectedRelations, matches);
    }

    @Override
    public EngSentenceWordRel selectFirstWordRelation(Long articleId, Long wordId)
    {
        if (articleId == null || wordId == null)
        {
            return null;
        }
        return sentenceWordRelMapper.selectFirstByArticleAndWord(articleId, wordId, SecurityUtils.getUsername());
    }

    @Override
    public EngSentenceWordRel selectFirstWordRelation(Long wordId)
    {
        return wordId == null ? null
                : sentenceWordRelMapper.selectFirstByWord(wordId, SecurityUtils.getUsername());
    }

    @Override
    @Transactional
    public boolean insertEngSentenceBatch(BatchAddSentences request)
    {
        if (request == null || request.getArticleId() == null || StringUtils.isEmpty(request.getSentenceStr()))
        {
            throw new ServiceException("文章和句子内容不能为空");
        }
        requireOwnedArticle(request.getArticleId());
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

    /** 查询并校验句子归属当前用户。 */
    private EngSentence requireSentence(Long sentenceId)
    {
        if (sentenceId == null)
        {
            throw new ServiceException("句子主键不能为空");
        }
        EngSentence sentence = mapper.selectEngSentenceById(sentenceId, SecurityUtils.getUsername());
        if (sentence == null)
        {
            throw new ServiceException("句子不存在或无权操作");
        }
        return sentence;
    }

    /** 查询并校验文章归属当前用户，避免通过句子接口向他人文章写入数据。 */
    private EngArticle requireOwnedArticle(Long articleId)
    {
        if (articleId == null)
        {
            throw new ServiceException("文章主键不能为空");
        }
        EngArticle article = articleMapper.selectEngArticleById(articleId, SecurityUtils.getUsername());
        if (article == null)
        {
            throw new ServiceException("文章不存在或无权操作");
        }
        return article;
    }

    /** 将句子拆为保留原始空白和标点的有序片段。 */
    private EngSentenceWordOptionsVo buildWordOptions(EngSentence sentence, List<EngSentenceWordRel> relations,
            List<TokenMatch> matches)
    {
        Map<Long, EngSentenceWordRel> selectedByWordId = new LinkedHashMap<>();
        for (EngSentenceWordRel relation : relations)
        {
            selectedByWordId.put(relation.getWordId(), relation);
        }
        List<EngSentenceSegmentVo> segments = new ArrayList<>();
        int offset = 0;
        for (TokenMatch match : matches)
        {
            if (match.start() > offset)
            {
                segments.add(textSegment(sentence.getContent().substring(offset, match.start())));
            }
            EngSentenceSegmentVo segment = new EngSentenceSegmentVo();
            segment.setText(match.text());
            segment.setType("WORD");
            segment.setSelectable(match.word() != null);
            if (match.word() != null)
            {
                segment.setWordId(match.word().getId());
                segment.setWordName(match.word().getWordName());
                segment.setSelected(selectedByWordId.containsKey(match.word().getId()));
            }
            segments.add(segment);
            offset = match.end();
        }
        if (offset < sentence.getContent().length())
        {
            segments.add(textSegment(sentence.getContent().substring(offset)));
        }
        EngSentenceWordOptionsVo options = new EngSentenceWordOptionsVo();
        options.setSentenceId(sentence.getId());
        options.setArticleId(sentence.getArticleId());
        options.setSegments(segments);
        return options;
    }

    /** 解析每个英文词片段；存在多个规范词候选时保留原文但禁止选择。 */
    private List<TokenMatch> resolveTokenMatches(String content)
    {
        List<TokenMatch> matches = new ArrayList<>();
        if (StringUtils.isEmpty(content))
        {
            return matches;
        }
        Matcher matcher = WORD_PATTERN.matcher(content);
        List<RawToken> tokens = new ArrayList<>();
        Set<String> forms = new LinkedHashSet<>();
        while (matcher.find())
        {
            String text = matcher.group();
            String key = normalize(text);
            tokens.add(new RawToken(matcher.start(), matcher.end(), text, key));
            forms.add(key);
        }
        Map<String, Map<Long, EngWord>> candidatesByForm = new LinkedHashMap<>();
        if (!forms.isEmpty())
        {
            List<EngWordFormMatchVo> rows = wordMapper.selectWordFormMatches(new ArrayList<>(forms));
            for (EngWordFormMatchVo row : rows == null ? List.<EngWordFormMatchVo>of() : rows)
            {
                if (row == null || row.getLookupKey() == null || row.getWordId() == null)
                {
                    continue;
                }
                EngWord word = new EngWord();
                word.setId(row.getWordId());
                word.setWordName(row.getWordName());
                candidatesByForm.computeIfAbsent(row.getLookupKey(), ignored -> new LinkedHashMap<>())
                        .putIfAbsent(row.getWordId(), word);
            }
        }
        for (RawToken token : tokens)
        {
            Map<Long, EngWord> candidates = candidatesByForm.get(token.key());
            EngWord word = candidates != null && candidates.size() == 1
                    ? candidates.values().iterator().next() : null;
            matches.add(new TokenMatch(token.start(), token.end(), token.text(), word));
        }
        return matches;
    }

    /** 句子修改后只保留仍能在新内容中确认的旧关系，并处理文章变更。 */
    private void reconcileRelationsAfterUpdate(Long oldArticleId, EngSentence sentence,
            List<EngSentenceWordRel> relations)
    {
        String username = SecurityUtils.getUsername();
        Map<Long, TokenMatch> matches = new LinkedHashMap<>();
        for (TokenMatch match : resolveTokenMatches(sentence.getContent()))
        {
            if (match.word() != null)
            {
                matches.putIfAbsent(match.word().getId(), match);
            }
        }
        for (EngSentenceWordRel relation : relations)
        {
            TokenMatch match = matches.get(relation.getWordId());
            if (match == null)
            {
                sentenceWordRelMapper.deleteBySentenceAndWord(sentence.getId(), relation.getWordId(), username);
            }
            else
            {
                relation.setMatchedText(match.text());
                relation.setUpdateBy(username);
                sentenceWordRelMapper.updateMatchedText(relation);
                ensureArticleRelation(sentence.getArticleId(), match.word());
            }
            if (match == null || !oldArticleId.equals(sentence.getArticleId()))
            {
                removeArticleRelationIfUnused(oldArticleId, relation.getWordId(), username);
            }
        }
    }

    /** 当前文章不存在该规范词时按现有自增主键顺序追加。 */
    private void ensureArticleRelation(Long articleId, EngWord word)
    {
        articleWordRelService.insertMissingByWordIds(articleId, List.of(word.getId()));
    }

    /** 最后一个句子引用移除后同步删除文章词关系。 */
    private void removeArticleRelationIfUnused(Long articleId, Long wordId, String username)
    {
        if (articleId == null || wordId == null
                || sentenceWordRelMapper.countByArticleAndWord(articleId, wordId, username) > 0)
        {
            return;
        }
        EngWord word = wordMapper.selectEngWordById(wordId);
        if (word == null)
        {
            return;
        }
        EngArticleWordRel condition = new EngArticleWordRel();
        condition.setArticleId(articleId);
        condition.setWordName(word.getWordName());
        condition.setCreateBy(username);
        List<Long> ids = articleWordRelMapper.selectEngArticleWordRelList(condition).stream()
                .map(EngArticleWordRel::getId).filter(id -> id != null).toList();
        if (!ids.isEmpty())
        {
            articleWordRelMapper.deleteEngArticleWordRelByIds(ids.toArray(Long[]::new), username);
        }
    }

    /** 请求集合去重并拒绝空主键。 */
    private Set<Long> validateRequestedWordIds(EngSentenceWordUpdateDto request)
    {
        Set<Long> ids = new LinkedHashSet<>();
        if (request == null || request.getWordIds() == null)
        {
            throw new ServiceException("单词列表不能为 null");
        }
        for (Long id : request.getWordIds())
        {
            if (id == null)
            {
                throw new ServiceException("单词主键不能为空");
            }
            ids.add(id);
        }
        return ids;
    }

    /** 构造新增句子关系。 */
    private EngSentenceWordRel relation(Long sentenceId, Long wordId, String matchedText, String username)
    {
        EngSentenceWordRel relation = new EngSentenceWordRel();
        relation.setSentenceId(sentenceId);
        relation.setWordId(wordId);
        relation.setMatchedText(matchedText);
        relation.setCreateBy(username);
        return relation;
    }

    /** 构造不可选择的普通文本片段。 */
    private EngSentenceSegmentVo textSegment(String text)
    {
        EngSentenceSegmentVo segment = new EngSentenceSegmentVo();
        segment.setText(text);
        segment.setType("TEXT");
        return segment;
    }

    /** 词形检索键统一使用 NFKC、小写和标准撇号。 */
    private String normalize(String value)
    {
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .replace('’', '\'').toLowerCase(Locale.ROOT);
    }

    /** 原句中的词片段及其唯一规范词解析结果。 */
    private record TokenMatch(int start, int end, String text, EngWord word) {}

    /** 词典批量查询前保存的原始句子词片段。 */
    private record RawToken(int start, int end, String text, String key) {}

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
