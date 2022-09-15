(ns toddler.dev
  (:require
    [helix.core
     :refer [defnc $ <> provider]]
    [helix.hooks :as hooks]
    [helix.dom :as d]
    [helix.children :as c]
    [helix.styled-components
     :refer [global-style defstyled --themed]]
    [toddler.theme :as theme]
    [toddler.dev.theme]
    [toddler.dev.context
     :refer [*components*
             *navbar*
             *header*]]
    [toddler.router.dom :as router]
    [toddler.hooks
     :refer [use-current-user
             use-current-locale
             use-window-dimensions
             use-layout-dimensions
             use-dimensions]]
    [toddler.i18n.default]
    [toddler.elements :as toddler]
    [toddler.elements.window :as window]
    [toddler.elements.popup :as popup]
    ["react" :as react]
    ["react-icons/fa" :refer [FaChevronRight] :as fa]
    [toddler.app :as app]
    [toddler.i18n :as i18n]
    [clojure.string :as str]))



(defonce component-db (atom nil))



(defnc Component
  [{:keys [className component]}]
  (let [[{:keys [rendered] :as query} set-query!] (router/use-search-params)
        selected? (= rendered (:key component))]
    (d/div
      {:className (cond-> className
                    selected? (str " selected"))}
      ($ FaChevronRight {:className "icon"})
      (d/a
        {:className "name"
         :onClick (fn [] (set-query! (assoc query :rendered (:key component))))}
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
  (let [components (hooks/use-context *components*)
        {:keys [height]} (toddler/use-container-dimensions)]
    ($ toddler/simplebar
       {:className className
        :style #js {:height height
                    :maxWidth 500
                    :minWidth 300}
        :scrollableNodeProps #js {:ref _ref}
        :ref _ref}
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
                  :component c}))
             components))))))


(defstyled navbar Navbar
  {:min-width 350 :max-width 400
   :display "flex"
   :flex-direction "column"
   ".title" {:font-family "Audiowide"
             :justify-content "center"
             :font-size "1.5em"
             :align-items "center"
             :display "flex"
             :height 50}
   ".components-wrapper"
   {:display "flex"
    ; :justify-content "center"
    :padding "20px 10px 10px 10px"}}
  --themed)


(defnc LocaleDropdown
  []
  (let [{:keys [toggle! opened]} (hooks/use-context toddler/*dropdown*)
        locale (use-current-locale)
        pressed-button (when opened
                         {:color "#d3d3d3"
                          :background-color "#003366"
                          :border "2px solid #003366"})]
    (d/button
      {:className "circular-button"
       :onClick toggle!
       :style pressed-button}
      (str/upper-case (name locale)))))

(defnc Header
  [{:keys [className]} _ref]
  {:wrap [(react/forwardRef)]}
  (let [[{{locale :locale} :settings} set-user!] (use-current-user)]
    (d/div
      {:className className
       :ref _ref}
      ($ toddler/DropdownArea
         {:value locale
          :options [:hr :en :fa]
          :search-fn name
          :onChange (fn [v] (set-user! assoc-in [:settings :locale] v))}
         ($ LocaleDropdown)
         ($ toddler/DropdownPopup)))))


(defstyled header Header
  {:display "flex"
   :height "50px"
   :background "#d3e9eb"
   :flex-direction "row-reverse"
   :padding-right "15px"}
  --themed)


(defnc EmptyContent
  [{:keys [className]}]
  (let [window (use-window-dimensions)]
    ($ toddler/row
       {:className className
        :position :center}
       ($ toddler/row
          {:position :center
           :style #js {:height (:height window)}}
          "Select a component from the list"))))

(defstyled empty-content EmptyContent
  {:display "flex"
   :justify-content "center"
   :align-items "center"})


(defnc Content
  [{:keys [className]}]
  {:wrap [(react/forwardRef)]}
  (let [components (hooks/use-context *components*)
        [{:keys [rendered]}] (router/use-search-params)
        render  (some
                  (fn [c]
                    (when (= (:key c) rendered)
                      (:render c)))
                  components)
        {:keys [width height] :as window} (toddler/use-container-dimensions)
        {{nav-width :width} :navbar
         {head-height :height} :header :as layout} 
          (toddler/use-layout)]
    (if render
      ($ toddler/Container
         {:style {:height (- height head-height)
                  :width (- width nav-width)}
          :className (str className " render-zone")}
         ($ render))
      ($ empty-content))))


(defstyled content Content
  nil)


(def global-css
  (global-style
    #js [theme/global]))


(def simplebar-css
  (global-style
    #js [theme/simplebar]))


(defnc Playground
  [{:keys [className]}]
  {:wrap [(toddler/wrap-window)]}
  (let [[components set-components!] (hooks/use-state @component-db)
        [user set-user!] (hooks/use-state {:settings {:locale i18n/*locale*}})
        [{_navbar :navbar
          _header :header
          _content :content} layout] (use-layout-dimensions [:navbar :header :content])]
    (hooks/use-layout-effect
      :once
      (.log js/console "Adding playground watcher!")
      (add-watch
        component-db
        ::playground
        (fn [_ _ _ components]
          (set-components! components)))
      (fn []
        (remove-watch component-db ::playground)))
    ($ router/BrowserRouter
       (provider
         {:context *components*
          :value components}
         (provider
           {:context app/*user*
            :value [user set-user!]}
           (provider
             {:context toddler/*layout*
              :value layout}
             ($ global-css)
             ($ simplebar-css)
             (d/div
               {:className className}
               ($ navbar {:ref _navbar})
               (d/div
                 {:className "content"}
                 ($ header {:ref _header})
                 ($ content {:ref _content})))))))))


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



;; Component wrappers

(defnc CenteredComponent
  [{:keys [className] :as props}]
  (let [window (use-window-dimensions)
        navbar (hooks/use-context *navbar*)]
    (d/div
      {:className className
       :style {:width (- (:width window) (:width navbar))
               :minHeight (:height navbar)}}
      (d/div
        {:className "track"}
        (c/children props)))))


(defstyled centered-component CenteredComponent
  {:display "flex"
   :justify-content "center"
   :align-items "center"
   :flex-grow "1"
   ".track" {:display "flex"
             :flex-direction "column"
             :justify-content "center"}})