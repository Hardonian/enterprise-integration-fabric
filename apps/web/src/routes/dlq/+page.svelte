<script lang="ts">
import { apiGet, apiPost } from '$lib/api';
let tenantId = $state('tenant-north');
let rows = $state<any[] | null>(null);
let error = $state<string | null>(null);
let loading = $state(false);
let replayingId = $state<string | null>(null);
let replayMessage = $state<string | null>(null);

async function load() {
  loading = true;
  replayMessage = null;
  const r = await apiGet<any[]>(`/api/tenants/${tenantId}/dlq`);
  rows = r.data;
  error = r.error;
  loading = false;
}

async function replay(id: string) {
  replayingId = id;
  replayMessage = null;
  const res = await apiPost<{ status: string; dlq_id: string }>(`/api/tenants/${tenantId}/dlq/${id}/replay`, {});
  if (res.error) {
    replayMessage = `Replay error: ${res.error}`;
  } else {
    replayMessage = `Event ${id} successfully replayed!`;
    await load();
  }
  replayingId = null;
}

$effect(() => { load(); });
</script>

<h1>Dead-letter queue (DLQ)</h1>
<div style="display: flex; gap: 0.75rem; align-items: center; margin-bottom: 1rem;">
  <label>Tenant <input bind:value={tenantId}/></label>
  <button onclick={load}>Refresh</button>
</div>

{#if replayMessage}
  <section class="card" style="border-left: 4px solid #10b981;">
    <p>{replayMessage}</p>
  </section>
{/if}

{#if loading}
  <p>Loading DLQ…</p>
{:else if error}
  <section class="card error">{error}</section>
{:else if !rows?.length}
  <p class="empty">No dead-lettered events. The pipeline is healthy!</p>
{:else}
  <div class="grid">
    {#each rows as row}
      <article class="card">
        <div style="display:flex; justify-content:space-between; align-items:center;">
          <h2>{row.eventType}</h2>
          <button onclick={() => replay(row.id)} disabled={replayingId === row.id} style="background: #10b981;">
            {replayingId === row.id ? 'Replaying...' : 'Replay Event'}
          </button>
        </div>
        <p style="color: #f87171; font-family: monospace;">{row.failureReason}</p>
        <div>
          <span class="pill">Corr: {row.correlationId}</span>
          <span class="pill">ID: {row.id}</span>
        </div>
        <details style="margin-top: 0.5rem;">
          <summary style="cursor: pointer; color: #93c5fd;">View Raw Payload</summary>
          <pre style="background: #020617; padding: 0.5rem; border-radius: 0.375rem; overflow-x: auto; font-size: 0.85rem;">{JSON.stringify(row.payload, null, 2)}</pre>
        </details>
      </article>
    {/each}
  </div>
{/if}
