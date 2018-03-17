package reactivity.experimental.javafx

import javafx.application.Platform
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import org.junit.Test
import reactivity.experimental.*
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MultiJavaFxTest : TestBase() {
    @Test
    fun `multi JavaFx context`() = runTest {
        assertFalse(Platform.isFxApplicationThread())
        launch(JavaFx) {
            Multi.range(1, 3)
                    .delayEach(10)
                    .consumeEach { assertTrue(Platform.isFxApplicationThread()) }
        }
    }
}