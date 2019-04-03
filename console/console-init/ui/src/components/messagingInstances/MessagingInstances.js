import React from 'react';
import {
  Badge,
  Card,
  CardBody,
  CardHeader,
  Pagination, PaginationVariant,
  Text,
  Toolbar,
  ToolbarGroup,
  ToolbarItem
} from '@patternfly/react-core';
import {Table, TableVariant, TableHeader, TableBody} from '@patternfly/react-table';


import {css} from '@patternfly/react-styles';
import spacingStyles from '@patternfly/patternfly/utilities/Spacing/spacing.css';

import {deleteMessagingInstances} from './messagingInstance/enmasse/EnmasseAddressSpaces';

import moment from 'moment';

import Aux from '../../hoc/Aux/Aux';
import CreateAddressSpace from './messagingInstance/enmasse/CreateAddressSpace/CreateAddressSpace';

import FilterTypes from './FilterTypes/FilterTypes';

class MessagingInstances extends React.Component {


  constructor(props) {
    super(props);

    const del = deleteMessagingInstances;

    this.state = {
      page: 1,
      perPage: 5,
      columns: [{title: 'Name/Namespace'}, {title: 'Type'}, 'Time Created'],
      messagingInstances: [],
      rows: [],
      actions: [
        {
          title: 'Delete',
          onClick: (event, rowId, rowData, extra) =>
            del(rowData.cells[0].title.props.children[0].props.children, rowData.cells[0].title.props.children[1].props.children)
              .then(response => {
                console.log('DELETE success', response);
                this.reload();
              })
        }
      ],
    };
    this.onMessagingInstanceSelect = this.onMessagingInstanceSelect.bind(this);
  }

  componentDidUpdate(prevProps, prevState, snapshot) {
    if (prevProps.messagingInstances !== this.props.messagingInstances) {
      this.setState({messagingInstances: this.props.messagingInstances});
      this.updateRows(this.props.messagingInstances, this.state.page, this.state.perPage);
    }
  }

  onSetPage = (_event, pageNumber) => {
    this.setState({
      page: pageNumber
    });
    this.updateRows(this.props.messagingInstances, pageNumber, this.state.perPage);
  }

  onPerPageSelect = (_event, perPage) => {
    this.setState({
      perPage: perPage
    });
    this.updateRows(this.props.messagingInstances, this.state.page, perPage);
  }

  updateRows(instances, page, perPage) {
    let visibleInstances = this.getVisibleMessagingInstances(instances, page, perPage);
    this.setState({rows: this.getMessagingInstanceCells(visibleInstances)});
  }

  getVisibleMessagingInstances(messagingInstances, page, perPage) {
    let instances = [...messagingInstances];
    let end = page * perPage;
    let start = end - perPage;
    let visibleInstances = instances.slice(start, Math.min(end, instances.length));
    return visibleInstances;
  }

  reload = () => {
    this.props.reloadMessagingInstances();
  }

  getMessagingInstanceCells(instances, page, perPage) {
    var styleOrange = {
      backgroundColor: '#FFA300',
      fontSize: 'var(--pf-c-table-cell--FontSize)',
      fontweight: 'var(--pf-c-table-cell--FontWeight)',
    };
    //https://github.com/patternfly/patternfly-react/issues/1482 no verticle align
    if (instances) {
      let newMap = instances.map(space => ({
        cells: [{title: <Aux><a>{space.name}</a><Text>{space.namespace}</Text></Aux>}, {
          title: <Aux><Badge style={styleOrange}>{space.component}</Badge> {space.type}</Aux>
        }, <Aux>{moment(space.timeCreated).fromNow()}</Aux>]
      }));
      return newMap;
    }
    return [];
  }

  onMessagingInstanceSelect(event, isSelected, rowId) {
    let rows;
    if (rowId === -1) {
      rows = this.state.rows.map(oneRow => {
        oneRow.selected = isSelected;
        return oneRow;
      });
    } else {
      rows = [...this.state.rows];
      rows[rowId].selected = isSelected;
    }
    this.setState({
      rows
    });
  }

  onFilterSelect = (name) => {
    this.setState({selectedFilterType: name});
  }

  render() {
    const {isOpen, selectedFilterType} = this.state;

    const {actions, columns, rows} = this.state;
    return (
      <Card>
        <CardHeader>
          <Toolbar className={"pf-u-justify-content-space-between pf-u-mx-xl pf-u-my-md"}>
            <ToolbarGroup>
              <ToolbarItem>
                {/*<FilterTypes*/}
                {/*onFilterSelect={this.onFilterSelect}*/}
                {/*/>*/}
              </ToolbarItem>
              <ToolbarItem className={css(spacingStyles.mxMd)}>
                <CreateAddressSpace reload={this.reload}/>
              </ToolbarItem>
            </ToolbarGroup>
            <ToolbarGroup>
              <ToolbarItem>
                <Pagination
                  itemCount={this.state.messagingInstances.length}
                  perPage={this.state.perPage}
                  page={this.state.page}
                  onSetPage={this.onSetPage}
                  widgetId="pagination-options-menu-top"
                  onPerPageSelect={this.onPerPageSelect}
                />
              </ToolbarItem>
            </ToolbarGroup>
          </Toolbar>
        </CardHeader>
        <CardBody>
          <Table aria-label="table of messaging instances" onSelect={this.onMessagingInstanceSelect}
                 cells={columns} rows={rows} actions={actions}>
            <TableHeader/>
            <TableBody/>
          </Table>

          <Toolbar className={"pf-u-justify-content-flex-end pf-u-mx-xl pf-u-my-md"}>
            <ToolbarGroup>
              <ToolbarItem>
                <Pagination
                  itemCount={this.state.messagingInstances.length}
                  perPage={this.state.perPage}
                  page={this.state.page}
                  onSetPage={this.onSetPage}
                  widgetId="pagination-options-menu-bottom"
                  variant={PaginationVariant.bottom}
                  onPerPageSelect={this.onPerPageSelect}
                />
              </ToolbarItem>
            </ToolbarGroup>
          </Toolbar>
        </CardBody>
      </Card>
    );
  }
}

export default MessagingInstances;
