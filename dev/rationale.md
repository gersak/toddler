
## Love-Hate Relationship with UI Development

I've been coding UI for many years. Over time, it has been an emotional experience.
**I hate UI programming**, and I love it. Few things in programming feel as satisfying
as seeing what you imagined come to life exactly the way you wanted.

But why is this **so hard** most of the time? Styling, routing, positioning, responsiveness—
there are just so many details to handle to achieve that **perfect UI**.

You look for help, turn to existing frameworks, and feel like you're making progress.
Then, after days, weeks, or months, everything **breaks**. Your decisions backfire,
your UI turns to chaos, and you feel like you're back to square one. **Reset. Repeat.**

I've used so many great libraries—[styled-components](https://styled-components.com/),
[react-table](https://tanstack.com/table/latest), [react-router](https://reactrouter.com/),
[react-spring](https://www.react-spring.dev/), [framer-motion](https://motion.dev/), and more.
Each helped in some way, but over time, every single one of them fell short.

Some bloated my application, others were exhausting to integrate with ClojureScript,
and many became overloaded with features—sometimes leaning too heavily on JSX.

That's why I built **Toddler**. A lightweight, flexible UI system that helps me cut through  
this frustration. If you’ve ever felt the same, maybe it can help you too.


## A Smarter Approach to Components

During one of these UI struggles, a **brilliantly stupid** idea was born:
What if components were just **placeholders** in the code, and I could swap them out
using a shared context?

```clojure
(defmacro defcomponent
  "Defines a component that retrieves its implementation from the UI context."
  [_name key]
  `(helix.core/defnc ~_name [props# ref#]
     {:wrap [(toddler.ui/forward-ref)]}
     (let [components# (helix.hooks/use-context __components__)
           component# (get components# ~key)
           children# (helix.children/children props#)]
       (when component#
         (helix.core/$ component# {:ref ref# :& props#} children#)))))

(defmacro g
  "Retrieves and renders a component from the UI context.
  
  Example: (g :button {:className "positive"} "Good day")"
  [key & stuff]
  `(when-let [component# (get (helix.hooks/use-context __components__) ~key)]
     (helix.core/$ component# ~@stuff)))
```

This means I can **swap components dynamically** by changing the UI context.
Need to replace a dropdown? Just change the context—it will propagate automatically
throughout the app.


## How It Works (With an Example)

Let's say we want to create a dropdown that can **swap implementations** at runtime.
We define a generic `dropdown` component that looks for its implementation in the context.

```clojure
(ns toddler.showcase.components
  (:require
   [helix.core :refer [defnc $]]
   [helix.dom :as d]
   [helix.hooks :as hooks]
   [toddler.ui :as ui :refer [defcomponent]]
   [toddler.ui :refer [UI]))

;; Declare a general dropdown component
(defcomponent dropdown :my/dropdown)

(defnc dropdown-impl-1 [] (d/div "This is the first implementation"))

(defnc dropdown-impl-2 [] (d/div "This is the second implementation"))

;; Swap dropdown implementations dynamically
(defnc MyApp []
  (let [[state set-state!] (hooks/use-state nil)]
    ($ ui/row
       {:align :center}
       ($ ui/column
          {:align :center}
          ($ ui/row
             ($ UI
                {:components {:my/dropdown (if state dropdown-impl-2 dropdown-impl-1)}}
                ($ dropdown)))
          ($ ui/row
             ($ ui/button {:on-click #(set-state! not)} "Change"))))))
```

Clicking the **"Change"** button swaps the dropdown component between two implementations.
With **Toddler**, components become placeholders, and **UI customization becomes effortless**.


## Try It Out

<div id="components-example"></div>

All reusable components are declared in a single namespace, then used via
[Helix](https://github.com/lilactown/helix)'s element creation macro ([`$`](https://github.com/lilactown/helix/blob/master/docs/creating-elements.md)).

```clojure
(defnc MyAppBasedOnToddlerComponents
  {:wrap [(ui/wrap-ui toddler.ui.components/default)]}
  []
  ;; You can use components from toddler.ui namespace because
  ;; they are mapped to toddler.ui.components/default
  ($ Stuff))

(defnc MyAppWithSpecialPopup
   {:wrap [(ui/extend-ui {:popup my-special-implementation})]}
   []
   ;; Override or extend the current UI context dynamically
   )
```


## Toddler Is Not a Component Library

Yes, Toddler includes ready-to-use components, but that’s **not** its primary purpose.  
You are encouraged to create **your own** components, tailored to your needs.  

The real power of Toddler lies in **hooks** and **utility functions** found in the  
`toddler.core`, `toddler.routing`, `toddler.popup` namespaces.
Components exist mainly to **showcase that the system works**.  

If you’re tired of fighting with UI frameworks and want more flexibility,  
give Toddler a try. Maybe it will make your UI journey a little less painful. 🚀  

