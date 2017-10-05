package reactivity.experimental;

import org.junit.Test;
import reactivity.experimental.core.Schedulers;

public class MultiJdk8Test {
    @Test
    public void rangeJavaTest() {
        // Works with a nice jdk8 lambda !
        Multi.fromRange(Schedulers.EMPTY_CONTEXT, 1, 3)
                .subscribe((value) -> {
                    System.out.println(value);
                    return null;
                });
    }
}
