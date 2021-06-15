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
      <router-view/>
    </div>
  </div>
</template>

<script>
export default {
  name: 'app',
  methods: {
    login () {
      this.$auth.signInWithRedirect('/')
    },
    async logout () {
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