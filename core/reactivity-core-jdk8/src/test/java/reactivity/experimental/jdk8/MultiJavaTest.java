package reactivity.experimental.jdk8;

import org.junit.Test;
import reactivity.experimental.MultiBuilder;

public class MultiJavaTest {
    @Test
    public void rangeJavaTest() {
        MultiBuilder.fromRange(1, 3)
                .subscribe((value) -> {
                    System.out.println(value);
                    return null;
                });
    }
}
