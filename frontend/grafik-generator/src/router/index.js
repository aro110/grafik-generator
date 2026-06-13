import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/DashboardView.vue'
import CreatorView from '../views/CreatorView.vue'
import ScheduleView from '../views/ScheduleView.vue'
import StoreSummaryView from '../views/StoreSummaryView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'dashboard',
      component: DashboardView,
    },
    {
      path: '/create',
      name: 'create',
      component: CreatorView,
    },
    {
      path: '/schedule/:id',
      name: 'schedule',
      component: ScheduleView,
    },
    {
      path: '/summary/:id',
      name: 'summary',
      component: StoreSummaryView,
    },
  ],
})

export default router
