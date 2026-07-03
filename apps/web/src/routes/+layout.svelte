<script lang="ts">
  let nav = [
    ['/', 'Overview'], ['/tenants', 'Tenants'], ['/connectors', 'Connectors'], ['/traces', 'Trace Search'], ['/dlq', 'DLQ'], ['/contracts', 'Contracts'], ['/architecture', 'Architecture'], ['/onboarding', 'Onboarding'], ['/settings', 'Settings']
  ];
</script>
<svelte:head><title>Enterprise Integration Fabric</title></svelte:head>
<header class="top"><div><strong>Enterprise Integration Fabric</strong><span>Vendor-neutral LMS/SIS/CRM/Billing integration layer</span></div></header>
<nav aria-label="Primary"><ul>{#each nav as item}<li><a href={item[0]}>{item[1]}</a></li>{/each}</ul></nav>
<main><slot /></main>
<style>
:global(body){margin:0;font-family:Inter,system-ui,sans-serif;background:#0f172a;color:#e5e7eb}.top{padding:1rem 1.25rem;background:#111827;border-bottom:1px solid #334155}.top div{display:flex;gap:1rem;align-items:baseline;flex-wrap:wrap}.top span{color:#93c5fd}nav{background:#1e293b}ul{display:flex;gap:.5rem;list-style:none;padding:.75rem 1rem;margin:0;flex-wrap:wrap}a{color:#dbeafe;text-decoration:none;padding:.5rem .75rem;border-radius:.5rem}a:hover,a:focus{background:#334155}main{padding:1rem;max-width:1180px;margin:0 auto}:global(.card){background:#111827;border:1px solid #334155;border-radius:.75rem;padding:1rem;margin:.75rem 0}:global(.grid){display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,1fr));gap:1rem}:global(.error){border-left:4px solid #f87171}:global(.empty){color:#cbd5e1}:global(.pill){display:inline-block;border:1px solid #475569;border-radius:999px;padding:.15rem .5rem;margin:.1rem;color:#bfdbfe}:global(input),:global(button){font:inherit;padding:.6rem;border-radius:.5rem;border:1px solid #475569}:global(button){background:#2563eb;color:white;cursor:pointer}@media(max-width:640px){main{padding:.75rem}ul{display:block}li{margin:.5rem 0}}
</style>
