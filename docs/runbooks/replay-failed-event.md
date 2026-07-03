# Replay failed event

1. Fetch DLQ row by tenant and correlation_id.
2. Confirm payload does not contain raw secrets.
3. Identify failure_reason and downstream status.
4. Correct connector/config/payload issue.
5. Re-submit payload to the correct /api/events/{route}.
6. Verify new trace status and close incident.
