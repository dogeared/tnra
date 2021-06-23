// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import Vuex from 'vuex'
import SuiVue from 'semantic-ui-vue'
import _ from 'lodash';
import 'semantic-ui-css/semantic.min.css'

import '@/polyfills'
import App from '@/App'
import router from '@/router'
import config from '@/config'

import axios from 'axios';

Vue.config.productionTip = false
Vue.use(Vuex)
Vue.use(SuiVue)

const store = new Vuex.Store({
  state: {
    completedPost: {
      'id': undefined,
      'start': undefined,
      'finish': undefined,
      'state': undefined,
      'intro': {
        'widwytk': undefined,
        'kryptonite': undefined,
        'whatAndWhen': undefined
      },
      'personal': {
        'best': undefined,
        'worst': undefined
      },
      'family': {
        'best': undefined,
        'worst': undefined
      },
      'work': {
        'best': undefined,
        'worst': undefined
      },
      'stats': {
        'exercise': undefined,
        'gtg': undefined,
        'meditate': undefined,
        'meetings': undefined,
        'pray': undefined,
        'read': undefined,
        'sponsor': undefined
      }
    },
    inProgressPost: {}
  },
  getters: {
    getProperty: (state) =>  (name, key) => {
      return _.get(state[name], key)
    }
  },
  mutations: {
    patchInProgressPost: (state, payload) => {
      _.set(state.inProgressPost, payload.key, payload.value)
    },
    patchCompletePost: (state, payload) => {
      _.set(state.completedPost, payload.key, payload.value)
    },
    setCompletedPost: (state, post) => {
      state.completedPost = post
    }
  },
  actions: {
    getLastestCompletedPost: ({ commit }, payload) => {
      axios.get(config.resourceServer.my_last_post, payload.authHeader)
        .then((response) => {
          commit('setCompletedPost', response.data)
        })
    }
  }
})

/* eslint-disable no-new */
new Vue({
  el: '#app',
  router,
  template: '<App/>',
  components: { App },
  store
})
