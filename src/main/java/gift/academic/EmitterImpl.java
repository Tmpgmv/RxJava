package gift.academic;

/**
 * Реализация Эмиттера (Emitter) — это объект, который используется внутри Observable.create.
 * Он реализует два интерфейса:
 * 1. ObservableEmitter — чтобы отправлять данные (onNext, onError, onComplete).
 * 2. Disposable — чтобы управлять жизненным циклом (отменять отправку).
 */
public class EmitterImpl<T> implements ObservableEmitter<T>, Disposable {
    // Ссылка на настоящего Наблюдателя (Observer), который ждет данные в конце цепочки.
    private final Observer<T> actual;

    // Флаг состояния: если true, значит пользователь отписался или произошла ошибка/завершение
    private boolean disposed = false;

    // Конструктор принимает наблюдателя, которому мы будем пересылать события.
    EmitterImpl(Observer<T> actual) {
        this.actual = actual;
    }

    /**
     * Вызывается, когда источник генерирует новый элемент данных.
     *
     * @param item данные (например, число или строка)
     */
    @Override
    public void onNext(T item) {
        // Проверка: если отписка уже произошла, мы просто игнорируем новые данные
        // Это предотвращает утечки и лишнюю работу после завершения потока.
        if (!disposed) {
            actual.onNext(item);
        }
    }


    /**
     * Вызывается при возникновении критической ошибки в источнике данных.
     * @param t объект исключения
     */
    @Override
    public void onError(Throwable t) {

        // Ошибка передается дальше только если поток еще "жив".
        if (!disposed) {
            // Передать плохую новость дальше по конвейеру.
            // Метод actual.onError(t) вызывает одноименный
            // метод onError(Throwable t) у того Observer (наблюдателя),
            // который подписан на этот поток.
            actual.onError(t);
            // Помечаем поток как закрытый.
            dispose();
        }
    }


    /**
     * Вызывается, когда источник успешно закончил передачу всех данных.
     */
    @Override
    public void onComplete() {
        // Если еще не закрыто, уведомляем наблюдателя о финале.
        if (!disposed) {
            actual.onComplete();
            // Поток завершен, больше ничего отправлять нельзя
            dispose();
        }
    }

    /**
     * Метод для ручной остановки потока.
     * После его вызова наблюдатель перестанет получать любые уведомления.
     */
    @Override
    public void dispose() {
        disposed = true;
    }

    /**
     * Проверка: активен ли еще этот поток данных или он уже закрыт.
     */
    @Override
    public boolean isDisposed() {
        return disposed;
    }
}
