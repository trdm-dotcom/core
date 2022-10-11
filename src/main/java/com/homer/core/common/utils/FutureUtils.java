package com.homer.core.common.utils;

import rx.Observable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class FutureUtils {
    public static <T> CompletableFuture<Results<T>> allOf(Stream<CompletableFuture<T>> stream) {
        return allOf(stream, false);
    }

    public static <T> CompletableFuture<T> completeExceptionally(Throwable e) {
        CompletableFuture<T> fut = new CompletableFuture<T>();
        fut.completeExceptionally(e);
        return fut;
    }

    public static CompletableFuture<Results<Object>> allOfMix(Stream<CompletableFuture<?>> stream) {
        Results<Object> results = new Results<>();
        CompletableFuture<Results<Object>> future = new CompletableFuture<>();
        AtomicInteger index = new AtomicInteger(0);
        AtomicInteger total = new AtomicInteger(-1);
        stream.forEach(item -> {
            final int idx = index.getAndIncrement();
            item.handle((t, e) -> {
                results.add(new Result<>(idx, e, t));
                if (total.compareAndSet(results.size(), results.size())) {
                    if (!future.isDone()) {
                        future.complete(results);
                    }
                    return true;
                }
                return false;
            });
        });
        total.set(index.get());
        return future;
    }

    public static <T, E> CompletableFuture<T> fromObservable(Observable<T> observable) {
        CompletableFuture<T> future = new CompletableFuture<>();
        observable.subscribe(
                t -> {
                    if (!future.isDone()) {
                        future.complete(t);
                    }
                },
                e -> {
                    if (!future.isDone()) {
                        future.completeExceptionally(e);
                    }
                }
        );
        return future;
    }

    public static <T> CompletableFuture<Results<T>> allOf(Stream<CompletableFuture<T>> stream, boolean stopOnError) {
        Results<T> results = new Results<>();
        CompletableFuture<Results<T>> future = new CompletableFuture<>();
        AtomicInteger index = new AtomicInteger(0);
        AtomicInteger total = new AtomicInteger(-1);
        stream.forEach(item -> {
            final int idx = index.getAndIncrement();
            item.handle((t, e) -> {
                results.add(new Result<>(idx, e, t));
                if (stopOnError && e != null) {
                    future.complete(results);
                    return true;
                }
                if (total.compareAndSet(results.size(), results.size())) {
                    if (!future.isDone()) {
                        future.complete(results);
                    }
                    return true;
                }
                return false;
            });
        });
        total.set(index.get());
        return future;
    }

    public static final class AllOfBuilder<T> {
        private List<CompletableFuture<T>> list = new ArrayList<>();

        public AllOfBuilder<T> add(CompletableFuture<T> item) {
            list.add(item);
            return this;
        }

        public CompletableFuture<Results<T>> allOf() {
            return FutureUtils.allOf(this.list.stream());
        }
    }

    public static class Results<T> extends ArrayList<Result<T>> {
        private List<Throwable> exceptions = new ArrayList<>();
        private List<T> success = new ArrayList<>();

        @Override
        public boolean add(Result<T> t) {
            if (t.isError()) {
                exceptions.add(t.e);
            } else {
                success.add(t.getData());
            }
            return super.add(t);
        }

        public Throwable getFirstException() {
            if (exceptions.isEmpty()) {
                return null;
            }
            return exceptions.get(0);
        }

        public List<Throwable> getExceptions() {
            return exceptions;
        }

        public List<T> getSuccess() {
            return success;
        }
    }

    public static final class Result<T> {
        private int index;
        private T data;
        private Throwable e;
        public boolean isError() {
            return this.e != null;
        }

        public Result(int index, Throwable e, T data) {
            this.index = index;
            if (e != null) {
                this.e = e;
            } else {
                this.data = data;
            }
        }

        public Result(int index, T data) {
            this.index = index;
            this.data = data;
        }

        public T getData() {
            return data;
        }

        public Throwable getE() {
            return e;
        }

        public int getIndex() {
            return index;
        }
    }
}
