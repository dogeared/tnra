import Vue from 'vue'
import SuiVue from 'semantic-ui-vue'
import 'semantic-ui-css/semantic.min.css'
import "vue-easytable/libs/theme-default/index.css"
import VueEasytable from "vue-easytable"

import '@/polyfills'
import App from '@/App'
import router from '@/router'

Vue.config.productionTip = false
Vue.use(SuiVue)
Vue.use(VueEasytable)

import store from '@/store'

/* eslint-disable no-new */
new Vue({
  el: '#app',
  router,
  template: '<App/>',
  components: { App },
  store
})
