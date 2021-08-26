import Router from 'vue-router'
import Home from './views/Home.vue'

export default new Router({
  mode: 'history',
  base: process.env.BASE_URL,
  routes: [
    {
      path: '/app2',
      name: 'home',
      component: Home
    },
    {
      path: '/app2/about',
      name: 'about',
      component: () => import('./views/About.vue')
    }
  ]
})
