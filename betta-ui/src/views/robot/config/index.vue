<template>
  <div class="app-container">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="通道配置" name="config">
        <channel-config :showSearch.sync="showSearch" ref="channelConfig" />
      </el-tab-pane>
      <el-tab-pane label="消息记录" name="record">
        <message-record :showSearch.sync="showSearch" :activeTab="activeTab" ref="messageRecord" />
      </el-tab-pane>
      <el-tab-pane label="大模型配置" name="llm">
        <llm-config :showSearch.sync="showSearch" :activeTab="activeTab" ref="llmConfig" />
      </el-tab-pane>
      <el-tab-pane label="API配置" name="api">
        <api-config :showSearch.sync="showSearch" :activeTab="activeTab" ref="apiConfig" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import ChannelConfig from './ChannelConfig'
import MessageRecord from './MessageRecord'
import LlmConfig from './LlmConfig'
import ApiConfig from './ApiConfig'

export default {
  name: 'MessageConfig',
  components: {
    ChannelConfig,
    MessageRecord,
    LlmConfig,
    ApiConfig
  },
  data() {
    return {
      activeTab: 'config',
      showSearch: true
    }
  }
}
</script>


