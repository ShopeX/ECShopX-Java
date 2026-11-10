#!/bin/bash
set -euo pipefail
mkdir -p /usr/share/elasticsearch/data /usr/share/elasticsearch/logs
chown -R elasticsearch:elasticsearch /usr/share/elasticsearch/data /usr/share/elasticsearch/logs
exec runuser -u elasticsearch -- "$@"
