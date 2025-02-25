# FAQ

## **What's a Pure Swift Package ?**

The **Pure** word means the Package is written only in Swift, like 90% of the existing package.

The Swift language is commonly used in the Apple Platform and [is not compatible with KMP, but only with ObjC](https://kotlinlang.org/docs/native-objc-interop.html#importing-swift-objective-c-libraries-to-kotlin).

Unlike Kotlin with Java, the interoperability between Swift and ObjC needs to be [explicitly declared](https://www.hackingwithswift.com/example-code/language/what-is-the-objc-attribute), but it's like a **downgrade**, and nobody want to do that.

Some old libraries, like Firebase, are mainly written in ObjC or a few of them want to have the compatibility with ObjC.

So mainly, Apple targets libraries that are written in Swift; you can check inside their repository, the Languages section to see which language they are using.

But sometimes, like [google nearby](https://github.com/frankois944/spm4Kmp/issues/68), a wrapper Swift <- ObjC is made to increase the compatibility with Swift and make it unexportable to Kotlin.

## **When exporting a product I have only SWIFT_TYPEDEFS or swift_... available in my Kotlin code.**

That means your product is not compatible with ObjC.

During the compilation of the package, the Swift compiler generates an ObjC header with all compatible code.

The exported ObjC code can be found inside
```
[project]/build/spmKmpPlugin/[cinteropName]/scratch/release/[product].build/module.modulemap

```

or

```
[project]/build/spmKmpPlugin/[cinteropName]/scratch/release/[product].framework/Modules/module.modulemap
```

This module contains every available ObjC header inside the Package.

A Pure Swift package has only generic content like `SWIFT_TYPEDEFS`.
