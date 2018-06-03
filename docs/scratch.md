# Scratch

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
