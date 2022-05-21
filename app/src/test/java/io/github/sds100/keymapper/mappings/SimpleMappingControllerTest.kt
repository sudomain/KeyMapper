package io.github.sds100.keymapper.mappings

import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.actions.FakeAction
import io.github.sds100.keymapper.actions.PerformActionsUseCase
import io.github.sds100.keymapper.actions.RepeatMode
import io.github.sds100.keymapper.constraints.ConstraintSnapshotImpl
import io.github.sds100.keymapper.constraints.DetectConstraintsUseCase
import junitparams.JUnitParamsRunner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*

/**
 * Created by sds100 on 15/05/2021.
 */

@ExperimentalCoroutinesApi
@RunWith(JUnitParamsRunner::class)
class SimpleMappingControllerTest {

    companion object {
        private const val REPEAT_RATE = 50L
        private const val HOLD_DOWN_DURATION = 1000L
    }

    private val testDispatcher = TestCoroutineDispatcher()
    private val coroutineScope = TestCoroutineScope(testDispatcher)

    private lateinit var controller: SimpleMappingController
    private lateinit var detectMappingUseCase: DetectMappingUseCase
    private lateinit var performActionsUseCase: PerformActionsUseCase
    private lateinit var detectConstraintsUseCase: DetectConstraintsUseCase

    @Before
    fun init() {
        performActionsUseCase = mock {
            MutableStateFlow(REPEAT_RATE).apply {
                on { defaultRepeatRate } doReturn this
            }

            MutableStateFlow(HOLD_DOWN_DURATION).apply {
                on { defaultHoldDownDuration } doReturn this
            }
        }

        detectConstraintsUseCase = mock {
            on { getSnapshot() } doReturn ConstraintSnapshotImpl(
                accessibilityService = mock(),
                mediaAdapter = mock(),
                devicesAdapter = mock(),
                displayAdapter = mock(),
                cameraAdapter = mock(),
                networkAdapter = mock(),
                inputMethodAdapter = mock(),
                lockScreenAdapter = mock(),
                phoneAdapter = mock(),
                powerAdapter = mock()
            )
        }

        controller = FakeSimpleMappingController(
                coroutineScope,
                detectMappingUseCase,
                performActionsUseCase,
                detectConstraintsUseCase
        )
    }

    @After
    fun tearDown() {
        coroutineScope.cleanupTestCoroutines()
    }

    /**
     * #663
     */
    @Test
    fun `action with repeat until limit reached shouldn't stop repeating when trigger is detected again`() =
            coroutineScope.runBlockingTest {
                //GIVEN
                val action = FakeAction(
                        data = ActionData.InputKeyEvent(1),
                        repeat = true,
                        repeatMode = RepeatMode.LIMIT_REACHED,
                        repeatLimit = 2
                )

                //WHEN
                controller.onDetected("id", FakeMapping(actionList = listOf(action)))
                controller.onDetected("id", FakeMapping(actionList = listOf(action)))
                advanceUntilIdle()

                //THEN
                //3 times because it performs once and then repeats twice. It starts performing again when it is triggered again
                verify(performActionsUseCase, atLeast(3)).perform(action.data)
            }

    /**
     * issue #663
     */
    @Test
    fun `when triggering action that repeats until limit reached, then stop repeating when the limit has been reached`() =
        coroutineScope.runBlockingTest {
            //GIVEN
            val action = FakeAction(
                    data = ActionData.InputKeyEvent(keyCode = 1),
                    repeat = true,
                    repeatLimit = 13,
                    repeatMode = RepeatMode.LIMIT_REACHED
            )

                //WHEN
                controller.onDetected("id", FakeMapping(actionList = listOf(action)))
                advanceUntilIdle()

                //THEN
                verify(performActionsUseCase, times(action.repeatLimit!! + 1)).perform(action.data)
            }

    /**
     * issue #663
     */
    @Test
    fun `when triggering action that repeats until pressed again with repeat limit, then stop repeating when the trigger has been pressed again`() =
            coroutineScope.runBlockingTest {
                //GIVEN
                val action = FakeAction(
                        data = ActionData.InputKeyEvent(keyCode = 1),
                        repeat = true,
                        repeatLimit = 10,
                        repeatRate = 100,
                        repeatMode = RepeatMode.TRIGGER_PRESSED_AGAIN
                )

                //WHEN
                controller.onDetected("id", FakeMapping(actionList = listOf(action)))
                advanceTimeBy(200)
                controller.onDetected("id", FakeMapping(actionList = listOf(action)))

                //THEN
                verify(performActionsUseCase, times(3)).perform(action.data)
            }

    /**
     * issue #663
     */
    @Test
    fun `when triggering action that repeats until pressed again with repeat limit, then stop repeating when limit reached and trigger hasn't been pressed again`() =
            coroutineScope.runBlockingTest {
                //GIVEN
                val action = FakeAction(
                        data = ActionData.InputKeyEvent(keyCode = 1),
                        repeat = true,
                        repeatLimit = 10,
                        repeatMode = RepeatMode.TRIGGER_PRESSED_AGAIN
                )

                //WHEN
                controller.onDetected("id", FakeMapping(actionList = listOf(action)))
                advanceTimeBy(5000)
                controller.onDetected("id", FakeMapping(actionList = listOf(action)))

                //THEN
                verify(performActionsUseCase, times(action.repeatLimit!! + 1)).perform(action.data)
            }
}