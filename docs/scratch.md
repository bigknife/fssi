# Scratch

## some idea
From message-sender's point of view:

1. correspond to user's requests
2. correspond to p2p messages

That's to say, there is two kinds of service, one is to
serve for the outer and the other one serves for the inner.

So, I call the firs kind of service entity `???`， maybe
 `nymph`, and the second kind of service entity `???`
 maybe `warrior`

 For `nymph`:

1. handle user's enrollment, create a pair of keys
for user, which is the only one to identify an account.
2. handle user's transactions, put them into the
backend, then the `warrior` will compute them.
3. handle user's query requests for previous transactions.


In my mind, it's more clear that there should be three type of nodes:
1. **Nymph Node**, which is the edge of the system, receive user's requests,
and disseminate them to `Warrior Node`.
2. **Warrior Node**, which is the core node, only interact with each other
and `Nymph Node`, validate transactions by running consensus, and record
transactions.
3. **Mirror Node**, which is a high performance for querying node, just sync data
from `Warrior Node`, and provide a responsible web page to show system status,
and receive query request and show the result.

**Mirror Node** is not so urgent.

----

Node can be categorized:
1. NymphNode
2. WarriorNode
3. QueryNode

every node will be paid for their work:
1. NymphNode, paid by completing every uc,such as register, sendTransaction.
2. WarriorNode, paid by validating every moment.
3. query node, paid by their services of querying.

so nodes may be bound to an account. of course it's ok not to be bound to an account.
if bound, the account balance would increase by being paid.

that means, an account should be create without nodes, so, a command tool is needed to do such things.

----

Transaction jsonrpc protocol.

```json
{
  "type": "Transfer",
  "payload": {
    "id": "usercreatedid",
    "from": "account-id",
    "to": "account-id",
    "amount": "1000Sweet",
  },
  "signature": "BlaBla..."
}
```

how to sign?

ECDSAwithUserPrivateKey(type + ascorder(payload properties))


----
consensus engine uses scp as it's implementation.
pool moment will be reformed to start scp nominate.

how to init scp?
- what is the id of this scp node?
- how to emit scp messages to peers?
- how to validate values?
- how to handle scp message?

scp component should be a little world. singleton.

## test cases
build a four nodes cluster.

### working directories
make a root directory for the tests.

```
mkdir ~/fssi_test
```

then, make 1 nymph node, four warrior nodes woring directories:

```
mkdir ~/fssi_test/nymph
mkdir ~/fssi_test/warrior_1
mkdir ~/fssi_test/warrior_2
mkdir ~/fssi_test/warrior_3
mkdir ~/fssi_test/warrior_4
```

### create four accounts
assume: set password to 12345678
so use the command:

```
fssi cmd createAccount -p 12345678 > acc1.json
fssi cmd createAccount -p 12345678 > acc2.json
fssi cmd createAccount -p 12345678 > acc3.json
fssi cmd createAccount -p 12345678 > acc4.json
```

### run four warrior nodes

1. warrior 1, create a seed node bound with account 1.
```bash
fssi warrior \
	--working-dir ~/fssi_test/warrior_1 \
	--snapshot-db-port 20000 \
	--node-ip 10.65.106.11 \
	--node-port 8800 \
	--color true \
	--public-key 03d3e70862e399fc94ac62edd111398aa48f61de3a20d1ace979c02b3d9075e08a \
	--private-key 22ff67f0db998bbd11ff4e53eeac3157a8d271e1a4e30b3562bf7e349464d671bcda9c92885e38ab \
	--iv 616c7a6172746f6a 
```

**note** you should input the password bound into the node, when starting warrior node.

2. warrior 2, start the second warrior node, with warrior 1 as it's seed. bound account 2.

```bash
fssi warrior \
	--working-dir ~/fssi_test/warrior_2 \
	--snapshot-db-port 20001 \
	--node-ip 10.65.106.11 \
	--node-port 8801 \
	--color true \
	--public-key  0281e3120890180ae38cda921a8223c17f4db284ff43c9c31fed54e0dd6356c042 \
	--private-key d2a7a192ed9c17a7a3d3f2c92b91618ca515b998567774f9ab911534a68dd3d7f0752c1a1c937ab2 \
	--iv 696c686c726d6e79 \
	--seeds 10.65.106.11:8800
```

3. warrior 3,4, just like 2.

```bash
fssi warrior \
	--working-dir ~/fssi_test/warrior_3 \
	--snapshot-db-port 20002 \
	--node-ip 10.65.106.11 \
	--node-port 8802 \
	--color true \
	--public-key  0338984c81ba98e807dee4b1ff2079a77609a7ad8765054b7510df278ef5250f71 \
	--private-key 2c4f82b557f526b7097e51b9e121465862bf865f0ad5028e0ee36261238d466dd438644491bd60a9 \
	--iv 6961686c6c707a64 \
	--seeds 10.65.106.11:8800,10.65.106.11:8801
```

```bash
fssi warrior \
	--working-dir ~/fssi_test/warrior_4 \
	--snapshot-db-port 20003 \
	--node-ip 10.65.106.11 \
	--node-port 8803 \
	--color true \
	--public-key  02a107e6206824925ff218add39dcdff99092b426bc79b071eb311edb22be426db \
	--private-key 1a9eda1696481b9a0bc274bd4dd731069de4591be49871bba63a9a43a05d1bcf4dd7fa0e9bc3a007 \
	--iv 7979746873626866 \
	--seeds 10.65.106.11:8800,10.65.106.11:8801
```

4. start a nymph node

bound account 1

```bash
fssi nymph \
	--working-dir ~/fssi_test/nymph \
	--snapshot-db-port 10000 \
	--node-ip 10.65.106.11 \
	--node-port 8700 \
	--seeds 10.65.106.11:8800,10.65.106.11:8801 \
	--color true \
	--public-key 03d3e70862e399fc94ac62edd111398aa48f61de3a20d1ace979c02b3d9075e08a
```

### create a transaction
use acc2 to create a banana transaction

```bash
fssi cmd createRunContract \
	--private-key d2a7a192ed9c17a7a3d3f2c92b91618ca515b998567774f9ab911534a68dd3d7f0752c1a1c937ab2 \
	--password 12345678 \
	--iv 696c686c726d6e79 \
	--account-id 0281e3120890180ae38cda921a8223c17f4db284ff43c9c31fed54e0dd6356c042 \
	--name com.test \
	--version 0.0.1 \
	--function function1 \
	--params '["phi", "0.5", "100"]' \
	--output-format space2
```
