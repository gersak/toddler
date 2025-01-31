## Confession

I've been coding UI for many years. Through this many years it has
been very a emotional expirience. **I hate UI programming** and I love
it. There are few things in programming that rewards you with satisfaction
as much as seeing the thing you imagioned exactly the way you wanted.

Why is this **so hard** most of the time. Styling, routing, positioning,
responsivness, etc. There is just so much details to take into account
and implement to have that good feeling.

On the way you look for help and turn to existing implementations, frameworks
and this takes you one step closer to you goal. Than after few days, weeks,
months that decission blows up and everything turns to shit for a moment taking
you two steps back. Reset! Again... Repeat

I've been using so many good libraries like [styled-components](https://styled-components.com/),
[react-table](https://tanstack.com/table/latest), [react router](https://reactrouter.com/),
[react-spring](https://www.react-spring.dev/), [framer-motion](https://motion.dev/) etc.
This are all great libraris but along the way every single one of those library fell off.

Either it was bloating my application, or it was exhausting integrating it with
Clojurescript, or it became super featured and sometimes those libraries wen't to
much in JSX direction. 

Toddler is codebase that helps me get through this sometimes painfull expirience
and I hope that it will benefit you as well.


## Component system

Once upon a time during that wonderfull development cycle briliantly stupid idea was
born. Components are functions. What if I provided those functions through react
context and in my code I use placeholders that will extract components from
context and render it. 


```clojure
(defmacro defcomponent
  "Wrapper macro around helix.core/defnc function that
  will try to pull key from __components__ and render
  found component."
  [_name key]
  `(helix.core/defnc ~_name [props# ref#]
     {:wrap [(toddler.ui/forward-ref)]}
     (let [components# (helix.hooks/use-context __components__)
           component# (get components# ~key)
           children# (helix.children/children props#)]
       (when component#
         (helix.core/$ component# {:ref ref# :& props#} children#)))))

(defmacro g
  "Macro that will try to pull key component from __components__ context
  and render it with helix.core/$ macro
  
  I.E.  (g :button {:className \"positive\"} \"Good day\") "
  ^{:style/indent 0
    :cljfmt/ident [:form]}
  [key & stuff]
  `(when-let [component# (get (helix.hooks/use-context __components__) ~key)]
     (helix.core/$ component# ~@stuff)))
```

Then I could swap components by swaping context. Result would be easier management.
Just replace your old dropdown with new one and it should propagate through out
whole application where dropdown placeholder was used. Or even override current
context by replacing component.


#### How does it work
Lets say that I wan't to create dropdown element that will receive some props
and show me options and stuff. Following components system from above we
declare ```dropdown``` component that will look for dropdown implementation under
```:my/dropdown``` key of provided components context.

Following example will show two "implementations" of dropdown that are swaped
on "Change" button click. This should demonstrate that dropdown symbol will
be rendered properly by Helix create element macro.


```clojure
(ns toddler.showcase.components
  (:require
   [helix.core :refer [defnc $]]
   [helix.dom :as d]
   [helix.hooks :as hooks]
   [toddler.ui :as ui :refer [defcomponent]]
   [toddler.provider :as provider]))

;; Declare dropdown component in general
(defcomponent dropdown :my/dropdown)

(defnc dropdown-impl-1
  []
  (d/div "This is first implementation"))

(defnc dropdown-impl-2
  []
  (d/div "This is second implementation"))

;; In this app current components are still
;; components from toddler.ui so we need to
;; provide context for :my/dropdown component
(defnc MyApp
  []
  (let [[state set-state!] (hooks/use-state nil)]
    ($ ui/row
       {:align :center}
       ($ ui/column
          {:align :center}
          ($ ui/row
             ;; This is 
             ($ provider/UI
                {:components {:my/dropdown (if state dropdown-impl-2 dropdown-impl-1)}}
                ($ dropdown)))
          ($ ui/row
             ($ ui/button
                {:on-click (fn []
                             (set-state! not))}
                "Change"))))))
```

#### Try it out
<div id="components-example"></div>

Idea is to have as much of reusable components declared in single namespace. Than use that declarations
through [Helix](https://github.com/lilactown/helix) element creation macro([$](https://github.com/lilactown/helix/blob/master/docs/creating-elements.md)).

Components used in this showcase and many others can be found in ```toddler.ui.components```
namespace. Implementations are in ```toddler.ui.*``` namespaces respectevily.

```clojure
(defnc MyAppBasedOnToddlerComponents
  {:wrap [(provider/wrap-ui toddler.ui.components/default)]}
  []
  ;; Here goes your code where you can use components from
  ;; toddler.ui namespace because they are mapped to 
  ;; toddler.ui.components/default implementation
  ($ Stuff))

(defnc MyAppWithSpecialPopup
   {:wrap [(provider/extend-ui {:popup my-special-implementation})]}
   []
   ;; It is possible to extend current components context
   ;; or override and replace current component by changing
   ;; context
   )
```

## Toddler is NOT component library
In this showcase you will find many components that are ready to use and you
are free to use them. Even more than using existing components
**you are encuraged to make your own components**
using Toddler codebase. ```toddler.core``` namespace is really core of this
framework and in it you will find hooks that address many of challenges above.

That is focus of this library, **hooks** and utility **functions**. Components
are byproduct of showcasing that code is working. And there is alot of code :sweat_smile:
