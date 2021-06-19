<template lang="html">
  <div>
    <h2>Post</h2>
    <h3>started: {{started}}, finished: {{finished}}</h3>
    <sui-form>
      <sui-tab>
        <sui-tab-pane title="Intro">
          <sui-form-field>
            <label>What I Don't Want you to Know</label>
            <textarea v-model="wid"></textarea>
          </sui-form-field>
          <sui-form-field>
            <label>Kryptonite</label>
            <input v-model="kry"/>
          </sui-form-field>
          <sui-form-field>
            <label>What and When</label>
            <textarea v-model="wha"></textarea>
          </sui-form-field>
        </sui-tab-pane>
        <sui-tab-pane title="Personal">
          <sui-form-field>
            <label>Best</label>
            <textarea v-model="perBes"></textarea>
          </sui-form-field>
          <sui-form-field>
            <label>Worst</label>
            <textarea v-model="perWor"></textarea>
          </sui-form-field>
        </sui-tab-pane>
        <sui-tab-pane title="Family">
          <sui-form-field>
            <label>Best</label>
            <textarea v-model="famBes"></textarea>
          </sui-form-field>
          <sui-form-field>
            <label>Worst</label>
            <textarea v-model="famWor"></textarea>
          </sui-form-field>
        </sui-tab-pane>
        <sui-tab-pane title="Work">
          <sui-form-field>
            <label>Best</label>
            <textarea v-model="worBes"></textarea>
          </sui-form-field>
          <sui-form-field>
            <label>Worst</label>
            <textarea v-model="worWor"></textarea>
          </sui-form-field>
        </sui-tab-pane>
        <sui-tab-pane title="Stats">
          <sui-grid :columns="4">
            <sui-grid-row>
              <sui-grid-column>
                Exercise
              </sui-grid-column>
              <sui-grid-column>
                GTG
              </sui-grid-column>
              <sui-grid-column>
                Sponsor
              </sui-grid-column>
              <sui-grid-column>
                Meetings
              </sui-grid-column>
            </sui-grid-row>
            <sui-grid-row>
              <sui-grid-column>
                <sui-dropdown
                    selection
                    :options="options"
                    v-model="exe"
                />
              </sui-grid-column>
              <sui-grid-column>
                <sui-dropdown
                    placeholder=""
                    selection
                    :options="options"
                    v-model="gtg"
                />
              </sui-grid-column>
              <sui-grid-column>
                <sui-dropdown
                    placeholder=""
                    selection
                    :options="options"
                    v-model="spo"
                />
              </sui-grid-column>
              <sui-grid-column>
                <sui-dropdown
                    placeholder=""
                    selection
                    :options="options"
                    v-model="mee"
                />
              </sui-grid-column>
            </sui-grid-row>
            <sui-grid-row>
              <sui-grid-column>
                Read
              </sui-grid-column>
              <sui-grid-column>
                Pray
              </sui-grid-column>
              <sui-grid-column>
                Meditate
              </sui-grid-column>
            </sui-grid-row>
            <sui-grid-row>
              <sui-grid-column>
                <sui-dropdown
                    placeholder=""
                    selection
                    :options="options"
                    v-model="rea"
                />
              </sui-grid-column>
              <sui-grid-column>
                <sui-dropdown
                    placeholder=""
                    selection
                    :options="options"
                    v-model="pra"
                />
              </sui-grid-column>
              <sui-grid-column>
                <sui-dropdown
                    placeholder=""
                    selection
                    :options="options"
                    v-model="med"
                />
              </sui-grid-column>
            </sui-grid-row>
          </sui-grid>
        </sui-tab-pane>
      </sui-tab>
      <p/>
<!--      <sui-button type="submit">Submit</sui-button>-->
    </sui-form>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'FormFieldsAccordion',
  data() {
    return {
      exe: null,
      gtg: null,
      med: null,
      rea: null,
      pra: null,
      mee: null,
      spo: null,
      wid: '',
      kry: '',
      wha: '',
      perBes: '',
      perWor: '',
      famBes: '',
      famWor: '',
      worBes: '',
      worWor: '',
      started: '',
      finished: '',
      options: [
        { text: '0', value: 0 },
        { text: '1', value: 1 },
        { text: '2', value: 2 },
        { text: '3', value: 3 },
        { text: '4', value: 4 },
        { text: '5', value: 5 },
        { text: '6', value: 6 },
        { text: '7', value: 7 },
      ],
    };
  },
  methods: {
    async authConfig() {
      const accessToken = await this.$auth.getAccessToken()
      return {
        headers: {
          Authorization: `Bearer ${accessToken}`
        }
      }
    },
    formatTime(timeStr) {
      return new Date(timeStr).toLocaleString();
    },
    async getLatestPost() {
      axios.get("/api/v1/my_last_post", await this.authConfig())
        .then((response) => {
          let post = response.data
          this.started = this.formatTime(post.start)
          this.finished = this.formatTime(post.finish)
          this.wid = post.intro.widwytk
          this.kry = post.intro.kryptonite
          this.wha = post.intro.whatAndWhen
          this.perBes = post.personal.best
          this.perWor = post.personal.worst
          this.famBes = post.family.best
          this.famWor = post.family.worst
          this.worBes = post.work.best
          this.worWor = post.work.worst
          this.exe = post.stats.exercise
          this.gtg = post.stats.gtg
          this.med = post.stats.meditate
          this.pra = post.stats.pray
          this.rea = post.stats.read
          this.spo = post.stats.sponsor
          this.mee = post.stats.meetings

          console.log(post)
        })
    }
  },
  mounted() {
    this.getLatestPost()
  }
};
</script>
<style>
.ui.selection.dropdown {
  min-width: 1em;
}
.ui.grid>.row {
  padding-top: 2px;
  padding-bottom: 2px;
}
</style>