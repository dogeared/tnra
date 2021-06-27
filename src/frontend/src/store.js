import Vue from 'vue';
import Vuex from 'vuex';
import _ from 'lodash';
import axios from 'axios';
import config from '@/config';

Vue.use(Vuex)

/* eslint-disable no-unused-vars */
let updatePost = (postObj, payload) => {
    // console.log(_.get(postObj, payload.key))
    axios.post(config.resourceServer.in_progress, postObj, payload.authHeader)
        .then((response) => {
            // TODO
            // console.log(response.data)
        })
        .catch(() => {
            // TODO
        })
}
/* eslint-enable no-unused-vars */

let debouncedUpdatePost = _.debounce(updatePost, 1500, { maxWait: 1500 })

export default new Vuex.Store({
    state: {
        completedPost: null,
        inProgressPost: null
    },
    getters: {
        getProperty: (state) =>  (name, key) => {
            return _.get(state[name], key)
        }
    },
    mutations: {
        patchInProgressPost: (state, payload) => {
            _.set(state.inProgressPost, payload.key, payload.value)
            debouncedUpdatePost(state.inProgressPost, payload)
        },
        patchCompletePost: (state, payload) => {
            _.set(state.completedPost, payload.key, payload.value)
            debouncedUpdatePost(state.completedPost, payload)
        },
        setCompletedPost: (state, post) => {
            state.completedPost = post
        },
        setInProgressPost: (state, post) => {
            state.inProgressPost = post
        }
    },
    actions: {
        /* eslint-disable no-unused-vars */
        async getOptionalCompletedPost({ commit }, payload) {
            return await axios.get(config.resourceServer.complete, payload.authHeader)
        },
        async getOptionalInProgressPost({ commit }, payload) {
            return await axios.get(config.resourceServer.in_progress, payload.authHeader)
        },
        /* eslint-enable no-unused-vars */
        startPost: ({ commit }, payload) => {
            axios.get(config.resourceServer.start, payload.authHeader)
                .then((response) => {
                    commit('setInProgressPost', response.data)
                })
        }
    }
})