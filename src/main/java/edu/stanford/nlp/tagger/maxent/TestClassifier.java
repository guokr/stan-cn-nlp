package edu.stanford.nlp.tagger.maxent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.io.PrintFile;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.common.TaggerConstants;
import edu.stanford.nlp.tagger.io.TaggedFileRecord;


/** Tags data and can handle either data with gold-standard tags (computing
 *  performance statistics) or unlabeled data.
 *  (The Constructor actually runs the tagger. The main entry points are the
 *  static methods at the bottom of the class.)
 *
 *  Also can train data using the saveModel method.  This class is really the entry
 *  point to all tagger operations, it seems.
 *
 *  @author Kristina Toutanova
 *  @version 1.0
 */
// TODO: can we break this class up in some way?  Perhaps we can
// spread some functionality into TestSentence and some into MaxentTagger
// TODO: at the very least, it doesn't seem to make sense to make it
// an object with state, rather than just some static methods
public class TestClassifier {

  private final TaggedFileRecord fileRecord;
  private int numRight;
  private int numWrong;
  private int unknownWords;
  private int numWrongUnknown;
  private int numCorrectSentences;
  private int numSentences;

  // TODO: only one boolean here instead of 3?
  private boolean writeUnknDict;
  private boolean writeWords;
  private boolean writeTopWords;

  private Dictionary wrongWords = new Dictionary();
  // Dictionary unknownWordsDict = new Dictionary();

  public TestClassifier(TaggerConfig config,
                        MaxentTagger maxentTagger) throws IOException {
    setDebug(config.getDebug());

    fileRecord = TaggedFileRecord.createRecord(config, config.getFile());

    String dPrefix = config.getDebugPrefix();
    if (dPrefix == null || dPrefix.equals("")) {
      dPrefix = fileRecord.filename();
    }
    test(config, dPrefix, maxentTagger);
  }

  /**
   * Adds the EOS marker to both a list of words and a list of tags.
   */
  private static void appendSentenceEnd(List<String> words, List<String> tags) {
    //the sentence is read already, add eos
    words.add(TaggerConstants.EOS_WORD);
    tags.add(TaggerConstants.EOS_TAG);
  }

  private void testOneSentence(List<String> sentence, List<String> tagsArr,
                               PrintFile wordsFile, PrintFile unknDictFile,
                               PrintFile topWordsFile, boolean verboseResults,
                               MaxentTagger maxentTagger) {
    numSentences++;

    int len = sentence.size();
    String[] testSent = new String[len];
    String[] correctTags = new String[len];
    for (int i = 0; i < len; i++) {
      testSent[i] = sentence.get(i);
      correctTags[i] = tagsArr.get(i);
    }

    TestSentence testS = new TestSentence(maxentTagger,
                                          testSent, correctTags,
                                          wordsFile, wrongWords,
                                          verboseResults);
    if (writeUnknDict) testS.printUnknown(numSentences, unknDictFile);
    if (writeTopWords) testS.printTop(topWordsFile);

    numWrong = numWrong + testS.numWrong;
    numRight = numRight + testS.numRight;
    unknownWords = unknownWords + testS.numUnknown;
    numWrongUnknown = numWrongUnknown + testS.numWrongUnknown;
    if (testS.numWrong == 0) {
      numCorrectSentences++;
    }
    if (verboseResults) {
      System.err.println("Sentence number: " + numSentences + "; length " + (len-1) +
                         "; correct: " + testS.numRight + "; wrong: " + testS.numWrong +
                         "; unknown wrong: " + testS.numWrongUnknown);
      System.err.println("  Total tags correct: " + numRight + "; wrong: " + numWrong +
                         "; unknown wrong: " + numWrongUnknown);
    }
  }

  /**
   * Test on a file containing correct tags already. when init'ing from trees
   * TODO: Add the ability to have a second transformer to transform output back; possibly combine this method
   * with method below
   */
  private void test(TaggerConfig config, String saveRoot,
                    MaxentTagger maxentTagger)
    throws IOException
  {
    numSentences = 0;
    PrintFile pf = null;
    PrintFile pf1 = null;
    PrintFile pf3 = null;

    if(writeWords) pf = new PrintFile(saveRoot + ".words");
    if(writeUnknDict) pf1 = new PrintFile(saveRoot + ".un.dict");
    if(writeTopWords) pf3 = new PrintFile(saveRoot + ".words.top");

    boolean verboseResults = config.getVerboseResults();

    for (List<TaggedWord> taggedSentence : fileRecord.reader()) {
      List<String> sentence = new ArrayList<String>();
      List<String> tagsArr = new ArrayList<String>();

      for (TaggedWord cur : taggedSentence) {
        tagsArr.add(cur.tag());
        sentence.add(cur.word());
      }

      appendSentenceEnd(sentence, tagsArr);
      testOneSentence(sentence, tagsArr, pf, pf1, pf3,
                      verboseResults, maxentTagger);
    }

    if(pf != null) pf.close();
    if(pf1 != null) pf1.close();
    if(pf3 != null) pf3.close();
  }


  String resultsString(TaggerConfig config, MaxentTagger maxentTagger) {
    StringBuilder output = new StringBuilder();
    output.append("Model " + config.getModel() + " has xSize=" + maxentTagger.xSize +
                  ", ySize=" + maxentTagger.ySize + ", and numFeatures=" +
                  maxentTagger.prob.lambda.length + ".\n");
    output.append("Results on " + numSentences + " sentences and " +
                  (numRight + numWrong) + " words, of which " +
                  unknownWords + " were unknown.\n");
    output.append(String.format("Total sentences right: %d (%f%%); wrong: %d (%f%%).\n",
                                numCorrectSentences, numCorrectSentences * 100.0 / numSentences,
                                numSentences - numCorrectSentences,
                                (numSentences - numCorrectSentences) * 100.0 / (numSentences)));
    output.append(String.format("Total tags right: %d (%f%%); wrong: %d (%f%%).\n",
                                numRight, numRight * 100.0 / (numRight + numWrong), numWrong,
                                numWrong * 100.0 / (numRight + numWrong)));
    if (unknownWords > 0) {
      output.append(String.format("Unknown words right: %d (%f%%); wrong: %d (%f%%).\n",
                                  (unknownWords - numWrongUnknown),
                                  100.0 - (numWrongUnknown * 100.0 / unknownWords),
                                  numWrongUnknown, numWrongUnknown * 100.0 / unknownWords));
    }
    return output.toString();
  }

  void printModelAndAccuracy(TaggerConfig config, MaxentTagger maxentTagger) {
    // print the output all at once so that multiple threads don't clobber each other's output
    System.err.println(resultsString(config, maxentTagger));
  }


  int getNumWords() {
    return numRight + numWrong;
  }

  void setDebug(boolean status) {
    writeUnknDict = status;
    writeWords = status;
    writeTopWords = status;
  }


}
