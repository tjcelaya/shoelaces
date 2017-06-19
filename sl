#!/bin/sh
command -v java >/dev/null 2>&1 || {
    echo >&2 "Can't find java. Aborting."; exit 1; }

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
JAR="$DIR/sl.jar"

test -f "$JAR" >/dev/null 2>&1 || {
    echo >&2 "Can't find sl (shoelaces). Aborting."; exit 2; }

java -jar "$JAR"
