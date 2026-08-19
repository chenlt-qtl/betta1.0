<template>
  <div class="group-tags">
    <el-tag
      size="small"
      v-for="i in groupList"
      :key="i.id"
      :type="i.id == value ? '' : 'info'"
      @click="() => onTagClick(i)"
    >
      {{ i.name }}
    </el-tag>
  </div>
</template>

<script>
export default {
  props: ["groupList", "value"],
  data() {
    return {};
  },
  methods: {
    onTagClick(group) {
      this.$emit("input", group.id);
    },
  },
};
</script>

<style scoped lang="scss">
.group-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  span {
    cursor: pointer;
  }
}
</style>
