package edu.stanford.nlp.sequences;

import edu.stanford.nlp.util.concurrent.*;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.math.ArrayMath;
import edu.stanford.nlp.util.StringUtils;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.PrintStream;

// TODO: change so that it uses the scoresOf() method properly

/**
 * A Gibbs sampler for sequence models. Given a sequence model implementing the SequenceModel
 * interface, this class is capable of
 * sampling sequences from the distribution over sequences that it defines. It can also use
 * this sampling procedure to find the best sequence.
 * @author grenager
 */
public class SequenceGibbsSampler implements BestSequenceFinder {

  // a random number generator
  private static Random random = new Random(2147483647L);
  public static int verbose = 0;

  private List document;
  private int numSamples;
  private int sampleInterval;
  private SequenceListener listener;
  private static final int RANDOM_SAMPLING = 0;
  private static final int SEQUENTIAL_SAMPLING = 1;
  private static final int CHROMATIC_SAMPLING = 2;

  public boolean returnLastFoundSequence = false;
  private int samplingStyle;
  // determines how many parallel threads to run in chromatic sampling
  private int chromaticSize;
  private List<List<Integer>> partition;

  public static int[] copy(int[] a) {
    int[] result = new int[a.length];
    System.arraycopy(a, 0, result, 0, a.length);
    return result;
  }

  public static int[] getRandomSequence(SequenceModel model) {
    int[] result = new int[model.length()];
    for (int i = 0; i < result.length; i++) {
      int[] classes = model.getPossibleValues(i);
      result[i] = classes[random.nextInt(classes.length)];
    }
    return result;
  }

  /**
   * Finds the best sequence by collecting numSamples samples, scoring them, and then choosing
   * the highest scoring sample.
   * @return the array of type int representing the highest scoring sequence
   */
  public int[] bestSequence(SequenceModel model) {
    int[] initialSequence = getRandomSequence(model);
    return findBestUsingSampling(model, numSamples, sampleInterval, initialSequence);
  }

  /**
   * Finds the best sequence by collecting numSamples samples, scoring them, and then choosing
   * the highest scoring sample.
   * @return the array of type int representing the highest scoring sequence
   */
  public int[] findBestUsingSampling(SequenceModel model, int numSamples, int sampleInterval, int[] initialSequence) {
    List samples = collectSamples(model, numSamples, sampleInterval, initialSequence);
    int[] best = null;
    double bestScore = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < samples.size(); i++) {
      int[] sequence = (int[]) samples.get(i);
      double score = model.scoreOf(sequence);
      if (score>bestScore) {
        best = sequence;
        bestScore = score;
        System.err.println("found new best ("+bestScore+")");
        System.err.println(ArrayMath.toString(best));
      }
    }
    return best;
  }

  public int[] findBestUsingAnnealing(SequenceModel model, CoolingSchedule schedule) {
    int[] initialSequence = getRandomSequence(model);
    return findBestUsingAnnealing(model, schedule, initialSequence);
  }

  public int[] findBestUsingAnnealing(SequenceModel model, CoolingSchedule schedule, int[] initialSequence) {
    if (verbose>0) System.err.println("Doing annealing");
    listener.setInitialSequence(initialSequence);
    List result = new ArrayList();
    int[] sequence = initialSequence;
    int[] best = null;
    double bestScore = Double.NEGATIVE_INFINITY;
    double score = Double.NEGATIVE_INFINITY;
    if (!returnLastFoundSequence) {
      score = model.scoreOf(sequence);
    }

    for (int i=0; i<schedule.numIterations(); i++) {
      sequence = copy(sequence); // so we don't change the initial, or the one we just stored
      double temperature = schedule.getTemperature(i);
      sampleSequenceForward(model, sequence, temperature); // modifies tagSequence
      result.add(sequence);
      if (returnLastFoundSequence) {
        best = sequence;
      } else {
        score = model.scoreOf(sequence);
        //System.err.println(i+" "+score+" "+Arrays.toString(sequence));
        if (score>bestScore) {
          best = sequence;
          bestScore = score;
        }      
      }
      if (verbose>0) System.err.print(".");
    }
    if (verbose>1) {
      System.err.println();
      printSamples(result, System.err);
    }
    if (verbose>0) System.err.println("done.");
    //return sequence;
    return best;
  }

  /**
   * Collects numSamples samples of sequences, from the distribution over sequences defined
   * by the sequence model passed on construction.
   * All samples collected are sampleInterval samples apart, in an attempt to reduce
   * autocorrelation.
   * @return a List containing the sequence samples, as arrays of type int, and their scores
   */
  public List<int[]> collectSamples(SequenceModel model, int numSamples, int sampleInterval) {
    int[] initialSequence = getRandomSequence(model);
    return collectSamples(model, numSamples, sampleInterval, initialSequence);
  }

  /**
   * Collects numSamples samples of sequences, from the distribution over sequences defined
   * by the sequence model passed on construction.
   * All samples collected are sampleInterval samples apart, in an attempt to reduce
   * autocorrelation.
   * @return a Counter containing the sequence samples, as arrays of type int, and their scores
   */
  public List<int[]> collectSamples(SequenceModel model, int numSamples, int sampleInterval, int[] initialSequence) {
    if (verbose>0) System.err.print("Collecting samples");
    listener.setInitialSequence(initialSequence);
    List<int[]> result = new ArrayList<int[]>();
    int[] sequence = initialSequence;
    for (int i=0; i<numSamples; i++) {
      sequence = copy(sequence); // so we don't change the initial, or the one we just stored
      sampleSequenceRepeatedly(model, sequence, sampleInterval); // modifies tagSequence
      result.add(sequence); // save it to return later
      if (verbose>0) System.err.print(".");
      System.err.flush();
    }
    if (verbose>1) {
      System.err.println();
      printSamples(result, System.err);
    }
    if (verbose>0) System.err.println("done.");
    return result;
  }

  /**
   * Samples the sequence repeatedly, making numSamples passes over the entire sequence.
   */
  public void sampleSequenceRepeatedly(SequenceModel model, int[] sequence, int numSamples) {
    sequence = copy(sequence); // so we don't change the initial, or the one we just stored
    listener.setInitialSequence(sequence);
    for (int iter=0; iter<numSamples; iter++) {
      sampleSequenceForward(model, sequence);
    }
  }

  /**
   * Samples the sequence repeatedly, making numSamples passes over the entire sequence.
   * Destructively modifies the sequence in place.
   */
  public void sampleSequenceRepeatedly(SequenceModel model, int numSamples) {
    int[] sequence = getRandomSequence(model);
    sampleSequenceRepeatedly(model, sequence, numSamples);
  }

  /**
   * Samples the complete sequence once in the forward direction
   * Destructively modifies the sequence in place.
   * @param sequence the sequence to start with.
   */
  public void sampleSequenceForward(SequenceModel model, int[] sequence) {
    sampleSequenceForward(model, sequence, 1.0);
  }
  /**
   * Samples the complete sequence once in the forward direction
   * Destructively modifies the sequence in place.
   * @param sequence the sequence to start with.
   */
  public void sampleSequenceForward(final SequenceModel model, final int[] sequence, final double temperature) {
    // System.err.println("Sampling forward");
    if (samplingStyle == SEQUENTIAL_SAMPLING) {
      for (int pos=0; pos<sequence.length; pos++) {
        samplePosition(model, sequence, pos, temperature);
      }
    } else if (samplingStyle == RANDOM_SAMPLING) {
      for (int itr=0; itr<sequence.length; itr++) {
        int pos = random.nextInt(sequence.length);
        samplePosition(model, sequence, pos, temperature);
      }
    } else if (samplingStyle == CHROMATIC_SAMPLING) {
      // make copies of the sequences and merge at the end
      List<Pair<Integer, Integer>> results = new ArrayList<Pair<Integer, Integer>>();
      for (List<Integer> indieList: partition) {
        if (indieList.size() <= chromaticSize) {
          for (int pos: indieList) {
            Pair<Integer, Double> newPosProb = samplePositionHelper(model, sequence, pos, temperature); 
            sequence[pos] = newPosProb.first();
          }
        } else {
          MulticoreWrapper<List<Integer>, List<Pair<Integer, Integer>>> wrapper = new MulticoreWrapper<List<Integer>, List<Pair<Integer, Integer>>>(chromaticSize, 
              new ThreadsafeProcessor<List<Integer>, List<Pair<Integer, Integer>>>() {
            @Override
            public List<Pair<Integer, Integer>> process(List<Integer> posList) {
              List<Pair<Integer, Integer>> allPos = new ArrayList<Pair<Integer, Integer>>(posList.size());
              Pair<Integer, Double> newPosProb = null;
              for (int pos: posList) {
                newPosProb = samplePositionHelper(model, sequence, pos, temperature); 
                // returns the position to sample in first place and new label in second place
                allPos.add(new Pair<Integer, Integer>(pos, newPosProb.first()));
              }
              return allPos;
            }
            @Override
            public ThreadsafeProcessor<List<Integer>, List<Pair<Integer, Integer>>> newInstance() {
              return this;
            }
          });
          results.clear();
          int interval = Math.max(1, indieList.size() / chromaticSize);
          for (int begin = 0, end = 0, indieListSize = indieList.size(); end < indieListSize; begin += interval) {
            end = Math.min(begin + interval, indieListSize);
            wrapper.submit(indieList.subList(begin, end));
            while (wrapper.hasNext()) {
              results.addAll(wrapper.next());
            }
          }
          wrapper.join();
          while (wrapper.hasNext()) {
            results.addAll(wrapper.next());
          }
          for(Pair<Integer, Integer> posVal : results) {
            sequence[posVal.first()] = posVal.second();
          }
        }
      }
    }
  }

  /**
   * Samples the complete sequence once in the backward direction
   * Destructively modifies the sequence in place.
   * @param sequence the sequence to start with.
   */
  public void sampleSequenceBackward(SequenceModel model, int[] sequence) {
    sampleSequenceBackward(model, sequence, 1.0);
  }
  /**
   * Samples the complete sequence once in the backward direction
   * Destructively modifies the sequence in place.
   * @param sequence the sequence to start with.
   */
  public void sampleSequenceBackward(SequenceModel model, int[] sequence, double temperature) {
    for (int pos=sequence.length-1; pos>=0; pos--) {
      samplePosition(model, sequence, pos, temperature);
    }
  }

  /**
   * Samples a single position in the sequence.
   * Destructively modifies the sequence in place.
   * returns the score of the new sequence
   * @param sequence the sequence to start with
   * @param pos the position to sample.
   */
  public double samplePosition(SequenceModel model, int[] sequence, int pos) {
    return samplePosition(model, sequence, pos, 1.0);
  }

  /**
   * Samples a single position in the sequence.
   * Does not modify the sequence passed in.
   * returns the score of the new label for the position to sample
   * @param sequence the sequence to start with
   * @param pos the position to sample.
   * @param temperature the temperature to control annealing
   */
  private Pair<Integer, Double> samplePositionHelper(SequenceModel model, int[] sequence, int pos, double temperature) {
    double[] distribution = model.scoresOf(sequence, pos);
    if (temperature!=1.0) {
      if (temperature==0.0) {
        // set the max to 1.0
        int argmax = ArrayMath.argmax(distribution);
        Arrays.fill(distribution, Double.NEGATIVE_INFINITY);
        distribution[argmax] = 0.0;
      } else {
        // take all to a power
        // use the temperature to increase/decrease the entropy of the sampling distribution
        ArrayMath.multiplyInPlace(distribution, 1.0/temperature);
      }
    }
    ArrayMath.logNormalize(distribution);
    ArrayMath.expInPlace(distribution);
    int newTag = ArrayMath.sampleFromDistribution(distribution, random);
    double newProb = distribution[newTag];
    return new Pair<Integer, Double>(newTag, newProb);
  }

  /**
   * Samples a single position in the sequence.
   * Destructively modifies the sequence in place.
   * returns the score of the new sequence
   * @param sequence the sequence to start with
   * @param pos the position to sample.
   * @param temperature the temperature to control annealing
   */
  public double samplePosition(SequenceModel model, int[] sequence, int pos, double temperature) {
    Pair<Integer, Double> newPosProb = samplePositionHelper(model, sequence, pos, temperature); 
    int newTag = newPosProb.first();
    int oldTag = sequence[pos];
//    System.out.println("Sampled " + oldTag + "->" + newTag);
    sequence[pos] = newTag;
    listener.updateSequenceElement(sequence, pos, oldTag);
    return newPosProb.second();
  }

  public void printSamples(List samples, PrintStream out) {
    for (int i = 0; i < document.size(); i++) {
      HasWord word = (HasWord) document.get(i);
      String s = "null";
      if (word!=null) {
        s = word.word();
      }
      out.print(StringUtils.padOrTrim(s, 10));
      for (int j = 0; j < samples.size(); j++) {
        int[] sequence = (int[]) samples.get(j);
        out.print(" " + StringUtils.padLeft(sequence[i], 2));
      }
      out.println();
    }
  }

  /**
   * @param document the underlying document which is a list of HasWord; a slight abstraction violation, but useful for debugging!!
   */
  public SequenceGibbsSampler(int numSamples, int sampleInterval, SequenceListener listener, List document,
      boolean returnLastFoundSequence, int samplingStyle, int chromaticSize, List<List<Integer>> partition) {
    this.numSamples = numSamples;
    this.sampleInterval = sampleInterval;
    this.listener = listener;
    this.document = document;
    this.returnLastFoundSequence = returnLastFoundSequence;
    this.samplingStyle = samplingStyle;
    if (verbose > 0) {
      if (samplingStyle == RANDOM_SAMPLING) {
        System.err.println("Using random sampling");
      } else if (samplingStyle == CHROMATIC_SAMPLING) {
        System.err.println("Using chromatic sampling with " + chromaticSize + " threads");
      } else if (samplingStyle == SEQUENTIAL_SAMPLING) {
        System.err.println("Using sequential sampling");
      }
    }
    this.chromaticSize = chromaticSize;
    this.partition = partition;
  }

  public SequenceGibbsSampler(int numSamples, int sampleInterval, SequenceListener listener, List document) {
    this(numSamples, sampleInterval, listener, document, false, 1, 0, null);
  }

  public SequenceGibbsSampler(int numSamples, int sampleInterval, SequenceListener listener) {
    this(numSamples, sampleInterval, listener, null);
  }

  public SequenceGibbsSampler(int numSamples, int sampleInterval, SequenceListener listener, int samplingStyle, int chromaticSize, List<List<Integer>> partition) {
    this(numSamples, sampleInterval, listener, null, false, samplingStyle, chromaticSize, partition);
  }
}
