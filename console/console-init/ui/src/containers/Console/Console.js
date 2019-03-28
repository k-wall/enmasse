import React, { Component } from 'react';
import { loadMessagingInstances } from '../../components/messagingInstances/messagingInstance/enmasse/EnmasseAddressSpaces';

import NamespaceTable from '../../components/messagingInstances/MessagingInstances';
import Layout from '../../components/Layout/Layout';
import User from '../../components/User';


class Console extends Component {

  state = {
    messagingInstances: null,
    instanceTypes: {
      totalInstances: 0,
      standardAddressSpaces: 0,
      brokeredAddressSpaces: 0
    }
  };



  componentDidUpdate() {

    console.log('[CONSOLE] componentDidUpdate');
  }

  componentDidMount() {

    console.log('[CONSOLE] DidMount');
    loadMessagingInstances()
       .then(messagingSpaces => {
         let standardInstances = 0;
         let brokeredInstances = 0;

         messagingSpaces.forEach(function (space, index) {
           switch (space.type) {
             case ('standard'):
               standardInstances++;
               break;
             case ('brokered'):
               brokeredInstances++;
               break;
           }
         });

         let totalInstances = standardInstances + brokeredInstances;
         this.setState({messagingInstances: messagingSpaces,
           instanceTypes: {
             totalInstances: totalInstances,
             standardInstances: standardInstances,
             brokeredInstances: brokeredInstances}});
         console.log('Container set state for '+messagingSpaces);
       })
       .catch(error => {
         console.log("Couldn't set the message instances: " + error);
       });
  }

  render() {
    console.log('[CONSOLE] Render');
    return (
      <Layout user={<User/>} instanceTypes={this.state.instanceTypes}>
        <NamespaceTable messagingInstances={this.state.messagingInstances}/>
      </Layout>
    )
  }
}

export default Console;
