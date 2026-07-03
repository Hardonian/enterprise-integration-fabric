<script lang="ts">
import { apiGet } from '$lib/api';
type Tenant = { id:string; name:string; status:string };
let result = $state({ data: null as Tenant[] | null, error: null as string | null, loading: true });
$effect(() => { apiGet<Tenant[]>('/api/tenants').then(r => result = r); });
</script>
<h1>Tenants</h1>{#if result.loading}<p>Loading tenants…</p>{:else if result.error}<section class="card error"><h2>API unavailable</h2><p>{result.error}</p></section>{:else if !result.data?.length}<p class="empty">No tenants found.</p>{:else}<div class="grid">{#each result.data as t}<article class="card"><h2>{t.name}</h2><p><span class="pill">{t.id}</span><span class="pill">{t.status}</span></p></article>{/each}</div>{/if}
