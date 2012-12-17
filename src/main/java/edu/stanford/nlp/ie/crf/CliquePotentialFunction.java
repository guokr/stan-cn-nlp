package edu.stanford.nlp.ie.crf;

/**
 * @author Mengqiu Wang
 */

public interface CliquePotentialFunction {

  /**
   * @param cliqueSize 1 if node clique, 2 if edge clique, etc
   * @param labelIndex the index of the output class label
   * @param data a double array containing the features that are active in this clique
   *
   * @return clique potential value
   */
  public double computeCliquePotential(int cliqueSize, int labelIndex, int[] cliqueFeatures);

}
