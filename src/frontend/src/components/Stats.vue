<template lang="html">
  <div class="stats">
    <h1 class="ui header">
      <sui-icon name="chart line"/>
      PQ Stats
    </h1>
    <div v-if="!isAuthenticated">
      <h3 v-if="authError" class="ui red header">Invalid email or password. Try again.</h3>
      <p>You need to authenticate to PQ. You only need to do this once.</p>
      <p>Your password is NEVER saved in this app.</p>
      <sui-form id="pq-credentials-form" @submit.prevent="processPQ">
        <sui-form-field>
          <label>Credentials</label>
          <sui-form-fields fields="two">
            <sui-form-field>
              <input
                  type="text"
                  name="pq_email"
                  placeholder="Email used with PQ app"
                  v-model="pqEmail"
              />
            </sui-form-field>
            <sui-form-field>
              <input
                  type="password"
                  name="pq_password"
                  placeholder="Password used with PQ app"
                  v-model="pqPassword"
              />
            </sui-form-field>
          </sui-form-fields>
        </sui-form-field>
        <sui-button type="submit">Submit PQ Credentials</sui-button>
      </sui-form>
    </div>
    <table class="ui table" v-if="isAuthenticated">
      <thead>
      <tr>
        <th>Name</th>
        <th>Charge</th>
        <th>Muscle</th>
        <th>Reps Today</th>
      </tr>
      </thead>
      <tbody>
      <tr
          v-for="(value, key) in stats"
          :key="key"
      >
        <td>{{key}}</td>
        <td>{{calculateCharge(value.pq.charge, value.pq.updated_at)}}</td>
        <td>{{Number(value.pq.muscle).toFixed(0)}}</td>
        <td>{{value.pq.reps_day}}</td>
      </tr>
      </tbody>
    </table>

  </div>
</template>
<script>
import config from '@/config';
import axios from 'axios';

export default {
  name: 'FormFieldsAccordion',
  data() {
    return {
      stats: [],
      isAuthenticated: true,
      authError: false,
      pqEmail: '',
      pqPassword: ''
    }
  },
  mounted() {
    axios.get(config.resourceServer.pq_is_authenticated, this.authConfig())
      .then((result) => {
        this.isAuthenticated = result.data.is_authenticated
        this.getAllStats()
      })
  },
  methods: {
    authConfig() {
      const accessToken = this.$auth.getAccessToken()
      return { headers: { Authorization: `Bearer ${accessToken}` } }
    },
    calculateCharge(charge, updatedAt) {
      let now = Date.now()
      let adj = (now - updatedAt)/1000/60/4;
      let finalCharge = (charge-adj) < 0 ? 0 : (charge-adj);
      return Number(finalCharge).toFixed(0)
    },
    processPQ() {
      axios.post(
          config.resourceServer.pq_authenticate, {login: this.pqEmail, password: this.pqPassword}, this.authConfig()
      )
      .then(result => {
        if (result.data.status === 'SUCCESS') {
          this.isAuthenticated = true
          this.getAllStats()
        }
      })
      .catch(() => {
        this.authError = true
      })
    },
    getAllStats() {
      axios.get(
          config.resourceServer.pq_metrics_all, this.authConfig()
      )
      .then(result => {
        this.stats = result.data
      })
    }
  }
}
</script>
