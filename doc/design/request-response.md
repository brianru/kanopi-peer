Why are handlers split into request and response functions?

1. There may be multiple layers of side-effecting message handling -- a remote
   message handler responds AND a local message handler responses.

2. A complete request may not be generateable by the client requestor alone --
additional global context may be necessary which you don't want the UI to have
but the request handler does have.

3. 