package gift.academic;

/**
 * ForwardingObserver — это базовый класс-обертка для создания операторов.
 * Он реализует Observer<T> (слушает входящие данные типа T),
 * но хранит внутри ссылку на Observer<R> (передает преобразованные данные типа R дальше).
 *
 * @param <T> Тип данных, приходящих СВЕРХУ (от предыдущего звена/источника)
 * @param <R> Тип данных, уходящих ВНИЗ (к следующему звену/потребителю)
 */
public abstract class ForwardingObserver<T, R> implements Observer<T> {

    // Это следующий наблюдатель в цепочке.
    // Мы помечаем его protected, чтобы классы-наследники (map, filter)
    // могли напрямую вызывать его методы onNext/onError.
    protected final Observer<R> downstream;

    // Конструктор принимает того, кто стоит ниже нас по цепочке.
    ForwardingObserver(Observer<R> downstream) {
        this.downstream = downstream;
    }


    /**
     * Вызывается в самом начале, когда цепочка "собирается".
     * Мы просто пробрасываем сигнал о подписке дальше вниз.
     */
    @Override
    public void onSubscribe(Disposable d) {
        downstream.onSubscribe(d);
    }

    /**
     * Вызывается, если сверху пришла ошибка.
     * По умолчанию мы просто транслируем её вниз, чтобы конечный
     * пользователь узнал о проблеме.
     */
    @Override
    public void onError(Throwable t) {
        downstream.onError(t);
    }


    /**
     * Вызывается, когда источник сверху закончил работу.
     * Мы уведомляем нижнее звено, что данных больше не будет.
     */
    @Override
    public void onComplete() {
        downstream.onComplete();
    }
}