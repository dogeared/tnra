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
<style>
.ui.selection.dropdown {
  min-width: 1em;
}
.ui.grid>.row {
  padding-top: 2px;
  padding-bottom: 2px;
}
</style>