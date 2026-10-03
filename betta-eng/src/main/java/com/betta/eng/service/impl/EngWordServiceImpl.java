package com.betta.eng.service.impl;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.time.LocalDate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.betta.common.exception.ServiceException;
import com.betta.common.utils.SecurityUtils;
import com.betta.common.utils.StringUtils;
import com.betta.eng.domain.EngArticleWordRel;
import com.betta.eng.domain.EngIcibaSentence;
import com.betta.eng.domain.EngWord;
import com.betta.eng.domain.vo.EngWordVo;
import com.betta.eng.mapper.EngWordMapper;
import com.betta.eng.mapper.EngDailyTestWordMapper;
import com.betta.eng.service.IEngArticleWordRelService;
import com.betta.eng.service.IEngIcibaSentenceService;
import com.betta.eng.service.IEngSentenceService;
import com.betta.eng.service.IEngUserScoreService;
import com.betta.eng.service.IEngWordService;
import com.betta.eng.utils.dict.DictUtils;
import com.betta.eng.utils.FamiliarityCalculator;

/** 单词业务实现，负责去重、详情聚合以及文章关系的全量同步。 */
@Service
public class EngWordServiceImpl implements IEngWordService
{
    private static final String LEARNING = "LEARNING";
    private static final String REVIEW = "REVIEW";
    private static final String NEW = "NEW";
    private static final int ROUND_WORD_LIMIT = 5;
    private static final int DAILY_NEW_WORD_LIMIT = 50;
    private final EngWordMapper mapper;
    private final EngDailyTestWordMapper dailyTestWordMapper;
    private final IEngArticleWordRelService relService;
    private final IEngUserScoreService scoreService;
    private final IEngSentenceService sentenceService;
    private final IEngIcibaSentenceService dictionarySentenceService;
    private final DictUtils dictUtils;

    /** 创建单词服务；参数依次负责单词、关系、成绩、句子、例句和词典访问。 */
    public EngWordServiceImpl(EngWordMapper mapper, IEngArticleWordRelService relService,
            IEngUserScoreService scoreService, IEngSentenceService sentenceService,
            IEngIcibaSentenceService dictionarySentenceService, DictUtils dictUtils,
            EngDailyTestWordMapper dailyTestWordMapper)
    {
        this.mapper = mapper;
        this.relService = relService;
        this.scoreService = scoreService;
        this.sentenceService = sentenceService;
        this.dictionarySentenceService = dictionarySentenceService;
        this.dictUtils = dictUtils;
        this.dailyTestWordMapper = dailyTestWordMapper;
    }

    @Override
    public EngWord selectEngWordById(Long id)
    {
        return mapper.selectEngWordById(id);
    }

    @Override
    public List<EngWordVo> selectEngWordList(EngWord word)
    {
        return mapper.selectEngWordList(word);
    }

    @Override
    public List<EngWordVo> selectWordListByArticle(Long articleId)
    {
        List<EngWordVo> result = selectDailyCandidatesByArticle(articleId);
        applyEffectiveFamiliarity(result);
        return result;
    }

    @Override
    public List<EngWordVo> selectDailyCandidatesByArticle(Long articleId)
    {
        return mapper.selectWordListByArticleId(articleId, SecurityUtils.getUsername());
    }

    @Override
    public List<EngWordVo> selectNextReviewWords(Long userId, Long articleId)
    {
        return selectNextReviewWords(userId, articleId, LocalDate.now(), new Date());
    }

    /** 使用明确日期和时间生成下一轮队列，供稳定的边界回归测试复用。 */
    List<EngWordVo> selectNextReviewWords(Long userId, Long articleId, LocalDate studyDate, Date now)
    {
        List<Long> completed = dailyTestWordMapper.selectCompletedWordIds(userId, studyDate);
        Set<Long> completedIds = new HashSet<>(completed == null ? List.of() : completed);
        Map<Long, EngWordVo> uniqueCandidates = new LinkedHashMap<>();
        List<EngWordVo> candidates = selectDailyCandidatesByArticle(articleId);
        for (EngWordVo word : candidates == null ? List.<EngWordVo>of() : candidates)
        {
            if (isValidReviewCandidate(word) && !completedIds.contains(word.getId()))
            {
                uniqueCandidates.putIfAbsent(word.getId(), word);
            }
        }

        List<EngWordVo> learning = new ArrayList<>();
        List<EngWordVo> review = new ArrayList<>();
        List<EngWordVo> newWords = new ArrayList<>();
        for (EngWordVo word : uniqueCandidates.values())
        {
            int base = FamiliarityCalculator.clamp(word.getBaseFamiliarity() == null
                    ? word.getFamiliarity() : word.getBaseFamiliarity());
            word.setBaseFamiliarity(base);
            word.setFamiliarity(FamiliarityCalculator.effective(base, word.getLastReviewTime(), now));
            if (!Boolean.TRUE.equals(word.getScoreExists()))
            {
                word.setReviewCategory(NEW);
                newWords.add(word);
            }
            else if (base < 3)
            {
                word.setReviewCategory(LEARNING);
                learning.add(word);
            }
            else if (FamiliarityCalculator.isReviewDue(base, word.getLastReviewTime(), now))
            {
                word.setReviewCategory(REVIEW);
                review.add(word);
            }
        }

        learning.sort(Comparator.comparing(EngWordVo::getBaseFamiliarity)
                .thenComparing(EngWordVo::getLastReviewTime, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(EngWordServiceImpl::normalizedWordName)
                .thenComparing(EngWordVo::getId));
        review.sort(Comparator.comparingDouble((EngWordVo word) ->
                        FamiliarityCalculator.overdueRatio(word.getBaseFamiliarity(), word.getLastReviewTime(), now))
                .reversed().thenComparing(EngWordVo::getFamiliarity)
                .thenComparing(EngWordServiceImpl::normalizedWordName).thenComparing(EngWordVo::getId));
        newWords.sort(Comparator.comparing(EngWordVo::getRelId, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(EngWordServiceImpl::normalizedWordName).thenComparing(EngWordVo::getId));

        List<EngWordVo> selected = new ArrayList<>(ROUND_WORD_LIMIT);
        appendReviewWords(selected, learning, ROUND_WORD_LIMIT);
        appendReviewWords(selected, review, ROUND_WORD_LIMIT);
        int remainingNewQuota = Math.max(0, DAILY_NEW_WORD_LIMIT
                - dailyTestWordMapper.countNewWords(userId, articleId, studyDate));
        appendReviewWords(selected, newWords, Math.min(ROUND_WORD_LIMIT, selected.size() + remainingNewQuota));
        return selected;
    }

    /** 按目标总数从候选列表追加单词。 */
    private static void appendReviewWords(List<EngWordVo> selected, List<EngWordVo> candidates, int targetSize)
    {
        for (EngWordVo candidate : candidates)
        {
            if (selected.size() >= targetSize || selected.size() >= ROUND_WORD_LIMIT)
            {
                break;
            }
            selected.add(candidate);
        }
    }

    private static String normalizedWordName(EngWordVo word)
    {
        return word.getWordName() == null ? "" : word.getWordName().toLowerCase(Locale.ROOT);
    }

    /** 无法生成基础双向题的脏数据不能占用本轮五个名额。 */
    private static boolean isValidReviewCandidate(EngWordVo word)
    {
        return word != null && word.getId() != null && StringUtils.isNotEmpty(word.getWordName())
                && StringUtils.isNotEmpty(word.getAcceptation());
    }

    @Override
    public List<EngWordVo> selectLowestFamiliarityWordsByArticle(Long articleId)
    {
        List<EngWordVo> result = mapper.selectLowestFamiliarityWordsByArticleId(articleId, SecurityUtils.getUsername());
        applyEffectiveFamiliarity(result);
        result.sort((left, right) -> {
            int familiarity = Integer.compare(left.getFamiliarity(), right.getFamiliarity());
            if (familiarity != 0) {
                return familiarity;
            }
            int name = left.getWordName().compareTo(right.getWordName());
            return name != 0 ? name : left.getId().compareTo(right.getId());
        });
        return result.size() <= 5 ? result : new ArrayList<>(result.subList(0, 5));
    }

    @Override
    public void updateFamiliarity(String wordName, int delta)
    {
        scoreService.updateEngUserScore(wordName, delta);
    }

    /** 将文章单词携带的基础值换算为查询时刻的有效熟悉度。 */
    private void applyEffectiveFamiliarity(List<EngWordVo> words)
    {
        Date now = new Date();
        for (EngWordVo word : words)
        {
            word.setFamiliarity(FamiliarityCalculator.effective(word.getBaseFamiliarity(),
                    word.getLastReviewTime(), now));
        }
    }

    @Override
    public int addEngWord(EngWord word)
    {
        String normalized = normalize(word == null ? null : word.getWordName());
        if (!mapper.selectEngWordByWordName(normalized).isEmpty())
        {
            throw new ServiceException("单词 " + normalized + " 已存在");
        }
        word.setWordName(normalized);
        word.setCreateBy(SecurityUtils.getUsername());
        return mapper.insertEngWord(word);
    }

    @Override
    @Transactional
    public EngWordVo getWordVo(String wordName)
    {
        EngWord word = getOrCreate(normalize(wordName));
        EngWordVo result = new EngWordVo();
        BeanUtils.copyProperties(word, result);
        EngIcibaSentence sentence = new EngIcibaSentence();
        sentence.setWordId(word.getId());
        result.setIcibaSentenceList(dictionarySentenceService.selectEngIcibaSentenceList(sentence));
        result.setSentenceList(sentenceService.selectByWordTop10(word));
        EngArticleWordRel rel = new EngArticleWordRel();
        // articleId=0 专用于当前用户生词收藏，文章关系不能作为收藏状态返回。
        rel.setArticleId(0L);
        rel.setWordName(word.getWordName());
        List<EngArticleWordRel> rels = relService.selectEngArticleWordRelList(rel);
        if (!rels.isEmpty())
        {
            result.setRelId(rels.get(0).getId());
        }
        return result;
    }

    @Override
    public EngWord getWordFromApi(String wordName)
    {
        return dictUtils.getWord(normalize(wordName));
    }

    @Override
    public int updateEngWord(EngWord word)
    {
        if (word == null || word.getId() == null)
        {
            throw new ServiceException("单词主键不能为空");
        }
        word.setWordName(normalize(word.getWordName()));
        word.setUpdateBy(SecurityUtils.getUsername());
        return mapper.updateEngWord(word);
    }

    @Override
    @Transactional
    public void deleteEngWordByIds(Long[] ids)
    {
        for (Long id : ids)
        {
            dictionarySentenceService.deleteByWordId(id);
            mapper.deleteEngWordById(id);
        }
    }

    @Override
    public List<EngWord> selectNewList(EngWord word)
    {
        return mapper.selectRelList(word, SecurityUtils.getUsername());
    }

    @Override
    @Transactional
    public void updateByArticle(List<String> words, Long articleId)
    {
        if (articleId == null || words == null)
        {
            throw new ServiceException("文章和单词列表不能为空");
        }
        EngArticleWordRel condition = new EngArticleWordRel();
        condition.setArticleId(articleId);
        List<EngArticleWordRel> old = relService.selectEngArticleWordRelList(condition);
        Set<String> desired = new HashSet<>();
        for (String value : words)
        {
            if (StringUtils.isNotEmpty(value))
            {
                EngWord resolvedWord = getOrCreate(normalize(value));
                desired.add(normalize(resolvedWord.getWordName()));
            }
        }
        List<Long> removeIds = new ArrayList<>();
        for (EngArticleWordRel rel : old)
        {
            String oldWordName = StringUtils.isEmpty(rel.getWordName()) ? null : normalize(rel.getWordName());
            if (oldWordName == null || !desired.remove(oldWordName))
            {
                removeIds.add(rel.getId());
            }
        }
        if (!removeIds.isEmpty())
        {
            relService.deleteEngArticleWordRelByIds(removeIds.toArray(new Long[0]));
        }
        for (String canonicalWordName : desired)
        {
            EngArticleWordRel rel = new EngArticleWordRel();
            rel.setArticleId(articleId);
            rel.setWordName(canonicalWordName);
            relService.insertEngArticleWordRel(rel);
        }
    }

    @Override
    @Transactional
    public void addArticleWord(Long articleId, String wordName)
    {
        String normalized = normalize(wordName);
        EngWord resolvedWord = getOrCreate(normalized);
        addResolvedArticleWord(articleId, normalize(resolvedWord.getWordName()));
    }

    @Override
    @Transactional
    public List<String> addArticleWords(Long articleId, List<String> words)
    {
        if (articleId == null || words == null)
        {
            throw new ServiceException("文章和单词列表不能为空");
        }
        Set<String> normalizedInputs = new LinkedHashSet<>();
        for (String word : words)
        {
            if (StringUtils.isNotEmpty(word) && StringUtils.isNotEmpty(word.trim()))
            {
                normalizedInputs.add(normalize(word));
            }
        }
        if (normalizedInputs.isEmpty())
        {
            throw new ServiceException("单词列表不能为空");
        }

        List<String> missingWords = new ArrayList<>();
        Set<String> canonicalWordNames = new LinkedHashSet<>();
        for (String normalizedInput : normalizedInputs)
        {
            try
            {
                EngWord resolvedWord = getOrCreate(normalizedInput);
                String canonicalWordName = normalize(resolvedWord.getWordName());
                canonicalWordNames.add(canonicalWordName);
            }
            catch (ServiceException exception)
            {
                // 未收录属于可跳过的业务结果；其他业务异常仍由事务统一回滚。
                if (!isMissingDictionaryWord(exception, normalizedInput))
                {
                    throw exception;
                }
                missingWords.add(normalizedInput);
            }
        }
        for (String canonicalWordName : canonicalWordNames)
        {
            addResolvedArticleWord(articleId, canonicalWordName);
        }
        return missingWords;
    }

    /** 按已解析的正式词头新增关系，确保批次内每个词头只执行一次原有规则。 */
    private void addResolvedArticleWord(Long articleId, String canonicalWordName)
    {
        EngArticleWordRel condition = new EngArticleWordRel();
        condition.setArticleId(articleId);
        condition.setWordName(canonicalWordName);
        if (relService.selectEngArticleWordRelList(condition).isEmpty())
        {
            EngArticleWordRel rel = new EngArticleWordRel();
            rel.setArticleId(articleId);
            rel.setWordName(canonicalWordName);
            relService.insertEngArticleWordRel(rel);
        }
        else
        {
            // 重复收藏生词是幂等操作；真实文章重复加词仍沿用原有扣熟悉度规则。
            if (Long.valueOf(0L).equals(articleId))
            {
                return;
            }
            scoreService.updateEngUserScore(canonicalWordName, -1);
        }
    }

    /** 判断本地词典查询异常是否仅表示 normalizedInput 未收录。 */
    private boolean isMissingDictionaryWord(ServiceException exception, String normalizedInput)
    {
        return ("词典未查询到单词：" + normalizedInput).equals(exception.getMessage());
    }

    /** 将 value 规范化为小写非空单词。 */
    private String normalize(String value)
    {
        if (StringUtils.isEmpty(value) || StringUtils.isEmpty(value.trim()))
        {
            throw new ServiceException("单词不能为空");
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC).trim().toLowerCase(Locale.ROOT);
    }

    /** 仅从本地数据库查询 normalized 对应的正式词条或别名。 */
    private EngWord getOrCreate(String normalized)
    {
        return dictUtils.getWord(normalized);
    }
}
