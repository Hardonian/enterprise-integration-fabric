<script lang="ts">
import { apiGet } from '$lib/api';
let tenantId = $state('tenant-north'); let correlationId = $state('corr-seed-1'); let rows = $state<any[] | null>(null); let error = $state<string | null>(null); let loading=$state(false);
async function search(){ loading=true; const r=await apiGet<any[]>(`/api/tenants/${tenantId}/traces/${correlationId}`); rows=r.data; error=r.error; loading=false; }
</script>
<h1>Event trace search</h1><form onsubmit={(e)=>{e.preventDefault(); search();}}><label>Tenant <input bind:value={tenantId}/></label> <label>Correlation ID <input bind:value={correlationId}/></label> <button>Search</button></form>{#if loading}<p>Searching…</p>{:else if error}<section class="card error">{error}</section>{:else if rows && rows.length===0}<p class="empty">No trace records.</p>{:else if rows}<div>{#each rows as row}<article class="card"><strong>{row.routeName}</strong> <span class="pill">{row.status}</span><p>{row.message}</p><small>{row.createdAt}</small></article>{/each}</div>{/if}
