<script lang="ts">
  import { createEventDispatcher } from 'svelte';

  const dispatch = createEventDispatcher();
  function click(): void {
    dispatch('click');
  }
</script>

<button class="our-button" on:click={click}>
  <slot />
</button>

<style lang="scss">
  @import './button';
</style>
