import { createRouter, createWebHistory } from 'vue-router'
import LicenceView from '../views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'licence',
      component: LicenceView,
    },
  ],
})

export default router
