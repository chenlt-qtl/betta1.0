export function getQuestions(wordList) {
  if (!wordList || wordList.length < 5) {
    return [];
  }

  //生成题目
  const questionList = [];
  wordList.forEach((word) => {
    //0：词选意思  1：意思选词， 2：句子填空
    questionList.push(getQuestion(word, 0, wordList));
    questionList.push(getQuestion(word, 1, wordList));
    const question3 = getQuestion(word, 2, wordList);
    question3 && questionList.push(question3);
  });
  //打乱顺序
  const length = questionList.length;

  for (let i = 0; i < length; i++) {
    const j = parseInt(Math.random() * length);
    const temp = questionList[i];
    questionList[i] = questionList[j];
    questionList[j] = temp;
  }
  return questionList;
}

//获取题目
function getQuestion(word, type, wordList) {
  let method, question, sentenceAcceptation;
  switch (type) {
    case 0:
      method = getExchange;
      question = word.wordName;
      break;
    case 1:
      method = getWordName;
      question = getExchange(word);
      break;
    case 2:
      if (!word.sentence || /^\[NT\]/.test(word.sentence)) { //"'[NT]' 开头的句子不做测试"
        return;
      }
      method = getWordName;
      question = word.sentence.replace(
        new RegExp(word.wordName, "i"),
        "_____"
      );
      sentenceAcceptation = word.sentenceAcceptation;
      break;
  }
  const answer = method(word);
  const answers = [];
  const answerKey = parseInt(Math.random() * 4);
  answers[answerKey] = answer;
  const otherWords = getRandomWords(3, word, wordList);
  for (let i = 0; i < 4; i++) {
    if (i != answerKey) {
      const randomWord = otherWords.pop();
      answers[i] = method(randomWord);
    }
  }

  return {
    type,
    answerKey,
    answers,
    question,
    sentenceAcceptation,
    word,
  };
}

function getExchange(word) {
  return word.exchange || (word.acceptation || "").split("|").join("");
}

function getWordName(word) {
  return word.wordName;
}

function getRandomWords(number, word, wordList) {
  const result = [];
  let i = 200; //最多循环200下
  while (i > 0) {
    i--;
    const j = parseInt(Math.random() * wordList.length);
    const randomWord = wordList[j];
    const exist = result.find(
      (existWord) => randomWord.wordName && existWord.wordName == randomWord.wordName
    );

    if (!exist && randomWord.wordName != word.wordName) {
      result.push(randomWord);
    }

    if (result.length >= number) {
      break;
    }
  }
  return result;
}
