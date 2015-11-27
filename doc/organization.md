# Why
Code organization enables our development, testing and deployment
methods. It affects how we think about Kanopi. It defines our
possibility space. It gives us api and language boundaries which
support separations of concerns, good documentation and testing. This
is important.

## Patterns

###  Folders communicate purpose, deployment, and development.
###  Namespaces communicate content, meaning and usage.

## Folder structure
kanopi-peer/
            client/
                   src/
                   src-cljs/
                   src-cljc/
                   dev/
                    ; repl
                   dev-cljs/
                    ; repl + devcards (cljs client)
                   test/
                    ; clj client
                   test-cljs/
                    ; cljs client
            server/
                   src/ 
                   dev/
                    ; repl
                   test/
                    ; server, server + client via selenium
            shared/
                   src/
                   src-clj/
                   test/
                    ; clj server + clj client, clj server + cljs client via selenium

## Namespace structure (repeated in each leaf from the above folder structure)
kanopi-peer.
            model.
            view.
            controller.
