# Provenance and repository boundaries

Fraynix Studio was assembled on October 22, 2026 as an independent private project for Vaughn Scott / eyeoverthink.

## Inputs

1. Supplied `physics_mario` editor source and assets, continued through the working Vaughn-Scotts-Fraymus-Engine checkout.
2. The working editor integration: complete scene observations, strict proposal protocol, detached physics validation, persistent apply/undo and live local-model panels.
3. Selected Java modules from Full-AI-System, already exported into that integration as factory-runtime, factory-kernel and factory-models.

`provenance/IMPORT_MANIFEST.json` records hashes of the imported source/build files before Studio adaptations, the source repository's base commit and the fact that uncommitted working changes were included. It is a provenance snapshot, not a final-file integrity manifest.

## Independence

- A fresh Git repository and one new origin; no copied `.git` history or submodules.
- All module source directories reside under this checkout.
- No build-time fallback to another drive or repository.
- Distinct application/distribution names and default project/catalog directories.
- Existing Java package names are retained for serialized component compatibility.
- Ollama is external local infrastructure. Configuring the same service does not merge repositories or projects.
- No automatic synchronization back to the source repositories.

The original repositories were read as inputs. Their remotes, branches, tracked files and running application were not replaced by this extraction.

## Preserved rights

Retain inherited source comments and library notices. Keep model licenses with the separately installed weights. No new license is implied for the supplied sample art or inherited source merely because it now appears in an independently named private repository.
