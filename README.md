# FSSI
A block chain system running on JVM

# 运行指南

## 前置条件

### JDK
* 版本：`JDK 8 以上`
* 安装参考：[https://docs.oracle.com/javase/8/docs/technotes/guides/install/install_overview.html](https://docs.oracle.com/javase/8/docs/technotes/guides/install/install_overview.html)
* 校验命令
```
java -version

类似显示:
java version "1.8.0_91"
Java(TM) SE Runtime Environment (build 1.8.0_91-b14)
Java HotSpot(TM) 64-Bit Server VM (build 25.91-b14, mixed mode)
```

### SBT
* 版本：`1.x`
* 安装参考: [https://www.scala-sbt.org/1.x/docs/Setup.html](https://www.scala-sbt.org/1.x/docs/Setup.html)
* 校验命令
```
sbt sbtVersion

类似显示:
[info] Loading settings for project global-plugins from idea.sbt ...
[info] Loading global plugins from /Root/Users/.sbt/1.0/plugins
[info] Loading project definition from /Root/Users/project
[info] Set current project to user (in build file:/Users/user/)
[info] 1.2.0
```

## 生成操作命令
* 下载仓库
```
git clone https://github.com/bigknife/fssi.git
```
* 生成命令
进入项目的下载目录运行下面的命令:
```
sbt packInstallAll
```
成功执行会生成`tool`,`corenode`,`edgenode`三个命令,可以运行对应命令配上`--help`参数查看帮助文档

## 创建账户
* 运行命令，示例如下:
```
tool CreateAccount -s account -af /tmp/account/account.json -kf /tmp/account/account_secret.json

参数说明:
s: 随机种子
af: 账户信息存储文件绝对路径
kf: 账户私钥存储文件绝对路径

```

* 账户文件示例
```
account.json文件:
{
  "encPrivKey" : "8kRNtH14ezpwhrX2KTP9NhG4Yd6UdT5YjibvB2MMeU83teZ1urhaoAT",
  "pubKey" : "24UY6SpsGVmwYNufBnnWH8gFdXGb7fzES8tVURWe4LPEG",
  "iv" : "L9WPFRYttXQ",
  "id" : "19hJizKttFQ3a9kKpdeTvvQEDgCnTGBmtU"
}
```
```
account_secret.json文件:
G7TQb35W8FUURtDTQDbvkpYEoxHKBm9rXZVoHemhiNs4
```

## 初始化链
* 运行命令,示例如下:
```
tool CreateChain -d /tmp/block/ -id testNet

参数说明:
-d: 链数据存储绝对路径
-id: 链标识
```
执行成功会在存储路径下生成`db`文件夹和`fssi.conf`文件,其中`db`文件夹为链内置数据库存储目录，`fssi.conf`为链的配置信息

## 启动节点

### 启动CoreNode
* 拷贝链根目录到`/tmp/block/core_node`，编辑`fssi.conf`文件并修改,示例如下:
```
// config sample
chainId = "testNet"

// as a core-node 
core-node {
  // functioning mode, full or semi
  mode = "full" //semi just participate in consensus not handle edge node request

  // consensus network config
  consensus-network {
    host = "192.168.1.11"
    port = 9400

    // seeds, empty array indicates that it's a seed node
    // or else set other core node consensus network host and port
    // seeds = ["192.168.1.11:9401"]
    seeds = []

    // bound account copy from account.json and account_secret.json file
    account  {
      encPrivKey = "8kRNtH14ezpwhrX2KTP9NhG4Yd6UdT5YjibvB2MMeU83teZ1urhaoAT"
      pubKey = "24UY6SpsGVmwYNufBnnWH8gFdXGb7fzES8tVURWe4LPEG"
      iv = "L9WPFRYttXQ"
      id = "19hJizKttFQ3a9kKpdeTvvQEDgCnTGBmtU"
      secretKey = "G7TQb35W8FUURtDTQDbvkpYEoxHKBm9rXZVoHemhiNs4"
    }

    // scp protocol config
    scp  {
      // current node id is the bound account's public key.
      quorums = {
        // quorum threashold
        threshold = 1

        // quorum members, an arry of node id (bound account's public key)
        validators = ["28ZX3S6nQyk372CMUotnby35hWTkeqMToict9AQrMM4Bp","p5uk2L7imv3Yfr32seuC7qkQCiQooFBBxPYkmPENo18h"]

        //optional nested quorums 
        // the element of a nested quorum should not have innerSets
        innerSets = [
          {
            threshold = 0
            validators = ["", ""]
          },
          {
            threshold = 0
            validators = ["", ""]
          }
        ]
      }

      // max timeout seconds
      maxTimeoutSeconds = 1800

      // max nominating times for every slotIndex
      maxNominatingTimes = 50

      // millis to broadcast all of nominate and ballot message
      broadcastTimeout = 2000

      // max transaction size in block
      maxTransactionSizeInBlock = 2
      
      // max timeout seconds for consensus to calculate transactions
      maxConsensusWaitTimeout = 5000
    }
  }

  // if mode is full functioning, we start a application network
  application-network {
    host = "192.168.1.11"
    port = 9500

    // seeds, empty array indicates that it's a seed node
    // or else set other core node application network host and port
    //seeds = ["192.168.1.11:9501"]
    seeds = []

    // bound account, maybe same as consensus-network's
    account = {
     encPrivKey = "8kRNtH14ezpwhrX2KTP9NhG4Yd6UdT5YjibvB2MMeU83teZ1urhaoAT"
      pubKey = "24UY6SpsGVmwYNufBnnWH8gFdXGb7fzES8tVURWe4LPEG"
      iv = "L9WPFRYttXQ"
      id = "19hJizKttFQ3a9kKpdeTvvQEDgCnTGBmtU"
      secretKey = "G7TQb35W8FUURtDTQDbvkpYEoxHKBm9rXZVoHemhiNs4"
    }
  }
}

// as a edge-node if current node is core node can be ignored
edge-node {
  client-network {
    http-json-rpc {
      host = ""
      port = 

      // ssl not support now
    }

    // maybe support other network, such as gRPC
  }

  // a p2p network, connect with core-node to transmit application request and response
  application-network {
    host = ""
    port = 

    // seeds, empty array indicates that it's a seed node
    // seeds = []
    seeds = []

    // bound account, maybe same as consensus-network's
    account = {
      encPrivKey = ""
      pubKey = ""
      iv = ""
      id = ""
      secretKey = ""
    }
  }
}
```

* 执行启动命令
```
corenode -w /tmp/block/core_node

参数说明:
w: CoreNode根目录绝对路径
```
启动成功会有类似如下日志:
```
2019-06-27 13:47:59,454 INFO  [main] - contract runtime checking passed.
2019-06-27 13:48:00,140 INFO  [main] - store loaded, and checking passed.
2019-06-27 13:48:00,529 INFO  [main] - ==== consensus node current members: ===================
2019-06-27 13:48:00,532 INFO  [main] - =    -- B12724BE63541098C5DC@192.168.1.11:9400 --
2019-06-27 13:48:00,533 INFO  [main] - ========================================================
2019-06-27 13:48:00,599 INFO  [main] - node (10.65.100.65:9400(19hJizKttFQ3a9kKpdeTvvQEDgCnTGBmtU:24UY6SpsGVmwYNufBnnWH8gFdXGb7fzES8tVURWe4LPEG)) started
2019-06-27 13:48:00,640 INFO  [main] - ==== application node current members: ===================
2019-06-27 13:48:00,640 INFO  [main] - =    -- 835F214FE4CACA67BA2B@192.168.1.11:9500 --
2019-06-27 13:48:00,640 INFO  [main] - ========================================================
2019-06-27 13:48:00,643 INFO  [main] - node (192.168.1.11:9500(19hJizKttFQ3a9kKpdeTvvQEDgCnTGBmtU:24UY6SpsGVmwYNufBnnWH8gFdXGb7fzES8tVURWe4LPEG)) started
2019-06-27 13:48:00,644 INFO  [main] - network startup.
2019-06-27 13:48:02,198 INFO  [main] - consensus engine initialized.
2019-06-27 13:48:02,200 INFO  [main] - CoreNode startup!
```

### 启动EdgeNode
* 拷贝链根目录`/tmp/block/edge_node`，编辑`fssi.conf`文件并修改,示例如下:
```
// config sample
chainId = "testNet"

// as a core-node if edge node can be ignored 
core-node {
  mode = ""

  // consensus network config
  consensus-network {
    host = ""
    port = 80

    // seeds, empty array indicates that it's a seed node
    // or else set other core node consensus network host and port
    seeds = []

    // bound account copy from account.json and account_secret.json file
    account  {
      encPrivKey = ""
      pubKey = ""
      iv = ""
      id = ""
      secretKey = ""
    }

    // scp protocol config
    scp  {
      // current node id is the bound account's public key.
      quorums = {
        // quorum threashold
        threshold = 1

        // quorum members, an arry of node id (bound account's public key)
        validators = ["",""]

        //optional nested quorums 
        // the element of a nested quorum should not have innerSets
        innerSets = [
          {
            threshold = 0
            validators = ["", ""]
          },
          {
            threshold = 0
            validators = ["", ""]
          }
        ]
      }

      // max timeout seconds
      maxTimeoutSeconds = 1800

      // max nominating times for every slotIndex
      maxNominatingTimes = 50

      // millis to broadcast all of nominate and ballot message
      broadcastTimeout = 2000

      // max transaction size in block
      maxTransactionSizeInBlock = 2
      
      // max timeout seconds for consensus to calculate transactions
      maxConsensusWaitTimeout = 5
    }
  }

  // if mode is full functioning, we start a application network
  application-network {
    host = ""
    port = 80

    // seeds, empty array indicates that it's a seed node
    // or else set other application network host and port
    seeds = []

    // bound account, maybe same as consensus-network's
    account = {
     encPrivKey = ""
      pubKey = ""
      iv = ""
      id = ""
      secretKey = ""
    }
  }
}

// as a edge-node 
edge-node {
  client-network {
    http-json-rpc {
      host = "192.168.1.11"
      port = 9600

      // ssl not support now
    }

    // maybe support other network, such as gRPC
  }

  // a p2p network, connect with core-node to transmit application request and response
  application-network {
    host = "192.168.1.11"
    port = 9501

    // seeds, empty array indicates that it's a seed node
    // or else set core node aplication network host and port
    // seeds = []
    seeds = ["192.168.1.11:9500"]

    // bound account, maybe same as consensus-network's
    account = {
      encPrivKey : "AM5THyUZuAb5GPNXp5ZsZ2vstte6xMusHsp9J14WjQZSzxtpqkieirY",
      pubKey : "wKfbyYZaJLUaKSMGc7NL9LVZCSHfQFRQ4YqSagwuiFAv",
      iv : "LU9keWhKAP3",
      id : "1M5DWC7u4saVMBSTqvMs4uJvmWgyKrCHuX"
      secretKey = "2H4hon6mey3mpe9aBadgkhqynQKvELswQM3tx58mf67L"
    }
  }
}
```

* 执行启动命令
```
edgenode -w /tmp/block/edge_node

参数说明:
w: EdgeNode根目录绝对路径
```
启动成功会有类似如下日志:
```
2019-06-27 14:41:47,933 INFO  fssi.interpreter.NetworkHandler@[main] - ==== application node current members: ===================
2019-06-27 14:41:47,936 INFO  fssi.interpreter.NetworkHandler@[main] - =    -- 835F214FE4CACA67BA2B@192.168.1.11:9500 --
2019-06-27 14:41:47,936 INFO  fssi.interpreter.NetworkHandler@[main] - =    -- B75E0BED0F2D86C3B03C@192.168.1.11:9501 --
2019-06-27 14:41:47,936 INFO  fssi.interpreter.NetworkHandler@[main] - ========================================================
2019-06-27 14:41:47,998 INFO  fssi.interpreter.NetworkHandler@[main] - node (192.168.1.11:9501(1M5DWC7u4saVMBSTqvMs4uJvmWgyKrCHuX:wKfbyYZaJLUaKSMGc7NL9LVZCSHfQFRQ4YqSagwuiFAv)) started
2019-06-27 14:41:48,011 INFO  fssi.interpreter@[main] - application node start up
2019-06-27 14:41:48,769 INFO  fssi.interpreter.NetworkHandler@[main] - edge node json rpc service startup: http://192.168.1.11:9600/jsonrpc/edge/v1
2019-06-27 14:41:48,777 INFO  fssi.interpreter@[main] - EdgeNode start up!
```

## 智能合约相关

### 智能合约项目结构

-- root

    -- lib
    -- src
        -- main
            --java
            -- resources
                -- META-INF
                     -- contract.meta.conf (智能合约元信息配置文件)
        --test
            --java
            -- resources
                -- META-INF
        
### 智能合约元信息配置文件

##### 名称
`contract.meta.conf`

##### 内容
```
contract {
  owner = "${account id}"

  name = "${contract name}"

  version = "${contract.version}"

  interfaces {
    # 模板  methodAlias = qualifiedClassName#methodName(Context,arguments*)
    
      tokenQuery = "com.fssi.sample.InterfaceSample#tokenQuerySample(Context,String)"

      kvStore = "com.fssi.sample.InterfaceSample#kvStoreSample(Context,String,String)"

      currentAccountId = "com.fssi.sample.InterfaceSample#currentAccountIdSample(Context)"
  }
}

```
#### 说明
* `owner`的值为账户存储文件中的`id`值
* `name`指定该智能合约的名称
* `version`指定该智能合约当前版本
* `interfaces`为暴露的智能合约接口列表
* 下载SDK拷贝到项目lib目录下。[点击下载](https://dl.bintray.com/bigknife/fssi/fssi/contractlib_2.12/0.2/contractlib_2.12-0.2.jar)
* Context类型为智能合约执行的上下文信息，全类名为: `fssi.contract.lib.Context` ，必须以首个参数出现
* 智能合约方法参数只能支持`java`中`int` 、`long` 、`float` 、`double` 、`boolean` 、 `String` 7种类型

### 约束

#### 文件约束
* `META-INF` 文件夹下面只能存放`contract.meta.conf`文件

#### 包名约束
* 智能合约的`package`不能以`fssi`开头

#### 类型约束
* 不可用使用 `java.util.concurrent` 包下所有类型
* 不可用使用 `java.lang.reflect` 包下所有类型
* 不可用使用 `javax` 包下所有类型
* 不可用使用 `java.net` 包下所有类型
* 不可用使用 `java.sql` 包下所有类型
* 不可用使用 `sun` 包下所有类型
* 不可用使用 `java.lang.Thread` 类
* 不可用使用 `java.lang.Class` 类

### 创建智能合约项目
运行创建合约项目命令,实例如下:
```
tool CreateContractProject -pd /tmp/contract_project

参数说明:
pd: 项目存储绝对路径
```

### 编译智能合约
* 运行编译智能合约命令，示例如下:
```
tool CompileContract -af /tmp/account/account.json -kf /tmp/account/account_secret.json -pd /tmp/contract_project/ -of /tmp/contract -sv 1.0.0

参数说明:
af: 账户信息存储文件绝对路径,必须和项目配置文件中的owner一致
kf: 账户私钥存储文件绝对路径
pd: 智能合约项目绝对路径
of: 编译命令执行成功结果存储文件的绝对路径
sv: 指定命令版本，目前仅支持: 1.0.0
```

* 编译命令运行示例如下:
```
15:51:58.526 [main] INFO fssi.sandbox.world.Compiler - compile contract under path /tmp/contract_project at version 1.0.0 saved to /tmp/contract.json
15:51:58.538 [main] INFO fssi.sandbox.world.Checker - check project structure at path: /tmp/contract_project
15:51:58.546 [main] INFO fssi.sandbox.world.Checker - check contract required files validity at path: /tmp/contract_project/src/main/resources/META-INF
15:51:58.559 [main] INFO fssi.sandbox.world.Checker - check resource files validity at path: /tmp/contract_project/src/main/resources/META-INF
15:51:58.571 [main] INFO fssi.sandbox.world.Checker - check contract meta file is valid: /tmp/contract_project/src/main/resources/META-INF/contract.meta.conf
15:51:58.762 [main] INFO fssi.sandbox.world.Checker - check contract method description for descriptors file Vector(MethodDescriptor(kvStore,com.fssi.sample.InterfaceSample#kvStoreSample(Context,String,String)), MethodDescriptor(tokenQuery,com.fssi.sample.InterfaceSample#tokenQuerySample(Context,String)), MethodDescriptor(currentAccountId,com.fssi.sample.InterfaceSample#currentAccountIdSample(Context)))
15:51:58.763 [main] DEBUG fssi.sandbox.world.Checker - smart contract exposes method [kvStore = com.fssi.sample.InterfaceSample#kvStoreSample(Context,String,String)]
15:51:58.819 [main] DEBUG fssi.sandbox.world.Checker - smart contract exposes method [tokenQuery = com.fssi.sample.InterfaceSample#tokenQuerySample(Context,String)]
15:51:58.820 [main] DEBUG fssi.sandbox.world.Checker - smart contract exposes method [currentAccountId = com.fssi.sample.InterfaceSample#currentAccountIdSample(Context)]
15:51:58.878 [main] INFO fssi.sandbox.world.Compiler - compile project /tmp/contract_project saved to /tmp/aa6d8787ebb64d63ba506bc7d6ebbf54
15:52:00.303 [main] INFO fssi.sandbox.world.Compiler - upgrade contract class from version 1.0.0
15:52:00.394 [main] DEBUG fssi.sandbox.contract.ContractFileBuilder - add magic FSSI to contract file: /tmp/contract.json
15:52:00.398 [main] DEBUG fssi.sandbox.contract.ContractFileBuilder - add contract size 1603 to contract file: /tmp/contract.json
15:52:00.400 [main] DEBUG fssi.sandbox.contract.ContractFileBuilder - add contract 1603 bytes to contract file: /tmp/contract.json
15:52:00.402 [main] DEBUG fssi.sandbox.contract.ContractFileBuilder - make signature for contract ,private key length: 32
15:52:01.092 [main] DEBUG fssi.sandbox.contract.ContractFileBuilder - add contract signature 71 bytes to contract file: /tmp/contract.json
15:52:01.147 [main] DEBUG fssi.sandbox.contract.ContractFileBuilder - read magic from contract file: /tmp/contract.json
15:52:01.153 [main] DEBUG fssi.sandbox.contract.ContractFileBuilder - read contract size from contract file: /tmp/contract.json
15:52:01.156 [main] DEBUG fssi.sandbox.contract.ContractFileBuilder - read smart contract from contract file: /tmp/contract.json
15:52:01.168 [main] DEBUG fssi.sandbox.contract.ContractFileBuilder - read contract signature from contract file: /tmp/contract.json
15:52:01.175 [main] DEBUG fssi.sandbox.contract.ContractFileBuilder - verify signature for contract ,public key length: 33
15:52:01.629 [main] INFO fssi.sandbox.world.Checker - check contract determinism for contract /tmp/81101ac6-372e-43cd-ab47-56a94f73b561
15:52:01.630 [main] INFO fssi.sandbox.world.Builder - degrade class version for dir: /tmp/81101ac6-372e-43cd-ab47-56a94f73b561 saved to /tmp/d6b707d9-6dd4-48e4-9724-bdd70f3e1530
15:52:01.673 [main] INFO fssi.sandbox.world.Builder - build contract meta from contract file: /tmp/81101ac6-372e-43cd-ab47-56a94f73b561
15:52:01.674 [main] INFO fssi.sandbox.world.Checker - check contract meta file is valid: /tmp/81101ac6-372e-43cd-ab47-56a94f73b561/META-INF/contract.meta.conf
15:52:01.699 [main] INFO fssi.sandbox.world.Checker - check contract method description for descriptors file Vector(MethodDescriptor(kvStore,com.fssi.sample.InterfaceSample#kvStoreSample(Context,String,String)), MethodDescriptor(tokenQuery,com.fssi.sample.InterfaceSample#tokenQuerySample(Context,String)), MethodDescriptor(currentAccountId,com.fssi.sample.InterfaceSample#currentAccountIdSample(Context)))
15:52:01.699 [main] DEBUG fssi.sandbox.world.Checker - smart contract exposes method [kvStore = com.fssi.sample.InterfaceSample#kvStoreSample(Context,String,String)]
15:52:01.699 [main] DEBUG fssi.sandbox.world.Checker - smart contract exposes method [tokenQuery = com.fssi.sample.InterfaceSample#tokenQuerySample(Context,String)]
15:52:01.700 [main] DEBUG fssi.sandbox.world.Checker - smart contract exposes method [currentAccountId = com.fssi.sample.InterfaceSample#currentAccountIdSample(Context)]
15:52:02.011 [main] INFO fssi.sandbox.world.Checker - check contract class at path: /tmp/d6b707d9-6dd4-48e4-9724-bdd70f3e1530
15:52:02.052 [main] INFO fssi.tool.handler.package$compileContract$ - contract compiled
15:52:02.056 [main] INFO fssi.tool.ToolMain$ - Main Program Exit
```

### 发布智能合约
* 运行发布智能合约命令示例如下:
```
tool CreateTransaction deploy -af /tmp/account/account.json -kf /tmp/account/account_secret.json -cf /tmp/contract -of /tmp/deploy.json

参数说明:
af: 账户信息存储文件绝对路径,必须和项目配置文件中的owner一致
kf: 账户私钥存储文件绝对路径
cf: 编译成功结果输出文件的绝对路径
of: 发布命令执行成功结果存储文件的绝对路径
```

* 发布命令运行示例如下:
```
16:14:52.504 [main] DEBUG fssi.sandbox.contract.ContractFileBuilder - read magic from contract file: /tmp/contract.json
16:14:52.514 [main] DEBUG fssi.sandbox.contract.ContractFileBuilder - read contract size from contract file: /tmp/contract.json
16:14:52.517 [main] DEBUG fssi.sandbox.contract.ContractFileBuilder - read smart contract from contract file: /tmp/contract.json
16:14:52.522 [main] DEBUG fssi.sandbox.contract.ContractFileBuilder - read contract signature from contract file: /tmp/contract.json
16:14:52.524 [main] DEBUG fssi.sandbox.contract.ContractFileBuilder - verify signature for contract ,public key length: 33
16:14:53.484 [main] INFO fssi.sandbox.world.Builder - build contract from path: /tmp/bffa9e6f-6d6d-4567-9a81-07f6200a6383
16:14:53.487 [main] INFO fssi.sandbox.world.Builder - build contract meta from contract file: /tmp/bffa9e6f-6d6d-4567-9a81-07f6200a6383
16:14:53.500 [main] INFO fssi.sandbox.world.Checker - check contract meta file is valid: /tmp/bffa9e6f-6d6d-4567-9a81-07f6200a6383/META-INF/contract.meta.conf
16:14:53.672 [main] INFO fssi.sandbox.world.Checker - check contract determinism for contract /tmp/bffa9e6f-6d6d-4567-9a81-07f6200a6383
16:14:53.673 [main] INFO fssi.sandbox.world.Builder - degrade class version for dir: /tmp/bffa9e6f-6d6d-4567-9a81-07f6200a6383 saved to /tmp/78edb95c-d744-4eb9-9cc4-f4e997abd030
16:14:53.769 [main] INFO fssi.sandbox.world.Builder - build contract meta from contract file: /tmp/bffa9e6f-6d6d-4567-9a81-07f6200a6383
16:14:53.770 [main] INFO fssi.sandbox.world.Checker - check contract meta file is valid: /tmp/bffa9e6f-6d6d-4567-9a81-07f6200a6383/META-INF/contract.meta.conf
16:14:53.775 [main] INFO fssi.sandbox.world.Checker - check contract method description for descriptors file Vector(MethodDescriptor(kvStore,com.fssi.sample.InterfaceSample#kvStoreSample(Context,String,String)), MethodDescriptor(tokenQuery,com.fssi.sample.InterfaceSample#tokenQuerySample(Context,String)), MethodDescriptor(currentAccountId,com.fssi.sample.InterfaceSample#currentAccountIdSample(Context)))
16:14:53.776 [main] DEBUG fssi.sandbox.world.Checker - smart contract exposes method [kvStore = com.fssi.sample.InterfaceSample#kvStoreSample(Context,String,String)]
16:14:53.795 [main] DEBUG fssi.sandbox.world.Checker - smart contract exposes method [tokenQuery = com.fssi.sample.InterfaceSample#tokenQuerySample(Context,String)]
16:14:53.797 [main] DEBUG fssi.sandbox.world.Checker - smart contract exposes method [currentAccountId = com.fssi.sample.InterfaceSample#currentAccountIdSample(Context)]
16:14:53.981 [main] INFO fssi.sandbox.world.Checker - check contract class at path: /tmp/78edb95c-d744-4eb9-9cc4-f4e997abd030
16:14:54.303 [main] INFO fssi.tool.ToolMain$ - Main Program Exit
```

* 发布命令结果示例如下:
```
{
  "id" : "4rLAsAoqwrFkvrhwe4z1dR1Qm28JxZXcuL4MnvyRw1Wb",
  "method" : "sendTransaction",
  "params" : {
    "type" : "Deploy",
    "transaction" : {
      "id" : "OTYyNDMwYTljMjliNDgzYzhkMGZmZjczM2E1MGU0YWQ=",
      "owner" : "19hJizKttFQ3a9kKpdeTvvQEDgCnTGBmtU",
      "publicKeyForVerifying" : "24UY6SpsGVmwYNufBnnWH8gFdXGb7fzES8tVURWe4LPEG",
      "contract" : {
        "owner" : "19hJizKttFQ3a9kKpdeTvvQEDgCnTGBmtU",
        "name" : "dGVzdE5hbWU=",
        "version" : "1.0.1",
        "code" : "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",
        "methods" : [
          {
            "alias" : "currentAccountId",
            "fullSignature" : "com.fssi.sample.InterfaceSample#currentAccountIdSample(Context)"
          },
          {
            "alias" : "kvStore",
            "fullSignature" : "com.fssi.sample.InterfaceSample#kvStoreSample(Context,String,String)"
          },
          {
            "alias" : "tokenQuery",
            "fullSignature" : "com.fssi.sample.InterfaceSample#tokenQuerySample(Context,String)"
          }
        ],
        "description" : "dGVzdCBmc3NpIGNvbnRyYWN0",
        "signature" : "MEUCIQC3MCDma+cci/GzwGA2G3G8w2wTUBwlYbJQZUB2AoGZCwIgTJIU1Zu6t8dJcCjzh6aWhzmND3u/TpoEE7M+qpQA0a4="
      },
      "signature" : "MEUCIHSOT8l5Gm4Y6BQVcG/cVJD2x6q5Pih3Frz2ucK2D2ERAiEA8yugZhLvTzDrtJFnm3g57Y6NpPxmjBOHgnZr/xnriU8=",
      "timestamp" : 1561623294138
    }
  },
  "jsonrpc" : "2.0"
}
```

* 发布智能合约到链上
构造如下`POST`请求:
```
curl -X POST -d @/tmp/deploy.json  http://192.168.1.11:9600/jsonrpc/edge/v1
```

### 执行智能合约
* 执行智能合约命令示例如下:
```
tool CreateTransaction run -af /tmp/account/account.json -kf /tmp/account/account_secret.json -owner 19hJizKttFQ3a9kKpdeTvvQEDgCnTGBmtU -name testName -version 1.0.1 -m currentAccountId -of /tmp/run.json

参数说明:
af: 调用智能合约账户信息存储文件绝对路径
kf: 调用智能合约账户私钥存储文件绝对路径
owner: 被调智能合约的owner的id标识
name: 被调智能合约的名称
version: 被调智能合约的版本
m: 被调智能合约暴露的接口的别名，即智能合约配置文件里面的methodAlias
of: 执行命令成功结果存储文件的绝对路径
```

* 执行命令成功示例如下:
```
{
  "id" : "4o9KuVCpQ4ouPwrqRE4jUnB5t1vPuixszp2ueziCdhNX",
  "method" : "sendTransaction",
  "params" : {
    "type" : "Run",
    "transaction" : {
      "id" : "OGUxMmE4OTYxZmVkNGQ3ZGJlYWQ0NjU1ZDNkYjg0YTA=",
      "caller" : "19hJizKttFQ3a9kKpdeTvvQEDgCnTGBmtU",
      "publicKeyForVerifying" : "24UY6SpsGVmwYNufBnnWH8gFdXGb7fzES8tVURWe4LPEG",
      "owner" : "19hJizKttFQ3a9kKpdeTvvQEDgCnTGBmtU",
      "contractName" : "dGVzdE5hbWU=",
      "contractVersion" : "1.0.1",
      "methodAlias" : "currentAccountId",
      "contractParameter" : null,
      "signature" : "MEUCIQCa1G70UWg6D97cLC66G7mMQX5Hdz+xll1LzJ2lhdzOfQIgEbeHYH12bM5LB3sVhgvK3hmZ9YhJzW5vhNgyTPEawSE=",
      "timestamp" : 1561624364862
    }
  },
  "jsonrpc" : "2.0"
}
```

* 在链上执行智能合约
构造如下`POST`请求:
```
curl -X POST -d @/tmp/run.json  http://192.168.1.11:9600/jsonrpc/edge/v1
```

## 补充
### 命令参数说明
```
Usage: fssitool [CreateAccount|CreateChain|CreateContractProject|CompileContract|CreateTransaction] [options]

  -h, --help               print this help messages
```
  
```
Command: CreateAccount [options]
Create An FSSI Account
  -s, --random-seed <value>
                           a random string to create secret key for account
                           
  -af, --account-file <value>
                           output account json file path
                           
  -kf, --key-file <value>  output secret key file
```

```
Command: CreateChain [options]
Create a chain
  -d, --root-dir <value>
  
  -id, --chain-id <value>
```

```
Command: CreateContractProject [options]
Create Contract Project
  -pd, --project-directory <value>
                           smart contract project root path default current dir
```                         

```
Command: CompileContract [options]
Compile Smart Contract Project
  -af, --account-file <value>
                           compiler account file created by 'CreateAccount'
                           
  -kf, --key-file <value>  compiler account secret key file
  
  -pd, --project-directory <value>
                           smart contract project root path
                           
  -of, --output-file <value>
                           the compiled artifact file name, with absolute path
                           
  -sv, --sandbox-version <value>
                           supported version of the sandbox on which the smart contract will run, default is 1.0.0(only support 1.0.0 now)
```

```
Command: CreateTransaction [transfer|deploy|run]
Create Transaction
``` 

```
Command: CreateTransaction transfer [options]
create transfer transaction
  -af, --account-file <value>
                           payer's account file created by 'CreateAccount'
                           
  -kf, --key-file <value>  payer's account secret key file
  
  -pi, --payee-id <value>  payee's account id created by 'CreateAccount'
  
  -t, --token <value>      amount to be transfered, in form of 'number' + 'unit', eg. 100Sweet.
  
  -of, --output-file <value>
                           if set, the message will output to this file
```

```
Command: CreateTransaction deploy [options]
create deploy contract transaction
  -af, --account-file <value>
                           contract owner's account file created by 'CreateAccount'
                           
  -kf, --key-file <value>  contract owner's account secret key file
  
  -cf, --contract-file <value>
                           smart contract file
                           
  -o, --output-file <value>
                           if set, the message will output to this file
```

```
Command: CreateTransaction run [options]
create run contract transaction
  -af, --account-file <value>
                           contract caller's account file created by 'CreateAccount'
                           
  -kf, --key-file <value>  contract caller's account secret key file
  
  -owner, --owner-id <value>
                           contract owner
                           
  -name, --contract-name <value>
                           calling contract name
                           
  -version, --contract-version <value>
                           the version of the invoking contract
                           
  -m, --method-alias <value>
                           alias of method to invoke
                           
  -p, --parameter <value>  parameters for this invoking, args must wrapped in [] and list by methods arg's index ignored first argument
  
  -of, --output-file <value>
                           if set, the message will output to this file
```

