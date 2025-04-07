<script lang="ts">
  import ComponentB from './dependencies/_component-b.svelte';
</script>

<div class="beautiful-box">A<ComponentB /></div>

<style lang="scss">
  @use './dependencies/style';
</style>
