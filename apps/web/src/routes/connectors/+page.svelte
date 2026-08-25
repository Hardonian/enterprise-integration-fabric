<script lang="ts">
import { apiGet, apiPost } from '$lib/api';
type Connector = { id:string; tenantId:string; name:string; systemType:string; baseUrl:string; status:string; credentialReference:string };
type PingResult = { connector_id: string; status: string; base_url: string; latency_ms: number; checked_at: string };

let tenantId = $state('tenant-north');
let result = $state({ data: null as Connector[] | null, error: null as string | null, loading: false });
let pingResults = $state<Record<string, PingResult>>({});
let pingingId = $state<string | null>(null);

async function load(){
  result = { data: null, error: null, loading: true };
  result = await apiGet<Connector[]>(`/api/tenants/${tenantId}/connectors`);
}

async function testConnection(id: string) {
  pingingId = id;
  const res = await apiPost<PingResult>(`/api/tenants/${tenantId}/connectors/${id}/ping`, {});
  if (res.data) {
    pingResults[id] = res.data;
  }
  pingingId = null;
}

$effect(() => { load(); });
</script>

<h1>Connectors & Health Diagnostics</h1>
<div style="display: flex; gap: 0.75rem; align-items: center; margin-bottom: 1rem;">
  <label>Tenant <input bind:value={tenantId} aria-label="Tenant ID" /></label>
  <button onclick={load}>Refresh</button>
</div>

{#if result.loading}
  <p>Loading connectors…</p>
{:else if result.error}
  <section class="card error"><p>{result.error}</p></section>
{:else if !result.data?.length}
  <p class="empty">No connectors found for this tenant.</p>
{:else}
  <div class="grid">
    {#each result.data as c}
      <article class="card">
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <h2>{c.name}</h2>
          <button onclick={() => testConnection(c.id)} disabled={pingingId === c.id} style="background: #3b82f6; font-size: 0.85rem; padding: 0.4rem 0.75rem;">
            {pingingId === c.id ? 'Testing...' : 'Test Connection'}
          </button>
        </div>
        <p>
          <span class="pill">{c.systemType}</span>
          <span class="pill" style="color: {c.status === 'ACTIVE' ? '#34d399' : '#f87171'}">{c.status}</span>
          {#if pingResults[c.id]}
            <span class="pill" style="border-color: #10b981; color: #10b981;">{pingResults[c.id].latency_ms}ms ({pingResults[c.id].status})</span>
          {/if}
        </p>
        <p style="font-family: monospace; font-size: 0.9rem; word-break: break-all;">{c.baseUrl}</p>
        <small style="color: #94a3b8;">Vault Credential Ref: <code>{c.credentialReference}</code></small>
      </article>
    {/each}
  </div>
{/if}
