package com.betta.eng.service.impl;

import com.betta.common.exception.ServiceException;
import com.betta.common.utils.SecurityUtils;
import com.betta.common.utils.StringUtils;
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
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 新词关卡与全局复习统一学习服务。 */
@Service
public class EngStudyServiceImpl implements IEngStudyService
{
    private static final String NEW = "NEW";
    private static final String REVIEW = "REVIEW";
    private static final List<String> PREFIXES = List.of("WORD_TO_CN:", "CN_TO_WORD:",
            "SENTENCE_CHOICE:", "SENTENCE_FILL:");
    private static final int REVIEW_WORD_LIMIT = 5;
    private static final int FILL_LENGTH = 4;
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";
    private static final Pattern ASCII_WORD = Pattern.compile("[A-Za-z]+");

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

    /** 注入学习、进度、记录和奖励依赖。 */
    public EngStudyServiceImpl(IEngArticleService articleService, IEngSentenceService sentenceService,
            IEngIcibaSentenceService dictionarySentenceService, IEngWordService wordService,
            EngStudyRecordMapper recordMapper, EngStudyRecordWordMapper recordWordMapper,
            EngCoinWalletMapper coinWalletMapper, EngWrongWordMapper wrongWordMapper,
            EngUserWordProgressMapper wordProgressMapper, EngArticleLevelProgressMapper levelProgressMapper,
            EngArticleWordRelMapper articleWordRelMapper)
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
        map.setCompletedLevels(completed); map.setLevels(levels);
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
        String actualMode = requireMode(mode);
        List<EngWordVo> words = NEW.equals(actualMode) ? newWordsForLevel(articleId, levelNo, true)
                : reviewWords(wordIds);
        if (words.isEmpty()) throw new ServiceException(NEW.equals(actualMode) ? "本关新词已全部掌握" : "暂无可复习单词");
        List<QuestionDefinition> definitions = buildDefinitions(actualMode, articleId, words);
        EngChallengeVo challenge = new EngChallengeVo();
        challenge.setAttemptId(UUID.randomUUID().toString()); challenge.setMode(actualMode);
        challenge.setArticleId(articleId); challenge.setLevelNo(levelNo);
        challenge.setTitle(NEW.equals(actualMode) ? requireArticle(articleId).getTitle() : "单词复习");
        challenge.setWords(words);
        List<EngChallengeQuestionVo> questions = definitions.stream().map(this::questionVo)
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(questions); challenge.setQuestions(questions);
        return challenge;
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
        QuestionDefinition definition = findDefinition(buildDefinitions(mode, request.getArticleId(), words),
                request.getQuestionId());
        if (definition == null) throw new ServiceException("题目不属于当前测试");
        return resultItem(definition, request.getAnswer());
    }

    @Override
    @Transactional
    public EngChallengeResultVo submitChallenge(EngChallengeSubmitDto request)
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
        List<QuestionDefinition> definitions = buildDefinitions(mode, request.getArticleId(), words);
        Map<String, String> answers = validateAnswers(request.getAnswers(), definitions);
        EngChallengeResultVo result = calculateResult(request, mode, definitions, answers);
        EngStudyRecord record = recordHeader(userId, username, request, mode, result);
        if (recordMapper.insertIgnoreEngStudyRecord(record) == 0)
            return existingResult(findAttemptForUpdate(userId, request.getAttemptId()));

        Map<Long, WordScore> scores = wordScores(definitions, result.getResults(), mode, result.getStars(),
                request.getArticleId(), userId);
        List<EngStudyRecordWord> details = new ArrayList<>();
        long milestoneCoin = 0, reviewCoin = 0;
        Date now = new Date();
        for (EngWordVo word : words)
        {
            WordScore score = scores.get(word.getId());
            EngUserWordProgress progress = lockProgress(userId, username, word.getId(), request.getArticleId(), request.getLevelNo());
            int targetHighest = Math.max(value(progress.getHighestStars()), score.stars());
            long wordMilestone = EngWordStarCalculator.milestoneCoin(value(progress.getRewardedStars()), targetHighest);
            long wordReview = REVIEW.equals(mode) && score.allCorrect() ? 1 : 0;
            progress.setLearned(1); progress.setHighestStars(targetHighest); progress.setLatestStars(score.stars());
            progress.setLatestTestTime(now); progress.setRewardedStars(targetHighest); progress.setUpdateBy(username);
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

    private List<EngWordVo> reviewWords(List<Long> requested)
    {
        EngReviewOverviewVo overview = getReviewOverview();
        Map<Long, EngReviewWordVo> byId = overview.getWords().stream()
                .collect(Collectors.toMap(EngReviewWordVo::getWordId, item -> item, (a, b) -> a, LinkedHashMap::new));
        List<Long> ids = requested != null && !requested.isEmpty()
                ? new ArrayList<>(new LinkedHashSet<>(requested))
                : overview.getWords().stream().filter(EngReviewWordVo::getRecommended).limit(REVIEW_WORD_LIMIT)
                        .map(EngReviewWordVo::getWordId).toList();
        if (ids.size() > REVIEW_WORD_LIMIT) throw new ServiceException("每轮最多复习5个单词");
        List<EngWordVo> result = new ArrayList<>();
        for (Long id : ids)
        {
            if (!byId.containsKey(id)) throw new ServiceException("只能复习已学单词");
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
                : reviewWords(new ArrayList<>(ids));
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

    private List<QuestionDefinition> buildDefinitions(String mode, Long articleId, List<EngWordVo> words)
    {
        List<QuestionDefinition> result = new ArrayList<>();
        for (EngWordVo word : words)
        {
            result.add(wordToCn(word, words)); result.add(cnToWord(word, words));
            SentenceContent sentence = sentenceContent(mode, articleId, word);
            if (sentence != null)
            {
                result.add(sentenceChoice(word, words, sentence));
                if (sentence.answer().length() > FILL_LENGTH && ASCII_WORD.matcher(sentence.answer()).matches())
                    result.add(sentenceFill(word, sentence));
            }
        }
        return result;
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

    private QuestionDefinition sentenceFill(EngWordVo word, SentenceContent sentence)
    {
        String answer = sentence.answer().substring(0, FILL_LENGTH).toLowerCase(Locale.ROOT);
        List<String> options = new ArrayList<>(); for (char item : answer.toCharArray()) options.add(String.valueOf(item));
        int start = Math.floorMod(word.getWordName().hashCode(), ALPHABET.length());
        for (int offset = 0; options.size() < 10; offset++)
        {
            String item = String.valueOf(ALPHABET.charAt((start + offset) % ALPHABET.length()));
            if (!answer.contains(item)) options.add(item);
        }
        Collections.shuffle(options, new Random(31L * word.getId() + word.getWordName().hashCode()));
        return definition("SENTENCE_FILL:", "SENTENCE_FILL",
                sentencePrompt("请选择句子中的空缺字母", sentence.fillBlank(), sentence.acceptation()),
                answer, options, word, FILL_LENGTH, sentence.sourceArticleId());
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

    private Map<String, String> validateAnswers(List<EngChallengeAnswerDto> submitted, List<QuestionDefinition> definitions)
    {
        Set<String> allowed = definitions.stream().map(QuestionDefinition::id).collect(Collectors.toSet());
        Map<String, String> answers = new HashMap<>();
        for (EngChallengeAnswerDto answer : submitted)
        {
            if (answer == null || !allowed.contains(answer.getQuestionId())) throw new ServiceException("存在不属于当前测试的题目");
            if (StringUtils.isEmpty(answer.getAnswer())) throw new ServiceException("答案不能为空");
            if (answers.put(answer.getQuestionId(), answer.getAnswer()) != null) throw new ServiceException("题目不能重复提交");
        }
        if (answers.size() != definitions.size()) throw new ServiceException("请完成全部题目后再提交");
        return answers;
    }

    private EngChallengeResultVo calculateResult(EngChallengeSubmitDto request, String mode,
            List<QuestionDefinition> definitions, Map<String, String> answers)
    {
        List<EngChallengeResultVo.ResultItem> items = new ArrayList<>(); int correct = 0;
        for (QuestionDefinition definition : definitions)
        {
            var item = resultItem(definition, answers.get(definition.id()));
            if (Boolean.TRUE.equals(item.getCorrect())) correct++; items.add(item);
        }
        int score = definitions.isEmpty() ? 0 : correct * 100 / definitions.size();
        EngChallengeResultVo result = new EngChallengeResultVo();
        result.setAttemptId(request.getAttemptId()); result.setMode(mode); result.setArticleId(request.getArticleId());
        result.setLevelNo(request.getLevelNo()); result.setScore(score); result.setCorrectCount(correct);
        result.setTotalCount(definitions.size()); result.setStars(EngWordStarCalculator.levelStars(score));
        result.setPassed(result.getStars() >= 1); result.setResults(items); return result;
    }

    private Map<Long, WordScore> wordScores(List<QuestionDefinition> definitions,
            List<EngChallengeResultVo.ResultItem> items, String mode, int levelStars,
            Long articleId, Long userId)
    {
        Map<String, Boolean> correctness = items.stream().collect(Collectors.toMap(
                EngChallengeResultVo.ResultItem::getQuestionId, EngChallengeResultVo.ResultItem::getCorrect));
        Map<Long, int[]> counts = new LinkedHashMap<>();
        for (QuestionDefinition definition : definitions)
        {
            int[] count = counts.computeIfAbsent(definition.word().getId(), ignored -> new int[2]); count[1]++;
            if (Boolean.TRUE.equals(correctness.get(definition.id()))) count[0]++;
        }
        Map<Long, WordScore> scores = new LinkedHashMap<>();
        for (var entry : counts.entrySet())
        {
            int correct = entry.getValue()[0], total = entry.getValue()[1];
            int stars = NEW.equals(mode) ? levelStars : EngWordStarCalculator.reviewStars(correct, total);
            Long sourceArticleId = articleId;
            if (REVIEW.equals(mode))
            {
                EngUserWordProgress progress = wordProgressMapper.selectByUserAndWord(userId, entry.getKey());
                sourceArticleId = progress == null ? null : progress.getFirstArticleId();
            }
            scores.put(entry.getKey(), new WordScore(correct, total, correct == total, stars, sourceArticleId));
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
            item.setHighestStars(progress == null ? 0 : value(progress.getHighestStars()));
            item.setCurrentStars(progress == null ? 0 : EngWordStarCalculator.currentStars(
                    progress.getLatestStars(), progress.getLatestTestTime(), now));
            item.setMilestoneCoin(detail.getMilestoneCoin()); item.setReviewCoin(detail.getReviewCoin());
            wordResults.add(item);
        }
        result.setWordResults(wordResults);
        return result;
    }

    /** 通关后跳过空洞和自动掌握关，只返回真正可练习的下一关。 */
    private void applyNextLevel(EngChallengeResultVo result, Long articleId, Integer currentLevelNo, int stars)
    {
        result.setNextLevelUnlocked(false);
        result.setNextLevelNo(null);
        if (!NEW.equals(result.getMode()) || stars < 1 || articleId == null || currentLevelNo == null) return;
        for (EngArticleLevelVo level : getArticleLevels(articleId).getLevels())
        {
            if (level.getLevelNo() <= currentLevelNo || !Boolean.TRUE.equals(level.getUnlocked())
                    || Boolean.TRUE.equals(level.getMasteredByExistingWords())) continue;
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
        if (!NEW.equals(value) && !REVIEW.equals(value)) throw new ServiceException("学习模式必须为 NEW 或 REVIEW");
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
    { EngChallengeQuestionVo vo = new EngChallengeQuestionVo(); vo.setQuestionId(item.id()); vo.setType(item.type()); vo.setPrompt(item.prompt()); vo.setOptions(item.options()); vo.setAudioUrl(item.audio()); vo.setAnswerLength(item.answerLength()); return vo; }
    private EngChallengeResultVo.ResultItem resultItem(QuestionDefinition definition, String answer)
    { var item = new EngChallengeResultVo.ResultItem(); item.setQuestionId(definition.id()); item.setCorrect(normalize(definition.answer()).equals(normalize(answer))); item.setCorrectAnswer(definition.answer()); return item; }
    private QuestionDefinition findDefinition(List<QuestionDefinition> definitions, String id)
    { return definitions.stream().filter(item -> item.id().equals(id)).findFirst().orElse(null); }
    private Long extractWordId(String id)
    { if (id == null) return null; for (String prefix : PREFIXES) if (id.startsWith(prefix)) try { return Long.valueOf(id.substring(prefix.length())); } catch (NumberFormatException ignored) { return null; } return null; }
    private String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
    private boolean validWord(EngWordVo word) { return word != null && word.getId() != null && StringUtils.isNotEmpty(word.getWordName()) && StringUtils.isNotEmpty(word.getAcceptation()); }
    private int value(Integer value) { return value == null ? 0 : value; }
    private long longValue(Long value) { return value == null ? 0L : value; }

    private record QuestionDefinition(String id, String type, String prompt, String answer, List<String> options,
            String audio, Integer answerLength, EngWordVo word, Long sourceArticleId) {}
    private record SentenceContent(String choiceBlank, String fillBlank, String acceptation, String answer,
            Long sourceArticleId) {}
    private record WordScore(int correct, int total, boolean allCorrect, int stars, Long sourceArticleId) {}
}
