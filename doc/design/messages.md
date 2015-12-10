Some thoughts on kanopi's message oriented architecture.


Messages are created by user actions. They are sent to a dispatcher
which handles our polymorphism based on two levels of context: the
client's current 'mode' and whether the message represents a 'request'
or a 'response'. This could be handled differently, but some messages
are sent to multiple recipients, which makes a single multimethod
unsuitable.

NOTE: The model is client-centric, which arises from the fact that the
initial dispatcher lives in the client. There will later be another
dispatcher in Onyx handling broader stuff.

User action -> request message

Request message -> response or transport message

transport messages carry another message as their noun. they include
instructions for how to transport the message from one piece of the
architecture to another and how to handle the response (if
synchronous)

response message -> affect some change to the system and optionally
produce some additional messages of any type
