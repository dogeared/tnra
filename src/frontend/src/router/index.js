import Vue from 'vue'
import Router from 'vue-router'

import HomeComponent from '@/components/Home'
import ProfileComponent from '@/components/Profile'
import PostComponent from '@/components/Post'

import { OktaAuth } from '@okta/okta-auth-js'
import OktaVue, { LoginCallback } from '@okta/okta-vue'
import authConfig from '@/config'

Vue.use(Router)

const oktaAuth = new OktaAuth(authConfig.oidc)

Vue.use(OktaVue, { oktaAuth })

const router = new Router({
  mode: 'history',
  routes: [
    {
      // handles OAuth callback
      path: '/login/callback',
      component: LoginCallback
    },
    {
      path: '/',
      component: HomeComponent
    },
    {
      path: '/post',
      component: PostComponent,
      meta: {
        requiresAuth: true
      }
    },
    {
      path: '/profile',
      component: ProfileComponent,
      meta: {
        requiresAuth: true
      }
    }
  ]
})

export default router
