package gift.academic;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;


class RxLibraryTest {

    @Test
    @DisplayName("Проверка базовой цепочки: Create -> Filter -> Map")
    void testBasicChain() {
        List<String> results = new ArrayList<>();

        Observable.create(emitter -> {
                    emitter.onNext(1);
                    emitter.onNext(2);
                    emitter.onNext(3);
                    emitter.onComplete();
                })
                .filter(i -> (int) i > 1) // Пройдут 2 и 3
                .map(i -> "Item " + i)    // Превращаем в строки
                .subscribe(new TestObserver<>(results));

        Assertions.assertEquals(Arrays.asList("Item 2", "Item 3"), results);
    }

    @Test
    @DisplayName("Проверка оператора FlatMap")
    void testFlatMap() {
        List<Integer> results = new ArrayList<>();

        // Указываем <Integer>
        Observable.<Integer>create(emitter -> {
                    emitter.onNext(1);
                    emitter.onNext(2);
                    emitter.onComplete();
                })
                .flatMap(i -> Observable.<Integer>create(inner -> {
                    inner.onNext(i * 10);
                    inner.onNext(i * 10 + 1);
                }))
                .subscribe(new TestObserver<Integer>(results));

        assertEquals(Arrays.asList(10, 11, 20, 21), results);
    }

    @Test
    @DisplayName("Проверка обработки ошибок (onError)")
    void testErrorHandling() {
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        String errorMessage = "Boom!";

        Observable.create(emitter -> {
                    emitter.onNext("Safe");
                    emitter.onError(new RuntimeException(errorMessage));
                    emitter.onNext("Unsafe"); // Не должно дойти
                })
                .subscribe(new Observer<Object>() {
                    @Override public void onSubscribe(Disposable d) {}
                    @Override public void onNext(Object item) {}
                    @Override public void onComplete() {}
                    @Override public void onError(Throwable t) {
                        errorRef.set(t);
                    }
                });

        assertNotNull(errorRef.get());
        assertEquals(errorMessage, errorRef.get().getMessage());
    }

    @Test
    @DisplayName("Проверка работы Schedulers (многопоточность)")
    void testSchedulers() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> emitterThread = new AtomicReference<>();
        AtomicReference<String> observerThread = new AtomicReference<>();

        Observable.create(emitter -> {
                    emitterThread.set(Thread.currentThread().getName());
                    emitter.onNext(1);
                    emitter.onComplete();
                })
                .subscribeOn(Schedulers.io()) // Источник в IO
                .observeOn(Schedulers.single()) // Наблюдатель в Single
                .subscribe(new Observer<Object>() {
                    @Override public void onSubscribe(Disposable d) {}
                    @Override public void onError(Throwable t) {}
                    @Override public void onNext(Object item) {
                        observerThread.set(Thread.currentThread().getName());
                    }
                    @Override public void onComplete() {
                        latch.countDown(); // Сигнализируем завершение теста
                    }
                });

        // Ждем максимум 2 секунды завершения асинхронных задач
        assertTrue(latch.await(2, TimeUnit.SECONDS));

        assertNotEquals(Thread.currentThread().getName(), emitterThread.get(), "Emitter должен быть в фоне");
        assertNotEquals(emitterThread.get(), observerThread.get(), "Потоки subscribeOn и observeOn должны быть разными");
    }

    @Test
    @DisplayName("Проверка Disposable (отмена подписки)")
    void testDisposable() {
        List<Integer> results = new ArrayList<>();

        // Указываем <Integer>
        Observable.<Integer>create(emitter -> {
                    emitter.onNext(1);
                    ((Disposable) emitter).dispose();
                    emitter.onNext(2);
                })
                .subscribe(new TestObserver<Integer>(results));

        assertEquals(1, results.size());
        assertEquals(1, results.get(0));
        assertFalse(results.contains(2));
    }

    // Вспомогательный класс для сбора результатов в тестах
    private static class TestObserver<T> implements Observer<T> {
        private final List<T> results;
        TestObserver(List<T> results) { this.results = results; }
        @Override public void onSubscribe(Disposable d) {}
        @Override public void onNext(T item) { results.add(item); }
        @Override public void onError(Throwable t) {}
        @Override public void onComplete() {}
    }
}
