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
    <ve-table v-if="isAuthenticated" :columns="columns" :table-data="tableData" :sort-option="sortOption" :cell-style-option="cellStyleOption" />
  </div>
</template>

<style>
.table-body-cell-class-reps {
  background: #91d5ff !important;
  color: #fff !important;
}

.table-body-cell-class-muscle {
  background: #91b5ff !important;
  color: #fff !important;
}

.table-body-cell-class-charge {
  background: #9195ff !important;
  color: #fff !important;
}
</style>

<script>
import config from '@/config';
import axios from 'axios';

export default {
  name: 'FormFieldsAccordion',
  data() {
    return {
      isAuthenticated: true,
      authError: false,
      pqEmail: '',
      pqPassword: '',
      columns: [
        { field: "name", key: "a", title: "Name", align: "left", sortBy: "asc" },
        { field: "charge", key: "b", title: "Charge", align: "right", sortBy: "" },
        { field: "muscle", key: "c", title: "Muscle", align: "right", sortBy: "" },
        { field: "reps", key: "d", title: "Reps Today", align: "right", sortBy: "" },
      ],
      tableData: [],
      sortOption: {
        sortChange: (params) => {
          this.sortChange(params);
        }
      },
      cellStyleOption: {
        bodyCellClass: ({column, rowIndex}) => {
          let winRows = this.calcWinRows()
          if (column.field === 'reps' && rowIndex === winRows.reps) {
            return 'table-body-cell-class-reps'
          } else if (column.field === 'muscle' && rowIndex === winRows.muscle) {
            return 'table-body-cell-class-muscle'
          } else if (column.field === 'charge' && rowIndex === winRows.charge) {
            return 'table-body-cell-class-charge'
          }
        }
      }
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
    calcWinRows() {
      let winVals = {reps: 0, muscle: 0, charge: 0}
      let winRows = {reps: 0, muscle: 0, charge: 0}
      for (let i = 0; i<this.tableData.length; i++) {
        ['reps', 'muscle', 'charge'].forEach(elem => {
          if (Number(this.tableData[i][elem]) > winVals[elem]) {
            winVals[elem] = Number(this.tableData[i][elem])
            winRows[elem] = i
          }
        })
      }
      return winRows
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
    transformPQData(data) {
      let ret = []
      for (let name in data) {
        let val = data[name]
        let newVal = {name: name}
        if (val === null) {
          newVal.charge = 'no data, please re-authenticate'
          newVal.muscle = ''
          newVal.reps = ''
        } else {
          let pq = val.pq
          newVal.charge = this.calculateCharge(pq.charge, pq.updated_at)
          newVal.muscle = Number(pq.muscle).toFixed(0)
          newVal.reps = pq.reps_day
        }
        ret.push(newVal)
      }
      return ret.sort((a,b) => {
        return a.name.localeCompare(b.name)
      })
    },
    getAllStats() {
      axios.get(
          config.resourceServer.pq_metrics_all, this.authConfig()
      )
      .then(result => {
        this.tableData = this.transformPQData(result.data)
      })
    },
    sortChange(params) {
      this.tableData.sort((a, b) => {
        if (params.name) {
          if (params.name === 'asc') {
            return a.name.localeCompare(b.name)
          } else if (params.name === 'desc') {
            return b.name.localeCompare(a.name)
          } else {
            return 0
          }
        } else if (params.charge) {
          if (params.charge === 'asc') {
            return a.charge - b.charge
          } else if (params.charge === 'desc') {
            return b.charge - a.charge
          } else {
            return 0
          }
        } else if (params.muscle) {
          if (params.muscle === 'asc') {
            return a.muscle - b.muscle
          } else if (params.muscle === 'desc') {
            return b.muscle - a.muscle
          } else {
            return 0
          }
        } else if (params.reps) {
          if (params.reps === 'asc') {
            return a.reps - b.reps
          } else if (params.reps === 'desc') {
            return b.reps - a.reps
          } else {
            return 0
          }
        }
      })
    }
  }
}
</script>