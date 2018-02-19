package reactivity.experimental.swing

import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.swing.Swing
import org.junit.Test
import reactivity.experimental.*
import javax.swing.SwingUtilities
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MultiSwingTest : TestBase() {
    @Test
    fun `multi Swing context`() = runTest {
        assertFalse(SwingUtilities.isEventDispatchThread())
        launch(Swing) {
            Multi.range(1, 3)
                    .delay(10)
                    .consumeEach { assertTrue(SwingUtilities.isEventDispatchThread()) }
        }
    }
}