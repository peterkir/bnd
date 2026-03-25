# Bnd and Bndrun Support for VS Code

A Visual Studio Code extension providing rich language support for **bnd** (`.bnd`) and **bndrun** (`.bndrun`) files used in [OSGi](https://www.osgi.org/) development with the [bnd/bndtools](https://bnd.bndtools.org) toolchain.

## Features

### Syntax Highlighting

Full TextMate grammar covering:

- **Instructions** – Lines starting with `-keyword:` (e.g., `-buildpath:`, `-runbundles:`, `-privatepackage:`) highlighted as control keywords.
- **OSGi Headers** – Standard manifest headers (`Bundle-SymbolicName:`, `Export-Package:`, `Import-Package:`, etc.) highlighted as storage types.
- **Properties** – Lowercase key-value properties highlighted as variables.
- **Macros** – `${macroname}` expressions, with the macro name highlighted as a function.
- **Version ranges** – `[1.0,2.0)` and `(1.0,2.0]` highlighted as numeric constants.
- **String literals** – Single and double-quoted strings.
- **Directives** – `key:=value` attribute/directive syntax.
- **Continuation lines** – Trailing backslash `\` line continuations.
- **Comments** – `#` line comments and `//` inline comments.

### IntelliSense Completions

Trigger completions with `Ctrl+Space` (or automatically on `-`, `$`, `{`, `:`):

- **153 bnd instructions** – All documented `-instruction:` entries with detail and documentation, e.g.:
  - `-buildpath`, `-runbundles`, `-runrequires`, `-runfw`, `-privatepackage`, `-exportcontents`
  - `-dsannotations`, `-metatypeannotations`, `-cdiannotations`
  - `-resolve`, `-standalone`, `-runee`, `-runvm`, `-runproperties`
  - and many more…
- **138 bnd macros** – All documented `${macro}` entries when cursor is inside `${...}`:
  - `${bsn}`, `${version}`, `${range}`, `${repo}`, `${githead}`, `${tstamp}`
  - `${filter}`, `${filterout}`, `${replace}`, `${sort}`, `${join}`
  - and many more…
- **48 OSGi headers and bnd pseudo-headers** – Standard manifest keys with documentation.

All completion items include:
- A **detail** line showing the full syntax signature.
- **Documentation** from the official bnd docs rendered as Markdown.
- **Snippet insert text** placing the cursor after `: ` for easy value entry.

### Hover Documentation

Hover over any instruction keyword, OSGi header, or macro name to see:
- The full syntax signature (bold).
- A documentation summary from the bnd reference docs.

## Installation

### From VSIX

1. Download the `.vsix` file.
2. In VS Code open the Extensions view (`Ctrl+Shift+X`).
3. Click the `...` menu → **Install from VSIX…** and select the file.

### Build from Source

```bash
cd vscode-bnd
npm install
npm run compile
npx @vscode/vsce package --allow-missing-repository --no-git-tag-version
# Produces vscode-bnd-0.1.0.vsix
```

Then install the generated `.vsix` as above.

## Usage

The extension activates automatically for any file with the `.bnd` or `.bndrun` extension.

### Example `bnd.bnd`

```properties
Bundle-SymbolicName: com.example.mybundle
Bundle-Version:      1.0.0

-buildpath: \
    osgi.core;version='[7,8)', \
    osgi.annotation;version='[8,9)'

Export-Package: com.example.api;version='${Bundle-Version}'
Private-Package: com.example.internal.*

-dsannotations: *
```

### Example `launch.bndrun`

```properties
-standalone: \
    https://repo.maven.apache.org/maven2/,index;name=central

-runfw: org.apache.felix.framework;version='[7,8)'
-runee: JavaSE-17

-runrequires: \
    osgi.identity;filter:='(osgi.identity=com.example.mybundle)'

-runbundles: \
    com.example.mybundle;version='[1.0.0,1.0.1)'
```

## About

This extension is part of the [bnd/bndtools](https://github.com/bndtools/bnd) project.

- **bnd documentation**: <https://bnd.bndtools.org>
- **Issue tracker**: <https://github.com/bndtools/bnd/issues>
