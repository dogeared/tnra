import Vue from 'vue'
import Router from 'vue-router'

import HomeComponent from '@/components/Home'
import ProfileComponent from '@/components/Profile'
import PostComponent from '@/components/Post'

import { OktaAuth } from '@okta/okta-auth-js'
import OktaVue, { LoginCallback } from '@okta/okta-vue'
import authConfig from '@/config'
import store from '@/store'

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

const onAuthRequired = async (from, to, next) => {
  if (!from.matched.some(record => record.meta.requiresAuth)) {
    next()
    return
  }

  Vue.prototype.$auth.session.get()
    .then(session => {
      if (!store.state.sessionExpiresAt) {
        store.commit('setSessionExpiresAt', new Date(session.expiresAt))
      }
      console.log(store.state.sessionExpiresAt)
    })
    .catch(err => {
      // TODO handle error
      store.commit('setSessionExpiresAt', new Date())
      console.log(err);
    })
  next()
}

router.beforeEach(onAuthRequired)

export default router
