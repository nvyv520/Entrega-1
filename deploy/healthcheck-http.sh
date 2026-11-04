#!/usr/bin/env bash
set -eu

# Check an internal HTTP endpoint without installing curl in the Java images.
port="${1:?HTTP port is required}"
request_path="${2:-/v3/api-docs}"
exec 3<>/dev/tcp/127.0.0.1/"$port"
printf 'GET %s HTTP/1.0\r\nHost: localhost\r\nConnection: close\r\n\r\n' "$request_path" >&3
IFS= read -r response <&3
[[ "$response" == HTTP/*" 200 "* ]]
