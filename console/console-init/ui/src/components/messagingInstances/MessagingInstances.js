import React from 'react';
import { Badge, Text } from '@patternfly/react-core';
import { Table, TableVariant, TableHeader, TableBody } from '@patternfly/react-table';
import moment from 'moment';

import { deleteMessagingInstances } from './messagingInstance/enmasse/EnmasseAddressSpaces';

import Aux from '../../hoc/Aux/Aux';



class NamespaceTable extends React.Component {


  constructor(props) {
    super(props);

    const del = deleteMessagingInstances;

    this.state = {
      columns: [{ title: 'Name/Namespace' }, { title: 'Type' }, 'Time Created'],
      messagingSpaces: [],
      rows: [],
      actions: [
        {
          title: 'Delete',
          onClick: (event, rowId, rowData, extra) =>
            // console.log('deleting: ', rowData.cells[0].title.props.children[0].props.children)
             del(rowData.cells[0].title.props.children[0].props.children, rowData.cells[0].title.props.children[1].props.children)
               .then(response => console.log('DELETE success', response))
        }
      ]
    };
    this.onSelect = this.onSelect.bind(this);
  }

  getMessagingSpacesCells(messagingInstances) {
    var styleOrange = {
      backgroundColor: '#FFA300',
      fontSize: 'var(--pf-c-table-cell--FontSize)',
      fontweight: 'var(--pf-c-table-cell--FontWeight)',
    };
    //https://github.com/patternfly/patternfly-react/issues/1482 no verticle align
    if (messagingInstances) {
      let instances =[...messagingInstances];
      let newMap = instances.map(space => ({
         cells: [{title: <Aux><a>{space.name}</a><Text>{space.namespace}</Text></Aux>}, {
           title: <Aux><Badge style={styleOrange}>{space.component}</Badge> {space.type}</Aux>
         }, <Aux>{moment(space.timeCreated).fromNow()}</Aux>]
      }));
      return newMap;
    }
    return [];
  }

  componentDidUpdate(prevProps, prevState, snapshot) {
    // deleleMessagingInstances('xxx');

    if (prevProps.messagingInstances !== this.props.messagingInstances) {
      let cells = this.getMessagingSpacesCells(this.props.messagingInstances);
      this.setState({rows: cells})
    }

  }

  onSelect(event, isSelected, rowId) {
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

  render() {
    const { actions, columns, rows } = this.state;
    return (
      <Table aria-label="table of messaging instances" variant={TableVariant.compact} onSelect={this.onSelect} cells={columns} rows={rows} actions={actions} >
        <TableHeader />
        <TableBody />
      </Table>
    );
  }
}

export default NamespaceTable;
