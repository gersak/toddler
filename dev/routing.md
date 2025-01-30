Welcome to routing showcase


Toddler will feal similar to JS react-router in
many cases. That is to older versions of react-router. Although it borrows some
of core features from react-router, there are significant differences on how
toddler approaches route composition.

Also ```toddler.router``` does not and will not go in direction of supporting
code splitting. For that use **shadow.loader** as demonstrated
in ```toddler.md.lazy``` namespace.


## BASICS

Toddler routing namespace has Helix component called ```Provider```
that accepts almost no arguments and provides few contexts that are
than used by other hooks in router namespace.

Provided contexts are:

 * **navigation** - this contexts holds map for handlers that can **push, pop, replace, go, forward, back** to location 
 * **router** - context that contains state of current :location and :tree that represents component tree
 * **dispatch** - dispatch function for router context


This is how you use provider and how to utilize provided contexts:

```clojure
(ns toddler.showcase.routing
  (:require
   [cljs.pprint :refer [pprint]]
   [helix.core :refer [$ defnc <>]]
   [helix.dom :as d]
   [toddler.md.lazy :as md]
   [toddler.ui :as ui]
   [toddler.core :as toddler]
   [toddler.router :as router]
   [toddler.layout :as layout]))


(defnc ModalTest
  {:wrap [(router/wrap-rendered ::modal)]}
  []
  (let [back (router/use-go-to ::router/ROOT)]
    ($ ui/modal-dialog
       {:on-close #(back)
        :style {:max-width 300}}
       (d/div {:className "title"} "Testing modal")
       (d/div {:className "content"}
              (toddler/mlf
               "Hello from testing modal window. You have successfully"
               "changed URL!"))
       (d/div {:className "footer"}
              ($ ui/button {:on-click #(back)}
                 "CLOSE")))))

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
  (let [{:keys [go back]} (router/use-navigate)
        location (router/use-location)
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
     ($ ui/row
        {:position :center}
        ($ ui/button {:on-click #(open-modal)} "GO TO MODAL")
        ($ ui/button {:on-click #(set-query!
                                  {:test1 100
                                   :test2 "John"
                                   :test3 :test3
                                   :test4 ["100" "200" :goo 400]})}
           "CHANGE QUERY")
        ($ ui/button {:on-click #(reset)} "RESET")
        ($ ui/button {:on-click #(go-to-landing)} "GO TO FRAGMENT"))
     ($ ModalTest))))

(defnc App []
  (let []
    ($ router/Provider
       {:base "routing"}
       ($ Root))))

```

This example is short demonstration of basic ```toddler.router``` hooks. First observe
that ```Provider``` was initialized with base set to "routing". This is our current location
if you haven't pressed any of buttons bellow.


Result of above code is this section bellow. It will render map created
by combing bindings from ```use-location, use-query, use-component-tree``` hooks
(check source above).

Four buttons are rendered as well. With those buttons you can change browser
URL using same hooks. Try it out, and track changes in result...

#### RESULT
<div id="router-basics"></div>



```use-link``` hook is used to link components with parent component. First you should
in your **root** (usually the one that is mounted) link children to **toddler.router/ROOT**
component. **toddler.router/ROOT** is parent of all components and when router is looking
for some component it will start from there.

Components should be added on the edge, thus linking won't affect parent components. This
approach is targeting low maintainance. When components
are rendered and ```use-link``` hook is present, this hook will call dispatch
on **-router-** context that will ::add-component if it is not allready there.

If component wasn't present in routing tree, then context is changed,
components are re-redered and registration continues.

You can control what components are rendered by using ```use-rendered?```
and ```use-authorized?``` hooks, or by wrapping components with
```wrap-rendered``` and ```wrap-authorized``` components.

By practicing this convention router can easily answer following questions:

 * **Is this component rendered?**
 * **Is user authorized to use this component?**
 * **Is this person superuser?**
 * **Is this component candidate for landing page?**

For more information on what options are available out of the box
when linking components read following docs from ```use-link``` hook.


```clojure
(defhook use-link
  "Hook will link parent with children components and add that to
  component tree for current -router- context. Children is expected
  to be map of:

  :id          Component ID. Should uniquely identify component

  :name        Name of component. Can be used to resolve what to display.
               If :name is of type string than use-component-name hook
               will return that name.
           
               When keyword is used as name value use-component-name will
               try to resolve that keyword as translation in respect to
               locale in current app/locale context.

  :hash        Optional hash that is appended to component URL

  :segment     Segment of path that is conjoined to all parent segments. Used
               to resolve if component is rendered or not and in use-go-to
               hook to resolve what is target path if I wan't to \"go\" to
               component with id
  
  :roles       #{} with roles that are allowed to access this component

  :permissions #{} with permissions that are allowed to access this component
  
  :landing     [number] to mark this component as possible landing site with number priority
  
  Linking should start with parent :toddler.router/ROOT component, as this
  component is parent to all other components"
  [parent children]
  (let [dispatch (hooks/use-context -dispatch-)]
    (when (nil? dispatch)
      (.error js/console "Router provider not initialized. Use Provider from this namespace and instantiate it in one of parent components!"))
    (hooks/use-layout-effect
      :once
      (dispatch
       {:type ::add-components
        :components children
        :parent parent}))))
```


## ROUTE PROTECTION
Modern applications mostly use access token to communicate with resource
providers. Usually this token, access token, can contain information about
user roles or/and permissions.

Even if it doesn't there must be some way to resolve user roles and 
permissions and using that information to control what user can access.

#### Example
```clojure
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
        ($ ui/row
           ($ ui/multiselect-field
              {:name "USER ROLES"
               :options ["admin" "superuser"]
               :value roles
               :on-change #(set-user! assoc :roles %)}))))))

```

<div id="route-protection-example"></div>


Above example displays how you can control what is displayed on UI.
```Protect``` is component that provides **-roles-, -permissions-, -super-**
context. From my expirience good place where to protect your application
would be as high as possible, where you fetch user data. At login I suppose.

Router will use values provided to that contexts to protect routes added
to component tree by comparing what is route requirement, that is what values
were provided to **:roles** and **:permissions** keys during component linking.

If there is intersection between user roles/permissions set and roles/permissions
definition defined in ```use-link```, than ```use-authorized``` hook will return
**true**. Otherwise it will return **false**.


Notice that there are wrapers so that you don't have to use ```use-authorized```
hook. Use ```wrap-authorized``` and ```wrap-rendered``` that will display target
component if it is rendered or when user is authorized.

Keep in mind that not all components require **segments**. It is possible to control
UI state with query values as well. This can come handy when working with modals. Like I
wan't to display modal for thing with id=100, and use ```use-query``` to check
if modal is displayed or not.

Components like actions, buttons, sensitive information should be protected as well.
This components don't require **:segment** and can also be protected by Toddler Router.

## LANDING PAGE
Landing page component will come in handy when working with OAuth authorization code flow.
When user logs in to authorization server that is compliant with OAuth authorization code flow,
then authorization server will return response to client on URL that client sent
during authorization flow request. 

In short client will redirect user to authorize on authorization server and when user
logs in on authorization server (not client), than authorization server needs to know
where to redirect user. Client sends something like

**https://example.app.com/oauth/callback**

and authorization server will return response with **authorization-code** to that URL.

On "that" URL there is SPA that has implementation that will handle rest of authorization
code flow to get **access-token**. But what happens when process is finished. What should
then be displayed? :eyes:

Usually OAuth client side implementation supports some kind of callback that will be
triggered when process is finished. Still question remains...

```clojure
(defnc MyApp
  []
  ($ router/Provider
     ($ router/LandingPage
        {:url "/landing"}
        ($ Root))))
```

Answer is ```LandingPage```. You should add "on sign in finished" callback handler that
will redirect browser to **/landing** URL. LandingPage will listen location changes
(when URL changes) and if **/landing** URL is current browser location, than LandingPage
component will walk through component tree and isolate all components that are marked
with positive ```:landing *priority*``` and check if user is authorized to access that component.

When all accessible components are filtered, than those components are sorted by landing
priority and component with highest priority is selected.

URL for that component is resolved by Toddler Router and once again user is redirected to
URL (component) with highest priority.
