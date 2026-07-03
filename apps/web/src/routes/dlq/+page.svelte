<script lang="ts">
import { apiGet } from '$lib/api';
let tenantId=$state('tenant-north'); let rows=$state<any[]|null>(null); let error=$state<string|null>(null); let loading=$state(false); async function load(){loading=true; const r=await apiGet<any[]>(`/api/tenants/${tenantId}/dlq`); rows=r.data; error=r.error; loading=false;} $effect(()=>{load();});
</script>
<h1>Dead-letter queue</h1><label>Tenant <input bind:value={tenantId}/></label> <button onclick={load}>Load</button>{#if loading}<p>Loading DLQ…</p>{:else if error}<section class="card error">{error}</section>{:else if !rows?.length}<p class="empty">No dead-lettered events.</p>{:else}{#each rows as row}<article class="card"><h2>{row.eventType}</h2><p>{row.failureReason}</p><span class="pill">{row.correlationId}</span></article>{/each}{/if}
