package reactivity.experimental;

import org.junit.Test;
import reactivity.experimental.jdk8.CompletableFutureFromSolo;

import java.util.concurrent.ExecutionException;

public class SoloJdk8Test {
    @Test
    public void fromValueWithoutScheduler() throws ExecutionException, InterruptedException {
        Solo<Integer> han = SoloBuilder.fromValue(1);
        CompletableFutureFromSolo.build(han).get();
    }
}
