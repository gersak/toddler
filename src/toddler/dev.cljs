(ns toddler.dev
  (:require
    [helix.core
     :refer [defnc $ create-context <> provider]]
    [helix.hooks :as hooks]
    [helix.dom :as d]
    [helix.styled-components
     :refer [global-style defstyled --themed]]
    [toddler.theme :as theme]
    [toddler.dev.theme]
    [toddler.hooks 
     :refer [use-window-dimensions
             use-dimensions]]
    [toddler.interactions :as interactions]
    [toddler.elements.window :as window]
    [toddler.elements.popup :as popup]
    ["react" :as react]
    ["@fortawesome/free-solid-svg-icons"
     :refer [faChevronRight]]))


(defonce component-db (atom nil))

(def ^:dynamic *components* (create-context))
(def ^:dynamic *render* (create-context))
(def ^:dynamic *set-rendered* (create-context))
(def ^:dynamic *set-componets* (create-context))
(def ^:dynamic *navbar* (create-context))


(defnc Component
  [{:keys [className selected? component]}]
  (let [render! (hooks/use-context *set-rendered*)]
    (d/div
      {:className (cond-> className
                    selected? (str " selected"))}
      ($ interactions/fa
         {:className "icon"
          :icon faChevronRight})
      (d/a
        {:className "name"
         :onClick (fn [] (render! component))}
        (:name component)))))


(defstyled component Component
  {:margin "7px 3px"
   :display "flex"
   :align-items "center"
   ".icon" {:width 8
            :color "transparent"
            :margin-right 4}})


(defnc Navbar
  [{:keys [className]} _ref]
  {:wrap [(react/forwardRef)]}
  (let [size (use-window-dimensions)
        rendered (hooks/use-context *render*)
        components (hooks/use-context *components*)]
    ($ interactions/simplebar
       {:className className
        :style #js {:height (:height size)
                    }
        :scrollableNodeProps #js {:ref _ref}
        :ref #(reset! _ref %)}
       (d/div
         {:className "title"}
         "TODDLER")
       (d/div
         {:className "components-wrapper"}
         (d/div
           {:className "components-list"}
           (map
             (fn [c]
               ($ component
                 {:key (:key c)
                  :component c
                  :selected? (= rendered c)}))
             components))))))


(defstyled navbar Navbar
  {:min-width 200 :max-width 400
   :display "flex"
   :flex-direction "column"
   ".title" {:font-family "Audiowide"
             :justify-content "center"
             :align-items "center"
             :display "flex"
             :height 50}
   ".components-wrapper"
   {:display "flex"
    ; :justify-content "center"
    :padding "20px 10px 10px 10px"}}
  --themed)


(defnc Header
  [{:keys [className]}]
  (d/div
    {:className className}))


(defstyled header Header
  {:display "flex"})


; (defnc EmptyContent
;   [{:keys [className]}]
;   (let [window (use-window-dimensions)]
;     (d/div
;       {:className className
;        :style {:height (:height window)}}
;       "Select some component")))

(defnc EmptyContent
  [{:keys [className]}]
  (let [window (use-window-dimensions)]
    ($ interactions/row
       {:className className
        :position :center}
       ($ interactions/column
          {:position :center}
          "Select some component"))))

(defstyled empty-content EmptyContent
  {:display "flex"
   :justify-content "center"
   :align-items "center"})


(defnc Content
  [{:keys [className]}]
  (let [{:keys [render]} (hooks/use-context *render*)
        window (use-window-dimensions)
        {nav-width :width} (hooks/use-context *navbar*)]
    ($ popup/Container
       ($ interactions/simplebar
          {:style #js {:height (:height window)
                       :width (- (:width window) nav-width)}}
          (d/div
            {:className "wrapper"}
            (d/div
              {:className className}
              (if render
                ($ render)
                ($ empty-content))))))))


(defstyled content Content
  {".wrapper"
   {:padding 20
    :flex-grow "1"
    :height "100%"}})


(def global-css
  (global-style
    #js [theme/global]))


(def simplebar-css
  (global-style
    #js [theme/simplebar]))


(defnc Playground
  [{:keys [className]}]
  (let [[components set-compoments!] (hooks/use-state @component-db)
        [rendered set-rendered!] (hooks/use-state nil)
        [navbar-ref navbar-dimensions] (use-dimensions)]
    (hooks/use-layout-effect
      :once
      (.log js/console "Adding playground watcher!")
      (add-watch
        component-db
        ::playground
        (fn [_ _ _ components]
          (set-compoments! components)))
      (fn []
        (remove-watch component-db ::playground)))
    ($ window/DimensionsProvider
       (provider
         {:context *navbar*
          :value navbar-dimensions}
         (provider
         {:context *components*
          :value components}
         (provider
           {:context *render*
            :value rendered}
           (provider
             {:context *set-rendered*
              :value set-rendered!}
             (<>
               ($ global-css)
               ($ simplebar-css)
               (d/div
                 {:className className}
                 ($ navbar {:ref navbar-ref})
                 (d/div
                   {:className "content"}
                   ($ header)
                   ($ content)))))))))))


(defstyled playground Playground
  {:display "flex"
   ".content"
   {:display "flex"
    :flex-direction "column"}})


(defn add-component
  [c]
  (swap! component-db
         (fn [current]
           (if (empty? current) [c]
             (let [idx (.indexOf (map :key current) (:key c))]
               (if (neg? idx)
                 (conj current c)
                 (assoc current idx c)))))))
