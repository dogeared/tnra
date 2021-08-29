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
export default {
  name: 'app',
  data() {
    return {
      open: false,
      interval: undefined
    }
  },
  mounted() {
    this.interval = setInterval(async function () {
      console.log(`Session expires at: ${this.$store.state.sessionExpiresAt}`)
      this.$store.dispatch('sessionWarnCheck')
      if (this.$store.state.sessionWarning && !this.$store.state.sessionWarningSeen) {
        this.$store.commit('setSessionWarningSeen', true)
        this.open = true
      }
      if (
          this.$store.state.sessionExpiresAt &&
          this.$store.state.sessionExpiresAt.getTime() - new Date().getTime() < 1000*20
      ) {
        this.logout()
      }
    }.bind(this), 30000)
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
    }
  }
}
</script>
<style>
.wide-container {
  max-width: 1024px!important;
}
</style>