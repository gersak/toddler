

[![Clojars Project](https://img.shields.io/clojars/v/dev.gersak/toddler.svg)](https://clojars.org/dev.gersak/toddler)  
[![Clojars Project](https://img.shields.io/clojars/v/dev.gersak/toddler-ui.svg)](https://clojars.org/dev.gersak/toddler-ui)  
[![Clojars Project](https://img.shields.io/clojars/v/dev.gersak/toddler-theme.svg)](https://clojars.org/dev.gersak/toddler-theme)  
[![Clojars Project](https://img.shields.io/clojars/v/dev.gersak/toddler-icons.svg)](https://clojars.org/dev.gersak/toddler-icons)  
[![Clojars Project](https://img.shields.io/clojars/v/dev.gersak/toddler-graphql.svg)](https://clojars.org/dev.gersak/toddler-graphql)  


# About
Toddler is a library containing a collection of hooks and functions that I found useful throughout my career.
On top of this reusable code, I've built UI components that you can test in the [showcase](https://gersak.github.io/toddler).

The **Showcase** is a Git submodule within this repository, used for testing, development, and documentation of Toddler UI components.

If you want to see the full API documentation, visit the [API Docs](https://gersak.github.io/toddler/codox/index.html).

## Quickstart
Toddler includes a built-in template to create a new project. In the following commands,
we refer to the project as `new-project`. Feel free to replace it with your preferred name.

```sh
clj -Sdeps '{:deps {dev.gersak/toddler-ui {:mvn/version "0.1.0"}}}' -m toddler.start new.project
cd new.project
npm run dev
# Navigate to http://localhost:8000
```

## CSS
Toddler components use [shadow-css](https://github.com/thheller/shadow-css.git) to generate CSS files for component styling.

In the **"dev/"** folder, you’ll find the **compile_css.clj** file, which contains functions for indexing and compiling CSS in your codebase.  
During development, the function ```compile_css/go``` is called automatically from the `dev/user.clj` namespace, which is autoloaded.

## Releasing
To compile a production-ready release, run:

```sh
npm run release
```

This will generate both **JS** and **CSS** files in the **build/** directory, ready for distribution.

## Development
First, initialize the Showcase submodule (only needed once):

```sh
git submodule init showcase
git submodule update showcase
```

Then, start the development server:

```sh
npm run dev
```

# Where is this going?

I've been exploring possibilities for mobile and desktop applications.
My next goal is to build applications using [Tauri](https://v2.tauri.app/),
focusing on integration and a proof-of-concept (PoC) project.

Drag-and-drop (DnD) is another area I want to cover, leveraging functions, hooks, and signal events.

## Complete Documentation

Not all functions, hooks, and components are currently showcased or documented. Expanding and refining the documentation is a key priority.
