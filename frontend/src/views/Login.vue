<script setup lang="ts">
import { ref } from 'vue'
import { useAppStore } from '@/stores/app'
import UiIcon from '@/components/ui/UiIcon.vue'

defineOptions({ name: 'LoginView' })

const store = useAppStore()

const username = ref('admin')
const password = ref('123456')
const remember = ref(true)
const loading = ref(false)

const features = [
  { icon: 'calendar', title: '智能排课', desc: '按教师、教室、课程多维排课，自动检测资源冲突' },
  { icon: 'clipboard-check', title: '考勤管理', desc: '课堂考勤一键录入，缺勤迟到实时统计' },
  { icon: 'bar-chart', title: '数据报表', desc: '营收、学员、课消多维度可视化分析' },
]

function submit() {
  if (!username.value.trim() || !password.value.trim()) {
    store.toast('请输入账号和密码', 'warning')
    return
  }
  loading.value = true
  setTimeout(() => {
    store.login(username.value.trim())
    store.toast('登录成功，欢迎回来！')
  }, 600)
}
</script>

<template>
  <div class="login">
    <div class="login__brand">
      <div class="login__brand-inner">
        <div class="login__logo">
          <UiIcon name="graduation-cap" :size="28" />
        </div>
        <h1 class="login__title">智慧教培教务系统</h1>
        <p class="login__subtitle">面向教培机构的一体化教务管理平台</p>

        <ul class="login__features">
          <li v-for="f in features" :key="f.title" class="login__feature">
            <span class="login__feature-icon">
              <UiIcon :name="f.icon" :size="20" />
            </span>
            <div>
              <p class="login__feature-title">{{ f.title }}</p>
              <p class="login__feature-desc">{{ f.desc }}</p>
            </div>
          </li>
        </ul>
      </div>
      <div class="login__brand-deco login__brand-deco--1" />
      <div class="login__brand-deco login__brand-deco--2" />
    </div>

    <div class="login__panel">
      <div class="login__form-wrap">
        <h2 class="login__form-title">欢迎登录</h2>
        <p class="login__form-sub">请输入您的账号信息</p>

        <form class="login__form" @submit.prevent="submit">
          <div class="login__field">
            <label class="login__label">账号</label>
            <div class="login__input">
              <UiIcon name="user" :size="18" />
              <input v-model="username" type="text" placeholder="请输入账号" />
            </div>
          </div>

          <div class="login__field">
            <label class="login__label">密码</label>
            <div class="login__input">
              <UiIcon name="lock" :size="18" />
              <input v-model="password" type="password" placeholder="请输入密码" />
            </div>
          </div>

          <div class="login__row">
            <label class="login__remember">
              <input v-model="remember" type="checkbox" />
              <span>记住我</span>
            </label>
            <a class="login__link" href="#" @click.prevent>忘记密码？</a>
          </div>

          <button class="login__submit" type="submit" :disabled="loading">
            {{ loading ? '登录中…' : '登 录' }}
          </button>
        </form>

        <div class="login__demo">
          <p>演示账号：<b>admin</b>　密码：<b>123456</b></p>
          <p class="login__demo-tip">（任意账号密码均可登录体验）</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login {
  display: grid;
  grid-template-columns: 1fr 1fr;
  min-height: 100vh;
}

/* ---- 左侧品牌区 ---- */
.login__brand {
  position: relative;
  display: flex;
  align-items: center;
  padding: 60px;
  background: linear-gradient(140deg, #1e1b4b 0%, #312e81 55%, #4338ca 100%);
  color: #fff;
  overflow: hidden;
}

.login__brand-inner {
  position: relative;
  z-index: 2;
  max-width: 480px;
}

.login__logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.2);
  margin-bottom: 28px;
}

.login__title {
  font-size: 32px;
  font-weight: 700;
  letter-spacing: 0.01em;
}

.login__subtitle {
  margin-top: 10px;
  font-size: 15px;
  color: #c7d2fe;
}

.login__features {
  margin-top: 48px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.login__feature {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.login__feature-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.1);
  color: #a5b4fc;
  flex-shrink: 0;
}

.login__feature-title {
  font-size: 15px;
  font-weight: 600;
}

.login__feature-desc {
  margin-top: 3px;
  font-size: 13px;
  color: #a5b4fc;
}

.login__brand-deco {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.06);
}

.login__brand-deco--1 {
  width: 420px;
  height: 420px;
  right: -140px;
  bottom: -120px;
}

.login__brand-deco--2 {
  width: 240px;
  height: 240px;
  right: 60px;
  top: -80px;
}

/* ---- 右侧表单区 ---- */
.login__panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: #fff;
}

.login__form-wrap {
  width: 100%;
  max-width: 380px;
}

.login__form-title {
  font-size: 26px;
  font-weight: 700;
  color: var(--gray-900);
}

.login__form-sub {
  margin-top: 8px;
  font-size: 14px;
  color: var(--gray-500);
}

.login__form {
  margin-top: 32px;
}

.login__field {
  margin-bottom: 20px;
}

.login__label {
  display: block;
  margin-bottom: 8px;
  font-size: 13.5px;
  font-weight: 500;
  color: var(--gray-700);
}

.login__input {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 46px;
  padding: 0 14px;
  border: 1px solid var(--gray-200);
  border-radius: var(--radius-md);
  color: var(--gray-400);
  transition:
    border-color var(--transition),
    box-shadow var(--transition);
}

.login__input:focus-within {
  border-color: var(--brand-500);
  box-shadow: 0 0 0 3px var(--brand-100);
}

.login__input input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 14px;
  color: var(--gray-800);
}

.login__input input:focus {
  outline: none;
}

.login__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.login__remember {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13.5px;
  color: var(--gray-600);
  cursor: pointer;
}

.login__remember input {
  accent-color: var(--brand-600);
  width: 15px;
  height: 15px;
}

.login__link {
  font-size: 13.5px;
  color: var(--brand-600);
}

.login__submit {
  width: 100%;
  height: 46px;
  border-radius: var(--radius-md);
  background: var(--brand-600);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.2em;
  transition:
    background-color var(--transition),
    box-shadow var(--transition);
}

.login__submit:hover:not(:disabled) {
  background: var(--brand-700);
  box-shadow: 0 8px 20px rgba(79, 70, 229, 0.3);
}

.login__submit:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.login__demo {
  margin-top: 28px;
  padding: 14px 16px;
  border-radius: var(--radius-md);
  background: var(--brand-50);
  font-size: 13px;
  color: var(--brand-700);
  text-align: center;
}

.login__demo-tip {
  margin-top: 2px;
  font-size: 12px;
  color: var(--brand-400);
}

@media (max-width: 900px) {
  .login {
    grid-template-columns: 1fr;
  }

  .login__brand {
    display: none;
  }
}
</style>
