import React from 'react';

export default class UnauthenticatedUser extends React.Component {

  getRequestAuthorizationHeaders(headers) {
    return headers;
  }

  render() {
    return <React.Fragment>Developer</React.Fragment>;
  }
}
