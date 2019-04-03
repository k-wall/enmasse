import axios from 'axios';
import MessagingSpace from '../MessagingInstance';

const translate = addressSpaces => {

    let translation = addressSpaces.items.map(namespace => new MessagingSpace(namespace.metadata.name,
      namespace.metadata.namespace,
      namespace.kind,
      namespace.spec.type,
      namespace.metadata.creationTimestamp,
      namespace.status.isReady));

    return translation;
}

export function loadMessagingInstances() {
  return axios.get('apis/enmasse.io/v1beta1/addressspaces')
    .then(response => translate(response.data))
    .catch(error => {
      console.log(error);
      return [];
    });
}

export function deleteMessagingInstances(name, namespace) {
  console.log('clicked on delete action, on: ' + name + ' ' + namespace);
  return axios.delete('apis/enmasse.io/v1beta1/namespaces/' + namespace + '/addressspaces/' + name)
    .then(response => console.log('DELETE successful: ' + response))
    .catch(error =>
      console.log('DELETE FAILED: ', error)
    );
}

export function createNewAddressSpace(instance) {
  console.log('Creating new address sapce', instance);
  return axios.post('apis/enmasse.io/v1beta1/namespaces/'+ instance.namespace +'/addressspaces', {
    apiVersion: 'enmasse.io/v1beta1',
    kind: 'AddressSpace',
    metadata: {
      name: instance.name,
      namespace: instance.namespace
    },
    spec: {
      type: (instance.typeStandard ? 'standard' : 'brokered'),
      plan: instance.plan
    }
  })
    .then(response => console.log('CREATE successful: '+ response))
    .catch(error =>
      console.log('CREATE FAILED: ', error)
    );
}


