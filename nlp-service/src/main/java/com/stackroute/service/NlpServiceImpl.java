package com.stackroute.service;

import com.aliasi.sentences.IndoEuropeanSentenceModel;
import com.aliasi.sentences.SentenceModel;
import com.aliasi.tokenizer.*;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.*;
@Service
@PropertySource(value = "classpath:application.properties")
public class NlpServiceImpl implements NlpService {

    String paragraph;
    String paragraphId = "para001";
    String documentId = "doc001";

    String[] stopwords={"i", "me", "my", "myself", "we", "our", "ours", "ourselves", "could", "he'd","he'll", "he's", "here's", "how's", "ought", "she'd", "she'll", "that's", "there's", "they'd","they'll", "they're", "they've", "we'd", "we'll", "we're", "we've", "what's", "when's", "where's","who's", "why's", "would", "i'd", "i'll", "i'm", "i've", "you", "you're", "you've", "you'll","you'd", "your", "yours", "yourself", "yourselves", "he", "him", "his", "himself", "she","she's", "her", "hers", "herself", "it", "it's", "its", "itself", "they", "them", "their","theirs", "themselves", "what", "which", "who", "whom", "this", "that", "that'll", "these","those", "am", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had", "having","do", "does", "did", "doing", "a", "an", "the", "and", "but", "if", "or", "because", "as", "until","while", "of", "at", "by", "for", "with", "about", "against", "between", "into", "through","during", "before", "after", "above", "below", "to", "from", "up", "down", "in", "out", "off", "over", "under", "again", "further", "then", "once", "here", "there", "when", "where","why", "how", "all", "any", "both", "each", "few", "more", "most", "other", "some", "such", "no","nor", "not", "only", "own", "same", "so", "than", "too", "very", "s", "t", "can", "will","just", "don", "don't", "should", "should've", "now", "d", "ll", "m", "o", "re", "ve", "y", "ain","aren", "aren't", "couldn", "couldn't", "didn", "didn't", "doesn", "doesn't", "hadn", "hadn't","hasn", "hasn't", "haven", "haven't", "isn", "isn't", "ma", "mightn", "mightn't", "mustn", "mustn't","needn", "needn't", "shan", "shan't", "shouldn", "shouldn't", "wasn", "wasn't", "weren", "weren't","won", "won't", "wouldn", "wouldn't"};
    String[] domainSpecificNgrams={"annotations", "ioc container", "beans", "spring core", "spring data jpa", "spring datajpa","spring aop", "spring security", "spring cloud", "spring reactive", "spring mvc"};

    public void setParagraph(String paragraph) {
        this.paragraph = paragraph;
    }

    public String getCleanerParagrah() {
        String inputParagraph = this.paragraph;
        // Data Cleaning by removing extra spaces.
        inputParagraph = inputParagraph.trim();
        inputParagraph = inputParagraph.replaceAll("\\s+", " ");
        inputParagraph = inputParagraph.replaceAll("\\t", " ");

        String[] tokenizedWord = inputParagraph.split(" ");
        StringBuffer cleanedParagraph = new StringBuffer();
        for (int i = 0; i < tokenizedWord.length; i++) {
            cleanedParagraph.append(tokenizedWord[i] + " ");
        }
        return cleanedParagraph.toString().trim();
    }

    public ArrayList<String> getLemmitizedWords() {
        Properties properties = new Properties();
        properties.setProperty("annotator", "lemma");
        // StanfordCoreNLP uses pipeline and this pipeline is create
        // based on the properties we specity in java.util.Properties
        // different set of propeties provide different NLP tasks
        StanfordCoreNLP pipeline = new StanfordCoreNLP(properties);
        // This annotations object gives the special meaning to the
        // string we used in propeties.put() method
        Annotation annotations = new Annotation(getCleanerParagrah());
        // pipeline.annotate(annotations)  provies the annotation to those particular objects.
        pipeline.annotate(annotations);
        // sentenceList contains list of sentences
        ArrayList<String> lemmaWords = new ArrayList<>();
        ArrayList<String> originalWords = new ArrayList<>();
        List<CoreMap> sentenceList = annotations.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentenceList) {
            for (CoreLabel word : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                lemmaWords.add(word.lemma());
                originalWords.add(word.originalText());
            }
        }
        return lemmaWords;
    }

    public List<String> getStemmedWords() {
        TokenizerFactory tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;
        TokenizerFactory porterFactory = new PorterStemmerTokenizerFactory(tokenizerFactory);
        ArrayList<String> wordTokens = getLemmitizedWords();
        List<String> stemmedWordsList = new ArrayList<>();
        for (String word : wordTokens) {
            Tokenization tokenization = new Tokenization(word, porterFactory);
            stemmedWordsList.add(tokenization.tokenList().toString());
        }
        return stemmedWordsList;
    }

    public ArrayList<String> getWordsWithoutStopWords() {
        ArrayList<String> wordsWithOutStopwords = getLemmitizedWords();
        for (int j = 0; j < stopwords.length; j++) {
            if (wordsWithOutStopwords.contains(stopwords[j])) {
                wordsWithOutStopwords.remove(stopwords[j]);//remove it
            }
        }
        return wordsWithOutStopwords;
    }

    public String getParagraphWithOutStopWords() {
        ArrayList<String> wordsWithOutStopwords = getWordsWithoutStopWords();
        StringBuffer paragraphWithOutStopWords = new StringBuffer();
        for (int i = 0; i < wordsWithOutStopwords.size(); i++) {
            paragraphWithOutStopWords.append(wordsWithOutStopwords.get(i) + " ");
        }
        return paragraphWithOutStopWords.toString().trim();
    }

    public ArrayList<POSTagging> getPOSWords() {
        Properties properties = new Properties();
        properties.setProperty("annotator", "pos");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(properties);
        CoreDocument coreDocument = new CoreDocument(getParagraphWithOutStopWords());
        pipeline.annotate(coreDocument);
        List<CoreLabel> coreLabelsList = coreDocument.tokens();
        ArrayList<POSTagging> wordsWithPOSTag = new ArrayList<>();
        for (CoreLabel coreLabel : coreLabelsList) {
            String partsOfSpeech = coreLabel.get(CoreAnnotations.PartOfSpeechAnnotation.class);
            wordsWithPOSTag.add(new POSTagging(coreLabel.originalText(), partsOfSpeech));
        }
        return wordsWithPOSTag;
    }

    public void showAllResults() {
        String clearedParagraph = new String(getCleanerParagrah());
        ArrayList<String> allLemmas = new ArrayList<>(getLemmitizedWords());
        ArrayList<String> allStems = new ArrayList<>(getStemmedWords());
        ArrayList<String> allStopWords = new ArrayList<>(getWordsWithoutStopWords());
        String paragraphWithOutStopWords = new String(getParagraphWithOutStopWords());
        ArrayList<POSTagging> posTaggings = new ArrayList<>(getPOSWords());
    }

}