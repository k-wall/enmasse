/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import {
  DataToolbarGroup,
  DataToolbarFilter,
  DataToolbarItem,
  DataToolbarChip,
  DataToolbarToggleGroup,
  DataToolbar,
  DataToolbarContent
} from "@patternfly/react-core/dist/js/experimental";
import {
  Dropdown,
  DropdownToggle,
  DropdownItem,
  InputGroup,
  Button,
  ButtonVariant,
  Badge,
  Select,
  SelectVariant,
  SelectOption,
  SelectOptionObject
} from "@patternfly/react-core";
import { FilterIcon, SearchIcon } from "@patternfly/react-icons";
import { ISortBy } from "@patternfly/react-table";
import useWindowDimensions from "components/common/WindowDimension";
import { SortForMobileView } from "components/common/SortForMobileView";
import { useApolloClient } from "@apollo/react-hooks";
import {
  RETURN_ALL_CONTAINER_IDS_OF_ADDRESS_LINKS_FOR_TYPEAHEAD_SEARCH,
  RETURN_ALL_NAMES_OF_ADDRESS_LINK_FOR_TYPEAHEAD_SEARCH
} from "queries";
import { id } from "date-fns/esm/locale";
import {
  ISearchAddressLinkNameResponse,
  ISearchAddressLinkContainerResponse
} from "types/ResponseTypes";

interface IAddressLinksFilterProps {
  filterValue: string;
  setFilterValue: (value: string) => void;
  filterNames: any[];
  setFilterNames: (value: Array<any>) => void;
  filterContainers: any[];
  setFilterContainers: (value: Array<any>) => void;
  filterRole?: string;
  setFilterRole: (role: string | undefined) => void;
  totalLinks: number;
  sortValue?: ISortBy;
  setSortValue: (value: ISortBy) => void;
  addressName: string;
  addressSpaceName: string;
  namespace: string;
}
export const AddressLinksFilter: React.FunctionComponent<IAddressLinksFilterProps> = ({
  filterValue,
  setFilterValue,
  filterNames,
  setFilterNames,
  filterContainers,
  setFilterContainers,
  filterRole,
  setFilterRole,
  totalLinks,
  sortValue,
  setSortValue,
  addressName,
  addressSpaceName,
  namespace
}) => {
  const { width } = useWindowDimensions();
  const client = useApolloClient();
  const [filterIsExpanded, setFilterIsExpanded] = React.useState<boolean>(
    false
  );
  const [roleIsExpanded, setRoleIsExpanded] = React.useState<boolean>(false);
  const [isSelectNameExpanded, setIsSelectNameExpanded] = React.useState<
    boolean
  >(false);
  const [
    isSelectContainerExpanded,
    setIsSelectContainerExpanded
  ] = React.useState<boolean>(false);
  const [nameSelected, setNameSelected] = React.useState<string>();
  const [containerSelected, setContainerSelected] = React.useState<string>();
  const [nameOptions, setNameOptions] = React.useState<Array<string>>();
  const [containerOptions, setContainerOptions] = React.useState<
    Array<string>
  >();
  const [nameInput, setNameInput] = React.useState<string>("");
  const [containerInput, setContainerInput] = React.useState<string>("");
  const filterMenuItems = [
    { key: "filterName", value: "Name" },
    { key: "filterContainers", value: "Container" },
    { key: "filterRole", value: "Role" }
  ];

  const roleMenuItems = [
    { key: "roleSender", value: "Sender" },
    { key: "roleReceiver", value: "Receiver" }
  ];

  const sortMenuItems = [
    { key: "name", value: "Name", index: 2 },
    { key: "deliveryRate", value: "DeliveryRate", index: 3 },
    { key: "backlog", value: "Backlog", index: 4 }
  ];

  const onAddInput = (event: any) => {
    if (filterValue && filterValue === "Name") {
      if (nameSelected && nameSelected.trim() !== "")
        if (
          filterNames.map(filter => filter.value).indexOf(nameSelected.trim()) <
          0
        ) {
          setFilterNames([
            ...filterNames,
            { value: nameSelected.trim(), isExact: true }
          ]);
          setNameSelected(undefined);
        }
      if (!nameSelected && nameInput && nameInput.trim() !== "")
        if (
          filterNames.map(filter => filter.value).indexOf(nameInput.trim()) < 0
        )
          setFilterNames([
            ...filterNames,
            { value: nameInput.trim(), isExact: false }
          ]);
    } else if (filterValue && filterValue === "Container") {
      if (containerSelected && containerSelected.trim() !== "")
        if (
          filterContainers
            .map(filter => filter.value)
            .indexOf(containerSelected.trim()) < 0
        ) {
          setFilterContainers([
            ...filterContainers,
            { value: containerSelected.trim(), isExact: true }
          ]);
          setContainerSelected(undefined);
        }
      if (!containerSelected && containerInput && containerInput.trim() !== "")
        if (
          filterContainers
            .map(filter => filter.value)
            .indexOf(containerInput.trim()) < 0
        )
          setFilterContainers([
            ...filterContainers,
            { value: containerInput.trim(), isExact: false }
          ]);
    }
  };

  const onFilterSelect = (event: any) => {
    setFilterValue(event.target.value);
    setFilterIsExpanded(!filterIsExpanded);
  };
  const onRoleSelect = (event: any) => {
    setFilterRole(event.target.value);
    setRoleIsExpanded(!roleIsExpanded);
  };
  const onNameSelectToggle = () => {
    setIsSelectNameExpanded(!isSelectNameExpanded);
  };
  const onContainerSelectToggle = () => {
    setIsSelectContainerExpanded(!isSelectContainerExpanded);
  };
  const onChangeNameData = async (value: string) => {
    setNameOptions(undefined);
    if (value.trim().length < 6) {
      setNameOptions([]);
      return;
    }
    const response = await client.query<ISearchAddressLinkNameResponse>({
      query: RETURN_ALL_NAMES_OF_ADDRESS_LINK_FOR_TYPEAHEAD_SEARCH(
        addressName,
        namespace,
        value.trim()
      )
    });
    if (
      response &&
      response.data &&
      response.data.addresses &&
      response.data.addresses.addresses &&
      response.data.addresses.addresses.length > 0 &&
      response.data.addresses.addresses[0].links &&
      response.data.addresses.addresses[0].links.links &&
      response.data.addresses.addresses[0].links.links.length > 0
    ) {
      if (response.data.addresses.addresses[0].links.total > 100) {
        setNameOptions([]);
      } else {
        const obtainedList = response.data.addresses.addresses[0].links.links.map(
          (link: any) => {
            return link.objectMeta.Name;
          }
        );
        setNameOptions(Array.from(new Set(obtainedList)));
      }
    }
  };
  const onNameSelectFilterChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setNameInput(e.target.value);
    const data = onChangeNameData(e.target.value);
    const options: React.ReactElement[] = nameOptions
      ? nameOptions.map((option, index) => (
          <SelectOption key={index} value={option} />
        ))
      : [];
    return options;
  };

  const onChangeContainerData = async (value: string) => {
    setContainerOptions(undefined);
    if (value.trim().length < 6) {
      setContainerOptions([]);
      return;
    }
    const response = await client.query<ISearchAddressLinkContainerResponse>({
      query: RETURN_ALL_CONTAINER_IDS_OF_ADDRESS_LINKS_FOR_TYPEAHEAD_SEARCH(
        addressName,
        namespace,
        value.trim()
      )
    });
    if (
      response &&
      response.data &&
      response.data.addresses &&
      response.data.addresses.addresses &&
      response.data.addresses.addresses.length > 0 &&
      response.data.addresses.addresses[0].links &&
      response.data.addresses.addresses[0].links.links &&
      response.data.addresses.addresses[0].links.links.length > 0
    ) {
      if (response.data.addresses.addresses[0].links.total > 100) {
        setContainerOptions([]);
      } else {
        const obtainedList = response.data.addresses.addresses[0].links.links.map(
          (link: any) => {
            return link.spec.connection.spec.containerId;
          }
        );
        setContainerOptions(Array.from(new Set(obtainedList)));
      }
    }
  };

  const onContainerSelectFilterChange = (
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
    setContainerInput(e.target.value);
    const data = onChangeContainerData(e.target.value);
    const options: React.ReactElement[] = containerOptions
      ? containerOptions.map((option, index) => (
          <SelectOption key={index} value={option} />
        ))
      : [];
    return options;
  };

  const onNameSelect = (event: any, selection: string | SelectOptionObject) => {
    setNameSelected(selection.toString());
    setIsSelectNameExpanded(false);
  };

  const onContainerSelect = (
    event: any,
    selection: string | SelectOptionObject
  ) => {
    setContainerSelected(selection.toString());
    setIsSelectContainerExpanded(false);
  };

  const onDelete = (
    type: string | DataToolbarChip,
    id: string | DataToolbarChip
  ) => {
    switch (type) {
      case "Name":
        if (filterNames && id) {
          let index = filterNames
            .map(filter => filter.value)
            .indexOf(id.toString());
          if (index >= 0) filterNames.splice(index, 1);
          setFilterNames([...filterNames]);
        }
        break;
      case "Container":
        if (filterContainers && id) {
          let index = filterContainers
            .map(filter => filter.value)
            .indexOf(id.toString());
          if (index >= 0) filterContainers.splice(index, 1);
          setFilterContainers([...filterContainers]);
        }
        break;
      case "Role":
        setFilterRole(undefined);
        break;
    }
  };
  const checkIsFilterApplied = () => {
    if (
      (filterNames && filterNames.length > 0) ||
      (filterContainers && filterContainers.length > 0) ||
      (filterRole && filterRole.trim() !== "")
    ) {
      return true;
    }
    return false;
  };
  const onDeleteAll = () => {
    setFilterValue("Name");
    setFilterNames([]);
    setFilterContainers([]);
    setFilterRole(undefined);
  };

  const toggleGroupItems = (
    <>
      <DataToolbarGroup variant="filter-group">
        <DataToolbarFilter categoryName="Filter">
          <Dropdown
            id="ad-links-filter-dropdown"
            position="left"
            onSelect={onFilterSelect}
            isOpen={filterIsExpanded}
            toggle={
              <DropdownToggle onToggle={setFilterIsExpanded}>
                <FilterIcon />
                &nbsp;
                {filterValue}
              </DropdownToggle>
            }
            dropdownItems={filterMenuItems.map(option => (
              <DropdownItem
                id={`ad-links-filter-dropdown-item${option.key}`}
                key={option.key}
                value={option.value}
                itemID={option.key}
                component={"button"}
              >
                {option.value}
              </DropdownItem>
            ))}
          />
        </DataToolbarFilter>
        <>
          <DataToolbarItem>
            <DataToolbarFilter
              chips={filterNames.map(filter => filter.value)}
              deleteChip={onDelete}
              categoryName="Name"
            >
              {filterValue && filterValue === "Name" && (
                <InputGroup>
                  <Select
                    id="ad-links-filter-select-name"
                    variant={SelectVariant.typeahead}
                    aria-label="Select a Name"
                    onToggle={onNameSelectToggle}
                    onSelect={onNameSelect}
                    onClear={() => {
                      setNameSelected(undefined);
                      setIsSelectNameExpanded(false);
                    }}
                    maxHeight="200px"
                    selections={nameSelected}
                    onFilter={onNameSelectFilterChange}
                    isExpanded={isSelectNameExpanded}
                    ariaLabelledBy={"typeahead-select-id"}
                    placeholderText="Select name"
                    isDisabled={false}
                    isCreatable={false}
                  >
                    {nameOptions && nameOptions.length > 0 ? (
                      nameOptions.map((option, index) => (
                        <SelectOption key={index} value={option} />
                      ))
                    ) : nameInput.trim().length < 5 ? (
                      <SelectOption
                        key={"invalid-input-length"}
                        value={"Enter more characters"}
                        disabled={true}
                      />
                    ) : (
                      <SelectOption
                        key={"no-results-found"}
                        value={"No results found"}
                        disabled={true}
                      />
                    )}
                    {/* {} */}
                  </Select>
                  <Button
                    id="ad-links-filter-search-name"
                    variant={ButtonVariant.control}
                    aria-label="search button for search name"
                    onClick={onAddInput}
                  >
                    <SearchIcon />
                  </Button>
                </InputGroup>
              )}
            </DataToolbarFilter>
          </DataToolbarItem>
          <DataToolbarItem>
            <DataToolbarFilter
              chips={filterContainers.map(filter => filter.value)}
              deleteChip={onDelete}
              categoryName="Container"
            >
              {filterValue && filterValue === "Container" && (
                <InputGroup>
                  <Select
                    id="ad-links-filter-select-container"
                    variant={SelectVariant.typeahead}
                    aria-label="Select a Container"
                    onToggle={onContainerSelectToggle}
                    onSelect={onContainerSelect}
                    onClear={() => {
                      setContainerSelected(undefined);
                      setIsSelectContainerExpanded(false);
                    }}
                    maxHeight="200px"
                    selections={containerSelected}
                    onFilter={onContainerSelectFilterChange}
                    isExpanded={isSelectContainerExpanded}
                    ariaLabelledBy={"typeahead-select-id"}
                    placeholderText="Select container"
                    isDisabled={false}
                    isCreatable={false}
                  >
                    {containerOptions && containerOptions.length > 0 ? (
                      containerOptions.map((option, index) => (
                        <SelectOption key={index} value={option} />
                      ))
                    ) : containerInput.trim().length < 5 ? (
                      <SelectOption
                        key={"invalid-input-length"}
                        value={"Enter more characters"}
                        disabled={true}
                      />
                    ) : (
                      <SelectOption
                        key={"no-results-found"}
                        value={"No results found"}
                        disabled={true}
                      />
                    )}
                    {/* {} */}
                  </Select>
                  <Button
                    id="ad-links-filter-search-container"
                    variant={ButtonVariant.control}
                    aria-label="search button for search containers"
                    onClick={onAddInput}
                  >
                    <SearchIcon />
                  </Button>
                </InputGroup>
              )}
            </DataToolbarFilter>
          </DataToolbarItem>

          <DataToolbarItem>
            <DataToolbarFilter
              chips={filterRole ? [filterRole] : []}
              deleteChip={onDelete}
              categoryName="Role"
            >
              {filterValue === "Role" && (
                <Dropdown
                  id="ad-links-filter-select-role"
                  position="left"
                  onSelect={onRoleSelect}
                  isOpen={roleIsExpanded}
                  toggle={
                    <DropdownToggle onToggle={setRoleIsExpanded}>
                      {filterRole || "Select Role"}
                    </DropdownToggle>
                  }
                  dropdownItems={roleMenuItems.map(option => (
                    <DropdownItem
                      id={`ad-links-filter-select-option-role${option.key}`}
                      key={option.key}
                      value={option.value}
                      itemID={option.key}
                      component={"button"}
                    >
                      {option.value}
                    </DropdownItem>
                  ))}
                />
              )}
            </DataToolbarFilter>
          </DataToolbarItem>
        </>
      </DataToolbarGroup>
    </>
  );

  return (
    <DataToolbar
      id="data-toolbar-with-filter"
      className="pf-m-toggle-group-container"
      collapseListedFiltersBreakpoint="xl"
      clearAllFilters={onDeleteAll}
    >
      <DataToolbarContent>
        <>
          <DataToolbarToggleGroup
            toggleIcon={
              <>
                <FilterIcon />
                {checkIsFilterApplied() && (
                  <Badge key={1} isRead>
                    {totalLinks}
                  </Badge>
                )}
              </>
            }
            breakpoint="xl"
          >
            {toggleGroupItems}
          </DataToolbarToggleGroup>
          {width < 769 && (
            <SortForMobileView
              sortMenu={sortMenuItems}
              sortValue={sortValue}
              setSortValue={setSortValue}
            />
          )}
        </>
      </DataToolbarContent>
    </DataToolbar>
  );
};
