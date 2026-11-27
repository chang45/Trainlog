import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import AppLayout from '../components/AppLayout.vue'
import DashboardView from '../views/DashboardView.vue'
import PlanView from '../views/PlanView.vue'
import RecordsView from '../views/RecordsView.vue'
import StatsView from '../views/StatsView.vue'
import DietView from '../views/DietView.vue'
import { hasToken } from '../api/http'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { guestOnly: true } },
    { path: '/register', name: 'register', component: RegisterView, meta: { guestOnly: true } },
    {
      path: '/',
      component: AppLayout,
      meta: { requiresAuth: true },
      children: [
        { path: '', name: 'dashboard', component: DashboardView },
        { path: 'plan', name: 'plan', component: PlanView },
        { path: 'records', name: 'records', component: RecordsView },
        { path: 'diet', name: 'diet', component: DietView },
        { path: 'stats', name: 'stats', component: StatsView }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

router.beforeEach((to) => {
  const loggedIn = hasToken()
  if (to.meta.requiresAuth && !loggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guestOnly && loggedIn) {
    return { name: 'dashboard' }
  }
  return true
})

export default router
