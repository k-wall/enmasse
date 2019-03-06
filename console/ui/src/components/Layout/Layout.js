import React from 'react';
import {
  Avatar,
  BackgroundImage,
  BackgroundImageSrc,
  Button,
  ButtonVariant,
  Dropdown,
  DropdownToggle,
  DropdownItem,
  DropdownSeparator,
  KebabToggle,
  Nav,
  NavItem,
  NavList,
  NavVariants,
  Page,
  PageHeader,
  PageSection,
  PageSectionVariants,
  TextContent,
  Text,
  Toolbar,
  ToolbarGroup,
  ToolbarItem
} from '@patternfly/react-core';
import accessibleStyles from '@patternfly/patternfly/utilities/Accessibility/accessibility.css';
import spacingStyles from '@patternfly/patternfly/utilities/Spacing/spacing.css';
import { css } from '@patternfly/react-styles';
import { BellIcon, CogIcon } from '@patternfly/react-icons';

import avatarImg from "../../assets/images/img_avatar.svg";
import xsImage from '../../assets/images/pfbg_576.jpg';
import xs2xImage from '../../assets/images/pfbg_576@2x.jpg';
import smImage from '../../assets/images/pfbg_768.jpg';
import sm2xImage from '../../assets/images/pfbg_768@2x.jpg';
import lgImage from '../../assets/images/pfbg_1200.jpg';
import filter from '../../assets/images/background-filter.svg';

class Layout extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isDropdownOpen: false,
      isKebabDropdownOpen: false,
      activeItem: 0
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

  render() {
    const { isDropdownOpen, isKebabDropdownOpen, activeItem } = this.state;

    const PageNav = (
      <Nav onSelect={this.onNavSelect} aria-label="Nav">
        <NavList variant={NavVariants.horizontal}>
          <NavItem to="#nav-link1" itemId={0} isActive={activeItem === 0}>
            Total Instances
          </NavItem>
          <NavItem to="#nav-link2" itemId={1} isActive={activeItem === 1}>
            Standard Address Spaces
          </NavItem>
          <NavItem to="#nav-link3" itemId={2} isActive={activeItem === 2}>
            Brokered Address Spaces
          </NavItem>
        </NavList>
      </Nav>
    );
    const kebabDropdownItems = [
      <DropdownItem>
        <BellIcon /> Notifications
      </DropdownItem>,
      <DropdownItem>
        <CogIcon /> Settings
      </DropdownItem>
    ];
    const userDropdownItems = [
      <DropdownItem>Link</DropdownItem>,
      <DropdownItem component="button">Action</DropdownItem>,
      <DropdownItem isDisabled>Disabled Link</DropdownItem>,
      <DropdownItem isDisabled component="button">
        Disabled Action
      </DropdownItem>,
      <DropdownSeparator />,
      <DropdownItem>Separated Link</DropdownItem>,
      <DropdownItem component="button">Separated Action</DropdownItem>
    ];
    const PageToolbar = (
      <Toolbar>
        <ToolbarGroup className={css(accessibleStyles.screenReader, accessibleStyles.visibleOnLg)}>
          <ToolbarItem>
            <Button id="horizontal-example-uid-01" aria-label="Notifications actions" variant={ButtonVariant.plain}>
              <BellIcon />
            </Button>
          </ToolbarItem>
          <ToolbarItem>
            <Button id="horizontal-example-uid-02" aria-label="Settings actions" variant={ButtonVariant.plain}>
              <CogIcon />
            </Button>
          </ToolbarItem>
        </ToolbarGroup>
        <ToolbarGroup>
          <ToolbarItem className={css(accessibleStyles.hiddenOnLg, spacingStyles.mr_0)}>
            <Dropdown
              isPlain
              position="right"
              onSelect={this.onKebabDropdownSelect}
              toggle={<KebabToggle onToggle={this.onKebabDropdownToggle} />}
              isOpen={isKebabDropdownOpen}
              dropdownItems={kebabDropdownItems}
            />
          </ToolbarItem>
          <ToolbarItem className={css(accessibleStyles.screenReader, accessibleStyles.visibleOnMd)}>
            <Dropdown
              isPlain
              position="right"
              onSelect={this.onDropdownSelect}
              isOpen={isDropdownOpen}
              toggle={<DropdownToggle onToggle={this.onDropdownToggle}>Developer</DropdownToggle>}
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
      [BackgroundImageSrc.filter]: `{filter}#image_overlay`
    };

    const Header = (
      <PageHeader
        logo={"Console"}
        toolbar={PageToolbar}
        avatar={<Avatar src={avatarImg} alt="Avatar image" />}
        topNav={PageNav}
      />
    );

    console.log(avatarImg);

    return (
      <React.Fragment>
        <BackgroundImage src={bgImages} />
        <Page header={Header}>
          <PageSection variant={PageSectionVariants.light}>
            <TextContent>
              <Text component="h1">Enmasse Console</Text>
            </TextContent>
          </PageSection>
        </Page>
      </React.Fragment>
    );
  }
}

export default Layout;
