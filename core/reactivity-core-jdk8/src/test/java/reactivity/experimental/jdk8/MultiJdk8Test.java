package reactivity.experimental.jdk8;

import org.junit.Test;
import reactivity.experimental.MultiBuilder;

public class MultiJdk8Test {
    @Test
    public void rangeJavaTest() {
        // shows that it works with a nice jdk8 lambda !
        MultiBuilder.fromRange(1, 3)
                .subscribe((value) -> {
                    System.out.println(value);
                    return null;
                });
    }
}
