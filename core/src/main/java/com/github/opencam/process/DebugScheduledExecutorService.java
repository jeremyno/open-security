package com.github.opencam.process;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.time.StopWatch;

public class DebugScheduledExecutorService implements ScheduledExecutorService {
  ScheduledExecutorService base;
  AtomicLong jobsSubmitted = new AtomicLong();
  AtomicLong jobsCompleted = new AtomicLong();
  boolean debugOutput;
  double debugFilter;

  public DebugScheduledExecutorService(final ScheduledExecutorService base, final boolean debug, final double minTime) {
    super();
    this.base = base;
    this.debugOutput = debug;
    this.debugFilter = minTime;
    System.out.println("Filter is: " + this.debugFilter + " and min time is " + minTime);
    if (debug) {
      base.scheduleWithFixedDelay(new Runnable() {

        public void run() {
          System.out.println("Freemem: " + Runtime.getRuntime().freeMemory() / 1024 / 1024 + " MB");
        }

      }, 60, 60, TimeUnit.SECONDS);
    }
  }

  public ScheduledFuture<?> schedule(final Runnable command, final long delay, final TimeUnit unit) {
    return base.schedule(wrapTask(command), delay, unit);
  }

  public void execute(final Runnable command) {
    base.execute(wrapJob(command));
  }

  public <V> ScheduledFuture<V> schedule(final Callable<V> callable, final long delay, final TimeUnit unit) {
    return base.schedule(callable, delay, unit);
  }

  public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay, final long period, final TimeUnit unit) {
    return base.scheduleAtFixedRate(wrapTask(command), initialDelay, period, unit);
  }

  public void shutdown() {
    base.shutdown();
  }

  public List<Runnable> shutdownNow() {
    return base.shutdownNow();
  }

  public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay, final long delay, final TimeUnit unit) {
    return base.scheduleWithFixedDelay(wrapTask(command), initialDelay, delay, unit);
  }

  public boolean isShutdown() {
    return base.isShutdown();
  }

  public boolean isTerminated() {
    return base.isTerminated();
  }

  public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
    return base.awaitTermination(timeout, unit);
  }

  public <T> Future<T> submit(final Callable<T> task) {
    return base.submit(task);
  }

  public <T> Future<T> submit(final Runnable task, final T result) {
    return base.submit(wrapJob(task), result);
  }

  public Future<?> submit(final Runnable task) {
    return base.submit(wrapJob(task));
  }

  public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
    return base.invokeAll(tasks);
  }

  public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException {
    return base.invokeAll(tasks, timeout, unit);
  }

  public <T> T invokeAny(final Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
    return base.invokeAny(tasks);
  }

  public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return base.invokeAny(tasks, timeout, unit);
  }

  private Runnable wrapJob(final Runnable r) {
    jobsSubmitted.addAndGet(1);
    return new Runnable() {

      public void run() {
        final StopWatch timer = new StopWatch();
        try {
          timer.start();
          r.run();
          timer.stop();
        } catch (final Exception e) {
          Logger.getLogger(getClass().getCanonicalName()).log(Level.WARNING, "Problem running " + r, e);
        } finally {
          final long complete = jobsCompleted.addAndGet(1);
          final long submitted = jobsSubmitted.get();
          if (debugOutput) {
            final float ms = (float) timer.getNanoTime() / 1000000;
            if (ms > DebugScheduledExecutorService.this.debugFilter) {
              final String time = String.format("%.3f", ms);
              System.out.println("Job " + r + " completed in " + time + "ms. Currently " + (submitted - complete) + " jobs in flight.");
            }
          }
        }
      }
    };
  }

  private Runnable wrapTask(final Runnable r) {
    return new Runnable() {

      public void run() {
        final StopWatch timer = new StopWatch();
        try {
          timer.start();
          r.run();
          timer.stop();
        } catch (final Exception e) {
          Logger.getLogger(getClass().getCanonicalName()).log(Level.WARNING, "Problem running " + r, e);
        } finally {
          if (debugOutput) {
            final float ms = (float) timer.getNanoTime() / 1000000;
            if (ms > DebugScheduledExecutorService.this.debugFilter) {
              final String time = String.format("%.3f", ms);
              System.out.println("Task " + r + " completed in " + time + "ms.");
            }
          }
        }
      }
    };
  }
}
