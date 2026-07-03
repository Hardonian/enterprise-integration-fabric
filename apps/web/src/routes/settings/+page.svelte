<script lang="ts">
  let bearerToken = $state('');
  let devTenantIds = $state('');
  let saved = $state(false);
  $effect(() => {
    bearerToken = localStorage.getItem('fabric.bearerToken') ?? '';
    devTenantIds = localStorage.getItem('fabric.devTenantIds') ?? '';
  });
  function save() {
    if (bearerToken.trim()) localStorage.setItem('fabric.bearerToken', bearerToken.trim()); else localStorage.removeItem('fabric.bearerToken');
    if (devTenantIds.trim()) localStorage.setItem('fabric.devTenantIds', devTenantIds.trim()); else localStorage.removeItem('fabric.devTenantIds');
    saved = true;
    setTimeout(() => saved = false, 2000);
  }
</script>
<h1>Dashboard settings</h1>
<section class="card">
  <h2>API authentication</h2>
  <p>Production mode requires a Keycloak-issued bearer token. Paste a short-lived token here for operational dashboard calls. Tokens are stored only in this browser's localStorage.</p>
  <label for="token">Bearer token</label><br />
  <textarea id="token" bind:value={bearerToken} rows="6" style="width:100%;max-width:900px" aria-label="Bearer token"></textarea>
  <h3>Local dev tenant scope</h3>
  <p>When the API runs with DEV_AUTH_ENABLED=true, this optional comma-separated list sends X-Dev-Tenant-Ids so local testing still exercises tenant isolation.</p>
  <label for="devTenants">Dev tenant ids</label><br />
  <input id="devTenants" bind:value={devTenantIds} placeholder="tenant-north,tenant-east" style="width:100%;max-width:480px" />
  <p><button onclick={save}>Save settings</button> {#if saved}<span class="pill">Saved</span>{/if}</p>
</section>
