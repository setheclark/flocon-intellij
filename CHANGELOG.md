<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Flocon Intellij Network Inspector Changelog

## [Unreleased]

## [0.2.2] - 2026-05-13

### Fixed

- Fixed logic for normalizing line separators

## [0.2.1] - 2026-05-13

### Changed

- Gracefully handle binary request/response bodies.
- Updated Flocon version to [1.8.1](https://github.com/openflocon/Flocon/releases/tag/1.8.1). This will prevent the creation of the ~/Desktop/Flocon directory.

## [0.2.0] - 2026-04-28

### Changed

- Add "Show hidden tabs" button for tool window tabs.
- Redesigned the call details UI.
- UI Rewrite using Compose

### Fixed

- Fixed icon size for "View" -> "Tool Windows" menu
- Fix plugin instructions to use the correct tool window name

## [0.1.9] - 2026-03-02

### Changed

- Updated Flocon verion to [1.7.8](https://github.com/openflocon/Flocon/releases/tag/1.7.8).  This addresses issues parsing `operationName` from graphQl query params.

## [0.1.8] - 2026-02-23

### Fixed

- Fix environment variable logic
- Workaround for GraphQl mutation operationName resolution

## [0.1.7] - 2026-02-17

### Fixed

- Message service is no longer recreated with each project.
- Fix icon coloring

### Changed

- Network details panel now opens as a new tab in the network tool window.
- Move icons to left side of network list window.
- Removed storage settings.

### Added

- Columns in the network list are now togglable.
- Ability to open network details as an editor window (configurable in settings)
- A changelog :)

[Unreleased]: https://github.com/setheclark/flocon-intellij/compare/v0.2.2...HEAD
[0.2.2]: https://github.com/setheclark/flocon-intellij/compare/v0.2.1...v0.2.2
[0.2.1]: https://github.com/setheclark/flocon-intellij/compare/v0.2.0...v0.2.1
[0.2.0]: https://github.com/setheclark/flocon-intellij/compare/v0.1.9...v0.2.0
[0.1.9]: https://github.com/setheclark/flocon-intellij/compare/v0.1.8...v0.1.9
[0.1.8]: https://github.com/setheclark/flocon-intellij/compare/v0.1.7...v0.1.8
[0.1.7]: https://github.com/setheclark/flocon-intellij/commits/v0.1.7
