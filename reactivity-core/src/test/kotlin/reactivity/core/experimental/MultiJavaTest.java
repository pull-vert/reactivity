package reactivity.core.experimental;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.junit.Test;

public class MultiJavaTest {
    @Test
    public void rangeJavaTest() {
        Multi.range(1, 3, CoroutineContexts.emptyCoroutineContext())
                .subscribe(new Function1<Integer, Unit>() {
                    @Override
                    public Unit invoke(Integer value) {
                        System.out.println(value);
                        return null;
                    }
                });
//        // for coming java 8 module test
//        .subscribe((value) -> {
//            System.out.println(value);
//            return null;
//        });
    }
}
