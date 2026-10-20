# Spec: `TasksListBindings.kt`

> Post-conditions are the test oracle: write the expected behavior from intent, not from the code.
> Tag each post-condition [Unit] if its test runs on the JVM; otherwise tag it [Integration].
> End each valid case with `Tested by <test>` if an existing test checks it, or `Tip:` describing the test to write.

## `fun setDueDateLabel(textView: TextView, task: Task?)`

**Intent**: Show the task's current D-day label and hide the label when there is nothing to display. Each call refreshes text and visibility so a recycled TextView cannot retain another task's label.

**Pre-conditions**

- `textView` is a valid Android `TextView`, updated on the UI thread. `task` may be null; a non-null task may have a null due date.
- Today is the current date in the system default time zone. Integration checks must control the clock/default zone so `LocalDate.now()` equals the exact date in each Tip throughout the call.
- In every Tip, `Task(dueDate = ..., isCompleted = ...)` uses the remaining constructor defaults. Unless a Tip specifies otherwise, the TextView starts with text = "stale" and visibility = View.VISIBLE.
- N is the total calendar-day distance; it includes intervening months, years, and leap days.

**Post-conditions**

- Success cases
  - A completed task displays exactly `"CLEAR"` with visibility `View.VISIBLE`, regardless of its due date. `[Integration]`
    - Tip: Given today = 2026-10-19 and Task(dueDate = d, isCompleted = true) for d in {null, 2026-10-18, 2026-10-19, 2026-10-20} / When setDueDateLabel is called for each task / Then text is "CLEAR" and visibility is View.VISIBLE after each call.
  - An active task with a due date displays `"D-Day"`, `"D-N"`, or `"D+N"` and is visible. `[Integration]`
    - Tip: Given today = 2026-10-19 and Task(dueDate = d, isCompleted = false) for d in {2026-10-19, 2026-12-20, 2026-08-18} / When setDueDateLabel is called for each task / Then text is respectively "D-Day", "D-62", and "D+62", and visibility is View.VISIBLE after each call.
  - A null task clears the text and hides the view with `View.GONE`. `[Integration]`
    - Tip: Given today = 2026-10-19, task = null, text = "D-3", and visibility = View.VISIBLE / When setDueDateLabel is called / Then text is "" and visibility is View.GONE.
  - An active task without a due date clears the text and hides the view with `View.GONE`. `[Integration]`
    - Tip: Given today = 2026-10-19, Task(dueDate = null, isCompleted = false), text = "CLEAR", and visibility = View.VISIBLE / When setDueDateLabel is called / Then text is "" and visibility is View.GONE.
- Error cases
  - None. Null tasks and missing due dates are valid binding inputs. `[Integration]`
    - Tip: Given today = 2026-10-19 and task values {null, Task(dueDate = null, isCompleted = false), Task(dueDate = null, isCompleted = true)} / When setDueDateLabel is called for each value / Then the (text, visibility) pairs are respectively ("", View.GONE), ("", View.GONE), and ("CLEAR", View.VISIBLE), without an exception.
- Edge cases
  - Yesterday, today, and tomorrow display distinct labels at the date boundary. `[Integration]`
    - Tip: Given today = 2026-10-19 and Task(dueDate = d, isCompleted = false) for d in {2026-10-18, 2026-10-19, 2026-10-20} / When setDueDateLabel is called for each task / Then text is respectively "D+1", "D-Day", and "D-1", and visibility is View.VISIBLE after each call.
  - Completion-state transitions refresh the same view in both directions, with or without a due date. `[Integration]`
    - Tip: Given today = 2026-10-19 and one TextView initially containing "stale" with visibility View.VISIBLE / When setDueDateLabel is called sequentially with Task(dueDate = null, isCompleted = false), Task(dueDate = null, isCompleted = true), Task(dueDate = null, isCompleted = false), Task(dueDate = 2026-10-20, isCompleted = false), Task(dueDate = 2026-10-20, isCompleted = true), and Task(dueDate = 2026-10-20, isCompleted = false) / Then the successive (text, visibility) pairs are ("", View.GONE), ("CLEAR", View.VISIBLE), ("", View.GONE), ("D-1", View.VISIBLE), ("CLEAR", View.VISIBLE), and ("D-1", View.VISIBLE).
  - Missing-date transitions refresh the same view in both directions. `[Integration]`
    - Tip: Given today = 2026-10-19 and one TextView initially containing "stale" with visibility View.VISIBLE / When setDueDateLabel is called sequentially with Task(dueDate = null, isCompleted = false), Task(dueDate = 2026-10-20, isCompleted = false), and Task(dueDate = null, isCompleted = false) / Then the successive (text, visibility) pairs are ("", View.GONE), ("D-1", View.VISIBLE), and ("", View.GONE).
  - A nonempty label restores visibility from every Android visibility state, including an initially invisible or gone view. `[Integration]`
    - Tip: Given today = 2026-10-19, Task(dueDate = 2026-10-19, isCompleted = false), text = "old", and initial visibility in {View.VISIBLE, View.INVISIBLE, View.GONE} / When setDueDateLabel is called for each initial state / Then text is "D-Day" and visibility is View.VISIBLE in every case.
  - An empty label clears and hides the view from every Android visibility state, including repeated empty bindings. `[Integration]`
    - Tip: Given today = 2026-10-19, task = null, text = "old", and initial visibility in {View.VISIBLE, View.INVISIBLE, View.GONE} / When setDueDateLabel is called twice for each initial state / Then text is "" and visibility is View.GONE after both calls in every case.
  - As the current date crosses midnight on the due date, rebinding uses the new current date. `[Integration]`
    - Tip: Given Task(dueDate = 2026-10-19, isCompleted = false) and one TextView / When setDueDateLabel is called with the controlled current date successively 2026-10-18, 2026-10-19, and 2026-10-20 / Then text is respectively "D-1", "D-Day", and "D+1", and visibility is View.VISIBLE after each call.
  - Calendar rollover preserves total day counts on the day before, at, and after month, year, and leap-day boundaries. `[Integration]`
    - Tip: Given today = 2026-01-30 and Task(dueDate = d, isCompleted = false) for d in {2026-01-31, 2026-02-01, 2026-02-02} / When setDueDateLabel is called for each task / Then text is respectively "D-1", "D-2", and "D-3", and visibility is View.VISIBLE after each call.
    - Tip: Given today = 2026-12-30 and Task(dueDate = d, isCompleted = false) for d in {2026-12-31, 2027-01-01, 2027-01-02} / When setDueDateLabel is called for each task / Then text is respectively "D-1", "D-2", and "D-3", and visibility is View.VISIBLE after each call.
    - Tip: Given today = 2024-02-27 and Task(dueDate = d, isCompleted = false) for d in {2024-02-28, 2024-02-29, 2024-03-01} / When setDueDateLabel is called for each task / Then text is respectively "D-1", "D-2", and "D-3", and visibility is View.VISIBLE after each call.

**Undefined behavior** (optional)

- Calls outside the Android UI thread or without a valid TextView are outside this binding contract.
