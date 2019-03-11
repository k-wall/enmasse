import React from 'react';

let OAuth = require('@zalando/oauth2-client-js');

// TODO guard against leak of credentials via referer header and scrub browser history of fragment.
// TODO logout
export default class OAuth2User extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};

    this.implictOAuth2 = new OAuth.Provider({
      id: 'oauth',
      authorization_url: props.authorizationEndpoint
    });
    try {
      this.implictOAuth2.parse(window.location.hash);
    } catch (e) {
      console.log("User not logged in");
      let request = this._createOAuth2Request();
      this._doLoginRedirect(request);
    }
  }

  componentDidMount() {
    fetch(this.props.userInfoEndpoint,
      {headers: new Headers(this.getRequestAuthorizationHeaders({}))})
      .then(res => res.json())
      .then(prof => this.setState({
        name: prof.name
      }))
      .catch(reason => this._doLoginRedirect(this._createOAuth2Request()));
  }

  getRequestAuthorizationHeaders(headers) {
    headers['Authorization'] = 'Bearer ' + this.implictOAuth2.getAccessToken();
    return headers;
  }

  _doLoginRedirect(request) {
    window.location.href = this.implictOAuth2.requestToken(request);
  }

  _createOAuth2Request() {
    let request = new OAuth.Request({
      client_id: this.props.clientId,
      redirect_uri: this.props.redirectUri,
      scope: this.props.scope
    });
    this.implictOAuth2.remember(request);
    return request;
  }

  render() {
    return <React.Fragment>{this.state.name}</React.Fragment>;
  }
}
