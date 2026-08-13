<template>
  <div class="edit-profile-view">
    <div class="mf-container">
      <div class="edit-card mf-card">
        <h2 class="page-title"><span class="bar"></span>编辑个人资料</h2>
        <p class="page-sub">完善资料，让缘分更容易找到你 ✨</p>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" size="large" class="edit-form">
          <!-- 头像上传 -->
          <el-form-item label="头像">
            <div class="avatar-upload">
              <div class="avatar-preview">
                <el-avatar :size="100" :src="avatarUrl" />
                <div class="avatar-mask" @click="triggerUpload">
                  <el-icon><Camera /></el-icon>
                  <span>更换</span>
                </div>
              </div>
              <input ref="fileInput" type="file" accept="image/*" hidden @change="onAvatarChange" />
              <div class="avatar-tip">点击头像上传，支持 JPG/PNG</div>
            </div>
          </el-form-item>

          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="form.nickname" placeholder="请输入昵称" maxlength="20" show-word-limit />
          </el-form-item>

          <el-form-item label="性别" prop="gender">
            <el-radio-group v-model="form.gender">
              <el-radio :value="1">♂ 男</el-radio>
              <el-radio :value="2">♀ 女</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="生日" prop="birthday">
            <el-date-picker
              v-model="form.birthday"
              type="date"
              placeholder="选择生日"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>

          <el-form-item label="年龄" prop="age">
            <el-input-number v-model="form.age" :min="18" :max="99" />
          </el-form-item>

          <el-form-item label="身高(cm)" prop="height">
            <el-input-number v-model="form.height" :min="140" :max="220" />
          </el-form-item>

          <el-form-item label="体重(kg)" prop="weight">
            <el-input-number v-model="form.weight" :min="35" :max="150" />
          </el-form-item>

          <el-form-item label="城市" prop="city">
            <el-input v-model="form.city" placeholder="如：北京" :prefix-icon="Location" />
          </el-form-item>

          <!-- 职业：弹窗选择 ≤ 2 -->
          <el-form-item label="职业" prop="occupation">
            <div class="picker-field">
              <div class="picker-selected" v-if="occupationList.length">
                <span class="selected-tag" v-for="(o, i) in occupationList" :key="'occ-'+i">
                  {{ o }}
                  <el-icon class="remove" @click="removeOccupation(i)"><Close /></el-icon>
                </span>
                <span class="picker-count">已选 {{ occupationList.length }}/2</span>
              </div>
              <div class="picker-empty" v-else-if="!occupationDictLoading">
                暂未选择职业
              </div>
              <div class="picker-empty" v-else>
                <span class="loader-inline"></span> 加载中...
              </div>
              <el-button class="picker-btn" type="primary" plain @click="openOccupationDialog">
                <el-icon><Briefcase /></el-icon>
                选择职业
              </el-button>
            </div>
          </el-form-item>

          <el-form-item label="个性签名" prop="signature">
            <el-input
              v-model="form.signature"
              type="textarea"
              :rows="3"
              placeholder="写一句介绍自己，让别人更了解你~"
              maxlength="100"
              show-word-limit
            />
          </el-form-item>

          <!-- 兴趣爱好：弹窗选择 ≤ 10 -->
          <el-form-item label="兴趣爱好" prop="hobbies">
            <div class="picker-field">
              <div class="picker-selected hobby-selected" v-if="hobbyList.length">
                <span class="selected-tag hobby-tag-pretty" v-for="(h, i) in hobbyList" :key="'hb-'+i">
                  {{ h }}
                  <el-icon class="remove" @click="removeHobby(i)"><Close /></el-icon>
                </span>
                <span class="picker-count" :class="{ full: hobbyList.length >= 10 }">
                  已选 {{ hobbyList.length }}/10
                </span>
              </div>
              <div class="picker-empty" v-else-if="!hobbyDictLoading">
                暂未选择兴趣爱好
              </div>
              <div class="picker-empty" v-else>
                <span class="loader-inline"></span> 加载中...
              </div>
              <el-button class="picker-btn hobby-btn" type="primary" plain @click="openHobbyDialog">
                <el-icon><Star /></el-icon>
                选择爱好
              </el-button>
            </div>
          </el-form-item>

          <el-form-item>
            <div class="form-actions">
              <button type="button" class="mf-btn is-ghost" @click="router.back()">取消</button>
              <button type="button" class="mf-btn" :class="{ loading }" @click="onSave" :disabled="loading">
                <span v-if="!loading">保存资料</span>
                <span v-else class="loader"></span>
              </button>
            </div>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 职业选择弹窗 -->
    <el-dialog
      v-model="occupationDialogVisible"
      title="选择职业"
      width="680px"
      :close-on-click-modal="false"
      destroy-on-close
      class="dict-dialog"
    >
      <div class="dialog-search">
        <el-input v-model="occupationSearch" placeholder="搜索职业，如：设计师" :prefix-icon="Search" clearable />
      </div>
      <div class="dialog-selected-bar">
        <span class="bar-label">已选 <b>{{ tempOccupationList.length }}</b>/2</span>
        <div class="bar-tags" v-if="tempOccupationList.length">
          <span class="selected-tag" v-for="(o, i) in tempOccupationList" :key="'temp-occ-'+i">
            {{ o }}
            <el-icon class="remove" @click="tempOccupationList.splice(i, 1)"><Close /></el-icon>
          </span>
        </div>
        <span class="bar-hint" v-else>点击下方选项进行选择，最多 2 个</span>
      </div>
      <div class="dialog-scroll">
        <div class="dict-grid" v-if="!occupationSearch && occupationDictByCat.length">
          <div class="dict-cat" v-for="cat in occupationDictByCat" :key="'oc-'+cat.name">
            <div class="dict-cat-title">{{ cat.name }}</div>
            <div class="dict-chips">
              <div
                class="dict-chip"
                :class="{
                  active: tempOccupationList.includes(item.name),
                  disabled: !tempOccupationList.includes(item.name) && tempOccupationList.length >= 2
                }"
                v-for="item in cat.items"
                :key="'o-'+item.id"
                @click="toggleTempOccupation(item)"
              >
                <span class="chip-icon" v-if="item.icon">{{ item.icon }}</span>
                <span class="chip-name">{{ item.name }}</span>
              </div>
            </div>
          </div>
        </div>
        <div class="dict-grid" v-else-if="occupationSearch">
          <div class="dict-chips search-results">
            <div
              class="dict-chip"
              :class="{
                active: tempOccupationList.includes(item.name),
                disabled: !tempOccupationList.includes(item.name) && tempOccupationList.length >= 2
              }"
              v-for="item in filteredOccupations"
              :key="'o-search-'+item.id"
              @click="toggleTempOccupation(item)"
            >
              <span class="chip-icon" v-if="item.icon">{{ item.icon }}</span>
              <span class="chip-name">{{ item.name }}</span>
              <span class="chip-cat-tag">{{ item.category }}</span>
            </div>
          </div>
          <div class="no-result" v-if="filteredOccupations.length === 0">未找到相关职业</div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="clearTempOccupation">清空</el-button>
          <div class="footer-right">
            <el-button @click="occupationDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="confirmOccupation">确定</el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- 兴趣爱好选择弹窗 -->
    <el-dialog
      v-model="hobbyDialogVisible"
      title="选择兴趣爱好"
      width="720px"
      :close-on-click-modal="false"
      destroy-on-close
      class="dict-dialog hobby-dialog"
    >
      <div class="dialog-search">
        <el-input v-model="hobbySearch" placeholder="搜索爱好，如：摄影、旅行" :prefix-icon="Search" clearable />
      </div>
      <div class="dialog-selected-bar">
        <span class="bar-label">已选 <b :class="{ full: tempHobbyList.length >= 10 }">{{ tempHobbyList.length }}</b>/10</span>
        <div class="bar-tags" v-if="tempHobbyList.length">
          <span class="selected-tag hobby-tag-pretty" v-for="(h, i) in tempHobbyList" :key="'temp-hob-'+i">
            {{ h }}
            <el-icon class="remove" @click="tempHobbyList.splice(i, 1)"><Close /></el-icon>
          </span>
        </div>
        <span class="bar-hint" v-else>点击下方选项进行选择，最多 10 个</span>
      </div>
      <div class="dialog-scroll">
        <div class="dict-grid" v-if="!hobbySearch && hobbyDictByCat.length">
          <div class="dict-cat" v-for="cat in hobbyDictByCat" :key="'hc-'+cat.name">
            <div class="dict-cat-title">{{ cat.name }}</div>
            <div class="dict-chips">
              <div
                class="dict-chip hobby-chip"
                :class="{
                  active: tempHobbyList.includes(item.name),
                  disabled: !tempHobbyList.includes(item.name) && tempHobbyList.length >= 10
                }"
                v-for="item in cat.items"
                :key="'h-'+item.id"
                @click="toggleTempHobby(item)"
              >
                <span class="chip-icon" v-if="item.icon">{{ item.icon }}</span>
                <span class="chip-name">{{ item.name }}</span>
              </div>
            </div>
          </div>
        </div>
        <div class="dict-grid" v-else-if="hobbySearch">
          <div class="dict-chips search-results">
            <div
              class="dict-chip hobby-chip"
              :class="{
                active: tempHobbyList.includes(item.name),
                disabled: !tempHobbyList.includes(item.name) && tempHobbyList.length >= 10
              }"
              v-for="item in filteredHobbies"
              :key="'h-search-'+item.id"
              @click="toggleTempHobby(item)"
            >
              <span class="chip-icon" v-if="item.icon">{{ item.icon }}</span>
              <span class="chip-name">{{ item.name }}</span>
              <span class="chip-cat-tag">{{ item.category }}</span>
            </div>
          </div>
          <div class="no-result" v-if="filteredHobbies.length === 0">未找到相关爱好</div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="clearTempHobby">清空</el-button>
          <div class="footer-right">
            <el-button @click="hobbyDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="confirmHobby">确定</el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Location, Briefcase, Camera, Close, Star, Search } from '@element-plus/icons-vue'
import { updateUser, uploadAvatar } from '@/api/user'
import { getDictAll } from '@/api/dict'
import { useUserStore } from '@/stores/user'
import { resolveAvatar } from '@/utils/format'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const fileInput = ref(null)
const loading = ref(false)

const hobbyList = ref([])
const occupationList = ref([])

const hobbyDictLoading = ref(true)
const occupationDictLoading = ref(true)
const hobbyDict = ref([])
const occupationDict = ref([])

// 弹窗状态
const occupationDialogVisible = ref(false)
const hobbyDialogVisible = ref(false)
const occupationSearch = ref('')
const hobbySearch = ref('')
const tempOccupationList = ref([])
const tempHobbyList = ref([])

const form = reactive({
  nickname: '',
  gender: 2,
  birthday: '',
  age: 18,
  height: 170,
  weight: 55,
  city: '',
  occupation: '',
  signature: '',
  hobbies: ''
})

const rules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度 2-20 位', trigger: 'blur' }
  ],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }]
}

const avatarUrl = computed(() => resolveAvatar(userStore.userInfo && userStore.userInfo.avatar, form.nickname))

const occupationDictByCat = computed(() => {
  const map = new Map()
  for (const item of occupationDict.value || []) {
    const key = item.category || '其他'
    if (!map.has(key)) map.set(key, [])
    map.get(key).push(item)
  }
  const result = []
  map.forEach((items, name) => result.push({ name, items }))
  return result
})

const hobbyDictByCat = computed(() => {
  const map = new Map()
  for (const item of hobbyDict.value || []) {
    const key = item.category || '其他'
    if (!map.has(key)) map.set(key, [])
    map.get(key).push(item)
  }
  const result = []
  map.forEach((items, name) => result.push({ name, items }))
  return result
})

const filteredOccupations = computed(() => {
  if (!occupationSearch.value) return []
  const kw = occupationSearch.value.trim().toLowerCase()
  return (occupationDict.value || []).filter(item =>
    item.name.toLowerCase().includes(kw) ||
    (item.category && item.category.toLowerCase().includes(kw))
  )
})

const filteredHobbies = computed(() => {
  if (!hobbySearch.value) return []
  const kw = hobbySearch.value.trim().toLowerCase()
  return (hobbyDict.value || []).filter(item =>
    item.name.toLowerCase().includes(kw) ||
    (item.category && item.category.toLowerCase().includes(kw))
  )
})

function triggerUpload() {
  fileInput.value && fileInput.value.click()
}

async function onAvatarChange(e) {
  const file = e.target.files[0]
  if (!file) return
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 5MB')
    return
  }
  try {
    const res = await uploadAvatar(file)
    const url = (res.data && res.data.url) || res.data || res.url
    await updateUser({ avatar: url })
    const info = { ...(userStore.userInfo || {}) }
    info.avatar = url
    userStore.setUserInfo(info)
    ElMessage.success('头像上传成功')
  } catch (err) {
  } finally {
    e.target.value = ''
  }
}

/* -------- 职业操作 -------- */
function openOccupationDialog() {
  tempOccupationList.value = [...occupationList.value]
  occupationSearch.value = ''
  occupationDialogVisible.value = true
}
function toggleTempOccupation(item) {
  const name = item.name
  const idx = tempOccupationList.value.indexOf(name)
  if (idx >= 0) {
    tempOccupationList.value.splice(idx, 1)
    return
  }
  if (tempOccupationList.value.length >= 2) {
    ElMessage.warning('最多选择 2 个职业')
    return
  }
  tempOccupationList.value.push(name)
}
function clearTempOccupation() {
  tempOccupationList.value = []
}
function confirmOccupation() {
  occupationList.value = [...tempOccupationList.value]
  occupationDialogVisible.value = false
}
function removeOccupation(i) {
  occupationList.value.splice(i, 1)
}

/* -------- 兴趣爱好操作 -------- */
function openHobbyDialog() {
  tempHobbyList.value = [...hobbyList.value]
  hobbySearch.value = ''
  hobbyDialogVisible.value = true
}
function toggleTempHobby(item) {
  const name = item.name
  const idx = tempHobbyList.value.indexOf(name)
  if (idx >= 0) {
    tempHobbyList.value.splice(idx, 1)
    return
  }
  if (tempHobbyList.value.length >= 10) {
    ElMessage.warning('最多选择 10 个兴趣爱好')
    return
  }
  tempHobbyList.value.push(name)
}
function clearTempHobby() {
  tempHobbyList.value = []
}
function confirmHobby() {
  hobbyList.value = [...tempHobbyList.value]
  hobbyDialogVisible.value = false
}
function removeHobby(i) {
  hobbyList.value.splice(i, 1)
}

async function onSave() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  loading.value = true
  try {
    const data = {
      ...form,
      occupation: occupationList.value.join(','),
      hobbies: hobbyList.value.join(',')
    }
    await updateUser(data)
    const info = { ...(userStore.userInfo || {}), ...data }
    userStore.setUserInfo(info)
    ElMessage.success('保存成功')
    router.push('/profile')
  } catch (e) {
  } finally {
    loading.value = false
  }
}

async function loadDicts() {
  try {
    const res = await getDictAll()
    const data = res.data || res
    const hobbies = data.hobbies || []
    const occupations = data.occupations || []
    hobbyDict.value = hobbies
    occupationDict.value = occupations
    if (!hobbies.length) hobbyDict.value = fallbackHobbies
    if (!occupations.length) occupationDict.value = fallbackOccupations
  } catch (e) {
    hobbyDict.value = fallbackHobbies
    occupationDict.value = fallbackOccupations
  } finally {
    hobbyDictLoading.value = false
    occupationDictLoading.value = false
  }
}

function fillForm() {
  const info = userStore.userInfo || {}
  form.nickname = info.nickname || ''
  form.gender = info.gender || 2
  form.birthday = info.birthday || ''
  form.age = info.age || 18
  form.height = info.height || 170
  form.weight = info.weight || 55
  form.city = info.city || ''
  form.occupation = info.occupation || ''
  form.signature = info.signature || ''
  if (info.hobbies) {
    hobbyList.value = String(info.hobbies).split(/[,，、\s;；]+/).filter(Boolean)
  }
  if (info.occupation) {
    occupationList.value = String(info.occupation).split(/[,，、\s;；]+/).filter(Boolean)
  }
}

const fallbackHobbies = [
  { id: 1, name: '摄影', icon: '📷', category: '艺术', sort: 10 },
  { id: 2, name: '绘画', icon: '🎨', category: '艺术', sort: 20 },
  { id: 3, name: '音乐', icon: '🎵', category: '艺术', sort: 30 },
  { id: 4, name: '吉他', icon: '🎸', category: '艺术', sort: 40 },
  { id: 5, name: '钢琴', icon: '🎹', category: '艺术', sort: 50 },
  { id: 6, name: '唱歌', icon: '🎤', category: '艺术', sort: 60 },
  { id: 7, name: '跳舞', icon: '💃', category: '艺术', sort: 70 },
  { id: 8, name: '电影', icon: '🎬', category: '娱乐', sort: 110 },
  { id: 9, name: '追剧', icon: '📺', category: '娱乐', sort: 120 },
  { id: 10, name: '小说', icon: '📖', category: '娱乐', sort: 130 },
  { id: 11, name: '游戏', icon: '🎮', category: '娱乐', sort: 140 },
  { id: 12, name: '剧本杀', icon: '🎭', category: '娱乐', sort: 150 },
  { id: 13, name: '桌游', icon: '🎲', category: '娱乐', sort: 160 },
  { id: 14, name: '美食', icon: '🍜', category: '美食', sort: 210 },
  { id: 15, name: '咖啡', icon: '☕', category: '美食', sort: 220 },
  { id: 16, name: '烘焙', icon: '🍰', category: '美食', sort: 230 },
  { id: 17, name: '烹饪', icon: '🍳', category: '美食', sort: 240 },
  { id: 18, name: '旅行', icon: '✈️', category: '旅行', sort: 310 },
  { id: 19, name: '徒步', icon: '🥾', category: '户外', sort: 320 },
  { id: 20, name: '露营', icon: '🏕️', category: '户外', sort: 330 },
  { id: 21, name: '登山', icon: '⛰️', category: '户外', sort: 340 },
  { id: 22, name: '骑行', icon: '🚴', category: '户外', sort: 350 },
  { id: 23, name: '跑步', icon: '🏃', category: '运动', sort: 410 },
  { id: 24, name: '健身', icon: '🏋️', category: '运动', sort: 420 },
  { id: 25, name: '瑜伽', icon: '🧘', category: '运动', sort: 430 },
  { id: 26, name: '游泳', icon: '🏊', category: '运动', sort: 440 },
  { id: 27, name: '篮球', icon: '🏀', category: '运动', sort: 450 },
  { id: 28, name: '足球', icon: '⚽', category: '运动', sort: 460 },
  { id: 29, name: '羽毛球', icon: '🏸', category: '运动', sort: 470 },
  { id: 30, name: '乒乓球', icon: '🏓', category: '运动', sort: 480 },
  { id: 31, name: '网球', icon: '🎾', category: '运动', sort: 490 },
  { id: 32, name: '宠物', icon: '🐶', category: '生活', sort: 610 },
  { id: 33, name: '养猫', icon: '🐱', category: '生活', sort: 620 },
  { id: 34, name: '养花', icon: '🌷', category: '生活', sort: 630 },
  { id: 35, name: '手工', icon: '🧵', category: '生活', sort: 640 },
  { id: 36, name: '读书', icon: '📚', category: '学习', sort: 710 },
  { id: 37, name: '写作', icon: '✍️', category: '学习', sort: 720 },
  { id: 38, name: '编程', icon: '💻', category: '科技', sort: 810 },
  { id: 39, name: '数码', icon: '📱', category: '科技', sort: 820 },
  { id: 40, name: 'AI', icon: '🤖', category: '科技', sort: 830 },
  { id: 41, name: '投资', icon: '📈', category: '学习', sort: 840 },
  { id: 42, name: '理财', icon: '💰', category: '学习', sort: 850 },
  { id: 43, name: '动漫', icon: '🎌', category: '娱乐', sort: 930 },
  { id: 44, name: '展览', icon: '🖼️', category: '艺术', sort: 940 },
  { id: 45, name: '话剧', icon: '🎭', category: '艺术', sort: 950 },
  { id: 46, name: '博物馆', icon: '🏛️', category: '文化', sort: 960 },
  { id: 47, name: '钓鱼', icon: '🎣', category: '户外', sort: 1100 },
  { id: 48, name: '飞盘', icon: '🥏', category: '运动', sort: 1180 },
  { id: 49, name: '多肉', icon: '🌵', category: '生活', sort: 1200 },
  { id: 50, name: '插花', icon: '💐', category: '生活', sort: 1210 },
  { id: 51, name: '短视频', icon: '📹', category: '娱乐', sort: 1560 },
  { id: 52, name: '直播', icon: '📡', category: '娱乐', sort: 1570 },
  { id: 53, name: '脱口秀', icon: '🎙️', category: '娱乐', sort: 1620 }
]
const fallbackOccupations = [
  { id: 1, name: '学生', icon: '🎓', category: '学生', sort: 10 },
  { id: 2, name: '研究生', icon: '📚', category: '学生', sort: 20 },
  { id: 3, name: '前端开发', icon: '💻', category: '互联网', sort: 100 },
  { id: 4, name: '后端开发', icon: '🖥️', category: '互联网', sort: 110 },
  { id: 5, name: '全栈开发', icon: '⚙️', category: '互联网', sort: 120 },
  { id: 6, name: '产品经理', icon: '📱', category: '互联网', sort: 160 },
  { id: 7, name: 'UI设计师', icon: '🎨', category: '互联网', sort: 170 },
  { id: 8, name: '设计师', icon: '🎨', category: '文化', sort: 190 },
  { id: 9, name: '运营', icon: '📊', category: '互联网', sort: 240 },
  { id: 10, name: '新媒体运营', icon: '📣', category: '互联网', sort: 250 },
  { id: 11, name: '市场', icon: '📢', category: '市场', sort: 300 },
  { id: 12, name: '销售', icon: '💼', category: '市场', sort: 340 },
  { id: 13, name: '银行职员', icon: '🏦', category: '金融', sort: 410 },
  { id: 14, name: '会计师', icon: '🧾', category: '金融', sort: 460 },
  { id: 15, name: '律师', icon: '⚖️', category: '法律', sort: 510 },
  { id: 16, name: '公务员', icon: '🏛️', category: '政府', sort: 530 },
  { id: 17, name: '事业编', icon: '🏛️', category: '政府', sort: 540 },
  { id: 18, name: '医生', icon: '👨‍⚕️', category: '医疗', sort: 610 },
  { id: 19, name: '护士', icon: '👩‍⚕️', category: '医疗', sort: 620 },
  { id: 20, name: '老师', icon: '👩‍🏫', category: '教育', sort: 710 },
  { id: 21, name: '教授', icon: '👨‍🏫', category: '教育', sort: 720 },
  { id: 22, name: '幼师', icon: '🧒', category: '教育', sort: 730 },
  { id: 23, name: '健身教练', icon: '💪', category: '运动', sort: 770 },
  { id: 24, name: '演员', icon: '🎭', category: '文化', sort: 910 },
  { id: 25, name: '主播', icon: '🎥', category: '文化', sort: 930 },
  { id: 26, name: '博主', icon: '📸', category: '文化', sort: 940 },
  { id: 27, name: 'UP主', icon: '📹', category: '文化', sort: 950 },
  { id: 28, name: '自媒体', icon: '📱', category: '文化', sort: 960 },
  { id: 29, name: '摄影师', icon: '📷', category: '文化', sort: 990 },
  { id: 30, name: '作家', icon: '📖', category: '文化', sort: 1050 },
  { id: 31, name: '编辑', icon: '📰', category: '文化', sort: 1070 },
  { id: 32, name: '记者', icon: '🎤', category: '媒体', sort: 1080 },
  { id: 33, name: '翻译', icon: '🌐', category: '语言', sort: 1090 },
  { id: 34, name: '建筑师', icon: '🏗️', category: '建筑', sort: 1110 },
  { id: 35, name: '室内设计师', icon: '🏠', category: '建筑', sort: 1120 },
  { id: 36, name: '机械工程师', icon: '⚙️', category: '制造', sort: 1210 },
  { id: 37, name: '厨师', icon: '👨‍🍳', category: '服务', sort: 1310 },
  { id: 38, name: '咖啡师', icon: '☕', category: '服务', sort: 1330 },
  { id: 39, name: '服装设计师', icon: '👗', category: '时尚', sort: 1390 },
  { id: 40, name: '自由职业', icon: '🎨', category: '自由职业', sort: 1510 },
  { id: 41, name: '自由插画师', icon: '🖼️', category: '自由职业', sort: 1520 },
  { id: 42, name: '自由设计师', icon: '🎨', category: '自由职业', sort: 1530 },
  { id: 43, name: '自由开发者', icon: '💻', category: '自由职业', sort: 1540 },
  { id: 44, name: '创业者', icon: '🚀', category: '创业者', sort: 1610 },
  { id: 45, name: '个体户', icon: '🏪', category: '创业者', sort: 1620 },
  { id: 46, name: '退休', icon: '🌴', category: '其他', sort: 1710 },
  { id: 47, name: '全职妈妈', icon: '👶', category: '家庭', sort: 1730 },
  { id: 48, name: '全职爸爸', icon: '👶', category: '家庭', sort: 1740 },
  { id: 49, name: '军人', icon: '🎖️', category: '政府', sort: 1770 },
  { id: 50, name: '警察', icon: '👮', category: '政府', sort: 1780 },
  { id: 51, name: '程序员', icon: '👨‍💻', category: '互联网', sort: 1870 },
  { id: 52, name: '数据分析师', icon: '📊', category: '互联网', sort: 1880 },
  { id: 53, name: 'AI工程师', icon: '🤖', category: '互联网', sort: 1900 }
]

onMounted(async () => {
  if (!userStore.userInfo) {
    await userStore.fetchUserInfo()
  }
  fillForm()
  loadDicts()
})
</script>

<style lang="scss" scoped>
.edit-profile-view {
  padding: 30px 0 40px;
}

.edit-card {
  max-width: 760px;
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

.avatar-upload {
  display: flex;
  align-items: center;
  gap: 18px;
  .avatar-preview {
    position: relative;
    width: 100px;
    height: 100px;
    border-radius: 50%;
    overflow: hidden;
    cursor: pointer;
    .avatar-mask {
      position: absolute;
      inset: 0;
      background: rgba(0, 0, 0, 0.45);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 2px;
      color: #fff;
      font-size: 12px;
      opacity: 0;
      transition: opacity 0.25s;
      .el-icon { font-size: 20px; }
    }
    &:hover .avatar-mask { opacity: 1; }
  }
  .avatar-tip { font-size: 12px; color: #aaa; }
}

/* ============== 紧凑选择区（主表单） ============== */
.picker-field {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  width: 100%;
}

.picker-selected {
  flex: 1;
  min-height: 40px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: linear-gradient(135deg, #faf5ff 0%, #fff0f5 100%);
  border: 1px dashed #d8b4fe;
  border-radius: 10px;
}

.hobby-selected { border-color: #f9a8d4; background: linear-gradient(135deg, #fdf2f8 0%, #faf5ff 100%); }

.selected-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 999px;
  background: #fff;
  color: #7e22ce;
  font-size: 13px;
  font-weight: 500;
  box-shadow: 0 1px 3px rgba(168, 85, 247, 0.12);
  .remove {
    cursor: pointer; font-size: 12px;
    &:hover { color: #f43f5e; }
  }
}

.hobby-tag-pretty {
  background: linear-gradient(135deg, #fff0f5 0%, #f3e8ff 100%);
  color: #a855f7;
}

.picker-count {
  margin-left: auto;
  font-size: 12px;
  color: #8b8b9c;
  &.full { color: #f43f5e; font-weight: 600; }
}

.picker-empty {
  flex: 1;
  padding: 8px 12px;
  background: #fafafa;
  border-radius: 10px;
  font-size: 13px;
  color: #9a9aaa;
  border: 1px dashed #e5e7eb;
}

.picker-btn {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 5px;
  border-radius: 10px !important;
  font-weight: 600;
  border-color: #c4b5fd !important;
  color: #7e22ce !important;
  background: linear-gradient(135deg, #fff 0%, #faf5ff 100%) !important;
  &:hover {
    background: linear-gradient(135deg, #faf5ff 0%, #f3e8ff 100%) !important;
    color: #6d28d9 !important;
  }
  .el-icon { font-size: 15px; }
}

.hobby-btn {
  border-color: #f9a8d4 !important;
  color: #be185d !important;
  background: linear-gradient(135deg, #fff 0%, #fdf2f8 100%) !important;
  &:hover {
    background: linear-gradient(135deg, #fdf2f8 0%, #fce7f3 100%) !important;
    color: #9d174d !important;
  }
}

.loader-inline {
  width: 14px; height: 14px;
  border: 2px solid rgba(168, 85, 247, 0.2);
  border-top-color: #a855f7;
  border-radius: 50%;
  animation: spinSlow 0.8s linear infinite;
  display: inline-block;
  vertical-align: -2px;
  margin-right: 6px;
}

/* ============== 弹窗样式 ============== */
.dict-dialog {
  :deep(.el-dialog) {
    border-radius: 16px;
    overflow: hidden;
  }
  :deep(.el-dialog__header) {
    padding: 18px 24px 10px;
    border-bottom: 1px solid #f3f4f6;
  }
  :deep(.el-dialog__title) {
    font-size: 17px;
    font-weight: 700;
    color: #2d2d3a;
  }
  :deep(.el-dialog__body) {
    padding: 16px 24px 10px;
  }
  :deep(.el-dialog__footer) {
    padding: 10px 24px 16px;
  }
}

.dialog-search {
  margin-bottom: 12px;
  :deep(.el-input__wrapper) {
    border-radius: 10px;
    box-shadow: 0 0 0 1px #e5e7eb;
    &:hover { box-shadow: 0 0 0 1px #c4b5fd; }
    &.is-focus { box-shadow: 0 0 0 2px #a855f7; }
  }
}

.dialog-selected-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: linear-gradient(135deg, #faf5ff 0%, #fff0f5 100%);
  border-radius: 10px;
  margin-bottom: 12px;
  min-height: 44px;

  .bar-label {
    font-size: 13px;
    color: #6b21a8;
    font-weight: 600;
    b { color: #7e22ce; font-size: 15px; &.full { color: #f43f5e; } }
  }
  .bar-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    flex: 1;
  }
  .bar-hint {
    font-size: 12px;
    color: #9a9aaa;
  }
}

.hobby-dialog .dialog-selected-bar {
  background: linear-gradient(135deg, #fdf2f8 0%, #faf5ff 100%);
  .bar-label { color: #9d174d; b { color: #be185d; &.full { color: #f43f5e; } } }
}

.dialog-scroll {
  max-height: 400px;
  overflow-y: auto;
  padding: 4px 4px 4px 0;
  scrollbar-width: thin;
  scrollbar-color: #e5e7eb transparent;
  &::-webkit-scrollbar { width: 6px; }
  &::-webkit-scrollbar-thumb { background: #e5e7eb; border-radius: 3px; }
  &::-webkit-scrollbar-thumb:hover { background: #c4b5fd; }
}

.dict-grid {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.dict-cat {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px 12px;
  background: #fafbff;
  border-radius: 10px;
  border: 1px solid #eef0ff;
}

.dict-cat-title {
  font-size: 12px;
  font-weight: 700;
  color: #6b51b8;
  padding-left: 8px;
  border-left: 3px solid #a855f7;
  line-height: 1;
}

.dict-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.dict-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 11px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  font-size: 12.5px;
  color: #4a4a5a;
  cursor: pointer;
  transition: all 0.18s ease;
  user-select: none;
  position: relative;

  .chip-icon { font-size: 13px; line-height: 1; }
  .chip-name { line-height: 1; }
  .chip-cat-tag {
    margin-left: 4px;
    padding: 1px 6px;
    font-size: 10px;
    color: #8b8b9c;
    background: #f3f4f6;
    border-radius: 6px;
  }

  &:hover:not(.disabled) {
    border-color: #c4b5fd;
    color: #6d28d9;
    transform: translateY(-1px);
    box-shadow: 0 2px 6px rgba(168, 85, 247, 0.12);
  }

  &.active {
    background: linear-gradient(135deg, #a855f7 0%, #ec4899 100%);
    color: #fff;
    border-color: transparent;
    box-shadow: 0 3px 10px rgba(168, 85, 247, 0.28);
    .chip-cat-tag {
      color: rgba(255, 255, 255, 0.8);
      background: rgba(255, 255, 255, 0.2);
    }
    &:hover { color: #fff; }
  }

  &.disabled {
    opacity: 0.35;
    cursor: not-allowed;
    background: #f3f4f6;
  }
}

.hobby-chip {
  padding: 6px 12px;
  background: linear-gradient(135deg, #ffffff 0%, #fff5fb 100%);
  border-color: #fce7f3;
  color: #831843;
  &:hover:not(.disabled) {
    border-color: #f9a8d4;
    color: #be185d;
  }
  &.active {
    background: linear-gradient(135deg, #ec4899 0%, #a855f7 100%);
    color: #fff;
    border-color: transparent;
    box-shadow: 0 3px 10px rgba(236, 72, 153, 0.28);
  }
}

.search-results {
  padding: 4px 2px;
}

.no-result {
  text-align: center;
  padding: 30px;
  color: #9a9aaa;
  font-size: 13px;
}

.dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  .footer-right {
    display: flex;
    gap: 10px;
  }
}

.form-actions {
  display: flex;
  gap: 14px;
  width: 100%;
  justify-content: flex-end;
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

@keyframes spinSlow {
  to { transform: rotate(360deg); }
}

:deep(.el-form-item__label) {
  font-weight: 600;
  color: #4a4a5a;
}

/* ================== 响应式 ================== */
@media (max-width: 1023px) {
  .edit-profile-view { padding: 20px 0 30px; }
  .page-title { font-size: 22px; }
}
@media (max-width: 767px) {
  .edit-profile-view { padding: 14px 0 24px; }
  .page-title { font-size: 19px; gap: 8px; .bar { height: 18px; width: 4px; } }
  .edit-card { padding: 16px 14px !important; border-radius: 12px; max-width: 100%; }
  .form-actions { flex-direction: column-reverse; justify-content: center; gap: 10px; }
  .form-actions .mf-btn { width: 100%; justify-content: center; }
  .picker-field { flex-wrap: wrap; }
  .picker-btn { width: 100%; justify-content: center; }
  .dict-dialog :deep(.el-dialog) { width: 94% !important; margin: 10px auto !important; }
  .dialog-scroll { max-height: 320px; }
}
@media (max-width: 479px) {
  .page-title { font-size: 17px; }
  .edit-card { padding: 14px 10px !important; }
  .dict-cat-title { font-size: 11.5px; }
  .dict-chip { font-size: 12px; padding: 5px 9px; }
  .dialog-selected-bar { flex-wrap: wrap; }
}
</style>
