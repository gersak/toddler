(ns clj-kondo.helix.placenta
  (:require
    [clj-kondo.hooks-api :as api]))


(defn defstyled [{:keys [:node]}]
  (let [[cname ctype style & mixins] (rest (:children node))]
    (when-not (and cname ctype)
      (throw
        (ex-info "Define both new component name and extended component"
                 {:component cname
                  :extending ctype})))
    (let [new-node (if (not-empty mixins)
                     (api/list-node
                       (list
                         (api/token-node 'def)
                         cname
                         (api/list-node
                           (into
                             [(api/list-node [(api/token-node 'helix.styled-components/styled) ctype])
                              style]
                             mixins))))
                     (api/list-node
                       (list
                         (api/token-node 'def)
                         cname
                         (api/list-node
                           [(api/list-node [(api/token-node 'helix.styled-components/styled)])
                            ctype
                            style]))))]
      {:node new-node})))
