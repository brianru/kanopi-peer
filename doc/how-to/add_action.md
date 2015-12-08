Actions are things clients can do.

Actions are nominally namespaced keywords describing what is being
done using the vocabulary most natural to the agent performing the
action.

Action keywords have corresponding success and failure keywords.

Actions are performed by generating a request message, which is then handled
by a request handler, which returns a response message, which is then
handled by a response handler.

Action message shapes are defined using schemas.

Actions may be performed in either or both the client and server
subsystems, depending on the nature of the action. This routing is
determined by the dispatcher.

### How to create a new action:
1 - 
2 - 
