<script setup>
import { computed, ref } from "vue";
import { VyIcon } from "./ui.js";

const props = defineProps({
  modelValue: {
    type: [Array, String, Number],
    default: () => []
  },
  options: {
    type: Array,
    default: () => []
  },
  labelKey: {
    type: String,
    default: "label"
  },
  valueKey: {
    type: String,
    default: "value"
  },
  placeholder: {
    type: String,
    default: "Buscar..."
  },
  emptyText: {
    type: String,
    default: "Sin resultados"
  },
  multiple: {
    type: Boolean,
    default: true
  }
});

const emit = defineEmits(["update:modelValue"]);

const query = ref("");
const open = ref(false);

const selectedValues = computed(() => {
  if (props.multiple) {
    return Array.isArray(props.modelValue) ? props.modelValue : [];
  }

  return props.modelValue === "" || props.modelValue === null || props.modelValue === undefined
    ? []
    : [props.modelValue];
});

const selectedOptions = computed(() => props.options.filter((option) => {
  return selectedValues.value.map(String).includes(String(option[props.valueKey]));
}));

const filteredOptions = computed(() => {
  const normalizedQuery = query.value.trim().toLowerCase();
  const selected = new Set(selectedValues.value.map(String));

  return props.options.filter((option) => {
    const value = String(option[props.valueKey]);
    const label = String(option[props.labelKey] || "").toLowerCase();
    return !selected.has(value) && (!normalizedQuery || label.includes(normalizedQuery));
  });
});

function selectOption(option) {
  emit("update:modelValue", props.multiple ? [...selectedValues.value, option[props.valueKey]] : option[props.valueKey]);
  query.value = "";
  open.value = props.multiple;
}

function removeOption(value) {
  if (props.multiple) {
    emit("update:modelValue", selectedValues.value.filter((item) => String(item) !== String(value)));
    return;
  }

  emit("update:modelValue", "");
}

function closeLater() {
  window.setTimeout(() => {
    open.value = false;
    query.value = "";
  }, 120);
}
</script>

<template>
  <div class="vy-autocomplete" @focusin="open = true" @focusout="closeLater">
    <div class="select-box" :class="{ active: open }">
      <span v-for="option in selectedOptions" :key="option[valueKey]" class="selected-chip">
        {{ option[labelKey] }}
        <button type="button" @click.stop="removeOption(option[valueKey])">×</button>
      </span>

      <input
        v-model="query"
        type="text"
        :placeholder="selectedOptions.length ? '' : placeholder"
        @focus="open = true"
      />

      <VyIcon name="chevD" :size="14" stroke="var(--vy-ink-3)" />
    </div>

    <div v-if="open" class="options-menu">
      <button
        v-for="option in filteredOptions"
        :key="option[valueKey]"
        type="button"
        @mousedown.prevent="selectOption(option)"
      >
        {{ option[labelKey] }}
      </button>
      <div v-if="!filteredOptions.length" class="empty-option">{{ emptyText }}</div>
    </div>
  </div>
</template>

<style scoped>
.vy-autocomplete {
  position: relative;
}

.select-box {
  min-height: 44px;
  width: 100%;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  padding: 7px 10px;
  border: 1px solid var(--vy-line);
  border-radius: 10px;
  background: var(--vy-surface);
}

.select-box.active {
  outline: 2px solid rgba(242, 135, 5, 0.22);
  border-color: var(--vy-orange);
}

.select-box input {
  min-width: 120px;
  flex: 1;
  border: 0;
  outline: 0;
  padding: 4px 0;
  background: transparent;
  color: var(--vy-ink);
  font: inherit;
  font-size: 13px;
  font-weight: 600;
}

.selected-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  border-radius: 999px;
  background: var(--vy-cream);
  color: #6b4a12;
  font-size: 12px;
  font-weight: 800;
}

.selected-chip button {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: rgba(31, 26, 20, 0.12);
  color: var(--vy-ink);
  line-height: 1;
  font-size: 13px;
}

.options-menu {
  position: absolute;
  left: 0;
  right: 0;
  top: calc(100% + 6px);
  z-index: 40;
  max-height: 220px;
  overflow: auto;
  padding: 6px;
  border: 1px solid var(--vy-line);
  border-radius: 12px;
  background: var(--vy-surface);
  box-shadow: var(--vy-shadow-lg);
}

.options-menu button,
.empty-option {
  width: 100%;
  padding: 9px 10px;
  border-radius: 9px;
  text-align: left;
  font-size: 13px;
  font-weight: 700;
  color: var(--vy-ink-2);
}

.options-menu button:hover {
  background: var(--vy-surface-2);
  color: var(--vy-ink);
}

.empty-option {
  color: var(--vy-ink-3);
}
</style>
