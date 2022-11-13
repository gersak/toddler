(require '[cljs.build.api :as b])


(b/build
  {:main 'toddler.showcase
   :optimizations :none
   :npm-deps {:react "18.2.0"
              :react-dom "18.2.0"
              :react-is "18.2.0"
              :react-router-dom "6.3.0"
              :react-spring "9.5.2"
              :simplebar-react "2.4.1"
              :styled-components "5.3.5"}
   :install-deps true
   :output-to "public/js/main.js"})
