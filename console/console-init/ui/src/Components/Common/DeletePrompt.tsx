import * as React from "react";
import { Modal, Button } from "@patternfly/react-core";

interface IDeleteProps {
  header: string;
  detail: string;
  name: string;
  handleCancelDelete: () => void;
  handleConfirmDelete: () => void;
}
export const DeletePrompt: React.FunctionComponent<IDeleteProps> = ({
  header,
  detail,
  name,
  handleCancelDelete,
  handleConfirmDelete
}) => {
  return (
    <Modal
      isSmall={true}
      title={header}
      isOpen={true}
      onClose={handleCancelDelete}
      actions={[
        <Button key="Delete" variant="primary" onClick={handleConfirmDelete}>
          Confirm
        </Button>,
        <Button key="cancel" variant="link" onClick={handleCancelDelete}>
          Cancel
        </Button>
      ]}
      isFooterLeftAligned={true}
    >
      <b>{name}</b>
      <br />
      {detail}
    </Modal>
  );
};
