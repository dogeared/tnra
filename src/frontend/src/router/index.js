import Vue from 'vue'
import Router from 'vue-router'
import SuiVue from 'semantic-ui-vue'
import 'semantic-ui-css/semantic.min.css'

import HomeComponent from '@/components/Home'
import ProfileComponent from '@/components/Profile'

import { OktaAuth } from '@okta/okta-auth-js'
import OktaVue, { LoginCallback } from '@okta/okta-vue'
import authConfig from '@/config'

Vue.use(Router)
Vue.use(SuiVue)

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
      path: '/profile',
      component: ProfileComponent,
      meta: {
        requiresAuth: true
      }
    }
  ]
})

export default router