# Changelog

## [1.4.15-1.20.1-fabric] - 2026-04-08

### Added
- Ported the mod to Minecraft **1.20.1**.
- Full support for **Fabric Loader**.
- Updated to **Java 17**.

### Changed
- Updated all dependencies to be compatible with 1.20.1 (Fabric API, Yarn mappings).
- Refactored rendering logic to comply with modern Minecraft rendering standards.
- Updated entity interaction logic to ensure compatibility with 1.20.1.
- Integrated ShadowJar for better dependency management and to resolve class loading issues (e.g., `CatalogManager`).

### Fixed
- Fixed GUI rendering issues that were present during the porting process.
- Fixed an issue where the PC editing menu would not open when interacting with components.
- Resolved various runtime compatibility errors related to Java 17 and relocated libraries.

## Development Plans (Branches)

Three main development branches have been defined for the project:

1.  **Legacy**:
    *   Contains already released versions and versions that have become obsolete for any reason (critical bugs, etc.).
2.  **Port/Rewrite**:
    *   Port of the original mod and its refinement as intended by the original author.
    *   Versions: **1.x.x**.
3.  **Reimagining**:
    *   Personal ideas, custom components, new devices, and expanded functionality.
    *   Versions: **2.x.x**.

