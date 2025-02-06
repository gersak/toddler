(ns toddler.showcase.routing
  (:require
   [cljs.pprint :refer [pprint]]
   [helix.core :refer [$ defnc <>]]
   [helix.dom :as d]
   [helix.hooks :as hooks]
   [shadow.css :refer [css]]
   [toddler.i18n.keyword :refer [add-translations]]
   [toddler.md.lazy :as md]
   [toddler.ui :as ui :refer [!]]
   [toddler.core :as toddler]
   [toddler.router :as router]
   [toddler.layout :as layout]))

(add-translations
 #:showcase.routing {:default "Routing"})

(defnc ModalTest
  {:wrap [(router/wrap-rendered ::modal)]}
  []
  (let [back (router/use-go-to ::router/ROOT)]
    (! :modal/dialog
       {:on-close #(back)
        :style {:max-width 300}}
       (d/div {:className "title"} "Testing modal")
       (d/div
        {:className "content"}
        (toddler/mlf
         "Hello from testing modal window. You have successfully"
         "changed URL!"))
       (d/div
        {:className "footer"}
        (! :button {:on-click #(back)} "CLOSE")))))

(defn p [text]
  (with-out-str
    (pprint text)))

(defnc Root
  {:wrap [(router/wrap-link
           ::router/ROOT
           [{:id ::basics
             :hash "basics"}
            {:id ::modal
             :name :routing.modal
             :segment "modal"}
            {:id ::protection
             :hash "route-protection"}
            {:id ::landing
             :hash "landing"}])]}
  []
  (let [location (router/use-location)
        [query set-query!] (router/use-query)
        open-modal (router/use-go-to ::modal)
        go-to-landing (router/use-go-to ::protection)
        reset (router/use-go-to ::router/ROOT)
        tree (router/use-component-tree)]
    (<>
     ($ md/show
        {:content
         (str
          "```clojure\n"
          (p
           {:location location
            :query query
            :tree tree})
          "\n```")})
     (! :row
        {:position :center}
        (! :button {:on-click #(open-modal)} "GO TO MODAL")
        (! :button {:on-click #(set-query!
                                {:test1 100
                                 :test2 "John"
                                 :test3 :test3
                                 :test4 ["100" "200" :goo 400]})}
           "CHANGE QUERY")
        (! :button {:on-click #(reset)} "RESET")
        (! :button {:on-click #(go-to-landing)} "GO TO FRAGMENT"))
     ($ ModalTest))))

(defnc public-route
  []
  (d/div
   {:className (css :p-4 :my-3 :rounded-xl :text-lg :font-semibold :bg-normal-)}
   "This data is publicly available!"))

(defnc admin-route
  []
  (let [authorized? (router/use-authorized? ::admin)]
    (when authorized?
      (d/div
       {:className (css :p-4 :my-3 :rounded-xl :text-lg :font-semibold :bg-warn)}
       "This data is only available to route administrator!"))))

(defnc super-route
  {:wrap [(router/wrap-authorized)]}
  []
  (d/div
   {:className (css :p-4 :my-3 :rounded-xl :text-lg :font-semibold :bg-positive)}
   "This data is only available to route SUPERUSER!!!"))

(defnc RouteProtection
  {:wrap [(router/wrap-link
           ::protection
           [{:id ::everyone}
            {:id ::admin
             :roles #{"admin"}}
            {:id ::superuser}])]}
  []
  (let [[{:keys [roles]} set-user!] (hooks/use-state nil)]
    ($ router/Protect
       {:roles (set roles)
        :super "superuser"}
       (<>
        ($ public-route)
        ($ admin-route)
        ($ super-route)
        (! :row
           (! :field/multiselect
              {:name "USER ROLES"
               :options ["admin" "superuser"]
               :value roles
               :on-change #(set-user! assoc :roles %)}))))))

(defnc App []
  (let []
    ($ router/Provider
       {:base "routing"}
       ($ Root))))

(defnc MyApp
  []
  ($ router/Provider
     ($ router/LandingPage
        {:url "/landing"}
        ($ Root))))

(defnc doc
  {:wrap [(router/wrap-rendered :toddler.routing)]}
  []
  (let [{:keys [height width]} (layout/use-container-dimensions)
        base (hooks/use-context router/-base-)]
    ($ router/Provider
       {:base (str (when base (str base "/")) "routing")}
       (! :simplebar
          {:style {:height height
                   :width width}
           :shadow true}
          (! :row {:align :center}
             (! :column {:align :center
                         :className (css :items-center ["& .toddler-markdown" {:max-width "40rem"}])}
                ($ md/watch-url {:url "/routing.md"})
                ($ toddler/portal
                   {:locator #(.getElementById js/document "router-basics")}
                   ($ Root))
                ($ toddler/portal
                   {:locator #(.getElementById js/document "route-protection-example")}
                   ($ RouteProtection))))))))

(defnc Routing
  {:wrap [(router/wrap-link
           :toddler.routing
           [{:id ::basics
             :name "Basics"
             :hash "basics"}
            {:id ::route-protection
             :name "Route Protection"
             :hash "route-protection"}
            {:id ::landing-page
             :name "LandingPage"
             :hash "landing-page"}])]}
  []
  ($ doc))
