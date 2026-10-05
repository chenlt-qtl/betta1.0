package com.betta.eng.service.impl;

import com.betta.common.exception.ServiceException;
import com.betta.common.constant.CacheConstants;
import com.betta.common.core.redis.RedisCache;
import com.betta.common.utils.SecurityUtils;
import com.betta.common.utils.StringUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.betta.eng.config.EngPronunciationProperties;
import com.betta.eng.domain.*;
import com.betta.eng.domain.dto.*;
import com.betta.eng.domain.vo.*;
import com.betta.eng.mapper.*;
import com.betta.eng.service.*;
import com.betta.eng.utils.EngWordStarCalculator;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 新词关卡、全局复习与独立拼写测试统一学习服务。 */
@Service
public class EngStudyServiceImpl implements IEngStudyService
{
    private static final String NEW = "NEW";
    private static final String REVIEW = "REVIEW";
    private static final String SPELLING = "SPELLING";
    private static final String PRONUNCIATION_PREFIX = "PRONUNCIATION:";
    private static final String ASSESSED = "ASSESSED";
    private static final List<String> PREFIXES = List.of("WORD_TO_CN:", "CN_TO_WORD:",
            "SENTENCE_CHOICE:", "SENTENCE_FILL:", PRONUNCIATION_PREFIX);
    private static final int REVIEW_WORD_LIMIT = 5;
    private static final int FILL_LENGTH = 4;
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";
    private static final Pattern ASCII_WORD = Pattern.compile("[A-Za-z]+");
    private static final int PRONUNCIATION_CACHE_MINUTES = 30;
    private static final int MAX_AUDIO_BYTES = 256 * 1024;
    private static final int MIN_AUDIO_MILLIS = 300;
    private static final int MAX_AUDIO_MILLIS = 5000;
    private static final int MAX_PRONUNCIATION_ATTEMPTS = 3;
    private static final int PRONUNCIATION_CACHE_SECONDS = PRONUNCIATION_CACHE_MINUTES * 60;
    private static final DefaultRedisScript<Long> BEGIN_PRONUNCIATION_SCRIPT = new DefaultRedisScript<>(
            "local count=tonumber(redis.call('get',KEYS[3]) or '0');"
                    + "if count>=tonumber(ARGV[3]) then return 0 end;"
                    + "count=redis.call('incr',KEYS[3]);redis.call('expire',KEYS[3],ARGV[2]);"
                    + "redis.call('set',KEYS[1],ARGV[1],'EX',ARGV[2]);redis.call('del',KEYS[2]);return count",
            Long.class);
    private static final DefaultRedisScript<Long> RELEASE_PRONUNCIATION_ATTEMPT_SCRIPT = new DefaultRedisScript<>(
            "local count=tonumber(redis.call('get',KEYS[1]) or '0');"
                    + "if count<=1 then redis.call('del',KEYS[1]);return 0 end;"
                    + "count=redis.call('decr',KEYS[1]);redis.call('expire',KEYS[1],ARGV[1]);return count",
            Long.class);
    private static final DefaultRedisScript<Long> SAVE_PRONUNCIATION_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get',KEYS[1])==ARGV[1] then redis.call('set',KEYS[2],ARGV[2],'EX',ARGV[3]);return 1 end;return 0",
            Long.class);

    private final IEngArticleService articleService;
    private final IEngSentenceService sentenceService;
    private final IEngIcibaSentenceService dictionarySentenceService;
    private final IEngWordService wordService;
    private final EngStudyRecordMapper recordMapper;
    private final EngStudyRecordWordMapper recordWordMapper;
    private final EngCoinWalletMapper coinWalletMapper;
    private final EngWrongWordMapper wrongWordMapper;
    private final EngUserWordProgressMapper wordProgressMapper;
    private final EngArticleLevelProgressMapper levelProgressMapper;
    private final EngArticleWordRelMapper articleWordRelMapper;
    private final EngPronunciationProperties pronunciationProperties;
    private final IPronunciationAssessmentClient pronunciationClient;
    private final RedisCache redisCache;

    /** 注入学习、进度、记录和奖励依赖。 */
    @Autowired
    public EngStudyServiceImpl(IEngArticleService articleService, IEngSentenceService sentenceService,
            IEngIcibaSentenceService dictionarySentenceService, IEngWordService wordService,
            EngStudyRecordMapper recordMapper, EngStudyRecordWordMapper recordWordMapper,
            EngCoinWalletMapper coinWalletMapper, EngWrongWordMapper wrongWordMapper,
            EngUserWordProgressMapper wordProgressMapper, EngArticleLevelProgressMapper levelProgressMapper,
            EngArticleWordRelMapper articleWordRelMapper, EngPronunciationProperties pronunciationProperties,
            IPronunciationAssessmentClient pronunciationClient, RedisCache redisCache)
    {
        this.articleService = articleService;
        this.sentenceService = sentenceService;
        this.dictionarySentenceService = dictionarySentenceService;
        this.wordService = wordService;
        this.recordMapper = recordMapper;
        this.recordWordMapper = recordWordMapper;
        this.coinWalletMapper = coinWalletMapper;
        this.wrongWordMapper = wrongWordMapper;
        this.wordProgressMapper = wordProgressMapper;
        this.levelProgressMapper = levelProgressMapper;
        this.articleWordRelMapper = articleWordRelMapper;
        this.pronunciationProperties = pronunciationProperties;
        this.pronunciationClient = pronunciationClient;
        this.redisCache = redisCache;
    }

    /** 供不启用跟读的独立回归入口复用原有依赖装配。 */
    EngStudyServiceImpl(IEngArticleService articleService, IEngSentenceService sentenceService,
            IEngIcibaSentenceService dictionarySentenceService, IEngWordService wordService,
            EngStudyRecordMapper recordMapper, EngStudyRecordWordMapper recordWordMapper,
            EngCoinWalletMapper coinWalletMapper, EngWrongWordMapper wrongWordMapper,
            EngUserWordProgressMapper wordProgressMapper, EngArticleLevelProgressMapper levelProgressMapper,
            EngArticleWordRelMapper articleWordRelMapper)
    {
        this(articleService, sentenceService, dictionarySentenceService, wordService, recordMapper, recordWordMapper,
                coinWalletMapper, wrongWordMapper, wordProgressMapper, levelProgressMapper, articleWordRelMapper,
                new EngPronunciationProperties(), null, null);
    }

    @Override
    public EngStudySummaryVo getSummary()
    {
        Long userId = SecurityUtils.getUserId();
        EngStudySummaryVo summary = new EngStudySummaryVo();
        summary.setTotalScore(recordMapper.sumScoreByUserId(userId));
        summary.setCoinBalance(coinWalletMapper.selectCoinBalance(userId));
        summary.setStudyCount(recordMapper.countByUserId(userId));
        summary.setCompletedArticleCount(levelProgressMapper.countCompletedArticles(userId, SecurityUtils.getUsername()));
        summary.setWrongWordCount(wrongWordMapper.countByUserAndMastered(userId, 0));
        summary.setMasteredWrongWordCount(wrongWordMapper.countByUserAndMastered(userId, 1));
        summary.setRecentRecords(recordMapper.selectRecentByUserId(userId));
        return summary;
    }

    @Override
    @Transactional
    public EngArticleLevelMapVo getArticleLevels(Long articleId)
    {
        EngArticle article = requireArticle(articleId);
        Long userId = SecurityUtils.getUserId();
        String username = SecurityUtils.getUsername();
        if (articleWordRelMapper.lockArticle(articleId, username) == null)
            throw new ServiceException("文章不存在或无权操作");
        Map<Integer, EngArticleLevelProgress> progressMap = levelProgressMapper
                .selectByUserAndArticle(userId, articleId).stream()
                .collect(Collectors.toMap(EngArticleLevelProgress::getLevelNo, item -> item));
        int maxLevel = value(articleWordRelMapper.selectMaxLevelNo(articleId));
        List<EngArticleLevelVo> levels = new ArrayList<>();
        boolean priorCompleted = true;
        int completed = 0;
        for (int levelNo = 1; levelNo <= maxLevel; levelNo++)
        {
            List<EngWordVo> words = wordsForLevel(articleId, levelNo);
            if (words.isEmpty()) continue;
            List<Long> ids = words.stream().map(EngWordVo::getId).toList();
            Set<Long> learned = new HashSet<>(wordProgressMapper.selectLearnedWordIds(userId, ids));
            EngArticleLevelProgress progress = progressMap.get(levelNo);
            boolean allLearnedElsewhere = learned.size() == ids.size();
            if (allLearnedElsewhere)
            {
                for (Long wordId : ids)
                {
                    EngUserWordProgress wordProgress = wordProgressMapper.selectByUserAndWord(userId, wordId);
                    if (wordProgress == null || (articleId.equals(wordProgress.getFirstArticleId())
                            && Integer.valueOf(levelNo).equals(wordProgress.getFirstLevelNo())))
                    {
                        allLearnedElsewhere = false;
                        break;
                    }
                }
            }
            if (allLearnedElsewhere && (progress == null || value(progress.getCompletedByKnownWords()) == 0))
            {
                if (progress == null) progress = levelProgress(userId, articleId, levelNo, username);
                progress.setCompletedByKnownWords(1);
                levelProgressMapper.upsertKnown(progress);
                progressMap.put(levelNo, progress);
            }
            EngArticleLevelVo level = new EngArticleLevelVo();
            level.setLevelNo(levelNo);
            level.setTotalWordCount(ids.size());
            level.setLearnedWordCount(learned.size());
            level.setNewWordCount(ids.size() - learned.size());
            level.setUnlocked(levels.isEmpty() || priorCompleted);
            level.setMasteredByExistingWords(progress != null && value(progress.getCompletedByKnownWords()) == 1);
            level.setBestScore(progress == null ? 0 : value(progress.getBestScore()));
            level.setHighestStars(progress == null ? 0 : value(progress.getHighestStars()));
            priorCompleted = level.getHighestStars() >= 1 || Boolean.TRUE.equals(level.getMasteredByExistingWords());
            if (priorCompleted) completed++;
            levels.add(level);
        }
        EngArticleLevelMapVo map = new EngArticleLevelMapVo();
        map.setArticleId(articleId); map.setTitle(article.getTitle()); map.setTotalLevels(levels.size());
        map.setCompletedLevels(completed); map.setPronunciationEnabled(pronunciationProperties.isAvailable());
        map.setLevels(levels);
        return map;
    }

    @Override
    public EngReviewOverviewVo getReviewOverview()
    {
        Date now = new Date();
        List<EngReviewWordVo> words = wordProgressMapper.selectReviewWords(SecurityUtils.getUserId());
        for (EngReviewWordVo word : words)
        {
            int current = EngWordStarCalculator.currentStars(word.getLatestStars(), word.getLatestTestTime(), now);
            word.setCurrentStars(current);
            word.setRecommended(current < value(word.getHighestStars()));
            word.setSourceArticles(StringUtils.isEmpty(word.getSourceArticleNames()) ? List.of()
                    : List.of(word.getSourceArticleNames().split(",")));
            word.setSpellingEligible(isSpellingEligible(word.getWordName()));
        }
        words.sort(Comparator.comparing((EngReviewWordVo word) -> !Boolean.TRUE.equals(word.getRecommended()))
                .thenComparing(EngReviewWordVo::getCurrentStars)
                .thenComparing(EngReviewWordVo::getLatestTestTime, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(EngReviewWordVo::getWordId));
        EngReviewOverviewVo overview = new EngReviewOverviewVo();
        overview.setLearnedWordCount(words.size());
        overview.setRecommendedCount((int) words.stream().filter(EngReviewWordVo::getRecommended).count());
        overview.setWords(words);
        return overview;
    }

    @Override
    @Transactional
    public EngChallengeVo getChallenge(String mode, Long articleId, Integer levelNo, List<Long> wordIds)
    {
        return getChallenge(mode, articleId, levelNo, wordIds, true);
    }

    @Override
    @Transactional
    public EngChallengeVo getChallenge(String mode, Long articleId, Integer levelNo, List<Long> wordIds,
            boolean pronunciationAllowed)
    {
        String actualMode = requireMode(mode);
        List<EngWordVo> words = NEW.equals(actualMode) ? newWordsForLevel(articleId, levelNo, true)
                : reviewWords(wordIds, SPELLING.equals(actualMode));
        if (words.isEmpty()) throw new ServiceException(NEW.equals(actualMode) ? "本关新词已全部掌握" : "暂无可复习单词");
        String attemptId = UUID.randomUUID().toString();
        List<QuestionDefinition> definitions = buildDefinitions(actualMode, articleId, words, attemptId,
                pronunciationAllowed);
        EngChallengeVo challenge = new EngChallengeVo();
        challenge.setAttemptId(attemptId); challenge.setMode(actualMode);
        challenge.setArticleId(articleId); challenge.setLevelNo(levelNo);
        challenge.setTitle(NEW.equals(actualMode) ? requireArticle(articleId).getTitle()
                : SPELLING.equals(actualMode) ? "拼写测试" : "单词复习");
        challenge.setWords(words); challenge.setPronunciationEnabled(pronunciationAllowed && !SPELLING.equals(actualMode)
                && pronunciationProperties.isAvailable());
        List<EngChallengeQuestionVo> questions = definitions.stream().map(this::questionVo)
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(questions); challenge.setQuestions(questions);
        return challenge;
    }

    @Override
    public EngPronunciationAssessmentVo assessPronunciation(EngPronunciationAssessDto request)
    {
        validatePronunciationRequest(request);
        String mode = requireMode(request.getMode());
        Long wordId = extractWordId(request.getQuestionId());
        if (wordId == null || !request.getQuestionId().startsWith(PRONUNCIATION_PREFIX))
            throw new ServiceException("跟读题目标识无效");
        List<EngWordVo> words = resolveSubmittedWords(mode, request.getArticleId(), request.getLevelNo(),
                Set.of(wordId), false);
        QuestionDefinition definition = findDefinition(buildDefinitions(mode, request.getArticleId(), words,
                request.getAttemptId(), true),
                request.getQuestionId());
        if (definition == null || !isPronunciation(definition)) throw new ServiceException("跟读题不属于当前测试");
        byte[] audio = audioBytes(request);
        validateWav(audio);
        Long userId = SecurityUtils.getUserId();
        String generation = UUID.randomUUID().toString();
        int attemptCount = beginPronunciation(userId, request, generation);
        EngPronunciationAssessmentVo result;
        try
        {
            result = pronunciationClient.assess(definition.word().getWordName(), audio);
        }
        catch (RuntimeException exception)
        {
            releasePronunciationAttempt(userId, request, exception);
            throw exception;
        }
        if (result == null || result.getAccuracy() == null || result.getScore() == null)
        {
            ServiceException exception = new ServiceException("未取得有效跟读评分，请重试");
            releasePronunciationAttempt(userId, request, exception);
            throw exception;
        }
        int matchTag = result.getMatchTag() == null ? (Boolean.TRUE.equals(result.getMatched()) ? 0 : 3)
                : result.getMatchTag();
        result.setQuestionId(definition.id()); result.setMatchTag(matchTag); result.setMatched(matchTag == 0);
        result.setPassed(matchTag == 0 && result.getAccuracy() >= 60D);
        result.setAttemptCount(attemptCount);
        result.setRemainingAttempts(MAX_PRONUNCIATION_ATTEMPTS - attemptCount);
        if (!cachePronunciation(userId, request, result, generation))
            throw new ServiceException("本次跟读结果已过期，请使用最新录音重试");
        return result;
    }

    @Override
    @Transactional
    public EngChallengeResultVo.ResultItem checkChallengeAnswer(EngChallengeCheckDto request)
    {
        if (request == null || StringUtils.isEmpty(request.getAttemptId())
                || StringUtils.isEmpty(request.getQuestionId()) || StringUtils.isEmpty(request.getAnswer()))
            throw new ServiceException("测试标识、题目标识和答案不能为空");
        String mode = requireMode(request.getMode());
        Long wordId = extractWordId(request.getQuestionId());
        if (wordId == null) throw new ServiceException("题目标识无效");
        List<EngWordVo> words = resolveSubmittedWords(mode, request.getArticleId(), request.getLevelNo(), Set.of(wordId), false);
        QuestionDefinition definition = findDefinition(buildDefinitions(mode, request.getArticleId(), words,
                request.getAttemptId(), true),
                request.getQuestionId());
        if (definition == null) throw new ServiceException("题目不属于当前测试");
        if (isPronunciation(definition)) throw new ServiceException("跟读题请使用跟读评分接口");
        return resultItem(definition, request.getAnswer());
    }

    @Override
    @Transactional
    public EngChallengeResultVo submitChallenge(EngChallengeSubmitDto request)
    {
        return submitChallenge(request, true);
    }

    @Override
    @Transactional
    public EngChallengeResultVo submitChallenge(EngChallengeSubmitDto request, boolean pronunciationAllowed)
    {
        validateSubmit(request);
        Long userId = SecurityUtils.getUserId();
        String username = SecurityUtils.getUsername();
        EngStudyRecord existing = findAttempt(userId, request.getAttemptId());
        if (existing != null) return existingResult(existing);
        String mode = requireMode(request.getMode());
        if (NEW.equals(mode) && articleWordRelMapper.lockArticle(request.getArticleId(), username) == null)
            throw new ServiceException("文章不存在或无权操作");
        Set<Long> submittedIds = request.getAnswers().stream().map(item -> extractWordId(item.getQuestionId()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (submittedIds.contains(null)) throw new ServiceException("题目标识无效");
        List<EngWordVo> words = resolveSubmittedWords(mode, request.getArticleId(), request.getLevelNo(), submittedIds, true);
        List<QuestionDefinition> definitions = buildDefinitions(mode, request.getArticleId(), words,
                request.getAttemptId(), pronunciationAllowed);
        Map<String, String> answers = validateAnswers(request.getAnswers(), definitions);
        Map<Long, EngPronunciationAssessmentVo> pronunciation = loadPronunciationAssessments(
                userId, request, mode, definitions, answers);
        EngChallengeResultVo result = calculateResult(request, mode, definitions, answers, pronunciation);
        EngStudyRecord record = recordHeader(userId, username, request, mode, result);
        if (recordMapper.insertIgnoreEngStudyRecord(record) == 0)
            return existingResult(findAttemptForUpdate(userId, request.getAttemptId()));

        Map<Long, WordScore> scores = wordScores(definitions, result.getResults(), mode, result.getStars(),
                request.getArticleId(), userId, pronunciation);
        List<EngStudyRecordWord> details = new ArrayList<>();
        long milestoneCoin = 0, reviewCoin = 0;
        Date now = new Date();
        for (EngWordVo word : words)
        {
            WordScore score = scores.get(word.getId());
            EngUserWordProgress progress = lockProgress(userId, username, word.getId(), request.getArticleId(), request.getLevelNo());
            int targetHighest = Math.max(value(progress.getHighestStars()), score.stars());
            int targetSpellingRewarded = Math.max(value(progress.getSpellingRewardedStars()), score.stars());
            long wordMilestone = SPELLING.equals(mode)
                    ? EngWordStarCalculator.spellingMilestoneCoin(value(progress.getSpellingRewardedStars()),
                            targetSpellingRewarded)
                    : EngWordStarCalculator.milestoneCoin(value(progress.getRewardedStars()), targetHighest);
            long wordReview = REVIEW.equals(mode) && score.allCorrect() ? 1 : 0;
            progress.setLearned(1);
            if (SPELLING.equals(mode))
            {
                // 拼写奖励进度与普通学习星级隔离，避免拼写测试刷新遗忘曲线或占用普通里程碑。
                progress.setSpellingRewardedStars(targetSpellingRewarded);
            }
            else
            {
                progress.setHighestStars(targetHighest); progress.setLatestStars(score.stars());
                progress.setLatestTestTime(now); progress.setRewardedStars(targetHighest);
            }
            progress.setUpdateBy(username);
            wordProgressMapper.updateProgress(progress);
            milestoneCoin += wordMilestone; reviewCoin += wordReview;
            details.add(detail(record.getId(), word, score, mode, wordMilestone, wordReview));
            updateWrongWord(userId, username, word, score);
            wordService.updateFamiliarity(word.getWordName(), score.allCorrect() ? 1 : -1);
        }
        recordWordMapper.insertBatch(details);
        if (NEW.equals(mode))
        {
            EngArticleLevelProgress level = levelProgress(userId, request.getArticleId(), request.getLevelNo(), username);
            level.setBestScore(result.getScore()); level.setHighestStars(result.getStars());
            levelProgressMapper.upsertBest(level);
            applyNextLevel(result, request.getArticleId(), request.getLevelNo(), result.getStars());
        }
        result.setMilestoneCoin(milestoneCoin); result.setReviewCoin(reviewCoin);
        result.setCoinReward(milestoneCoin + reviewCoin);
        if (result.getCoinReward() > 0) coinWalletMapper.increaseCoinBalance(userId, result.getCoinReward(), username);
        result.setCoinBalance(coinWalletMapper.selectCoinBalance(userId));
        record.setMilestoneCoin(milestoneCoin); record.setReviewCoin(reviewCoin); record.setCoinReward(result.getCoinReward());
        recordMapper.updateOutcome(record);
        result.setWordResults(buildWordResults(words, scores, userId, details));
        return result;
    }

    @Override
    public List<EngWrongWord> selectWrongWordList(EngWrongWord wrongWord)
    {
        EngWrongWord condition = wrongWord == null ? new EngWrongWord() : wrongWord;
        condition.setUserId(SecurityUtils.getUserId()); return wrongWordMapper.selectEngWrongWordList(condition);
    }

    @Override
    public int markWrongWordMastered(Long id)
    {
        if (id == null) throw new ServiceException("错词主键不能为空");
        int rows = wrongWordMapper.markMastered(id, SecurityUtils.getUserId(), SecurityUtils.getUsername());
        if (rows == 0) throw new ServiceException("错词不存在或无权操作"); return rows;
    }

    @Override
    public List<EngStudyRecordWord> selectRecordWords(Long recordId)
    {
        if (recordId == null) throw new ServiceException("学习记录主键不能为空");
        return recordWordMapper.selectByRecordAndUser(recordId, SecurityUtils.getUserId());
    }

    private List<EngWordVo> newWordsForLevel(Long articleId, Integer levelNo, boolean requireUnlocked)
    {
        if (articleId == null || levelNo == null || levelNo < 1) throw new ServiceException("文章和关卡不能为空");
        requireArticle(articleId);
        if (requireUnlocked)
        {
            EngArticleLevelVo level = getArticleLevels(articleId).getLevels().stream()
                    .filter(item -> levelNo.equals(item.getLevelNo())).findFirst()
                    .orElseThrow(() -> new ServiceException("关卡不存在"));
            if (!Boolean.TRUE.equals(level.getUnlocked())) throw new ServiceException("请先通过上一关");
        }
        List<EngWordVo> all = wordsForLevel(articleId, levelNo);
        if (all.isEmpty()) throw new ServiceException("关卡不存在");
        Long userId = SecurityUtils.getUserId();
        boolean masteredByExistingWords = levelProgressMapper.selectByUserAndArticle(userId, articleId).stream()
                .anyMatch(item -> levelNo.equals(item.getLevelNo()) && value(item.getCompletedByKnownWords()) == 1);
        // 自动掌握关仍允许反复正式测试，且测试范围必须恢复为本关全部单词。
        if (masteredByExistingWords) return all;
        List<EngWordVo> eligible = new ArrayList<>();
        for (EngWordVo word : all)
        {
            EngUserWordProgress progress = wordProgressMapper.selectByUserAndWord(userId, word.getId());
            if (progress == null || value(progress.getLearned()) == 0
                    || (articleId.equals(progress.getFirstArticleId()) && levelNo.equals(progress.getFirstLevelNo())))
                eligible.add(word);
        }
        return eligible;
    }

    private List<EngWordVo> reviewWords(List<Long> requested, boolean spellingOnly)
    {
        EngReviewOverviewVo overview = getReviewOverview();
        Map<Long, EngReviewWordVo> byId = overview.getWords().stream()
                .collect(Collectors.toMap(EngReviewWordVo::getWordId, item -> item, (a, b) -> a, LinkedHashMap::new));
        List<Long> ids = requested != null && !requested.isEmpty()
                ? new ArrayList<>(new LinkedHashSet<>(requested))
                : overview.getWords().stream().filter(item -> !spellingOnly || Boolean.TRUE.equals(item.getSpellingEligible()))
                        .filter(EngReviewWordVo::getRecommended).limit(REVIEW_WORD_LIMIT)
                        .map(EngReviewWordVo::getWordId).toList();
        if (ids.size() > REVIEW_WORD_LIMIT) throw new ServiceException("每轮最多复习5个单词");
        List<EngWordVo> result = new ArrayList<>();
        for (Long id : ids)
        {
            if (!byId.containsKey(id)) throw new ServiceException("只能复习已学单词");
            if (spellingOnly && !Boolean.TRUE.equals(byId.get(id).getSpellingEligible()))
                throw new ServiceException("拼写测试仅支持不少于4个字符的英文单词");
            EngWord word = wordService.selectEngWordById(id);
            if (word == null) throw new ServiceException("复习单词不存在");
            EngWordVo vo = new EngWordVo(); BeanUtils.copyProperties(word, vo); result.add(vo);
        }
        return result;
    }

    private List<EngWordVo> resolveSubmittedWords(String mode, Long articleId, Integer levelNo, Set<Long> ids,
            boolean requireCompleteLevel)
    {
        List<EngWordVo> candidates = NEW.equals(mode) ? newWordsForLevel(articleId, levelNo, true)
                : reviewWords(new ArrayList<>(ids), SPELLING.equals(mode));
        Map<Long, EngWordVo> map = candidates.stream().collect(Collectors.toMap(EngWordVo::getId, item -> item,
                (a, b) -> a, LinkedHashMap::new));
        if (NEW.equals(mode) && requireCompleteLevel && !map.keySet().equals(ids))
            throw new ServiceException("请完成本关全部新词");
        if (!map.keySet().containsAll(ids)) throw new ServiceException("存在不属于当前测试的单词");
        return map.values().stream().filter(word -> ids.contains(word.getId())).toList();
    }

    private List<EngWordVo> wordsForLevel(Long articleId, Integer levelNo)
    {
        List<EngArticleWordRel> relations = articleWordRelMapper.selectByArticleAndLevel(articleId, levelNo,
                SecurityUtils.getUsername());
        Map<String, EngWordVo> byName = new HashMap<>();
        for (EngWordVo word : wordService.selectWordListByArticle(articleId))
            byName.putIfAbsent(word.getWordName().toLowerCase(Locale.ROOT), word);
        List<EngWordVo> result = new ArrayList<>(); Set<Long> ids = new HashSet<>();
        for (EngArticleWordRel relation : relations)
        {
            EngWordVo word = byName.get(relation.getWordName().toLowerCase(Locale.ROOT));
            if (validWord(word) && ids.add(word.getId())) result.add(word);
        }
        return result;
    }

    private List<QuestionDefinition> buildDefinitions(String mode, Long articleId, List<EngWordVo> words,
            String attemptId, boolean includePronunciation)
    {
        List<QuestionDefinition> result = new ArrayList<>();
        for (EngWordVo word : words)
        {
            if (SPELLING.equals(mode))
            {
                result.add(spellingFill(word, attemptId));
                continue;
            }
            result.add(wordToCn(word, words)); result.add(cnToWord(word, words));
            SentenceContent sentence = sentenceContent(mode, articleId, word);
            if (sentence != null)
                result.add(sentenceChoice(word, words, sentence));
            if (includePronunciation && pronunciationProperties.isAvailable()) result.add(pronunciation(word));
        }
        return result;
    }

    private QuestionDefinition pronunciation(EngWordVo word)
    {
        return definition(PRONUNCIATION_PREFIX, "PRONUNCIATION", "请跟读单词 “" + word.getWordName() + "”",
                ASSESSED, List.of(), word, null, null);
    }

    private QuestionDefinition wordToCn(EngWordVo word, List<EngWordVo> words)
    {
        String answer = meaning(word);
        return definition("WORD_TO_CN:", "WORD_TO_CN", "请选择单词 “" + word.getWordName() + "” 的正确释义",
                answer, options(answer, words, false, word.getId()), word, null, null);
    }

    private QuestionDefinition cnToWord(EngWordVo word, List<EngWordVo> words)
    {
        return definition("CN_TO_WORD:", "CN_TO_WORD", "请选择释义 “" + meaning(word) + "” 对应的英文单词",
                word.getWordName(), options(word.getWordName(), words, true, word.getId()), word, null, null);
    }

    private QuestionDefinition sentenceChoice(EngWordVo word, List<EngWordVo> words, SentenceContent sentence)
    {
        return definition("SENTENCE_CHOICE:", "SENTENCE_CHOICE",
                sentencePrompt("请选择句子中的空缺单词", sentence.choiceBlank(), sentence.acceptation()),
                sentence.answer(), options(sentence.answer(), words, true, word.getId()), word, null,
                sentence.sourceArticleId());
    }

    /** 根据测试标识稳定随机四个位置，同一轮的即时判题与提交可重建完全相同的题目。 */
    private QuestionDefinition spellingFill(EngWordVo word, String attemptId)
    {
        String source = word.getWordName().toLowerCase(Locale.ROOT);
        List<Integer> positions = new ArrayList<>();
        for (int index = 0; index < source.length(); index++) positions.add(index);
        long seed = 31L * Objects.hashCode(attemptId) + 17L * word.getId() + source.hashCode();
        Collections.shuffle(positions, new Random(seed));
        Set<Integer> hidden = new TreeSet<>(positions.subList(0, FILL_LENGTH));
        StringBuilder masked = new StringBuilder(source);
        StringBuilder answerBuilder = new StringBuilder(FILL_LENGTH);
        for (Integer position : hidden)
        {
            answerBuilder.append(source.charAt(position));
            masked.setCharAt(position, '_');
        }
        String answer = answerBuilder.toString();
        List<String> options = new ArrayList<>(); for (char item : answer.toCharArray()) options.add(String.valueOf(item));
        int start = Math.floorMod(Long.hashCode(seed), ALPHABET.length());
        for (int offset = 0; options.size() < 10; offset++)
        {
            String item = String.valueOf(ALPHABET.charAt((start + offset) % ALPHABET.length()));
            if (!answer.contains(item)) options.add(item);
        }
        Collections.shuffle(options, new Random(seed ^ 0x5DEECE66DL));
        return definition("SENTENCE_FILL:", "SENTENCE_FILL",
                "请选择单词 “" + masked + "” 中空缺的字母", answer, options, word, FILL_LENGTH, null);
    }

    private QuestionDefinition definition(String prefix, String type, String prompt, String answer,
            List<String> options, EngWordVo word, Integer length, Long sourceArticleId)
    {
        return new QuestionDefinition(prefix + word.getId(), type, prompt, answer, options, word.getPhMp3(), length,
                word, sourceArticleId);
    }

    private SentenceContent sentenceContent(String mode, Long articleId, EngWordVo word)
    {
        EngSentenceWordRel relation = NEW.equals(mode) ? sentenceService.selectFirstWordRelation(articleId, word.getId())
                : sentenceService.selectFirstWordRelation(word.getId());
        if (relation != null)
        {
            SentenceContent content = sentenceContent(relation.getSentenceContent(), relation.getSentenceAcceptation(),
                    relation.getMatchedText(), relation.getArticleId());
            if (content != null) return content;
        }
        EngIcibaSentence condition = new EngIcibaSentence(); condition.setWordId(word.getId());
        List<EngIcibaSentence> examples = dictionarySentenceService.selectEngIcibaSentenceList(condition);
        Long fallbackArticleId = articleId;
        if (REVIEW.equals(mode))
        {
            EngUserWordProgress progress = wordProgressMapper.selectByUserAndWord(SecurityUtils.getUserId(), word.getId());
            fallbackArticleId = progress == null ? null : progress.getFirstArticleId();
        }
        return examples == null || examples.isEmpty() ? null
                : sentenceContent(examples.get(0).getOrig(), examples.get(0).getTrans(), word.getWordName(), fallbackArticleId);
    }

    private SentenceContent sentenceContent(String text, String acceptation, String matchedText, Long sourceArticleId)
    {
        if (StringUtils.isEmpty(text) || text.startsWith("[NT]") || StringUtils.isEmpty(matchedText)) return null;
        Matcher matcher = wholeWord(matchedText).matcher(text); if (!matcher.find()) return null;
        String actual = matcher.group();
        String choice = matcher.replaceFirst(Matcher.quoteReplacement("_".repeat(actual.length())));
        String fill = actual.length() > FILL_LENGTH
                ? matcher.replaceFirst(Matcher.quoteReplacement("_".repeat(FILL_LENGTH) + actual.substring(FILL_LENGTH))) : null;
        return new SentenceContent(choice, fill, acceptation, actual, sourceArticleId);
    }

    private Map<Long, EngPronunciationAssessmentVo> loadPronunciationAssessments(Long userId,
            EngChallengeSubmitDto request, String mode, List<QuestionDefinition> definitions,
            Map<String, String> answers)
    {
        Map<Long, EngPronunciationAssessmentVo> result = new LinkedHashMap<>();
        for (QuestionDefinition definition : definitions)
        {
            if (!isPronunciation(definition)) continue;
            if (!ASSESSED.equals(answers.get(definition.id()))) throw new ServiceException("跟读题必须先完成评分");
            String cached = redisCache == null ? null : redisCache.getCacheObject(
                    pronunciationCacheKey(userId, request.getAttemptId(), definition.id()));
            if (StringUtils.isEmpty(cached)) throw new ServiceException("跟读评分已失效，请重新录制");
            JSONObject payload;
            try
            {
                payload = JSON.parseObject(cached);
            }
            catch (RuntimeException exception)
            {
                throw new ServiceException("跟读评分已失效，请重新录制");
            }
            if (!mode.equals(payload.getString("mode"))
                    || !Objects.equals(request.getArticleId(), payload.getLong("articleId"))
                    || !Objects.equals(request.getLevelNo(), payload.getInteger("levelNo"))
                    || !definition.id().equals(payload.getString("questionId")))
                throw new ServiceException("跟读评分不属于当前测试");
            Integer score = payload.getInteger("score"); Double accuracy = payload.getDouble("accuracy");
            Integer matchTag = payload.getInteger("matchTag");
            if (score == null || accuracy == null || matchTag == null)
                throw new ServiceException("跟读评分已失效，请重新录制");
            EngPronunciationAssessmentVo assessment = new EngPronunciationAssessmentVo();
            assessment.setQuestionId(definition.id()); assessment.setScore(score); assessment.setAccuracy(accuracy);
            assessment.setMatchTag(matchTag); assessment.setMatched(matchTag == 0);
            // 即便缓存内容异常，也以服务端固定合格线重新判定，客户端值永不参与计分。
            boolean passed = matchTag == 0 && accuracy >= 60D;
            assessment.setPassed(passed); result.put(definition.word().getId(), assessment);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private int beginPronunciation(Long userId, EngPronunciationAssessDto request, String generation)
    {
        String generationKey = pronunciationGenerationKey(userId, request.getAttemptId(), request.getQuestionId());
        String resultKey = pronunciationCacheKey(userId, request.getAttemptId(), request.getQuestionId());
        String attemptKey = pronunciationAttemptKey(userId, request.getAttemptId(), request.getQuestionId());
        if (redisCache.redisTemplate != null)
        {
            Long count = (Long) redisCache.redisTemplate.execute(BEGIN_PRONUNCIATION_SCRIPT,
                    List.of(generationKey, resultKey, attemptKey), generation, PRONUNCIATION_CACHE_SECONDS,
                    MAX_PRONUNCIATION_ATTEMPTS);
            if (count == null || count < 1) throw new ServiceException("每个单词最多测评3次");
            return count.intValue();
        }
        synchronized (redisCache)
        {
            Integer current = redisCache.getCacheObject(attemptKey);
            int count = current == null ? 0 : current;
            if (count >= MAX_PRONUNCIATION_ATTEMPTS) throw new ServiceException("每个单词最多测评3次");
            count++;
            redisCache.setCacheObject(attemptKey, count, PRONUNCIATION_CACHE_MINUTES, TimeUnit.MINUTES);
            redisCache.setCacheObject(generationKey, generation, PRONUNCIATION_CACHE_MINUTES, TimeUnit.MINUTES);
            redisCache.deleteObject(resultKey);
            return count;
        }
    }

    /** 第三方未返回有效评分时释放预占次数，不让服务异常消耗用户机会。 */
    @SuppressWarnings("unchecked")
    private void releasePronunciationAttempt(Long userId, EngPronunciationAssessDto request)
    {
        String attemptKey = pronunciationAttemptKey(userId, request.getAttemptId(), request.getQuestionId());
        if (redisCache.redisTemplate != null)
        {
            redisCache.redisTemplate.execute(RELEASE_PRONUNCIATION_ATTEMPT_SCRIPT, List.of(attemptKey),
                    PRONUNCIATION_CACHE_SECONDS);
            return;
        }
        synchronized (redisCache)
        {
            Integer current = redisCache.getCacheObject(attemptKey);
            if (current == null || current <= 1) redisCache.deleteObject(attemptKey);
            else redisCache.setCacheObject(attemptKey, current - 1,
                    PRONUNCIATION_CACHE_MINUTES, TimeUnit.MINUTES);
        }
    }

    /** 释放失败只作为附加诊断，不得覆盖第三方或无效响应的原始异常。 */
    private void releasePronunciationAttempt(Long userId, EngPronunciationAssessDto request,
            RuntimeException originalException)
    {
        try
        {
            releasePronunciationAttempt(userId, request);
        }
        catch (RuntimeException releaseException)
        {
            originalException.addSuppressed(releaseException);
        }
    }

    /** 使用 Redis 原子比较代次并写入，确保慢返回的旧录音不能覆盖较新的重录。 */
    @SuppressWarnings("unchecked")
    private boolean cachePronunciation(Long userId, EngPronunciationAssessDto request,
            EngPronunciationAssessmentVo assessment, String generation)
    {
        JSONObject payload = new JSONObject();
        payload.put("mode", requireMode(request.getMode())); payload.put("articleId", request.getArticleId());
        payload.put("levelNo", request.getLevelNo()); payload.put("questionId", request.getQuestionId());
        payload.put("score", assessment.getScore()); payload.put("accuracy", assessment.getAccuracy());
        payload.put("matchTag", assessment.getMatchTag());
        String generationKey = pronunciationGenerationKey(userId, request.getAttemptId(), request.getQuestionId());
        String resultKey = pronunciationCacheKey(userId, request.getAttemptId(), request.getQuestionId());
        if (redisCache.redisTemplate != null)
        {
            Long saved = (Long) redisCache.redisTemplate.execute(SAVE_PRONUNCIATION_SCRIPT,
                    List.of(generationKey, resultKey), generation, payload.toJSONString(),
                    PRONUNCIATION_CACHE_SECONDS);
            return Long.valueOf(1L).equals(saved);
        }
        synchronized (redisCache)
        {
            String current = redisCache.getCacheObject(generationKey);
            if (!generation.equals(current)) return false;
            redisCache.setCacheObject(resultKey, payload.toJSONString(), PRONUNCIATION_CACHE_MINUTES, TimeUnit.MINUTES);
            return true;
        }
    }

    private String pronunciationCacheKey(Long userId, String attemptId, String questionId)
    {
        return CacheConstants.ENG_PRONUNCIATION_KEY + userId + ":" + attemptId + ":" + questionId;
    }

    private String pronunciationGenerationKey(Long userId, String attemptId, String questionId)
    {
        return pronunciationCacheKey(userId, attemptId, questionId) + ":generation";
    }

    private String pronunciationAttemptKey(Long userId, String attemptId, String questionId)
    {
        return pronunciationCacheKey(userId, attemptId, questionId) + ":attempts";
    }

    private void validatePronunciationRequest(EngPronunciationAssessDto request)
    {
        if (!pronunciationProperties.isAvailable() || pronunciationClient == null || redisCache == null)
            throw new ServiceException("跟读评分服务暂未启用");
        if (request == null || StringUtils.isEmpty(request.getAttemptId()) || request.getAttemptId().length() != 36
                || StringUtils.isEmpty(request.getQuestionId()) || request.getAudio() == null
                || request.getAudio().isEmpty())
            throw new ServiceException("测试标识、题目标识和录音不能为空");
        try
        {
            UUID.fromString(request.getAttemptId());
        }
        catch (IllegalArgumentException exception)
        {
            throw new ServiceException("测试标识无效");
        }
    }

    private byte[] audioBytes(EngPronunciationAssessDto request)
    {
        if (request.getAudio().getSize() > MAX_AUDIO_BYTES) throw new ServiceException("录音文件不能超过256KB");
        try
        {
            return request.getAudio().getBytes();
        }
        catch (IOException exception)
        {
            throw new ServiceException("读取录音失败，请重新录制");
        }
    }

    /** 严格解析 PCM WAV 块，防止仅伪造文件头或利用异常块长度绕过校验。 */
    private void validateWav(byte[] audio)
    {
        if (audio == null || audio.length < 44 || audio.length > MAX_AUDIO_BYTES
                || !ascii(audio, 0, "RIFF") || !ascii(audio, 8, "WAVE"))
            throw new ServiceException("录音必须为16kHz单声道PCM WAV");
        ByteBuffer buffer = ByteBuffer.wrap(audio).order(ByteOrder.LITTLE_ENDIAN);
        if (Integer.toUnsignedLong(buffer.getInt(4)) + 8L != audio.length)
            throw new ServiceException("WAV录音结构无效");
        int offset = 12, format = -1, channels = -1, sampleRate = -1, byteRate = -1;
        int blockAlign = -1, bits = -1;
        int dataOffset = -1, dataSize = -1;
        while (offset + 8 <= audio.length)
        {
            long size = Integer.toUnsignedLong(buffer.getInt(offset + 4));
            long next = offset + 8L + size + (size & 1L);
            if (next > audio.length || size > Integer.MAX_VALUE) throw new ServiceException("WAV录音结构无效");
            if (ascii(audio, offset, "fmt "))
            {
                if (size < 16) throw new ServiceException("WAV录音格式块无效");
                format = Short.toUnsignedInt(buffer.getShort(offset + 8));
                channels = Short.toUnsignedInt(buffer.getShort(offset + 10));
                sampleRate = buffer.getInt(offset + 12);
                byteRate = buffer.getInt(offset + 16);
                blockAlign = Short.toUnsignedInt(buffer.getShort(offset + 20));
                bits = Short.toUnsignedInt(buffer.getShort(offset + 22));
            }
            else if (ascii(audio, offset, "data"))
            {
                dataOffset = offset + 8; dataSize = (int) size;
            }
            offset = (int) next;
        }
        if (format != 1 || channels != 1 || sampleRate != 16000 || byteRate != 32000 || blockAlign != 2
                || bits != 16 || dataOffset < 0 || dataSize <= 0 || dataSize % blockAlign != 0)
            throw new ServiceException("录音必须为16kHz、16bit、单声道PCM WAV");
        int durationMillis = (int) Math.round(dataSize * 1000D / (sampleRate * channels * bits / 8D));
        if (durationMillis < MIN_AUDIO_MILLIS || durationMillis > MAX_AUDIO_MILLIS)
            throw new ServiceException("录音时长必须在0.3至5秒之间");
        int peak = 0;
        for (int index = dataOffset; index + 1 < dataOffset + dataSize; index += 2)
            peak = Math.max(peak, Math.abs(buffer.getShort(index)));
        if (peak < 64) throw new ServiceException("未检测到有效声音，请重新录制");
    }

    private boolean ascii(byte[] source, int offset, String expected)
    {
        if (offset < 0 || offset + expected.length() > source.length) return false;
        for (int index = 0; index < expected.length(); index++)
            if (source[offset + index] != (byte) expected.charAt(index)) return false;
        return true;
    }

    private Map<String, String> validateAnswers(List<EngChallengeAnswerDto> submitted, List<QuestionDefinition> definitions)
    {
        Set<String> allowed = definitions.stream().map(QuestionDefinition::id).collect(Collectors.toSet());
        Map<String, String> answers = new HashMap<>();
        for (EngChallengeAnswerDto answer : submitted)
        {
            if (answer == null || !allowed.contains(answer.getQuestionId())) throw new ServiceException("存在不属于当前测试的题目");
            if (StringUtils.isEmpty(answer.getAnswer())) throw new ServiceException("答案不能为空");
            if (answer.getQuestionId().startsWith(PRONUNCIATION_PREFIX) && !ASSESSED.equals(answer.getAnswer()))
                throw new ServiceException("跟读题答案标记无效");
            if (answers.put(answer.getQuestionId(), answer.getAnswer()) != null) throw new ServiceException("题目不能重复提交");
        }
        if (answers.size() != definitions.size()) throw new ServiceException("请完成全部题目后再提交");
        return answers;
    }

    private EngChallengeResultVo calculateResult(EngChallengeSubmitDto request, String mode,
            List<QuestionDefinition> definitions, Map<String, String> answers,
            Map<Long, EngPronunciationAssessmentVo> pronunciation)
    {
        List<EngChallengeResultVo.ResultItem> items = new ArrayList<>(); int correct = 0;
        int knowledgeTotal = 0;
        for (QuestionDefinition definition : definitions)
        {
            EngChallengeResultVo.ResultItem item;
            if (isPronunciation(definition))
            {
                item = pronunciationResultItem(definition, pronunciation.get(definition.word().getId()));
            }
            else
            {
                knowledgeTotal++;
                item = resultItem(definition, answers.get(definition.id()));
                if (Boolean.TRUE.equals(item.getCorrect())) correct++;
            }
            items.add(item);
        }
        int pronunciationPassed = (int) pronunciation.values().stream()
                .filter(item -> Boolean.TRUE.equals(item.getPassed())).count();
        int pronunciationTotal = pronunciation.size();
        int score = pronunciationTotal == 0
                ? (knowledgeTotal == 0 ? 0 : (int) Math.round(correct * 100D / knowledgeTotal))
                : (int) Math.round(correct * 90D / knowledgeTotal + pronunciationPassed * 10D / pronunciationTotal);
        EngChallengeResultVo result = new EngChallengeResultVo();
        result.setAttemptId(request.getAttemptId()); result.setMode(mode); result.setArticleId(request.getArticleId());
        result.setLevelNo(request.getLevelNo()); result.setScore(score); result.setCorrectCount(correct);
        result.setTotalCount(knowledgeTotal); result.setPronunciationPassedCount(pronunciationPassed);
        result.setPronunciationTotalCount(pronunciationTotal);
        result.setPronunciationAverageScore(pronunciationTotal == 0 ? null : (int) Math.round(
                pronunciation.values().stream().mapToInt(EngPronunciationAssessmentVo::getScore).average().orElse(0D)));
        result.setStars(SPELLING.equals(mode)
                ? EngWordStarCalculator.spellingStars(correct, knowledgeTotal)
                : EngWordStarCalculator.levelStars(score));
        result.setPassed(result.getStars() >= 1); result.setResults(items); return result;
    }

    private Map<Long, WordScore> wordScores(List<QuestionDefinition> definitions,
            List<EngChallengeResultVo.ResultItem> items, String mode, int levelStars,
            Long articleId, Long userId, Map<Long, EngPronunciationAssessmentVo> pronunciation)
    {
        Map<String, Boolean> correctness = items.stream().collect(Collectors.toMap(
                EngChallengeResultVo.ResultItem::getQuestionId, EngChallengeResultVo.ResultItem::getCorrect));
        Map<Long, int[]> counts = new LinkedHashMap<>();
        for (QuestionDefinition definition : definitions)
        {
            if (isPronunciation(definition)) continue;
            int[] count = counts.computeIfAbsent(definition.word().getId(), ignored -> new int[2]); count[1]++;
            if (Boolean.TRUE.equals(correctness.get(definition.id()))) count[0]++;
        }
        Map<Long, WordScore> scores = new LinkedHashMap<>();
        for (var entry : counts.entrySet())
        {
            int correct = entry.getValue()[0], total = entry.getValue()[1];
            EngPronunciationAssessmentVo assessment = pronunciation.get(entry.getKey());
            boolean pronunciationPassed = assessment == null || Boolean.TRUE.equals(assessment.getPassed());
            int stars = NEW.equals(mode) || SPELLING.equals(mode)
                    ? levelStars : EngWordStarCalculator.reviewStars(correct, total);
            if (REVIEW.equals(mode) && assessment != null && !pronunciationPassed) stars = Math.min(stars, 2);
            Long sourceArticleId = articleId;
            if (!NEW.equals(mode))
            {
                EngUserWordProgress progress = wordProgressMapper.selectByUserAndWord(userId, entry.getKey());
                sourceArticleId = progress == null ? null : progress.getFirstArticleId();
            }
            scores.put(entry.getKey(), new WordScore(correct, total, correct == total && pronunciationPassed,
                    stars, sourceArticleId, assessment == null ? null : assessment.getScore(),
                    assessment == null ? null : pronunciationPassed));
        }
        return scores;
    }

    private EngUserWordProgress lockProgress(Long userId, String username, Long wordId, Long articleId, Integer levelNo)
    {
        EngUserWordProgress initial = new EngUserWordProgress(); initial.setUserId(userId); initial.setWordId(wordId);
        initial.setFirstArticleId(articleId); initial.setFirstLevelNo(levelNo); initial.setCreateBy(username);
        initial.setUpdateBy(username); wordProgressMapper.ensureProgress(initial);
        return wordProgressMapper.selectForUpdate(userId, wordId);
    }

    private void updateWrongWord(Long userId, String username, EngWordVo word, WordScore score)
    {
        if (score.sourceArticleId() == null) return;
        EngWrongWord condition = new EngWrongWord(); condition.setUserId(userId);
        condition.setArticleId(score.sourceArticleId()); condition.setWordId(word.getId());
        if (score.allCorrect())
        {
            EngWrongWord existing = wrongWordMapper.selectByUserArticleWord(condition);
            if (existing != null && value(existing.getMastered()) == 0)
                wrongWordMapper.markMasteredByUserArticleWord(userId, score.sourceArticleId(), word.getId(), username);
        }
        else
        {
            condition.setCreateBy(username); condition.setUpdateBy(username); wrongWordMapper.upsertWrongWord(condition);
        }
    }

    private EngStudyRecordWord detail(Long recordId, EngWordVo word, WordScore score, String mode,
            long milestoneCoin, long reviewCoin)
    {
        EngStudyRecordWord item = new EngStudyRecordWord(); item.setStudyRecordId(recordId); item.setWordId(word.getId());
        item.setSourceArticleId(score.sourceArticleId()); item.setStudyMode(mode); item.setCorrectCount(score.correct());
        item.setTotalCount(score.total()); item.setAllCorrect(score.allCorrect() ? 1 : 0); item.setStars(score.stars());
        item.setPronunciationScore(score.pronunciationScore());
        item.setPronunciationPassed(score.pronunciationPassed() == null ? null : (score.pronunciationPassed() ? 1 : 0));
        item.setMilestoneCoin(milestoneCoin); item.setReviewCoin(reviewCoin); return item;
    }

    private List<EngChallengeWordResultVo> buildWordResults(List<EngWordVo> words, Map<Long, WordScore> scores,
            Long userId, List<EngStudyRecordWord> details)
    {
        Map<Long, EngStudyRecordWord> detailMap = details.stream().collect(Collectors.toMap(EngStudyRecordWord::getWordId, item -> item));
        List<EngChallengeWordResultVo> result = new ArrayList<>(); Date now = new Date();
        for (EngWordVo word : words)
        {
            WordScore score = scores.get(word.getId()); EngStudyRecordWord detail = detailMap.get(word.getId());
            EngUserWordProgress progress = wordProgressMapper.selectByUserAndWord(userId, word.getId());
            EngChallengeWordResultVo item = new EngChallengeWordResultVo(); item.setWordId(word.getId());
            item.setWordName(word.getWordName()); item.setSourceArticleId(score.sourceArticleId());
            item.setCorrectCount(score.correct()); item.setTotalCount(score.total()); item.setAllCorrect(score.allCorrect());
            item.setPronunciationScore(score.pronunciationScore()); item.setPronunciationPassed(score.pronunciationPassed());
            item.setStars(score.stars()); item.setHighestStars(value(progress.getHighestStars()));
            item.setCurrentStars(EngWordStarCalculator.currentStars(progress.getLatestStars(), progress.getLatestTestTime(), now));
            item.setMilestoneCoin(detail.getMilestoneCoin()); item.setReviewCoin(detail.getReviewCoin()); result.add(item);
        }
        return result;
    }

    private EngChallengeResultVo existingResult(EngStudyRecord record)
    {
        if (record == null) throw new ServiceException("重复提交结果读取失败");
        EngChallengeResultVo result = new EngChallengeResultVo(); result.setAttemptId(record.getAttemptId());
        result.setMode(record.getStudyMode()); result.setArticleId(record.getArticleId()); result.setLevelNo(record.getLevelNo());
        result.setScore(record.getScore()); result.setCorrectCount(record.getCorrectCount()); result.setTotalCount(record.getTotalCount());
        result.setPassed(value(record.getPassed()) == 1); result.setStars(value(record.getStars()));
        result.setMilestoneCoin(longValue(record.getMilestoneCoin())); result.setReviewCoin(longValue(record.getReviewCoin()));
        result.setCoinReward(longValue(record.getCoinReward())); result.setCoinBalance(coinWalletMapper.selectCoinBalance(record.getUserId()));
        applyNextLevel(result, record.getArticleId(), record.getLevelNo(), value(record.getStars()));
        List<EngChallengeWordResultVo> wordResults = new ArrayList<>();
        Date now = new Date();
        for (EngStudyRecordWord detail : recordWordMapper.selectByRecordAndUser(record.getId(), record.getUserId()))
        {
            EngUserWordProgress progress = wordProgressMapper.selectByUserAndWord(record.getUserId(), detail.getWordId());
            EngChallengeWordResultVo item = new EngChallengeWordResultVo(); item.setWordId(detail.getWordId());
            item.setWordName(detail.getWordName()); item.setSourceArticleId(detail.getSourceArticleId());
            item.setCorrectCount(detail.getCorrectCount()); item.setTotalCount(detail.getTotalCount());
            item.setAllCorrect(value(detail.getAllCorrect()) == 1); item.setStars(detail.getStars());
            item.setPronunciationScore(detail.getPronunciationScore());
            item.setPronunciationPassed(detail.getPronunciationPassed() == null ? null : detail.getPronunciationPassed() == 1);
            item.setHighestStars(progress == null ? 0 : value(progress.getHighestStars()));
            item.setCurrentStars(progress == null ? 0 : EngWordStarCalculator.currentStars(
                    progress.getLatestStars(), progress.getLatestTestTime(), now));
            item.setMilestoneCoin(detail.getMilestoneCoin()); item.setReviewCoin(detail.getReviewCoin());
            wordResults.add(item);
        }
        List<Integer> pronunciationScores = wordResults.stream().map(EngChallengeWordResultVo::getPronunciationScore)
                .filter(Objects::nonNull).toList();
        result.setPronunciationTotalCount(pronunciationScores.size());
        result.setPronunciationPassedCount((int) wordResults.stream()
                .filter(item -> Boolean.TRUE.equals(item.getPronunciationPassed())).count());
        result.setPronunciationAverageScore(pronunciationScores.isEmpty() ? null : (int) Math.round(
                pronunciationScores.stream().mapToInt(Integer::intValue).average().orElse(0D)));
        result.setWordResults(wordResults);
        return result;
    }

    /** 通关后返回下一可测试关；自动掌握关也属于可测试关卡。 */
    private void applyNextLevel(EngChallengeResultVo result, Long articleId, Integer currentLevelNo, int stars)
    {
        result.setNextLevelUnlocked(false);
        result.setNextLevelNo(null);
        if (!NEW.equals(result.getMode()) || stars < 1 || articleId == null || currentLevelNo == null) return;
        for (EngArticleLevelVo level : getArticleLevels(articleId).getLevels())
        {
            if (level.getLevelNo() <= currentLevelNo || !Boolean.TRUE.equals(level.getUnlocked())) continue;
            if (!newWordsForLevel(articleId, level.getLevelNo(), false).isEmpty())
            {
                result.setNextLevelNo(level.getLevelNo());
                result.setNextLevelUnlocked(true);
                return;
            }
        }
    }

    private EngStudyRecord recordHeader(Long userId, String username, EngChallengeSubmitDto request, String mode,
            EngChallengeResultVo result)
    {
        EngStudyRecord record = new EngStudyRecord(); record.setUserId(userId);
        record.setArticleId(NEW.equals(mode) ? request.getArticleId() : null); record.setAttemptId(request.getAttemptId());
        record.setStudyMode(mode); record.setLevelNo(request.getLevelNo()); record.setScore(result.getScore());
        record.setCorrectCount(result.getCorrectCount()); record.setTotalCount(result.getTotalCount());
        record.setPassed(Boolean.TRUE.equals(result.getPassed()) ? 1 : 0); record.setStars(result.getStars());
        record.setMilestoneCoin(0L); record.setReviewCoin(0L); record.setCoinReward(0L); record.setCreateBy(username); return record;
    }

    private EngStudyRecord findAttempt(Long userId, String attemptId)
    {
        EngStudyRecord condition = new EngStudyRecord(); condition.setUserId(userId); condition.setAttemptId(attemptId);
        return recordMapper.selectByUserAndAttempt(condition);
    }

    /** 重复提交冲突后使用当前读，避免 RR 快照看不到并发已提交记录。 */
    private EngStudyRecord findAttemptForUpdate(Long userId, String attemptId)
    {
        EngStudyRecord condition = new EngStudyRecord(); condition.setUserId(userId); condition.setAttemptId(attemptId);
        return recordMapper.selectByUserAndAttemptForUpdate(condition);
    }

    private EngArticleLevelProgress levelProgress(Long userId, Long articleId, Integer levelNo, String username)
    {
        EngArticleLevelProgress progress = new EngArticleLevelProgress(); progress.setUserId(userId);
        progress.setArticleId(articleId); progress.setLevelNo(levelNo); progress.setBestScore(0);
        progress.setHighestStars(0); progress.setCompletedByKnownWords(0); progress.setCreateBy(username);
        progress.setUpdateBy(username); return progress;
    }

    private void validateSubmit(EngChallengeSubmitDto request)
    {
        if (request == null || StringUtils.isEmpty(request.getAttemptId()) || request.getAnswers() == null
                || request.getAnswers().isEmpty()) throw new ServiceException("测试标识和答案不能为空");
    }

    private String requireMode(String mode)
    {
        String value = mode == null ? "" : mode.trim().toUpperCase(Locale.ROOT);
        if (!NEW.equals(value) && !REVIEW.equals(value) && !SPELLING.equals(value))
            throw new ServiceException("学习模式必须为 NEW、REVIEW 或 SPELLING");
        return value;
    }

    private EngArticle requireArticle(Long articleId)
    {
        if (articleId == null) throw new ServiceException("文章主键不能为空");
        EngArticle article = articleService.selectEngArticleById(articleId);
        if (article == null) throw new ServiceException("文章不存在或无权操作"); return article;
    }

    private List<String> options(String correct, List<EngWordVo> words, boolean names, Long excluded)
    {
        List<String> result = new ArrayList<>(); result.add(correct);
        for (EngWordVo word : words)
        {
            if (result.size() >= 4) break; if (word.getId().equals(excluded)) continue;
            String item = names ? word.getWordName() : meaning(word); if (!result.contains(item)) result.add(item);
        }
        Collections.sort(result); return result;
    }

    private String meaning(EngWordVo word) { return StringUtils.isNotEmpty(word.getExchange()) ? word.getExchange() : word.getAcceptation(); }
    private String sentencePrompt(String instruction, String sentence, String translation)
    { return instruction + "：“" + sentence + "”" + (StringUtils.isEmpty(translation) ? "" : "；中文提示：" + translation); }
    private Pattern wholeWord(String word) { return Pattern.compile("(?<![A-Za-z])" + Pattern.quote(word) + "(?![A-Za-z])", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE); }
    private EngChallengeQuestionVo questionVo(QuestionDefinition item)
    { EngChallengeQuestionVo vo = new EngChallengeQuestionVo(); vo.setQuestionId(item.id()); vo.setType(item.type()); vo.setPrompt(item.prompt()); vo.setOptions(item.options()); vo.setAudioUrl(item.audio()); vo.setAnswerLength(item.answerLength()); vo.setWord(isPronunciation(item) ? item.word().getWordName() : null); return vo; }
    private EngChallengeResultVo.ResultItem resultItem(QuestionDefinition definition, String answer)
    { var item = new EngChallengeResultVo.ResultItem(); item.setQuestionId(definition.id()); item.setCorrect(normalize(definition.answer()).equals(normalize(answer))); item.setCorrectAnswer(definition.answer()); return item; }
    private EngChallengeResultVo.ResultItem pronunciationResultItem(QuestionDefinition definition,
            EngPronunciationAssessmentVo assessment)
    { var item = new EngChallengeResultVo.ResultItem(); item.setQuestionId(definition.id()); item.setCorrect(Boolean.TRUE.equals(assessment.getPassed())); item.setCorrectAnswer(ASSESSED); return item; }
    private boolean isPronunciation(QuestionDefinition definition)
    { return definition != null && "PRONUNCIATION".equals(definition.type()); }
    private QuestionDefinition findDefinition(List<QuestionDefinition> definitions, String id)
    { return definitions.stream().filter(item -> item.id().equals(id)).findFirst().orElse(null); }
    private Long extractWordId(String id)
    { if (id == null) return null; for (String prefix : PREFIXES) if (id.startsWith(prefix)) try { return Long.valueOf(id.substring(prefix.length())); } catch (NumberFormatException ignored) { return null; } return null; }
    private String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
    private boolean validWord(EngWordVo word) { return word != null && word.getId() != null && StringUtils.isNotEmpty(word.getWordName()) && StringUtils.isNotEmpty(word.getAcceptation()); }
    private boolean isSpellingEligible(String wordName)
    { return wordName != null && wordName.length() >= FILL_LENGTH && ASCII_WORD.matcher(wordName).matches(); }
    private int value(Integer value) { return value == null ? 0 : value; }
    private long longValue(Long value) { return value == null ? 0L : value; }

    private record QuestionDefinition(String id, String type, String prompt, String answer, List<String> options,
            String audio, Integer answerLength, EngWordVo word, Long sourceArticleId) {}
    private record SentenceContent(String choiceBlank, String fillBlank, String acceptation, String answer,
            Long sourceArticleId) {}
    private record WordScore(int correct, int total, boolean allCorrect, int stars, Long sourceArticleId,
            Integer pronunciationScore, Boolean pronunciationPassed) {}
}
