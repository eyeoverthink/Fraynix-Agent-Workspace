# Third-party notices and provenance

Project: **Fraynix Studio**, maintained by Vaughn Scott / eyeoverthink.

This independent repository was assembled from the supplied `physics_mario` application, the working Vaughn-Scotts-Fraymus-Engine integration, and selected Full-AI-System Java modules. Existing package names, source comments, assets, and the physics binary are retained. These imports are recorded in `docs/provenance/IMPORT_MANIFEST.json`. The supplied editor snapshot contained no application-wide license and no complete original author/art inventory. This document does not assign ownership or grant rights to inherited material.

## Library sources

These links identify the upstream projects. Consult the license text shipped by the corresponding version; a brand name or wrapper does not replace upstream terms.

| Dependency | Upstream / notices |
| --- | --- |
| LWJGL | [LWJGL](https://github.com/LWJGL/lwjgl3), BSD-style license in its distribution |
| JOML | [JOML](https://github.com/JOML-CI/JOML), MIT license |
| Gson | [Gson](https://github.com/google/gson), Apache License 2.0 |
| JSON-java (local model protocol) | [JSON-java](https://github.com/stleary/JSON-java), consult notices in the bundled 20231013 artifact |
| imgui-java | [imgui-java](https://github.com/SpaiR/imgui-java), MIT license |
| Dear ImGui | [Dear ImGui](https://github.com/ocornut/imgui), MIT license |
| JBox2D | [JBox2D](https://github.com/jbox2d/jbox2d), BSD-style license; exact included artifact version unconfirmed |
| Gradle wrapper | [Gradle](https://github.com/gradle/gradle), Apache License 2.0 |
| JUnit 4 | [JUnit 4](https://github.com/junit-team/junit4), Eclipse Public License 1.0 |
| Hamcrest | [Java Hamcrest](https://github.com/hamcrest/JavaHamcrest), BSD-style license |

Downloaded dependency JARs retain their embedded notices. The build distribution carries the dependency JARs rather than repackaging their classes into a notice-stripping fat JAR.

## Vendored physics binary

File: `libs/jbox2d-library.jar`

SHA-256:

```text
8d6d78431b0403a4236b7e266cf6c62aee63332d8381433150a16f202c574350
```

This binary is copied intact from the supplied application. Its original version and provenance have not been established from the checkout, so no precise version is invented.

## Graphics and fonts

The sample graphics under `assets/images` are preserved from the local application, including Mario-style tiles and decorations. No artwork provenance or license file was supplied. These files are not covered by a newly invented Fraymus license; use replacement assets with documented permissions for a separately distributed product.

No Microsoft font is included. The application may load Segoe UI from the user's existing Windows installation. If unavailable, it uses ImGui's built-in font.

## Application license

No project-wide redistribution license was chosen during this private import. Original and third-party rights remain with their respective holders. A future license choice should account for inherited source, graphics, the vendored library, imported Fraynix modules and separately installed model weights. Ollama and model weights are not included in this repository or application distribution.


## Imported Mario engine

The full gameplay and associated assets were imported from the user-supplied MarioYoutube-master source. Its original MIT notice, copyright 2022 codingminecraft, is preserved in licenses/MarioYoutube-LICENSE.txt. Imported-file hashes and provenance are recorded in docs/mario-import-provenance.json. Studio shader fixes and existing host services are retained.

The sensory cortex adapts the user-supplied fraymus.neural.HrmNeuromorphic implementation; the original source is preserved in docs/neural/HrmNeuromorphic-original.java.txt.
