<template>
  <div v-if="visible" class="image-preview-mask" @click.self="handleClose">
    <div class="preview-header">
      <span class="preview-filename">{{ fileName }}</span>
      <div class="preview-actions">
        <button class="preview-btn" @click="handleDownload" title="下载图片">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
            <polyline points="7 10 12 15 17 10"></polyline>
            <line x1="12" y1="15" x2="12" y2="3"></line>
          </svg>
        </button>
        <button class="preview-btn" @click="handleClose" title="关闭">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"></line>
            <line x1="6" y1="6" x2="18" y2="18"></line>
          </svg>
        </button>
      </div>
    </div>
    <div class="preview-image-wrapper">
      <img :src="imageUrl" :alt="fileName" class="preview-image"/>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ImagePreview',
  props: {
    visible: {type: Boolean, default: false},
    imageUrl: {type: String, default: ''},
    fileName: {type: String, default: 'image.png'}
  },
  methods: {
    handleClose() {
      this.$emit('close')
    },
    handleDownload() {
      if (!this.imageUrl) return
      const a = document.createElement('a')
      a.href = this.imageUrl
      a.download = this.fileName
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
    }
  }
}
</script>

<style scoped>
.image-preview-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.85);
  z-index: 9999;
  display: flex;
  flex-direction: column;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  color: #e5e5e5;
  background: rgba(0, 0, 0, 0.5);
}

.preview-filename {
  font-size: 14px;
  color: #e5e5e5;
}

.preview-actions {
  display: flex;
  gap: 8px;
}

.preview-btn {
  width: 36px;
  height: 36px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #e5e5e5;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.preview-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.preview-image-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  overflow: auto;
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 8px;
}
</style>
