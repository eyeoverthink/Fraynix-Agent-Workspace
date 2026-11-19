# Edition 181 implementation plan

## Reuse Fray in one reviewed workflow

1. Use the Workbench as the entry point. Chat sends a potential converter request to Java for classification. The Workbench also supports explicit action selection. Review remembers the exact action and wording.
2. Reuse the output of the existing FraynixBoot compiler. Compile the expression **inches * 25.4** using FrayBytecode.Compiler.compile(source).serialise(). The source commit is recorded in the receipt. The Workbench reads the compiled program's input and constant and accepts its fixed multiply-and-return instruction sequence.
3. Build a self-contained HTML converter. Chromium uses integer decimal arithmetic and Java evaluates the Fray numeric calculation. Compare the real screen text and input values. Include zero, negative and positive values, a fraction, the largest allowed magnitude, a value near that limit, incorrect syntax, a blank field and overflow.
4. Save only after Java accepts the Chromium observations. Link the original browser checks, Fray source, compiler output and exact HTML bytes. Recover on restart using the same artifact and evidence. Confirm the exported HTML runs directly from disk.

The original FraynixBoot compiler is available in the owner's checkout. The approved formula and its 36-byte build output travel with this edition. [Fray compiler and VM](https://github.com/eyeoverthink/fraynixboot/blob/d93947154a1f21dcfd8dfcc97f27439fed6e3275/src/main/java/fraymus/lang/FrayBytecode.java)

## Next phase

Add a second conversion with a different program shape, such as Celsius to Fahrenheit. Use the original Fray compiler to produce its bytecode, then measure behavior on fixtures reserved independently of development. Before expanding to general source translation, define supported types and operators, reject unsupported instructions explicitly, and run the emitted artifacts against the same held-out examples.
