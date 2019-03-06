import React, { Component } from 'react';
import './app.css';
import Layout from "../components/Layout/Layout"

export default class App extends Component {

  render() {
    console.log('[App.js] Inside render()');
    //Render methods in Container components (components that handle state), shouldn't be involved in the UI too much.

    return (
      <div >
        <Layout>
        </Layout>
      </div>
    );
  }
}
