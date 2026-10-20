# Spec: `DueDateUtils.kt`

> Post-conditions are the test oracle: write the expected behavior from intent, not from the code.
> Tag each post-condition [Unit] if its test runs on the JVM; otherwise tag it [Integration].
> End each valid case with `Tested by <test>` if an existing test checks it, or `Tip:` describing the test to write.

## `internal fun dueDateLabel(dueDate: LocalDate?, today: LocalDate, isCompleted: Boolean): String`

**Intent**: Produce the task's D-day label using the total number of calendar days between today and its due date. Completion takes precedence over the presence or value of a due date.

**Pre-conditions**

- `today` is a valid non-null `LocalDate`; `dueDate` is either a valid `LocalDate` or null. Both completion states are valid.
- The supplied `today`, rather than the system clock, defines today. N below is the total calendar-day distance, including all intervening months and years.

**Post-conditions**

- Success cases
  - Completed tasks return exactly `"CLEAR"`, regardless of due date. `[Unit]`
    - Tip: Given today = 2026-10-19, isCompleted = true, and dueDate in {null, 2026-10-18, 2026-10-19, 2026-10-20} / When dueDateLabel is called for each value / Then each result is "CLEAR".
  - Active tasks without a due date return the empty string. `[Unit]`
    - Tip: Given dueDate = null, today = 2026-10-19, and isCompleted = false / When dueDateLabel is called / Then the result is "".
  - Active tasks due today return exactly `"D-Day"`. `[Unit]`
    - Tip: Given dueDate = 2026-10-19, today = 2026-10-19, and isCompleted = false / When dueDateLabel is called / Then the result is "D-Day".
  - Future active tasks return `"D-N"`, with positive N written as decimal digits without padding. `[Unit]`
    - Tip: Given dueDate = 2026-12-20, today = 2026-10-19, and isCompleted = false / When dueDateLabel is called / Then the result is "D-62".
  - Overdue active tasks return `"D+N"`, with positive N written as decimal digits without padding. `[Unit]`
    - Tip: Given dueDate = 2026-10-19, today = 2026-12-20, and isCompleted = false / When dueDateLabel is called / Then the result is "D+62".
- Error cases
  - None. Every input satisfying the pre-conditions is valid, including a missing due date. `[Unit]`
    - Tip: Given dueDate = null, today = 2026-10-19, and isCompleted in {false, true} / When dueDateLabel is called for each state / Then the results are respectively "" and "CLEAR", without an exception.
- Edge cases
  - The overdue/today/future boundary distinguishes the immediately adjacent days. `[Unit]`
    - Tip: Given today = 2026-10-19, isCompleted = false, and dueDate in {2026-10-18, 2026-10-19, 2026-10-20} / When dueDateLabel is called for each date / Then the results are respectively "D+1", "D-Day", and "D-1".
  - Completion overrides each date category and the missing-date category. Boolean and null boundaries have only discrete alternatives. `[Unit]`
    - Tip: Given today = 2026-10-19 and dueDate in {null, 2026-10-18, 2026-10-19, 2026-10-20} / When dueDateLabel is called with isCompleted = false and then true for each value / Then the result pairs are respectively ("", "CLEAR"), ("D+1", "CLEAR"), ("D-Day", "CLEAR"), and ("D-1", "CLEAR").
  - Month rollover counts the days before, at, and after the first day of the next month in both directions. `[Unit]`
    - Tip: Given isCompleted = false and (today, dueDate) in {(2026-01-30, 2026-01-31), (2026-01-30, 2026-02-01), (2026-01-30, 2026-02-02), (2026-01-31, 2026-01-30), (2026-02-01, 2026-01-30), (2026-02-02, 2026-01-30)} / When dueDateLabel is called for each pair / Then the results are respectively "D-1", "D-2", "D-3", "D+1", "D+2", and "D+3".
  - A full month of elapsed time does not reset N, including the immediately adjacent days. `[Unit]`
    - Tip: Given isCompleted = false and (today, dueDate) in {(2026-01-01, 2026-01-31), (2026-01-01, 2026-02-01), (2026-01-01, 2026-02-02), (2026-01-31, 2026-01-01), (2026-02-01, 2026-01-01), (2026-02-02, 2026-01-01)} / When dueDateLabel is called for each pair / Then the results are respectively "D-30", "D-31", "D-32", "D+30", "D+31", and "D+32".
  - Year rollover counts the days before, at, and after January 1 in both directions. `[Unit]`
    - Tip: Given isCompleted = false and (today, dueDate) in {(2026-12-30, 2026-12-31), (2026-12-30, 2027-01-01), (2026-12-30, 2027-01-02), (2026-12-31, 2026-12-30), (2027-01-01, 2026-12-30), (2027-01-02, 2026-12-30)} / When dueDateLabel is called for each pair / Then the results are respectively "D-1", "D-2", "D-3", "D+1", "D+2", and "D+3".
  - Leap day counts as one calendar day, with correct results immediately before, at, and after February 29 in both directions. `[Unit]`
    - Tip: Given isCompleted = false and (today, dueDate) in {(2024-02-27, 2024-02-28), (2024-02-27, 2024-02-29), (2024-02-27, 2024-03-01), (2024-02-28, 2024-02-27), (2024-02-29, 2024-02-27), (2024-03-01, 2024-02-27)} / When dueDateLabel is called for each pair / Then the results are respectively "D-1", "D-2", "D-3", "D+1", "D+2", and "D+3".
  - A non-leap February transitions directly from February 28 to March 1, including immediately adjacent dates. `[Unit]`
    - Tip: Given isCompleted = false and (today, dueDate) in {(2026-02-27, 2026-02-28), (2026-02-27, 2026-03-01), (2026-02-27, 2026-03-02), (2026-02-28, 2026-02-27), (2026-03-01, 2026-02-27), (2026-03-02, 2026-02-27)} / When dueDateLabel is called for each pair / Then the results are respectively "D-1", "D-2", "D-3", "D+1", "D+2", and "D+3".

**Undefined behavior** (optional)

- None for inputs satisfying the pre-conditions.
