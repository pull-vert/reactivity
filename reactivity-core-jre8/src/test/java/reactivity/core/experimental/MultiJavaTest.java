package reactivity.core.experimental;

import org.junit.Test;

public class MultiJavaTest {
    @Test
    public void rangeJavaTest() {
        Multi.range(1, 3, CoroutineContexts.emptyCoroutineContext())
                .subscribe((value) -> {
                    System.out.println(value);
                    return null;
                });
    }
}
