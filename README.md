# ShoeLaces

Lace up your threads.

[![Build Status](https://travis-ci.org/tjcelaya/shoelaces.svg?branch=master)](https://travis-ci.org/tjcelaya/shoelaces)

[test output](./public/co.tjcelaya.shoelaces.html)

[coverage](./public/coverage/)

CLI tool for managing mental threads

Features:
 - human-readable JSON storage
 - `SHOELACES_HOME` env defaults to `$PWD` and is used to find files
 - `SHOELACES_FILE` env defaults to current date stamped file `YYYY-MM-DD.sldb` and will be created if missing

Usage:
```
usage: sl [-h] [-s|-k|-i|-ret [THREAD]] [-p|-r]
 -h                     help
 -i,--interrupt <arg>   run a new PRIMARY thread
 -k,--kill <arg>        kill a thread
 -p,--pause             pause (background) the PRIMARY thread
 -r,--resume            resume (foreground) the PRIMARY thread
 -ret,--return <arg>    exit the PRIMARY thread and return to <arg>, if
                        given
 -s,--spawn <arg>       spawn a new thread
```
