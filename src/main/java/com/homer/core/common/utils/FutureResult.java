package com.homer.core.common.utils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FutureResult<T> implements Future<T> {
    private boolean isDone = false;
    private T result;
    private ExecutionException e;
    private long start;
    private final Object lock = new Object();

    public FutureResult() {
        this.start = System.currentTimeMillis();
    }

    public boolean setResult(T result) {
        if (this.isDone) {
            return false;
        }
        synchronized (lock) {
            this.result = result;
            this.isDone = true;
            lock.notify();
        }
        return true;
    }

    public boolean setException(Throwable e) {
        if (this.isDone) {
            return false;
        }
        synchronized (lock) {
            this.e = new ExecutionException(e);
            this.isDone = true;
            lock.notify();
        }
        return true;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return isDone;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return result;
    }

    public T getAndWait() throws InterruptedException, ExecutionException {
        if (this.isDone) {
            return returnResult();
        }
        synchronized (lock) {
            while (!this.isDone) {
                lock.wait();
            }
        }
        return this.returnResult();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return this.result;
    }

    public T getAndWaitTimeOut(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException {
        if (this.isDone) {
            return returnResult();
        }
        long to = unit.toMillis(timeout);
        while (!this.isDone) {
            if (System.currentTimeMillis() - this.start > to) {
                this.setException(new ExecutionException(new TimeoutException()));
                break;
            }
        }
        return this.returnResult();
    }

    private T returnResult() throws ExecutionException {
        if (this.e != null) {
            throw this.e;
        }
        return this.result;
    }
}
