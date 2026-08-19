package com.betta.eng.service.impl;

import com.betta.common.core.domain.entity.SysUser;
import com.betta.common.core.domain.model.LoginUser;
import com.betta.common.exception.ServiceException;
import com.betta.eng.domain.EngArticle;
import com.betta.eng.domain.EngSentence;
import com.betta.eng.domain.EngWrongWord;
import com.betta.eng.domain.dto.EngChallengeAnswerDto;
import com.betta.eng.domain.dto.EngChallengeCheckDto;
import com.betta.eng.domain.vo.EngChallengeVo;
import com.betta.eng.domain.vo.EngChallengeQuestionVo;
import com.betta.eng.domain.vo.EngChallengeResultVo;
import com.betta.eng.domain.vo.EngWordVo;
import com.betta.eng.mapper.EngArticleProgressMapper;
import com.betta.eng.mapper.EngStudyRecordMapper;
import com.betta.eng.mapper.EngWrongWordMapper;
import com.betta.eng.service.IEngArticleService;
import com.betta.eng.service.IEngSentenceService;
import com.betta.eng.service.IEngWordService;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 英语闯关业务的无第三方依赖回归测试，通过 JDK 动态代理和反射验证题目构建、即时判题、最终判分及答案校验。
 */
public class EngStudyServiceImplTest {
    /** 固定用于测试的文章主键。 */
    private static final Long ARTICLE_ID = 10L;

    /**
     * 依次执行全部回归场景；args 为命令行参数且本测试不使用，无异常返回表示全部通过。
     *
     * @param args 命令行参数
     * @throws Exception 反射调用或断言失败时抛出异常
     */
    public static void main(String[] args) throws Exception {
        EngStudyServiceImplTest test = new EngStudyServiceImplTest();
        test.shouldBuildSentenceChoiceAndExistingQuestionTypesWithoutLeakingAnswer();
        test.shouldShuffleChallengeQuestionsWithoutChangingQuestionContent();
        test.shouldCheckSingleAnswerWithoutWritingStudyData();
        test.shouldRejectInvalidSingleAnswerRequest();
        test.shouldScoreAllFourQuestionTypesIgnoringCaseAndOuterWhitespace();
        test.shouldRejectMissingDuplicateAndUnknownAnswers();
        test.shouldUpdateWrongWordOnceWhenAnyQuestionIsWrong();
        test.shouldMarkExistingWrongWordWhenAllQuestionsAreCorrect();
    }

    /**
     * 验证公开挑战接口会在每次获取时独立打乱题序，同时保持题目数量、标识、内容和选项顺序完整不变。
     *
     * @throws Exception 反射读取内部标准题目失败时抛出异常
     */
    private void shouldShuffleChallengeQuestionsWithoutChangingQuestionContent() throws Exception {
        EngStudyServiceImpl service = createService(createWords(), createSentences());
        List<?> definitions = buildDefinitions(service);
        List<String> originalOrder = definitionQuestionIds(definitions);
        Map<String, List<Object>> expectedContents = definitionContents(definitions);
        Set<List<String>> observedOrders = new HashSet<>();
        boolean observedNonOriginalOrder = false;

        setTestLoginUser();
        try {
            // 连续获取多轮可覆盖每次请求重新洗牌，并将随机恰好保持原顺序造成误报的概率降至可忽略。
            for (int round = 0; round < 32; round++) {
                EngChallengeVo challenge = service.getChallenge(ARTICLE_ID);
                List<EngChallengeQuestionVo> questions = challenge.getQuestions();
                List<String> actualOrder = questionIds(questions);

                assertEquals(definitions.size(), questions.size(), "洗牌后题目总数不得变化");
                assertEquals(questions.size(), new HashSet<>(actualOrder).size(), "洗牌后题目标识不得重复");
                assertEquals(expectedContents.keySet(), new HashSet<>(actualOrder), "洗牌后题目不得丢失或新增");
                for (EngChallengeQuestionVo question : questions) {
                    assertEquals(expectedContents.get(question.getQuestionId()), questionContent(question),
                            "洗牌只能改变题目位置，不得改变题目内容或选项顺序");
                }
                observedOrders.add(List.copyOf(actualOrder));
                observedNonOriginalOrder |= !originalOrder.equals(actualOrder);
            }
        } finally {
            // 清理线程认证信息，避免影响后续即时判题、计分和错词回归场景。
            SecurityContextHolder.clearContext();
        }

        assertTrue(observedNonOriginalOrder, "多轮获取挑战时题序不应始终保持原始生成顺序");
        assertTrue(observedOrders.size() > 1, "每次获取挑战应独立洗牌，不应始终返回同一题序");
    }

    /**
     * 验证四类题固定顺序、句子选词选项、完整词挖空、首句选择及不泄露答案规则。
     *
     * @throws Exception 反射调用失败时抛出异常
     */
    private void shouldBuildSentenceChoiceAndExistingQuestionTypesWithoutLeakingAnswer() throws Exception {
        EngStudyServiceImpl service = createService(createWords(), createSentences());
        List<?> definitions = buildDefinitions(service);

        // 四个有效单词各生成两道双向选择题，apple、cat、don't 生成句子选词，纯字母前两者再生成填词。
        assertEquals(13, definitions.size(), "题目总数应包含八道双向选择题、三道句子选词和两道填词题");
        assertEquals("WORD_TO_CN:1", definitionValue(definitions.get(0), "questionId"), "每个单词第一题应看词选中文");
        assertEquals("CN_TO_WORD:1", definitionValue(definitions.get(1), "questionId"), "每个单词第二题应看中文选英文");
        assertEquals("SENTENCE_CHOICE:1", definitionValue(definitions.get(2), "questionId"),
                "每个有句子的单词第三题应为句子选词");
        assertEquals("SENTENCE_FILL:1", definitionValue(definitions.get(3), "questionId"),
                "纯字母单词第四题应为逐字填词");
        assertEquals(1, countDefinitions(definitions, "WORD_TO_CN:1"), "重复单词关系只能生成一道看词选中文题");
        assertEquals(1, countDefinitions(definitions, "CN_TO_WORD:1"), "重复单词关系只能生成一道看中文选英文题");
        assertEquals(1, countDefinitions(definitions, "SENTENCE_CHOICE:1"), "重复单词关系只能生成一道句子选词题");
        assertEquals(1, countDefinitions(definitions, "SENTENCE_FILL:1"), "重复单词关系只能生成一道句子填空题");
        Object wordToCn = findDefinition(definitions, "WORD_TO_CN:1");
        Object cnToWord = findDefinition(definitions, "CN_TO_WORD:1");
        Object appleChoice = findDefinition(definitions, "SENTENCE_CHOICE:1");
        Object appleFill = findDefinition(definitions, "SENTENCE_FILL:1");
        Object catFill = findDefinition(definitions, "SENTENCE_FILL:2");
        assertEquals("WORD_TO_CN", definitionValue(wordToCn, "type"), "看词选中文题型错误");
        assertEquals("CN_TO_WORD", definitionValue(cnToWord, "type"), "看中文选英文题型错误");
        assertEquals("https://audio.example/apple.mp3", definitionValue(cnToWord, "audioUrl"),
                "看中文选英文题应携带目标单词音频");
        assertEquals(null, definitionValue(wordToCn, "audioUrl"), "看词选中文题不应携带音频");
        assertEquals("https://audio.example/apple.mp3", definitionValue(appleFill, "audioUrl"),
                "句子填词题应携带目标单词音频");
        assertEquals(5, definitionValue(appleFill, "answerLength"), "apple 填词题答案长度应为五个字母");
        assertEquals(3, definitionValue(catFill, "answerLength"), "cat 填词题答案长度应为三个字母");
        assertEquals(null, definitionValue(wordToCn, "answerLength"), "选择题不应返回填词答案长度");
        assertEquals(null, definitionValue(appleChoice, "audioUrl"), "句子选词题不应返回发音地址");
        assertEquals(null, definitionValue(appleChoice, "answerLength"), "句子选词题不应返回填词答案长度");
        assertTrue(((List<?>) definitionValue(wordToCn, "options")).contains("苹果"), "中文选项应包含正确释义");
        assertTrue(((List<?>) definitionValue(cnToWord, "options")).contains("apple"), "英文选项应包含正确单词");
        List<?> sentenceOptions = (List<?>) definitionValue(appleChoice, "options");
        assertTrue(sentenceOptions.contains("apple"), "句子选词选项必须包含正确英文单词");
        assertTrue(sentenceOptions.size() <= 4, "句子选词选项不得超过四个");
        assertEquals(sentenceOptions.size(), new HashSet<>(sentenceOptions).size(), "句子选词选项不得重复");

        // 两类句子题都应跳过 [NT] 和 Pineapple 子串，并共用随后首个完整匹配句子及中文提示。
        String appleChoicePrompt = (String) definitionValue(appleChoice, "prompt");
        String applePrompt = (String) definitionValue(appleFill, "prompt");
        assertTrue(appleChoicePrompt.contains("_____ falls from the tree."), "句子选词应挖空首个完整匹配单词");
        assertTrue(appleChoicePrompt.contains("一个苹果从树上掉下来。"), "句子选词应包含中文释义提示");
        assertTrue(!appleChoicePrompt.contains("second"), "句子选词只能采用首个合格句子");
        assertTrue(applePrompt.contains("_____ falls from the tree."), "应忽略大小写挖空完整的 apple 单词");
        assertTrue(applePrompt.contains("一个苹果从树上掉下来。"), "填空题应包含中文释义提示");
        assertTrue(!applePrompt.contains("second"), "每个单词只能采用首个合格句子");
        String catPrompt = (String) definitionValue(catFill, "prompt");
        assertTrue(catPrompt.contains("A ___ sleeps."), "catapult 子串不得命中，且空缺短线数应等于 cat 字母数");
        assertEquals(null, findDefinitionOrNull(definitions, "SENTENCE_FILL:3"),
                "只出现在其他单词子串中的 app 不应生成填空题");
        assertEquals(null, findDefinitionOrNull(definitions, "SENTENCE_CHOICE:3"),
                "只出现在其他单词子串中的 app 不应生成句子选词题");
        assertEquals(1, countDefinitions(definitions, "WORD_TO_CN:3"), "无合格句子的单词仍应生成看词选中文题");
        assertEquals(1, countDefinitions(definitions, "CN_TO_WORD:3"), "无合格句子的单词仍应生成看中文选英文题");
        assertEquals(1, countDefinitions(definitions, "WORD_TO_CN:4"), "含撇号单词仍应生成看词选中文题");
        assertEquals(1, countDefinitions(definitions, "CN_TO_WORD:4"), "含撇号单词仍应生成看中文选英文题");
        Object apostropheChoice = findDefinition(definitions, "SENTENCE_CHOICE:4");
        assertEquals("SENTENCE_CHOICE", definitionValue(apostropheChoice, "type"), "含撇号单词应生成句子选词题");
        assertTrue(((List<?>) definitionValue(apostropheChoice, "options")).contains("don't"),
                "含撇号单词的句子选词选项应包含正确答案");
        assertEquals(null, findDefinitionOrNull(definitions, "SENTENCE_FILL:4"),
                "含撇号单词不能由逐字母输入框完整填写，不应生成填词题");

        // 对外题目 VO 不定义正确答案字段，确保获取挑战接口无法序列化服务端答案。
        boolean exposesCorrectAnswer = false;
        for (java.lang.reflect.Field field : EngChallengeQuestionVo.class.getDeclaredFields()) {
            exposesCorrectAnswer |= "correctAnswer".equals(field.getName());
        }
        assertTrue(!exposesCorrectAnswer, "挑战题展示对象不得包含正确答案字段");
    }

    /**
     * 验证即时判题正确、错误、大小写和首尾空格规则，并确认该入口不写学习记录、进度或错词。
     */
    private void shouldCheckSingleAnswerWithoutWritingStudyData() {
        Map<String, Integer> mapperCalls = new HashMap<>();
        EngStudyServiceImpl service = createService(createWords(), createSentences(), mapperCalls);

        EngChallengeResultVo.ResultItem correct = service.checkChallengeAnswer(
                checkRequest(ARTICLE_ID, "SENTENCE_CHOICE:1", " Apple "));
        assertEquals("SENTENCE_CHOICE:1", correct.getQuestionId(), "即时判题应返回句子选词题目标识");
        assertEquals(Boolean.TRUE, correct.getCorrect(), "句子选词即时判题应忽略英文大小写和首尾空格");
        assertEquals("apple", correct.getCorrectAnswer(), "即时判题应返回正确答案");

        EngChallengeResultVo.ResultItem wrong = service.checkChallengeAnswer(
                checkRequest(ARTICLE_ID, "SENTENCE_CHOICE:1", "cat"));
        assertEquals(Boolean.FALSE, wrong.getCorrect(), "句子选词错误答案应被即时判定为错误");
        assertEquals("apple", wrong.getCorrectAnswer(), "句子选词回答错误时应返回该题正确答案");
        assertEquals(0, mapperCalls.getOrDefault("insertEngStudyRecord", 0), "即时判题不得写入学习记录");
        assertEquals(0, mapperCalls.getOrDefault("upsertBestProgress", 0), "即时判题不得写入文章进度");
        assertEquals(0, mapperCalls.getOrDefault("upsertWrongWord", 0), "即时判题不得写入错词");
    }

    /**
     * 验证即时判题拒绝空答案、空题目标识及不属于指定文章的题目标识。
     */
    private void shouldRejectInvalidSingleAnswerRequest() {
        EngStudyServiceImpl service = createService(createWords(), createSentences());

        assertCheckServiceException(service, checkRequest(null, "SENTENCE_FILL:1", "apple"),
                "空文章主键应被即时判题拒绝");
        assertCheckServiceException(service, checkRequest(ARTICLE_ID, "SENTENCE_FILL:1", " "),
                "空答案应被即时判题拒绝");
        assertCheckServiceException(service, checkRequest(ARTICLE_ID, " ", "apple"),
                "空题目标识应被即时判题拒绝");
        assertCheckServiceException(service, checkRequest(ARTICLE_ID, "WORD_TO_CN:999", "苹果"),
                "不属于指定文章的题目标识应被即时判题拒绝");
    }

    /**
     * 验证四类题均参与判分，新句子选词答错会进入总数和得分，其他答案保持忽略大小写及首尾空格。
     *
     * @throws Exception 反射调用失败时抛出异常
     */
    private void shouldScoreAllFourQuestionTypesIgnoringCaseAndOuterWhitespace() throws Exception {
        EngStudyServiceImpl service = createService(createWords(), createSentences());
        List<?> allDefinitions = buildDefinitions(service);
        List<Object> definitions = List.of(
                findDefinition(allDefinitions, "WORD_TO_CN:1"),
                findDefinition(allDefinitions, "CN_TO_WORD:1"),
                findDefinition(allDefinitions, "SENTENCE_CHOICE:1"),
                findDefinition(allDefinitions, "SENTENCE_FILL:1"));
        Map<String, String> answers = new HashMap<>();
        answers.put("WORD_TO_CN:1", "  苹果  ");
        answers.put("CN_TO_WORD:1", " APPLE ");
        answers.put("SENTENCE_CHOICE:1", "cat");
        answers.put("SENTENCE_FILL:1", " Apple ");

        EngChallengeResultVo result = calculateResult(service, definitions, answers);
        assertEquals(3, result.getCorrectCount(), "四类题中应有三题正确");
        assertEquals(4, result.getTotalCount(), "计分总数应包含新增句子选词题");
        assertEquals(75, result.getScore(), "句子选词答错后四题应得到七十五分");
        assertEquals(Boolean.TRUE, result.getPassed(), "七十五分应达到六十分通关线");
    }

    /**
     * 验证缺失、重复和未知题目标识仍按既有提交规则拒绝。
     *
     * @throws Exception 反射调用失败时抛出异常
     */
    private void shouldRejectMissingDuplicateAndUnknownAnswers() throws Exception {
        EngStudyServiceImpl service = createService(createWords(), createSentences());
        List<?> allDefinitions = buildDefinitions(service);
        List<Object> definitions = List.of(
                findDefinition(allDefinitions, "WORD_TO_CN:1"),
                findDefinition(allDefinitions, "CN_TO_WORD:1"));

        assertServiceException(service, List.of(answer("WORD_TO_CN:1", "苹果")), definitions,
                "缺失题目答案应被拒绝");
        assertServiceException(service,
                List.of(answer("WORD_TO_CN:1", "苹果"), answer("WORD_TO_CN:1", "苹果")), definitions,
                "重复题目答案应被拒绝");
        assertServiceException(service,
                List.of(answer("WORD_TO_CN:1", "苹果"), answer("UNKNOWN:1", "apple")), definitions,
                "未知题目标识应被拒绝");
    }

    /**
     * 验证同一单词四类题存在多题答错时仍只累计一次错词，且不会在同轮标记为已掌握。
     *
     * @throws Exception 反射调用失败时抛出异常
     */
    private void shouldUpdateWrongWordOnceWhenAnyQuestionIsWrong() throws Exception {
        Map<String, Integer> calls = new HashMap<>();
        EngStudyServiceImpl service = createService(createWords(), createSentences(), wrongWordMapper(calls, null));
        List<?> definitions = appleDefinitions(buildDefinitions(service));
        Map<String, String> answers = new HashMap<>();
        answers.put("WORD_TO_CN:1", "错误答案");
        answers.put("CN_TO_WORD:1", "apple");
        answers.put("SENTENCE_CHOICE:1", "错误答案");
        answers.put("SENTENCE_FILL:1", "apple");
        EngChallengeResultVo result = calculateResult(service, definitions, answers);

        updateWrongWords(service, definitions, result);

        assertEquals(1, calls.getOrDefault("upsertWrongWord", 0), "同一单词同轮存在错题时只应累计一次");
        assertEquals(0, calls.getOrDefault("markMasteredByUserArticleWord", 0),
                "同轮存在错题时不得标记为已掌握");
    }

    /**
     * 验证同一单词四类题全部答对且已有未掌握错词时只标记一次掌握，不新增错误次数。
     *
     * @throws Exception 反射调用失败时抛出异常
     */
    private void shouldMarkExistingWrongWordWhenAllQuestionsAreCorrect() throws Exception {
        Map<String, Integer> calls = new HashMap<>();
        EngWrongWord existing = new EngWrongWord();
        existing.setMastered(0);
        EngStudyServiceImpl service = createService(createWords(), createSentences(), wrongWordMapper(calls, existing));
        List<?> definitions = appleDefinitions(buildDefinitions(service));
        Map<String, String> answers = new HashMap<>();
        answers.put("WORD_TO_CN:1", "苹果");
        answers.put("CN_TO_WORD:1", "apple");
        answers.put("SENTENCE_CHOICE:1", "apple");
        answers.put("SENTENCE_FILL:1", "apple");
        EngChallengeResultVo result = calculateResult(service, definitions, answers);

        updateWrongWords(service, definitions, result);

        assertEquals(1, calls.getOrDefault("markMasteredByUserArticleWord", 0),
                "全部答对时已有未掌握错词只应标记一次");
        assertEquals(0, calls.getOrDefault("upsertWrongWord", 0), "全部答对时不得新增错误次数");
    }

    /**
     * 创建包含测试数据服务桩的学习业务；words 为文章单词，sentences 为文章句子，返回待测试服务。
     */
    private EngStudyServiceImpl createService(List<EngWordVo> words, List<EngSentence> sentences) {
        return createService(words, sentences, proxy(EngWrongWordMapper.class, Map.of()));
    }

    /**
     * 创建包含指定错词 Mapper 的学习业务；words 为文章单词、sentences 为文章句子、wrongWordMapper 为错词桩。
     */
    private EngStudyServiceImpl createService(List<EngWordVo> words, List<EngSentence> sentences,
            EngWrongWordMapper wrongWordMapper) {
        IEngWordService wordService = proxy(IEngWordService.class, Map.of("selectWordListByArticle", words));
        IEngSentenceService sentenceService = proxy(IEngSentenceService.class,
                Map.of("selectEngSentenceList", sentences));
        EngArticle article = new EngArticle();
        article.setId(ARTICLE_ID);
        return new EngStudyServiceImpl(proxy(IEngArticleService.class, Map.of("selectEngArticleById", article)),
                sentenceService, wordService,
                proxy(EngStudyRecordMapper.class, Map.of()), proxy(EngArticleProgressMapper.class, Map.of()),
                wrongWordMapper);
    }

    /**
     * 创建可记录数据库 Mapper 调用次数的学习业务；words 为文章单词、sentences 为文章句子、calls 为调用计数。
     */
    private EngStudyServiceImpl createService(List<EngWordVo> words, List<EngSentence> sentences,
            Map<String, Integer> calls) {
        IEngWordService wordService = proxy(IEngWordService.class, Map.of("selectWordListByArticle", words));
        IEngSentenceService sentenceService = proxy(IEngSentenceService.class,
                Map.of("selectEngSentenceList", sentences));
        EngArticle article = new EngArticle();
        article.setId(ARTICLE_ID);
        return new EngStudyServiceImpl(proxy(IEngArticleService.class, Map.of("selectEngArticleById", article)),
                sentenceService, wordService, countingProxy(EngStudyRecordMapper.class, calls),
                countingProxy(EngArticleProgressMapper.class, calls), countingProxy(EngWrongWordMapper.class, calls));
    }

    /**
     * 创建记录全部方法调用次数的接口代理；type 为接口类型、calls 为调用计数，返回代理实例。
     */
    @SuppressWarnings("unchecked")
    private <T> T countingProxy(Class<T> type, Map<String, Integer> calls) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
            calls.merge(method.getName(), 1, Integer::sum);
            return defaultValue(method.getReturnType());
        });
    }

    /**
     * 创建记录错词 Mapper 调用次数的动态代理；calls 为调用计数，existing 为查询时返回的已有错词。
     */
    private EngWrongWordMapper wrongWordMapper(Map<String, Integer> calls, EngWrongWord existing) {
        return (EngWrongWordMapper) Proxy.newProxyInstance(EngWrongWordMapper.class.getClassLoader(),
                new Class<?>[] {EngWrongWordMapper.class}, (proxy, method, args) -> {
                    calls.merge(method.getName(), 1, Integer::sum);
                    if ("selectByUserArticleWord".equals(method.getName())) {
                        return existing;
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    /**
     * 使用固定方法返回值创建接口动态代理；type 为接口类型，results 为方法名到返回值映射，返回代理实例。
     */
    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> type, Map<String, Object> results) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (proxy, method, args) -> results.getOrDefault(method.getName(), defaultValue(method.getReturnType())));
    }

    /**
     * 返回基本类型的动态代理默认值；type 为返回类型，返回对应零值，引用类型返回 null。
     */
    private Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }

    /** 构造文章测试单词并返回，其中无主键单词用于验证无效数据跳过。 */
    private List<EngWordVo> createWords() {
        List<EngWordVo> words = new ArrayList<>();
        words.add(word(1L, "apple", "苹果", "https://audio.example/apple.mp3"));
        words.add(word(2L, "cat", "猫", null));
        words.add(word(3L, "app", "应用程序", null));
        words.add(word(4L, "don't", "不要", null));
        // 模拟文章单词关系查询返回同一个单词的重复行，正式逻辑应按主键保序去重。
        words.add(word(1L, "apple", "苹果", "https://audio.example/apple.mp3"));
        words.add(word(null, "invalid", "无效", null));
        return words;
    }

    /** 构造文章测试句子并返回，覆盖禁用标记、子串、大小写及首句选择。 */
    private List<EngSentence> createSentences() {
        return List.of(
                sentence(1L, "[NT] Apple must be skipped.", "应跳过。"),
                sentence(2L, "Pineapple tastes sweet.", "菠萝很甜。"),
                sentence(3L, "APPLE falls from the tree.", "一个苹果从树上掉下来。"),
                sentence(4L, "apple appears in the second valid sentence.", "第二个有效句子。"),
                sentence(5L, "A catapult moves.", "投石器移动。"),
                sentence(6L, "A CAT sleeps.", "一只猫在睡觉。"),
                sentence(7L, "Don't stop learning.", "不要停止学习。"));
    }

    /** 根据输入字段构造单词；id 为主键、name 为英文、acceptation 为中文、audioUrl 为音频地址。 */
    private EngWordVo word(Long id, String name, String acceptation, String audioUrl) {
        EngWordVo word = new EngWordVo();
        word.setId(id);
        word.setWordName(name);
        word.setAcceptation(acceptation);
        word.setPhMp3(audioUrl);
        return word;
    }

    /** 根据输入字段构造句子；id 为主键、content 为英文内容、acceptation 为中文释义。 */
    private EngSentence sentence(Long id, String content, String acceptation) {
        EngSentence sentence = new EngSentence();
        sentence.setId(id);
        sentence.setArticleId(ARTICLE_ID);
        sentence.setContent(content);
        sentence.setAcceptation(acceptation);
        return sentence;
    }

    /** 调用私有题目构建方法；service 为待测试服务，返回内部标准题目列表。 */
    @SuppressWarnings("unchecked")
    private List<?> buildDefinitions(EngStudyServiceImpl service) throws Exception {
        Method method = EngStudyServiceImpl.class.getDeclaredMethod("buildDefinitions", Long.class);
        method.setAccessible(true);
        return (List<?>) method.invoke(service, ARTICLE_ID);
    }

    /**
     * 按内部标准题目的生成顺序提取题目标识；definitions 为标准题目集合，返回有序题目标识。
     *
     * @throws Exception 反射读取题目标识失败时抛出异常
     */
    private List<String> definitionQuestionIds(List<?> definitions) throws Exception {
        List<String> questionIds = new ArrayList<>();
        for (Object definition : definitions) {
            questionIds.add((String) definitionValue(definition, "questionId"));
        }
        return questionIds;
    }

    /**
     * 将内部标准题目转换为按题目标识索引的对外内容快照；definitions 为标准题目集合，返回内容映射。
     *
     * @throws Exception 反射读取题目字段失败时抛出异常
     */
    private Map<String, List<Object>> definitionContents(List<?> definitions) throws Exception {
        Map<String, List<Object>> contents = new HashMap<>();
        for (Object definition : definitions) {
            String questionId = (String) definitionValue(definition, "questionId");
            List<Object> content = new ArrayList<>();
            content.add(definitionValue(definition, "type"));
            content.add(definitionValue(definition, "prompt"));
            content.add(definitionValue(definition, "options"));
            content.add(definitionValue(definition, "audioUrl"));
            content.add(definitionValue(definition, "answerLength"));
            contents.put(questionId, content);
        }
        return contents;
    }

    /**
     * 提取对外题目当前排列中的题目标识；questions 为接口返回题目，返回有序题目标识。
     */
    private List<String> questionIds(List<EngChallengeQuestionVo> questions) {
        List<String> questionIds = new ArrayList<>();
        for (EngChallengeQuestionVo question : questions) {
            questionIds.add(question.getQuestionId());
        }
        return questionIds;
    }

    /**
     * 提取对外题目除标识和位置之外的全部业务内容；question 为接口返回题目，返回内容快照。
     */
    private List<Object> questionContent(EngChallengeQuestionVo question) {
        List<Object> content = new ArrayList<>();
        content.add(question.getType());
        content.add(question.getPrompt());
        content.add(question.getOptions());
        content.add(question.getAudioUrl());
        content.add(question.getAnswerLength());
        return content;
    }

    /**
     * 为公开挑战接口设置固定测试登录用户；该方法无输入输出，认证信息由调用方在场景结束后清理。
     */
    private void setTestLoginUser() {
        SysUser user = new SysUser();
        user.setUserName("tester");
        LoginUser loginUser = new LoginUser(20L, 30L, user, Set.of());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                loginUser, null, loginUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * 调用私有计分方法；service 为待测试服务、definitions 为题目定义、answers 为答案，返回计分结果。
     */
    private EngChallengeResultVo calculateResult(EngStudyServiceImpl service, List<?> definitions,
            Map<String, String> answers) throws Exception {
        Method method = EngStudyServiceImpl.class.getDeclaredMethod("calculateResult", List.class, Map.class);
        method.setAccessible(true);
        return (EngChallengeResultVo) method.invoke(service, definitions, answers);
    }

    /** 从全部题目中筛选 apple 对应四类题；definitions 为全部定义，返回四题集合。 */
    private List<?> appleDefinitions(List<?> definitions) throws Exception {
        return List.of(
                findDefinition(definitions, "WORD_TO_CN:1"),
                findDefinition(definitions, "CN_TO_WORD:1"),
                findDefinition(definitions, "SENTENCE_CHOICE:1"),
                findDefinition(definitions, "SENTENCE_FILL:1"));
    }

    /**
     * 调用私有错词维护方法；service 为服务、definitions 为题目、result 为判分结果。
     */
    private void updateWrongWords(EngStudyServiceImpl service, List<?> definitions, EngChallengeResultVo result)
            throws Exception {
        Method method = EngStudyServiceImpl.class.getDeclaredMethod("updateWrongWords", Long.class, String.class,
                Long.class, List.class, EngChallengeResultVo.class);
        method.setAccessible(true);
        method.invoke(service, 20L, "tester", ARTICLE_ID, definitions, result);
    }

    /**
     * 断言答案校验抛出业务异常；service 为待测试服务、answers 为提交答案、definitions 为标准题目。
     */
    private void assertServiceException(EngStudyServiceImpl service, List<EngChallengeAnswerDto> answers,
            List<?> definitions, String message) throws Exception {
        Method method = EngStudyServiceImpl.class.getDeclaredMethod("validateAnswers", List.class, List.class);
        method.setAccessible(true);
        try {
            method.invoke(service, answers, definitions);
            throw new AssertionError(message);
        } catch (InvocationTargetException exception) {
            assertTrue(exception.getCause() instanceof ServiceException, message);
        }
    }

    /**
     * 断言即时判题抛出业务异常；service 为待测试服务、request 为单题请求、message 为失败说明。
     */
    private void assertCheckServiceException(EngStudyServiceImpl service, EngChallengeCheckDto request,
            String message) {
        try {
            service.checkChallengeAnswer(request);
            throw new AssertionError(message);
        } catch (ServiceException exception) {
            // 捕获到业务异常即符合非法请求的预期。
        }
    }

    /** 按题目标识查找内部定义；definitions 为定义集合、questionId 为标识，找不到时抛出断言异常。 */
    private Object findDefinition(List<?> definitions, String questionId) throws Exception {
        Object definition = findDefinitionOrNull(definitions, questionId);
        if (definition == null) {
            throw new AssertionError("未找到题目：" + questionId);
        }
        return definition;
    }

    /** 按题目标识查找内部定义；definitions 为定义集合、questionId 为标识，找不到时返回 null。 */
    private Object findDefinitionOrNull(List<?> definitions, String questionId) throws Exception {
        for (Object definition : definitions) {
            if (questionId.equals(definitionValue(definition, "questionId"))) {
                return definition;
            }
        }
        return null;
    }

    /** 统计指定题目标识出现次数；definitions 为定义集合、questionId 为标识，返回出现次数。 */
    private int countDefinitions(List<?> definitions, String questionId) throws Exception {
        int count = 0;
        for (Object definition : definitions) {
            if (questionId.equals(definitionValue(definition, "questionId"))) {
                count++;
            }
        }
        return count;
    }

    /** 调用内部题目记录访问器；definition 为内部定义、name 为字段名，返回字段值。 */
    private Object definitionValue(Object definition, String name) throws Exception {
        Method method = definition.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        return method.invoke(definition);
    }

    /** 构造单题答案；questionId 为题目标识、value 为答案文本，返回提交对象。 */
    private EngChallengeAnswerDto answer(String questionId, String value) {
        EngChallengeAnswerDto answer = new EngChallengeAnswerDto();
        answer.setQuestionId(questionId);
        answer.setAnswer(value);
        return answer;
    }

    /**
     * 构造即时单题判题请求；articleId 为文章主键、questionId 为题目标识、value 为用户答案。
     */
    private EngChallengeCheckDto checkRequest(Long articleId, String questionId, String value) {
        EngChallengeCheckDto request = new EngChallengeCheckDto();
        request.setArticleId(articleId);
        request.setQuestionId(questionId);
        request.setAnswer(value);
        return request;
    }

    /** 断言条件成立；condition 为判断结果、message 为失败说明。 */
    private void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /** 断言期望值和实际值相等；expected 为期望、actual 为实际、message 为失败说明。 */
    private void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + "，期望：" + expected + "，实际：" + actual);
        }
    }
}
