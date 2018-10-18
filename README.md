# Kanopi

Record, explore, analyze, discover.

Targeting a new sweet spot between 

                | Rigid               | Flexible
_______________________________________________________________
Unstructured    | Key-Value Store?    | Plain text, notebooks
Semi-structured | Purpose-built apps  | Outlines, Spreadsheets
Structured      | Relational Database | <<<<!(_Kanopi_)!>>>>

## Rationale

Accelerating change is not all we deal with. Such change increases the
complexity of our world as we lack both the tools and the time to rapidly
scalably simplify the new. Kanopi is an attempt to fight complexity at
scale with the only system that works at scale: learning.

## Usage: TODO

### The following environment variables must be set:

#### Credentials for accessing my.datomic.com
- KANOPI_DATOMIC_USERNAME
- KANOPI_DATOMIC_PASSWORD

### Running Tests

$ lein test
$ lein test <namespace>
$ lein test :only <namespace>/<test-fn>

## License: TODO

Copyright © 2018 Duhsoft
