import React, {Component} from 'react';
import './app.css';
import Layout from "../components/Layout/Layout"
import User from "../components/User";

const user = <User/>;

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
