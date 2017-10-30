package reactivity.experimental;

import org.junit.Test;

public class MultiJdk8Test {
    @Test
    public void rangeJavaWithScheduler() {
        // Works with a nice jdk8 lambda !
        MultiBuilder.fromRange(Schedulers.EMPTY_CONTEXT, 1, 3)
                .map(value -> value * 2)
                .subscribe(value -> {
                    System.out.println(value);
                    return null;
                });
    }

    @Test
    public void rangeJavaWithoutScheduler() {
        // Works with a nice jdk8 lambda !
        MultiBuilder.fromRange(1, 3)
                .subscribe(value -> {
                    System.out.println(value);
                    return null;
                });
    }
}
