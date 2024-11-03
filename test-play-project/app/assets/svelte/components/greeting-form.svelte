<script lang="ts">
  import OurButton from './common/_our-button.svelte';
  import OurJsButton from './common/_our-js-button.svelte';

  export let greeting: string;
  let toggleValue = false;
  let toggleText: string;
  let jsButton: OurJsButton;

  $: if (toggleValue) {
    toggleText = 'on';
  } else {
    toggleText = 'off';
  }

  function toggle(): void {
    toggleValue = !toggleValue;
    jsButton.call();
  }
</script>

<div class="greeting-form bg-yellow-800">
  <h1>{greeting}</h1>
  <p data-test-id="text">Value: {toggleText}</p>
  <OurButton on:click={toggle}>Toggle Typescript button</OurButton>
  <OurJsButton on:click={toggle} bind:this={jsButton}>Toggle Javascript button</OurJsButton>
</div>

<style lang="scss">
  .greeting-form {
    display: block;
    text-align: center;
    padding: 25px 0px;
  }
</style>
