import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/home/HomeView.vue'),
        meta: { title: '首页', requireAuth: true }
      },
      {
        path: 'dynamic',
        name: 'DynamicSquare',
        component: () => import('@/views/dynamic/DynamicSquare.vue'),
        meta: { title: '动态广场', requireAuth: true }
      },
      {
        path: 'dynamic/publish',
        name: 'Publish',
        component: () => import('@/views/dynamic/PublishView.vue'),
        meta: { title: '发布动态', requireAuth: true }
      },
      {
        path: 'dynamic/:id',
        name: 'DynamicDetail',
        component: () => import('@/views/dynamic/DynamicDetail.vue'),
        meta: { title: '动态详情', requireAuth: true }
      },
      {
        path: 'chat',
        name: 'ChatList',
        component: () => import('@/views/chat/ChatListView.vue'),
        meta: { title: '消息', requireAuth: true }
      },
      {
        path: 'chat/:sessionId',
        name: 'ChatRoom',
        component: () => import('@/views/chat/ChatRoomView.vue'),
        meta: { title: '聊天室', requireAuth: true }
      },
      {
        path: 'match',
        name: 'MatchList',
        component: () => import('@/views/user/MatchListView.vue'),
        meta: { title: '匹配', requireAuth: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/user/ProfileView.vue'),
        meta: { title: '我的主页', requireAuth: true }
      },
      {
        path: 'profile/edit',
        name: 'EditProfile',
        component: () => import('@/views/user/EditProfileView.vue'),
        meta: { title: '编辑资料', requireAuth: true }
      },
      {
        path: 'user/:id',
        name: 'UserDetail',
        component: () => import('@/views/user/UserDetailView.vue'),
        meta: { title: '用户主页', requireAuth: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/home'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

// 全局前置守卫
router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} · 搭伴` : '搭伴'
  const userStore = useUserStore()
  if (to.meta.requireAuth && !userStore.isLogin) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else if ((to.path === '/login' || to.path === '/register') && userStore.isLogin) {
    next('/home')
  } else {
    next()
  }
})

export default router
