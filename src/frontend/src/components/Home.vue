<template>
  <div id="home">
    <h1 class="ui header">Taking the Next Right Action</h1>
    <div v-if="!authState.isAuthenticated">
      <sui-button primary animated v-on:click="login()">
        <sui-button-content visible>Login</sui-button-content>
        <sui-button-content hidden>
          <sui-icon name="right arrow" />
        </sui-button-content>
      </sui-button>
    </div>

    <div v-if="authState.isAuthenticated">
      <p>Welcome back, {{claims && claims.name}}!</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'home',
  data: function () {
    return {
      claims: ''
    }
  },
  mounted () { this.setup() },
  methods: {
    setup () {
      if (this.authState.isAuthenticated) {
        this.$auth.getUser().then(claims => this.claims = claims)
      }
    },
    login () {
      this.$auth.signInWithRedirect('/')
    }
  }
}
</script>