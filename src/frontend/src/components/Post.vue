<template lang="html">
  <div>
    <h2>POST - <span v-if="finished">COMPLETE</span><span v-else>IN PROGRESS</span></h2>
    <sui-form>
      <sui-button primary :disabled="!startEnabled" v-on:click.prevent="doStart">Start New Post</sui-button>
      <sui-button primary :disabled="!finishEnabled" v-on:click.prevent="doFinish">Finish Post</sui-button>
      <h3>started: {{started}}<span v-if="finished">, finished: {{finished}}</span></h3>
      <sui-tab>
        <sui-tab-pane title="Intro">
          <sui-form-field>
            <label>What I Don't Want you to Know</label>
            <textarea v-model="wid" :readonly="startEnabled"></textarea>
          </sui-form-field>
          <sui-form-field>
            <label>Kryptonite</label>
            <input v-model="kry" :readonly="startEnabled"/>
          </sui-form-field>
          <sui-form-field>
            <label>What and When</label>
            <textarea v-model="wha" :readonly="startEnabled"></textarea>
          </sui-form-field>
        </sui-tab-pane>
        <sui-tab-pane title="Personal">
          <sui-form-field>
            <label>Best</label>
            <textarea v-model="perBes" :readonly="startEnabled"></textarea>
          </sui-form-field>
          <sui-form-field>
            <label>Worst</label>
            <textarea v-model="perWor" :readonly="startEnabled"></textarea>
          </sui-form-field>
        </sui-tab-pane>
        <sui-tab-pane title="Family">
          <sui-form-field>
            <label>Best</label>
            <textarea v-model="famBes" :readonly="startEnabled"></textarea>
          </sui-form-field>
          <sui-form-field>
            <label>Worst</label>
            <textarea v-model="famWor" :readonly="startEnabled"></textarea>
          </sui-form-field>
        </sui-tab-pane>
        <sui-tab-pane title="Work">
          <sui-form-field>
            <label>Best</label>
            <textarea v-model="worBes" :readonly="startEnabled"></textarea>
          </sui-form-field>
          <sui-form-field>
            <label>Worst</label>
            <textarea v-model="worWor" :readonly="startEnabled"></textarea>
          </sui-form-field>
        </sui-tab-pane>
        <sui-tab-pane title="Stats">
          <h4>NOTE: You must set a value for each stat, even if 0</h4>
          <sui-grid>
            <sui-grid-row :columns="2">
              <sui-grid-column>
                <vue-slider v-model="exe" :max="7" :dot-options="dotOptions"
                            :tooltip="'always'" :tooltip-formatter="exeFormatter"
                            style="padding: 45px 10px 30px 40px; width: auto; height: 4px;"></vue-slider>
              </sui-grid-column>
              <sui-grid-column>
                <vue-slider v-model="gtg" :max="7" :dot-options="dotOptions"
                            :tooltip="'always'" :tooltip-formatter="gtgFormatter"
                            style="padding: 45px 35px 30px 50px; width: auto; height: 4px;"></vue-slider>
              </sui-grid-column>
            </sui-grid-row>
            <sui-grid-row :columns="2">
              <sui-grid-column>
                <vue-slider v-model="spo" :max="7" :dot-options="dotOptions"
                            :tooltip="'always'" :tooltip-formatter="spoFormatter"
                            style="padding: 30px 10px 30px 40px; width: auto; height: 4px;"></vue-slider>
              </sui-grid-column>
              <sui-grid-column>
                <vue-slider v-model="mee" :max="7" :dot-options="dotOptions"
                            :tooltip="'always'" :tooltip-formatter="meeFormatter"
                            style="padding: 30px 35px 30px 50px; width: auto; height: 4px;"></vue-slider>
              </sui-grid-column>
            </sui-grid-row>
            <sui-grid-row :columns="2">
              <sui-grid-column>
                <vue-slider v-model="rea" :max="7" :dot-options="dotOptions"
                            :tooltip="'always'" :tooltip-formatter="reaFormatter"
                            style="padding: 30px 10px 30px 40px; width: auto; height: 4px;"></vue-slider>
              </sui-grid-column>
              <sui-grid-column>
                <vue-slider v-model="pra" :max="7" :dot-options="dotOptions"
                            :tooltip="'always'" :tooltip-formatter="praFormatter"
                            style="padding: 30px 35px 30px 50px; width: auto; height: 4px;"></vue-slider>
              </sui-grid-column>
            </sui-grid-row>
            <sui-grid-row :columns="2">
              <sui-grid-column>
                <vue-slider v-model="med" :max="7" :dot-options="dotOptions"
                            :tooltip="'always'" :tooltip-formatter="medFormatter"
                            style="padding: 30px 10px 30px 40px; width: auto; height: 4px;"></vue-slider>
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
import VueSlider from 'vue-slider-component'
import 'vue-slider-component/theme/default.css'

export default {
  name: 'FormFieldsAccordion',
  components: {
    VueSlider
  },
  data() {
    return {
      startEnabled: false,
      finishEnabled: false,
      postName: 'completedPost',
      mutatorName: 'patchCompletePost',
      exeFormatter: v => v === null ? 'Exercise' : `Exercise: ${v}`,
      gtgFormatter: v => v === null ? 'Go-to-Guy' : `Go-to-Guy: ${v}`,
      spoFormatter: v => v === null ? 'Sponsor' : `Sponsor: ${v}`,
      meeFormatter: v => v === null ? 'Meetings' : `Meetings: ${v}`,
      praFormatter: v => v === null ? 'Pray' : `Pray: ${v}`,
      reaFormatter: v => v === null ? 'Read' : `Read: ${v}`,
      medFormatter: v => v === null ? 'Meditate' : `Meditate: ${v}`
    };
  },
  computed: {
    dotOptions() {
      return [{disabled: this.startEnabled}]
    },
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
      this.finishEnabled = this.$store.getters.checkFinished()
    },
    formatTime(timeStr) {
      return (timeStr) ? new Date(timeStr).toLocaleString() : null;
    },
    doStart() {
      this.$store.dispatch('startPost', { authHeader: this.authConfig() })
          .then((response) => {
            this.$store.commit('setInProgressPost', response.data)
            this.$store.commit('setCompletedPost', null)
            this.postName = 'inProgressPost'
            this.mutatorName = 'patchInProgressPost'
            this.startEnabled = false
          })
    },
    doFinish() {
      this.$store.dispatch('finishPost', { authHeader: this.authConfig() })
          .then((response) => {
            this.$store.commit('setInProgressPost', null)
            this.$store.commit('setCompletedPost', response.data)
            this.postName = 'completedPost'
            this.mutatorName = 'patchCompletePost'
            this.startEnabled = true
            this.finishEnabled = this.$store.getters.checkFinished()
          })
    }
  },
  async beforeMount() {
    // TODO - need to deal wih situation where there are no in progress nor completed posts
    let inProgressPostResponse = await this.$store.dispatch('getOptionalInProgressPost', { authHeader: this.authConfig() })
    let completePostResponse = await this.$store.dispatch('getOptionalCompletedPost', { authHeader:  this.authConfig() })
    if (inProgressPostResponse && inProgressPostResponse.data) {
      this.$store.commit('setInProgressPost', inProgressPostResponse.data)
      this.postName = 'inProgressPost'
      this.mutatorName = 'patchInProgressPost'
    } else if (completePostResponse && completePostResponse.data) {
      this.$store.commit('setCompletedPost', completePostResponse.data)
      this.postName = 'completedPost'
      this.mutatorName = 'patchCompletedPost'
      this.startEnabled = true
    }
    this.finishEnabled = this.$store.getters.checkFinished()
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