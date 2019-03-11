#!/usr/bin/env bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
TARGET_DIR=${1-/apps}

export COOKIE_SECRET=$(python -c 'import os,base64; print base64.b64encode(os.urandom(16))')

find ${SCRIPT_DIR}/.. -type d
mkdir -p ${TARGET_DIR}
tar -cf - -C ${SCRIPT_DIR}/.. . | tar -xf - --no-overwrite-dir -C ${TARGET_DIR}

for c in $(find ${TARGET_DIR} -name "*.cfg" -type f)
do
  ex ${c} << EOF
%!envsubst
wq
EOF
done
