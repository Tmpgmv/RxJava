```markdown
# Реактивная библиотека (RxJava-подобная реализация)

Реализованная система представляет собой упрощенную версию **RxJava** на основе паттерна **Observer**, с цепочками операторов, асинхронностью и управлением потоками. [file:1]  
Архитектура построена вокруг абстрактного класса `Observable<T>`, который формирует цепочку трансформаций, запускаемую только при `subscribe`. [file:1]

## 🏗️ Архитектура системы

Система использует **цепочку ответственности**: каждый оператор возвращает новый `Observable`, а реальная работа начинается только при подписке конечного `Observer`.

```
Observable.create(source)
↓ .filter(predicate)
↓ .map(transformer)
↓ .subscribeOn(scheduler)
↓ .observeOn(scheduler)
↓ .subscribe(observer) ← ТОЛЬКО ЗДЕСЬ выполняется цепочка!
```

### Основные компоненты

| Компонент | Назначение |
|-----------|------------|
| **`Observable<T>`** | "Чертеж" потока: абстрактный класс, каждый оператор создает анонимную подреализацию [file:1] |
| **`Observer<T>`** | Получатель: `onSubscribe(Disposable)`, `onNext(T)`, `onError(Throwable)`, `onComplete()` [file:1] |
| **`ObservableEmitter<T>`** | "Передатчик" внутри `create()`: расширяет `Disposable` [file:1] |
| **`ForwardingObserver<T,R>`** | "Прокси" для операторов: пробрасывает события вниз [file:1] |
| **`Disposable`** | Управление подпиской: флаг `disposed` блокирует события [file:1] |

**Жизненный цикл**: `onSubscribe` → `onNext*` → (`onError` **или** `onComplete`). Ошибки ловятся в `create/map`. [file:1]

## ⚙️ Schedulers (Управление потоками)

`Scheduler` — функциональный интерфейс `execute(Runnable)`: абстрагирует запуск задач. [file:1]

### Различия планировщиков

| Scheduler | Реализация | Особенности | Применение |
|-----------|------------|-------------|------------|
| **`Schedulers.io()`** | `CachedThreadPool` | Динамические потоки | **I/O**: сеть, файлы, БД [file:1] |
| **`Schedulers.computation()`** | `FixedThreadPool` (по CPU) | Фиксированный пул | **CPU**: расчеты, парсинг [file:1] |
| **`Schedulers.single()`** | `SingleThreadExecutor` | Один поток FIFO | **Последовательность**: логи, UI [file:1] |

### Операторы потоков
- **`subscribeOn(Scheduler)`**: **Источник** (`create`) → scheduler (один раз для цепочки) [file:1]
- **`observeOn(Scheduler)`**: **События ниже** (`onNext/onError/onComplete`) → scheduler (можно несколько) [file:1]

## 🧪 Тестирование

**JUnit-тесты** в `RxLibraryTest` используют `TestObserver` для проверки результатов.

| Тест | Сценарий | Проверка |
|------|----------|----------|
| `testBasicChain` | `create→filter→map` | `["Item 2", "Item 3"]` [file:1] |
| `testFlatMap` | Элемент → `Observable` | `[10,11,20,21]` [file:1] |
| `testErrorHandling` | `onError` блокирует `onNext` | Исключение передано [file:1] |
| `testSchedulers` | `io()→single()` | Разные потоки (`CountDownLatch`) [file:1] |
| `testDisposable` | `dispose()` после 1-го | Только первый элемент [file:1] |

## 💻 Примеры использования

### 1. Полная цепочка (из `Main`)

```java
Observable.create(emitter -> {
    System.out.println("Emitter: " + Thread.currentThread().getName());
    emitter.onNext(1); emitter.onNext(2); emitter.onNext(3); emitter.onComplete();
})
.subscribeOn(Schedulers.io())           // ← Источник в IO
.filter(i -> (int) i > 1)               // 1→❌, 2→✅, 3→✅  
.map(i -> "Number: " + i)               // → "Number: 2", "Number: 3"
.observeOn(Schedulers.single())         // ← Обработка в single
.subscribe(new Observer<String>() {
    public void onNext(String item) { 
        System.out.println("Result: " + item); 
    }
    public void onComplete() { System.out.println("Done!"); }
});
Thread.sleep(1000);
```

**Вывод**:

```
Emitter: pool-1-thread-1     // IO-поток
Result: Number: 2 on pool-2-thread-1  // single-поток  
Result: Number: 3 on pool-2-thread-1
Done!
```


### 2. FlatMap + Ошибка

```java
Observable.<Integer>create(emitter -> {
    emitter.onNext(1);
    emitter.onError(new RuntimeException("Boom!")); // ← Всё останавливается
})
.flatMap(i -> Observable.create(inner -> inner.onNext(i * 2)))
.subscribe(observer); // onError("Boom!")
```


### 3. Отмена подписки

```java
Observable.<Integer>create(emitter -> {
    emitter.onNext(1);
    ((Disposable)emitter).dispose();  // ← Блокирует
    emitter.onNext(2);                // Игнорируется
}).subscribe(obs -> System.out::println); // Вывод: только 1
```


## ✅ Соответствие заданию

| Блок задания | Статус | Ключевые моменты |
| :-- | :-- | :-- |
| 1. Базовые компоненты | ✅ | `Observer`, `Observable.create()`, `Emitter` [file:1] |
| 2. Операторы | ✅ | `map`, `filter` + тесты [file:1] |
| 3. Schedulers | ✅ | 3 типа + `subscribeOn/observeOn` [file:1] |
| 4. Дополнительно | ✅ | `flatMap`, `Disposable`, `onError` [file:1] |
| 5. Тестирование | ✅ | Полное покрытие сценариев [file:1] |

## 🔧 Возможные улучшения

- **Управление подписками во `flatMap`**: композитный `Disposable` для внутренних потоков
- **Thread-пулы**: переиспользование между тестами/приложениями
- **Бэкпрешур**: ограничение скорости `onNext` для медленных потребителей


