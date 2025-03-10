(ns toddler.docs
  (:require
   [clojure.core.async :as async]
   [helix.core
    :refer [defnc $ provider create-context <>]]
   [helix.hooks :as hooks]
   [helix.dom :as d]
   [helix.children :refer [children]]
   [toddler.router :as router]
   [toddler.core
    :as toddler
    :refer [use-window-dimensions
            use-dimensions]]
   [toddler.i18n.default]
   [toddler.app :as app]
   [toddler.ui :as ui :refer [!]]
   [toddler.layout :as layout]
   [toddler.material.outlined :as outlined]
   [toddler.fav6.brands :as brands]
   ["react" :as react]
   [shadow.css :refer [css]]))

(def -level- (create-context))

(defnc subcomponent
  [{:keys [id name children hash rendered?]}]
  (let [level (hooks/use-context -level-)
        on-click (router/use-go-to id)
        [visible? set-visible!] (hooks/use-state false)]
    (hooks/use-effect
      [rendered?]
      (let [close-channel (app/listen-to-signal
                           :toddler.md/intersection
                           (fn [{ids :ids}]
                             (set-visible!
                              (fn [current]
                                (let [targeting? (contains? ids hash)]
                                  (cond
                                    (and (not current) targeting?)
                                    (do
                                      true)
                                    (and current (not targeting?))
                                    (do
                                      false)
                                    :else current))))))]
        (fn []
          (set-visible! false)
          (async/close! close-channel))))
    (when name
      (<>
       (d/div
        {:class ["subcomponent" (str "level-" level) (when rendered? "fade-in")]}
        (d/a
         {:class ["name"
                  (when (and rendered? visible?) "selected")]
          :on-click #(on-click)}
         name))
       (map
        (fn [{:keys [id] :as props}]
          ($ subcomponent {& props}))
        children)))))

(defnc component
  [{:keys [component]}]
  (let [rendered? (router/use-rendered? (:id component))
        level (hooks/use-context -level-)
        path (router/use-go-to (:id component))
        $component (css :px-3 :pt-1
                        ["& a" :no-underline :select-none]
                        ["& .icon" :w-5 :text-transparent :mr-1]
                        ["& .level-1" :pl-2]
                        ["& .level-2" :pl-4]
                        ["& .level-3" :pl-6])
        [_subs {sub-height :height}] (toddler/use-dimensions)]
    (d/div
     {:class [$component]}
     (d/a
      {:class ["name" (when rendered? "selected")]
       :onClick #(path)}
      (str (:name component)))
     (provider
      {:context -level-
       :value (inc level)}
      (d/div
       {:style {:overflow "hidden"
                :transition "height .3s ease-in-out"
                :height (if rendered? sub-height 0)}}
       (d/div
        {:ref #(reset! _subs %)}
        (map
         (fn [{:keys [id] :as props}]
           ($ subcomponent {:key id :rendered? rendered? & props}))
         (:children component))))))))

(defnc navbar
  {:wrap [(react/forwardRef)]}
  [{:keys [render/logo]} _ref]
  (let [{links :children} (router/use-component-tree)
        {:keys [height] :as window} (use-window-dimensions)
        {:keys [width]} (toddler/use-window-dimensions)
        mobile? (< width 1000)
        [opened? set-opened!] (hooks/use-state false)
        [_logo {logo-height :height}] (toddler/use-dimensions)]
    (if mobile?
      ($ ui/row
         {:align :center
          :on-click #(set-opened! not)
          :className (css
                      :absolute
                      :top-0 :left-0 :pl-4
                      {:font-size "20px" :height "50px"}
                      ["& .logo" :ml-4 {:max-height "24px"}]
                      ["& .menu" {:font-size "28px"}]
                      ["& .opened"
                       {:transition "transform .3s ease-in-out"
                        :transform "rotate(90deg)"}])}
         ($ outlined/menu {:class ["menu" (when opened? "opened")]})
         (when logo ($ logo {:mobile mobile?}))
         (d/div
          {:class ["drawer" (css
                             :pl-2
                             ["& .name" :text-xl :font-semibold {:color "var(--color-inactive)"}]
                             ["& .name:hover" :text-xl {:color "var(--color-active)"}]
                             ["& .selected.name" {:color "var(--color-normal)"}]
                             ["& .name.selected" {:color "var(--color-normal)"}])]
           :style {:width (if-not opened? 0 width)
                   :transition "width .3s ease-in-out"
                   :overflow "hidden"
                   :background "var(--background)"
                   :z-index "100"
                   :position "absolute"
                   :left 0 :top 50}}
          ($ ui/simplebar
             {:style {:height (- height 50)
                      :width width}
              :shadow true}
             (d/div
              {:className "components-wrapper"}
              (d/div
               {:className "component-list"}
               (provider
                {:context -level-
                 :value 0}
                (map
                 (fn [c]
                   ($ component
                      {:key (:id c)
                       :component c}))
                 links)))))))
      ($ ui/column
         {:ref _ref
          :style {:height height}
          :className (css
                      :flex
                      :flex-col
                      :toddler/menu-link-selected
                      ["& .component-list"
                       :ml-3
                       :mb-20]
                      ["& .logo-wrapper" :flex]
                      ["& .name" :text-xs :font-semibold {:color "var(--color-inactive)"}]
                      ["& .name:hover" :text-xs {:color "var(--color-active)"}]
                      ["& .selected.name" {:color "var(--color-normal)"}]
                      ["& .name.selected" {:color "var(--color-normal)"}])}
         (d/div
          {:ref _logo
           :className "logo-wrapper"}
          (when logo ($ logo {:mobile? mobile?})))

         ($ ui/simplebar
            {:style {:height (- height logo-height)
                     :min-width 200
                     :max-width 400}
             :shadow false}
            (d/div
             {:className "components-wrapper"}
             (d/div
              {:className "component-list"}
              (provider
               {:context -level-
                :value 0}
               (map
                (fn [c]
                  ($ component
                     {:key (:id c)
                      :component c}))
                links)))))))))

(defnc header
  {:wrap [(react/forwardRef)]}
  [{:keys [style] :as props} _ref]
  (d/div
   {:className (css
                :flex
                :h-15
                :flex-row-reverse
                :pr-3
                :box-border
                ["& .wrapper"
                 :items-center :flex :items-center {:font-size "24px"} :mr-4
                 :cursor-pointer :color-inactive]
                ["& .wrapper:first-child" :mr-0]
                ["& .tooltip-popup-area:hover" :color-normal])
    :ref _ref
    :style style}
   (children props)))

(defnc empty-content
  []
  (let [window (use-window-dimensions)
        $empty (css :flex
                    :justify-center
                    :items-center)]
    (! :row
       {:className $empty
        :position :center}
       (! :row
          {:position :center
           :style #js {:height (:height window)}}
          "Select a component from the list"))))

(defnc content
  {:wrap [(react/forwardRef)]}
  [{:keys [style] :as props}]
  (let [$content (css
                  :background-normal
                  :rounded-md)]
    (provider
     {:context layout/*container-dimensions*
      :value style}
     (d/div
      {:style style
       :class [$content "render-zone"]}
      (children props)))))

(defnc toddler-logo
  []
  (let [theme (hooks/use-context app/theme)
        logo (str "https://raw.githubusercontent.com/gersak/toddler/refs/heads/main/ui/assets/toddler_" theme ".png")]
    (d/div
     {:className (css :flex :items-center :justify-center :grow {:min-height "150px"})}
     (d/img
      {:src logo
       :className (css {:max-height "40px"})}))))

(defnc toddler-actions
  [{:keys [theme on-theme-change]}]
  (<>
   (d/div
    {:className "wrapper"}
    ($ ui/tooltip {:message "Change theme"}
       (d/div
        {:on-click (fn []
                     (on-theme-change
                      (case theme
                        ("dark" 'dark) "light"
                        ("light" 'light) "dark"
                        "light")))}
        (if (= "dark" theme)
          ($ outlined/light-mode)
          ($ outlined/dark-mode)))))
   (d/div
    {:className "wrapper"}
    ($ ui/tooltip {:message "Open Github project"}
       (d/a
        {:href "https://github.com/gersak/toddler"}
        ($ brands/github))))
   (d/div
    {:className "wrapper"}
    ($ ui/tooltip {:message "API Docs"}
       (d/a
        {:href "https://gersak.github.io/toddler/codox/index.html"
         :className (css :text-base :font-bold :no-underline :select-none)}
        "API")))))

(defnc page
  [{:keys [components max-width render/logo render/actions]
    :or {logo toddler-logo
         actions toddler-actions}}]
  (let [window (use-window-dimensions)
        [_navbar {navigation-width :width}] (use-dimensions)
        [_header] (use-dimensions)
        [_content {content-height :height}] (use-dimensions)
        header-height 50
        right-width (min
                     (- (or max-width (:width window)) navigation-width)
                     (- (:width window) navigation-width))
        header-width right-width
        content-height (- (:height window) header-height)
        content-width right-width
        [theme set-theme!] (toddler/use-local-storage ::theme str)]
    (hooks/use-effect
      [theme]
      (if (empty? theme)
        (set-theme! "light")
        (async/go
          (loop []
            (if-some [html (.querySelector js/document "html")]
              (when-not (= (.getAttribute html "data-theme") theme)
                (.setAttribute html "data-theme" theme))
              (do
                (async/<! (async/timeout 100))
                (recur)))))))
    (provider
     {:context app/theme
      :value theme}
     ($ ui/row {:key ::center
                :& (cond->
                    {:align :center
                     :style {:flex-grow "1"}})}
        ($ ui/row
           {:key ::wrapper
            :style {:max-width (+ content-width navigation-width)}}
           ($ navbar {:ref _navbar :render/logo logo})
           ($ ui/column {:className "content"}
              ($ header
                 {:ref _header
                  :style {:width header-width
                          :height header-height}}
                 (when actions
                   ($ actions
                      {:theme theme
                       :on-theme-change set-theme!})))
              ($ content
                 {:ref _content
                  :style {:height content-height
                          :width content-width}}
                 (map
                  (fn [{:keys [id render]}]
                    (when render
                      ($ render {:key id})))
                  components))))))))
