<template>
  <div class="publish-view">
    <div class="mf-container">
      <div class="publish-card mf-card">
        <h2 class="page-title"><span class="bar"></span>发布动态</h2>
        <p class="page-sub">分享你的生活，让心动的人看见你 ✨</p>

        <el-form ref="formRef" :model="form" :rules="rules" size="large">
          <el-form-item prop="content">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="6"
              placeholder="这一刻的想法..."
              maxlength="500"
              show-word-limit
              resize="none"
            />
          </el-form-item>

          <!-- 图片上传 -->
          <el-form-item>
            <div class="upload-area">
              <div class="img-list">
                <div class="img-item" v-for="(img, i) in imageList" :key="i">
                  <img :src="img" alt="" />
                  <div class="img-remove" @click="removeImage(i)">
                    <el-icon><Close /></el-icon>
                  </div>
                </div>
                <div class="upload-trigger" v-if="imageList.length < 9" @click="triggerUpload">
                  <el-icon class="up-icon"><Plus /></el-icon>
                  <span>添加图片</span>
                  <span class="count">{{ imageList.length }}/9</span>
                </div>
              </div>
              <input ref="fileInput" type="file" accept="image/*" multiple hidden @change="onFileChange" />
            </div>
          </el-form-item>

          <!-- 话题标签 -->
          <el-form-item>
            <div class="topic-line">
              <span class="topic-label"><el-icon><PriceTag /></el-icon>话题</span>
              <span
                class="topic-chip"
                v-for="t in topics"
                :key="t"
                :class="{ active: form.topic === t }"
                @click="form.topic = form.topic === t ? '' : t"
              >#{{ t }}</span>
            </div>
          </el-form-item>

          <el-form-item>
            <div class="form-actions">
              <button type="button" class="mf-btn is-ghost" @click="$router.back()">取消</button>
              <button type="button" class="mf-btn" :class="{ loading }" @click="onPublish" :disabled="loading">
                <span v-if="!loading"><el-icon><Promotion /></el-icon>发布</span>
                <span v-else class="loader"></span>
              </button>
            </div>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Close, PriceTag, Promotion } from '@element-plus/icons-vue'
import { publishDynamic, uploadImage } from '@/api/dynamic'
import { resolveImage } from '@/utils/format'
import { ElMessage } from 'element-plus'

const router = useRouter()
const formRef = ref(null)
const fileInput = ref(null)
const loading = ref(false)
const imageList = ref([])

const topics = ['日常', '旅行', '美食', '心情', '摄影', '运动']

const form = reactive({
  content: '',
  images: '',
  topic: ''
})

const rules = {
  content: [
    { required: true, message: '请输入动态内容', trigger: 'blur' },
    { min: 1, max: 500, message: '内容长度 1-500 字', trigger: 'blur' }
  ]
}

function triggerUpload() {
  fileInput.value && fileInput.value.click()
}

async function onFileChange(e) {
  const files = Array.from(e.target.files || [])
  if (!files.length) return
  const remain = 9 - imageList.value.length
  if (files.length > remain) {
    ElMessage.warning(`最多上传9张，已选${remain}张`)
  }
  for (const file of files.slice(0, remain)) {
    if (file.size > 5 * 1024 * 1024) {
      ElMessage.warning(`${file.name} 超过5MB，已跳过`)
      continue
    }
    try {
      const res = await uploadImage(file)
      const url = (res.data && res.data.url) || res.data || res.url
      imageList.value.push(resolveImage(url))
    } catch (err) {
      // 上传失败跳过
    }
  }
  e.target.value = ''
}

function removeImage(i) {
  imageList.value.splice(i, 1)
}

async function onPublish() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  loading.value = true
  try {
    const content = form.topic ? `#${form.topic}# ${form.content}` : form.content
    await publishDynamic({
      content,
      images: imageList.value.join(',')
    })
    ElMessage.success('发布成功')
    router.push('/dynamic')
  } catch (e) {
    // 忽略
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.publish-view {
  padding: 30px 0 40px;
}

.publish-card {
  max-width: 720px;
  margin: 0 auto;
  padding: 36px 40px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 24px;
  font-weight: 800;
  color: #2d2d3a;
  margin: 0 0 6px;

  .bar {
    width: 6px;
    height: 24px;
    border-radius: 3px;
    background: linear-gradient(135deg, #ff6b9d 0%, #a855f7 100%);
  }
}

.page-sub {
  margin: 0 0 28px;
  font-size: 14px;
  color: #9a9aaa;
  padding-left: 16px;
}

.upload-area {
  width: 100%;
}

.img-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
}

.img-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 12px;
  overflow: hidden;
  background: #f5f5f7;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .img-remove {
    position: absolute;
    top: 6px;
    right: 6px;
    width: 24px;
    height: 24px;
    border-radius: 50%;
    background: rgba(0, 0, 0, 0.5);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: background 0.2s;
    &:hover {
      background: rgba(244, 63, 95, 0.85);
    }
  }
}

.upload-trigger {
  aspect-ratio: 1;
  border: 2px dashed #e9d5ff;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  cursor: pointer;
  color: #a855f7;
  transition: all 0.25s ease;
  background: #faf5ff;

  .up-icon {
    font-size: 28px;
  }
  span {
    font-size: 13px;
  }
  .count {
    font-size: 11px;
    color: #aaa;
  }

  &:hover {
    border-color: #a855f7;
    background: #f3e8ff;
    transform: scale(1.02);
  }
}

.topic-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;

  .topic-label {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-size: 14px;
    font-weight: 600;
    color: #4a4a5a;
  }

  .topic-chip {
    padding: 5px 14px;
    border-radius: 999px;
    background: #f5f5f7;
    color: #7a7a8a;
    font-size: 13px;
    cursor: pointer;
    transition: all 0.25s ease;

    &:hover {
      background: #fff0f5;
      color: #ff4f8b;
    }

    &.active {
      background: linear-gradient(135deg, #ff6b9d 0%, #a855f7 100%);
      color: #fff;
    }
  }
}

.form-actions {
  display: flex;
  gap: 14px;
  width: 100%;
  justify-content: flex-end;
  .mf-btn {
    display: inline-flex;
    align-items: center;
    gap: 5px;
  }
}

.loader {
  width: 20px;
  height: 20px;
  border: 2.5px solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spinSlow 0.8s linear infinite;
  display: inline-block;
}

:deep(.el-textarea__inner) {
  font-size: 15px;
  line-height: 1.7;
}

/* ================== PublishView 响应式 ================== */
@media (max-width: 1023px) {
  .publish-view { padding: 20px 0 30px; }
}
@media (max-width: 767px) {
  .publish-view { padding: 14px 0 24px; }
  .page-head { margin-bottom: 14px; }
  .page-title { font-size: 19px; gap: 8px; .bar { height: 18px; width: 4px; } }
  .page-sub { font-size: 13px; padding-left: 12px; }
  .publish-card { padding: 14px 12px !important; border-radius: 12px !important; }
  .content-textarea { min-height: 120px; }
  .uploader-list { grid-template-columns: repeat(3, 1fr) !important; gap: 8px !important; }
  .upload-item { height: 96px; }
  .meta-row { flex-direction: column; align-items: stretch; gap: 10px; }
  .meta-item { width: 100%; }
  .form-actions { flex-direction: column-reverse; justify-content: center; gap: 10px; }
  .form-actions .mf-btn, .form-actions .el-button { width: 100%; justify-content: center; }
}
@media (max-width: 479px) {
  .page-title { font-size: 17px; }
  .page-sub { font-size: 12px; padding-left: 9px; }
  .publish-card { padding: 12px 10px !important; }
  .uploader-list { grid-template-columns: repeat(3, 1fr) !important; gap: 6px !important; }
  .upload-item { height: 84px; border-radius: 10px; }
  .add-uploader { font-size: 11px; }
}
</style>
