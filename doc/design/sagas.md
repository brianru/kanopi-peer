# Sagas

## NOTE: Inspired by Tim Ewald's talk about using reified transactions.
"Understanding and Using Reified Transactions"
http://www.datomic.com/videos.html

## Why?
The entire system must be able to operate asynchronously, with queues
and message broadcast systems between requester and responsder.

Further, requesters must be able to find out what happened to a
particular request they made. User feedback. Failure recovery in the
presenence of optimistic updates. Etc.

## Who? What? Where? When?
Messages are sent by a requestor. Currently, that requestor is usually
an Om Component. Later requestors will include server peers, onyx
things, etc.

Sagas have a uuid and some other stuff which I have to determine. It
would make sense to include the initiating user, initiating datetime,
initiating "place", etc.

Sagas can be created anywhere as part of a message. Datomic will use
the saga to reify transactions pertaining to the handling of said
message. There can and will be multiple transactions in a saga. There
also can and will be multiple message request/response pairs in a
single saga. Sagas may not be persisted in Datomic if their handling
does not reach Datomic, such sagas are ephemeral.

## How?

### Storing Status
#### Om Components => Local State
#### Clients => App State
#### Peer Process => Datomic

### Communicating Status Updates
#### triggering novelty -> Onyx -> websockets -> clients

### Modifying Status
#### implied by contents of datomic
If all request messages in a saga have a corresponding success or
failure response messeage, the sage is complete.
