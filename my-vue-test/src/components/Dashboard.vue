<!-- Dashboard.vue -->
<script setup>
import MrijaControl from '@/components/MrijaControl.vue'
import ApiLogsManager from '@/components/ApiLogsManager.vue'
import AiAgentConsole from '@/components/AiAgentConsole.vue'
</script>

<template>
  <!-- Просто обертка без Bootstrap-классов колонок, так как они уже есть в AdminLayout -->
  <div class="dashboard-content pb-5">
    <MrijaControl />
    <ApiLogsManager />
    <AiAgentConsole />
  </div>
</template>