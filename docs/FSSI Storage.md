# FSSI 存储架构

## 存储选型

为了方便进行一致性校验，采用嵌入式的 `KV` 数据库。Bitcoin使用LevelDB，ETH选择RocksDB，作为JVM生态系统的区块链系统，优先选择纯Java的的嵌入式`KV`数据库。比较成熟的有：

1. H2 MVStore
2. MapDB
3. Berkeley DB JE
4. Chronicle Map
5. **Xodus Patricia/BTree**

Xodus 已经在Jetbrain 的 [YouTrack](https://youtrack.jetbrains.com/issues) 系统运行了超过10年，物理数据库大小已经超过100GB，性能优良，成熟稳定，并且提供`ACID`一致性，因此作为底层存储的第一选择。

另外，使用KV作为底层，也能方便包装一层SQL接口，实际上一些关系型数据库就是这么做的。

## Scheme的设计

区块链所有的数据存放在一个Xodus实例中，以获得较好的性能和事务支持。但是在一个数据库里需要同时存储多种类型的数据，需要对K-V进行精心的设计。

我们采用类似于`URL`的表现形式来标志数据的分类及寻址，在Value层面，设计3个字端，分别表示存储的值、Hash以及子节点的地址列表，以达到Merkle树的效果。

### Key，Data Address

我们采用统一资源定位符（URL）的形式对数据进行寻址定位。在FSSI里，需要存储4大类数据，我们分别使用4个Scheme来标志：

1. **meta:** 元数据，存储FSSI区块链实例的元信息，如chainId、version、height等。
2. **block:** 区块数据，存储区块数据，包含前一个区块的hash，当前区块hash，交易hash，receipt hash等。
3. **transaction:** 区块的交易列表
4. **receipt:** 交易执行结果数据，存储区块数据中交易列表执行产生的结果数据。
5. **state:** 全局世界状态数据，存储账户相关数据，包含余额、合约、合约数据等。

#### meta:

元数据。

* **meta://chainId** 当前链的标志
* **meta://height** 当前链的高度
* **meta://version** 当前链的版本

#### block:

块数据。

* **block:{height}://preWorldState** 前一个块所处的世界状态，由 `meta`,`block-n`,`tranction-n`,`receipt-n`,`state` 共同构成（hash）
* **block:{height}://transactions** 当前块包含的交易ID有序列表
* **block:{height}://receipts** 当前块包含的receipt有序列表
* **block:{height}://timestamp** 当前出块时间，或者当前块包含的最后一笔交易时间
* **block:{height}://hash** 当前块的Hash值（base58或者base64）

#### transaction:

交易列表。

* **transaction:{height}://{transactionId}** 某个区块（height标志）中某个交易

#### recepit:

执行凭证，执行结果信息。

* **receipt:{height}://{transactionId}/result** 执行结果，成功或者失败
* **receipt:{height}://{transactionId}/cost** 执行成本测量数据
* **receipt:{height}://{transactionId}/logs** 执行日志信息

#### state:

世界状态。

* **state://{accountId}/balance** 某个账户的余额信息
* **state://{accountId}/contracts/{contractName}/versions/{versionCode}/desc** 合约特定版本的描述信息
* **state://{accountId}/contracts/{contractName}/versions/{versionCode}/code** 合约特定版本的代码内容
* **state://{accountId}/contracts/{contractName}/versions/{versionCode}/runtime** 合约特定版本运行时要求信息
* **state://{accountId}/contracts/{contractName}/db/{applicationKey}** 合约应用层面定义的key对应的value
* **state://{accountId}/contracts/{contractName}/invoke** 合约调用日志，内容如：transactionId@blockHeight by accountId

### Value, Validated

对于存储来说，数据仅是一个字节块，但为了快速验证，或者是为了零知识证明，每个`Value`都附加一个`Hash`，该`Hash`的内容来源于当前节点的`Value`以及下一级节点的`Hash` 。当一个节点的值发生变化了，应该将Hash值向上传递，达到类似于`Merkle`树的效果 

`Value` 形如：

```scala
case class StoredValue(bytes: Array[Byte], childrenKeys: Array[String], hash: Array[Byte])
```

## 读写操作

### 读取

通过应用层构建正确的`URL` 地址，可读取数据，如果没有则返回`None`，读取方法形如：

```scala
def get(key: StoredKey): Option[StoredValue]
```

> 由于采用类似于URL的设计，我们可以很简单的将存储数据映射到`Web`资源，通过浏览器进行查看

### 验证

通过验证`StoredValue`的`hash`字段就可以快速验证两个存储是否一致。

### 写入

写入方法形如：

```scala
def put(key: StoredKey, value: StoredValue): Unit
```

需要注意的是，**在写入某个节点的时候，需要将变化产生的新的Hash值向上传递，从而保证Hash的可验证性，为了提高读取子节点性能，还需要把子节点的key插入到父节点的childrenKeys字端**。

### 事务，批量写入

写入时一定会因此递归向上传递，产生多个`put`操作，为了保证一致性和数据完整性，我们利用`Xodus`提供的事务支持，将多个写入操作放在事务之内。

事务操作形如：

```scala
def transact(batch: Store => Unit): Transaction
def commit(transaction: Transaction): Unit
def rollback(transaction: Transaction): Unit
```

## 应用场景

### 转账

账户A像账户B转100个Token，操作流程为：

```
balanceA = get state:/accountA/balance
if balanceA < 100 return false.
else 
    balanceB = get state:/accountB/balance
    balanceA = balanceA - 100
    balanceB = balanceB + 100
    transaction = transact {
        put state:/accountA/balance balanceA
        put state:/accountB/balance balanceB
    }
    try {
        commit(transaction)
        return true
    } catch {
        rollback(transaction)
        return false
    }
```

### 发布智能合约

账户A发布智能合约`contractA`的第一个版本，版本号为`1.0.0`，操作流程为：

```
transaction = transact {
    put state:/accountA/contract/contractA/version/1.0.0/desc "what is it for"
	put state:/accountA/contract/contractA/version/1.0.0/runtime "chain:>1.0,jdk:>1.8"
	put state:/accountA/contract/contractA/version/1.0.0/code <code bytes>
	put state:/accountA/contract/contractA/version/1.0.0 <new value hash>
	put state:/accountA/contract/contractA/version <new value hash>
	put state:/accountA/contract/contractA <new value hash>
	put state:/accountA/contract <new value hash>
	put state:/accountA <new value hash>
	put state: <new value hash>
}
try {
    commit(transaction)
    return true
} catch {
    rollback(transaction)
    return false
}
```

### 执行智能合约

账户A调用账户B发布的智能合约`contractB` 的`1.0.0`版本，操作流程为：

```
transaction = transact {
    ops = contract {
        a = get 'a'
        b = get 'b'
        put a (a + b)
    }
    ops.map(key => "state:/accountA/contract/contractB/1.0.0/db/" + key)
}
try {
    commit(transaction)
    return true
} catch {
    rollback(transaction)
    return false
}
```

## 参考实现

### 核心存储

核心存储使用KV Store实现，在存储核心，我们只关心字节级的存取操作：

```scala
trait KVStore {
  def get(key: Array[Byte]): Option[Array[Byte]]
  def put(key: Array[Byte], value: Array[Byte]): Either[Throwable, Boolean]
  def delete(key: Array[Byte]): Either[Throwable, Boolean]
  def keyIterator: Iterator[Array[Byte]]
}
```

### 区块链存储

区块链存储基于核心存储实现，但需要体现区块链存储的特征。区块链存储的运作过程可比喻为一个状态机：

```
                +------+                   +------+
S(immutable) ---| 验证  |--- S-snapshot --- | 出块 | --- S'(immutable)
                +------+                   +------+
```



因此我们可以将区块链存储抽象为`ImmutableStore` 和 `SnapshotStore` 两种状态，通过操作在两种状态中跃迁：

#### ImmutableStore

不变存储，只提供`read*`只读方法和状态跃迁方法`beginTransaction`

```scala
trait ImmutableStore {
  def read(key: StoreKey): Option[StoreValue]
  def readChildrenKeys(key: StoreKey): Option[StoreKeySet]
  def readHash(key: StoreKey): Option[Array[Byte]]
  def readValue(key: StoreKey): Option[Array[Byte]]
  
  def beginTransaction(): SnapshotStore
}
```

#### SnapshotStore

快照存储，提供`read*`和`write`方法，当写入的变化仅体现在快照中，可以通过`commit`方法进行提交，或者通过`rollback`放弃修改。

值得一提的是，在快照存储中提供一个事务级的方法`transact`，为应用层提供事务支持。

```scala
trait SnapshotStore {
  def read(key: StoreKey): Option[StoreValue]
  def readChildrenKeys(key: StoreKey): Option[StoreKeySet]
  def readHash(key: StoreKey): Option[Array[Byte]]
  def readValue(key: StoreKey): Option[Array[Byte]]

  def write(key: StoreKey, value: Array[Byte]): Either[Throwable, Unit]

  trait ReadWriteProxy
  def transact[A](f: ReadWriteProxy => A): Either[Throwable, A]

  def commit(): ImmutableStore
  def rollback(): ImmutableStore
}
```

#### 状态跃迁

```scala
val store = ImmutableStore.load()
val snpshot = store.beginTransaction()
// do somthing
// ....

// It's ok
val store1 = snapshot.commit() // store1 != store

// Somthing wrong
val store1 = snapshot.rollback() // store1 == store
```



#### 数据结构

##### Key

为了能够实现`Merkel`数据的逻辑结构，可以设计一种分段式的结构来表示树形的层级关系，同时可以设置Tag，来做到Key的表征与实际存储内容的分离。

为了一致性校验，我们还要求Key是全排序的：

```scala
sealed trait StoreKey extends Ordered[StoreKey]{
  def stringValue: String
  def previousLevel: Option[StoreKey]
  private[store] def withTag(tag: String): StoreKey

  def ===(that: StoreKey): Boolean = stringValue == that.stringValue
  def bytesValue: Array[Byte] = stringValue.getBytes("utf-8")
}

case class Segmented(segments: Array[String], tag: Option[String] = None)
  extends StoreKey
```



##### Value

对于一个`key`，需要存储具体的数据内容，和`merkel` 树节点的Hash数据，同时为了快速遍历一个节点的字节点，还保存了下一级节点的`key`表，三种类型的数据，组成了`StoreValue`数据结构：

```scala
case class StoreValue(bytes: Array[Byte], childrenKeys: StoreKeySet, hash: Array[Byte])
```

#### 写入数据逻辑

写入一个key的逻辑：

1. 将数据写入`key#value` 
2. 计算并存储Hash
   1. 查找 `key#chidlren` 并读取子节点 `childrenkey#hash` hash值
   2. 对所有子节点的hash以及当前节点的值进行Hash
   3. 将hash值写入`key#hash`
3. 更新父级节点的字节点列表
4. 递归更新上一级节点的hash值

可见写入一个Key包含了N个写入操作，所以需要底层存储支持事务。这也是选择`xodus`的原因之一。