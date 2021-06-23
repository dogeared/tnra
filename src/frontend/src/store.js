import Vue from 'vue';
import Vuex from 'vuex';
import _ from 'lodash';
import axios from 'axios';
import config from '@/config';

Vue.use(Vuex)

let updatePost = (postObj, key) => {
    console.log(_.get(postObj, key))
}

let debouncedUpdatePost = _.debounce(updatePost, 1500, { maxWait: 1500 })

export default new Vuex.Store({
    state: {
        completedPost: {},
        inProgressPost: {}
    },
    getters: {
        getProperty: (state) =>  (name, key) => {
            return _.get(state[name], key)
        }
    },
    mutations: {
        patchInProgressPost: (state, payload) => {
            _.set(state.inProgressPost, payload.key, payload.value)
            debouncedUpdatePost(state.inProgressPost, payload.key)
        },
        patchCompletePost: (state, payload) => {
            _.set(state.completedPost, payload.key, payload.value)
            debouncedUpdatePost(state.completedPost, payload.key)
        },
        setCompletedPost: (state, post) => {
            state.completedPost = post
        }
    },
    actions: {
        getLastestCompletedPost: ({ commit }, payload) => {
            axios.get(config.resourceServer.my_last_post, payload.authHeader)
                .then((response) => {
                    commit('setCompletedPost', response.data)
                })
        }
    }
})