<template lang="html">
  <div class="gtg">
    <h1 class="ui header">
      <sui-icon name="address book outline"/>
      GTG - {{ new Date(gtg.startDate).toLocaleDateString() }}
    </h1>
    <sui-table basic="very" celled collapsing>
      <sui-table-header>
        <sui-table-row>
          <sui-table-header-cell>Name</sui-table-header-cell>
          <sui-table-header-cell>Calls</sui-table-header-cell>
        </sui-table-row>
      </sui-table-header>
      <sui-table-body>
        <sui-table-row v-for="(row, index) in gtg.goToGuyPairs" :key="index">
          <sui-table-cell>
            <h4 is="sui-header" image>
              <sui-image
                  :src="'static/images/' + row.caller.profileImage"
                  shape="rounded"
                  size="large"
              />
              <sui-header-content>
                {{ row.caller.firstName }}
                <sui-header-subheader>{{ formatPhoneNumber(row.caller.phoneNumber) }}</sui-header-subheader>
              </sui-header-content>
            </h4>
          </sui-table-cell>
          <sui-table-cell>
            <h4 is="sui-header" image>
              <sui-image
                  :src="'static/images/' + row.callee.profileImage"
                  shape="rounded"
                  size="large"
              />
              <sui-header-content>
                {{ row.callee.firstName }}
                <sui-header-subheader>{{ formatPhoneNumber(row.callee.phoneNumber) }}</sui-header-subheader>
              </sui-header-content>
            </h4>
          </sui-table-cell>
        </sui-table-row>
      </sui-table-body>
    </sui-table>
  </div>
</template>
<script>
export default {
  name: 'gtg',
  methods: {
    authConfig() {
      const accessToken = this.$auth.getAccessToken()
      return { headers: { Authorization: `Bearer ${accessToken}` } }
    },
    formatPhoneNumber(phoneNumber) {
      return phoneNumber.slice(0,3) + '-' + phoneNumber.slice(3,6) + '-' + phoneNumber.slice(6)
    }
  },
  computed: {
    gtg() {
        return this.$store.state.gtg
    }
  },
  async beforeMount() {
    var response = await this.$store.dispatch('getLatestGTG', { authHeader: this.authConfig() });
    response.data.goToGuyPairs =
        response.data.goToGuyPairs.sort((a, b) => (a.caller.firstName > b.caller.firstName)?1:-1)
    this.$store.commit('setGTG', response.data)
  }
}
</script>