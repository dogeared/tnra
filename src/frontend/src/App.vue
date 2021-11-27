<template>
  <div id="app">
    <div class="ui inverted top fixed menu">
      <div class="ui text container">
        <router-link
          to="/"
          class="header item"
        >
          <img
            class="ui mini image"
            src="./assets/logo.png"
          >
          &nbsp;
          TNRA
        </router-link>
        <a
          class="item"
          v-if="!authState.isAuthenticated"
          v-on:click="login()"
        >
        Login
        </a>
        <router-link
          to="/post"
          class="item"
          id="post-button"
          v-if="authState.isAuthenticated"
        >
          Post
        </router-link>
        <router-link
          to="/stats"
          class="item"
          id="post-button"
          v-if="authState.isAuthenticated"
        >
          PQ Stats
        </router-link>
        <router-link
          to="/profile"
          class="item"
          id="profile-button"
          v-if="authState.isAuthenticated"
        >
        Profile
        </router-link>
        <a
          id="logout-button"
          class="item"
          v-if="authState.isAuthenticated"
          v-on:click="logout()"
        >
        Logout
        </a>
      </div>
    </div>
    <div
      class="ui text container"
      style="margin-top: 7em;max-width: 1024px!important;"
    >
      <sui-modal v-model="open">
        <sui-modal-header>Session Warning</sui-modal-header>
        <sui-modal-content>
          <sui-modal-description>
            <sui-header>Your session will expire soon</sui-header>
            <p>
              When the session expires, you will be logged out.
              You will not lose any work. You can logout now and
              login to continue where you left off.
            </p>
          </sui-modal-description>
        </sui-modal-content>
        <sui-modal-actions>
          <sui-button positive @click.native="closeSessionModal()">
            OK
          </sui-button>
        </sui-modal-actions>
      </sui-modal>
      <router-view/>
    </div>
  </div>
</template>

<script>
import store from "@/store";

export default {
  name: 'app',
  data() {
    return {
      open: false,
      interval: undefined
    }
  },
  async mounted() {
    await this.sessionCheckStart()
  },
  methods: {
    closeSessionModal () {
      this.open = false
    },
    login () {
      this.$auth.signInWithRedirect('/')
    },
    async logout () {
      clearInterval(this.interval)
      await this.$auth.signOut()
    },
    async sessionCheckStart () {
      // TODO - make this event driven
      this.interval = setInterval(async function () {
        let token = await this.$auth.tokenManager.get('accessToken')
        if (token && token.claims && !store.state.sessionExpiresAt) {
          store.commit('setSessionExpiresAt', new Date(token.claims.exp*1000))
        }

        if (store.state.sessionExpiresAt) {
          console.log(`Session expires at: ${store.state.sessionExpiresAt}`)
        }
        await store.dispatch('sessionWarnCheck')
        if (store.state.sessionWarning && !store.state.sessionWarningSeen) {
          store.commit('setSessionWarningSeen', true)
          this.open = true
        }
        if (
            store.state.sessionExpiresAt &&
            store.state.sessionExpiresAt.getTime() - new Date().getTime() < 1000*20
        ) {
          this.logout()
        }
      }.bind(this), 30000)
    }
  }
}
</script>
<style>
.wide-container {
  max-width: 1024px!important;
}
</style>