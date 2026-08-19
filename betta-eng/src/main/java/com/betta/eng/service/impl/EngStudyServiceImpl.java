package com.betta.eng.service.impl;

import com.betta.common.exception.ServiceException;
import com.betta.common.utils.SecurityUtils;
import com.betta.common.utils.StringUtils;
import com.betta.eng.domain.EngArticle;
import com.betta.eng.domain.EngArticleProgress;
import com.betta.eng.domain.EngSentence;
import com.betta.eng.domain.EngStudyRecord;
import com.betta.eng.domain.EngWord;
import com.betta.eng.domain.EngWrongWord;
import com.betta.eng.domain.dto.EngChallengeAnswerDto;
import com.betta.eng.domain.dto.EngChallengeCheckDto;
import com.betta.eng.domain.dto.EngChallengeSubmitDto;
import com.betta.eng.domain.vo.EngChallengeQuestionVo;
import com.betta.eng.domain.vo.EngChallengeResultVo;
import com.betta.eng.domain.vo.EngChallengeVo;
import com.betta.eng.domain.vo.EngStudySummaryVo;
import com.betta.eng.domain.vo.EngWordVo;
import com.betta.eng.mapper.EngArticleProgressMapper;
import com.betta.eng.mapper.EngStudyRecordMapper;
import com.betta.eng.mapper.EngWrongWordMapper;
import com.betta.eng.service.IEngArticleService;
import com.betta.eng.service.IEngSentenceService;
import com.betta.eng.service.IEngStudyService;
import com.betta.eng.service.IEngWordService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 游戏化学习业务实现，统一负责题目构建、提交校验、计分、进度与错词维护。
 */
@Service
public class EngStudyServiceImpl implements IEngStudyService {
    /** 闯关通过所需的最低百分制分数。 */
    private static final int PASS_SCORE = 60;
    /** 看词选中文题目标识前缀。 */
    private static final String WORD_TO_CN_PREFIX = "WORD_TO_CN:";
    /** 看中文选英文题目标识前缀。 */
    private static final String CN_TO_WORD_PREFIX = "CN_TO_WORD:";
    /** 句子挖空选词题目标识前缀。 */
    private static final String SENTENCE_CHOICE_PREFIX = "SENTENCE_CHOICE:";
    /** 句子挖空填词题目标识前缀。 */
    private static final String SENTENCE_FILL_PREFIX = "SENTENCE_FILL:";
    /** 看词选中文题型。 */
    private static final String WORD_TO_CN_TYPE = "WORD_TO_CN";
    /** 看中文选英文题型。 */
    private static final String CN_TO_WORD_TYPE = "CN_TO_WORD";
    /** 句子挖空选词题型。 */
    private static final String SENTENCE_CHOICE_TYPE = "SENTENCE_CHOICE";
    /** 句子挖空填词题型。 */
    private static final String SENTENCE_FILL_TYPE = "SENTENCE_FILL";
    /** 填词输入框仅支持逐个 ASCII 字母，因此填词题答案必须是纯英文字母。 */
    private static final Pattern SENTENCE_FILL_WORD_PATTERN = Pattern.compile("[A-Za-z]+");
    private final IEngArticleService articleService;
    private final IEngSentenceService sentenceService;
    private final IEngWordService wordService;
    private final EngStudyRecordMapper recordMapper;
    private final EngArticleProgressMapper progressMapper;
    private final EngWrongWordMapper wrongWordMapper;

    /**
     * 注入学习业务依赖；参数依次用于文章、句子、单词、记录、进度和错词访问。
     */
    public EngStudyServiceImpl(IEngArticleService articleService, IEngSentenceService sentenceService,
            IEngWordService wordService, EngStudyRecordMapper recordMapper,
            EngArticleProgressMapper progressMapper, EngWrongWordMapper wrongWordMapper) {
        this.articleService = articleService;
        this.sentenceService = sentenceService;
        this.wordService = wordService;
        this.recordMapper = recordMapper;
        this.progressMapper = progressMapper;
        this.wrongWordMapper = wrongWordMapper;
    }

    /** {@inheritDoc} */
    @Override
    public EngStudySummaryVo getSummary() {
        Long userId = SecurityUtils.getUserId();
        EngStudySummaryVo summary = new EngStudySummaryVo();
        summary.setTotalScore(recordMapper.sumScoreByUserId(userId));
        summary.setStudyCount(recordMapper.countByUserId(userId));
        summary.setCompletedArticleCount(progressMapper.countCompletedByUserId(userId));
        summary.setWrongWordCount(wrongWordMapper.countByUserAndMastered(userId, 0));
        summary.setMasteredWrongWordCount(wrongWordMapper.countByUserAndMastered(userId, 1));
        summary.setRecentRecords(recordMapper.selectRecentByUserId(userId));
        return summary;
    }

    /** {@inheritDoc} */
    @Override
    public EngArticleProgress getProgress(Long articleId) {
        requireArticle(articleId);
        EngArticleProgress condition = progressCondition(SecurityUtils.getUserId(), articleId);
        EngArticleProgress progress = progressMapper.selectByUserAndArticle(condition);
        if (progress == null) {
            progress = condition;
            progress.setBestScore(0);
            progress.setBestCorrectCount(0);
            progress.setBestTotalCount(0);
            progress.setCompleted(0);
        }
        // 进度更新时间即最近一次更新最好成绩的时间；最近提交仍保留在学习记录中。
        return progress;
    }

    /** {@inheritDoc} */
    @Override
    public EngChallengeVo getChallenge(Long articleId) {
        EngArticle article = requireArticle(articleId);
        List<QuestionDefinition> definitions = buildDefinitions(articleId);
        EngChallengeVo challenge = new EngChallengeVo();
        challenge.setArticleId(articleId);
        challenge.setTitle(article.getTitle());
        challenge.setProgress(getProgress(articleId));
        List<EngChallengeQuestionVo> questions = new ArrayList<>();
        for (QuestionDefinition definition : definitions) {
            EngChallengeQuestionVo question = new EngChallengeQuestionVo();
            question.setQuestionId(definition.questionId());
            question.setType(definition.type());
            question.setPrompt(definition.prompt());
            question.setOptions(definition.options());
            question.setAudioUrl(definition.audioUrl());
            question.setAnswerLength(definition.answerLength());
            questions.add(question);
        }
        // 每次获取挑战都独立打乱完整题集；仅调整题目位置，不改变题目标识、内容及选择题选项顺序。
        Collections.shuffle(questions);
        challenge.setQuestions(questions);
        return challenge;
    }

    /** {@inheritDoc} */
    @Override
    public EngChallengeResultVo.ResultItem checkChallengeAnswer(EngChallengeCheckDto request) {
        if (request == null || request.getArticleId() == null) {
            throw new ServiceException("文章主键不能为空");
        }
        if (StringUtils.isEmpty(request.getQuestionId()) || request.getQuestionId().trim().isEmpty()) {
            throw new ServiceException("题目标识不能为空");
        }
        if (request.getAnswer() == null || request.getAnswer().trim().isEmpty()) {
            throw new ServiceException("答案不能为空");
        }
        // 先确认文章存在，再使用文章数据重建标准题目，避免接受其他文章的题目标识。
        requireArticle(request.getArticleId());
        List<QuestionDefinition> definitions = buildDefinitions(request.getArticleId());
        QuestionDefinition definition = findQuestionDefinition(definitions, request.getQuestionId());
        if (definition == null) {
            throw new ServiceException("存在不属于当前文章的题目");
        }
        // 即时判题仅组装判定结果，不调用学习记录、进度或错词写入逻辑。
        return buildResultItem(definition, request.getAnswer());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public EngChallengeResultVo submitChallenge(EngChallengeSubmitDto request) {
        if (request == null || request.getArticleId() == null || request.getAnswers() == null
                || request.getAnswers().isEmpty()) {
            throw new ServiceException("文章和答案不能为空");
        }
        requireArticle(request.getArticleId());
        List<QuestionDefinition> definitions = buildDefinitions(request.getArticleId());
        if (definitions.isEmpty()) {
            throw new ServiceException("当前文章暂无可提交的挑战题");
        }
        Map<String, String> answers = validateAnswers(request.getAnswers(), definitions);
        EngChallengeResultVo result = calculateResult(definitions, answers);
        Long userId = SecurityUtils.getUserId();
        String username = SecurityUtils.getUsername();
        saveStudyRecord(userId, username, request.getArticleId(), result);
        saveBestProgress(userId, username, request.getArticleId(), result);
        updateWrongWords(userId, username, request.getArticleId(), definitions, result);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public List<EngWrongWord> selectWrongWordList(EngWrongWord wrongWord) {
        EngWrongWord condition = wrongWord == null ? new EngWrongWord() : wrongWord;
        condition.setUserId(SecurityUtils.getUserId());
        return wrongWordMapper.selectEngWrongWordList(condition);
    }

    /** {@inheritDoc} */
    @Override
    public int markWrongWordMastered(Long id) {
        if (id == null) {
            throw new ServiceException("错词主键不能为空");
        }
        int rows = wrongWordMapper.markMastered(id, SecurityUtils.getUserId(), SecurityUtils.getUsername());
        if (rows == 0) {
            throw new ServiceException("错词不存在或无权操作");
        }
        return rows;
    }

    /**
     * 根据文章已有单词和句子重建服务端标准题目；articleId 为文章主键，返回题目定义。
     */
    private List<QuestionDefinition> buildDefinitions(Long articleId) {
        List<QuestionDefinition> definitions = new ArrayList<>();
        List<EngWordVo> words = wordService.selectWordListByArticle(articleId);
        Map<Long, EngWordVo> validWordMap = new LinkedHashMap<>();
        for (EngWordVo word : words) {
            if (isValidChallengeWord(word)) {
                // 文章关系查询可能返回重复单词，按首次出现顺序去重以保证题目标识唯一且可完整提交。
                validWordMap.putIfAbsent(word.getId(), word);
            }
        }
        List<EngWordVo> validWords = new ArrayList<>(validWordMap.values());
        EngSentence condition = new EngSentence();
        condition.setArticleId(articleId);
        List<EngSentence> sentences = sentenceService.selectEngSentenceList(condition);
        for (EngWordVo word : validWords) {
            // 每个有效单词固定生成两道双向选择题，候选不足四项时返回实际唯一候选数。
            definitions.add(buildWordToCnDefinition(word, validWords));
            definitions.add(buildCnToWordDefinition(word, validWords));
            // 两类句子题共用首个合格句子的挖空内容，并固定按选词、逐字填词顺序追加。
            SentenceQuestionContent sentenceContent = findSentenceQuestionContent(word, sentences);
            if (sentenceContent != null) {
                definitions.add(buildSentenceChoiceDefinition(word, validWords, sentenceContent));
                if (isSentenceFillWord(word.getWordName())) {
                    definitions.add(buildSentenceFillDefinition(word, sentenceContent));
                }
            }
        }
        return definitions;
    }

    /**
     * 判断单词是否具备生成闯关题所需的主键、英文文本和中文释义；word 为候选单词，返回是否有效。
     */
    private boolean isValidChallengeWord(EngWordVo word) {
        return word != null && word.getId() != null && StringUtils.isNotEmpty(word.getWordName())
                && StringUtils.isNotEmpty(word.getAcceptation());
    }

    /**
     * 构建看词选中文题；word 为目标单词，validWords 为文章全部有效单词，返回服务端标准题目定义。
     */
    private QuestionDefinition buildWordToCnDefinition(EngWordVo word, List<EngWordVo> validWords) {
        List<String> options = buildOptions(word.getAcceptation(), validWords, false);
        return new QuestionDefinition(WORD_TO_CN_PREFIX + word.getId(), WORD_TO_CN_TYPE,
                "请选择单词 “" + word.getWordName() + "” 的正确释义", word.getAcceptation(), options, null, null,
                word);
    }

    /**
     * 构建看中文选英文题；word 为目标单词，validWords 为文章全部有效单词，返回包含发音地址的标准题目定义。
     */
    private QuestionDefinition buildCnToWordDefinition(EngWordVo word, List<EngWordVo> validWords) {
        List<String> options = buildOptions(word.getWordName(), validWords, true);
        return new QuestionDefinition(CN_TO_WORD_PREFIX + word.getId(), CN_TO_WORD_TYPE,
                "请选择释义 “" + word.getAcceptation() + "” 对应的英文单词", word.getWordName(), options,
                word.getPhMp3(), null, word);
    }

    /**
     * 构建唯一且稳定排序的选择题选项；correctAnswer 为正确答案，validWords 为候选单词，
     * useWordName 表示候选值是否取英文单词，返回最多四个选项。
     */
    private List<String> buildOptions(String correctAnswer, List<EngWordVo> validWords, boolean useWordName) {
        List<String> options = new ArrayList<>();
        options.add(correctAnswer);
        for (EngWordVo candidateWord : validWords) {
            if (options.size() >= 4) {
                break;
            }
            String candidate = useWordName ? candidateWord.getWordName() : candidateWord.getAcceptation();
            if (!options.contains(candidate)) {
                options.add(candidate);
            }
        }
        // 使用稳定排序，确保获取题目与提交答案时服务端能够重建相同选项。
        Collections.sort(options);
        return options;
    }

    /**
     * 构建句子挖空选词题；word 为目标单词、validWords 为英文选项来源、sentenceContent 为共用句子内容，
     * 返回音频和答案长度均为空的标准题目定义。
     */
    private QuestionDefinition buildSentenceChoiceDefinition(EngWordVo word, List<EngWordVo> validWords,
            SentenceQuestionContent sentenceContent) {
        List<String> options = buildOptions(word.getWordName(), validWords, true);
        String prompt = buildSentencePrompt("请选择句子中的空缺单词", sentenceContent);
        return new QuestionDefinition(SENTENCE_CHOICE_PREFIX + word.getId(), SENTENCE_CHOICE_TYPE, prompt,
                word.getWordName(), options, null, null, word);
    }

    /**
     * 构建句子挖空逐字填词题；word 为纯 ASCII 字母单词、sentenceContent 为共用句子内容，返回标准题目定义。
     */
    private QuestionDefinition buildSentenceFillDefinition(EngWordVo word,
            SentenceQuestionContent sentenceContent) {
        String prompt = buildSentencePrompt("请填写句子中的空缺单词", sentenceContent);
        return new QuestionDefinition(SENTENCE_FILL_PREFIX + word.getId(), SENTENCE_FILL_TYPE, prompt,
                word.getWordName(), Collections.emptyList(), word.getPhMp3(), word.getWordName().length(), word);
    }

    /**
     * 查找目标单词在文章中的首个合格句子并生成共用挖空内容；word 为目标单词、sentences 为文章句子，
     * 没有非空、非 [NT] 且完整匹配的句子时返回 null。
     */
    private SentenceQuestionContent findSentenceQuestionContent(EngWordVo word, List<EngSentence> sentences) {
        if (sentences == null || sentences.isEmpty()) {
            return null;
        }
        Pattern wordPattern = buildWholeWordPattern(word.getWordName());
        for (EngSentence sentence : sentences) {
            if (sentence == null || StringUtils.isEmpty(sentence.getContent())
                    || sentence.getContent().startsWith("[NT]")) {
                continue;
            }
            Matcher matcher = wordPattern.matcher(sentence.getContent());
            if (!matcher.find()) {
                continue;
            }
            // 按答案字符数生成挖空短线；纯字母填词题中该数量同时等于逐字母输入框数量。
            String blankSentence = matcher.replaceFirst(buildSentenceBlank(word.getWordName()));
            return new SentenceQuestionContent(blankSentence, sentence.getAcceptation());
        }
        return null;
    }

    /**
     * 组装句子题统一提示文本；instruction 为题型操作说明、content 为挖空句子及中文释义，返回完整题干。
     */
    private String buildSentencePrompt(String instruction, SentenceQuestionContent content) {
        String prompt = instruction + "：“" + content.blankSentence() + "”";
        if (StringUtils.isNotEmpty(content.acceptation())) {
            prompt += "；中文提示：" + content.acceptation();
        }
        return prompt;
    }

    /**
     * 判断单词能否由逐字母输入框完整填写；wordName 为英文答案，返回是否仅包含 ASCII 字母。
     */
    private boolean isSentenceFillWord(String wordName) {
        return SENTENCE_FILL_WORD_PATTERN.matcher(wordName).matches();
    }

    /**
     * 构建忽略大小写且不匹配英文单词子串的正则；wordName 为目标英文单词，返回可复用匹配器模式。
     */
    private Pattern buildWholeWordPattern(String wordName) {
        String expression = "(?<![A-Za-z])" + Pattern.quote(wordName) + "(?![A-Za-z])";
        return Pattern.compile(expression, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    /**
     * 按目标词条字符数构建句子空缺短线；wordName 为正确英文词条，返回等长下划线文本。
     */
    private String buildSentenceBlank(String wordName) {
        return "_".repeat(wordName.length());
    }

    /**
     * 按题目标识查找文章内标准题目；definitions 为文章题目，questionId 为待查标识，未找到时返回 null。
     */
    private QuestionDefinition findQuestionDefinition(List<QuestionDefinition> definitions, String questionId) {
        for (QuestionDefinition definition : definitions) {
            if (definition.questionId().equals(questionId)) {
                return definition;
            }
        }
        return null;
    }

    /**
     * 校验提交题目标识完整且属于文章；submitted 为答案列表，definitions 为标准题目，返回答案映射。
     */
    private Map<String, String> validateAnswers(List<EngChallengeAnswerDto> submitted,
            List<QuestionDefinition> definitions) {
        Set<String> allowed = new HashSet<>();
        for (QuestionDefinition definition : definitions) {
            allowed.add(definition.questionId());
        }
        Map<String, String> answers = new HashMap<>();
        for (EngChallengeAnswerDto answer : submitted) {
            if (answer == null || StringUtils.isEmpty(answer.getQuestionId()) || !allowed.contains(answer.getQuestionId())) {
                throw new ServiceException("存在不属于当前文章的题目");
            }
            if (answer.getAnswer() == null || answer.getAnswer().trim().isEmpty()) {
                throw new ServiceException("答案不能为空");
            }
            if (answers.put(answer.getQuestionId(), answer.getAnswer()) != null) {
                throw new ServiceException("题目不能重复提交");
            }
        }
        if (answers.size() != definitions.size()) {
            throw new ServiceException("请完成全部题目后再提交");
        }
        return answers;
    }

    /**
     * 对答案执行忽略首尾空白和大小写的比较；definitions 为标准题目、answers 为用户答案，返回计分结果。
     */
    private EngChallengeResultVo calculateResult(List<QuestionDefinition> definitions, Map<String, String> answers) {
        int correctCount = 0;
        List<EngChallengeResultVo.ResultItem> items = new ArrayList<>();
        for (QuestionDefinition definition : definitions) {
            // 最终提交复用即时判题的单题结果组装，保证两种入口的比较规则完全一致。
            EngChallengeResultVo.ResultItem item = buildResultItem(definition, answers.get(definition.questionId()));
            if (Boolean.TRUE.equals(item.getCorrect())) {
                correctCount++;
            }
            items.add(item);
        }
        int score = correctCount * 100 / definitions.size();
        EngChallengeResultVo result = new EngChallengeResultVo();
        result.setScore(score);
        result.setCorrectCount(correctCount);
        result.setTotalCount(definitions.size());
        result.setPassed(score >= PASS_SCORE);
        result.setResults(items);
        return result;
    }

    /**
     * 使用统一规则生成单题判定结果；definition 为标准题目，answer 为用户答案，返回正确状态和正确答案。
     */
    private EngChallengeResultVo.ResultItem buildResultItem(QuestionDefinition definition, String answer) {
        boolean correct = normalizeAnswer(definition.correctAnswer()).equals(normalizeAnswer(answer));
        EngChallengeResultVo.ResultItem item = new EngChallengeResultVo.ResultItem();
        item.setQuestionId(definition.questionId());
        item.setCorrect(correct);
        item.setCorrectAnswer(definition.correctAnswer());
        return item;
    }

    /** 将 answer 规范化用于答案比较并返回。 */
    private String normalizeAnswer(String answer) {
        return answer == null ? "" : answer.trim().toLowerCase(Locale.ROOT);
    }

    /** 保存本次学习记录；参数包含用户、文章和计分结果。 */
    private void saveStudyRecord(Long userId, String username, Long articleId, EngChallengeResultVo result) {
        EngStudyRecord record = new EngStudyRecord();
        record.setUserId(userId);
        record.setArticleId(articleId);
        record.setScore(result.getScore());
        record.setCorrectCount(result.getCorrectCount());
        record.setTotalCount(result.getTotalCount());
        record.setPassed(Boolean.TRUE.equals(result.getPassed()) ? 1 : 0);
        record.setCreateBy(username);
        recordMapper.insertEngStudyRecord(record);
    }

    /** 仅在首次或更高分时写入最好进度；参数包含用户、文章和计分结果。 */
    private void saveBestProgress(Long userId, String username, Long articleId, EngChallengeResultVo result) {
        EngArticleProgress progress = progressCondition(userId, articleId);
        applyResult(progress, result);
        progress.setCreateBy(username);
        progress.setUpdateBy(username);
        progressMapper.upsertBestProgress(progress);
    }

    /** 将 result 的最好成绩字段复制到 progress。 */
    private void applyResult(EngArticleProgress progress, EngChallengeResultVo result) {
        progress.setBestScore(result.getScore());
        progress.setBestCorrectCount(result.getCorrectCount());
        progress.setBestTotalCount(result.getTotalCount());
        progress.setCompleted(Boolean.TRUE.equals(result.getPassed()) ? 1 : 0);
    }

    /**
     * 对关联单词的多类题维护错词：任一题答错则累计并保持未掌握，同一单词全部答对才标记已掌握。
     */
    private void updateWrongWords(Long userId, String username, Long articleId,
            List<QuestionDefinition> definitions, EngChallengeResultVo result) {
        Map<String, Boolean> resultMap = new HashMap<>();
        for (EngChallengeResultVo.ResultItem item : result.getResults()) {
            resultMap.put(item.getQuestionId(), item.getCorrect());
        }
        Map<Long, EngWord> words = new HashMap<>();
        Set<Long> wrongWordIds = new HashSet<>();
        for (QuestionDefinition definition : definitions) {
            EngWord word = definition.word();
            if (word == null) {
                continue;
            }
            words.put(word.getId(), word);
            if (!Boolean.TRUE.equals(resultMap.get(definition.questionId()))) {
                wrongWordIds.add(word.getId());
            }
        }
        for (EngWord word : words.values()) {
            EngWrongWord condition = new EngWrongWord();
            condition.setUserId(userId);
            condition.setArticleId(articleId);
            condition.setWordId(word.getId());
            if (!wrongWordIds.contains(word.getId())) {
                EngWrongWord existing = wrongWordMapper.selectByUserArticleWord(condition);
                if (existing != null && existing.getMastered() == 0) {
                    // 使用原子状态更新，避免并发答错累计后被旧实体中的错误次数覆盖。
                    wrongWordMapper.markMasteredByUserArticleWord(userId, articleId, word.getId(), username);
                }
                continue;
            }
            condition.setCreateBy(username);
            condition.setUpdateBy(username);
            // 同一轮多种题型答错只累计一次，保持原有按单词维护错误次数的业务口径。
            wrongWordMapper.upsertWrongWord(condition);
        }
    }

    /** 查询并校验 articleId 对应文章存在，返回文章。 */
    private EngArticle requireArticle(Long articleId) {
        if (articleId == null) {
            throw new ServiceException("文章主键不能为空");
        }
        EngArticle article = articleService.selectEngArticleById(articleId);
        if (article == null) {
            throw new ServiceException("文章不存在");
        }
        return article;
    }

    /** 构造用户文章组合查询条件并返回。 */
    private EngArticleProgress progressCondition(Long userId, Long articleId) {
        EngArticleProgress condition = new EngArticleProgress();
        condition.setUserId(userId);
        condition.setArticleId(articleId);
        return condition;
    }

    /**
     * 服务端内部题目定义，包含正确答案与关联单词，绝不由挑战获取接口序列化。
     */
    private record QuestionDefinition(String questionId, String type, String prompt, String correctAnswer,
            List<String> options, String audioUrl, Integer answerLength, EngWord word) {
    }

    /**
     * 两类句子题共用的内部内容，包含挖空英文句子和可选中文释义，不对外序列化。
     */
    private record SentenceQuestionContent(String blankSentence, String acceptation) {
    }
}
