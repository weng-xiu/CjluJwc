<template>
  <div style="padding: 16px" @click="toggleClick">
    <svg
      :class="{ 'is-active': isActive }"
      class="hamburger"
      viewBox="0 0 64 64"
      xmlns="http://www.w3.org/2000/svg"
      width="18"
      height="18"
    >
      <path
        d="M16 16h48v8H16zM16 28h48v8H16zM16 40h48v8H16z"
        fill="currentColor"
      />
    </svg>
  </div>
</template>

<script>
export default {
  name: 'Hamburger',
  props: {
    isActive: { type: Boolean, default: false }
  },
  emits: ['toggleClick'],
  methods: {
    toggleClick() {
      this.$emit('toggleClick')
    }
  }
}
</script>

<style scoped>
.hamburger {
  display: inline-block;
  vertical-align: middle;
  width: 20px;
  height: 20px;
}
.hamburger.is-active {
  transform: rotate(180deg);
}
</style>
