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
export default {
  name: 'FormFieldsAccordion',
  data() {
    return {
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
      return this.formatTime(this.getPostPart('completedPost', 'start'))
    },
    finished() {
      return this.formatTime(this.getPostPart('completedPost', 'finish'))
    },
    wid: {
      get() {
        return this.getPostPart('completedPost', 'intro.widwytk')
      },
      set(value) {
        this.updatePost('patchCompletePost', 'intro.widwytk', value)
      }
    },
    kry: {
      get() {
        return this.getPostPart('completedPost', 'intro.kryptonite')
      },
      set(value) {
        this.updatePost('patchCompletePost', 'intro.kryptonite', value)
      }
    },
    wha: {
      get() {
        return this.getPostPart('completedPost', 'intro.whatAndWhen')
      },
      set(value) {
        this.updatePost('patchCompletePost', 'intro.whatAndWhen', value)
      }
    },
    perBes: {
      get() {
        return this.getPostPart('completedPost', 'personal.best')
      },
      set(value) {
        this.updatePost('patchCompletePost', 'personal.best', value)
      }
    },
    perWor: {
      get() {
        return this.getPostPart('completedPost', 'personal.worst')
      },
      set(value) {
        this.updatePost('patchCompletePost', 'personal.word', value)
      }
    },
    famBes: {
      get() {
        return this.getPostPart('completedPost', 'family.best')
      },
      set(value) {
        this.updatePost('patchCompletePost', 'family.best', value)
      }
    },
    famWor: {
      get() {
        return this.getPostPart('completedPost', 'family.worst')
      },
      set(value) {
        this.updatePost('patchCompletePost', 'family.worst', value)
      }
    },
    worBes: {
      get() {
        return this.getPostPart('completedPost', 'work.best')
      },
      set(value) {
        this.updatePost('patchCompletePost', 'work.best', value)
      }
    },
    worWor: {
      get() {
        return this.getPostPart('completedPost', 'work.worst')
      },
      set(value) {
        this.updatePost('patchCompletePost', 'work.worst', value)
      }
    },
    exe: {
      get() {
        return this.getPostPart('completedPost', 'stats.exercise')
      },
      set(value) {
        this.updatePost('patchCompletePost', 'stats.exercise', value)
      }
    },
    gtg: {
      get() {
        return this.getPostPart('completedPost', 'stats.gtg')
      },
      set(value) {
        this.updatePost('patchCompletePost', 'stats.gtg', value)
      }
    },
    med: {
      get() {
        return this.getPostPart('completedPost', 'stats.meditate')
      },
      set(value) {
        this.updatePost('patchCompletePost', 'stats.meditate', value)
      }
    },
    rea: {
      get() {
        return this.getPostPart('completedPost', 'stats.read')
      },
      set(value) {
        this.updatePost('patchCompletePost', 'stats.read', value)
      }
    },
    pra: {
      get() {
        return this.getPostPart('completedPost', 'stats.pray')
      },
      set(value) {
        this.updatePost('patchCompletePost', 'stats.pray', value)
      }
    },
    mee: {
      get() {
        return this.getPostPart('completedPost', 'stats.meetings')
      },
      set(value) {
        this.updatePost('patchCompletePost', 'stats.meetings', value)
      }
    },
    spo: {
      get() {
        return this.getPostPart('completedPost', 'stats.sponsor')
      },
      set(value) {
        this.updatePost('patchCompletePost', 'stats.sponsor', value)
      }
    }
  },
  methods: {
    getPostPart(name, key) {
      return this.$store.getters.getProperty(name, key)
    },
    updatePost(mutator, key, value) {
      this.$store.commit(mutator, {key: key, value: value})
      console.log(this.getPostPart('completedPost', key))
    },
    authConfig() {
      const accessToken = this.$auth.getAccessToken()
      return {
        headers: {
          Authorization: `Bearer ${accessToken}`
        }
      }
    },
    formatTime(timeStr) {
      return new Date(timeStr).toLocaleString();
    }
  },
  beforeMount() {
    this.$store.dispatch('getLastestCompletedPost', { authHeader:  this.authConfig() })
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