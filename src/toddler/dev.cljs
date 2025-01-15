(ns toddler.dev
  (:require
    ; [cljs-bean.core :refer [->clj ->js]]
   [clojure.core.async :as async]
   [helix.core
    :refer [defnc $ provider]]
   [helix.hooks :as hooks]
   [helix.dom :as d]
    ; [toddler.dev.context
    ;  :refer [*components*]]
   [toddler.router :as router]
   [toddler.core
    :as toddler
    :refer [use-user
            use-window-dimensions
            use-dimensions]]
   [toddler.i18n.default]
   [toddler.app :as app]
   [toddler.ui :as ui]
   [toddler.ui.elements :as e]
   [toddler.provider :refer [UI]]
   [toddler.ui.components :as default]
   [toddler.layout :as layout]
   [toddler.window :as window]
   [toddler.popup :as popup]
   [toddler.dropdown :as dropdown]
   ["react" :as react]
   [toddler.i18n :as i18n]
   [shadow.css :refer [css]]
   [clojure.string :as str]))

(defonce component-db (atom nil))

(defnc component
  [{:keys [component]}]
  (let [rendered? (router/use-rendered? (:id component))
        path (router/use-go-to (:id component))
        $component (css :mx-3 :my-2
                        :flex
                        :items-center
                        ["& .name" :toddler/menu-link]
                        ["& .icon" :w-5 :text-transparent :mr-1]
                        ["&.selected .icon" :toddler/menu-link-selected]
                        ["&.selected .name" :toddler/menu-link-selected])
        translate (toddler/use-translate)]
    (d/div
     {:class [$component
              (when rendered? "selected")]}
      ; ($ icon/selectedRow {:className "icon"})
     (d/a
      {:className "name"
       :onClick #(path)}
      (str (translate (:name component)))))))

(defnc navbar
  {:wrap [(react/forwardRef)]}
  [_ _ref]
  (let [links (:children (router/use-component-tree))
        {:keys [height]} (use-window-dimensions)
        $navbar (css
                 :flex
                 :flex-col
                 :toddler/menu-link-selected
                 ["& .title"
                  :flex
                  :h-20
                  :items-center
                  :text-2xl
                  :justify-center
                  {:font-family "Caveat Brush, serif"
                   :font-size "3rem"}])]
    ($ ui/simplebar
       {:ref _ref
        :className $navbar
        :style {:height height
                :min-width 300
                :max-width 500}}
       (d/div
        {:className "title"}
        "toddler")
       (d/div
        {:className "components-wrapper"}
        (d/div
         {:className "components-list"}
         (map
          (fn [c]
            ($ component
               {:key (:id c)
                :component c}))
          links))))))

(let [popup-preference
      [#{:bottom :center}
       #{:bottom :right}]]
  (defnc LocaleDropdown
    []
    (let [[{{locale :locale
             :or {locale :en}} :settings} set-user!] (use-user)
          ;;
          translate (toddler/use-translate)
          ;;
          {:keys [toggle! opened area] :as dropdown}
          (dropdown/use-dropdown
           {:value locale
            :options [:en :es :de :fr :hr]
            :search-fn #(i18n/translate :locale %)
            :area-position #{:bottom :center}
            :onChange (fn [v]
                        (set-user! assoc-in [:settings :locale] v))})]
      ;;
      (provider
       {:context dropdown/*dropdown*
        :value dropdown}
       ($ popup/Area
          {:ref area :class (css :flex :items-center :font-bold)}
          (d/button
           {:onClick toggle!
            :class [(css
                     :toddler/menu-link
                     :items-center
                     ["&:hover" :toddler/menu-link-selected])
                    (when opened (css :toddler/menu-link-selected))]}
           (translate :locale))
          ($ dropdown/Popup
             {:className "dropdown-popup"
              :preference popup-preference}
             ($ e/dropdown-wrapper
                ($ dropdown/Options
                   {:render e/dropdown-option}))))))))

(defnc header
  {:wrap [(react/forwardRef)]}
  [{:keys [style]} _ref]
  (d/div
   {:className (css
                :flex
                :h-15
                :flex-row-reverse
                :pr-3
                :box-border
                {:color "#2c2c2c"})
    :ref _ref
    :style style}
   ($ LocaleDropdown)))

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
  {:wrap [(react/forwardRef)]}
  [{:keys [style]}]
  (let [rendered-components (router/use-url->components)
        render (last (filter some? (map :render rendered-components)))
        $content (css
                  :background-normal
                  :rounded-md)]
    (if render
      (provider
       {:context layout/*container-dimensions*
        :value style}
       (d/div
        {:style style
         :class [$content "render-zone"]}
        ($ render)))
      ($ empty-content))))

(defnc playground-layout
  []
  (let [[components set-components!] (hooks/use-state @component-db)
        window (use-window-dimensions)
        [_navbar {navigation-width :width}] (use-dimensions)
        [_header] (use-dimensions)
        [_content {content-height :height}] (use-dimensions)
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
    ($ UI
       {:components default/components}
       ($ popup/Container
          ($ window/DimensionsProvider
             (d/div
              {:className $playground}
              ($ navbar {:ref _navbar})
              (let [header-height 50
                    header-width (- (:width window) navigation-width)
                    content-height (- (:height window) header-height)
                    content-width (- (:width window) navigation-width)]
                (d/div
                 {:className "content"}
                 ($ header
                    {:ref _header
                     :style {:width header-width
                             :height header-height}})
                 ($ content
                    {:ref _content
                     :style {:height content-height
                             :width content-width}})))))))))

(defnc playground
  [{:keys [components]}]
  (let [[{{locale :locale
           :or {locale :en}} :settings :as user} set-user!]
        (hooks/use-state {:settings {:locale i18n/*locale*}})
        [theme set-theme!] (hooks/use-state nil)]
    (hooks/use-effect
      :once
      (async/go
        (loop []
          (if-some [html (.querySelector js/document "html")]
            (when-not (= (.getAttribute html "data-theme") "light")
              (.setAttribute html "data-theme" "light"))
            #_(when-not (= (.getAttribute html "data-theme") "dark")
                (.setAttribute html "data-theme" "dark"))
            (do
              (async/<! (async/timeout 100))
              (recur))))))
    (router/use-link ::router/ROOT components)
    ($ window/DimensionsProvider
       (provider
        {:context app/user
         :value [user set-user!]}
        (provider
         {:context app/locale
          :value locale}
         ($ playground-layout))))))

(defn add-component
  [c]
  (swap! component-db
         (fn [current]
           (if (empty? current) [c]
               (let [idx (.indexOf (map :key current) (:id c))]
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
