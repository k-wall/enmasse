import React, {Component} from 'react';
import './app.css';
import Layout from "../components/Layout/Layout"
import OAuth2User from "../components/OAuth2User";
import UnauthenticatedUser from "../components/UnauthenticatedUser";

let oauth = false;

// TODO need to inject the oauth endpoints etc from the server side.
const user = oauth ? (<OAuth2User authorizationEndpoint='https://accounts.google.com/o/oauth2/v2/auth'
                                  userInfoEndpoint='https://openidconnect.googleapis.com/v1/userinfo'
                                  scope='openid profile'
                                  redirectUri='http://localhost:9000'
                                  clientId='1045256966207-mie784ka26ncjb1gvd98jatldd0ct42v.apps.googleusercontent.com'/>)
  : <UnauthenticatedUser/>;

export default class App extends Component {


  render() {
    console.log('[App.js] Inside render()');

    return (
      <div >
        <Layout user={user}/>
      </div>
    );
  }

}
