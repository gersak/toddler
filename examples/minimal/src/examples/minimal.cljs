(ns examples.minimal
  {:shadow.css/include
   ["examples/minimal.css"]}
  (:require
   ["react-dom/client" :refer [createRoot]]
   [shadow.css :refer [css]]
   [helix.core :refer [$ defnc provider]]
   [helix.dom :as d]
   [helix.hooks :as hooks]
   [toddler.app :as app]
   [toddler.ui :as ui]
   [toddler.i18n.time]
   [toddler.core :as toddler]
   [toddler.layout :as layout]
   [toddler.ui.components :refer [components]]
   [toddler.fav6.solid :as solid]
   [toddler.window :refer [wrap-window-provider]]
   [toddler.router :as router]))

(defonce root (atom nil))

; (toddler.i18n.time/add-symbols [:hr :de])
; (toddler.i18n.time/add-symbols [:hr :de])

(def animal-emoji
  {:dog "🐶"
   :cat "🐱"
   :cow "🐮"
   :horse "🐴"
   :sheep "🐑"})

(def animal-description
  {:dog "Loyal and affectionate, dogs are often called “man’s best friend.” They come in various breeds and sizes, serving as pets, guards, and working animals."
   :cat "Independent and playful, cats are known for their agility and hunting skills. They make great companions and are famous for their love of napping."
   :cow "A vital livestock animal, cows provide milk, meat, and leather. They are known for their gentle nature and are essential in agriculture."
   :horse "Horses have been used for transportation, farming, and sports for centuries. They are strong, intelligent, and have a deep bond with humans."
   :sheep "Domesticated for their wool, milk, and meat, sheep are docile animals that thrive in herds. Their wool is widely used in the textile industry."})

(defn Translate
  []
  (let [translate (toddler/use-translate)]
    (d/div (translate (js/Date.)))))

(defnc Greeting
  []
  ($ ui/row
     {:align :center
      :style {:width "100%" :height "100%"}}
     ($ ui/column
        {:align :center
         :style {:align-items "center"}}
        ($ ui/row
           ($ Translate)
           (d/div {:style {:font-size "100px"}} "🖖")
           (d/div "Hello from minimal example")))))

(def animals [:dog :cat :cow :horse :sheep])

(defnc AnimalCard
  []
  (let [{animal :pathname} (router/use-location)
        {:keys [go]} (router/use-navigate)
        animal (when animal (keyword (subs animal 1)))
        position (.indexOf animals animal)
        [previous-animal next-animal]
        (hooks/use-memo
          [position]
          [(if (pos? position)
             #(go (str "/" (name (get animals (dec position)))))
             identity)
           (if (< position (dec (count animals)))
             #(go (str "/" (name (get animals (inc position)))))
             identity)])]
    ($ ui/column
       {:className (css :border :rounded-lg :border-normal+ :p-4 :bg-normal+)}
       ($ ui/row
          {:className (css {:height "180px"})}
          ($ ui/row
             {:style {:font-size "100px"
                      :width "120px"}
              :align :center}
             (get animal-emoji animal))
          (d/div
           {:className (css :flex :flex-col
                            :p-4 :m-2
                            ["& h1" :uppercase])}
           (d/h1 (name animal))
           (d/pre
            {:className (css :whitespace-pre-wrap :word-break
                             {:width "200px"})}
            (get animal-description animal))))
       ($ ui/row
          {:align :explode
           :className (css :mt-10)}
          ($ ui/button
             {:disabled (= identity previous-animal)
              :on-click previous-animal}
             ($ solid/chevron-left))
          ($ ui/button
             {:disabled (= identity next-animal)
              :on-click next-animal}
             ($ solid/chevron-right))))))

(defnc Animals
  {:wrap [(router/wrap-link
           ::router/ROOT
           [{:id :dog
             :name "Dog"
             :segment "dog"
             :landing 10}
            {:id :cat
             :name "Cat"
             :segment "cat"}
            {:id :cow
             :name "Cow"
             :segment "cow"
             :landing 30}
            {:id :horse
             :name "Horse"
             :segment "horse"}
            {:id :sheep
             :name "Sheep"
             :segment "sheep"}])]}
  []
  ($ ui/row
     {:align :center
      :style {:width "100%" :height "100%"}}
     ($ ui/column
        {:align :center
         :style {:align-items "center"}}
        ($ ui/row
           ($ AnimalCard)))))

(defnc Minimal
  {:wrap [(router/wrap-landing "/" false)
          (router/wrap-router)
          (wrap-window-provider)
          (ui/wrap-ui components)]}
  []
  (let [[locale set-locale!] (hooks/use-state :hr)
        [theme set-theme!] (toddler/use-theme-state)]
    (provider
     {:context app/locale
      :value locale}
     (provider
      {:context app/theme
       :value theme}
      ($ Animals)))))

(defn ^:dev/after-load start! []
  (let [target ^js (.getElementById js/document "app")]
    (when-not @root
      (reset! root ^js (createRoot target)))
    (.render ^js @root ($ Minimal))))
