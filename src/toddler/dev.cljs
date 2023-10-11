(ns toddler.dev
  (:require
    ; [cljs-bean.core :refer [->clj ->js]]
    [helix.core
     :refer [defnc $ provider]]
    [helix.hooks :as hooks]
    [helix.dom :as d]
    [toddler.dev.context
     :refer [*components*]]
    [toddler.router.dom :as router]
    [toddler.hooks
     :refer [use-user
             use-window-dimensions
             use-dimensions]]
    [toddler.i18n.default]
    [toddler.ui :as ui]
    [toddler.ui.elements :as e]
    [toddler.provider :refer [UI]]
    [toddler.ui.components :as default]
    [toddler.layout :as layout
     :refer [use-layout]]
    [toddler.window :as window]
    [toddler.popup :as popup]
    [toddler.dropdown :as dropdown]
    ["react" :as react]
    [toddler.app :as app]
    [toddler.i18n :as i18n]
    [shadow.css :refer [css]]
    [clojure.string :as str]))



(defonce component-db (atom nil))


(defnc component
  [{:keys [component]}]
  (let [[{:keys [rendered] :as query} set-query!] (router/use-search-params)
        selected? (= rendered (:key component))
        $component (css :mx-3 :my-2
                        :flex
                        :items-center
                        ["& .name" :toddler/menu-link]
                        ["& .icon" :w-5 :text-transparent :mr-1]
                        ["&.selected .icon" :toddler/menu-link-selected]
                        ["&.selected .name" :toddler/menu-link-selected])]
    (d/div
      {:class [$component
               (when selected? "selected")]}
      ; ($ icon/selectedRow {:className "icon"})
      (d/a
        {:className "name"
         :onClick (fn [] (set-query! (assoc query :rendered (:key component))))}
        (:name component)))))



(defnc navbar
  [_ _ref]
  {:wrap [(react/forwardRef)]}
  (let [components (hooks/use-context *components*)
        {:keys [height]} (use-window-dimensions)
        $navbar (css
                  :flex
                  :flex-col
                  :toddler/menu-link-selected
                  ["& .title"
                   :flex
                   :h-28
                   :items-center
                   :text-2xl
                   :justify-center
                   {:font-family "Audiowide"}])]
    ($ ui/simplebar
       {:ref _ref
        :className $navbar
        :style {:height height 
                :min-width 300
                :max-width 500}}
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



(let [popup-preference
      [#{:bottom :center}
       #{:bottom :right}]]
  (defnc LocaleDropdown
    []
    (let [[area-position set-area-position!] (hooks/use-state #{:bottom :center})
          ;;
          [{{locale :locale} :settings} set-user!] (use-user)
          ;;
          {:keys [area toggle! opened] :as dropdown}
          (dropdown/use-dropdown
            {:value locale
             :options [:en :hr :fr :fa]
             :search-fn name
             :area-position #{:bottom :center}
             :onChange (fn [v] (set-user! assoc-in [:settings :locale] v))})]
      ;;
      ($ popup/Area
         {:ref area
          :class (css :flex :items-center :font-bold)}
         (d/button
           {:onClick toggle!
            :class [(css
                      :toddler/menu-link
                      :items-center
                      ["&:hover" :toddler/menu-link-selected])
                    (when opened (css :toddler/menu-link-selected))]}
           (str/upper-case (name locale)))
         ($ dropdown/Popup
            {:className "dropdown-popup"
             :preference popup-preference
             :render/option e/dropdown-option
             :render/wrapper e/dropdown-wrapper})))))

(defnc header
  [_ _ref]
  {:wrap [(react/forwardRef)]}
  (let [
        window (use-window-dimensions)
        layout (use-layout)
        header-height 50
        header-width (- (:width window) (get-in layout [:navbar :width]))
        $header (css
                  :flex
                  :h-15
                  :flex-row-reverse
                  :pr-3
                  :box-border
                  {:color "#2c2c2c"})]
    (d/div
      {:className $header 
       :ref _ref 
       :style {:height header-height
               :width header-width}}
      ($ LocaleDropdown))))




(defnc empty-content
  []
  (let [window (use-window-dimensions)
        $empty (css :flex
                    :justify-center
                    :items-center)]
    ($ ui/row
       {:className $empty
        :position :center}
       ($ ui/row
          {:position :center
           :style #js {:height (:height window)}}
          "Select a component from the list"))))



(defnc content
  []
  {:wrap [(react/forwardRef)]}
  (let [components (hooks/use-context *components*)
        [{:keys [rendered]}] (router/use-search-params)
        render  (some
                  (fn [c]
                    (when (= (:key c) rendered)
                      (:render c)))
                  components)
        window (use-window-dimensions)
        layout (use-layout)
        content-height (- (:height window) (get-in layout [:header :height]) 10)
        content-width (- (:width window) (get-in layout [:navbar :width]) 10)
        content-dimensions (hooks/use-memo
                             [content-height content-width]
                             {:width content-width
                              :height content-height})
        $content (css
                   :bg-neutral-100
                   :border-teal-600
                   :rounded-md)]
    (if render
      (provider
        {:context layout/*container-dimensions*
         :value content-dimensions}
        (d/div
          {:style
           {:height content-height
            :width content-width}
           :class [$content "render-zone"]}
          ($ render)))
      ($ empty-content))))


(defnc playground
  []
  (let [[components set-components!] (hooks/use-state @component-db)
        [user set-user!] (hooks/use-state {:settings {:locale i18n/*locale*}})
        window (use-window-dimensions)
        [{_navbar :navbar
          _header :header
          _content :content} layout] (use-dimensions [:navbar :header :content])
        $playground (css
                      :flex
                      ["& .content" :flex :flex-col])]
    (hooks/use-effect
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
         ($ UI
            {:components default/components}
            ($ popup/Container
               ($ window/DimensionsProvider
                  (d/div
                    {:className $playground}
                    ($ navbar {:ref _navbar})
                    (let [header-height 50
                          header-width (- (:width window) (get-in layout [:navigation :width]))
                          content-height (- (:height window) (get-in layout [:header :height]))
                          content-width (- (:width window) (get-in layout [:navigation :width]))]
                      (d/div
                        {:className "content"}
                        ($ header
                           {:ref _header
                            :style {:width header-width 
                                    :height header-height}})
                        ($ content
                           {:ref _content
                            :style {:height content-height 
                                    :width content-width}})))))))))))



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

; (defnc centered-compnoent
;   [{:keys [className] :as props}]
;   (let [window (use-window-dimensions)
;         {:keys [navbar]} (use-layout)]
;     (d/div
;       {:className className
;        :style {:width (- (:width window) (:width navbar))
;                :minHeight (:height navbar)}}
;       (d/div
;         {:className "track"}
;         (c/children props)))))
