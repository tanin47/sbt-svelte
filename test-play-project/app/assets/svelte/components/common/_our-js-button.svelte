<script lang="ts">
  import { createEventDispatcher } from 'svelte';

  const dispatch = createEventDispatcher();

  function click(): void {
    dispatch('click');
  }

  export function call(): void {
    console.log('our-js-button receives the message.');
  };
</script>

<button class="our-js-button bg-sky-500 p-2 rounded-lg" on:click={click}>
  <slot />
</button>

<style lang="scss">
</style>
