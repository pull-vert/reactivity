package reactivity.experimental;

import org.junit.Test;

public class MultiJavaTest {
    @Test
    public void rangeJavaTest() {
        MultiBuilder.fromRange(1, 3, Schedulers.emptyThreadContext())
                .subscribe((value) -> {
                    System.out.println(value);
                    return null;
                });
    }
}
