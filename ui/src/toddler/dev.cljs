(ns toddler.dev
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
    :refer [use-user
            use-window-dimensions
            use-dimensions]]
   [toddler.i18n.default]
   [toddler.app :as app]
   [toddler.ui :as ui :refer [!]]
   [toddler.ui.elements :as e]
   [toddler.layout :as layout]
   [toddler.window :as window]
   ; [toddler.tauri :as window]
   [toddler.popup :as popup]
   [toddler.dropdown :as dropdown]
   [toddler.material.outlined :as outlined]
   [toddler.fav6.brands :as brands]
   ["react" :as react]
   [toddler.i18n :as i18n]
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
        $component (css :px-3 :py-1
                        ["& a" :no-underline :select-none]
                        ["& .icon" :w-5 :text-transparent :mr-1]
                        ["& .level-1" :pl-2]
                        ["& .level-2" :pl-4]
                        ["& .level-3" :pl-6]
                        ; ["&.selected .icon" :color-hover]
                        )
        translate (toddler/use-translate)
        [_subs {sub-height :height}] (toddler/use-dimensions)]
    (d/div
     {:class [$component]}
     (d/a
      {:class ["name" (when rendered? "selected")]
       :onClick #(path)}
      (str (translate (:name component))))
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
  [_ _ref]
  (let [{links :children} (router/use-component-tree)
        {:keys [height] :as window} (use-window-dimensions)
        {:keys [width]} (toddler/use-window-dimensions)
        mobile? (< width 800)
        [opened? set-opened!] (hooks/use-state false)]
    (if mobile?
      ($ ui/row
         {:align :center
          :on-click #(set-opened! not)
          :className (css
                      :absolute
                      :top-0 :left-0 :pl-4
                      {:font-size "28px" :height "50px"}
                      ["& .title" :ml-4 {:font-family "Caveat Brush, serif"}]
                      ["& .opened"
                       {:transition "transform .3s ease-in-out"
                        :transform "rotate(90deg)"}])}
         ($ outlined/menu {:className (when opened? "opened")})
         (d/div
          {:className "title"}
          "toddler")
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
                      ["& .title"
                       :flex
                       :h-20
                       :items-center
                       :text-2xl
                       :mb-4
                       :justify-center
                       :select-none
                       {:font-family "Caveat Brush, serif"
                        :font-size "3rem"}]
                      ["& .component-list"
                       :ml-3
                       :mb-20]
                      ["& .name" :text-xs :font-semibold {:color "var(--color-inactive)"}]
                      ["& .name:hover" :text-xs {:color "var(--color-active)"}]
                      ["& .selected.name" {:color "var(--color-normal)"}]
                      ["& .name.selected" {:color "var(--color-normal)"}])}
         (d/div
          {:className "title"}
          "toddler")

         ($ ui/simplebar
            {:style {:height (- height 120)
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
  [{:keys [style theme on-theme-change]} _ref]
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
   (d/div
    {:className "wrapper"}
    (! :tooltip {:message "Change theme"}
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
    (! :tooltip {:message "Open Github project"}
       (d/a
        {:href "https://github.com/gersak/toddler"}
        ($ brands/github))))
   (d/div
    {:className "wrapper"}
    (! :tooltip {:message "API Docs"}
       (d/a
        {:href "https://gersak.github.io/toddler/codox/index.html"
         :className (css :text-base :font-bold :no-underline :select-none)}
        "API")))))

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

(defnc playground-layout
  [{:keys [components max-width]}]
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
     (! :row {:key ::center
              :& (cond->
                  {:align :center
                   :style {:flex-grow "1"}})}
        (! :row
           {:key ::wrapper
            :style {:max-width (+ content-width navigation-width)}}
           ($ navbar {:ref _navbar})
           (! :column {:className "content"}
              ($ header
                 {:ref _header
                  :theme theme
                  :on-theme-change set-theme!
                  :style {:width header-width
                          :height header-height}})
              ($ content
                 {:ref _content
                  :style {:height content-height
                          :width content-width}}
                 (map
                  (fn [{:keys [id render]}]
                    (when render
                      ($ render {:key id})))
                  components))))))))

(defnc playground
  {:wrap [(window/wrap-window-provider)]}
  [{:keys [components max-width]}]
  (let [[{{locale :locale
           :or {locale :en}} :settings :as user} set-user!]
        (hooks/use-state {:settings {:locale i18n/*locale*}})]
    (router/use-link ::router/ROOT components)
    (provider
     {:context app/user
      :value [user set-user!]}
     (provider
      {:context app/locale
       :value locale}
      ($ playground-layout
         {:max-width max-width
          :components components})))))
