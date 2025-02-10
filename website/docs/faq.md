# FAQ

## **When exporting a product I have only SWIFT_TYPEDEFS or swift_... available in my Kotlin code.**

That's mean your product is not compatible with ObjC.

During the compilation of the package, the swift compiler generate a ObjC header with all compatible code.

The exported ObjC code can be found inside
```
[project]/build/spmKmpPlugin/[cinteropName]/scratch/release/[product].build/module.modulemap

```

or

```
[project]/build/spmKmpPlugin/[cinteropName]/scratch/release/[product].framework/Modules/module.modulemap
```

This header contains every available ObjC code inside the Package.

A Pure Swift package has only generic content like `SWIFT_TYPEDEFS`.
