[![Clojars Project](https://img.shields.io/clojars/v/dev.gersak/toddler.svg)](https://clojars.org/dev.gersak/toddler)  
[![Clojars Project](https://img.shields.io/clojars/v/dev.gersak/toddler-ui.svg)](https://clojars.org/dev.gersak/toddler-ui)  
[![Clojars Project](https://img.shields.io/clojars/v/dev.gersak/toddler-theme.svg)](https://clojars.org/dev.gersak/toddler-theme)  
[![Clojars Project](https://img.shields.io/clojars/v/dev.gersak/toddler-icons.svg)](https://clojars.org/dev.gersak/toddler-icons)  
[![Clojars Project](https://img.shields.io/clojars/v/dev.gersak/toddler-graphql.svg)](https://clojars.org/dev.gersak/toddler-graphql)  

# Initialization
Toddler icons are separated as subproject in icons directory. Reason for this is advanced compilation
and treeshaking. Shadow-cljs was bundling whole react-icons/fa file that is approximately 1.2Mb. This
is alot of unnecessary code.

To get around this situation please execute in terminal:
```
cd icons
npm link
```

This will expose icons that are used by toddler framework and tree shaking will work fine. 
