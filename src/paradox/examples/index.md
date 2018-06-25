@@@ index

* [Construction](construction.md)
* [Construction with Effects](effects.md)
* [Construction with Subtypes](subtypes.md)
* [Composition](composition.md)
* [Attenuation](attenuation.md)
* [Abstraction](abstraction.md)
* [Modulation](modulation.md)
* [Revocation](revocation.md)
* [Amplification](amplification.md)
* [Delegation](delegation.md)
* [Gatekeeper](gatekeeper.md)
* [Dynamic Sealing](dynamic_seal.md)
* [Membrane](membrane.md)

@@@

# Examples

This is a series of examples showing capabilities in action using `ocaps`.  

**If you are not familiar with capabilities, please read through @ref:[the guide](../guide/index.md) for a more in-depth explanation.**

## Imports

All the examples given rely on the following imports:

```
import ocaps._
import ocaps.macros._
```

## Construction

This section shows creation of capabilities and direct use.

* @ref:[Construction](construction.md)
* @ref:[Construction with Effects](effects.md)
* @ref:[Construction with Subtypes](subtypes.md)

## Structural Patterns

This section shows structural patterns that do not change behavior.

* @ref:[Composition](composition.md)
* @ref:[Attenuation](attenuation.md)
* @ref:[Abstraction](abstraction.md)

## Behavioral Patterns

This section shows patterns that change the behavior of capabilities, typically adding side effects or blocking execution completely.

* @ref:[Modulation](modulation.md)
* @ref:[Revocation](revocation.md)

## Operational Patterns

This section shows capabilities used together or passed around.

* @ref:[Amplification](amplification.md)
* @ref:[Delegation](delegation.md)
* @ref:[Gatekeeper](gatekeeper.md)

## Confinement Patterns

This section shows capabilities being rendered inaccessible or made accessible through a membrane.

* @ref:[Dynamic Sealing](dynamic_seal.md)
* @ref:[Membrane](membrane.md)