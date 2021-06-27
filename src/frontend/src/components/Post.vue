<template lang="html">
  <div>
    <h2>Post</h2>
    <sui-form>
      <sui-button primary :disabled="!startEnabled" v-on:click.prevent="doStart">Start New Post</sui-button>
      <sui-button primary :disabled="!finishEnabled">Finish Post</sui-button>
      <h3>started: {{started}}<span v-if="finished">, finished: {{finished}}</span></h3>
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
    </sui-form>
  </div>
</template>

<script>
import _ from 'lodash'

export default {
  name: 'FormFieldsAccordion',
  data() {
    return {
      checkFinished: null,
      startEnabled: false,
      finishEnabled: false,
      postName: 'completedPost',
      mutatorName: 'patchCompletePost',
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
  computed: {
    started() {
      return this.formatTime(this.getPostPart(this.postName, 'start'))
    },
    finished() {
      return this.formatTime(this.getPostPart(this.postName, 'finish'))
    },
    wid: {
      get() {
        return this.getPostPart(this.postName, 'intro.widwytk')
      },
      set(value) {
        this.updatePost(this.mutatorName, 'intro.widwytk', value)
      }
    },
    kry: {
      get() {
        return this.getPostPart(this.postName, 'intro.kryptonite')
      },
      set(value) {
        this.updatePost(this.mutatorName, 'intro.kryptonite', value)
      }
    },
    wha: {
      get() {
        return this.getPostPart(this.postName, 'intro.whatAndWhen')
      },
      set(value) {
        this.updatePost(this.mutatorName, 'intro.whatAndWhen', value)
      }
    },
    perBes: {
      get() {
        return this.getPostPart(this.postName, 'personal.best')
      },
      set(value) {
        this.updatePost(this.mutatorName, 'personal.best', value)
      }
    },
    perWor: {
      get() {
        return this.getPostPart(this.postName, 'personal.worst')
      },
      set(value) {
        this.updatePost(this.mutatorName, 'personal.worst', value)
      }
    },
    famBes: {
      get() {
        return this.getPostPart(this.postName, 'family.best')
      },
      set(value) {
        this.updatePost(this.mutatorName, 'family.best', value)
      }
    },
    famWor: {
      get() {
        return this.getPostPart(this.postName, 'family.worst')
      },
      set(value) {
        this.updatePost(this.mutatorName, 'family.worst', value)
      }
    },
    worBes: {
      get() {
        return this.getPostPart(this.postName, 'work.best')
      },
      set(value) {
        this.updatePost(this.mutatorName, 'work.best', value)
      }
    },
    worWor: {
      get() {
        return this.getPostPart(this.postName, 'work.worst')
      },
      set(value) {
        this.updatePost(this.mutatorName, 'work.worst', value)
      }
    },
    exe: {
      get() {
        return this.getPostPart(this.postName, 'stats.exercise')
      },
      set(value) {
        this.updatePost(this.mutatorName, 'stats.exercise', value)
      }
    },
    gtg: {
      get() {
        return this.getPostPart(this.postName, 'stats.gtg')
      },
      set(value) {
        this.updatePost(this.mutatorName, 'stats.gtg', value)
      }
    },
    med: {
      get() {
        return this.getPostPart(this.postName, 'stats.meditate')
      },
      set(value) {
        this.updatePost(this.mutatorName, 'stats.meditate', value)
      }
    },
    rea: {
      get() {
        return this.getPostPart(this.postName, 'stats.read')
      },
      set(value) {
        this.updatePost(this.mutatorName, 'stats.read', value)
      }
    },
    pra: {
      get() {
        return this.getPostPart(this.postName, 'stats.pray')
      },
      set(value) {
        this.updatePost(this.mutatorName, 'stats.pray', value)
      }
    },
    mee: {
      get() {
        return this.getPostPart(this.postName, 'stats.meetings')
      },
      set(value) {
        this.updatePost(this.mutatorName, 'stats.meetings', value)
      }
    },
    spo: {
      get() {
        return this.getPostPart(this.postName, 'stats.sponsor')
      },
      set(value) {
        this.updatePost(this.mutatorName, 'stats.sponsor', value)
      }
    }
  },
  methods: {
    authConfig() {
      const accessToken = this.$auth.getAccessToken()
      return { headers: { Authorization: `Bearer ${accessToken}` } }
    },
    getPostPart(name, key) {
      return this.$store.getters.getProperty(name, key)
    },
    updatePost(mutator, key, value) {
      this.$store.commit(mutator, {key: key, value: value, authHeader: this.authConfig()})
      this.finishEnabled = this.checkFinished()
    },
    formatTime(timeStr) {
      return (timeStr) ? new Date(timeStr).toLocaleString() : null;
    },
    doStart() {
      this.$store.dispatch('startPost', { authHeader: this.authConfig() })
          .then(() => {
            this.$store.commit('setCompletedPost', null)
            this.postName = 'inProgressPost'
            this.mutatorName = 'patchInProgressPost'
            this.startEnabled = false
          })
    },
    doFinish() {

    }
  },
  created() {
    this.checkFinished = _.throttle(() => {
      let stringParts = [
        'intro.widwytk', 'intro.kryptonite', 'intro.whatAndWhen',
        'personal.best', 'personal.worst',
        'family.best', 'family.worst',
        'work.best', 'work.worst'
      ]
      let numParts = [
        'stats.exercise', 'stats.gtg', 'stats.meditate', 'stats.meetings',
        'stats.pray', 'stats.read', 'stats.sponsor'
      ]
      let partsWithDataScore = 0;
      stringParts.forEach((partName) => {
        if (
            this.$store.state.inProgressPost && this.$store.getters.getProperty('inProgressPost', partName) &&
            this.$store.getters.getProperty('inProgressPost', partName).length > 0
        ) {
          partsWithDataScore++;
        }
      })
      numParts.forEach((partName) => {
        if (
            this.$store.state.inProgressPost && this.$store.getters.getProperty('inProgressPost', partName) &&
            this.$store.getters.getProperty('inProgressPost', partName) >= 0
        ) {
          partsWithDataScore++;
        }
      })
      return (partsWithDataScore === (stringParts.length + numParts.length))
    }, 1500)
    this.checkFinished.bind(this);
  },
  async beforeMount() {
    // TODO - need to deal wih situation where there are no in progress nor completed posts
    let inProgressPostResponse = await this.$store.dispatch('getOptionalInProgressPost', { authHeader: this.authConfig() })
    let completePostResponse = await this.$store.dispatch('getOptionalCompletedPost', { authHeader:  this.authConfig() })
    if (inProgressPostResponse) {
      this.$store.commit('setInProgressPost', inProgressPostResponse.data)
      this.postName = 'inProgressPost'
      this.mutatorName = 'patchInProgressPost'
    } else if (completePostResponse) {
      this.$store.commit('setCompletedPost', completePostResponse.data)
      this.postName = 'completedPost'
      this.mutatorName = 'patchCompletedPost'
      this.startEnabled = true
    }
    this.finishEnabled = this.checkFinished()
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