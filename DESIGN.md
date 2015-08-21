## Schema

### Goals

#### Both fact attributes and values can be references to either thunks or values.

#### Values are simply promoted to thunks.
The goal is for everything to be described, thus to be thunks.

#### Literal values are shared.
A fact have a value datom which refers to either 1) a literal entity
or 2) a thunk.

#### Thunks may have special facts which describe their behavior when
they are themselves embedded in facts (as either attributes or values).

#### Literal entities contain all information necessary for rendering.

#### Literals maintain no history. All other entities retain history.

## Interface

### Fluid creation of thunks and literals via facts.
As-you-type quick-search for matching thunks (by label) and literals
(by string value). Simple in-place definition of particular literal
types or declaration that that the entity is a thunk.
