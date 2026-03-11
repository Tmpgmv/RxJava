package gift.academic;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Observable (Наблюдаемый) — это "чертеж" или "план" будущего потока данных.
 * Сам по себе этот класс ничего не запускает. Он лишь описывает, как данные
 * будут создаваться, фильтроваться и трансформироваться.
 *
 * Мы используем 'abstract', потому что каждый оператор (map, filter, subscribeOn)
 * создает свою собственную под-реализацию этого чертежа.
 */
public abstract class Observable<T> {

    /**
     * ГЛАВНЫЙ МЕТОД: subscribe (Подписаться)
     * Это "спусковой крючок". Пока вы не вызовете subscribe(), ни одна строка
     * кода в цепочке (даже в методе create) не выполнится.
     *
     * @param observer Тот, кто будет получать финальный результат обработки.
     */
    public abstract void subscribe(Observer<T> observer);

    /**
     * СТАТИЧЕСКИЙ МЕТОД: create (Создать)
     * Позволяет вам вручную "диктовать" данные в поток.
     * Внутри вы сами решаете, когда вызвать onNext, а когда — onError.
     *
     * @param source Лямбда-выражение, которое описывает логику генерации.
     * @param <T> Тип данных, которые полетят по трубам этой цепочки.
     */
    public static <T> Observable<T> create(ObservableOnSubscribe<T> source) {
        // Мы возвращаем новый объект Observable, который "запоминает" вашу лямбду source
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<T> observer) {
                // 1. Создаем EmitterImpl — это "умный посредник".
                // Он следит, чтобы мы не слали данные тому, кто уже отписался.
                EmitterImpl<T> emitter = new EmitterImpl<>(observer);

                // 2. Первым делом уведомляем наблюдателя: "Подписка принята, вот твой пульт управления (Disposable)".
                observer.onSubscribe(emitter);

                try {
                    // 3. Запускаем ваш код генерации данных.
                    source.subscribe(emitter);
                } catch (Exception e) {
                    // 4. Если в вашем коде внутри create случился "краш",
                    // мы перехватываем его и цивилизованно отправляем в onError.
                    emitter.onError(e);
                }
            }
        };
    }

    /**
     * Функциональный интерфейс для связи метода create с вашим кодом.
     * Слово 'throws Exception' позволяет вам не писать try-catch внутри лямбды create.
     */
    @FunctionalInterface
    public interface ObservableOnSubscribe<T> {
        void subscribe(ObservableEmitter<T> emitter) throws Exception;
    }

    /**
     * Интерфейс Эмиттера (Излучателя).
     * Это интерфейс "передатчика". Вы нажимаете кнопки (onNext, onError),
     * а библиотека доставляет эти сигналы всем подписчикам.
     */
    public interface ObservableEmitter<T> {
        void onNext(T item);   // Передать один объект дальше
        void onError(Throwable t); // Сообщить о фатальной ошибке и закрыть поток
        void onComplete();     // Сообщить об успешном финале и закрыть поток
    }

    /**
     * ВНУТРЕННЯЯ РЕАЛИЗАЦИЯ ЭМИТТЕРА (EmitterImpl)
     * Этот класс — охранник. Он реализует Disposable (возможность отмены).
     * Если вы вызвали dispose(), этот класс просто перестанет пропускать вызовы onNext.
     */
    private static class EmitterImpl<T> implements ObservableEmitter<T>, Disposable {
        private final Observer<T> observer; // Конечный получатель
        private boolean isDisposed = false; // Флаг: "Жив ли еще этот поток?"

        EmitterImpl(Observer<T> observer) {
            this.observer = observer;
        }

        @Override
        public void onNext(T item) {
            // Пропускаем данные только если пользователь еще хочет их получать (isDisposed == false)
            if (!isDisposed) observer.onNext(item);
        }

        @Override
        public void onError(Throwable t) {
            // Ошибка — это конец. Мы уведомляем наблюдателя и самоликвидируемся (dispose).
            if (!isDisposed) {
                observer.onError(t);
                dispose();
            }
        }

        @Override
        public void onComplete() {
            // Успех — это тоже конец. Больше данных присылать нельзя.
            if (!isDisposed) {
                observer.onComplete();
                dispose();
            }
        }

        @Override
        public void dispose() {
            isDisposed = true; // Выставляем флаг "Отписан"
        }

        @Override
        public boolean isDisposed() {
            return isDisposed;
        }
    }

    // ==========================================
    // ОПЕРАТОРЫ (Трансформация и Фильтрация)
    // ==========================================

    /**
     * ОПЕРАТОР: filter (Фильтрация)
     * Принимает "условие" (Predicate). Если условие выдает false — элемент исчезает из потока.
     */
    public final Observable<T> filter(Predicate<T> predicate) {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<T> observer) {
                // Мы подписываемся на текущий Observable (Observable.this),
                // но подставляем ему "шпиона" (ForwardingObserver), который будет фильтровать onNext.
                Observable.this.subscribe(new ForwardingObserver<T, T>(observer) {
                    @Override
                    public void onNext(T item) {
                        // Если элемент проходит проверку — шлем его дальше вниз (downstream)
                        if (predicate.test(item)) {
                            downstream.onNext(item);
                        }
                        // Если нет — элемент просто игнорируется (пропадает)
                    }
                });
            }
        };
    }

    /**
     * ОПЕРАТОР: map (Преобразование)
     * Самый частый оператор. Позволяет изменить тип данных (например, Integer в String).
     *
     * @param <R> — Новый тип данных на выходе.
     */
    public final <R> Observable<R> map(Function<T, R> mapper) {
        return new Observable<R>() {
            @Override
            public void subscribe(Observer<R> observer) {
                // Подписываемся на источник (T), но возвращать будем уже R.
                Observable.this.subscribe(new ForwardingObserver<T, R>(observer) {
                    @Override
                    public void onNext(T item) {
                        try {
                            // Применяем функцию трансформации к входящему элементу item
                            R result = mapper.apply(item);
                            // Отправляем результат преобразования дальше по цепи
                            downstream.onNext(result);
                        } catch (Exception e) {
                            // Если внутри вашей функции mapper случилась ошибка (например, деление на ноль),
                            // мы ловим её и отправляем в поток ошибок.
                            onError(e);
                        }
                    }
                });
            }
        };
    }

    /**
     * ForwardingObserver — это "ленивый" посредник.
     * Он автоматически пересылает onSubscribe, onError и onComplete вниз по цепи.
     * Это нужно, чтобы в каждом операторе (map, filter) не писать одно и то же 10 раз.
     */
    private abstract static class ForwardingObserver<T, R> implements Observer<T> {
        protected final Observer<R> downstream; // Тот, кто стоит следующим в очереди

        ForwardingObserver(Observer<R> downstream) {
            this.downstream = downstream;
        }

        @Override public void onSubscribe(Disposable d) { downstream.onSubscribe(d); }
        @Override public void onError(Throwable t) { downstream.onError(t); }
        @Override public void onComplete() { downstream.onComplete(); }
    }


    public final <R> Observable<R> flatMap(Function<T, Observable<R>> mapper) {
        return new Observable<R>() {
            @Override
            public void subscribe(Observer<R> observer) {
                Observable.this.subscribe(new ForwardingObserver<T, R>(observer) {
                    @Override
                    public void onNext(T item) {
                        // Превращаем элемент в новый Observable и тут же подписываемся на него
                        mapper.apply(item).subscribe(new Observer<R>() {
                            @Override public void onSubscribe(Disposable d) {}
                            @Override public void onNext(R r) { downstream.onNext(r); }
                            @Override public void onError(Throwable t) { downstream.onError(t); }
                            @Override public void onComplete() { /* В упрощенной версии просто игнорируем */ }
                        });
                    }
                });
            }
        };
    }


    // ==========================================
    // УПРАВЛЕНИЕ ПОТОКАМИ (SCHEDULERS)
    // ==========================================

    /**
     * ОПЕРАТОР: subscribeOn
     * Магия Rx: этот метод говорит, в каком потоке будет работать САМ ИСТОЧНИК (emitter).
     * Обычно вызывается один раз в цепочке.
     */
    public final Observable<T> subscribeOn(Scheduler scheduler) {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<T> observer) {
                // Мы просим планировщик запустить процесс подписки в его потоке (например, в IO).
                // Это заставит метод create() и emitter.onNext() работать в фоновом потоке.
                scheduler.execute(() -> Observable.this.subscribe(observer));
            }
        };
    }

    /**
     * ОПЕРАТОР: observeOn
     * Этот метод говорит: "Все, что происходит НИЖЕ этой строки, должно выполняться в этом потоке".
     * Полезно для возврата в Главный поток (UI) после тяжелых вычислений.
     */
    public final Observable<T> observeOn(Scheduler scheduler) {
        return new Observable<T>() {
            @Override
            public void subscribe(Observer<T> observer) {
                // Мы подписываемся на источник, но каждое событие onNext/onError/onComplete
                // принудительно перенаправляем в указанный scheduler.
                Observable.this.subscribe(new Observer<T>() {
                    @Override public void onSubscribe(Disposable d) { observer.onSubscribe(d); }

                    @Override
                    public void onNext(T item) {
                        // Каждое полученное значение упаковываем в задачу для другого потока.
                        scheduler.execute(() -> observer.onNext(item));
                    }

                    @Override
                    public void onError(Throwable t) {
                        scheduler.execute(() -> observer.onError(t));
                    }

                    @Override
                    public void onComplete() {
                        scheduler.execute(observer::onComplete);
                    }
                });
            }
        };
    }
}
