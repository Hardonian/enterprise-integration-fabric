<script lang="ts">
import { apiPost } from '$lib/api';

let tenantId = $state('tenant-north');
let routeName = $state('lms-course');
let payloadText = $state(JSON.stringify({
  tenant_id: 'tenant-north',
  type: 'lms.course.created',
  correlation_id: 'corr-sim-' + Math.floor(Math.random() * 10000),
  data: {
    courseId: 'BIO-101',
    title: 'Introductory Biology'
  }
}, null, 2));

let result = $state<any | null>(null);
let error = $state<string | null>(null);
let loading = $state(false);

const presets: Record<string, { route: string; payload: any }> = {
  'LMS Course Created': {
    route: 'lms-course',
    payload: {
      tenant_id: 'tenant-north',
      type: 'lms.course.created',
      correlation_id: 'corr-lms-' + Math.floor(Math.random() * 10000),
      data: { courseId: 'CS-101', title: 'Data Structures & Algorithms' }
    }
  },
  'SIS Enrollment Updated': {
    route: 'sis-enrollment',
    payload: {
      tenant_id: 'tenant-north',
      type: 'sis.enrollment.updated',
      correlation_id: 'corr-sis-' + Math.floor(Math.random() * 10000),
      data: { courseId: 'CS-101', learnerId: 'student-42', status: 'ENROLLED' }
    }
  },
  'Grade Sync': {
    route: 'grade-sync',
    payload: {
      tenant_id: 'tenant-north',
      type: 'lms.grade.published',
      correlation_id: 'corr-grade-' + Math.floor(Math.random() * 10000),
      data: { courseId: 'CS-101', learnerId: 'student-42', grade: '95.50' }
    }
  },
  'CRM Lead Created': {
    route: 'crm-lead',
    payload: {
      tenant_id: 'tenant-north',
      type: 'crm.lead.created',
      correlation_id: 'corr-crm-' + Math.floor(Math.random() * 10000),
      data: { institutionId: 'inst-east-1', contactEmail: 'dean@university.edu' }
    }
  },
  'Billing Customer Sync': {
    route: 'billing-customer',
    payload: {
      tenant_id: 'tenant-north',
      type: 'billing.customer.updated',
      correlation_id: 'corr-bill-' + Math.floor(Math.random() * 10000),
      data: { externalCustomerId: 'cust-9988', status: 'ACTIVE' }
    }
  },
  'Forced DLQ Failure': {
    route: 'lms-course',
    payload: {
      tenant_id: 'tenant-north',
      type: 'lms.course.created',
      correlation_id: 'corr-dlq-' + Math.floor(Math.random() * 10000),
      data: { courseId: 'FAIL-101', title: 'Broken Course', forceFailure: true }
    }
  },
  'Idempotent Event (Deduplication)': {
    route: 'lms-course',
    payload: {
      tenant_id: 'tenant-north',
      idempotency_key: 'idempotent-key-unique-123',
      type: 'lms.course.created',
      correlation_id: 'corr-dedup-1',
      data: { courseId: 'DEDUP-101', title: 'Idempotent Course' }
    }
  }
};

function applyPreset(name: string) {
  const preset = presets[name];
  if (preset) {
    routeName = preset.route;
    const p = { ...preset.payload, tenant_id: tenantId };
    payloadText = JSON.stringify(p, null, 2);
  }
}

async function sendEvent() {
  loading = true;
  result = null;
  error = null;
  try {
    const parsed = JSON.parse(payloadText);
    const res = await apiPost<any>(`/api/events/${routeName}`, parsed);
    result = res.data;
    error = res.error;
  } catch (err: any) {
    error = 'Invalid JSON: ' + err.message;
  } finally {
    loading = false;
  }
}
</script>

<h1>Event Simulation Studio</h1>
<p style="color: #94a3b8;">Emit synthetic enterprise events to validate routing, DLQ fallback, circuit breaking, idempotency, and tracing in real time.</p>

<section class="card">
  <h2>Quick Presets</h2>
  <div style="display: flex; gap: 0.5rem; flex-wrap: wrap; margin-top: 0.5rem;">
    {#each Object.keys(presets) as name}
      <button onclick={() => applyPreset(name)} style="background: #334155; font-size: 0.85rem; padding: 0.4rem 0.75rem;">
        {name}
      </button>
    {/each}
  </div>
</section>

<div class="grid" style="grid-template-columns: 1fr 1fr;">
  <section class="card">
    <h2>Event Ingestion Payload</h2>
    <div style="display: flex; gap: 0.5rem; margin-bottom: 0.75rem;">
      <label>Route: 
        <select bind:value={routeName} style="background: #1e293b; color: white; border: 1px solid #475569; padding: 0.4rem; border-radius: 0.375rem;">
          <option value="lms-course">lms-course</option>
          <option value="sis-enrollment">sis-enrollment</option>
          <option value="grade-sync">grade-sync</option>
          <option value="crm-lead">crm-lead</option>
          <option value="billing-customer">billing-customer</option>
        </select>
      </label>
    </div>
    <textarea bind:value={payloadText} rows="12" style="width: 95%; background: #020617; color: #38bdf8; font-family: monospace; border: 1px solid #334155; border-radius: 0.5rem; padding: 0.75rem; font-size: 0.9rem;"></textarea>
    <div style="margin-top: 0.75rem;">
      <button onclick={sendEvent} disabled={loading} style="background: #2563eb; width: 100%;">
        {loading ? 'Transmitting Event…' : 'Emit Event through Fabric'}
      </button>
    </div>
  </section>

  <section class="card">
    <h2>Execution Response & Trace</h2>
    {#if loading}
      <p>Processing message through Camel router...</p>
    {:else if error}
      <div style="background: #450a0a; border: 1px solid #f87171; border-radius: 0.5rem; padding: 0.75rem; color: #fca5a5;">
        <strong>Error:</strong> {error}
      </div>
    {:else if result}
      <div style="background: #064e3b; border: 1px solid #34d399; border-radius: 0.5rem; padding: 0.75rem; color: #a7f3d0; margin-bottom: 0.75rem;">
        <strong>Status:</strong> {result.status ?? 'ACCEPTED'}
      </div>
      <pre style="background: #020617; padding: 0.75rem; border-radius: 0.375rem; font-family: monospace; font-size: 0.85rem; overflow-x: auto;">{JSON.stringify(result, null, 2)}</pre>
      {#if result.correlation_id}
        <div style="margin-top: 0.75rem;">
          <a href={`/traces?correlationId=${result.correlation_id}`} style="color: #60a5fa; text-decoration: underline;">
            View Trace for {result.correlation_id} &rarr;
          </a>
        </div>
      {/if}
    {:else}
      <p style="color: #64748b;">No event dispatched yet. Select a preset and emit an event.</p>
    {/if}
  </section>
</div>
