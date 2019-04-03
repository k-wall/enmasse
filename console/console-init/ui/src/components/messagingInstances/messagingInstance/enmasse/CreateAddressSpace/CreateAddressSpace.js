import React from 'react';
import {Button, BackgroundImageSrc, Wizard} from '@patternfly/react-core';

import ConfigurationForm from './ConfigurationForm';
import Review from './Review';
import { createNewAddressSpace } from '../EnmasseAddressSpaces';

import Aux from '../../../../../hoc/Aux/Aux';
import xsImage from "../../../../../assets/images/pfbg_576.jpg";
import xs2xImage from "../../../../../assets/images/pfbg_576@2x.jpg";
import smImage from "../../../../../assets/images/pfbg_768.jpg";
import sm2xImage from "../../../../../assets/images/pfbg_768@2x.jpg";
import lgImage from "../../../../../assets/images/pfbg_1200.jpg";
import filter from '../../../../../assets/images/background-filter.svg';

class CreateAddressSpace extends React.Component {
  state = {
    isOpen: false,
    isConfigurationFormValid: Boolean(false),
    newInstance: {
      name: '',
      namespace: '',
      typeStandard: '',
      typeBrokered: '',
      plan: ''
    },
  };

  toggleOpen = () => {
    this.setState(({isOpen}) => ({
      isOpen: !isOpen,
      isConfigurationFormValid: Boolean(false),
      newInstance: {
      name: '',
        namespace: '',
        typeStandard: '',
        typeBrokered: '',
        plan: ''
    }}));
  };

  onConfigurationFormChange = (isValid, value) => {
    this.setState(
      {
        isConfigurationFormValid: isValid,
        newInstance: value
      }
    );
  };

  onNext = ({id, name}, {prevId, prevName}) => {
    console.log('next');
    console.log(this.state.newInstance);
    console.log(`current id: ${id}, current name: ${name}, previous id: ${prevId}, previous name: ${prevName}`);
  };

  onBack = ({id, name}, {prevId, prevName}) => {
    console.log(`current id: ${id}, current name: ${name}, previous id: ${prevId}, previous name: ${prevName}`);
  };

  onGoToStep = ({id, name}, {prevId, prevName}) => {
    console.log(this.state.newInstance);
    console.log(`current id: ${id}, current name: ${name}, previous id: ${prevId}, previous name: ${prevName}`);
  };

  onSave = () => {
    createNewAddressSpace(this.state.newInstance)
      .then(response => {
        this.props.reload();
      })
      .finally(() => this.setState({isOpen: false}))
  };


  render() {
    const {isOpen, isConfigurationFormValid, newInstance, allStepsValid} = this.state;

    const images = {
      [BackgroundImageSrc.xs]: xsImage,
      [BackgroundImageSrc.xs2x]: xs2xImage,
      [BackgroundImageSrc.sm]: smImage,
      [BackgroundImageSrc.sm2x]: sm2xImage,
      [BackgroundImageSrc.lg]: lgImage,
      [BackgroundImageSrc.filter]: filter+"(#image_overlay)"
    };

    const steps = [
      {
        id: 1,
        name: 'Configuration',
        component: (<ConfigurationForm newInstance={newInstance}
                                       isConfigurationFormValid={isConfigurationFormValid}
                                       onChange={this.onConfigurationFormChange}/>),
        enableNext: isConfigurationFormValid
      },
      {
        id: 2,
        name: 'Review',
        component: (<Review newInstance={newInstance}/>)
      }
    ];

    return (
      <Aux>
        <Button variant="primary" onClick={this.toggleOpen}>Create</Button>
        {(
          <Wizard
            key={new Date().getTime()}
            className=".section"
            isOpen={isOpen}
            title="Create an Instance"
            onClose={this.toggleOpen}
            onSave={this.onSave}
            steps={steps}
            onNext={this.onNext}
            onBack={this.onBack}
            onGoToStep={this.onGoToStep}
            // footerRightAlign
            backgroundImgSrc={images}
            lastStepButtonText="Finish"
          />
        )}
      </Aux>
    );
  }
}

export default CreateAddressSpace;
