import React from 'react';
import {
  Avatar,
  BackgroundImage,
  BackgroundImageSrc,
  Brand,
  Button,
  ButtonVariant,
  Dropdown,
  DropdownToggle,
  DropdownItem,
  List,ListItem,
  Nav,
  NavItem,
  NavList,
  NavVariants,
  Page,
  PageHeader,
  PageSection,
  PageSectionVariants,
  TextContent,
  Toolbar,
  ToolbarGroup,
  ToolbarItem
} from '@patternfly/react-core';
import accessibleStyles from '@patternfly/patternfly/utilities/Accessibility/accessibility.css';
import { css } from '@patternfly/react-styles';
import Aux from '../../hoc/Aux/Aux';

import avatarImg from "../../assets/images/img_avatar.svg";
import xsImage from '../../assets/images/pfbg_576.jpg';
import xs2xImage from '../../assets/images/pfbg_576@2x.jpg';
import smImage from '../../assets/images/pfbg_768.jpg';
import sm2xImage from '../../assets/images/pfbg_768@2x.jpg';
import lgImage from '../../assets/images/pfbg_1200.jpg';
import filter from '../../assets/images/background-filter.svg';

import brandImg from '../../assets/images/logo.svg';

import './Layout.css';

class Layout extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isDropdownOpen: false,
      isKebabDropdownOpen: false,
      activeItem: 0,
    };
  }

  onDropdownToggle = isDropdownOpen => {
    this.setState({
      isDropdownOpen
    });
  };

  onDropdownSelect = event => {
    this.setState({
      isDropdownOpen: !this.state.isDropdownOpen
    });
  };

  onKebabDropdownToggle = isKebabDropdownOpen => {
    this.setState({
      isKebabDropdownOpen
    });
  };

  onKebabDropdownSelect = event => {
    this.setState({
      isKebabDropdownOpen: !this.state.isKebabDropdownOpen
    });
  };

  onNavSelect = result => {
    this.setState({
      activeItem: result.itemId
    });
  };

  convertCamelCaseToTitle(text) {
    var result = text.replace( /([A-Z])/g, " $1" );
    return result.charAt(0).toUpperCase() + result.slice(1);
  }

  render() {
    const { isDropdownOpen, activeItem } = this.state;

    var style = {
      textAlign: 'center',
    };

    const navItems = Object.keys(this.props.instanceTypes)
      .map((key, i )=> {
          return <NavItem to={"#nav-link"+i} key={i} itemId={i} isActive={activeItem === i}>
            <div style={style}>{this.props.instanceTypes[key]}<br/>{this.convertCamelCaseToTitle(key)}</div>
          </NavItem>
        });

    const PageNav = (
      <Nav onSelect={this.onNavSelect} aria-label="Nav">
        <NavList variant={NavVariants.horizontal}>
          {navItems}
        </NavList>
      </Nav>
    );
    const userDropdownItems = [
      <DropdownItem isDisabled>Logout</DropdownItem>
    ];
    const PageToolbar = (
      <Toolbar>
        <ToolbarGroup>
          <ToolbarItem className={css(accessibleStyles.screenReader, accessibleStyles.visibleOnMd)}>
            <Dropdown
              isPlain
              position="right"
              onSelect={this.onDropdownSelect}
              isOpen={isDropdownOpen}
              toggle={<DropdownToggle onToggle={this.onDropdownToggle}>{this.props.user}</DropdownToggle>}
              dropdownItems={userDropdownItems}
            />
          </ToolbarItem>
        </ToolbarGroup>
      </Toolbar>
    );

    const bgImages = {
      [BackgroundImageSrc.xs]: xsImage,
      [BackgroundImageSrc.xs2x]: xs2xImage,
      [BackgroundImageSrc.sm]: smImage,
      [BackgroundImageSrc.sm2x]: sm2xImage,
      [BackgroundImageSrc.lg]: lgImage,
      [BackgroundImageSrc.filter]: filter
    };

    const Header = (
      <PageHeader
        logo={<Brand src={brandImg} alt="Console Logo" />}
        toolbar={PageToolbar}
        avatar={<Avatar src={avatarImg} alt="Avatar image" />}
      />
    );

    return (
      <Aux>
        <BackgroundImage src={bgImages} />
        <Page header={Header}>
          <PageSection variant={PageSectionVariants.darker} className='navSection'>{PageNav}</PageSection>
          <PageSection variant={PageSectionVariants.light}>
            <TextContent>
              {this.props.children}
            </TextContent>
          </PageSection>

        </Page>
      </Aux>
    );
  }
}

export default Layout;
