
# About
Toddler is a library containing a collection of [Helix](https://github.com/lilactown/helix.git) hooks,
components and functions. On top of this reusable code, UI components are built. You can test default components
in the **[SHOWCASE](https://gersak.github.io/toddler)**.

The **[showcase](https://github.com/gersak/toddler-showcase.git)** is a Git submodule within this repository,
used for testing, development, and documentation of Toddler UI components.

If you want to see the full API documentation, visit the [API Docs](https://gersak.github.io/toddler/codox/index.html).

For anyone looking for [mystic gist](https://gist.github.com/gersak/32b7079918e753e52012455710ae1ef2)
from london-clojurians meetup...


---
# IN SHORT
<p style="display=flex;justify-content=center" align="center">
  <img src="/docs/images/ISANOT.png" width="400"/>
</p>

# Architecture
<p style="display=flex;justify-content=center" align="center">
  <img src="/docs/images/Architecture.png" max-width="800"/>
</p>

---

[![Clojars Project](https://img.shields.io/clojars/v/dev.gersak/toddler.svg)](https://clojars.org/dev.gersak/toddler)  


[![Clojars Project](https://img.shields.io/clojars/v/dev.gersak/toddler-ui.svg)](https://clojars.org/dev.gersak/toddler-ui)  

[![Clojars Project](https://img.shields.io/clojars/v/dev.gersak/toddler-icons.svg)](https://clojars.org/dev.gersak/toddler-icons)  

[![Clojars Project](https://img.shields.io/clojars/v/dev.gersak/toddler-graphql.svg)](https://clojars.org/dev.gersak/toddler-graphql)  

---



## Quickstart
Toddler includes a built-in template to create a new project. In the following commands,
we refer to the project as `new.project`. Feel free to replace it with your preferred name.


#### Windows
```pwsh
clj -Sdeps '{:deps {dev.gersak/toddler-template {:mvn/version \"0.5.4\"}}}' -M -m toddler.start new.project
```

#### Linux/OSx
```sh
clj -Sdeps '{:deps {dev.gersak/toddler-template {:mvn/version "0.5.4"}}}' -M -m toddler.start new.project
```

```sh
cd new.project
npm run dev
# Navigate to http://localhost:8000
```

#### CSS
Toddler components use [shadow-css](https://github.com/thheller/shadow-css.git) to generate CSS files for component styling.

In the **"dev/"** folder, you’ll find the **compile_css.clj** file, which contains functions for indexing and compiling CSS in your codebase.  
During development, the function ```compile_css/go``` is called automatically from the `dev/user.clj` namespace, which is autoloaded.

If you have created new project with Toddler template command from [quickstart](#quickstart) than
theme css file should be available at `src/new/project/main.css` (path depends on how you named your project).

You can style Toddler default UI components by changing variables in that file.

#### Releasing
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

#### [Tauri](https://v2.tauri.app/)
I've been exploring possibilities for development of mobile and desktop applications.
My next goal is to build applications using Tauri,
focusing on integration and a proof-of-concept (PoC) project.

#### Drag-and-drop
DnD is another area I want to cover, leveraging functions, hooks, and signal events.

#### Documentation tool
Showcase project is written using Toddler. Skelet with navigation menu, action bar where links
are positioined and content area to display **markdown** docs is **344** lines of Clojurescript code
that can be found [here](https://github.com/gersak/toddler/blob/main/ui/src/toddler/dev.cljs).

It wasn't hard, to implement but **it is missing search and generated static HTML files** to meet minimum
of industry standard tools like [Docusaurus](https://docusaurus.io/)

#### Complete Documentation
Not all functions, hooks, and components are currently showcased or documented. Expanding and refining the documentation is a key priority.
