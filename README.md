

[![Clojars Project](https://img.shields.io/clojars/v/dev.gersak/toddler.svg)](https://clojars.org/dev.gersak/toddler)  
[![Clojars Project](https://img.shields.io/clojars/v/dev.gersak/toddler-ui.svg)](https://clojars.org/dev.gersak/toddler-ui)  
[![Clojars Project](https://img.shields.io/clojars/v/dev.gersak/toddler-theme.svg)](https://clojars.org/dev.gersak/toddler-theme)  
[![Clojars Project](https://img.shields.io/clojars/v/dev.gersak/toddler-icons.svg)](https://clojars.org/dev.gersak/toddler-icons)  
[![Clojars Project](https://img.shields.io/clojars/v/dev.gersak/toddler-graphql.svg)](https://clojars.org/dev.gersak/toddler-graphql)  

# About
Toddler library is collection of hooks and functions that I found usefull throughout my carreer.
On top of this reusable code I've built UI components that you can test in [showcase](https://gersak.github.io/toddler).

Showcase is git submodule in this repo and is used to test, develop and document Toddler UI components.

If you wan't to see code documentation visit [API](https://gersak.github.io/toddler/codox/index.html).


## Quickstart
```
clj -Sdeps '{:deps {dev.gersak/toddler-ui {:mvn/version "0.1.0"}}}' -m toddler.start new.project
cd new.project
npm run dev
# Navigate to http://localhost:8000
```

## CSS
Toddler components use [shadow-css](https://github.com/thheller/shadow-css.git) to generate css files that style components.

In **dev/** folder you will find **compile_css.clj** file that has functions to index and compile css in your codebase.


## Releasing
Releases are compild into **build** folder. Both JS and CSS are compiled and ready for distribution.
```
npm run release
```

## Development
```
# Required only once
git submodule init showcase
git submodule update showcase

npm run dev
```
