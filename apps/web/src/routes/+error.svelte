<script lang="ts">import { page } from '$app/stores';</script>
<section class="card error"><h1>Dashboard route unavailable</h1><p>{$page.error?.message ?? 'Unknown route error'}</p><p>The dashboard failed gracefully; check API and route configuration.</p></section>
