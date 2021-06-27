const { CLIENT_ID, ISSUER } = process.env

export default {
  oidc: {
    clientId: CLIENT_ID,
    issuer: ISSUER,
    redirectUri: window.location.origin + '/login/callback',
    scopes: ['openid', 'profile', 'email']
  },
  resourceServer: {
    me: '/api/v1/me',
    complete: '/api/v1/complete',
    in_progress: '/api/v1/in_progress',
    start: '/api/v1/start_from_app',
    finish: '/api/v1/finish_from_app'
  }
}
