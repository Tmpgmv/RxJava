package gift.academic;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws InterruptedException {
        // 1. Создаем источник данных (Observable).
        Observable.create(emitter -> {
                    // Этот код выполнится в потоке, который мы укажем в subscribeOn.
                    System.out.println("Emitter thread: " + Thread.currentThread().getName());

                    // Генерируем три числа.
                    emitter.onNext(1);
                    emitter.onNext(2);
                    emitter.onNext(3);

                    // Сообщаем, что поток данных успешно завершен.
                    emitter.onComplete();
                })

                // 2. subscribeOn: Указывает, В КАКОМ ПОТОКЕ будет работать источник (метод create)
                // Schedulers.io() создает или берет поток из кэшированного пула для ввода-вывода.
                .subscribeOn(Schedulers.io()) // Запуск в IO.

                // 3. filter: Промежуточный оператор-фильтр
                // Пропускает дальше только те элементы, которые соответствуют условию (i > 1)
                // В данном случае 1 отсеется, а 2 и 3 пройдут дальше
                .filter(i -> (int) i > 1) // Мы отфильтровали элементы исключительно для демонстрации работы оператора. В реальном программировании это нужно, чтобы отсеять ненужные данные (например, пустые строки или отрицательные числа). В вашем примере условие i > 1 сработало так: 1 — не больше 1 (условие ложно), поэтому единица «погибла» в фильтре. 2 — больше 1 (условие истинно), прошла дальше. 3 — больше 1 (условие истинно), прошла дальше.

                // 4. map: Оператор преобразования.
                // Берет число (Integer) и превращает его в строку (String) с префиксом "Number: "
                .map(i -> "Number: " + i) // Превратим в строку

                // 5. observeOn: Переключает поток для ВСЕХ ПОСЛЕДУЮЩИХ действий ниже по цепочке
                // Schedulers.single() гарантирует, что вывод в консоль (onNext)
                // будет идти строго в одном и том же выделенном фоновом потоке.
                .observeOn(Schedulers.single()) // Обработка в отдельном потоке

                // 6. subscribe: Конечная точка, где мы "потребляем" данные
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        System.out.println("Subscribed");
                    }

                    @Override
                    public void onNext(String item) {
                        // Получаем обработанные данные.
                        System.out.println("Result: " + item + " on " + Thread.currentThread().getName());
                    }

                    @Override
                    public void onError(Throwable t) {
                        // Сюда попадет любая ошибка, возникшая в цепочке (например, в filter или map).
                        t.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        // Вызывается один раз, когда источник (emitter) вызвал onComplete().
                        // Т.е. подписались.
                        System.out.println("Done!");
                    }
                });

        // 7. Ждем 1 секунду, так как все действия выше происходят асинхронно в фоновых потоках.
        // Если main завершится сразу, программа закроется до того, как мы увидим результат в консоли.
        Thread.sleep(1000);
    }
}