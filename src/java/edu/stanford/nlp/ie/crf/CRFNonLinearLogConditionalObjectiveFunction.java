package edu.stanford.nlp.ie.crf;

import edu.stanford.nlp.math.ArrayMath;
import edu.stanford.nlp.optimization.AbstractCachingDiffFunction;
import edu.stanford.nlp.sequences.SeqClassifierFlags;
import edu.stanford.nlp.util.Index;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;

import java.util.*;

/**
 * @author Mengqiu Wang
 */

public class CRFNonLinearLogConditionalObjectiveFunction extends AbstractCachingDiffFunction {

  public static final int NO_PRIOR = 0;
  public static final int QUADRATIC_PRIOR = 1;
  /* Use a Huber robust regression penalty (L1 except very near 0) not L2 */
  public static final int HUBER_PRIOR = 2;
  public static final int QUARTIC_PRIOR = 3;
  Index<Integer> nodeFeatureIndicesMap;
  Index<Integer> edgeFeatureIndicesMap;
  boolean useOutputLayer;
  boolean useHiddenLayer;
  boolean useSigmoid;
  SeqClassifierFlags flags;

  int count = 0;
  protected int prior;
  protected double sigma;
  protected double epsilon;
  Random random = new Random(2147483647L);
  /** label indices - for all possible label sequences - for each feature */
  Index<CRFLabel>[] labelIndices;
  Index<String> classIndex;  // didn't have <String> before. Added since that's what is assumed everywhere.
  double[][] Ehat; // empirical counts of all the linear features [feature][class]
  double[][] Uhat; // empirical counts of all the output layer features [num of class][input layer size]
  double[][] What; // empirical counts of all the input layer features [input layer size][featureIndex.size()]
  int window;
  int numClasses;
  // hidden layer number of neuron = numHiddenUnits * numClasses
  int numHiddenUnits;
  int[] map;
  int[][][][] data;  // data[docIndex][tokenIndex][][]
  int[][] docWindowLabels;

  int[][] labels;    // labels[docIndex][tokenIndex]
  int domainDimension = -1;
  int inputLayerSize = -1;
  int outputLayerSize = -1;
  int edgeParamCount = -1;
  int numNodeFeatures = -1;
  int numEdgeFeatures = -1;
  int beforeOutputWeights = -1;
  int originalFeatureCount = -1;

  int[][] weightIndices;

  String backgroundSymbol;

  public static boolean VERBOSE = false;

  public static int getPriorType(String priorTypeStr)
  {
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

  CRFNonLinearLogConditionalObjectiveFunction(int[][][][] data, int[][] labels, int window, Index classIndex, Index[] labelIndices, int[] map, SeqClassifierFlags flags, Index<Integer> nodeFeatureIndicesMap, Index<Integer> edgeFeatureIndicesMap) {
    this(data, labels, window, classIndex, labelIndices, map, QUADRATIC_PRIOR, flags, nodeFeatureIndicesMap, edgeFeatureIndicesMap);
  }

  CRFNonLinearLogConditionalObjectiveFunction(int[][][][] data, int[][] labels, int window, Index<String> classIndex, Index[] labelIndices, int[] map, int prior, SeqClassifierFlags flags, Index<Integer> nodeFeatureIndicesMap, Index<Integer> edgeFeatureIndicesMap) {
    this.window = window;
    this.classIndex = classIndex;
    this.numClasses = classIndex.size();
    this.labelIndices = labelIndices;
    this.data = data;
    this.flags = flags;
    this.map = map;
    this.labels = labels;
    this.prior = prior;
    this.backgroundSymbol = flags.backgroundSymbol;
    this.sigma = flags.sigma;
    this.nodeFeatureIndicesMap = nodeFeatureIndicesMap;
    this.edgeFeatureIndicesMap = edgeFeatureIndicesMap;
    this.outputLayerSize = numClasses;
    this.numHiddenUnits = flags.numHiddenUnits;
    this.inputLayerSize = numHiddenUnits * numClasses;
    this.numNodeFeatures = nodeFeatureIndicesMap.size();
    this.numEdgeFeatures = edgeFeatureIndicesMap.size();
    this.useOutputLayer = flags.useOutputLayer;
    this.useHiddenLayer = flags.useHiddenLayer;
    this.useSigmoid = flags.useSigmoid;
    this.docWindowLabels = new int[data.length][];
    if (!useOutputLayer) {
      System.err.println("Output layer not activated, inputLayerSize must be equal to numClasses, setting it to " + numClasses);
      this.inputLayerSize = numClasses;
    } else if (flags.softmaxOutputLayer && !(flags.sparseOutputLayer || flags.tieOutputLayer)) {
      throw new RuntimeException("flags.softmaxOutputLayer == true, but neither flags.sparseOutputLayer or flags.tieOutputLayer is true");
    }
    empiricalCounts();
  }

  @Override
  public int domainDimension() {
    if (domainDimension < 0) {
      domainDimension = 0;
      edgeParamCount = numEdgeFeatures * labelIndices[1].size();

      originalFeatureCount = 0;
      for (int i = 0; i < map.length; i++) {
        int s = labelIndices[map[i]].size();
        originalFeatureCount += s;
      }

      domainDimension += edgeParamCount;
      domainDimension += inputLayerSize * numNodeFeatures;
      beforeOutputWeights = domainDimension;
      // TODO(mengqiu) temporary fix for debugging
      if (useOutputLayer) {
        if (flags.sparseOutputLayer) {
          domainDimension += outputLayerSize * numHiddenUnits;
        } else if (flags.tieOutputLayer) {
          domainDimension += 1 * numHiddenUnits;
        } else {
          domainDimension += outputLayerSize * inputLayerSize;
        }
      }
      System.err.println("edgeParamCount: "+edgeParamCount);
      System.err.println("originalFeatureCount: "+originalFeatureCount);
      System.err.println("beforeOutputWeights: "+beforeOutputWeights);
      System.err.println("domainDimension: "+domainDimension);
    }
    return domainDimension;
  }

  @Override 
  //TODO(mengqiu) initialize edge feature weights to be weights from CRF
  public double[] initial() {
    double[] initial = new double[domainDimension()];
    // randomly initialize weights
    if (useHiddenLayer || useOutputLayer) {
      double epsilon = 0.1;
      double twoEpsilon = epsilon * 2;
      int count = 0;
      double val = 0;

      if (flags.blockInitialize) {
        for (int i = 0; i < edgeParamCount; i++) {
          val = random.nextDouble() * twoEpsilon - epsilon;
          initial[count++] = val;
        }

        int interval = numNodeFeatures / numHiddenUnits;
        for (int i = 0; i < numHiddenUnits; i++) {
          int lower = i * interval;
          int upper = (i + 1) * interval;
          if (i == numHiddenUnits - 1)
            upper = numNodeFeatures;
          for (int j = 0; j < outputLayerSize; j++) {
            for (int k = 0; k < numNodeFeatures; k++) {
              val = 0;
              if (k >= lower && k < upper) {
                val = random.nextDouble() * twoEpsilon - epsilon;
              }
              initial[count++] = val;
            }
          }
        }
        if (count != beforeOutputWeights) {
          throw new RuntimeException("after blockInitialize, param Index (" + count + ") not equal to beforeOutputWeights (" + beforeOutputWeights + ")");
        }
      } else {
        for (int i = 0; i < beforeOutputWeights; i++) {
          val = random.nextDouble() * twoEpsilon - epsilon;
          initial[count++] = val;
        }
      }

      if (flags.sparseOutputLayer) {
        for (int i = 0; i < outputLayerSize; i++) {
          double total = 1;
          for (int j = 0; j < numHiddenUnits-1; j++) {
            val = random.nextDouble() * total;
            initial[count++] = val;
            total -= val;
          }
          initial[count++] = total;
        }
      } else if (flags.tieOutputLayer) {
        double total = 1;
        double sum = 0;
        for (int j = 0; j < numHiddenUnits-1; j++) {
          val = random.nextDouble() * total;
          initial[count++] = val;
          total -= val;
        }
        initial[count++] = total;
      } else {
        for (int i = beforeOutputWeights; i < domainDimension(); i++) {
          val = random.nextDouble() * twoEpsilon - epsilon;
          initial[count++] = val;
        }
      }
      if (count != domainDimension()) {
        throw new RuntimeException("after param initialization, param Index (" + count + ") not equal to domainDimension (" + domainDimension() + ")");
      }
    }
    return initial;
  }

  private void empiricalCounts() {
    Ehat = empty2D();

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
        // for (int j = 1; j < docData[i].length; j++) { // j starting from 1, skip all node features
        //TODO(mengqiu) generalize this for bigger cliques
        int j = 1;
        int[] cliqueLabel = new int[j + 1];
        System.arraycopy(windowLabels, window - 1 - j, cliqueLabel, 0, j + 1);
        CRFLabel crfLabel = new CRFLabel(cliqueLabel);
        int labelIndex = labelIndices[j].indexOf(crfLabel);
        int[] cliqueFeatures = docData[i][j];
        //System.err.println(crfLabel + " " + labelIndex);
        for (int n = 0; n < cliqueFeatures.length; n++) {
          // Ehat[docData[i][j][k]][labelIndex]++;
          Ehat[cliqueFeatures[n]][labelIndex]++;
        }
      }
    }
  }

  private double[][] emptyU() {
    int innerSize = inputLayerSize;
    if (flags.sparseOutputLayer || flags.tieOutputLayer) {
      innerSize = numHiddenUnits;
    }
    int outerSize = outputLayerSize;
    if (flags.tieOutputLayer) {
      outerSize = 1;
    }

    double[][] temp = new double[outerSize][innerSize];
    for (int i = 0; i < outerSize; i++) {
      temp[i] = new double[innerSize];
    }
    return temp;
  }

  private double[][] emptyW() {
    // TODO(mengqiu) temporary fix for debugging
    double[][] temp = new double[inputLayerSize][numNodeFeatures];
    for (int i = 0; i < inputLayerSize; i++) {
      temp[i] = new double[numNodeFeatures];
    }
    return temp;
  }

  public Triple<double[][], double[][], double[][]> separateWeights(double[] x) {
    double[] linearWeights = new double[edgeParamCount];
    System.arraycopy(x, 0, linearWeights, 0, edgeParamCount);
    double[][] linearWeights2D = to2D(linearWeights);
    int index = edgeParamCount;

    double[][] inputLayerWeights = emptyW();
    for (int i = 0; i < inputLayerWeights.length; i++) {
      for (int j = 0; j < inputLayerWeights[i].length; j++) {
        inputLayerWeights[i][j] = x[index++];
      }
    }

    double[][] outputLayerWeights = emptyU();
    for (int i = 0; i < outputLayerWeights.length; i++) {
      for (int j = 0; j < outputLayerWeights[i].length; j++) {
        // TODO(mengqiu) temporary fix for debugging
        if (useOutputLayer)
          outputLayerWeights[i][j] = x[index++];
        else
          outputLayerWeights[i][j] = 1;
      }
    }
    assert(index == x.length);
    return new Triple<double[][], double[][], double[][]>(linearWeights2D, inputLayerWeights, outputLayerWeights);
  }

  // todo [cdm]: Below data[m] --> docData
  /**
   * Calculates both value and partial derivatives at the point x, and save them internally.
   */
  @Override
  public void calculate(double[] x) {

    double prob = 0.0; // the log prob of the sequence given the model, which is the negation of value at this point
    Triple<double[][], double[][], double[][]> allParams = separateWeights(x);
    double[][] linearWeights = allParams.first();
    double[][] W = allParams.second(); // inputLayerWeights 
    double[][] U = allParams.third(); // outputLayerWeights 

    double[][] Y = null;
    if (flags.softmaxOutputLayer) {
      Y = new double[U.length][];
      for (int i = 0; i < U.length; i++) {
        Y[i] = ArrayMath.softmax(U[i]);
      }
    }

    double[][] What = emptyW();
    double[][] Uhat = emptyU();

    // the expectations over counts
    // first index is feature index, second index is of possible labeling
    double[][] E = empty2D();
    double[][] eW = emptyW();
    double[][] eU = emptyU();

    // iterate over all the documents
    for (int m = 0; m < data.length; m++) {
      int[][][] docData = data[m];
      int[] docLabels = labels[m];

      // make a clique tree for this document
      CRFCliqueTree cliqueTree = CRFCliqueTree.getCalibratedCliqueTree(docData, labelIndices, numClasses, classIndex,
        backgroundSymbol, new NonLinearCliquePotentialFunction(linearWeights, W, U, flags));

      // compute the log probability of the document given the model with the parameters x
      int[] given = new int[window - 1];
      Arrays.fill(given, classIndex.indexOf(backgroundSymbol));
      int[] windowLabels = new int[window];
      Arrays.fill(windowLabels, classIndex.indexOf(backgroundSymbol));

      if (docLabels.length>docData.length) { // only true for self-training
        // fill the given array with the extra docLabels
        System.arraycopy(docLabels, 0, given, 0, given.length);
        System.arraycopy(docLabels, 0, windowLabels, 0, windowLabels.length);
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
        System.arraycopy(given, 1, given, 0, given.length - 1);
        given[given.length - 1] = label;
      }

      // compute the expected counts for this document, which we will need to compute the derivative
      // iterate over the positions in this document
      for (int i = 0; i < docData.length; i++) {
        // for each possible clique at this position
        System.arraycopy(windowLabels, 1, windowLabels, 0, window - 1);
        windowLabels[window - 1] = docLabels[i];
        for (int j = 0; j < docData[i].length; j++) {
          Index<CRFLabel> labelIndex = labelIndices[j];
          // for each possible labeling for that clique
          int[] cliqueFeatures = docData[i][j];
          double[] As = null;
          double[] fDeriv = null;
          double[][] yTimesA = null;
          double[] sumOfYTimesA = null;

          if (j == 0) {
            As = NonLinearCliquePotentialFunction.hiddenLayerOutput(W, cliqueFeatures, flags);
            fDeriv = new double[inputLayerSize];
            double fD = 0;
            for (int q = 0; q < inputLayerSize; q++) {
              if (useSigmoid) {
                fD = As[q] * (1 - As[q]); 
              } else {
                fD = 1 - As[q] * As[q]; 
              }
              fDeriv[q] = fD;
            }

            // calculating yTimesA for softmax
            if (flags.softmaxOutputLayer) {
              double val = 0;

              yTimesA = new double[outputLayerSize][numHiddenUnits];
              for (int ii = 0; ii < outputLayerSize; ii++) {
                yTimesA[ii] = new double[numHiddenUnits];
              }
              sumOfYTimesA = new double[outputLayerSize];

              for (int k = 0; k < outputLayerSize; k++) {
                double[] Yk = null;
                if (flags.tieOutputLayer) {
                  Yk = Y[0];
                } else {
                  Yk = Y[k];
                }
                double sum = 0;
                for (int q = 0; q < inputLayerSize; q++) {
                  if (q % outputLayerSize == k) {
                    int hiddenUnitNo = q / outputLayerSize;
                    val = As[q] * Yk[hiddenUnitNo];
                    yTimesA[k][hiddenUnitNo] = val;
                    sum += val;
                  }
                }
                sumOfYTimesA[k] = sum;
              }
            }

            // calculating Uhat What
            int[] cliqueLabel = new int[j + 1];
            System.arraycopy(windowLabels, window - 1 - j, cliqueLabel, 0, j + 1);

            CRFLabel crfLabel = new CRFLabel(cliqueLabel);
            int givenLabelIndex = labelIndex.indexOf(crfLabel);
            double[] Uk = null;
            double[] UhatK = null;
            double[] Yk = null;
            double[] yTimesAK = null;
            double sumOfYTimesAK = 0;
            if (flags.tieOutputLayer) {
              Uk = U[0];
              UhatK = Uhat[0];
              if (flags.softmaxOutputLayer) {
                Yk = Y[0];
              }
            } else {
              Uk = U[givenLabelIndex];
              UhatK = Uhat[givenLabelIndex];
              if (flags.softmaxOutputLayer) {
                Yk = Y[givenLabelIndex];
              }
            }

            if (flags.softmaxOutputLayer) {
              yTimesAK = yTimesA[givenLabelIndex];
              sumOfYTimesAK = sumOfYTimesA[givenLabelIndex];
            }

            for (int k = 0; k < inputLayerSize; k++) {
              double deltaK = 1;
              if (flags.sparseOutputLayer || flags.tieOutputLayer) {
                if (k % outputLayerSize == givenLabelIndex) {
                  int hiddenUnitNo = k / outputLayerSize;
                  if (flags.softmaxOutputLayer) {
                    UhatK[hiddenUnitNo] += (yTimesAK[hiddenUnitNo] - Yk[hiddenUnitNo] * sumOfYTimesAK);
                    deltaK *= Yk[hiddenUnitNo];
                  } else {
                    UhatK[hiddenUnitNo] += As[k];
                    deltaK *= Uk[hiddenUnitNo];
                  }
                }
              } else {
                UhatK[k] += As[k];
                if (useOutputLayer) {
                  deltaK *= Uk[k];
                }
              }
              if (useHiddenLayer)
                deltaK *= fDeriv[k];
              if (useOutputLayer) {
                if (flags.sparseOutputLayer || flags.tieOutputLayer) {
                  if (k % outputLayerSize == givenLabelIndex) {
                    double[] WhatK = What[k];
                    for (int n = 0; n < cliqueFeatures.length; n++) {
                      WhatK[cliqueFeatures[n]] += deltaK;
                    }
                  }
                } else {
                  double[] WhatK = What[k];
                  for (int n = 0; n < cliqueFeatures.length; n++) {
                    WhatK[cliqueFeatures[n]] += deltaK;
                  }
                }
              } else {
                if (k == givenLabelIndex) {
                  double[] WhatK = What[k];
                  for (int n = 0; n < cliqueFeatures.length; n++) {
                    WhatK[cliqueFeatures[n]] += deltaK;
                  }
                }
              }
            }
          }

          for (int k = 0; k < labelIndex.size(); k++) { // labelIndex.size() == numClasses
            int[] label = labelIndex.get(k).getLabel();
            double p = cliqueTree.prob(i, label); // probability of these labels occurring in this clique with these features
            if (j == 0) { // for node features
              double[] Uk = null;
              double[] eUK = null;
              double[] Yk = null;
              if (flags.tieOutputLayer) {
                Uk = U[0];
                eUK = eU[0];
                if (flags.softmaxOutputLayer) {
                  Yk = Y[0];
                }
              } else {
                Uk = U[k];
                eUK = eU[k];
                if (flags.softmaxOutputLayer) {
                  Yk = Y[k];
                }
              }
              if (useOutputLayer) {
                for (int q = 0; q < inputLayerSize; q++) {
                  double deltaQ = 1;
                  if (flags.sparseOutputLayer || flags.tieOutputLayer) {
                    if (q % outputLayerSize == k) {
                      int hiddenUnitNo = q / outputLayerSize;
                      if (flags.softmaxOutputLayer) {
                        eUK[hiddenUnitNo] += (yTimesA[k][hiddenUnitNo] - Yk[hiddenUnitNo] * sumOfYTimesA[k]) * p;
                        deltaQ = Yk[hiddenUnitNo];
                      } else {
                        eUK[hiddenUnitNo] += As[q] * p;
                        deltaQ = Uk[hiddenUnitNo];
                      }
                    }
                  } else {
                    eUK[q] += As[q] * p;
                    deltaQ = Uk[q];
                  }
                  if (useHiddenLayer)
                    deltaQ *= fDeriv[q];
                  if (flags.sparseOutputLayer || flags.tieOutputLayer) {
                    if (q % outputLayerSize == k) {
                      double[] eWq = eW[q];
                      for (int n = 0; n < cliqueFeatures.length; n++) {
                        eWq[cliqueFeatures[n]] += deltaQ * p;
                      }
                    }
                  } else {
                    double[] eWq = eW[q];
                    for (int n = 0; n < cliqueFeatures.length; n++) {
                      eWq[cliqueFeatures[n]] += deltaQ * p;
                    }
                  }
                }
              } else {
                double deltaK = 1;
                if (useHiddenLayer)
                  deltaK *= fDeriv[k];
                double[] eWK = eW[k];
                for (int n = 0; n < cliqueFeatures.length; n++) {
                  eWK[cliqueFeatures[n]] += deltaK * p;
                }
              }
            } else { // for edge features
              for (int n = 0; n < cliqueFeatures.length; n++) {
                E[cliqueFeatures[n]][k] += p;
              }
            }
          }
        }
      }
    }

    if (Double.isNaN(prob)) { // shouldn't be the case
      throw new RuntimeException("Got NaN for prob in CRFNonLinearLogConditionalObjectiveFunction.calculate()");
    }

    value = -prob;
    if(VERBOSE){
      System.err.println("value is " + value);
    }

    // compute the partial derivative for each feature by comparing expected counts to empirical counts
    int index = 0;
    for (int i = 0; i < E.length; i++) {
      int originalIndex = edgeFeatureIndicesMap.get(i);

      for (int j = 0; j < E[i].length; j++) {
        derivative[index++] = (E[i][j] - Ehat[i][j]);
        if (VERBOSE) {
          System.err.println("linearWeights deriv(" + i + "," + j + ") = " + E[i][j] + " - " + Ehat[i][j] + " = " + derivative[index - 1]);
        }
      }
    }
    if (index != edgeParamCount)
      throw new RuntimeException("after edge derivative, index("+index+") != edgeParamCount("+edgeParamCount+")");

    for (int i = 0; i < eW.length; i++) {
      for (int j = 0; j < eW[i].length; j++) {
        derivative[index++] = (eW[i][j] - What[i][j]);
        if (VERBOSE) {
          System.err.println("inputLayerWeights deriv(" + i + "," + j + ") = " + eW[i][j] + " - " + What[i][j] + " = " + derivative[index - 1]);
        }
      }
    }


    if (index != beforeOutputWeights)
      throw new RuntimeException("after W derivative, index("+index+") != beforeOutputWeights("+beforeOutputWeights+")");

    if (useOutputLayer) {
      for (int i = 0; i < eU.length; i++) {
        for (int j = 0; j < eU[i].length; j++) {
          derivative[index++] = (eU[i][j] - Uhat[i][j]);
          if (VERBOSE) {
            System.err.println("outputLayerWeights deriv(" + i + "," + j + ") = " + eU[i][j] + " - " + Uhat[i][j] + " = " + derivative[index - 1]);
          }
        }
      }
    }

    if (index != x.length)
      throw new RuntimeException("after W derivative, index("+index+") != x.length("+x.length+")");

    int regSize = x.length;
    if (flags.skipOutputRegularization || flags.softmaxOutputLayer) {
      regSize = beforeOutputWeights;
    }

    // incorporate priors
    if (prior == QUADRATIC_PRIOR) {
      double sigmaSq = sigma * sigma;
      for (int i = 0; i < regSize; i++) {
        double k = 1.0;
        double w = x[i];
        value += k * w * w / 2.0 / sigmaSq;
        derivative[i] += k * w / sigmaSq;
      }
    } else if (prior == HUBER_PRIOR) {
      double sigmaSq = sigma * sigma;
      for (int i = 0; i < regSize; i++) {
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
      for (int i = 0; i < regSize; i++) {
        double k = 1.0;
        double w = x[i];
        value += k * w * w * w * w / 2.0 / sigmaQu;
        derivative[i] += k * w / sigmaQu;
      }
    }
  }

  public double[][] to2D(double[] linearWeights) {
    double[][] newWeights = new double[numEdgeFeatures][];
    int index = 0;
    int labelIndicesSize = labelIndices[1].size();
    for (int i = 0; i < numEdgeFeatures; i++) {
      newWeights[i] = new double[labelIndicesSize];
      System.arraycopy(linearWeights, index, newWeights[i], 0, labelIndicesSize);
      index += labelIndicesSize;
    }
    return newWeights;
  }

  public double[][] empty2D() {
    double[][] d = new double[numEdgeFeatures][];
    // int index = 0;
    int labelIndicesSize = labelIndices[1].size();
    for (int i = 0; i < numEdgeFeatures; i++) {
      d[i] = new double[labelIndicesSize];
      // cdm july 2005: below array initialization isn't necessary: JLS (3rd ed.) 4.12.5
      // Arrays.fill(d[i], 0.0);
      // index += labelIndices[map[i]].size();
    }
    return d;
  }

  public double[][] emptyFull2D() {
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
}
