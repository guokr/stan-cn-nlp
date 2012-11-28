package edu.stanford.nlp.ie.crf;

import edu.stanford.nlp.math.ArrayMath;
import edu.stanford.nlp.optimization.AbstractStochasticCachingDiffUpdateFunction;
import edu.stanford.nlp.util.Index;

import java.util.Arrays;

/**
 * @author Jenny Finkel
 */

public class CRFLogConditionalObjectiveFunction extends AbstractStochasticCachingDiffUpdateFunction {

  public static final int NO_PRIOR = 0;
  public static final int QUADRATIC_PRIOR = 1;
  /* Use a Huber robust regression penalty (L1 except very near 0) not L2 */
  public static final int HUBER_PRIOR = 2;
  public static final int QUARTIC_PRIOR = 3;

  private final int prior;
  private final double sigma;
  private final double epsilon = 0.1; // You can't actually set this at present
  /** label indices - for all possible label sequences - for each feature */
  private final Index<CRFLabel>[] labelIndices;
  private final Index<String> classIndex;  // didn't have <String> before. Added since that's what is assumed everywhere.
  private final Index featureIndex; // todo [cdm]: Unused. Remove
  private final double[][] Ehat; // empirical counts of all the features [feature][class]
  private final int window;
  private final int numClasses;
  private final int[] map;
  private final int[][][][] data;  // data[docIndex][tokenIndex][][]
  private final int[][] labels;    // labels[docIndex][tokenIndex]
  private final int domainDimension;

  private int[][] weightIndices;

  private final String backgroundSymbol;

  public static boolean VERBOSE = false;

  public static int getPriorType(String priorTypeStr) {
    if (priorTypeStr == null) return QUADRATIC_PRIOR;  // default
    if ("QUADRATIC".equalsIgnoreCase(priorTypeStr)) {
      return QUADRATIC_PRIOR;
    } else if ("HUBER".equalsIgnoreCase(priorTypeStr)) {
      return HUBER_PRIOR;
    } else if ("QUARTIC".equalsIgnoreCase(priorTypeStr)) {
      return QUARTIC_PRIOR;
    } else if ("NONE".equalsIgnoreCase(priorTypeStr)) {
      return NO_PRIOR;
    } else {
      throw new IllegalArgumentException("Unknown prior type: " + priorTypeStr);
    }
  }

  CRFLogConditionalObjectiveFunction(int[][][][] data, int[][] labels, Index featureIndex, int window, Index<String> classIndex, Index[] labelIndices, int[] map, String backgroundSymbol) {
    this(data, labels, featureIndex, window, classIndex, labelIndices, map, QUADRATIC_PRIOR, backgroundSymbol);
  }

  CRFLogConditionalObjectiveFunction(int[][][][] data, int[][] labels, Index featureIndex, int window, Index<String> classIndex, Index[] labelIndices, int[] map, String backgroundSymbol, double sigma) {
    this(data, labels, featureIndex, window, classIndex, labelIndices, map, QUADRATIC_PRIOR, backgroundSymbol, sigma);
  }

  CRFLogConditionalObjectiveFunction(int[][][][] data, int[][] labels, Index featureIndex, int window, Index<String> classIndex, Index[] labelIndices, int[] map, int prior, String backgroundSymbol) {
    this(data, labels, featureIndex, window, classIndex, labelIndices, map, prior, backgroundSymbol, 1.0);
  }

  CRFLogConditionalObjectiveFunction(int[][][][] data, int[][] labels, Index featureIndex, int window, Index<String> classIndex, Index[] labelIndices, int[] map, int prior, String backgroundSymbol, double sigma) {
    this.featureIndex = featureIndex;
    this.window = window;
    this.classIndex = classIndex;
    this.numClasses = classIndex.size();
    this.labelIndices = labelIndices;
    this.map = map;
    this.data = data;
    this.labels = labels;
    this.prior = prior;
    this.backgroundSymbol = backgroundSymbol;
    this.sigma = sigma;
    Ehat = empty2D();
    empiricalCounts(data, labels);
    int myDomainDimension = 0;
    for (int dim : map) {
      myDomainDimension += labelIndices[dim].size();
    }
    domainDimension = myDomainDimension;
  }

  // this used to be computed lazily, but that was clearly erroneous for multithreading!
  @Override
  public int domainDimension() {
    return domainDimension;
  }

  /**
   * Takes a double array of weights and creates a 2D array where:
   *
   * the first element is the mapped index of featuresIndex
   * the second element is the index of the of the element
   *
   * @return a 2D weight array
   */
  public static double[][] to2D(double[] weights, Index[] labelIndices, int[] map) {
    double[][] newWeights = new double[map.length][];
    int index = 0;
    for (int i = 0; i < map.length; i++) {
      newWeights[i] = new double[labelIndices[map[i]].size()];
      System.arraycopy(weights, index, newWeights[i], 0, labelIndices[map[i]].size());
      index += labelIndices[map[i]].size();
    }
    return newWeights;
  }

  public double[][] to2D(double[] weights) {
    return to2D(weights, this.labelIndices, this.map);
  }

  public static double[] to1D(double[][] weights, int domainDimension) {
    double[] newWeights = new double[domainDimension];
    int index = 0;
    for (int i = 0; i < weights.length; i++) {
      System.arraycopy(weights[i], 0, newWeights, index, weights[i].length);
      index += weights[i].length;
    }
    return newWeights;
  }

  public double[] to1D(double[][] weights) {
    return to1D(weights, domainDimension());
  }

  public int[][] getWeightIndices()
  {
    if (weightIndices == null) {
      weightIndices = new int[map.length][];
      int index = 0;
      for (int i = 0; i < map.length; i++) {
        weightIndices[i] = new int[labelIndices[map[i]].size()];
        for (int j = 0; j < labelIndices[map[i]].size(); j++) {
          weightIndices[i][j] = index;
          index++;
        }
      }
    }
    return weightIndices;
  }

  private double[][] empty2D() {
    double[][] d = new double[map.length][];
    // int index = 0;
    for (int i = 0; i < map.length; i++) {
      d[i] = new double[labelIndices[map[i]].size()];
      // cdm july 2005: below array initialization isn't necessary: JLS (3rd ed.) 4.12.5
      // Arrays.fill(d[i], 0.0);
      // index += labelIndices[map[i]].size();
    }
    return d;
  }

  private void empiricalCounts(int[][][][] data, int[][] labels) {
    for (int m = 0; m < data.length; m++) {
      int[][][] docData = data[m];
      int[] docLabels = labels[m];
      int[] windowLabels = new int[window];
      Arrays.fill(windowLabels, classIndex.indexOf(backgroundSymbol));

      if (docLabels.length>docData.length) { // only true for self-training
        // fill the windowLabel array with the extra docLabels
        System.arraycopy(docLabels, 0, windowLabels, 0, windowLabels.length);
        // shift the docLabels array left
        int[] newDocLabels = new int[docData.length];
        System.arraycopy(docLabels, docLabels.length-newDocLabels.length, newDocLabels, 0, newDocLabels.length);
        docLabels = newDocLabels;
      }
      for (int i = 0; i < docData.length; i++) {
        System.arraycopy(windowLabels, 1, windowLabels, 0, window - 1);
        windowLabels[window - 1] = docLabels[i];
        for (int j = 0; j < docData[i].length; j++) {
          int[] cliqueLabel = new int[j + 1];
          System.arraycopy(windowLabels, window - 1 - j, cliqueLabel, 0, j + 1);
          CRFLabel crfLabel = new CRFLabel(cliqueLabel);
          int labelIndex = labelIndices[j].indexOf(crfLabel);
          //System.err.println(crfLabel + " " + labelIndex);
          for (int k = 0; k < docData[i][j].length; k++) {
            Ehat[docData[i][j][k]][labelIndex]++;
          }
        }
      }
    }
  }

  // todo [cdm]: Below data[m] --> docData
  /**
   * Calculates both value and partial derivatives at the point x, and save them internally.
   */
  @Override
  public void calculate(double[] x) {

    double prob = 0.0; // the log prob of the sequence given the model, which is the negation of value at this point
    double[][] weights = to2D(x);

    // the expectations over counts
    // first index is feature index, second index is of possible labeling
    double[][] E = empty2D();

    // iterate over all the documents
    for (int m = 0; m < data.length; m++) {
      int[][][] docData = data[m];
      int[] docLabels = labels[m];

      if (docLabels.length == 0) continue;
      
      // make a clique tree for this document
      CRFCliqueTree cliqueTree = CRFCliqueTree.getCalibratedCliqueTree(weights, docData, labelIndices, numClasses, classIndex, backgroundSymbol);

      // compute the log probability of the document given the model with the parameters x
      int[] given = new int[window - 1];
      Arrays.fill(given, classIndex.indexOf(backgroundSymbol));
      if (docLabels.length>docData.length) { // only true for self-training
        // fill the given array with the extra docLabels
        System.arraycopy(docLabels, 0, given, 0, given.length);
        // shift the docLabels array left
        int[] newDocLabels = new int[docData.length];
        System.arraycopy(docLabels, docLabels.length-newDocLabels.length, newDocLabels, 0, newDocLabels.length);
        docLabels = newDocLabels;
      }
      // iterate over the positions in this document
      for (int i = 0; i < docData.length; i++) {
        int label = docLabels[i];
        double p = cliqueTree.condLogProbGivenPrevious(i, label, given);
        if (VERBOSE) {
          System.err.println("P(" + label + "|" + ArrayMath.toString(given) + ")=" + p);
        }
        prob += p;

        if (given.length == 0) continue;
        System.arraycopy(given, 1, given, 0, given.length - 1);
        given[given.length - 1] = label;
      }

      // compute the expected counts for this document, which we will need to compute the derivative
      // iterate over the positions in this document
      for (int i = 0; i < data[m].length; i++) {
        // for each possible clique at this position
        for (int j = 0; j < data[m][i].length; j++) {
          Index<CRFLabel> labelIndex = labelIndices[j];
          // for each possible labeling for that clique
          for (int k = 0; k < labelIndex.size(); k++) {
            int[] label = labelIndex.get(k).getLabel();
            double p = cliqueTree.prob(i, label); // probability of these labels occurring in this clique with these features
            for (int n = 0; n < data[m][i][j].length; n++) {
              E[data[m][i][j][n]][k] += p;
            }
          }
        }
      }
    }

    if (Double.isNaN(prob)) { // shouldn't be the case
      throw new RuntimeException("Got NaN for prob in CRFLogConditionalObjectiveFunction.calculate()" +
              " - this may well indicate numeric underflow due to overly long documents.");
    }

    value = -prob;
    if (VERBOSE) {
      System.err.println("value is " + value);
    }

    // compute the partial derivative for each feature by comparing expected counts to empirical counts
    int index = 0;
    for (int i = 0; i < E.length; i++) {
      for (int j = 0; j < E[i].length; j++) {
        derivative[index++] = (E[i][j] - Ehat[i][j]);
        if (VERBOSE) {
          System.err.println("deriv(" + i + "," + j + ") = " + E[i][j] + " - " + Ehat[i][j] + " = " + derivative[index - 1]);
        }
      }
    }
    
    // incorporate priors
    if (prior == QUADRATIC_PRIOR) {
      double sigmaSq = sigma * sigma;
      for (int i = 0; i < x.length; i++) {
        double k = 1.0;
        double w = x[i];
        value += k * w * w / 2.0 / sigmaSq;
        derivative[i] += k * w / sigmaSq;
      }
    } else if (prior == HUBER_PRIOR) {
      double sigmaSq = sigma * sigma;
      for (int i = 0; i < x.length; i++) {
        double w = x[i];
        double wabs = Math.abs(w);
        if (wabs < epsilon) {
          value += w * w / 2.0 / epsilon / sigmaSq;
          derivative[i] += w / epsilon / sigmaSq;
        } else {
          value += (wabs - epsilon / 2) / sigmaSq;
          derivative[i] += ((w < 0.0) ? -1.0 : 1.0) / sigmaSq;
        }
      }
    } else if (prior == QUARTIC_PRIOR) {
      double sigmaQu = sigma * sigma * sigma * sigma;
      for (int i = 0; i < x.length; i++) {
        double k = 1.0;
        double w = x[i];
        value += k * w * w * w * w / 2.0 / sigmaQu;
        derivative[i] += k * w / sigmaQu;
      }
    }
  }

  @Override
  public void calculateStochastic(double[] x, double [] v, int[] batch){
    calculateStochasticGradientOnly(x,batch);
  }

  @Override
  public int dataDimension(){
    return data.length;
  }



  public void calculateStochasticGradientOnly(double[] x, int[] batch) {

    double prob = 0.0; // the log prob of the sequence given the model, which is the negation of value at this point
    double[][] weights = to2D(x);

    double batchScale = ((double) batch.length)/((double) this.dataDimension());

    // the expectations over counts
    // first index is feature index, second index is of possible labeling
    double[][] E = empty2D();

    // iterate over all the documents
    for (int ind : batch) {
      int[][][] docData = data[ind];
      int[] docLabels = labels[ind];

      // make a clique tree for this document
      CRFCliqueTree cliqueTree = CRFCliqueTree.getCalibratedCliqueTree(weights, docData, labelIndices, numClasses, classIndex, backgroundSymbol);

      // compute the log probability of the document given the model with the parameters x
      int[] given = new int[window - 1];
      Arrays.fill(given, classIndex.indexOf(backgroundSymbol));
      if (docLabels.length > docData.length) { // only true for self-training
        // fill the given array with the extra docLabels
        System.arraycopy(docLabels, 0, given, 0, given.length);
        // shift the docLabels array left
        int[] newDocLabels = new int[docData.length];
        System.arraycopy(docLabels, docLabels.length - newDocLabels.length, newDocLabels, 0, newDocLabels.length);
        docLabels = newDocLabels;
      }
      // iterate over the positions in this document
      for (int i = 0; i < docData.length; i++) {
        int label = docLabels[i];
        double p = cliqueTree.condLogProbGivenPrevious(i, label, given);
        if (VERBOSE) {
          System.err.println("P(" + label + "|" + ArrayMath.toString(given) + ")=" + p);
        }
        prob += p;
        System.arraycopy(given, 1, given, 0, given.length - 1);
        given[given.length - 1] = label;
      }

      // compute the expected counts for this document, which we will need to compute the derivative
      // iterate over the positions in this document
      for (int i = 0; i < data[ind].length; i++) {
        // for each possible clique at this position
        for (int j = 0; j < data[ind][i].length; j++) {
          Index labelIndex = labelIndices[j];
          // for each possible labeling for that clique
          for (int k = 0; k < labelIndex.size(); k++) {
            int[] label = ((CRFLabel) labelIndex.get(k)).getLabel();
            double p = cliqueTree.prob(i, label); // probability of these labels occurring in this clique with these features
            for (int n = 0; n < data[ind][i][j].length; n++) {
              E[data[ind][i][j][n]][k] += p;
            }
          }
        }
      }
    }

    if (Double.isNaN(prob)) { // shouldn't be the case
      throw new RuntimeException("Got NaN for prob in CRFLogConditionalObjectiveFunction.calculate()");
    }

    value = -prob;

    // compute the partial derivative for each feature by comparing expected counts to empirical counts
    int index = 0;
    for (int i = 0; i < E.length; i++) {
      for (int j = 0; j < E[i].length; j++) {
        derivative[index++] = (E[i][j] - batchScale*Ehat[i][j]);
        if (VERBOSE) {
          System.err.println("deriv(" + i + "," + j + ") = " + E[i][j] + " - " + Ehat[i][j] + " = " + derivative[index - 1]);
        }
      }
    }


    // incorporate priors
    if (prior == QUADRATIC_PRIOR) {
      double sigmaSq = sigma * sigma;
      for (int i = 0; i < x.length; i++) {
        double k = 1.0;
        double w = x[i];
        value += batchScale*k * w * w / 2.0 / sigmaSq;
        derivative[i] += batchScale*k * w / sigmaSq;
      }
    } else if (prior == HUBER_PRIOR) {
      double sigmaSq = sigma * sigma;
      for (int i = 0; i < x.length; i++) {
        double w = x[i];
        double wabs = Math.abs(w);
        if (wabs < epsilon) {
          value += batchScale*w * w / 2.0 / epsilon / sigmaSq;
          derivative[i] += batchScale*w / epsilon / sigmaSq;
        } else {
          value += batchScale*(wabs - epsilon / 2) / sigmaSq;
          derivative[i] += batchScale*((w < 0.0) ? -1.0 : 1.0) / sigmaSq;
        }
      }
    } else if (prior == QUARTIC_PRIOR) {
      double sigmaQu = sigma * sigma * sigma * sigma;
      for (int i = 0; i < x.length; i++) {
        double k = 1.0;
        double w = x[i];
        value += batchScale*k * w * w * w * w / 2.0 / sigmaQu;
        derivative[i] += batchScale*k * w / sigmaQu;
      }
    }
  }

  /**
   * Performs stochastic update of weights x (scaled by xscale) based
   * on samples indexed by batch.
   * NOTE: This function does not do regularization (regularization is done by the minimizer).
   *
   * @param x - unscaled weights
   * @param xscale - how much to scale x by when performing calculations
   * @param batch - indices of which samples to compute function over
   * @param gscale - how much to scale adjustments to x
   * @return value of function at specified x (scaled by xscale) for samples
   */
  @Override
  public double calculateStochasticUpdate(double[] x, double xscale, int[] batch, double gscale) {
    double prob = 0.0; // the log prob of the sequence given the model, which is the negation of value at this point
    int[][] wis = getWeightIndices();

    // Adjust weight by -gscale*gradient
    // gradient is expected count - empirical count
    // so we adjust by + gscale(empirical count - expected count)

    int[] given = new int[window - 1];
    int[][] docCliqueLabels = new int[window][];
    for (int j = 0; j < window; j++) {
      docCliqueLabels[j] = new int[j+1];
    }
    // iterate over all the documents
    for (int ind : batch) {
      int[][][] docData = data[ind];
      int[] docLabels = labels[ind];

      // make a clique tree for this document
      CRFCliqueTree cliqueTree = CRFCliqueTree.getCalibratedCliqueTree(x, xscale, wis, docData,
              labelIndices, numClasses, classIndex, backgroundSymbol);

      // compute the log probability of the document given the model with the parameters x
      Arrays.fill(given, classIndex.indexOf(backgroundSymbol));
      if (docLabels.length > docData.length) { // only true for self-training
        // fill the given array with the extra docLabels
        System.arraycopy(docLabels, 0, given, 0, given.length);
        // shift the docLabels array left
        int[] newDocLabels = new int[docData.length];
        System.arraycopy(docLabels, docLabels.length - newDocLabels.length, newDocLabels, 0, newDocLabels.length);
        docLabels = newDocLabels;
      }
      // iterate over the positions in this document
      for (int i = 0; i < docData.length; i++) {
        int label = docLabels[i];
        double p = cliqueTree.condLogProbGivenPrevious(i, label, given);
        if (VERBOSE) {
          System.err.println("P(" + label + '|' + ArrayMath.toString(given) + ")=" + p);
        }
        prob += p;

        // Empirical count
        for (int j = 0; j < data[ind][i].length; j++) {
          if (j > 0) {
            System.arraycopy(given, window - j - 1, docCliqueLabels[j], 0, j);
          }
          docCliqueLabels[j][j] = label;
          // TODO: We can eliminate this lookup by saving the correctLabelIndex (or marking it in CRFLabel)
          CRFLabel crfLabel = new CRFLabel(docCliqueLabels[j]);
          int correctLabelIndex = labelIndices[j].indexOf(crfLabel);
          for (int n = 0; n < data[ind][i][j].length; n++) {
            // Adjust by gscale (empirical count)
            x[wis[data[ind][i][j][n]][correctLabelIndex]] += gscale;
          }
        }
        // Shift window over
        System.arraycopy(given, 1, given, 0, given.length - 1);
        given[given.length - 1] = label;
      }

      // compute the expected counts for this document, which we will need to compute the derivative
      // iterate over the positions in this document
      for (int i = 0; i < data[ind].length; i++) {
        // for each possible clique at this position
        for (int j = 0; j < data[ind][i].length; j++) {
          // Expected count
          Index labelIndex = labelIndices[j];
          // for each possible labeling for that clique
          for (int k = 0; k < labelIndex.size(); k++) {
            int[] label = ((CRFLabel) labelIndex.get(k)).getLabel();
            double p = cliqueTree.prob(i, label); // probability of these labels occurring in this clique with these features
            for (int n = 0; n < data[ind][i][j].length; n++) {
              // Adjust weight by -p*gscale (expected count scaled)
              x[wis[docData[i][j][n]][k]] -= p * gscale;
            }
          }
        }
      }
    }

    if (Double.isNaN(prob)) { // shouldn't be the case
      throw new RuntimeException("Got NaN for prob in CRFLogConditionalObjectiveFunction.calculate()");
    }

    value = -prob;
    return value;
  }

  /**
   * Computes value of function for specified value of x (scaled by xscale)
   * only over samples indexed by batch.
   * NOTE: This function does not do regularization (regularization is done by the minimizer).
   *
   * @param x - unscaled weights
   * @param xscale - how much to scale x by when performing calculations
   * @param batch - indices of which samples to compute function over
   * @return value of function at specified x (scaled by xscale) for samples
   */
  @Override
  public double valueAt(double[] x, double xscale, int[] batch) {
    double prob = 0; // the log prob of the sequence given the model, which is the negation of value at this point
    int[][] wis = getWeightIndices();

    int[] given = new int[window - 1];
    int[][] docCliqueLabels = new int[window][];
    for (int j = 0; j < window; j++) {
      docCliqueLabels[j] = new int[j+1];
    }
    // iterate over all the documents
    for (int ind : batch) {
      int[][][] docData = data[ind];
      int[] docLabels = labels[ind];

      // make a clique tree for this document
      CRFCliqueTree cliqueTree = CRFCliqueTree.getCalibratedCliqueTree(x, xscale, wis, docData,
              labelIndices, numClasses, classIndex, backgroundSymbol);

      // compute the log probability of the document given the model with the parameters x
      Arrays.fill(given, classIndex.indexOf(backgroundSymbol));
      if (docLabels.length > docData.length) { // only true for self-training
        // fill the given array with the extra docLabels
        System.arraycopy(docLabels, 0, given, 0, given.length);
        // shift the docLabels array left
        int[] newDocLabels = new int[docData.length];
        System.arraycopy(docLabels, docLabels.length - newDocLabels.length, newDocLabels, 0, newDocLabels.length);
        docLabels = newDocLabels;
      }
      // iterate over the positions in this document
      for (int i = 0; i < docData.length; i++) {
        int label = docLabels[i];
        double p = cliqueTree.condLogProbGivenPrevious(i, label, given);
        if (VERBOSE) {
          System.err.println("P(" + label + '|' + ArrayMath.toString(given) + ")=" + p);
        }
        prob += p;

        // Shift window over
        System.arraycopy(given, 1, given, 0, given.length - 1);
        given[given.length - 1] = label;
      }

    }

    if (Double.isNaN(prob)) { // shouldn't be the case
      throw new RuntimeException("Got NaN for prob in CRFLogConditionalObjectiveFunction.calculate()");
    }

    value = -prob;
    return value;
  }
}
