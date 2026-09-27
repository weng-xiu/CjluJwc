<template>
  <component :is="type" v-bind="linkProps(to)">
    <slot />
  </component>
</template>

<script>
import { isExternal } from '@/utils/validate'

export default {
  name: 'AppLink',
  props: {
    to: { type: [String, Object], required: true }
  },
  computed: {
    isExternal() {
      return isExternal(typeof this.to === 'string' ? this.to : this.to.path)
    },
    type() {
      return this.isExternal ? 'a' : 'router-link'
    }
  },
  methods: {
    linkProps(to) {
      if (this.isExternal) {
        return { href: to, target: '_blank', rel: 'noopener' }
      }
      return { to: to }
    }
  }
}
</script>
