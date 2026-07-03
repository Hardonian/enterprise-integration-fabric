# Debug grade sync

1. Search event_traces by correlation_id.
2. Confirm tenant_id and courseId match expected institution.
3. Check grade payload shape against AsyncAPI GradeData.
4. Check DLQ for forced or downstream failures.
5. Replay using /api/simulations/grade-sync after correction.
