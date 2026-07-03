# Failure modes

Malformed payloads return structured BAD_REQUEST errors. Unknown tenants are rejected before persistence. Transient route failures retry twice. Exhausted failures write dead_letter_events and trace records. Broker outage can be isolated in local dev by BROKER_ENABLED=false; production should alert on broker publish failures and DLQ growth.
