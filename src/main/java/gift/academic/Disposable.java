package gift.academic;

/**
 * Интерфейс для отмены подписки
 */
interface Disposable {
    void dispose();
    boolean isDisposed();
}