package edu.stanford.nlp.parser.lexparser;

import java.util.List;
import java.util.ArrayList;

import edu.stanford.nlp.trees.*;

/**
 * Does detransformations to a parsed sentence to map it back to the
 * standard treebank form for output or evaluation.
 * This version has Penn-Treebank-English-specific details, but can probably
 * be used without harm on other treebanks.
 * Returns labels to their basic category, removes punctuation (should be with
 * respect to a gold tree, but currently isn't), deletes the boundary symbol,
 * changes PRT labels to ADVP.
 *
 * @author Dan Klein
 * @author Christopher Manning
 */
public class TreeCollinizer implements TreeTransformer {

  private final TreebankLanguagePack tlp;
  private final boolean deletePunct;
  private final boolean fixCollinsBaseNP;

  public TreeCollinizer(TreebankLanguagePack tlp) {
    this(tlp, true, false);
  }

  public TreeCollinizer(TreebankLanguagePack tlp, boolean deletePunct,
      boolean fixCollinsBaseNP) {
    this.tlp = tlp;
    this.deletePunct = deletePunct;
    this.fixCollinsBaseNP = fixCollinsBaseNP;
  }

  protected TreeFactory tf = new LabeledScoredTreeFactory();

  public Tree transformTree(Tree tree) {
    if (tree == null) return null;
    String s = tree.value();
    if (tlp.isStartSymbol(s))
      return transformTree(tree.firstChild());

    if (tree.isLeaf()) {
      return tf.newLeaf(tree.label());  // make it a StringLabel
    }
    s = tlp.basicCategory(s);

    // wsg2010: Might need a better way to deal with tag ambiguity. This still doesn't handle the
    // case where the GOLD tree does not label a punctuation mark as such (common in French), and
    // the guess tree does.
    if (deletePunct && tree.isPreTerminal() &&
        (tlp.isEvalBIgnoredPunctuationTag(s) ||
        tlp.isPunctuationWord(tree.firstChild().value()))) {
      return null;
    }

    // remove the extra NPs inserted in the collinsBaseNP option
    if (fixCollinsBaseNP && s.equals("NP")) {
      Tree[] kids = tree.children();
      if (kids.length == 1 && tlp.basicCategory(kids[0].value()).equals("NP")) {
        return transformTree(kids[0]);
      }
    }
    // Magerman erased this decision, and everyone else has followed like sheep
    if (s.equals("PRT")) {
      s = "ADVP";
    }
    List<Tree> children = new ArrayList<Tree>();
    for (int cNum = 0, numKids = tree.numChildren(); cNum < numKids; cNum++) {
      Tree child = tree.children()[cNum];
      Tree newChild = transformTree(child);
      if (newChild != null) {
        children.add(newChild);
      }
    }
    if (children.isEmpty()) {
      return null;
    }

    Tree node = tf.newTreeNode(tree.label(), children);
    node.setValue(s);

    return node;
  }

} // end class TreeCollinizer
