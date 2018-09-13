# FSSI
Financial Standard Service Infrastructure.

## Build

### Prerequisite: 
1. JDK8
2. SBT
3. Scala 2.12, Optional, SBT will install it automatically

### Scripts

change current directory to project root, then run the scripts to build project.

```bash
# build core node
sbt ';project coreNode; assembly'

# build tools
sbt ';project tool; assembly'

# build edge node
sbt ';project edgeNode; assembly'
```



### Output

After building, we will get some artifacts:

1. *Core Node* executable command: `./core-node/target/scala-2.12/coreNode`
2. *Tool* executable command: `./tool/target/scala-2.12/tool`
3. *Edge Node* executable command: `./edge-node/target/scala-2.12/edgeNode`

## Quick Start

### Get Help Info

Every output executable command has a command line argument named `-h` or `--help ` , such as:

```bash
./core-node/target/scala-2.12/coreNode -h
corenode 0.1.0
Usage: corenode [options]

  -h, --help               print this help messages
  -w, --working-dir <value>
                           working directory
  -p, --password <value>   password of the bound account
```

### Create Account

*Account* is the user ID in `FSSI`,  and every node should bind an account, the account `ID` will be used as the P2P node ID.

We can use `tool CreateAccount` to generate an account.

> For convenience, we can link the `./tool/target/scala-2.12/tool` and other artifacts to `$PATH`, so that we can simply use them anywhere in terminal.

Assume we have linked the `tool` artifact with `fssi_tool`.

Now, we can use `fssi_tool` to create an *Account*:

```bash
fssi_tool CreateAccount -p 'passw0rd'
```

then, you'll see:

```json
{
  "publicKey" : "0x3059301306072a8648ce3d020106082a8648ce3d030107034200043fe56f382de970fa5b9150df28866bbad2611f8e6adcc4432cd855eff0d0f5a3c4441a36cc22ff4530bb366edb2e611600aafcdd5813770010657c903d6ff850",
  "encryptedPrivateKey" : "0x8691f98b497672ff30448dca294e54e5b69ff5eee9043ddb0dd40a4e9af331bb72dd65944e86d980a8a2ccf001d04b639d03b2b67936101f6c1a76b10c2979c2615ff0a09166684c4b77b19958797eff234a8c4bd8bbb4e56d0693c826aee0d68be356a50f95b8a8ec3ca4e37695ba1acbd12e54b07f47fbaf987a353d442fe3c382dfaf998f3dbc00a8b9bfa6ac6000e89aec83d5f2fa48",
  "iv" : "0x6e727479756f6f78"
}
```

It's a json, we can save it.

### Create a block chain

We can use `fssi_tool CreateChain` to create a `fssi` chain.

```bash
fssi_tool CreateChain -d ./testnet -id testnet
```

After that, you'll see log:

```bash
16:15:26.385 [main] INFO fssi.interpreter.BlockStoreHandler - saved current height: 0
16:15:26.453 [main] INFO fssi.interpreter.BlockStoreHandler - saved block for 0
16:15:26.456 [main] INFO fssi.ast - chain initialized, please edit the default config file: ./testnet/testnet/fssi.conf
16:15:26.456 [main] INFO fssi.tool.handler.package$createChain$ - created
```

Now, you can run `coreNode` at the `./testnet` directory.

### Run core node

`Core Node` is the validating node, which will participate the consensus procedure, and persist the `block` that is agreed by the hold block chain network.

**Prerequisite** the working director is ready, if not, please use `fssi_tool CreateChain` to create one.

We can use `corenode` to start the core node.

```bash
corenode -w ./testnet/ -p passw0rd
```

