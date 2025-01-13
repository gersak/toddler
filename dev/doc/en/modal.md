Hello,

This is place to showcase modal components. There are just
few modal building blocks. Modal background, dialog and strip.

Modal dialog and modal strip use modal background component
to display components that you will design and implement.


### MODAL BACKGROUND

As you can see in code bellow, modal-background component will
use react/createPortal to mount modal backround on popup/*container* 
node and pass component children to mounted div (one with :key ::background)

Optional properties for modal-background component include:

 * **can-close?** - controls if on-close can be called
 * **on-close** - when clicked on background will call on-close fn

<div id="modal-background-example"></div>

```clojure
(defnc modal-background
  [{:keys [class className can-close? on-close]
    :or {can-close? true
         on-close identity}
    :as props}]
  (let [container-node (hooks/use-context popup/*container*)]
    (rdom/createPortal
     (d/div
      {:key ::background
       :on-click (fn [] (when can-close? (on-close)))
       :class (cond-> [$modal-background
                       (when-not can-close? " block ")]
                (string? class) (conj class)
                (sequential? class) (into class)
                (string? className) (conj className))
       & (dissoc props :class :className :can-close? :on-click :onClick)}
      (c/children props))
     @container-node)))
```

### MODAL DIALOG

Dialog component will create dialog window and adjust its 
size to inside content. Same as modal-background it supports 
on-close handler, that will be called when user clicks 
outside of dialog window. If omitted you will have to 
handle modal closing by some other means. Like button or something similar.

There are some styled features that come with dialog 
component out of the box. This features are activated 
by adding class to content that is added to ui/modal-dialog component


 * **title** - Stating the purpose of modal-dialog. To emphisize current state
 * **content** - Adding content class will add padding to match footer and title
 * **footer** - Common place to add buttons, so this 
 class will add styles for displaying buttons in common fashion


<div id="modal-dialog-example"></div>


```clojure
($ ui/modal-dialog
 {:on-close close!
  :width 300
  :style {:max-width 400}
  :className (when context (name context))}
 (d/span
  {:className "title"}
  (translate :showcase.modal.dialog.title))
 (d/div
  {:class "content"}
  (d/pre
   {:className (css :mt-4 :word-break :whitespace-pre-wrap)}
   (translate :showcase.content.large)))
 (d/div
  {:className "footer"}
  ($ ui/button {:on-click close!} (translate :ok))
  ($ ui/button {:on-click close!} (translate :cancel))))
```