# Dependency Highlighter

![Build](https://github.com/pberdnik/dependency-highlighter/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/22926-dependency-highlighter.svg)](https://plugins.jetbrains.com/plugin/22926-dependency-highlighter)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/22926-dependency-highlighter.svg)](https://plugins.jetbrains.com/plugin/22926-dependency-highlighter)

<!-- Plugin description -->

This plugin helps you track file dependencies.
To initiate the analysis, click on the "Run analysis" button.
When you open a file, the plugin displays all the files that use it and all the files it uses on the right panel.
Files that depend on the current one are highlighted in purple, and files it depends on are highlighted in green
in the project panel.
<!-- Plugin description end -->

<img src="https://pberdnik.github.io/res/example.png" alt="Screenshot of IDE with plugin"/>

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Dependency
  Highlighter"</kbd> >
  <kbd>Install</kbd>

- Manually:

  Download the [latest release](https://github.com/pberdnik/dependency-highlighter/releases/latest) and install it
  manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template

[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
