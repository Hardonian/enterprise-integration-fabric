# Integration map

| Flow | Inbound | Fabric route | Outbound/State |
| --- | --- | --- | --- |
| LMS course created | LMS webhook/API | lms-course-inbound | lms.course.created topic + traces |
| SIS enrollment updated | SIS event | sis-enrollment-inbound | sis.enrollment.updated topic + entitlements lookup |
| Grade published | LMS/Product | grade-sync-outbound | lms.grade.published topic |
| CRM lead handoff | Product/commercial event | crm-lead-handoff | crm.lead.created topic |
| Billing customer updated | Billing event | billing-customer-sync | billing.customer.updated topic |
| Failure | Any route | onException retry | dead_letter_events |
