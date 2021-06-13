const { CLIENT_ID, ISSUER } = process.env

export default {
  oidc: {
    clientId: CLIENT_ID,
    issuer: ISSUER,
    redirectUri: window.location.origin + '/login/callback',
    scopes: ['openid', 'profile', 'email']
  },
  resourceServer: {
    location: '/api/v1/location',
    me: '/api/v1/me',
    everyone: '/api/v1/everyone',
    offices: '/api/v1/offices'
  }
}
