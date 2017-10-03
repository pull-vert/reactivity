package reactivity.experimental;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.junit.Test;
import reactivity.experimental.core.MultiBuilder;

public class MultiJdk6Test {
    @Test
    public void rangeJavaTest() {
        MultiBuilder.fromRange(1, 3)
                // Works with an anonymous function for jdk6 and jdk6
                .subscribe(new Function1<Integer, Unit>() {
                    @Override
                    public Unit invoke(Integer value) {
                        System.out.println(value);
                        return null;
                    }
                });
    }
}
