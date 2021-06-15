<template lang="html">
  <div>
    <h3>Post</h3>
    <sui-form>
      <sui-tab>
        <sui-tab-pane title="Intro">
          <sui-form-field>
            <label>What I Don't Want you to Know</label>
            <textarea></textarea>
          </sui-form-field>
          <sui-form-field>
            <label>Kryptonite</label>
            <input placeholder="KRY" />
          </sui-form-field>
          <sui-form-field>
            <label>What and When</label>
            <textarea></textarea>
          </sui-form-field>
        </sui-tab-pane>
        <sui-tab-pane title="Personal">
          <sui-form-field>
            <label>Best</label>
            <textarea></textarea>
          </sui-form-field>
          <sui-form-field>
            <label>Worst</label>
            <textarea></textarea>
          </sui-form-field>
        </sui-tab-pane>
        <sui-tab-pane title="Family">
          <sui-form-field>
            <label>Best</label>
            <textarea></textarea>
          </sui-form-field>
          <sui-form-field>
            <label>Worst</label>
            <textarea></textarea>
          </sui-form-field>
        </sui-tab-pane>
        <sui-tab-pane title="Work">
          <sui-form-field>
            <label>Best</label>
            <textarea></textarea>
          </sui-form-field>
          <sui-form-field>
            <label>Worst</label>
            <textarea></textarea>
          </sui-form-field>
        </sui-tab-pane>
        <sui-tab-pane title="Stats">
<!--            exercise: not set, gtg: not set, meditate: not set, meetings: not set, pray: not set, read: not set, sponsor: not set-->
            <sui-dropdown
                placeholder="Exercise"
                selection
                :options="options"
                v-model="exe"
            />
            <sui-dropdown
                placeholder="Go-to-Guy"
                selection
                :options="options"
                v-model="gtg"
            />
          <sui-dropdown
              placeholder="Read"
              selection
              :options="options"
              v-model="rea"
          />
          <sui-dropdown
              placeholder="Pray"
              selection
              :options="options"
              v-model="pra"
          />
          <sui-dropdown
              placeholder="Meditate"
              selection
              :options="options"
              v-model="med"
          />
          <sui-dropdown
              placeholder="Sponsor"
              selection
              :options="options"
              v-model="spo"
          />
          <sui-dropdown
              placeholder="Meetings"
              selection
              :options="options"
              v-model="mee"
          />
        </sui-tab-pane>
      </sui-tab>
      <p/>
      <sui-button type="submit">Submit</sui-button>
    </sui-form>
  </div>
</template>

<script>
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
      options: [
        {
          text: '0',
          value: 0,
        },
        {
          text: '1',
          value: 1,
        },
        {
          text: '2',
          value: 2,
        },
        {
          text: '3',
          value: 3,
        },
        {
          text: '4',
          value: 4,
        },
        {
          text: '5',
          value: 5,
        },
        {
          text: '6',
          value: 6,
        },
        {
          text: '7',
          value: 7,
        }
      ],
    };
  },
};
</script>