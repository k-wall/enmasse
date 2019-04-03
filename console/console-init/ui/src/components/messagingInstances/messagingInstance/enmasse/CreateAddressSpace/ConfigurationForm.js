import React from 'react';
import {
  Form,
  FormGroup,
  FormSelect,
  FormSelectOption,
  Radio,
  TextInput,
  Title
} from '@patternfly/react-core';


import {loadNamespaces} from '../../Namespaces.js';

class ConfigurationForm extends React.Component {
  state = {
    newInstance: this.props.newInstance,
    isValid: this.props.isConfigurationFormValid,
    namespaces: []
  };

  componentDidMount() {
    loadNamespaces()
      .then(namespaces => {
        this.setState({namespaces: namespaces});
        if (!this.state.newInstance.namespace) {
          var newInstance = {...this.state.newInstance};
          newInstance.namespace = namespaces[0];

          this.props.onChange(this.isValid(newInstance), newInstance);
          this.setState({newInstance: newInstance});
        }
      })
      .catch(error => {
        console.log("Couldn't set the namespaces instances: " + error);
      });
  }

  handleNameChange = name => {
    var newInstance = {...this.state.newInstance};
    newInstance.name = name;

    this.props.onChange(this.isValid(newInstance), newInstance);
    this.setState({newInstance: newInstance});
  }

  handleNamespaceChange = namespace => {
    var newInstance = {...this.state.newInstance};
    newInstance.namespace = namespace;
    this.props.onChange(this.isValid(newInstance), newInstance);
    this.setState({newInstance: newInstance});
  }

  handleTypeStandardChange = (value) => {
    var newInstance = {...this.state.newInstance};
    newInstance.typeStandard = value;
    newInstance.typeBrokered = !value;

    this.props.onChange(this.isValid(newInstance), newInstance);
    this.setState({newInstance: newInstance});
  }

  handleTypeBrokeredChange = (value) => {
    var newInstance = {...this.state.newInstance};
    newInstance.typeBrokered = value;
    newInstance.typeStandard = !value;

    this.props.onChange(this.isValid(newInstance), newInstance);
    this.setState({newInstance: newInstance});
  }

  handlePlanChange = plan => {
    var newInstance = {...this.state.newInstance};
    newInstance.plan = plan;
    this.props.onChange(this.isValid(newInstance), newInstance);
    this.setState({newInstance: newInstance});
  }


  isValid(instance) {
    return Boolean(instance.name && instance.plan && instance.namespace && (instance.typeBrokered || instance.typeStandard));
  }

  render() {
    const {newInstance} = this.state;

    return (
      <Form>
        <Title size={"xl"}>Configure your component</Title>
        <FormGroup
          label="Namespace"
          fieldId="form-namespace">
          <FormSelect
            value={this.state.newInstance.namespace}
            onChange={this.handleNamespaceChange}
            id="form-namespace"
            name="form-namespace"
          >
            {
              this.state.namespaces.map((option, index) => (
                <FormSelectOption key={index} value={option}
                                  label={option}/>
              ))
            }
          </FormSelect>
        </FormGroup>
        <FormGroup
          label="Name"
          isRequired
          fieldId="name"
        >
          <TextInput
            isRequired
            type="text"
            id="form-name"
            name="form-name"
            aria-describedby="formname"
            value={newInstance.name}
            onChange={this.handleNameChange}
          />
        </FormGroup>
        <FormGroup
          isRequired
          label="Address plan"
          fieldId="form-planName">
          <TextInput
            isRequired
            type="text"
            id="form-planName"
            name="form-planName"
            aria-describedby="plan Name"
            value={newInstance.plan}
            onChange={this.handlePlanChange}
          />
        </FormGroup>
        <FormGroup
          isInline
          label="Type"
          isRequired
          fieldId="addressSpaceType">
          <Radio id="addressSpaceType1" name="addressSpaceType" value={newInstance.typeStandard}
                 checked={newInstance.typeStandard} label="Standard" aria-label="Standard"
                 onChange={this.handleTypeStandardChange}/>
          <Radio id="addressSpaceType2" name="addressSpaceType" value={newInstance.typeBrokered}
                 checked={newInstance.typeBrokered} label="Brokered" aria-label="Brokered"
                 onChange={this.handleTypeBrokeredChange}/>
        </FormGroup>
      </Form>
    )
      ;
  }
}

export default ConfigurationForm;
