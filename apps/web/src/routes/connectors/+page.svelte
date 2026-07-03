<script lang="ts">
import { apiGet } from '$lib/api';
type Connector = { id:string; tenantId:string; name:string; systemType:string; baseUrl:string; status:string; credentialReference:string };
let tenantId = $state('tenant-north');
let result = $state({ data: null as Connector[] | null, error: null as string | null, loading: false });
async function load(){ result={data:null,error:null,loading:true}; result = await apiGet<Connector[]>(`/api/tenants/${tenantId}/connectors`); }
$effect(() => { load(); });
</script>
<h1>Connectors</h1><label>Tenant <input bind:value={tenantId} aria-label="Tenant ID" /></label> <button onclick={load}>Load</button>{#if result.loading}<p>Loading connectors…</p>{:else if result.error}<section class="card error"><p>{result.error}</p></section>{:else if !result.data?.length}<p class="empty">No connectors found.</p>{:else}<div class="grid">{#each result.data as c}<article class="card"><h2>{c.name}</h2><p><span class="pill">{c.systemType}</span><span class="pill">{c.status}</span></p><p>{c.baseUrl}</p><small>Credential: {c.credentialReference}</small></article>{/each}</div>{/if}
