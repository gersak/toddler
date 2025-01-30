## Intro
Toddler uses fixed DOM positioning for all popups. It uses react-dom
createPortal to mount popups on ```toddler.popup.Container``` component.

Popup container should be placed as close as posible to your mounted component.
It will create ```<div id="toddler-popups"></div>``` store it in Helix reference
and share it through \***container\*** context.

### POPUP AREA
Component is similar to ```popup/Container```. It will also create *div* element
and render all children inside of this *div* element. It keeps reference to created
*div* DOM element and provides it as **\*area\*** context.

Popup element can then use reference to DOM element to compute where to position on
current screen.

### POPUP ELEMENT
Uses popup area reference to compute where to position. Computing where to position
works in following logic. Popup element expects **preference** property or uses
**default-preference** to decide what positions are preferable.

This preference is than used to try and fit popup element on screen and first
position that can be fully visible is valid. ```popup/Element``` doesn't have
width or height by default. This div is adjusted to size of its children, so
whatever you put in, **that** is responsible for size of popup.

```clojure
(def default-preference
  [#{:bottom :left}
   #{:bottom :right}
   #{:top :left}
   #{:top :right}
   #{:bottom :center}
   #{:top :center}
   #{:left :center}
   #{:right :center}])
```

**offset** prop can be used to move popup element away from ```popup/Area```
DOM element.

By default ```popup/Element``` tracks window resize so that on resize popup
will reposition if necessary.

Popup element will use react *createPortal* function to render popup element
inside of **popup/Container**. So don't forget to create container component
somewhere above in component tree.

#### Example

<div id="popup-example"></div>


```clojure
(defnc popup-example
  []
  (let [[opened? set-opened!] (hooks/use-state false)
        [preference set-preference!] (hooks/use-state nil)
        [offset set-offset!] (hooks/use-state 10)]
    (<>
     ;; Row that controls configuration of popup element
     ;; in example above
     ($ ui/row
        {:className (css {:gap "1em"})}
        ($ ui/dropdown-field
           {:name "Position"
            :value preference
            :on-change set-preference!
            :placeholder "Choose position..."
            :options popup/default-preference})
        (d/div
         {:style {:max-width 100}}
         ($ ui/integer-field
            {:name "Offset"
             :value offset
             :on-change set-offset!})))
     ;; Popup button layout
     ($ ui/row
        {:align :center}
        ($ ui/column
           {:position :center}
           ;; Popup Area defintion
           ($ popup/Area
              ;; That holds one button to toggle
              ;; popup opened/closed
              ($ ui/button
                 {:on-click (fn [] (set-opened! not))}
                 (if opened? "Close" "Open"))
              ;; When it is opened, than show red popup
              (when opened?
                ($ popup/Element
                   {:offset offset
                    :preference (or
                                 (when (some? preference) [preference])
                                 popup/default-preference)}
                   (d/div
                    {:className (css
                                 :w-14 :h-14
                                 :bg-red-600
                                 :border-2
:rounded-lg)})))))))))
```

## Tooltip
Tooltips are built on top of popup logic. Two components are combined
in way that ```ui/tooltip``` will create popup area element, and render
rest of the children components.

Beside that it will also create popup element when user enters or leaves
popup area by tracking on mouse enter/leave events.

#### Example

<div id="tooltip-example"></div>


```clojure
(defn tooltip-example
  []
  (let [[state set-state!] (hooks/use-state "neutral")]
    (<>
     ($ ui/row
        ($ ui/dropdown-field
           {:name "Position"
            :value state
            :on-change set-state!
            :placeholder "Choose context..."
            :options ["neutral"
                      "positive"
                      "negative"
                      "warning"]}))
     ($ ui/row
        {:align :center}
        ($ ui/column
           {:align :center}
           ($ ui/tooltip
              {:message (case state
                          "positive" "I'm happy"
                          "negative" "Don't feel so good"
                          "warning"  (d/pre "I'm affraid that\nsomething might happen")
                          "Just business as usual")
               :className state}
              (d/div
               {:className (css
                            :font-bold
                            :text-base
                            :text-center
                            :cursor-default
                            :my-5)}
               "Hiii if you hover over me... Than")))))))
```
