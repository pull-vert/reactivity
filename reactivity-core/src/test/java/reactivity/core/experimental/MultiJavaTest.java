package reactivity.core.experimental;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.junit.Test;

public class MultiJavaTest {
    @Test
    public void rangeJavaTest() {
        MultiBuilder.fromRange(1, 3, Schedulers.emptyThreadContext())
                .subscribe(new Function1<Integer, Unit>() {
                    @Override
                    public Unit invoke(Integer value) {
                        System.out.println(value);
                        return null;
                    }
                });
    }
}
