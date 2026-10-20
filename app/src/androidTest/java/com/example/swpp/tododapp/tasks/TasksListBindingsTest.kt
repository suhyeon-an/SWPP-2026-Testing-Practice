package com.example.swpp.tododapp.tasks

import android.view.View
import android.widget.TextView
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.example.swpp.tododapp.data.Task
import java.time.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ErrorCollector
import org.junit.runner.RunWith
import org.hamcrest.CoreMatchers.equalTo
import org.mockito.MockitoSession
import org.mockito.quality.Strictness

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 28) // Dexmaker static mocking requires Android P or newer.
class TasksListBindingsTest {
    @get:Rule
    val errors = ErrorCollector()

    private var clockSession: MockitoSession? = null

    @Before
    fun freezeToday() {
        clockSession = ExtendedMockito.mockitoSession()
            .spyStatic(LocalDate::class.java)
            .strictness(Strictness.LENIENT)
            .startMocking()
        setToday("2026-10-19")
    }

    @After
    fun restoreClock() {
        clockSession?.finishMocking()
    }

    @Test
    @UiThreadTest
    fun setDueDateLabel_completedWithAnyDueDate_showsClear() {
        // Given
        val dates = listOf(null, "2026-10-18", "2026-10-19", "2026-10-20")
        dates.forEach { date ->
            val view = newView()
            val task = task(date, completed = true)
            // When
            setDueDateLabel(view, task)
            // Then
            assertView(view, "CLEAR", View.VISIBLE, "dueDate=$date")
        }
    }

    @Test
    @UiThreadTest
    fun setDueDateLabel_activeDueDates_showsExactCalendarLabels() {
        // Given
        val cases = listOf("2026-10-19" to "D-Day", "2026-12-20" to "D-62", "2026-08-18" to "D+62")
        cases.forEach { (date, expected) ->
            val view = newView()
            // When
            setDueDateLabel(view, task(date))
            // Then
            assertView(view, expected, View.VISIBLE, "dueDate=$date")
        }
    }

    @Test
    @UiThreadTest
    fun setDueDateLabel_nullTask_clearsAndHidesPreviousLabel() {
        // Given
        val view = newView("D-3")
        // When
        setDueDateLabel(view, null)
        // Then
        assertView(view, "", View.GONE)
    }

    @Test
    @UiThreadTest
    fun setDueDateLabel_activeWithoutDueDate_clearsAndHidesClearLabel() {
        // Given
        val view = newView("CLEAR")
        // When
        setDueDateLabel(view, task(null))
        // Then
        assertView(view, "", View.GONE)
    }

    @Test
    @UiThreadTest
    fun setDueDateLabel_nullAndMissingDates_acceptsAllInputs() {
        // Given
        val cases = listOf(
            Triple(null, "", View.GONE),
            Triple(task(null), "", View.GONE),
            Triple(task(null, completed = true), "CLEAR", View.VISIBLE)
        )
        cases.forEachIndexed { index, (task, expectedText, expectedVisibility) ->
            val view = newView()
            // When - An unexpected exception also fails this test.
            setDueDateLabel(view, task)
            // Then
            assertView(view, expectedText, expectedVisibility, "case=$index")
        }
    }

    @Test
    @UiThreadTest
    fun setDueDateLabel_yesterdayTodayTomorrow_showsAdjacentDayLabels() {
        // Given
        val cases = listOf("2026-10-18" to "D+1", "2026-10-19" to "D-Day", "2026-10-20" to "D-1")
        cases.forEach { (date, expected) ->
            val view = newView()
            // When
            setDueDateLabel(view, task(date))
            // Then
            assertView(view, expected, View.VISIBLE, "dueDate=$date")
        }
    }

    @Test
    @UiThreadTest
    fun setDueDateLabel_completionTransitions_refreshesReusedView() {
        // Given
        val view = newView()
        val cases = listOf(
            Triple(task(null), "", View.GONE),
            Triple(task(null, completed = true), "CLEAR", View.VISIBLE),
            Triple(task(null), "", View.GONE),
            Triple(task("2026-10-20"), "D-1", View.VISIBLE),
            Triple(task("2026-10-20", completed = true), "CLEAR", View.VISIBLE),
            Triple(task("2026-10-20"), "D-1", View.VISIBLE)
        )
        cases.forEachIndexed { index, (task, expectedText, expectedVisibility) ->
            // When
            setDueDateLabel(view, task)
            // Then - Collect failures so every transition is exercised.
            assertView(view, expectedText, expectedVisibility, "transition=$index")
        }
    }

    @Test
    @UiThreadTest
    fun setDueDateLabel_missingDateTransitions_refreshesReusedView() {
        // Given
        val view = newView()
        val cases = listOf(
            Triple(task(null), "", View.GONE),
            Triple(task("2026-10-20"), "D-1", View.VISIBLE),
            Triple(task(null), "", View.GONE)
        )
        cases.forEachIndexed { index, (task, expectedText, expectedVisibility) ->
            // When
            setDueDateLabel(view, task)
            // Then
            assertView(view, expectedText, expectedVisibility, "transition=$index")
        }
    }

    @Test
    @UiThreadTest
    fun setDueDateLabel_nonemptyLabelFromAllVisibilityStates_restoresVisibility() {
        // Given
        listOf(View.VISIBLE, View.INVISIBLE, View.GONE).forEach { initialVisibility ->
            val view = newView("old", initialVisibility)
            // When
            setDueDateLabel(view, task("2026-10-19"))
            // Then
            assertView(view, "D-Day", View.VISIBLE, "initialVisibility=$initialVisibility")
        }
    }

    @Test
    @UiThreadTest
    fun setDueDateLabel_repeatedEmptyBindingsFromAllVisibilityStates_clearsAndHides() {
        // Given
        listOf(View.VISIBLE, View.INVISIBLE, View.GONE).forEach { initialVisibility ->
            val view = newView("old", initialVisibility)
            repeat(2) { call ->
                // When
                setDueDateLabel(view, null)
                // Then
                assertView(view, "", View.GONE, "initialVisibility=$initialVisibility, call=$call")
            }
        }
    }

    @Test
    @UiThreadTest
    fun setDueDateLabel_currentDateCrossesDueDate_refreshesLabelOnRebind() {
        // Given
        val view = newView()
        val task = task("2026-10-19")
        val cases = listOf("2026-10-18" to "D-1", "2026-10-19" to "D-Day", "2026-10-20" to "D+1")
        cases.forEach { (today, expected) ->
            // When
            setToday(today)
            setDueDateLabel(view, task)
            // Then
            assertView(view, expected, View.VISIBLE, "today=$today")
        }
    }

    @Test
    @UiThreadTest
    fun setDueDateLabel_monthRollover_showsTotalCalendarDays() {
        // Given
        setToday("2026-01-30")
        val cases = listOf("2026-01-31" to "D-1", "2026-02-01" to "D-2", "2026-02-02" to "D-3")
        cases.forEach { (date, expected) ->
            val view = newView()
            // When
            setDueDateLabel(view, task(date))
            // Then
            assertView(view, expected, View.VISIBLE, "dueDate=$date")
        }
    }

    @Test
    @UiThreadTest
    fun setDueDateLabel_yearRollover_showsTotalCalendarDays() {
        // Given
        setToday("2026-12-30")
        val cases = listOf("2026-12-31" to "D-1", "2027-01-01" to "D-2", "2027-01-02" to "D-3")
        cases.forEach { (date, expected) ->
            val view = newView()
            // When
            setDueDateLabel(view, task(date))
            // Then
            assertView(view, expected, View.VISIBLE, "dueDate=$date")
        }
    }

    @Test
    @UiThreadTest
    fun setDueDateLabel_leapDayAndAdjacentDays_showsTotalCalendarDays() {
        // Given
        setToday("2024-02-27")
        val cases = listOf("2024-02-28" to "D-1", "2024-02-29" to "D-2", "2024-03-01" to "D-3")
        cases.forEach { (date, expected) ->
            val view = newView()
            // When
            setDueDateLabel(view, task(date))
            // Then
            assertView(view, expected, View.VISIBLE, "dueDate=$date")
        }
    }

    private fun setToday(date: String) {
        val fixedDate = LocalDate.parse(date)
        ExtendedMockito.doReturn(fixedDate).`when` { LocalDate.now() }
        assertEquals("Clock fixture must supply the specified date", fixedDate, LocalDate.now())
    }

    private fun task(date: String?, completed: Boolean = false): Task =
        Task(dueDate = date?.let(LocalDate::parse), isCompleted = completed)

    private fun newView(text: String = "stale", visibility: Int = View.VISIBLE): TextView =
        TextView(InstrumentationRegistry.getInstrumentation().targetContext).apply {
            this.text = text
            this.visibility = visibility
        }

    private fun assertView(view: TextView, text: String, visibility: Int, case: String = "binding") {
        errors.checkThat("$case: text", view.text.toString(), equalTo(text))
        errors.checkThat("$case: visibility", view.visibility, equalTo(visibility))
    }
}
