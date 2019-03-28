

class MessagingInstance {
  constructor(name, namespace, component, type, timeCreated, isReady) {
    this.name = name;
    this.namespace = namespace;
    this.component = 'AS';
    this.type = type;
    this.timeCreated = timeCreated;
    this.isReady = isReady;
  }

}

export default MessagingInstance;
