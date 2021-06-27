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

const stringParts = [
    'intro.widwytk', 'intro.kryptonite', 'intro.whatAndWhen',
    'personal.best', 'personal.worst',
    'family.best', 'family.worst',
    'work.best', 'work.worst'
]
const numParts = [
    'stats.exercise', 'stats.gtg', 'stats.meditate', 'stats.meetings',
    'stats.pray', 'stats.read', 'stats.sponsor'
]

let checkFinished = (state) => {
    let partsWithDataScore = 0;
    stringParts.forEach((partName) => {
        if (
            state.inProgressPost && store.getters.getProperty('inProgressPost', partName) &&
            store.getters.getProperty('inProgressPost', partName).length > 0
        ) {
            partsWithDataScore++;
        }
    })
    numParts.forEach((partName) => {
        if (
            state.inProgressPost &&
            store.getters.getProperty('inProgressPost', partName) !== null &&
            store.getters.getProperty('inProgressPost', partName) >= 0
        ) {
            partsWithDataScore++;
        }
    })
    return (partsWithDataScore === (stringParts.length + numParts.length))
}

let debouncedUpdatePost = _.debounce(updatePost, 1500, { maxWait: 1500 })

let throttledCheckFinished = _.throttle(checkFinished, 1500)

const store = new Vuex.Store({
    state: {
        completedPost: null,
        inProgressPost: null
    },
    getters: {
        getProperty: (state) =>  (name, key) => {
            return _.get(state[name], key)
        },
        checkFinished: (state) => () => {
            return throttledCheckFinished(state)
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
        async startPost({ commit }, payload) {
            return await axios.get(config.resourceServer.start, payload.authHeader)
        },
        async finishPost({ commit }, payload) {
            return await axios.post(config.resourceServer.finish, {}, payload.authHeader)
        }
        /* eslint-enable no-unused-vars */
    }
})

export default store