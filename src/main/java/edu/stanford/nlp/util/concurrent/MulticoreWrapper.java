package edu.stanford.nlp.util.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Provides convenient multicore processing for threadsafe objects. Objects that can
 * be wrapped by MulticoreWrapper must implement the ThreadsafeProcessor interface.
 *
 * TODO(spenceg): Make lastSubmittedId and lastReturnedId threadsafe. Should support independent threads
 * writing and reading from the queue.
 * 
 * @author Spence Green
 *
 * @param <I> input type
 * @param <O> output type
 */
public class MulticoreWrapper<I,O> {

  private final int nThreads;
  private int lastSubmittedId = 0;
  private int lastReturnedId = -1;
  private final PriorityBlockingQueue<QueueItem<O>> outputQueue;

  private final ExecutorService threadPool;
  private final List<Future<O>> submits;
  private final int[] submitIds;
  private final List<ThreadsafeProcessor<I,O>> processorList;
  
  /**
   * Print warnings if outputQueue exceeds this size.
   */
  private static final int WARN_AFTER_QUEUE_SIZE = 1000;

  private final Random random = new Random();

  public MulticoreWrapper(int nThreads, ThreadsafeProcessor<I,O> processor) {
    this.nThreads = nThreads;
    outputQueue = new PriorityBlockingQueue<QueueItem<O>>(10*nThreads);
    threadPool = Executors.newFixedThreadPool(nThreads);
    submits = new ArrayList<Future<O>>(nThreads);
    submitIds = new int[nThreads];
    processorList = new ArrayList<ThreadsafeProcessor<I,O>>(nThreads);

    processorList.add(processor);
    submits.add(null);
    for (int i = 1; i < nThreads; ++i) {
      processorList.add(processor.newInstance());
      submits.add(null);
    }
  }

  /**
   * Allocate instance to a process and return. This call blocks until I
   * can be assigned to a thread.
   *
   * @param inputInstance
   */
  public void submit(I inputInstance) {
    while(true) {
      for (int i = 0; i < nThreads; ++i) {
        if (submits.get(i) != null && submits.get(i).isDone()) {
          blockingGetResult(i);
        }
        if (submits.get(i) == null) {
          submitIds[i] = lastSubmittedId++;
          submits.set(i, threadPool.submit(new CallableJob<I,O>(inputInstance, processorList.get(i))));
          return;
        }
      }
      // Randomly choose a process to wait on, then loop again.
      // At least this process will have finished.
      blockingGetResult(random.nextInt(nThreads));
   
      // Check the output queue size
      if (outputQueue.size() > WARN_AFTER_QUEUE_SIZE) {
        System.err.printf("%s: WARNING: Output queue contains %d items.%n", 
            this.getClass().getName(), 
            outputQueue.size());
      }
    }
  }

  /**
   * Block until process specified by processorId returns a result.
   *
   * @param processorId
   */
  private void blockingGetResult(final int processorId) {
    if (submits.get(processorId) == null) {
      // Silently ignore redundant calls from join()
      return;
    }
    try {
      // Future.get() is a blocking call
      O result = submits.get(processorId).get();
      QueueItem<O> resultItem = new QueueItem<O>(result, submitIds[processorId]);
      outputQueue.add(resultItem);
      submits.set(processorId, null);

    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
    }
  }

  /**
   * Waits for all active processes to finish, then returns true only if all
   * results have been returned via calls to next().
   *
   * @return True on successful shutdown, false otherwise.
   */
  public boolean join() {
    // Make blocking calls to the last processes that are running
    for (int i = 0; i < nThreads; ++i) {
      blockingGetResult(i);
    }
    threadPool.shutdown();
    return lastSubmittedId-1 == lastReturnedId;
  }

  /**
   * Indicates whether a not a new result is available.
   *
   * @return true if a new result is available, false otherwise.
   */
  public boolean hasNext() {
    if (outputQueue.isEmpty()) {
      return false;
    } else {
      // Only return true if the top of the queue is the next item in the sequence
      int nextId = outputQueue.peek().getId();
      return nextId == lastReturnedId + 1;
    }
  }

  /**
   * Returns the next available result.
   *
   * @return
   */
  public O next() {
    if (!hasNext()) return null;
    lastReturnedId++;
    QueueItem<O> result = outputQueue.poll();
    return result.getItem();
  }

  /**
   * Internal class for adding a job to the thread pool.
   * 
   * @author Spence Green
   *
   * @param <I>
   * @param <O>
   */
  private static class CallableJob<I,O> implements Callable<O> {

    private final I item;
    private final ThreadsafeProcessor<I,O> processor;

    public CallableJob(I item, ThreadsafeProcessor<I,O> processor) {
      this.item = item;
      this.processor = processor;
    }

    @Override
    public O call() throws Exception {
      return processor.process(item);
    }
  }

  /**
   * Internal class for storing results of type O in a min queue.
   *
   * @author Spence Green
   *
   * @param <O>
   */
  private static class QueueItem<O> implements Comparable<QueueItem<O>> {

    private final int id;
    private final O item;

    public QueueItem(O item, int id) {
      this.item = item;
      this.id = id;
    }

    public int getId() { return id; }

    public O getItem() { return item; }

    @Override
    public int compareTo(QueueItem<O> other) {
      return this.id - other.id;
    }

    @SuppressWarnings("unchecked")
    public boolean equals(Object other) {
      if (other == this) return true;
      if ( ! (other instanceof QueueItem)) return false;
      QueueItem<O> otherQueue = (QueueItem<O>) other;
      return this.id == otherQueue.id;
    }

    public int hashCode() {
      return id;
    }
  }
}
