# FSSI

[![Build Status](https://travis-ci.org/bigknife/fssi.svg?branch=master)](https://travis-ci.org/bigknife/fssi)
[![codecov](https://codecov.io/gh/bigknife/fssi/branch/master/graph/badge.svg)](https://codecov.io/gh/bigknife/fssi)
[![Gitter chat](https://badges.gitter.im/gitterHQ/gitter.png)](https://gitter.im/fssi-talking/Lobby)

A block chain for finance service infrastucture.

## Usage

use `-h` to see help message.

```bash
FSSI_JAR=./target/scala-2.12/fssi-fssi-0.0.0-SNAPSHOT
java -jar $FSSI_JAR -h
```

### Nymph Server

use `nymph` command to startup Nymph Server.

```bash
FSSI_JAR=./target/scala-2.12/fssi-fssi-0.0.0-SNAPSHOT
java -jar $FSSI_JAR nymph
```

Some important arguments:

1. `--working-dir` nymph working root directory.
2. `--port` the port which nymph jsonrpc server is listening on.
3. `--snapshot-db-port` the port which snapdb is listening on.
4. `--node-port` the port which the p2p node is listening on.
5. `--warrior-nodes` the backend `Warrior Node` list, eg: 10.65.209.78:28080,10.65.209.77:28081

After startup, we can visit the jsonrpc service via urls prefixed with
`http://<ip>:<port>/jsonrpc/nympy/v1`.