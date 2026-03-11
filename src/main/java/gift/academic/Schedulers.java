package gift.academic;

import java.util.concurrent.Executors;


/**
 * Schedulers (Планировщики) — вспомогательный класс, предоставляющий
 * готовые реализации интерфейса Scheduler для разных типов задач.
 */
public class Schedulers {

    /**
     * IO-Планировщик: Оптимизирован для блокирующих операций.
     * Использует CachedThreadPool, который создает новые потоки по мере необходимости
     * и удаляет их, если они долго не используются.
     * Подходит для: запросов в сеть, чтения файлов, работы с базой данных.
     */
    private static final Scheduler IO = Executors.newCachedThreadPool()::execute;


    /**
     * Computation-Планировщик: Оптимизирован для задач, нагружающих процессор (CPU).
     * Использует FixedThreadPool с количеством потоков, равным числу ядер процессора.
     * Это предотвращает лишние переключения контекста между потоками.
     * Подходит для: математических расчетов, обработки изображений, парсинга больших JSON.
     */
    private static final Scheduler COMPUTATION = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors())::execute;

    /**
     * Single-Планировщик: Выполняет все задачи строго по очереди в одном единственном потоке.
     * Гарантирует строгий порядок выполнения (FIFO).
     * Подходит для: записи в лог-файл или последовательного обновления состояния,
     * где важна синхронизация без использования lock-ов.
     */
    private static final Scheduler SINGLE = Executors.newSingleThreadExecutor()::execute;

    /**
     * Возвращает планировщик для операций ввода-вывода.
     * @return Экземпляр IO Scheduler.
     */
    public static Scheduler io() {
        return IO;
    }

    /**
     * Возвращает планировщик для вычислительных задач.
     * @return Экземпляр Computation Scheduler.
     */
    public static Scheduler computation() {
        return COMPUTATION;
    }

    /**
     * Возвращает планировщик с одним фоновым потоком.
     * @return Экземпляр Single Scheduler.
     */
    public static Scheduler single() {
        return SINGLE;
    }
}