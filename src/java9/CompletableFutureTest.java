package java9;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 *
 * newIncompleteFuture создаёт новыйэкземпляр CompletableFuture, т.к. CompletableFuture немутируемый после выполнения(независимо от результата и исключения). Результатом работы будет применение переданной функции к результату предыдущей фьючи.
 * методы obtrudeValue() / obtrudeException() исключения, они могут менять состояние после завершения.
 * методы complete() / completeExceptionally() могут менять состояние во время выполнения.
 *
 * defaultExecutor возвращает ForkJoinPool.asyncCommonPool() в дефолтной реализации может быть переопределён в классах наследниках.
 * Если мы в CompletableFuture передаём свой Executor, он не будет попадать в результат defaultExecutor. Метод всё равно вернёт дефолтный Executor.
 *
 * copy делает типа копию(с помощью newIncompleteFuture), но она полностью зависит от предыдущей фьючи и результатом новой фьючи будет результат старой.
 * Если исходный CompletableFuture ещё не завершён:
 * Новый CompletableFuture (результат copy()) будет незавершённым.
 * У него нет своего результата: он ждёт, когда завершится оригинал.
 * Как только оригинал завершится (успешно или с ошибкой), новый тоже получит тот же результат.
 * Если оригинал никогда не завершится - новый тоже останется незавершённым.
 * По сути равноценно вызову CompletableFuture<T> copy = original.thenApply(x -> x);
 * Важно: подписчики(колбэки, методы которые ждут завершения фьючи) фьючи не копируются, они не знают про копию и реагируют только на оригинал.
 *
 * minimalCompletionStage() - Он возвращает «урезанное» представление CompletableFuture. Он автоматически повторяет завершение оригинала: если оригинал завершится со значением — этот тоже, если с ошибкой — тоже с ошибкой,
 * НО у него отключены возможности принудительно завершить его извне (нет методов вроде complete, completeExceptionally, obtrude… и т.п.), если попробовать завершить, будет ошибка.
 * Нужно например если мы хотим передать нашу фьючу кому-то во вне и быть уверенными, что никто её там не завершит внезапно для нас методами complete, completeExceptionally, obtrude.
 * CompletionStage Можно вернуть обратно в CompletableFuture через toCompletableFuture(), но это не вернёт права на завершение: попытка завершить вызовет UnsupportedOperationException
 *
 * complete() выполняет текущую работу в текущем потоке, по сути блокирует его. completeAsync() выполняет задачу в отдельном потоке, используется или дефолтный Executor, или передаётся кастомный.
 *
 * orTimeout() при дсостижении таймаута, возвращается новая CompletableFuture с Исключением, старая CompletableFuture продолжает выполнение.
 *
 * completeOnTimeout при дсостижении таймаута, возвращается новая CompletableFuture с дефолтным результатом, старая CompletableFuture продолжает выполнение.
 *
 * delayedExecutor() возвращает Executor, который с задержкой запускает задачу. Задержка начинается в момент execute(), пока она висит в очереди пула - ничего не происходит.
 * Если параметром не передан кастомный Executor, тогда CompletableFuture запустит задачу в дефолтном пуле(но с задержкой), иначе задача запустится на кастомном пуле.
 * не отменяет orTimeout или completeOnTimeout
 * Из‑за создания таймеров и дополнительной логики delayedExecutor не стоит использовать для тысяч задач в секунду — лучше ScheduledExecutorService
 * Может использоваться для тестирования поведения при задержках выполнения задачи, или вместо Thread.sleep() чтобы не стопить текущиё поток
 *
 * completedStage() по аналогии с minimalCompletionStage(), только вернёт уже завершённую CompletionStage с результатом переданным как параметр.
 *
 * failedFuture() - вернёт CompletableFuture c исключением.
 *
 * failedStage() - вернёт CompletionStage c исключением.
 */
public class CompletableFutureTest {
    static void main() {
        completeOnTime();
        orTimeout();
        delayedExecutor();

    }

    private static void completeOnTime() {
        int value1 = 1;
        int value2 = 2;

        CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return value1 + value2;
        }).completeOnTimeout(10, 2, TimeUnit.SECONDS).thenAccept(result-> {
            System.out.println("Result from completeOnTime: " + result);

            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private static void orTimeout() {
        int value1 = 1;
        int value2 = 2;

        CompletableFuture.supplyAsync(() -> {
                    try {
                        TimeUnit.SECONDS.sleep(3L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    return value1 + value2;
                })
                .orTimeout(1, TimeUnit.SECONDS)
                .whenComplete((result, exception) -> {
                    System.out.println("Result from orTimeout(): " + result);

                    if (exception != null)
                    {
                      exception.printStackTrace();
                      System.out.println("job not completed on time!");
                    }
                });

        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private static void delayedExecutor() {
        int value1 = 1;
        int value2 = 2;

        CompletableFuture.supplyAsync(() -> value1 + value2, CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS))
                .thenAccept(result -> System.out.println(" Result from delayed executor: " + result));
        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
