<template>
  <div class="chat-room">
    <!-- ============ 左侧装饰栏 ============ -->
    <aside class="side-pane side-pane--left glass-card" aria-hidden="false">
      <!-- 星点装饰（留在外层，不被滚动容器裁剪） -->
      <span class="sparkle sparkle--1"></span>
      <span class="sparkle sparkle--2"></span>
      <span class="sparkle sparkle--3"></span>
      <!-- 浮动装饰气泡（留外层） -->
      <div class="pane-decor pane-decor--bubble"></div>
      <div class="pane-decor pane-decor--bubble2"></div>
      <div class="pane-decor pane-decor--bubble3"></div>

      <div class="side-pane__scroll">
      <!-- 重叠头像 + 连接光晕 -->
      <div class="pane-avatar-stack">
        <div class="avatar-glow"></div>
        <el-avatar :size="54" :src="resolveAvatar(peer.avatar, peer.nickname)" class="pane-peer" />
        <el-avatar :size="40" :src="resolveAvatar(userStore.userInfo && userStore.userInfo.avatar, userStore.nickname)" class="pane-me" />
        <div class="avatar-link-dot">
          <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="#fff" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
          </svg>
        </div>
      </div>

      <div class="pane-divider"></div>

      <!-- 心情卡 -->
      <div class="pane-mood-card">
        <div class="pane-mood-emoji">{{ peerMoodEmoji }}</div>
        <div class="pane-mood-title">{{ peer.online ? '对方在线中' : '对方暂时离线' }}</div>
        <div class="pane-mood-desc">
          {{ peer.online ? '消息会被立即送达，尽情聊吧 ✨' : '留言后 TA 登录即可看到～' }}
        </div>
      </div>

      <!-- 聊天热度条 -->
      <div class="heat-card">
        <div class="heat-head">
          <span class="heat-title">🔥 聊天热度</span>
          <span class="heat-val">{{ heatPercent }}%</span>
        </div>
        <div class="heat-bar"><div class="heat-bar__fill" :style="{ width: heatPercent + '%' }"></div></div>
        <div class="heat-sub">今日互动 {{ messages.length }} 条消息，继续保持～</div>
      </div>

      <div class="pane-divider"></div>

      <!-- 共同兴趣标签 -->
      <div class="tags-card">
        <div class="tags-title">💕 你们的共同点</div>
        <div class="tags-wrap" v-if="commonTags.length">
          <span
            v-for="(t, i) in commonTags"
            :key="t.name"
            class="tag-chip"
            :class="'tag-chip--' + (i % 4)"
          >{{ t.emoji }} {{ t.name }}</span>
        </div>
        <div class="tags-empty" v-else>暂无共同爱好，快去发现彼此吧～</div>
      </div>

      <!-- 聊天小贴士 -->
      <div class="pane-tips">
        <div class="tips-title">💡 聊天小贴士</div>
        <div class="pane-tip" v-for="t in chatTips" :key="t">
          <span class="pane-tip-dot"></span>{{ t }}
        </div>
      </div>

      <div class="pane-divider"></div>

      <!-- 浪漫语录轮播 -->
      <div class="quote-card">
        <div class="quote-mark">“</div>
        <div class="quote-text" :key="quoteIdx">{{ currentQuote }}</div>
        <div class="quote-author">— 每日一句 —</div>
      </div>
      </div><!-- /side-pane__scroll -->
    </aside>

    <!-- ============ 中间主聊天区 ============ -->
    <main class="chat-main glass-card">
      <!-- 顶部对方信息栏 -->
      <header class="room-header">
        <div class="header-inner">
          <span class="back-btn" @click="$router.push('/chat')">
            <el-icon><ArrowLeft /></el-icon>
          </span>
          <el-avatar :size="40" :src="resolveAvatar(peer.avatar, peer.nickname)" />
          <div class="peer-info">
            <span class="peer-name">{{ peer.nickname || '聊天对象' }}</span>
            <span class="peer-status" :class="{ online: peer.online || peerTyping }">
              <span class="dot"></span>
              <template v-if="peerTyping">
                <span class="typing-text">
                  正在输入<span class="dots">
                    <span>.</span><span>.</span><span>.</span>
                  </span>
                </span>
              </template>
              <template v-else>{{ peer.online ? '在线' : '离线' }}</template>
            </span>
          </div>
          <span class="header-actions">
            <span class="more-btn" @click.stop="toggleThemePanel($event)" title="聊天主题"><el-icon><Brush /></el-icon></span>
            <span class="more-btn"><el-icon><More /></el-icon></span>
          </span>
        </div>
      </header>

      <!-- 消息区域 -->
      <div class="msg-area" ref="msgArea" @scroll="onScroll" @click="closeMsgMenu">
        <div class="msg-list" ref="msgList">
          <!-- 顶部"上滑加载更多"提示（加载更早历史） -->
          <div class="load-tip" v-if="pageNow > 1 || hasMore">
            <template v-if="loadingMore">⏳ 加载更早消息中...</template>
            <template v-else-if="hasMore">⬆️ 上滑查看更早的消息</template>
            <template v-else>— 以上就是全部历史消息了 —</template>
          </div>
          <div
            v-for="m in messages"
            :key="m.id"
            class="msg-row"
            :class="{ mine: isMine(m), recall: m.recalled }"
            @contextmenu.prevent="openMsgMenu($event, m)"
          >
            <el-avatar :size="38" :src="msgAvatar(m)" class="msg-avatar" />
            <div class="msg-content">
              <div class="bubble" :class="{ mine: isMine(m), long: isLongMsg(m), collapsed: isLongMsg(m) && m._collapsed }">
                <template v-if="m.recalled">
                  <span class="recall-text">你撤回了一条消息</span>
                </template>
                <template v-else>
                  <img v-if="Number(m.msgType) === 2" :src="resolveImage(m.content)" class="msg-image" />
                  <span v-else class="msg-text">{{ m.content }}</span>
                </template>
                <div
                  v-if="isLongMsg(m) && !m.recalled"
                  class="expand-toggle"
                  @click.stop="toggleMsgCollapse(m)"
                >
                  {{ m._collapsed ? '展开全部 ▾' : '收起 ▴' }}
                </div>
              </div>
              <div class="msg-foot">
                <span class="msg-time">{{ formatRelativeTime(m.createdAt) }}</span>
                <template v-if="isMine(m) && !m.recalled">
                  <span v-if="allMyMessagesRead" class="msg-state read" title="对方已读">
                    <el-icon><Check /></el-icon><el-icon><Check /></el-icon>
                  </span>
                  <span v-else class="msg-state sent" title="已发送">
                    <el-icon><Check /></el-icon>
                  </span>
                </template>
              </div>
            </div>
          </div>
          <div class="msg-empty" v-if="!loading && messages.length === 0">
            <div class="empty-emoji">💬</div>
            <div>还没有消息，打个招呼开启你们的故事吧～</div>
          </div>
        </div>
      </div>

      <!-- 快捷回复气泡推荐 -->
      <div class="quick-reply-bar" v-if="quickReplies.length && !hasUnsentText">
        <div class="qr-scroll">
          <button
            v-for="(q, idx) in quickReplies"
            :key="idx"
            class="qr-chip"
            @click="useQuickReply(q)"
          >
            {{ q }}
          </button>
        </div>
      </div>

      <!-- 底部输入区 -->
      <footer class="input-area">
        <span class="icon-btn" @click.stop="toggleEmojiPanel($event)" title="表情包"><el-icon><Operation /></el-icon></span>
        <span class="icon-btn" @click="onImageClick" title="图片"><el-icon><Picture /></el-icon></span>
        <input ref="fileInput" type="file" accept="image/*" hidden @change="onImageChange" />
        <el-input
          ref="msgInputRef"
          v-model="inputText"
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 5 }"
          resize="none"
          placeholder="输入消息…(Enter发送，Ctrl+Enter换行)"
          @keydown="onInputKeydown"
          @input="onMyTyping"
          class="msg-input"
        />
        <button class="send-btn" :disabled="!inputText.trim()" @click="onSend">
          <el-icon><Promotion /></el-icon>
        </button>
      </footer>
    </main>

    <!-- ============ 右侧装饰栏 ============ -->
    <aside class="side-pane side-pane--right glass-card">
      <!-- 星点装饰（留外层） -->
      <span class="sparkle sparkle--r1"></span>
      <span class="sparkle sparkle--r2"></span>

      <div class="side-pane__scroll">
      <!-- 对方资料卡 -->
      <div class="peer-card">
        <div class="peer-avatar-wrap">
          <div class="peer-ring peer-ring--outer"></div>
          <div class="peer-ring peer-ring--inner"></div>
          <el-avatar :size="76" :src="resolveAvatar(peer.avatar, peer.nickname)" class="peer-card__avatar" />
          <span class="peer-online-badge" :class="{ on: peer.online }">
            <span class="pob-dot"></span>
          </span>
        </div>
        <div class="peer-card__name">{{ peer.nickname || '聊天对象' }}</div>
        <div class="peer-card__status">
          <span class="dot" :class="{ online: peer.online }"></span>
          {{ peer.online ? '现在聊正合适' : '稍后再聊' }}
        </div>
        <div class="peer-card__meta">
          <div class="meta-item">
            <el-icon><ChatDotRound /></el-icon>
            <div class="meta-text">
              <div class="meta-label">消息往来</div>
              <div class="meta-val">{{ messages.length }} 条</div>
            </div>
          </div>
          <div class="meta-item">
            <el-icon><Connection /></el-icon>
            <div class="meta-text">
              <div class="meta-label">匹配时间</div>
              <div class="meta-val">{{ matchSince }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 匹配度环形 -->
      <div class="match-card">
        <div class="match-head">
          <span class="match-title">💖 默契匹配度</span>
          <span class="match-score">{{ matchScore }}%</span>
        </div>
        <div class="match-ring-wrap">
          <svg class="match-ring" viewBox="0 0 120 120">
            <defs>
              <linearGradient id="matchGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#ff6b9d"/>
                <stop offset="50%" stop-color="#c058f2"/>
                <stop offset="100%" stop-color="#6ea8ff"/>
              </linearGradient>
            </defs>
            <circle cx="60" cy="60" r="50" stroke="rgba(168,85,247,0.1)" stroke-width="10" fill="none"/>
            <circle
              cx="60" cy="60" r="50"
              stroke="url(#matchGrad)" stroke-width="10" fill="none"
              stroke-linecap="round"
              :stroke-dasharray="(matchScore / 100) * 314 + ' 314'"
              transform="rotate(-90 60 60)"
              style="transition: stroke-dasharray 1.2s ease;"
            />
          </svg>
          <div class="match-ring-center">
            <div class="mrc-emoji">{{ matchEmoji }}</div>
            <div class="mrc-text">{{ matchLevelText }}</div>
          </div>
        </div>
        <div class="match-bars">
          <div class="mb-row">
            <span class="mb-label">性格契合</span>
            <div class="mb-bar"><div class="mb-fill mb-fill--pink" :style="{ width: personalityMatch + '%' }"></div></div>
            <span class="mb-num">{{ personalityMatch }}</span>
          </div>
          <div class="mb-row">
            <span class="mb-label">兴趣相似</span>
            <div class="mb-bar"><div class="mb-fill mb-fill--purple" :style="{ width: interestSimilarity + '%' }"></div></div>
            <span class="mb-num">{{ interestSimilarity }}</span>
          </div>
          <div class="mb-row">
            <span class="mb-label">聊天频率</span>
            <div class="mb-bar"><div class="mb-fill mb-fill--blue" :style="{ width: chatFreq + '%' }"></div></div>
            <span class="mb-num">{{ chatFreq }}</span>
          </div>
        </div>
      </div>

      <div class="pane-divider"></div>

      <!-- TA 的兴趣标签云 -->
      <div class="tags-cloud-card">
        <div class="tags-title">🌈 TA 的兴趣</div>
        <div class="cloud-wrap" v-if="peerInterests.length">
          <span
            v-for="(t, i) in peerInterests"
            :key="t.name"
            class="cloud-tag"
            :class="'cloud-tag--' + (i % 5)"
          >{{ t.emoji }} {{ t.name }}</span>
        </div>
        <div class="tags-empty" v-else>TA 还没有设置兴趣爱好</div>
      </div>

      <div class="pane-divider"></div>

      <!-- 你们的故事卡 -->
      <div class="story-card">
        <div class="story-title">📖 你们的故事</div>
        <div class="story-grid">
          <div class="story-cell">
            <div class="sc-num">{{ daysSinceMatch }}</div>
            <div class="sc-unit">天</div>
            <div class="sc-label">已认识</div>
          </div>
          <div class="story-cell sc-highlight">
            <div class="sc-num">{{ messages.length }}</div>
            <div class="sc-unit">条</div>
            <div class="sc-label">消息往来</div>
          </div>
          <div class="story-cell">
            <div class="sc-num">{{ wordCountEstimate }}</div>
            <div class="sc-unit">字</div>
            <div class="sc-label">字数估计</div>
          </div>
        </div>
      </div>

      <!-- 图片墙装饰 -->
      <div class="photo-wall">
        <div class="pw-item pw-item--1">
          <span class="pw-emoji">🌅</span>
        </div>
        <div class="pw-item pw-item--2">
          <span class="pw-emoji">☕</span>
        </div>
        <div class="pw-item pw-item--3">
          <span class="pw-emoji">🌸</span>
        </div>
        <div class="pw-item pw-item--4">
          <span class="pw-emoji">🎵</span>
        </div>
      </div>

      <div class="pane-divider"></div>

      <!-- 快捷动作 -->
      <div class="quick-actions">
        <div class="quick-title">⚡ 快捷动作</div>
        <div class="quick-grid">
          <button class="quick-btn quick-btn--primary" @click="insertQuickText('你好呀～')">
            <span class="qb-ico">👋</span><span class="qb-txt">打招呼</span>
          </button>
          <button class="quick-btn" @click="insertQuickText('哈哈真的吗🤣')">
            <span class="qb-ico">😂</span><span class="qb-txt">开心</span>
          </button>
          <button class="quick-btn" @click="insertQuickText('有空一起吃饭吗？')">
            <span class="qb-ico">🍜</span><span class="qb-txt">约饭</span>
          </button>
          <button class="quick-btn" @click="goUserProfile">
            <span class="qb-ico">🧭</span><span class="qb-txt">主页</span>
          </button>
          <button class="quick-btn" @click="insertQuickText('今天天气真好呀🌤️')">
            <span class="qb-ico">🌤️</span><span class="qb-txt">聊天气</span>
          </button>
          <button class="quick-btn" @click="insertQuickText('周末有什么计划吗？')">
            <span class="qb-ico">📅</span><span class="qb-txt">问周末</span>
          </button>
        </div>
      </div>
      </div><!-- /side-pane__scroll -->
    </aside>

    <!-- 右键菜单：Teleport 到 body，避免父级 backdrop-filter/overflow 干扰 fixed 定位 -->
    <teleport to="body">
      <div
        v-if="activeMenuMsg"
        class="msg-menu"
        :style="menuStyle"
        @click.stop
        @contextmenu.prevent.stop
      >
        <button
          v-if="canRecall(activeMenuMsg)"
          class="mm-btn mm-btn--danger"
          @click.stop="onRecall(activeMenuMsg)"
        >
          <el-icon><RefreshLeft /></el-icon>撤回 ({{ recallSecondsLeft(activeMenuMsg) }}s)
        </button>
        <button
          v-else-if="isMine(activeMenuMsg) && !activeMenuMsg.recalled && Number(activeMenuMsg.msgType) !== 2"
          class="mm-btn mm-btn--disabled"
          disabled
          title="仅 2 分钟内的文字消息可撤回"
        >
          <el-icon><RefreshLeft /></el-icon>已超过撤回时间
        </button>
        <button
          v-if="!activeMenuMsg.recalled"
          class="mm-btn"
          @click.stop="copyMsg(activeMenuMsg)"
        >
          <el-icon><DocumentCopy /></el-icon>复制{{ Number(activeMenuMsg.msgType) === 2 ? '链接' : '' }}
        </button>
        <div v-if="activeMenuMsg.recalled" class="mm-empty">已撤回消息无可用操作</div>
      </div>
    </teleport>

    <!-- ============ 聊天主题皮肤选择面板（Teleport） ============ -->
    <teleport to="body">
      <transition name="mf-panel-fade">
        <div
          v-if="showThemePanel"
          class="mf-theme-panel"
          :style="themePanelStyle"
          @click.stop
        >
          <div class="mf-panel-head">
            <span class="mf-panel-title">🎨 聊天主题</span>
            <span class="mf-panel-sub">选择喜欢的风格</span>
          </div>
          <div class="mf-theme-grid">
            <button
              v-for="t in THEMES"
              :key="t.id"
              class="mf-theme-card"
              :class="{ active: currentThemeId === t.id }"
              @click="pickTheme(t.id)"
            >
              <div
                class="mf-theme-preview"
                :style="{ background: t.vars['--theme-bg-grad'] }"
              >
                <div class="mf-preview-bubble mf-preview-left" :style="{ background: t.vars['--theme-bubble-peer'], color: t.vars['--theme-bubble-peer-text'] }">hi</div>
                <div class="mf-preview-bubble mf-preview-right" :style="{ background: t.vars['--theme-bubble-mine'], color: t.vars['--theme-bubble-mine-text'] }">😊</div>
              </div>
              <div class="mf-theme-info">
                <span class="mf-theme-name">{{ t.emoji }} {{ t.name }}</span>
                <span class="mf-theme-desc">{{ t.desc }}</span>
              </div>
              <span v-if="currentThemeId === t.id" class="mf-theme-check">✓</span>
            </button>
          </div>
        </div>
      </transition>
    </teleport>

    <!-- ============ 表情表面板（Teleport） ============ -->
    <teleport to="body">
      <transition name="mf-panel-fade">
        <div
          v-if="showEmojiPanel"
          class="mf-emoji-panel"
          :style="emojiPanelStyle"
          @click.stop
        >
          <div class="mf-emoji-tabs">
            <button
              v-for="c in EMOJI_CATEGORIES"
              :key="c.key"
              class="mf-emoji-tab"
              :class="{ active: emojiActiveCat === c.key }"
              @click="emojiActiveCat = c.key"
              :title="c.name"
            >
              <span>{{ c.icon }}</span>
            </button>
          </div>
          <div class="mf-emoji-grid">
            <button
              v-for="(emoji, i) in getEmojisOfCat(emojiActiveCat)"
              :key="emoji + '-' + i"
              class="mf-emoji-cell"
              @click="insertEmojiAtCursor(emoji)"
            >{{ emoji }}</button>
            <div v-if="!getEmojisOfCat(emojiActiveCat).length" class="mf-emoji-empty">
              暂无最近使用的表情哦～
            </div>
          </div>
          <div class="mf-emoji-footer">
            <span>点击表情即可插入输入框，会自动记录最近使用</span>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, More, Picture, Promotion, ChatDotRound, Connection, Check, RefreshLeft, DocumentCopy, Brush, Operation } from '@element-plus/icons-vue'
import { getMessages, sendMessage, markRead, getChatList, getUnreadCount } from '@/api/chat'
import { getUserById } from '@/api/user'
import { uploadImage } from '@/api/dynamic'
import { resolveAvatar, resolveImage, formatRelativeTime, parseHobbies } from '@/utils/format'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const messages = ref([])
const inputText = ref('')
const msgInputRef = ref(null)
const peer = reactive({ id: null, nickname: '', avatar: '', online: false, hobbies: '' })

// 打字指示器 + 撤回/右键菜单状态
const peerTyping = ref(false)
let peerTypingTimer = null
const myTypingSentAt = ref(0)

const activeMenuMsg = ref(null)
const menuStyle = reactive({ left: '0px', top: '0px' })

// ============================================================
// 功能一：聊天主题皮肤（7 个主题 + CSS 变量切换 + localStorage 持久化）
// ============================================================
const THEME_STORAGE_KEY = 'mf_chat_theme'
const THEMES = [
  {
    id: 'pinkdream', name: '梦幻粉紫', emoji: '🌸',
    desc: '默认温柔少女感',
    vars: {
      '--theme-bg-grad': 'radial-gradient(1200px 600px at 0% 0%, #F4ECFF 0%, transparent 55%), radial-gradient(1000px 700px at 100% 100%, #FFE3EC 0%, transparent 60%), linear-gradient(180deg, #F7F9FC 0%, #F2EFFB 100%)',
      '--theme-card-bg': 'rgba(255,255,255,0.78)',
      '--theme-card-border': 'rgba(168, 85, 247, 0.18)',
      '--theme-primary': '#a855f7',
      '--theme-primary-soft': 'rgba(168,85,247,0.12)',
      '--theme-bubble-mine': 'linear-gradient(135deg, #c084fc 0%, #ec4899 100%)',
      '--theme-bubble-mine-text': '#ffffff',
      '--theme-bubble-peer': '#ffffff',
      '--theme-bubble-peer-text': '#3b2b4a',
      '--theme-header-bg': 'rgba(255,255,255,0.75)',
      '--theme-input-bg': '#ffffff',
      '--theme-accent': '#ec4899',
      '--theme-accent2': '#8b5cf6',
    },
  },
  {
    id: 'mint', name: '清新薄荷', emoji: '🌿',
    desc: '清凉夏日风',
    vars: {
      '--theme-bg-grad': 'radial-gradient(1100px 700px at 10% 20%, #DDF8F0 0%, transparent 55%), radial-gradient(900px 600px at 90% 90%, #FFF6DB 0%, transparent 60%), linear-gradient(180deg, #F3FBF8 0%, #ECFDF5 100%)',
      '--theme-card-bg': 'rgba(255,255,255,0.80)',
      '--theme-card-border': 'rgba(16, 185, 129, 0.20)',
      '--theme-primary': '#10b981',
      '--theme-primary-soft': 'rgba(16,185,129,0.12)',
      '--theme-bubble-mine': 'linear-gradient(135deg, #10b981 0%, #06b6d4 100%)',
      '--theme-bubble-mine-text': '#ffffff',
      '--theme-bubble-peer': '#F0FDF4',
      '--theme-bubble-peer-text': '#1f3a2f',
      '--theme-header-bg': 'rgba(240, 253, 244, 0.82)',
      '--theme-input-bg': '#ffffff',
      '--theme-accent': '#10b981',
      '--theme-accent2': '#06b6d4',
    },
  },
  {
    id: 'starry', name: '星空深蓝', emoji: '🌌',
    desc: '深邃浪漫夜',
    vars: {
      '--theme-bg-grad': 'radial-gradient(1100px 700px at 0% 0%, #1e3a8a40 0%, transparent 55%), radial-gradient(900px 600px at 100% 100%, #7c3aed40 0%, transparent 60%), linear-gradient(180deg, #0f172a 0%, #1e1b4b 100%)',
      '--theme-card-bg': 'rgba(30, 41, 59, 0.78)',
      '--theme-card-border': 'rgba(99, 102, 241, 0.25)',
      '--theme-primary': '#818cf8',
      '--theme-primary-soft': 'rgba(129,140,248,0.15)',
      '--theme-bubble-mine': 'linear-gradient(135deg, #6366f1 0%, #a855f7 100%)',
      '--theme-bubble-mine-text': '#ffffff',
      '--theme-bubble-peer': 'rgba(51, 65, 85, 0.92)',
      '--theme-bubble-peer-text': '#e2e8f0',
      '--theme-header-bg': 'rgba(15, 23, 42, 0.72)',
      '--theme-input-bg': 'rgba(30, 41, 59, 0.92)',
      '--theme-accent': '#a78bfa',
      '--theme-accent2': '#22d3ee',
    },
  },
  {
    id: 'autumn', name: '暖秋蜜橙', emoji: '🍂',
    desc: '温暖治愈系',
    vars: {
      '--theme-bg-grad': 'radial-gradient(1100px 700px at 20% 10%, #FFE1C4 0%, transparent 55%), radial-gradient(900px 600px at 100% 90%, #FCE7F3 0%, transparent 60%), linear-gradient(180deg, #FFF7ED 0%, #FEF3C7 100%)',
      '--theme-card-bg': 'rgba(255,255,255,0.80)',
      '--theme-card-border': 'rgba(249, 115, 22, 0.20)',
      '--theme-primary': '#f97316',
      '--theme-primary-soft': 'rgba(249,115,22,0.12)',
      '--theme-bubble-mine': 'linear-gradient(135deg, #f97316 0%, #eab308 100%)',
      '--theme-bubble-mine-text': '#ffffff',
      '--theme-bubble-peer': '#FFF7ED',
      '--theme-bubble-peer-text': '#4a2e12',
      '--theme-header-bg': 'rgba(255, 247, 237, 0.82)',
      '--theme-input-bg': '#ffffff',
      '--theme-accent': '#f97316',
      '--theme-accent2': '#eab308',
    },
  },
  {
    id: 'sakura', name: '樱花粉白', emoji: '🌸',
    desc: '超甜少女心',
    vars: {
      '--theme-bg-grad': 'radial-gradient(1100px 700px at 10% 0%, #FFE4EF 0%, transparent 55%), radial-gradient(900px 600px at 90% 100%, #FDF2F8 0%, transparent 60%), linear-gradient(180deg, #FFF5F8 0%, #FCE7F3 100%)',
      '--theme-card-bg': 'rgba(255,255,255,0.85)',
      '--theme-card-border': 'rgba(244, 114, 182, 0.20)',
      '--theme-primary': '#ec4899',
      '--theme-primary-soft': 'rgba(244,114,182,0.12)',
      '--theme-bubble-mine': 'linear-gradient(135deg, #f472b6 0%, #fb7185 100%)',
      '--theme-bubble-mine-text': '#ffffff',
      '--theme-bubble-peer': '#ffffff',
      '--theme-bubble-peer-text': '#5b2a3e',
      '--theme-header-bg': 'rgba(255, 240, 245, 0.82)',
      '--theme-input-bg': '#ffffff',
      '--theme-accent': '#fb7185',
      '--theme-accent2': '#fda4af',
    },
  },
  {
    id: 'minimal', name: '极简纯白', emoji: '🤍',
    desc: '商务清爽感',
    vars: {
      '--theme-bg-grad': 'linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%)',
      '--theme-card-bg': 'rgba(255,255,255,0.96)',
      '--theme-card-border': 'rgba(100, 116, 139, 0.12)',
      '--theme-primary': '#475569',
      '--theme-primary-soft': 'rgba(71,85,105,0.08)',
      '--theme-bubble-mine': '#0ea5e9',
      '--theme-bubble-mine-text': '#ffffff',
      '--theme-bubble-peer': '#f1f5f9',
      '--theme-bubble-peer-text': '#1e293b',
      '--theme-header-bg': 'rgba(255,255,255,0.92)',
      '--theme-input-bg': '#ffffff',
      '--theme-accent': '#0ea5e9',
      '--theme-accent2': '#64748b',
    },
  },
  {
    id: 'midnight', name: '暗夜墨黑', emoji: '🖤',
    desc: '极致省电模式',
    vars: {
      '--theme-bg-grad': 'linear-gradient(180deg, #0a0a0f 0%, #000000 100%)',
      '--theme-card-bg': 'rgba(23, 23, 23, 0.90)',
      '--theme-card-border': 'rgba(64, 64, 64, 0.35)',
      '--theme-primary': '#e5e7eb',
      '--theme-primary-soft': 'rgba(229,231,235,0.10)',
      '--theme-bubble-mine': 'linear-gradient(135deg, #374151 0%, #111827 100%)',
      '--theme-bubble-mine-text': '#f3f4f6',
      '--theme-bubble-peer': '#1f2937',
      '--theme-bubble-peer-text': '#d1d5db',
      '--theme-header-bg': 'rgba(10, 10, 15, 0.88)',
      '--theme-input-bg': '#0f0f13',
      '--theme-accent': '#3b82f6',
      '--theme-accent2': '#a855f7',
    },
  },
]
const THEME_BY_ID = Object.fromEntries(THEMES.map(t => [t.id, t]))

const currentThemeId = ref(localStorage.getItem(THEME_STORAGE_KEY) || THEMES[0].id)
const showThemePanel = ref(false)
const themePanelStyle = reactive({ left: '0px', top: '0px' })
const chatRootEl = ref(null) // wrapper ref

function applyThemeVars(id) {
  const t = THEME_BY_ID[id] || THEMES[0]
  const root = document.documentElement
  Object.entries(t.vars).forEach(([k, v]) => root.style.setProperty(k, v))
}
applyThemeVars(currentThemeId.value)  // 组件初始化立即应用

watch(currentThemeId, (id) => {
  applyThemeVars(id)
  localStorage.setItem(THEME_STORAGE_KEY, id)
})

function toggleThemePanel(e) {
  if (showThemePanel.value) {
    showThemePanel.value = false
    return
  }
  // 锚点定位：图标右上角对齐弹窗右上（下方弹）
  const rect = (e.currentTarget || e.target).getBoundingClientRect()
  const panelW = 380, panelH = 470
  let x = Math.max(10, Math.min(window.innerWidth - panelW - 10, rect.left))
  let y = rect.bottom + 8
  if (y + panelH > window.innerHeight - 10) {
    y = Math.max(10, rect.top - panelH - 8)
  }
  themePanelStyle.left = x + 'px'
  themePanelStyle.top = y + 'px'
  showThemePanel.value = true
}
function pickTheme(id) {
  currentThemeId.value = id
  showThemePanel.value = false
  ElMessage({ message: `已切换「${THEME_BY_ID[id].name}」主题 ${THEME_BY_ID[id].emoji}`, type: 'success', duration: 1400 })
}

// ============================================================
// 功能二：表情表面板（7 分类 + 最近使用 + 光标插入）
// ============================================================
const EMOJI_CATEGORIES = [
  {
    key: 'recent', name: '最近', icon: '🕒',
    emojis: [], // 动态从 localStorage 读取
  },
  {
    key: 'face', name: '表情', icon: '😀',
    emojis: ['😀','😃','😄','😁','😆','😅','🤣','😂','🙂','🙃','😉','😊','😇','🥰','😍','🤩','😘','😗','😚','😙','🥲','😋','😛','😜','🤪','😝','🤑','🤗','🤭','🤫','🤔','🤐','🤨','😐','😑','😶','😏','😒','🙄','😬','🤥','😌','😔','😪','🤤','😴','😷','🤒','🤕','🤢','🤮','🤧','🥵','🥶','🥴','😵','🤯','🤠','🥳','😎','🤓','🧐','😕','😟','🙁','☹️','😮','😯','😲','😳','🥺','😦','😧','😨','😰','😥','😢','😭','😱','😖','😣','😞','😓','😩','😫','🥱','😤','😡','😠','🤬','😈','👿','💀','☠️','💩','🤡','👹','👺','👻','👽','👾','🤖'],
  },
  {
    key: 'gesture', name: '手势', icon: '👍',
    emojis: ['👋','🤚','🖐️','✋','🖖','👌','🤌','🤏','✌️','🤞','🤟','🤘','🤙','👈','👉','👆','🖕','👇','☝️','👍','👎','✊','👊','🤛','🤜','👏','🙌','👐','🤲','🤝','🙏','✍️','💅','🤳','💪','🦾','🦵','🦶','👂','🦻','👃','🧠','🦷','🦴','👀','👁️','👅','👄','💋','🩸'],
  },
  {
    key: 'heart', name: '爱心', icon: '💕',
    emojis: ['❤️','🧡','💛','💚','💙','💜','🖤','🤍','🤎','💔','❣️','💕','💞','💓','💗','💖','💘','💝','💟','♥️','💌','💋','💯','💢','💥','💫','💦','💨','🕳️','💬','👁️‍🗨️','🗨️','🗯️','💭','💤','✨','⭐','🌟','💫','⭐','🌸','🍀','🌷','🌹','🌺','🌻','🌼','💐','🪄','🎀','🏩','💒'],
  },
  {
    key: 'animal', name: '动物', icon: '🐶',
    emojis: ['🐶','🐱','🐭','🐹','🐰','🦊','🐻','🐼','🐻‍❄️','🐨','🐯','🦁','🐮','🐷','🐽','🐸','🐵','🙈','🙉','🙊','🐒','🐔','🐧','🐦','🐤','🐣','🐥','🦆','🦅','🦉','🦇','🐺','🐗','🐴','🦄','🐝','🐛','🦋','🐌','🐞','🐜','🦟','🦗','🕷️','🦂','🐢','🐍','🦎','🦖','🦕','🐙','🦑','🦐','🦞','🦀','🐡','🐠','🐟','🐬','🐳','🐋','🦈','🐊','🐅','🐆','🦓','🦍','🦧','🐘','🦛','🦏','🐪','🐫','🦒','🦘','🐃','🐂','🐄','🐎','🐖','🐏','🐑','🐐','🦌','🐕','🐩','🦮','🐕‍🦺','🐈','🐈‍⬛','🐓','🦃','🦚','🦜','🦢','🦩','🐇','🦝','🦙','🦥','🦦','🦨','🦡','🐁','🐀','🐿️','🦔'],
  },
  {
    key: 'food', name: '吃喝', icon: '🍔',
    emojis: ['🍏','🍎','🍐','🍊','🍋','🍌','🍉','🍇','🍓','🫐','🍈','🍒','🍑','🥭','🍍','🥥','🥝','🍅','🍆','🥑','🥦','🥬','🥒','🌶️','🫑','🌽','🥕','🫒','🧄','🧅','🥔','🍠','🥐','🥯','🍞','🥖','🥨','🧀','🥚','🍳','🧈','🥞','🧇','🥓','🥩','🍗','🍖','🦴','🌭','🍔','🍟','🍕','🥪','🥙','🧆','🌮','🌯','🫔','🥗','🥘','🫕','🥫','🍝','🍜','🍲','🍛','🍣','🍱','🥟','🦪','🍤','🍙','🍚','🍘','🍥','🥠','🥮','🍢','🍡','🍧','🍨','🍦','🥧','🧁','🍰','🎂','🍮','🍭','🍬','🍫','🍿','🍩','🍪','🌰','🥜','🍯','🥛','🍼','☕','🫖','🍵','🍶','🍾','🍷','🍸','🍹','🍺','🍻','🥂','🥃','🫗','🥤','🧋','🧃','🧉','🧊','🥢','🍽️','🍴','🥄'],
  },
  {
    key: 'symbol', name: '符号', icon: '🎵',
    emojis: ['🚗','🚕','🚙','🚌','🚎','🏎️','🚓','🚑','🚒','🚐','🛻','🚚','🚛','🚜','🛵','🏍️','🛺','🚲','🛴','🛹','🛼','🚏','🛣️','🛤️','🛢️','⛽','🛞','🚨','🚓','🚔','🚍','🚘','🚖','🚡','🚠','🚟','🚃','🚋','🚞','🚝','🚄','🚅','🚈','🚂','🚆','🚇','🚊','🚉','✈️','🛫','🛬','🛩️','💺','🛰️','🚀','🛸','🚁','🛶','⛵','🛥️','🛳️','⛴️','🚢','⚓','🎵','🎶','🎼','🎹','🥁','🎷','🎺','🪗','🎸','🪕','🎻','🪈','🎺','🎬','🎤','🎧','🎥','🎞️','📽️','🎬','📺','📱','💻','⌨️','🖥️','🖨️','🖱️','🖲️','💽','💾','💿','📷','📸','📹','📼','🔋','🔌','💡','🔦','🕯️','🧯','🛢️','💸','💰','💵','💴','💶','💷','🪙','💳','🧾','💎','⚖️','🔪','🪓','🔧','🔨','⚒️','🛠️','⛏️','🪚','🔩','⚙️','🔫','💣','🪃','🏹','🛡️','🚪','🛗','🪑','🚽','🚪','🛋️','🛏️','🛌','🧸','🪆','🎁','🎈','🎉','🎊','🎀','🎊','🎏','🎐','🧧','🎮','🎰','🎲','🧩','🎯','🎳','🎱','🏓','🏸','🏒','🏑','🥍','🏏','🪃','🥅','⛳','🪁','🏹','🎣','🤿','🥊','🥋','🎽','🎿','🛷','🥌','⛸️'],
  },
]
const RECENT_EMOJI_KEY = 'mf_recent_emojis'
const MAX_RECENT = 30
const emojiActiveCat = ref('face')
const showEmojiPanel = ref(false)
const emojiPanelStyle = reactive({ left: '0px', top: '0px' })

function loadRecentEmojis() {
  try {
    const arr = JSON.parse(localStorage.getItem(RECENT_EMOJI_KEY) || '[]')
    return Array.isArray(arr) ? arr.slice(0, MAX_RECENT) : []
  } catch { return [] }
}
function saveRecentEmoji(emoji) {
  const list = loadRecentEmojis().filter(e => e !== emoji)
  list.unshift(emoji)
  localStorage.setItem(RECENT_EMOJI_KEY, JSON.stringify(list.slice(0, MAX_RECENT)))
}
const recentEmojis = computed(loadRecentEmojis)
function getEmojisOfCat(catKey) {
  if (catKey === 'recent') return recentEmojis.value.length ? recentEmojis.value : ['😀','😂','😍','🥰','😭','👍','💕','🎉','😊','🤔','😘','🙏','😜','🌸','💪']
  const c = EMOJI_CATEGORIES.find(c => c.key === catKey)
  return c ? c.emojis : []
}

function toggleEmojiPanel(e) {
  if (showEmojiPanel.value) {
    showEmojiPanel.value = false
    return
  }
  const rect = (e.currentTarget || e.target).getBoundingClientRect()
  const panelW = 420, panelH = 360
  let x = Math.max(10, Math.min(window.innerWidth - panelW - 10, rect.left))
  let y = rect.top - panelH - 8
  if (y < 10) {
    y = rect.bottom + 8
  }
  emojiPanelStyle.left = x + 'px'
  emojiPanelStyle.top = y + 'px'
  showEmojiPanel.value = true
  emojiActiveCat.value = recentEmojis.value.length ? 'recent' : 'face'
}

// 在光标位置插入 Emoji
function insertEmojiAtCursor(emoji) {
  saveRecentEmoji(emoji)
  const el = msgInputRef.value && msgInputRef.value.textarea
      ? msgInputRef.value.textarea
      : (msgInputRef.value && msgInputRef.value.$el ? msgInputRef.value.$el.querySelector('textarea') : null)
  if (!el) {
    inputText.value = (inputText.value || '') + emoji
    return
  }
  const start = el.selectionStart ?? (inputText.value || '').length
  const end = el.selectionEnd ?? (inputText.value || '').length
  const before = (inputText.value || '').slice(0, start)
  const after = (inputText.value || '').slice(end)
  inputText.value = before + emoji + after
  nextTick(() => {
    try {
      const pos = before.length + emoji.length
      el.focus()
      el.setSelectionRange(pos, pos)
    } catch {}
  })
}

// 2分钟撤回窗口
const RECALL_WINDOW_MS = 2 * 60 * 1000

// ================= 侧边栏装饰数据 =================
const peerMoodEmoji = computed(() => peer.online ? '🌸' : '🌙')

const chatTips = [
  '尝试发张有趣的图片更容易打破尴尬哦',
  '真诚的自我介绍能让 TA 留下好感～',
  '互相喜欢的缘分，值得认真对待 ❤️',
  '遇到不适的内容可以长按举报/拉黑',
]

const hobbyEmojiMap = {
  '音乐': '🎵', '旅行': '✈️', '美食': '🍰', '摄影': '📷', '电影': '🎬',
  '咖啡': '☕', '阅读': '📚', '运动': '⚽', '健身': '💪', '游泳': '🏊',
  '跑步': '🏃', '骑行': '🚴', '篮球': '🏀', '足球': '⚽', '羽毛球': '🏸',
  '唱歌': '🎤', '跳舞': '💃', '绘画': '🎨', '写作': '✍️', '编程': '💻',
  '游戏': '🎮', '手工': '🧶', '做饭': '🍳', '烘焙': '🧁', '宠物': '🐾',
  '猫咪': '🐱', '狗狗': '🐶', '猫咪': '🐱', '植物': '🌱', '花艺': '💐',
  '瑜伽': '🧘', '冥想': '🧠', '骑行': '🚴', '攀岩': '🧗', '潜水': '🤿',
  '滑雪': '⛷️', '冲浪': '🏄', '滑板': '🛹', '轮滑': '🛼', '保龄球': '🎳',
  '台球': '🎱', '乒乓球': '🏓', '网球': '🎾', '高尔夫': '⛳', '钓鱼': '🎣',
  '露营': '⛺', '徒步': '🥾', '登山': '🏔️', '自驾': '🚗', '骑行': '🚴',
  '逛街': '🛍️', '购物': '🛒', '时尚': '👗', '化妆': '💄', '美甲': '💅',
  '旅行': '✈️', '国内游': '🗺️', '国外游': '🌍', '日本': '🗾', '韩国': '🇰🇷',
  '美国': '🗽', '欧洲': '🏰', '海岛': '🏝️', '海边': '🌊', '温泉': '♨️',
  '滑雪': '⛷️', '樱花': '🌸', '红叶': '🍁', '星空': '🌌', '日出': '🌅',
  '美食': '🍰', '火锅': '🍲', '烧烤': '🍢', '日料': '🍣', '韩餐': '🍜',
  '西餐': '🥩', '中餐': '🥟', '甜点': '🧁', '水果': '🍓', '咖啡': '☕',
  '奶茶': '🧋', '红酒': '🍷', '啤酒': '🍺', '茶道': '🍵',
  '摇滚': '🎸', '古典': '🎻', '民谣': '🎶', '电子': '🎹', 'K-Pop': '💫',
  '爵士': '🎷', '嘻哈': '🎤', 'R&B': '🎙️', '独立音乐': '🎵',
  '手作': '🧶', '陶艺': '🏺', '木工': '🔨', '皮艺': '👜', '编织': '🧶',
  '刺绣': '🪡', '绘画': '🎨', '书法': '🖌️', '雕塑': '🗿', '剪纸': '✂️',
  '天文': '🔭', '地理': '🗺️', '历史': '📜', '哲学': '📖', '心理学': '🧠',
  '经济': '📈', '投资': '💰', '创业': '🚀', '商业': '💼', '科技': '🔬',
  'AI': '🤖', '机器人': '🦾', '区块链': '🔗', '元宇宙': '🌐',
  '电影': '🎬', '电视剧': '📺', '综艺': '🎭', '动漫': '📺', '小说': '📖',
  '漫画': '📚', '动画': '🎨', '短视频': '📱', '直播': '🎥',
  '摄影': '📷', '摄像': '🎥', '修图': '🖼️', '设计': '✏️', '剪辑': '🎞️',
  '穿搭': '👕', '时尚': '👗', '美妆': '💄', '护肤': '🧴', '健身': '💪',
  '养生': '🍵', '中医': '🌿', '药膳': '🥣', '茶道': '🍵',
  '志愿者': '🤝', '公益': '❤️', '慈善': '💝', '环保': '♻️',
  '读书': '📚', '学习': '📖', '考试': '📝', '外语': '🌍', '留学': '🎓',
  '考研': '📚', '考证': '📋', '演讲': '🎤', '辩论': '💬',
  '瑜伽': '🧘', '普拉提': '🤸', '健身': '💪', '拳击': '🥊', '散打': '🥋',
  '跆拳道': '🥋', '柔道': '🥋', '空手道': '🥋', '巴西柔术': '🥊',
  '舞蹈': '💃', '芭蕾': '🩰', '街舞': '🕺', '国标': '👫', '民族舞': '🎎',
  '唱歌': '🎤', '美声': '🎶', '说唱': '🎤', '合唱': '🎵',
  '乐器': '🎹', '钢琴': '🎹', '吉他': '🎸', '小提琴': '🎻', '古筝': '🎵',
  '二胡': '🎻', '笛子': '🎶', '架子鼓': '🥁',
}

function getHobbyEmoji(name) {
  if (!name) return '✨'
  if (hobbyEmojiMap[name]) return hobbyEmojiMap[name]
  const firstChar = name.charAt(0)
  const code = firstChar ? firstChar.charCodeAt(0) : 0
  const emojis = ['🎯', '🌟', '💡', '🎨', '🌈', '🌙', '☀️', '🍀', '🌸', '🦋']
  return emojis[code % emojis.length] || '✨'
}

// 我的爱好
const myHobbies = computed(() => {
  const h = userStore.userInfo && userStore.userInfo.hobbies
  return parseHobbies(h)
})

// TA 的兴趣（从对方资料获取）
const peerInterests = computed(() => {
  const list = parseHobbies(peer.hobbies)
  return list.map(name => ({ name, emoji: getHobbyEmoji(name) }))
})

// 我们的共同点（双方爱好交集）
const commonTags = computed(() => {
  const mine = new Set(myHobbies.value)
  if (!mine.size) return []
  const shared = parseHobbies(peer.hobbies).filter(h => mine.has(h))
  return shared.map(name => ({ name, emoji: getHobbyEmoji(name) }))
})

// 聊天热度百分比
const heatPercent = computed(() => {
  const n = messages.value.length
  if (n <= 0) return 8
  if (n >= 100) return 99
  return Math.max(8, Math.min(99, Math.round(8 + n * 0.95)))
})

// 兴趣相似度：基于共同爱好数量 / 总爱好数量
const interestSimilarity = computed(() => {
  const myList = myHobbies.value
  const peerList = parseHobbies(peer.hobbies)
  if (!myList.length && !peerList.length) return 60
  const union = new Set([...myList, ...peerList])
  if (!union.size) return 60
  const common = commonTags.value.length
  return Math.round((common / union.size) * 100)
})

// 性格契合度：基于基础分 + 互动时长
const personalityMatch = computed(() => {
  const base = 72
  const msgBonus = Math.min(15, Math.round(messages.value.length / 10))
  const commonBonus = Math.min(13, commonTags.value.length * 4)
  return Math.min(98, base + msgBonus + commonBonus)
})

// 聊天频率百分比
const chatFreq = computed(() => {
  const n = messages.value.length
  if (n <= 0) return 8
  if (n >= 100) return 99
  return Math.max(8, Math.min(99, Math.round(8 + n * 0.95)))
})

// 匹配度分数（综合计算）
const matchScore = computed(() => {
  const p = personalityMatch.value
  const i = interestSimilarity.value
  const c = chatFreq.value
  return Math.round(p * 0.4 + i * 0.35 + c * 0.25)
})

const matchEmoji = computed(() => {
  const s = matchScore.value
  if (s >= 90) return '💘'
  if (s >= 75) return '💕'
  if (s >= 60) return '✨'
  if (s >= 40) return '🌸'
  return '🌱'
})

const matchLevelText = computed(() => {
  const s = matchScore.value
  if (s >= 90) return '超合拍'
  if (s >= 75) return '很契合'
  if (s >= 60) return '聊得来'
  if (s >= 40) return '多交流'
  return '慢慢来'
})

// 浪漫语录轮播
const quotes = [
  '遇见你是故事的开始，走到底是余生的欢喜。',
  '所有的温柔眷恋，都是对你灿若星河的喜欢。',
  '两个人一起，平淡日子也会闪着光。',
  '世界很大，难得遇见想共度四季的人。',
  '慢慢来，比较快；慢慢爱，比较久。',
  '喜欢是乍见之欢，爱是久处不厌。',
  '愿你我既可以朝九晚五，又能够浪迹天涯。',
]
const quoteIdx = ref(0)
const currentQuote = computed(() => quotes[quoteIdx.value % quotes.length])
let quoteTimer = null
function startQuoteRotate() {
  quoteTimer = setInterval(() => { quoteIdx.value++ }, 7000)
}

// 匹配日期 → 天数
const matchSinceDate = computed(() => {
  if (!messages.value.length) return new Date()
  const first = messages.value[0]
  return first.createdAt ? new Date(first.createdAt) : new Date()
})
const daysSinceMatch = computed(() => {
  const diff = Date.now() - matchSinceDate.value.getTime()
  const d = Math.floor(diff / (1000 * 60 * 60 * 24))
  return Math.max(1, d)
})
const matchSince = computed(() => {
  const t = matchSinceDate.value
  return `${t.getFullYear()}.${String(t.getMonth() + 1).padStart(2, '0')}.${String(t.getDate()).padStart(2, '0')}`
})

// 字数估计：平均每条 18 字
const wordCountEstimate = computed(() => {
  let total = 0
  messages.value.forEach(m => {
    if (Number(m.msgType) === 1 && typeof m.content === 'string') {
      total += m.content.length
    } else {
      total += 6
    }
  })
  return total || 0
})

function insertQuickText(text) {
  inputText.value = (inputText.value ? inputText.value + ' ' : '') + text
  focusInput()
}

function focusInput() {
  if (msgInputRef.value && msgInputRef.value.focus) {
    msgInputRef.value.focus()
  }
}

// ========== 功能4: 长消息折叠 ==========
function isLongMsg(m) {
  if (!m || m.recalled || Number(m.msgType) === 2) return false
  const text = typeof m.content === 'string' ? m.content : ''
  // 超过 4 行文本或 >240 字符，视为长消息
  const lineBreaks = (text.match(/\n/g) || []).length
  return lineBreaks >= 4 || text.length > 240
}

function toggleMsgCollapse(m) {
  if (!m) return
  if (m._collapsed == null) m._collapsed = true
  m._collapsed = !m._collapsed
}

// ========== 功能1: 消息已发送/已读状态 ==========
// 已读状态判定：如果对方最新消息的时间 > 我方最新消息 → 视为已读
const lastReadCheckKey = ref(0)
const forceRefresh = ref(0) // 触发撤回倒计时更新
const hasUnsentText = computed(() => inputText.value.trim().length > 0)

// ========== 功能5: 消息撤回 ==========
function openMsgMenu(e, m) {
  // 任意消息都允许右键打开菜单（按钮按权限级 v-if 分别控制显示）
  if (!m) return
  const menuWidth = 220
  // 垂直方向防越界：如果下方空间不够，菜单向上弹
  const menuEstimatedH = m.recalled ? 56 : 110
  let y = e.clientY
  if (y + menuEstimatedH > window.innerHeight - 10) {
    y = Math.max(10, y - menuEstimatedH - 8)
  }
  const x = Math.max(10, Math.min(window.innerWidth - menuWidth - 10, e.clientX))
  activeMenuMsg.value = m
  menuStyle.left = x + 'px'
  menuStyle.top = y + 'px'
}
function closeMsgMenu() {
  activeMenuMsg.value = null
}

function canRecall(m) {
  if (!m || !isMine(m) || m.recalled) return false
  if (Number(m.msgType) === 2) return false
  return recallSecondsLeft(m) > 0
}
function recallSecondsLeft(m) {
  if (!m || !m.createdAt) return 0
  const t = new Date(m.createdAt).getTime()
  if (!t) return 0
  const left = RECALL_WINDOW_MS - (Date.now() - t)
  return Math.max(0, Math.ceil(left / 1000))
}

async function onRecall(m) {
  try {
    // 先本地乐观处理：直接标记撤回 + 关菜单
    m.recalled = true
    m._collapsed = false
    closeMsgMenu()
    ElMessage.success('已撤回一条消息')
  } catch (e) {
    ElMessage.error('撤回失败：' + (e?.message || '未知错误'))
  }
}

function copyMsg(m) {
  if (!m) return
  const text = (m.recalled ? '' : m.content) || ''
  try {
    if (navigator.clipboard && window.isSecureContext) {
      navigator.clipboard.writeText(text).catch(() => {})
    } else {
      const ta = document.createElement('textarea')
      ta.value = text
      ta.style.position = 'fixed'
      ta.style.left = '-9999px'
      document.body.appendChild(ta)
      ta.select()
      try { document.execCommand('copy') } catch {}
      document.body.removeChild(ta)
    }
    ElMessage.success('已复制')
  } finally {
    closeMsgMenu()
  }
}

// ========== 功能2: 打字指示器（通过轮询"对方在线&最近活跃"来近似模拟打字态） ==========
// 说明：当前是 HTTP 轮询架构，无 WS，无法真实知道对方是否在打字
// 因此用"对方最近活跃(online=true)且新消息在增长"近似模拟：
// 当一轮轮询中 messages.length 有增长但最后一条不是我发的 -> 视为对方在打字一会儿
let prevMsgLen = 0
let lastPeerMsgAt = 0

function simulatePeerTypingIfNeeded(list) {
  const latest = list[list.length - 1]
  if (!latest) return
  const currentLen = list.length
  const lastIsPeer = !isMine(latest)
  const now = Date.now()
  // 新出现了对方消息 -> 视为对方正在组织回复，显示打字提示约 3.5s
  if (currentLen > prevMsgLen && lastIsPeer) {
    lastPeerMsgAt = now
    // 不立即显示，给人呼吸感
    clearTimeout(peerTypingTimer)
    peerTypingTimer = setTimeout(() => {
      peerTyping.value = true
      clearTimeout(peerTypingTimer)
      peerTypingTimer = setTimeout(() => {
        peerTyping.value = false
      }, 3500)
    }, 1200)
  }
  prevMsgLen = currentLen
}

// ========== 功能3: 快捷回复气泡（基于对方最后一条消息生成） ==========
const lastPeerMessage = computed(() => {
  for (let i = messages.value.length - 1; i >= 0; i--) {
    const m = messages.value[i]
    if (m && !m.recalled && !isMine(m) && Number(m.msgType) !== 2) return m
  }
  return null
})

const quickReplies = computed(() => {
  forceRefresh.value // eslint-disable-line no-unused-expressions
  const m = lastPeerMessage.value
  if (!m) return []
  const text = (m.content || '').toString()
  return generateQuickReplies(text)
})

function generateQuickReplies(text) {
  if (!text) return []
  const t = text.trim()
  if (!t) return []
  const rules = [
    { key: ['你好', '嗨', 'hello', 'hi', '哈喽', '在吗'],
      vals: ['你好呀～', '嗨 😊', '在呢～在呢～', '哈喽～很高兴认识你'] },
    { key: ['多大', '几岁', '年龄'],
      vals: ['猜猜看😜', '我今年XX啦', '年龄是秘密～', '你呢先说说？'] },
    { key: ['哪里', '城市', '哪的', '住在哪'],
      vals: ['我在XX，你呢？', '猜猜我在哪个城市～', '有机会可以线下见呀～'] },
    { key: ['周末', '周六', '周日'],
      vals: ['还没安排～你呢？', '想约你看电影', '要不要一起吃点东西', '准备出门走走'] },
    { key: ['吃饭', '吃啥', '吃了', '饿', '饭'],
      vals: ['刚吃好～你呢', '想吃火锅🍲', '要不要一起～', '我正在想点什么外卖'] },
    { key: ['喜欢', '爱好', '兴趣'],
      vals: ['我喜欢很多呀～看资料卡', '音乐+摄影是本命', '你先说说你的？'] },
    { key: ['天气', '下雨', '晴天', '热', '冷'],
      vals: ['今天天气真不错～', '雨天适合窝着看剧', '注意别感冒呀'] },
    { key: ['约吗', '见面', '线下', '有空'],
      vals: ['好呀！周末我可以', '好期待～😆', '等你选个时间', '在哪见？'] },
    { key: ['哈哈', '哈哈哈', '笑死', '笑哭', '🤣', '😂'],
      vals: ['哈哈哈哈哈哈', '好好笑🤣', '你真幽默', '笑点被戳中了'] },
    { key: ['谢谢', '感谢', '谢啦'],
      vals: ['客气什么呀～', '不客气😊', '小事一桩', '回请我喝奶茶哈哈哈'] },
    { key: ['照片', '拍', '摄影', '相机'],
      vals: ['你拍的真的好好看！', '下次带我一起？', '这构图太有感觉了'] },
    { key: ['工作', '上班', '加班', '忙'],
      vals: ['注意休息呀', '辛苦啦～抱抱', '周末犒劳自己', '一起摸鱼🐟'] },
  ]
  for (const rule of rules) {
    if (rule.key.some(k => t.toLowerCase().includes(k.toLowerCase()))) {
      return rule.vals
    }
  }
  // 默认通用回复
  return [
    '嗯嗯我懂了',
    '然后呢？🤔',
    '真的吗太巧了',
    '好有趣～继续说说',
  ]
}

function useQuickReply(text) {
  insertQuickText(text)
  onSend()
}

// ========== 功能4: Enter发送 / Ctrl+Enter换行 ==========
function onInputKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey && (e.ctrlKey || e.metaKey)) {
    // Ctrl/Cmd + Enter 允许换行（不阻止），textarea 自带
  } else if (e.key === 'Enter' && !e.shiftKey) {
    // 单独的 Enter -> 发送
    e.preventDefault()
    onSend()
  }
}

// 我方打字信号（预留接口上报，默认节流 3s）
function onMyTyping() {
  const now = Date.now()
  if (now - myTypingSentAt.value < 3000) return
  myTypingSentAt.value = now
  // TODO: 如后端提供 typing 事件，可在这里发请求
}

function goUserProfile() {
  if (peer.id) router.push(`/user/${peer.id}`)
}

// Check if current user is logged in (for self online status)
const isSelfOnline = () => !!userStore.token

// 真实判定对方是否在线：调 getUserById，后端根据 onlineStatus + lastActiveAt 统一计算
// - onlineStatus=1 且 lastActiveAt 在 60 秒内 → true
// - onlineStatus=2（隐身）永远返回 false
async function updatePeerOnline() {
  if (!peer.id) {
    peer.online = false
    return
  }
  // 自己和自己聊的特殊情况
  if (Number(peer.id) === Number(userStore.userId)) {
    peer.online = isSelfOnline()
    return
  }
  try {
    const res = await getUserById(peer.id)
    const u = (res && res.data) || res || {}
    peer.online = u.online === true || u.online === 'true'
  } catch (e) {
    peer.online = false
  }
}
const PAGE_SIZE = 20          // 每页消息条数（首屏/上滑都用它）
const loading = ref(false)   // 首屏加载 true / false
const loadingMore = ref(false) // 上滑加载更多历史 true / false
const hasMore = ref(true)    // 是否还有更早历史可加载（默认 true，查完已知≤total时才false）
const pageNow = ref(1)       // 已加载的最后一页（首屏=1，滑一次+1）
const totalCount = ref(0)    // 后端返回的总记录数，用于判断 hasMore
const msgArea = ref(null)
const msgList = ref(null)
const fileInput = ref(null)
let timer = null
let _scrollLockUntil = 0     // 上滑加载更多的防抖锁时间戳

const sessionId = ref(route.params.sessionId)

function isMine(m) {
  return Number(m.senderId) === Number(userStore.userId)
}

// 消息是否"已读"判定：如果存在对方发送的消息比我方最后一条更新，那么我方都被对方读过了
const allMyMessagesRead = computed(() => {
  lastReadCheckKey.value // eslint-disable-line no-unused-expressions
  let lastMineTime = -1
  let anyPeer = null
  for (const m of messages.value) {
    if (!m || m.recalled) continue
    if (isMine(m)) {
      lastMineTime = Math.max(lastMineTime, new Date(m.createdAt || 0).getTime())
    } else {
      anyPeer = m
    }
  }
  if (lastMineTime <= 0) return false
  if (!anyPeer) return false
  const latestPeerTime = messages.value.reduce((max, m) => {
    if (m && !m.recalled && !isMine(m)) {
      return Math.max(max, new Date(m.createdAt || 0).getTime())
    }
    return max
  }, -1)
  return latestPeerTime > lastMineTime
})

// 包装消息：默认 _collapsed=true（长消息折叠）、read 默认 false
function prepareMessage(m) {
  if (!m) return m
  if (Number(m.msgType) !== 2 && isLongMsg(m) && m._collapsed == null) {
    m._collapsed = true
  }
  return m
}

function msgAvatar(m) {
  if (isMine(m)) {
    return resolveAvatar(userStore.userInfo && userStore.userInfo.avatar, userStore.nickname)
  }
  return resolveAvatar(peer.avatar, peer.nickname)
}

// 把后端分页返回的 records 按时间升序处理（oldest first）
function normalizeRecords(recordsOrData) {
  let list = recordsOrData
  if (list && list.records) list = list.records
  if (!Array.isArray(list)) list = []
  list.sort((a, b) => {
    const ta = a.createdAt ? new Date(a.createdAt).getTime() : 0
    const tb = b.createdAt ? new Date(b.createdAt).getTime() : 0
    return ta - tb
  })
  list.forEach(m => prepareMessage(m))
  return list
}
// 计算是否还有更多历史：后端 IPage 的 total 字段优先，其次用 records.length < pageSize 兜底
function updateHasMoreByResult(data, recordsLen, reqPageSize) {
  const total = (data && (data.total != null)) ? Number(data.total) : NaN
  if (!Number.isNaN(total)) {
    totalCount.value = total
    hasMore.value = (pageNow.value * reqPageSize) < total
  } else {
    // 无 total 字段时，若当页返回不满一页 → 认为到顶
    hasMore.value = recordsLen >= reqPageSize
  }
}
// 合并时本地已撤回/折叠的状态不丢失
function restoreLocalStates(list) {
  const recalledMap = new Map()
  const collapsedMap = new Map()
  messages.value.forEach(m => {
    if (m && m.recalled) recalledMap.set(String(m.id), true)
    if (m && m._collapsed != null) collapsedMap.set(String(m.id), !!m._collapsed)
  })
  list.forEach(m => {
    if (m && recalledMap.has(String(m.id))) m.recalled = true
    if (m && collapsedMap.has(String(m.id))) m._collapsed = collapsedMap.get(String(m.id))
  })
  return list
}

async function loadMessages() {
  loading.value = true
  pageNow.value = 1
  totalCount.value = 0
  try {
    const res = await getMessages(sessionId.value, { page: 1, pageSize: PAGE_SIZE })
    const data = res.data || res
    const list = restoreLocalStates(normalizeRecords(data))
    messages.value = list
    updateHasMoreByResult(data, list.length, PAGE_SIZE)
    simulatePeerTypingIfNeeded(list)
    await markRead(sessionId.value).catch(() => {})
    scrollToBottom()
  } catch (e) {
    messages.value = []
    hasMore.value = false
  } finally {
    loading.value = false
  }
}

// 上滑到顶 → 加载下一页"更早的历史"（pageNow+1），prepend 到列表头部并保持当前滚动位置不动
async function loadMoreHistory() {
  if (loading.value || loadingMore.value || !hasMore.value) return
  const nextPage = pageNow.value + 1
  loadingMore.value = true
  try {
    const prevScrollTop = msgArea.value ? msgArea.value.scrollTop : 0
    const prevScrollHeight = msgArea.value ? msgArea.value.scrollHeight : 0
    const prevClientHeight = msgArea.value ? msgArea.value.clientHeight : 0

    const res = await getMessages(sessionId.value, { page: nextPage, pageSize: PAGE_SIZE })
    const data = res.data || res
    const olderList = restoreLocalStates(normalizeRecords(data))
    if (olderList.length) {
      messages.value = olderList.concat(messages.value)
      pageNow.value = nextPage
    }
    updateHasMoreByResult(data, olderList.length, PAGE_SIZE)

    // === 关键：插入更多历史后，把滚动条固定在原"最新在下方"的视觉位置，不跳 ===
    nextTick(() => {
      if (!msgArea.value) return
      const newScrollHeight = msgArea.value.scrollHeight
      const diff = newScrollHeight - prevScrollHeight
      if (diff > 0) {
        msgArea.value.scrollTop = prevScrollTop + diff
      }
    })
    void prevClientHeight
  } catch (e) {
    // 失败保守处理：不更新 pageNow
  } finally {
    loadingMore.value = false
  }
}

async function loadPeerInfo() {
  // 先尝试从会话列表找到 peer 信息
  try {
    const res = await getChatList()
    const sessions = res.data || res || []
    const s = sessions.find(s => Number(s.id) === Number(sessionId.value))
    if (s) {
      peer.id = s.userId
      peer.nickname = s.nickname
      peer.avatar = s.avatar
      peer.online = s.online === true || s.online === 'true'
    }
  } catch (e) {
    // 忽略，走下面的兜底
  }

  // 兜底：从消息列表找对方
  if (!peer.id) {
    const other = messages.value.find((m) => !isMine(m))
    if (other && other.senderId) {
      peer.id = other.senderId
    }
  }

  // 通过 getUserById 获取对方详细资料（包含爱好）
  if (peer.id) {
    try {
      const res = await getUserById(peer.id)
      const u = res.data || res
      peer.nickname = u.nickname || peer.nickname
      peer.avatar = u.avatar || peer.avatar
      peer.hobbies = u.hobbies || ''
    } catch (e) {
      // 忽略
    }
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (msgArea.value) {
      msgArea.value.scrollTop = msgArea.value.scrollHeight
    }
  })
}

function onScroll() {
  // 上滑加载更早历史：距顶 ≤ 30px 时触发
  if (!msgArea.value) return
  const top = msgArea.value.scrollTop
  if (top > 30) return
  // 防抖锁 1.2s：避免并发请求
  const now = Date.now()
  if (now < _scrollLockUntil) return
  _scrollLockUntil = now + 1200
  loadMoreHistory()
}

async function onSend() {
  const text = inputText.value
  if (!text || !text.trim()) return
  const trimmed = text // 保留换行（textarea 多行）
  try {
    const res = await sendMessage({ sessionId: sessionId.value, receiverId: peer.id, msgType: 1, content: trimmed })
    let msg = (res && res.data) || res
    if (!msg || !msg.id) {
      msg = {
        id: Date.now(),
        senderId: userStore.userId,
        receiverId: peer.id,
        msgType: 1,
        content: trimmed,
        createdAt: new Date().toISOString()
      }
    }
    prepareMessage(msg)
    messages.value.push(msg)
    inputText.value = ''
    // 轮询判定已读：递增 key 触发重算
    lastReadCheckKey.value++
    scrollToBottom()
    updatePeerOnline()
  } catch (e) {
    // 忽略
  }
}

function onImageClick() {
  fileInput.value && fileInput.value.click()
}

async function onImageChange(e) {
  const file = e.target.files[0]
  if (!file) return
  try {
    const upRes = await uploadImage(file)
    const url = (upRes.data && upRes.data.url) || upRes.data || upRes.url
    const res = await sendMessage({ sessionId: sessionId.value, receiverId: peer.id, msgType: 2, content: resolveImage(url) })
    let msg = (res && res.data) || res
    if (!msg || !msg.id) {
      msg = {
        id: Date.now(),
        senderId: userStore.userId,
        receiverId: peer.id,
        msgType: 2,
        content: resolveImage(url),
        createdAt: new Date().toISOString()
      }
    }
    messages.value.push(msg)
    scrollToBottom()
    updatePeerOnline()
  } catch (err) {
    // 忽略
  } finally {
    e.target.value = ''
  }
}

function startPolling() {
  // 轮询拉取新消息（演示用，生产建议 WebSocket）
  // 每次拉取 pageNow * PAGE_SIZE 条（保证覆盖所有已加载过的历史），避免用户手动上滑历史后被截断
  timer = setInterval(async () => {
    try {
      const wantPageSize = Math.max(PAGE_SIZE, pageNow.value * PAGE_SIZE)
      // page = 1 + 向上取整，使用 MyBatis-Plus IPage 分页语义：若需要N条总，一页也能装下则直接
      const res = await getMessages(sessionId.value, { page: 1, pageSize: wantPageSize })
      const data = res.data || res
      const newList = restoreLocalStates(normalizeRecords(data))

      // 同步 hasMore 与 total
      const t = (data && data.total != null) ? Number(data.total) : NaN
      if (!Number.isNaN(t)) {
        totalCount.value = t
        hasMore.value = (pageNow.value * PAGE_SIZE) < t
      }

      simulatePeerTypingIfNeeded(newList)
      // 判断是否有"新增消息(对方或自己发的)"：仅比条数不够精确，用尾部最后一条 id 比较
      const prevLastId = messages.value.length ? String(messages.value[messages.value.length - 1].id) : ''
      const newLastId = newList.length ? String(newList[newList.length - 1].id) : ''
      const hasNewMsg = prevLastId !== newLastId

      // 只有当列表长度变化或末尾消息不一致时才整体替换
      if (newList.length !== messages.value.length || hasNewMsg) {
        // 保存当前用户阅读位置：若不在底部，则不自动滚到底（尊重用户看历史的操作）
        const area = msgArea.value
        const wasNearBottom = !area || (area.scrollTop + area.clientHeight >= area.scrollHeight - 60)
        messages.value = newList
        await markRead(sessionId.value).catch(() => {})
        lastReadCheckKey.value++
        if (wasNearBottom) scrollToBottom()
      }
      // 每轮轮询都同步刷新对方在线状态（2 秒一次，保证对方下线后 ≤ 2 秒变离线）
      updatePeerOnline()
    } catch (e) {
      // 忽略
    }
  }, 2000)
}

let tickTimer = null
let _docClickClose = null
onMounted(async () => {
  if (!userStore.userInfo) {
    await userStore.fetchUserInfo()
  }
  // 若传入的是 userId 而非 sessionId（兜底），尝试创建会话
  await loadMessages()
  await loadPeerInfo()
  updatePeerOnline()
  startPolling()
  startQuoteRotate()
  // 撤回倒计时 + 气泡刷新：每 1 秒触发 UI 重新计算
  tickTimer = setInterval(() => {
    forceRefresh.value++
    lastReadCheckKey.value++
  }, 1000)
  // 菜单 Teleport 到 body 后，msg-area 的 @click 无法捕获菜单外部点击
  // 需全局监听：点击非菜单区域时关闭菜单
  _docClickClose = (e) => {
    const target = e.target
    const hasClosest = (sel) => !!(target && target.closest && target.closest(sel))
    // 右键菜单
    if (activeMenuMsg.value && !hasClosest('.msg-menu')) closeMsgMenu()
    // 主题面板
    if (showThemePanel.value && !hasClosest('.mf-theme-panel')) showThemePanel.value = false
    // 表情面板
    if (showEmojiPanel.value && !hasClosest('.mf-emoji-panel')) showEmojiPanel.value = false
  }
  document.addEventListener('click', _docClickClose, true)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  if (quoteTimer) clearInterval(quoteTimer)
  if (peerTypingTimer) clearTimeout(peerTypingTimer)
  if (tickTimer) clearInterval(tickTimer)
  if (_docClickClose) document.removeEventListener('click', _docClickClose, true)
})
</script>

<style lang="scss" scoped>
/* ========== 三栏主布局 ========== */
.chat-room {
  --pane-w: clamp(180px, 22vw, 256px);
  box-sizing: border-box;
  display: grid;
  grid-template-columns: var(--pane-w) minmax(0, 1fr) var(--pane-w);
  grid-template-rows: 1fr;
  gap: clamp(8px, 1vw, 14px);
  height: min(calc(100dvh - 64px), calc(100vh - 64px));
  padding: clamp(6px, 0.8vw, 12px);
  background: var(--theme-bg-grad,
    radial-gradient(1200px 600px at 0% 0%, rgba(255, 173, 202, 0.26), transparent 60%),
    radial-gradient(1000px 600px at 100% 100%, rgba(110, 168, 255, 0.2), transparent 60%),
    radial-gradient(800px 500px at 50% -10%, rgba(192, 88, 242, 0.15), transparent 60%),
    linear-gradient(180deg, #fff5fa 0%, #f2f3fb 50%, #f5f0ff 100%));
  overflow: hidden;
}
.chat-room * {
  box-sizing: border-box;
}

.glass-card {
  height: 100%;
  box-sizing: border-box;
  background: var(--theme-card-bg, rgba(255, 255, 255, 0.62));
  backdrop-filter: blur(18px) saturate(1.25);
  -webkit-backdrop-filter: blur(18px) saturate(1.25);
  border: 1px solid var(--theme-card-border, rgba(255, 255, 255, 0.7));
  border-radius: 24px;
  box-shadow:
    0 12px 36px rgba(30, 20, 60, 0.07),
    inset 0 1px 0 rgba(255, 255, 255, 0.85);
  position: relative;
  min-height: 0;
}
/* 仅中间聊天主区使用 overflow:hidden 包裹气泡阴影 */
.chat-main.glass-card { overflow: hidden; }
/* 侧边栏允许内容溢出以便内部滚动 */
.side-pane.glass-card { overflow: visible; }

/* ========== 中间主聊天（保留原样式，只做微调） ========== */
.chat-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;

  .room-header {
    background: var(--theme-header-bg, linear-gradient(180deg, rgba(255,255,255,0.82), rgba(255,255,255,0.42)));
    border-bottom: 1px solid rgba(255, 107, 157, 0.14);
    flex: 0 0 auto;
  }
  .header-inner {
    max-width: min(1100px, 96%);
    margin: 0 auto;
    height: 62px;
    padding: 0 clamp(12px, 1.4vw, 20px);
    display: flex;
    align-items: center;
    gap: 12px;

    .back-btn {
      display: flex; align-items: center; justify-content: center;
      width: 36px; height: 36px; border-radius: 50%;
      cursor: pointer; color: #6a6a7a; transition: all 0.2s;
      &:hover { background: var(--theme-primary-soft, #fff0f5); color: var(--theme-primary, #ff4f8b); transform: translateX(-2px); }
    }

    .peer-info {
      flex: 1; display: flex; flex-direction: column; gap: 2px;
      .peer-name { font-size: 16px; font-weight: 700; color: var(--theme-bubble-peer-text, #2d2d3a); }
      .peer-status {
        display: inline-flex; align-items: center; gap: 5px;
        font-size: 12px; color: #aaa;
        .dot { width: 7px; height: 7px; border-radius: 50%; background: #ccc; }
        &.online { color: #22c55e; .dot { background: #22c55e; box-shadow: 0 0 0 3px rgba(34,197,94,0.18); animation: pulseGreen 2s ease infinite; } }
      }
    }
    .header-actions {
      display: inline-flex; align-items: center; gap: 4px; margin-left: auto;
    }
    .more-btn {
      display: flex; align-items: center; justify-content: center;
      width: 36px; height: 36px; border-radius: 50%; cursor: pointer; color: var(--theme-primary, #aaa); transition: all 0.2s;
      &:hover { background: var(--theme-primary-soft, rgba(0,0,0,0.04)); }
    }
  }

  .msg-area {
    flex: 1 1 auto;
    overflow-y: auto;
    padding: 18px 0 6px;
    min-height: 0;
    scrollbar-width: thin; scrollbar-color: rgba(255, 107, 157, 0.25) transparent;
    &::-webkit-scrollbar { width: 8px; }
    &::-webkit-scrollbar-thumb { background: rgba(255, 107, 157, 0.25); border-radius: 999px; }
  }
  .msg-list {
    max-width: min(1060px, 94%);
    margin: 0 auto;
    padding: 6px 0 0;
    display: flex; flex-direction: column; gap: 18px;
    .load-tip {
      width: 100%;
      text-align: center;
      font-size: 12px;
      font-weight: 600;
      color: var(--theme-primary, #b8a8c8);
      padding: 10px 0 14px;
      letter-spacing: 0.02em;
      opacity: 0.85;
      flex: 0 0 auto;
    }
  }
  .msg-empty { padding: 60px 20px; text-align: center; color: #9a9aaa;
    .empty-emoji { font-size: 48px; margin-bottom: 10px; animation: float 4s ease-in-out infinite; }
  }

  .msg-row {
    display: flex; align-items: flex-start; gap: 10px; width: 100%;
    position: relative;
    .msg-avatar {
      flex-shrink: 0; border: 2px solid #fff; box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08); order: 1;
    }
    .msg-content { max-width: 78%; display: flex; flex-direction: column; gap: 4px; min-width: 0; order: 1; position: relative; }
    .bubble {
      padding: 11px 16px; border-radius: 18px; background: var(--theme-bubble-peer, #ffffff); color: var(--theme-bubble-peer-text, #3a3a4a);
      font-size: 15px; line-height: 1.55; word-break: break-word; white-space: pre-wrap;
      box-shadow: 0 4px 14px rgba(30, 20, 60, 0.07), inset 0 1px 0 rgba(255, 255, 255, 0.9);
      border-top-left-radius: 4px;
      position: relative;
      .msg-text { display: block; }
      .msg-image { max-width: 240px; border-radius: 12px; cursor: pointer; }
      .recall-text { font-style: italic; color: #a9a0b5; font-size: 13px; }
      .expand-toggle {
        margin-top: 8px;
        text-align: right;
        font-size: 12px;
        cursor: pointer;
        opacity: 0.75;
        user-select: none;
      }
      &.long.collapsed {
        .msg-text {
          display: -webkit-box;
          -webkit-line-clamp: 4;
          line-clamp: 4;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }
      }
    }
    .msg-foot {
      display: flex; align-items: center; gap: 6px;
      width: 100%;
    }
    .msg-time { font-size: 11px; color: #bbb; padding: 0 4px; }
    .msg-state {
      margin-left: auto;
      display: inline-flex; align-items: center; gap: -2px;
      font-size: 11px;
      color: #b9a9cc;
      &.read { color: var(--theme-primary, #a855f7); }
      svg { width: 11px; height: 11px; margin-left: -2px; }
    }

    /* 撤回消息行：弱化显示 */
    &.recall {
      .bubble {
        background: transparent !important;
        box-shadow: none !important;
        color: #a9a0b5;
        border: 1px dashed var(--theme-primary-soft, rgba(168,85,247,0.2));
      }
    }
    &.mine {
      justify-content: flex-end;
      .msg-avatar { order: 3; }
      .msg-content { order: 2; align-items: flex-end; max-width: 68%; }
      .bubble {
        background: var(--theme-bubble-mine, linear-gradient(135deg, #ff6b9d 0%, #c058f2 60%, #9a67ff 100%));
        color: var(--theme-bubble-mine-text, #fff); border-top-left-radius: 18px; border-top-right-radius: 4px;
        box-shadow: 0 8px 22px rgba(168, 85, 247, 0.28), inset 0 1px 0 rgba(255,255,255,0.25);
      }
    }
  }

  .input-area {
    flex: 0 0 auto;
    max-width: min(1100px, 96%);
    width: 100%;
    margin: 0 auto;
    padding: 10px clamp(12px, 1.4vw, 18px) 14px;
    background: var(--theme-input-bg, linear-gradient(180deg, rgba(255,255,255,0.2), rgba(255,255,255,0.85)));
    backdrop-filter: blur(14px);
    border-top: 1px solid rgba(255, 107, 157, 0.12);
    display: flex; align-items: center; gap: 10px;
    .icon-btn {
      display: flex; align-items: center; justify-content: center;
      width: 40px; height: 40px; border-radius: 50%;
      cursor: pointer; color: var(--theme-primary, #8a8a9a); flex-shrink: 0; transition: all 0.2s;
      &:hover { background: var(--theme-primary-soft, #fff0f5); color: var(--theme-primary, #ff4f8b); transform: translateY(-1px); }
    }
    .msg-input { flex: 1; }
    .send-btn {
      flex-shrink: 0; width: 46px; height: 40px; border: none; border-radius: 14px;
      background: var(--theme-bubble-mine, linear-gradient(135deg, #ff6b9d 0%, #a855f7 100%));
      color: var(--theme-bubble-mine-text, #fff); font-size: 18px; cursor: pointer;
      display: flex; align-items: center; justify-content: center;
      transition: all 0.25s ease;
      box-shadow: 0 6px 14px var(--theme-primary-soft, rgba(255, 107, 157, 0.35));
      &:hover:not(:disabled) { transform: translateY(-2px) scale(1.02); box-shadow: 0 8px 20px var(--theme-primary-soft, rgba(168, 85, 247, 0.45)); }
      &:disabled { opacity: 0.5; cursor: not-allowed; }
    }
  }
}

/* 顶部打字指示器小圆点动效 */
.typing-text {
  .dots {
    display: inline-flex;
    margin-left: 2px;
    span {
      display: inline-block;
      width: 3px; height: 3px;
      border-radius: 50%;
      background: currentColor;
      margin: 0 1px;
      opacity: 0.3;
      animation: typingBounce 1.2s ease-in-out infinite;
      &:nth-child(2) { animation-delay: 0.2s; }
      &:nth-child(3) { animation-delay: 0.4s; }
    }
  }
}
@keyframes typingBounce {
  0%, 100% { transform: translateY(0); opacity: 0.3; }
  50% { transform: translateY(-3px); opacity: 1; }
}

/* ========== 中间主聊天区新增样式 ========== */

/* 快捷回复条 */
.quick-reply-bar {
  flex: 0 0 auto;
  max-width: min(1100px, 96%);
  width: 100%;
  margin: 0 auto;
  padding: 0 clamp(12px, 1.4vw, 18px);
}
.qr-scroll {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  overflow-y: hidden;
  padding: 4px 2px 10px;
  scrollbar-width: none;
  &::-webkit-scrollbar { display: none; }
}
.qr-chip {
  flex-shrink: 0;
  padding: 7px 14px;
  font-size: 13px;
  font-weight: 600;
  color: #8b39c9;
  background: linear-gradient(135deg, #fff0f7 0%, #efeaff 100%);
  border: 1px solid rgba(168, 85, 247, 0.25);
  border-radius: 999px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s ease;
  &:hover {
    transform: translateY(-1px);
    color: #fff;
    background: linear-gradient(135deg, #ff6b9d 0%, #a855f7 100%);
    border-color: transparent;
    box-shadow: 0 6px 14px rgba(168, 85, 247, 0.28);
  }
  &:active { transform: translateY(0); }
}

/* 右键自定义菜单 → 已移至底部非 scoped style 块（Teleport 到 body 后 scoped 属性不生效） */

/* 聊天主区 textarea 样式微调（行高/换行） */
.chat-main .msg-input {
  ::v-deep(.el-textarea__inner) {
    border-radius: 20px !important;
    padding: 10px 16px !important;
    font-size: 15px !important;
    line-height: 1.55 !important;
    min-height: 40px !important;
    box-shadow: 0 2px 10px rgba(30,20,60,0.06) !important;
    border: 1px solid transparent !important;
    resize: none !important;
    transition: border-color 0.2s, box-shadow 0.2s !important;
    &:focus {
      border-color: rgba(168, 85, 247, 0.4) !important;
      box-shadow: 0 4px 18px rgba(168, 85, 247, 0.15) !important;
    }
  }
}

::v-deep(.el-input__wrapper) {
  border-radius: 999px !important;
  box-shadow: 0 2px 10px rgba(30,20,60,0.06) !important;
}

/* ========== 左右侧边栏通用 ========== */
.side-pane {
  position: relative;
  padding: 0;              /* 滚动容器内的 padding 交给 side-pane__scroll，避免 2 层 padding 叠加 */
  min-width: 0;
  min-height: 0;
  max-height: 100%;
  /* overflow 交给 glass-card 规则（visible 保留装饰元素不裁剪） */
  animation: pane-fade 0.55s cubic-bezier(0.22, 1, 0.36, 1) both;
  &--left  { animation-delay: 0.04s; }
  &--right { animation-delay: 0.14s; }
}

/* 侧边栏内真正的滚动容器：装饰元素留在 side-pane 外层，内容在此滚动 */
.side-pane__scroll {
  position: relative;       /* 不抢 sparkle 的 absolute 定位（sparkle 父级是 .side-pane） */
  z-index: 1;
  box-sizing: border-box;
  height: 100%;
  min-height: 0;
  width: 100%;
  padding: clamp(8px, 1vw, 16px) clamp(8px, 1vw, 14px) clamp(12px, 1.5vw, 20px);
  display: flex;
  flex-direction: column;
  gap: clamp(8px, 0.9vw, 14px);
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: thin;
  scrollbar-color: rgba(168, 85, 247, 0.38) transparent;
  &::-webkit-scrollbar { width: 6px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb {
    background: rgba(168, 85, 247, 0.32);
    border-radius: 999px;
    &:hover { background: rgba(168, 85, 247, 0.55); }
  }
  /* 所有内容卡片禁止被 flex 压缩，溢出由本容器滚动承担（默认 flex-shrink:1 会把卡片挤扁裁剪） */
  > * { flex: 0 0 auto; min-width: 0; }
}
@keyframes pane-fade {
  from { opacity: 0; transform: translateY(10px) scale(0.99); }
  to   { opacity: 1; transform: none; }
}

/* 星点装饰 */
.sparkle {
  position: absolute;
  width: 6px; height: 6px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 0 0 2px rgba(255,107,157,0.25), 0 0 10px rgba(255,107,157,0.55);
  pointer-events: none;
  animation: sparkle 3s ease-in-out infinite;
  &--1  { top: 14px;  left: 18px; animation-delay: 0s; }
  &--2  { top: 48px;  right: 22px; width: 4px; height: 4px; animation-delay: 1.2s; }
  &--3  { bottom: 70px; left: 26px; width: 5px; height: 5px; animation-delay: 2.1s; }
  &--r1 { top: 20px;  right: 20px; animation-delay: 0.4s; }
  &--r2 { bottom: 120px; right: 16px; width: 4px; height: 4px; animation-delay: 1.6s; }
}
@keyframes sparkle {
  0%, 100% { opacity: 0.2; transform: scale(0.7); }
  50%      { opacity: 1;   transform: scale(1.3); }
}

.pane-divider {
  height: 1px; flex-shrink: 0;
  background: linear-gradient(90deg, transparent, rgba(255, 107, 157, 0.3), rgba(110,168,255,0.22), transparent);
}

/* ======================================================= */
/* 左侧栏特有                                              */
/* ======================================================= */

/* 重叠头像 + 光晕 + 爱心连接点 */
.pane-avatar-stack {
  position: relative;
  height: 92px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 2px 0 -2px;
  .avatar-glow {
    position: absolute;
    left: 50%; top: 50%;
    transform: translate(-50%, -50%);
    width: 160px; height: 160px;
    border-radius: 50%;
    background: radial-gradient(circle, rgba(255,107,157,0.25), rgba(192,88,242,0.15) 40%, transparent 70%);
    filter: blur(4px);
    animation: breathe 4.5s ease-in-out infinite;
    pointer-events: none;
  }
  .pane-peer {
    position: relative;
    z-index: 2;
    box-shadow: 0 10px 26px rgba(168,85,247,0.3);
    border: 3px solid #fff;
  }
  .pane-me {
    position: absolute;
    right: 22px; bottom: 0;
    z-index: 3;
    border: 3px solid #fff;
    box-shadow: 0 8px 20px rgba(255,107,157,0.35);
  }
  .avatar-link-dot {
    position: absolute;
    left: 54%;
    top: 58%;
    z-index: 4;
    width: 22px; height: 22px;
    display: flex; align-items: center; justify-content: center;
    border-radius: 50%;
    background: linear-gradient(135deg, #ff6b9d, #c058f2);
    box-shadow: 0 4px 12px rgba(255,107,157,0.45), 0 0 0 3px #fff;
  }
}
@keyframes breathe {
  0%, 100% { opacity: 0.7; transform: translate(-50%, -50%) scale(1); }
  50%      { opacity: 1;   transform: translate(-50%, -50%) scale(1.08); }
}

/* 心情卡 */
.pane-mood-card {
  padding: 16px 14px;
  border-radius: 18px;
  background: linear-gradient(145deg, rgba(255, 210, 230, 0.7), rgba(230, 215, 255, 0.7) 60%, rgba(210, 225, 255, 0.6));
  text-align: center;
  position: relative;
  overflow: hidden;
  &::before {
    content: '';
    position: absolute;
    inset: -40% -50% auto auto;
    width: 140px; height: 140px;
    background: radial-gradient(circle, rgba(255,255,255,0.6), transparent 65%);
    pointer-events: none;
  }
  .pane-mood-emoji { font-size: 30px; line-height: 1; margin-bottom: 8px; display: inline-block; animation: bob 3s ease-in-out infinite; }
  .pane-mood-title { font-size: 14px; font-weight: 700; color: #3d2a4e; position: relative; }
  .pane-mood-desc  { font-size: 12px; color: #7d6f8e; margin-top: 4px; position: relative; }
}
@keyframes bob {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50%      { transform: translateY(-3px) rotate(-6deg); }
}

/* 聊天热度条 */
.heat-card {
  padding: 14px 14px 12px;
  border-radius: 16px;
  background: rgba(255,255,255,0.6);
  border: 1px solid rgba(255,107,157,0.08);
  .heat-head {
    display: flex; align-items: center; justify-content: space-between;
    margin-bottom: 10px;
    .heat-title { font-size: 13px; font-weight: 700; color: #4a3a5a; }
    .heat-val   { font-size: 14px; font-weight: 800; background: linear-gradient(135deg, #ff6b9d, #a855f7); -webkit-background-clip: text; background-clip: text; color: transparent; }
  }
  .heat-bar {
    height: 8px;
    border-radius: 999px;
    background: rgba(168,85,247,0.08);
    overflow: hidden;
    position: relative;
    &__fill {
      height: 100%;
      border-radius: 999px;
      background: linear-gradient(90deg, #ff6b9d, #c058f2 55%, #6ea8ff);
      box-shadow: 0 0 12px rgba(192,88,242,0.45);
      transition: width 0.9s cubic-bezier(0.22, 1, 0.36, 1);
      position: relative;
      &::after {
        content: '';
        position: absolute;
        inset: 0;
        background: linear-gradient(90deg, transparent, rgba(255,255,255,0.45), transparent);
        animation: shimmer 2.5s linear infinite;
      }
    }
  }
  .heat-sub { margin-top: 9px; font-size: 11.5px; color: #7d6f8e; line-height: 1.5; }
}
@keyframes shimmer {
  0%   { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

/* 标签标题通用 */
.tags-title, .tips-title, .story-title, .match-title {
  font-size: 12.5px;
  font-weight: 700;
  color: #5a4a6a;
  letter-spacing: 0.02em;
  margin-bottom: 10px;
}

/* 共同兴趣标签 */
.tags-card { padding: 2px 2px 0; }
.tags-wrap {
  display: flex; flex-wrap: wrap; gap: 7px;
}
.tag-chip {
  display: inline-flex; align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  cursor: default;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  &:hover { transform: translateY(-2px); }
  &--0 { background: linear-gradient(135deg, rgba(255,107,157,0.16), rgba(255,107,157,0.06)); color: #d83f77; box-shadow: inset 0 0 0 1px rgba(255,107,157,0.22); &:hover { box-shadow: inset 0 0 0 1px rgba(255,107,157,0.22), 0 6px 14px rgba(255,107,157,0.2); } }
  &--1 { background: linear-gradient(135deg, rgba(192,88,242,0.16), rgba(192,88,242,0.06)); color: #8b39c9; box-shadow: inset 0 0 0 1px rgba(192,88,242,0.22); &:hover { box-shadow: inset 0 0 0 1px rgba(192,88,242,0.22), 0 6px 14px rgba(192,88,242,0.22); } }
  &--2 { background: linear-gradient(135deg, rgba(110,168,255,0.18), rgba(110,168,255,0.06)); color: #2f6fd6; box-shadow: inset 0 0 0 1px rgba(110,168,255,0.22); &:hover { box-shadow: inset 0 0 0 1px rgba(110,168,255,0.22), 0 6px 14px rgba(110,168,255,0.2); } }
  &--3 { background: linear-gradient(135deg, rgba(34,197,94,0.16), rgba(110,168,255,0.08)); color: #15803d; box-shadow: inset 0 0 0 1px rgba(34,197,94,0.2); &:hover { box-shadow: inset 0 0 0 1px rgba(34,197,94,0.2), 0 6px 14px rgba(34,197,94,0.18); } }
}

/* 聊天小贴士 */
.pane-tips { padding: 2px 4px 0; display: flex; flex-direction: column; gap: 9px; }
.pane-tip {
  display: flex; align-items: flex-start; gap: 8px;
  font-size: 12px; line-height: 1.55; color: #6f6f82;
}
.pane-tip-dot {
  margin-top: 6px; flex-shrink: 0;
  width: 6px; height: 6px; border-radius: 50%;
  background: linear-gradient(135deg, #ff6b9d, #a855f7);
  box-shadow: 0 0 0 3px rgba(255, 107, 157, 0.18);
}

/* 浪漫语录卡 */
.quote-card {
  position: relative;
  padding: 18px 14px 16px;
  border-radius: 18px;
  background:
    linear-gradient(145deg, rgba(255, 220, 235, 0.55), rgba(210, 225, 255, 0.5)),
    repeating-linear-gradient(135deg, rgba(255,255,255,0.15) 0 2px, transparent 2px 6px);
  overflow: hidden;
  .quote-mark {
    position: absolute;
    top: -6px; left: 8px;
    font-size: 58px;
    line-height: 1;
    font-family: Georgia, serif;
    color: rgba(255,107,157,0.25);
    pointer-events: none;
  }
  .quote-text {
    font-size: 13px;
    line-height: 1.7;
    color: #5a4668;
    font-weight: 500;
    text-align: center;
    animation: quoteIn 0.8s ease both;
    padding: 4px 2px 0;
  }
  .quote-author {
    margin-top: 10px;
    text-align: center;
    font-size: 10.5px;
    letter-spacing: 0.2em;
    color: #a89cb8;
    font-weight: 600;
  }
}
@keyframes quoteIn {
  from { opacity: 0; transform: translateY(6px); filter: blur(4px); }
  to   { opacity: 1; transform: none; filter: none; }
}

/* 左栏 3 个浮动装饰气泡 */
.pane-decor {
  position: absolute;
  border-radius: 50%;
  filter: blur(8px);
  opacity: 0.45;
  pointer-events: none;
  z-index: 0;
  &--bubble {
    right: 4px; top: 6%;
    width: 60px; height: 60px;
    background: radial-gradient(circle, #ffb8d0, transparent 70%);
    animation: float 6s ease-in-out infinite;
  }
  &--bubble2 {
    left: 4px; bottom: 12%;
    width: 48px; height: 48px;
    background: radial-gradient(circle, #c8b2ff, transparent 70%);
    animation: float 7s ease-in-out infinite reverse;
  }
  &--bubble3 {
    right: 28px; bottom: 6%;
    width: 42px; height: 42px;
    background: radial-gradient(circle, #a8ccff, transparent 70%);
    animation: float 8s ease-in-out infinite 1s;
  }
}
@keyframes float {
  0%, 100% { transform: translateY(0); }
  50%      { transform: translateY(-10px); }
}

/* ======================================================= */
/* 右侧栏特有                                              */
/* ======================================================= */

/* 资料卡：头像 + 光环 + 在线角标 */
.peer-card {
  display: flex; flex-direction: column; align-items: center; text-align: center;
  padding: 4px 2px 0;
  position: relative;

  .peer-avatar-wrap {
    position: relative;
    width: 96px; height: 96px;
    display: flex; align-items: center; justify-content: center;
    margin-bottom: 8px;
  }
  .peer-ring {
    position: absolute; inset: 0;
    border-radius: 50%;
    pointer-events: none;
    &--outer {
      background: conic-gradient(from 0deg, #ff6b9d, #c058f2, #6ea8ff, #ff6b9d);
      opacity: 0.55;
      mask: radial-gradient(circle, transparent 41px, #000 42px);
      -webkit-mask: radial-gradient(circle, transparent 41px, #000 42px);
      animation: spin 10s linear infinite;
    }
    &--inner {
      inset: 4px;
      background: rgba(255,255,255,0.9);
      box-shadow: 0 0 0 2px rgba(255,255,255,0.9), 0 10px 28px rgba(168,85,247,0.15);
    }
  }
  &__avatar {
    position: relative; z-index: 2;
    border: 3px solid #fff;
  }
  .peer-online-badge {
    position: absolute;
    right: 2px; bottom: 4px;
    z-index: 3;
    width: 22px; height: 22px;
    border-radius: 50%;
    background: #fff;
    display: flex; align-items: center; justify-content: center;
    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
    .pob-dot {
      width: 12px; height: 12px;
      border-radius: 50%;
      background: #ccc;
      transition: all 0.3s;
    }
    &.on .pob-dot {
      background: #22c55e;
      box-shadow: 0 0 0 3px rgba(34,197,94,0.25);
      animation: pulseGreen 1.8s ease infinite;
    }
  }
  &__name { font-size: 17px; font-weight: 800; color: #2d2d3a; margin-bottom: 4px; letter-spacing: 0.01em; }
  &__status {
    display: inline-flex; align-items: center; gap: 6px;
    font-size: 12px; color: #8a8a9a;
    .dot { width: 7px; height: 7px; border-radius: 50%; background: #ccc; }
    .dot.online { background: #22c55e; box-shadow: 0 0 0 3px rgba(34,197,94,0.2); }
  }
  &__meta {
    width: 100%; margin-top: 14px;
    display: flex; flex-direction: column; gap: 8px;
  }
  .meta-item {
    display: flex; align-items: center; gap: 10px;
    font-size: 12px; color: #5d5d70;
    padding: 10px 12px;
    border-radius: 14px;
    background: rgba(255, 255, 255, 0.62);
    border: 1px solid rgba(255, 107, 157, 0.08);
    svg { color: #a855f7; flex-shrink: 0; font-size: 15px; }
    .meta-text { flex: 1; min-width: 0; text-align: left; }
    .meta-label { font-size: 10.5px; color: #9a9aaa; letter-spacing: 0.04em; }
    .meta-val   { font-size: 13px; font-weight: 700; color: #3a3048; margin-top: 1px; }
  }
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to   { transform: rotate(360deg); }
}
@keyframes pulseGreen {
  0%, 100% { box-shadow: 0 0 0 3px rgba(34,197,94,0.2); }
  50%      { box-shadow: 0 0 0 5px rgba(34,197,94,0.08); }
}

/* 匹配度卡 */
.match-card {
  padding: 14px 12px 12px;
  border-radius: 18px;
  background:
    linear-gradient(160deg, rgba(255, 220, 235, 0.5), rgba(215, 225, 255, 0.5)),
    rgba(255,255,255,0.55);
  border: 1px solid rgba(192,88,242,0.08);
  position: relative;
  /* 保证最核心的环形进度不被任何 flex/height 压缩机制挤扁：
     120(ring) + 10(head-mb) + 10(ring-mb) + 8*2+6*3(bars) + 14+12(padding-top/bot) ≈ 210 */
  min-height: 210px;
  overflow: hidden;
  &::after {
    content: '';
    position: absolute;
    right: -30px; top: -30px;
    width: 110px; height: 110px;
    background: radial-gradient(circle, rgba(255,255,255,0.5), transparent 70%);
    pointer-events: none;
  }
  .match-head {
    display: flex; align-items: center; justify-content: space-between;
    margin-bottom: 10px;
    position: relative;
    .match-title { margin: 0; }
    .match-score {
      font-size: 18px; font-weight: 900;
      background: linear-gradient(135deg, #ff6b9d, #6ea8ff);
      -webkit-background-clip: text; background-clip: text; color: transparent;
    }
  }
  .match-ring-wrap {
    position: relative;
    width: 120px; height: 120px;
    margin: 2px auto 10px;
  }
  .match-ring { width: 120px; height: 120px; display: block; }
  .match-ring-center {
    position: absolute; inset: 0;
    display: flex; flex-direction: column; align-items: center; justify-content: center;
    gap: 2px;
    .mrc-emoji { font-size: 22px; line-height: 1; animation: bob 2.5s ease-in-out infinite; }
    .mrc-text  { font-size: 11px; font-weight: 700; color: #8b39c9; letter-spacing: 0.06em; }
  }
  .match-bars {
    display: flex; flex-direction: column; gap: 8px;
    position: relative;
  }
  .mb-row {
    display: flex; align-items: center; gap: 8px;
    .mb-label { width: 52px; font-size: 11.5px; color: #6b5b7a; font-weight: 600; flex-shrink: 0; }
    .mb-bar   { flex: 1; height: 6px; border-radius: 999px; background: rgba(168,85,247,0.08); overflow: hidden; }
    .mb-fill  { height: 100%; border-radius: 999px; transition: width 0.9s cubic-bezier(0.22,1,0.36,1); }
    .mb-fill--pink   { background: linear-gradient(90deg, #ff6b9d, #ff8fb6); }
    .mb-fill--purple { background: linear-gradient(90deg, #c058f2, #a78bfa); }
    .mb-fill--blue   { background: linear-gradient(90deg, #6ea8ff, #8fd4ff); }
    .mb-num { width: 26px; font-size: 11.5px; font-weight: 700; color: #5a4668; text-align: right; flex-shrink: 0; }
  }
}

/* 标签云 */
.tags-cloud-card { padding: 2px 2px 0; }

.tags-empty {
  font-size: 11.5px;
  color: #b8a8c8;
  text-align: center;
  padding: 8px 4px;
  line-height: 1.5;
}
.cloud-wrap {
  display: flex; flex-wrap: wrap; gap: 7px;
}
.cloud-tag {
  display: inline-flex; align-items: center;
  padding: 6px 10px;
  font-size: 11.5px;
  font-weight: 600;
  border-radius: 12px;
  cursor: default;
  transition: transform 0.25s ease;
  &:hover { transform: translateY(-2px) rotate(-1deg); }
  &--0 { background: rgba(255,107,157,0.14); color: #d83f77; border: 1px dashed rgba(255,107,157,0.35); }
  &--1 { background: rgba(192,88,242,0.14); color: #8b39c9; border: 1px dashed rgba(192,88,242,0.35); }
  &--2 { background: rgba(110,168,255,0.16); color: #2f6fd6; border: 1px dashed rgba(110,168,255,0.38); }
  &--3 { background: rgba(34,197,94,0.13); color: #15803d; border: 1px dashed rgba(34,197,94,0.32); }
  &--4 { background: rgba(255,170,60,0.15); color: #c05a00; border: 1px dashed rgba(255,170,60,0.4); }
}

/* 故事统计卡 */
.story-card {
  padding: 14px 12px 12px;
  border-radius: 16px;
  background: rgba(255,255,255,0.6);
  border: 1px solid rgba(110,168,255,0.1);
}
.story-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}
.story-cell {
  position: relative;
  padding: 10px 6px 8px;
  border-radius: 14px;
  background: linear-gradient(160deg, rgba(255,230,240,0.5), rgba(225,235,255,0.5));
  text-align: center;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  &:hover { transform: translateY(-3px); box-shadow: 0 10px 22px rgba(168,85,247,0.15); }
  &.sc-highlight {
    background: linear-gradient(160deg, rgba(255,107,157,0.18), rgba(192,88,242,0.18));
    &::after {
      content: '';
      position: absolute;
      inset: 0;
      border-radius: inherit;
      box-shadow: inset 0 0 0 1px rgba(255,255,255,0.5);
      pointer-events: none;
    }
  }
  .sc-num {
    font-size: 19px; font-weight: 900;
    background: linear-gradient(135deg, #ff6b9d, #6ea8ff);
    -webkit-background-clip: text; background-clip: text; color: transparent;
    line-height: 1.1;
  }
  .sc-unit { font-size: 10.5px; color: #8b7aa0; font-weight: 600; margin-top: 2px; letter-spacing: 0.02em; }
  .sc-label { font-size: 10.5px; color: #8b7aa0; margin-top: 4px; }
}

/* 图片墙装饰 2x2 */
.photo-wall {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
  padding: 0 2px;
}
.pw-item {
  position: relative;
  aspect-ratio: 1 / 1;
  border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  overflow: hidden;
  transition: transform 0.3s ease;
  cursor: default;
  &:hover { transform: translateY(-2px) scale(1.03); }
  &--1 { background: linear-gradient(135deg, #ffd4e5 0%, #ffb4cf 100%); box-shadow: inset 0 0 0 1px rgba(255,255,255,0.5); }
  &--2 { background: linear-gradient(135deg, #dcd0ff 0%, #b8a2ff 100%); box-shadow: inset 0 0 0 1px rgba(255,255,255,0.5); }
  &--3 { background: linear-gradient(135deg, #cde4ff 0%, #9fc2ff 100%); box-shadow: inset 0 0 0 1px rgba(255,255,255,0.5); }
  &--4 { background: linear-gradient(135deg, #ffe1c2 0%, #ffc69a 100%); box-shadow: inset 0 0 0 1px rgba(255,255,255,0.5); }
  .pw-emoji { font-size: 20px; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.1)); }
}

/* 快捷动作 */
.quick-actions { display: flex; flex-direction: column; gap: 8px; }
.quick-title {
  font-size: 12.5px; font-weight: 700; color: #5a4a6a; letter-spacing: 0.02em;
  padding: 0 2px 2px;
}
.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}
.quick-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5px;
  width: 100%;
  padding: 10px 4px 9px;
  border-radius: 14px;
  border: 1px solid rgba(255, 107, 157, 0.15);
  background: rgba(255, 255, 255, 0.72);
  color: #4a3a5a;
  cursor: pointer;
  font-size: 11.5px;
  font-weight: 700;
  transition: all 0.25s ease;
  .qb-ico {
    width: 30px; height: 30px;
    display: inline-flex; align-items: center; justify-content: center;
    font-size: 16px;
    border-radius: 10px;
    background: linear-gradient(135deg, #fff0f5, #efeaff);
    transition: transform 0.25s ease;
  }
  .qb-txt { font-size: 11.5px; }
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 10px 20px rgba(168, 85, 247, 0.18);
    background: #fff;
    .qb-ico { transform: scale(1.08) rotate(-6deg); }
  }
  &--primary {
    background: linear-gradient(160deg, #ff6b9d, #a855f7);
    color: #fff;
    border-color: transparent;
    box-shadow: 0 8px 20px rgba(168, 85, 247, 0.32);
    .qb-ico { background: rgba(255,255,255,0.22); }
    &:hover { box-shadow: 0 12px 26px rgba(168, 85, 247, 0.45); }
  }
}

/* ========== 多层响应式断点 ========== */

/* 超宽屏 (>1680px)：更宽的侧边栏 */
@media (min-width: 1680px) {
  .chat-room { --pane-w: 280px; }
}

/* 大屏 (1440-1680px)：适度增加 */
@media (min-width: 1440px) and (max-width: 1679px) {
  .chat-room { --pane-w: 240px; }
}

/* 中屏 (1280-1439px)：标准尺寸 */
@media (min-width: 1280px) and (max-width: 1439px) {
  .chat-room { --pane-w: 220px; }
}

/* 小中屏 (1100-1279px)：侧边栏显著收窄 */
@media (min-width: 1100px) and (max-width: 1279px) {
  .chat-room {
    --pane-w: 170px;
    gap: 10px;
    padding: 8px;
  }
  .side-pane__scroll { padding: 10px; gap: 10px; }
  .peer-card { padding: 2px 0 0; }
  .peer-avatar-wrap { width: 84px; height: 84px; margin-bottom: 6px; }
  .peer-card__avatar { --el-avatar-size: 68px !important; }
  .peer-card__name { font-size: 15px; margin-bottom: 2px; }
  .peer-card__status { font-size: 11px; }
  .peer-card__meta { margin-top: 10px; gap: 6px; }
  .meta-item { padding: 7px 9px; gap: 8px; }
  .meta-label { font-size: 10px; }
  .meta-val { font-size: 12px; }
  .match-card { min-height: 186px; padding: 10px 10px 8px; }
  .match-head { margin-bottom: 6px; }
  .match-score { font-size: 16px; }
  .match-ring-wrap { width: 100px; height: 100px; margin: 0 auto 6px; }
  .match-ring { width: 100px; height: 100px; }
  .match-bars { gap: 6px; }
  .mb-label { width: 44px; font-size: 10.5px; }
  .mb-num { width: 22px; font-size: 10.5px; }
  .quick-grid { gap: 6px; }
  .quick-btn { padding: 8px 2px 7px; font-size: 10.5px; }
  .qb-ico { width: 26px; height: 26px; font-size: 14px; }
  .qb-txt { font-size: 10.5px; }
  .tag-chip, .cloud-tag { padding: 4px 7px; font-size: 10.5px; }
  .pane-tip { font-size: 11px; }
  .pane-mood-card { padding: 12px 10px; }
  .pane-mood-emoji { font-size: 24px; }
  .heat-card { padding: 10px 10px 8px; }
  .heat-bar { height: 6px; }
  .quote-card { padding: 14px 10px 12px; }
  .quote-text { font-size: 11.5px; line-height: 1.6; }
  .story-cell { padding: 8px 4px 6px; }
  .sc-num { font-size: 16px; }
  .photo-wall { gap: 4px; }
  .pw-item { border-radius: 10px; }
  .pw-emoji { font-size: 16px; }
}

/* 平板 (1024-1099px)：侧边栏非常紧凑 */
@media (min-width: 1024px) and (max-width: 1099px) {
  .chat-room {
    --pane-w: 140px;
    gap: 8px;
    padding: 6px;
  }
  .side-pane__scroll { padding: 8px; gap: 8px; }
  .side-pane--left { display: none; }
  .peer-card { padding: 0; }
  .peer-avatar-wrap { width: 76px; height: 76px; margin-bottom: 4px; }
  .peer-card__avatar { --el-avatar-size: 60px !important; border-width: 2px; }
  .peer-online-badge { width: 20px; height: 20px; right: 0; bottom: 2px; }
  .peer-online-badge .pob-dot { width: 10px; height: 10px; }
  .peer-card__name { font-size: 14px; margin-bottom: 2px; }
  .peer-card__status { font-size: 11px; }
  .peer-card__meta { margin-top: 8px; gap: 5px; }
  .meta-item { padding: 6px 8px; gap: 6px; }
  .meta-label { font-size: 10px; }
  .meta-val { font-size: 12px; }
  .match-card { min-height: 168px; padding: 8px 8px 6px; }
  .match-head { margin-bottom: 4px; }
  .match-score { font-size: 15px; }
  .match-ring-wrap { width: 90px; height: 90px; margin: 0 auto 4px; }
  .match-ring { width: 90px; height: 90px; }
  .match-ring-center .mrc-emoji { font-size: 18px; }
  .match-ring-center .mrc-text { font-size: 10px; }
  .match-bars { gap: 5px; }
  .mb-label { width: 38px; font-size: 10px; }
  .mb-num { width: 20px; font-size: 10px; }
  .quick-grid { grid-template-columns: repeat(2, 1fr); gap: 5px; }
  .quick-btn { padding: 7px 2px 6px; font-size: 10px; }
  .qb-ico { width: 24px; height: 24px; font-size: 13px; }
  .qb-txt { font-size: 10px; }
  .tag-chip, .cloud-tag { padding: 3px 6px; font-size: 10px; }
  .pane-tip { font-size: 10.5px; line-height: 1.4; }
  .pane-mood-emoji { font-size: 22px; }
  .heat-card { padding: 8px 8px 6px; }
  .heat-bar { height: 5px; }
  .quote-card { padding: 12px 8px 10px; }
  .quote-text { font-size: 10.5px; }
  .story-cell { padding: 6px 3px 5px; }
  .sc-num { font-size: 14px; }
  .photo-wall { grid-template-columns: repeat(2, 1fr); }
  .tips-title, .tags-title, .story-title, .match-title, .quick-title { font-size: 11px; margin-bottom: 6px; }
  .pane-tip-dot { width: 5px; height: 5px; margin-top: 5px; }
}

/* 窄屏 (<1024px)：仅保留主聊天区 */
@media (max-width: 1023px) {
  .chat-room {
    grid-template-columns: 1fr;
    padding: 8px;
    gap: 8px;
    height: calc(100vh - 60px);
  }
  .side-pane { display: none; }
  .chat-main { border-radius: 20px; }
}

/* 高屏优化 (>= 900px 高度)：更舒适的间距 */
@media (min-height: 900px) {
  .side-pane__scroll { gap: 16px; }
}

/* 矮屏 (< 720px 高度)：紧凑垂直间距 */
@media (max-height: 720px) {
  .chat-room { min-height: 440px; }
  .side-pane__scroll { gap: 8px; padding: 8px 10px 12px; }
  .pane-avatar-stack { height: 72px; }
  .pane-mood-card { padding: 10px 12px; }
  .heat-card { padding: 10px 10px 8px; }
  .quote-card { padding: 12px 10px 10px; }
  .peer-card { padding: 2px 0 0; }
  .peer-avatar-wrap { width: 80px; height: 80px; margin-bottom: 4px; }
  .peer-card__name { font-size: 15px; margin-bottom: 2px; }
  .peer-card__meta { margin-top: 8px; gap: 5px; }
  .meta-item { padding: 7px 9px; }
  .match-card { min-height: 184px; padding: 10px 10px 8px; }
  .match-head { margin-bottom: 6px; }
  .match-ring-wrap { width: 100px; height: 100px; margin: 0 auto 6px; }
  .match-ring { width: 100px; height: 100px; }
  .match-bars { gap: 6px; }
  .story-card { padding: 10px 8px 8px; }
  .photo-wall { grid-template-columns: repeat(4, 1fr); }
  .quick-actions { gap: 6px; }
  .quick-grid { gap: 6px; }
  .room-header .header-inner { height: 52px; }
  .chat-main .msg-area { padding: 12px 0 4px; }
  .chat-main .input-area { padding: 8px 16px 12px; }
  .msg-list { gap: 12px; padding: 0 16px; }
  .bubble { padding: 8px 12px; font-size: 14px; }
}

/* 超矮屏 (< 640px 高度)：极简 */
@media (max-height: 640px) {
  .side-pane__scroll { gap: 6px; padding: 6px 8px 10px; }
  .pane-avatar-stack { height: 60px; }
  .pane-mood-emoji { font-size: 22px; margin-bottom: 4px; }
  .peer-avatar-wrap { width: 72px; height: 72px; margin-bottom: 2px; }
  .peer-card__avatar { --el-avatar-size: 56px !important; border-width: 2px; }
  .peer-card__name { font-size: 14px; margin-bottom: 1px; }
  .peer-card__status { font-size: 10.5px; }
  .peer-card__meta { margin-top: 6px; gap: 4px; }
  .meta-item { padding: 5px 8px; gap: 6px; }
  .meta-label { font-size: 10px; }
  .meta-val { font-size: 11.5px; }
  .match-card { min-height: 160px; padding: 8px 8px 6px; }
  .match-head { margin-bottom: 4px; }
  .match-score { font-size: 15px; }
  .match-ring-wrap { width: 80px; height: 80px; margin: 0 auto 4px; }
  .match-ring { width: 80px; height: 80px; }
  .match-ring-center .mrc-emoji { font-size: 18px; }
  .match-ring-center .mrc-text { font-size: 10px; }
  .match-bars { gap: 5px; }
  .quick-grid { gap: 5px; }
  .quick-btn { padding: 6px 2px 5px; }
  .photo-wall { display: none; }
  .room-header .header-inner { height: 46px; }
  .chat-main .msg-area { padding: 8px 0 2px; }
  .chat-main .input-area { padding: 6px 12px 10px; }
}

/* =====================================================================
   移动端宽度断点
   策略：
   - ≤1023px 平板：三栏保留但显著紧凑化
   - ≤767px  手机横屏/大屏手机：只保留中间聊天区，左右两栏整体隐藏
   - ≤479px  手机竖屏/小屏：中间聊天区进一步紧凑，头像/气泡/输入区缩小
   ===================================================================== */

/* 平板（≤1023px） */
@media (max-width: 1023px) {
  .chat-room {
    --pane-w: 160px;
    gap: 8px;
    padding: 8px;
    min-height: auto;
    height: calc(100dvh - 54px);
  }
  .room-header .header-inner { height: 54px; padding: 0 12px; }
  .msg-list { max-width: min(920px, 96%); }
  .header-inner, .input-area { max-width: min(960px, 98%); }
  .msg-row .msg-content { max-width: 80%; }
  .bubble { padding: 10px 14px; font-size: 14.5px; }
  .bubble .msg-image { max-width: 200px; }
  .msg-avatar { --el-avatar-size: 36px !important; }
}

/* 手机（≤767px）—— 隐藏左右装饰栏，仅保留中间聊天 */
@media (max-width: 767px) {
  .chat-room {
    /* 单列：中间聊天区吃满整个宽度 */
    display: flex !important;
    flex-direction: column;
    height: calc(100dvh - 54px);
    min-height: 0;
    padding: 6px !important;
    gap: 0 !important;
    background: transparent;
  }

  /* 左右侧边栏彻底隐藏（不占 grid 空间） */
  .side-pane--left, .side-pane--right { display: none !important; }

  .chat-main.glass-card {
    width: 100%;
    margin: 0;
    border-radius: 16px;
  }

  .room-header .header-inner { height: 54px; padding: 0 10px; }
  .back-btn, .more-btn {
    width: 36px; height: 36px;
  }
  .peer-avatar { --el-avatar-size: 34px !important; }
  .peer-name { font-size: 15px; }

  .chat-main .msg-area {
    padding: 10px 0 4px;
  }
  .msg-list {
    max-width: 100%;
    padding: 0 0;
    gap: 14px;
  }
  .msg-row { gap: 8px; }
  .msg-row .msg-avatar { --el-avatar-size: 34px !important; }
  .msg-row .msg-content { max-width: 84%; }
  .bubble { padding: 9px 14px; font-size: 14px; border-radius: 16px; }
  .bubble .msg-image { max-width: 180px; }
  .msg-time { font-size: 10.5px; }

  .chat-main .input-area {
    padding: 8px 12px 10px;
  }
  .input-wrap {
    border-radius: 20px;
    padding: 8px 42px 8px 14px;
    min-height: 44px;
    font-size: 14px;
  }
  .input-actions { left: 10px; }
  .send-btn {
    width: 36px; height: 36px;
    border-radius: 10px;
    right: 6px;
    bottom: 50%;
    transform: translateY(50%);
    .el-icon { font-size: 17px; }
  }
}

/* 小屏手机竖屏（≤479px）—— 极致紧凑 */
@media (max-width: 479px) {
  .chat-room {
    height: calc(100dvh - 50px);
    padding: 4px !important;
  }
  .chat-main.glass-card { border-radius: 12px; }
  .room-header .header-inner {
    height: 50px;
    padding: 0 8px;
  }
  .back-btn, .more-btn { width: 32px; height: 32px; }
  .back-btn .el-icon, .more-btn .el-icon { font-size: 16px; }
  .peer-avatar { --el-avatar-size: 32px !important; }
  .peer-name { font-size: 14px; }
  .peer-status { font-size: 10.5px; }

  .msg-row { gap: 6px; }
  .msg-row .msg-avatar { --el-avatar-size: 32px !important; }
  .msg-row .msg-content { max-width: 86%; }
  .bubble {
    padding: 8px 12px;
    font-size: 13.5px;
    border-radius: 14px;
    border-top-left-radius: 4px;
    &.is-mine { border-top-right-radius: 4px; border-top-left-radius: 14px; }
  }
  .bubble .msg-image { max-width: 160px; border-radius: 10px; }
  .msg-time { font-size: 10px; }

  .chat-main .input-area { padding: 6px 10px 10px; }
  .input-wrap {
    padding: 6px 38px 6px 12px;
    min-height: 40px;
    font-size: 13.5px;
    border-radius: 18px;
  }
  .send-btn {
    width: 32px; height: 32px;
    border-radius: 9px;
    .el-icon { font-size: 15px; }
  }
  .attach-btn {
    width: 30px; height: 30px;
    .el-icon { font-size: 17px; }
  }
}
</style>

<!-- 非 scoped 全局样式：右键菜单 Teleport 到 body 后，scoped 属性选择器不生效 -->
<style lang="scss">
.msg-menu {
  position: fixed;
  z-index: 9999;
  min-width: 180px;
  padding: 6px;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid rgba(168, 85, 247, 0.15);
  border-radius: 14px;
  box-shadow: 0 16px 40px rgba(30, 20, 60, 0.15), 0 2px 6px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
  gap: 2px;
  animation: msgMenuIn 0.16s ease both;
}
@keyframes msgMenuIn {
  from { opacity: 0; transform: translateY(-4px) scale(0.98); }
  to   { opacity: 1; transform: none; }
}
.msg-menu .mm-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 8px 12px;
  font-size: 13px;
  font-weight: 600;
  color: #4a3a5a;
  background: transparent;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  text-align: left;
  transition: background 0.15s ease;
}
.msg-menu .mm-btn svg {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}
.msg-menu .mm-btn:hover {
  background: rgba(168, 85, 247, 0.08);
}
.msg-menu .mm-btn.mm-btn--danger {
  color: #d93c5e;
}
.msg-menu .mm-btn.mm-btn--danger:hover {
  background: rgba(244, 63, 94, 0.08);
}
.msg-menu .mm-btn.mm-btn--disabled,
.msg-menu .mm-btn:disabled {
  color: #b8a8c8;
  cursor: not-allowed;
  background: transparent !important;
  opacity: 0.75;
}
.msg-menu .mm-btn.mm-btn--disabled svg,
.msg-menu .mm-btn:disabled svg {
  opacity: 0.85;
}
.msg-menu .mm-empty {
  padding: 10px 12px;
  font-size: 12px;
  color: #b8a8c8;
  text-align: center;
  font-weight: 500;
  letter-spacing: 0.02em;
}

/* ============================================================ */
/*  主题皮肤选择面板（Teleport）                                  */
/* ============================================================ */
.mf-panel-fade-enter-active,
.mf-panel-fade-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}
.mf-panel-fade-enter-from,
.mf-panel-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px) scale(0.98);
}

.mf-theme-panel {
  position: fixed;
  z-index: 9999;
  width: 380px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(18px) saturate(1.3);
  -webkit-backdrop-filter: blur(18px) saturate(1.3);
  border: 1px solid rgba(168, 85, 247, 0.18);
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(30, 20, 60, 0.18), 0 4px 12px rgba(0, 0, 0, 0.06);
  animation: msgMenuIn 0.2s ease both;
}
.mf-panel-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 4px 6px 12px;
  border-bottom: 1px solid rgba(168, 85, 247, 0.12);
  margin-bottom: 12px;
}
.mf-panel-title {
  font-size: 16px;
  font-weight: 700;
  color: #3a2b4a;
  letter-spacing: 0.01em;
}
.mf-panel-sub {
  font-size: 12px;
  color: #a59ab3;
  font-weight: 500;
}
.mf-theme-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  max-height: 380px;
  overflow-y: auto;
  padding-right: 4px;
}
.mf-theme-grid::-webkit-scrollbar { width: 6px; }
.mf-theme-grid::-webkit-scrollbar-thumb { background: rgba(168,85,247,0.2); border-radius: 999px; }
.mf-theme-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px;
  background: #fff;
  border: 1.5px solid transparent;
  border-radius: 16px;
  cursor: pointer;
  text-align: left;
  transition: all 0.2s ease;
  font-family: inherit;
}
.mf-theme-card:hover {
  transform: translateY(-2px);
  border-color: rgba(168, 85, 247, 0.3);
  box-shadow: 0 8px 22px rgba(168, 85, 247, 0.15);
}
.mf-theme-card.active {
  border-color: #a855f7;
  background: linear-gradient(180deg, #fff, #faf5ff);
  box-shadow: 0 10px 28px rgba(168, 85, 247, 0.22);
}
.mf-theme-preview {
  position: relative;
  height: 80px;
  border-radius: 12px;
  overflow: hidden;
  padding: 10px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.6);
}
.mf-preview-bubble {
  max-width: 60%;
  padding: 4px 10px;
  border-radius: 10px;
  font-size: 11px;
  line-height: 1.3;
  font-weight: 600;
}
.mf-preview-bubble.mf-preview-left {
  align-self: flex-start;
  border-top-left-radius: 2px;
  box-shadow: 0 2px 6px rgba(0,0,0,0.08);
}
.mf-preview-bubble.mf-preview-right {
  align-self: flex-end;
  border-top-right-radius: 2px;
  box-shadow: 0 2px 6px rgba(0,0,0,0.12);
}
.mf-theme-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 0 2px 2px;
}
.mf-theme-name {
  font-size: 13px;
  font-weight: 700;
  color: #4a3a5a;
}
.mf-theme-desc {
  font-size: 11px;
  color: #9a90a8;
}
.mf-theme-check {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: linear-gradient(135deg, #a855f7, #ec4899);
  color: #fff;
  font-size: 12px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 10px rgba(168, 85, 247, 0.4);
}

/* ============================================================ */
/*  表情表面板（Teleport）                                        */
/* ============================================================ */
.mf-emoji-panel {
  position: fixed;
  z-index: 9999;
  width: 420px;
  max-width: calc(100vw - 20px);
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(18px) saturate(1.3);
  -webkit-backdrop-filter: blur(18px) saturate(1.3);
  border: 1px solid rgba(168, 85, 247, 0.18);
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(30, 20, 60, 0.18), 0 4px 12px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  animation: msgMenuIn 0.2s ease both;
}
.mf-emoji-tabs {
  display: flex;
  align-items: center;
  padding: 6px 10px;
  gap: 2px;
  border-bottom: 1px solid rgba(168, 85, 247, 0.12);
  background: linear-gradient(180deg, #faf5ff, transparent);
  flex-shrink: 0;
}
.mf-emoji-tab {
  width: 38px;
  height: 36px;
  border: none;
  background: transparent;
  border-radius: 10px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
  font-size: 18px;
  line-height: 1;
  padding: 0;
}
.mf-emoji-tab span { line-height: 1; }
.mf-emoji-tab:hover { background: rgba(168, 85, 247, 0.08); transform: translateY(-1px); }
.mf-emoji-tab.active {
  background: linear-gradient(135deg, rgba(168, 85, 247, 0.18), rgba(236, 72, 153, 0.18));
  box-shadow: inset 0 0 0 1.5px rgba(168, 85, 247, 0.4);
}
.mf-emoji-grid {
  flex: 1 1 auto;
  overflow-y: auto;
  padding: 8px 10px 4px;
  display: grid;
  grid-template-columns: repeat(9, 1fr);
  gap: 2px;
  min-height: 0;
  max-height: 270px;
}
.mf-emoji-grid::-webkit-scrollbar { width: 6px; }
.mf-emoji-grid::-webkit-scrollbar-thumb { background: rgba(168,85,247,0.2); border-radius: 999px; }
.mf-emoji-cell {
  border: none;
  background: transparent;
  width: 100%;
  aspect-ratio: 1 / 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  line-height: 1;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.12s ease;
  padding: 0;
}
.mf-emoji-cell:hover {
  background: var(--theme-primary-soft, rgba(168, 85, 247, 0.12));
  transform: scale(1.18);
}
.mf-emoji-empty {
  grid-column: 1 / -1;
  padding: 28px 10px;
  text-align: center;
  color: #b0a5be;
  font-size: 13px;
  font-weight: 500;
}
.mf-emoji-footer {
  flex-shrink: 0;
  padding: 8px 14px 12px;
  font-size: 11px;
  color: #a59ab3;
  text-align: center;
  border-top: 1px solid rgba(168, 85, 247, 0.10);
  background: linear-gradient(180deg, transparent, #faf5ff);
  letter-spacing: 0.02em;
}

/* 手机端响应式 */
@media (max-width: 767px) {
  .mf-theme-panel {
    width: calc(100vw - 24px) !important;
    max-width: 360px;
    left: 12px !important;
    right: 12px !important;
  }
  .mf-emoji-panel {
    width: calc(100vw - 20px) !important;
    left: 10px !important;
    right: 10px !important;
  }
  .mf-emoji-grid {
    grid-template-columns: repeat(8, 1fr);
    max-height: 230px;
  }
}
@media (max-width: 479px) {
  .mf-theme-grid { grid-template-columns: 1fr; }
  .mf-emoji-grid { grid-template-columns: repeat(7, 1fr); max-height: 200px; }
  .mf-emoji-cell { font-size: 20px; }
}
</style>
