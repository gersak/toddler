(ns toddler.css)

(def components
  {:transition-normal {:transition "color .3s ease-in-out, border-color .3s ease-in-out, background-color .3s ease-in-out"}
   :border-normal- {:border-color "var(--border-light)"} ;; not used
   :border-normal+ {:border-color "var(--border-heavy)"}
   :border-normal {:border-color "var(--border-normal)"}

   ;; Deprecate border-normal prefix and replace with border-
   :border-highlighted {:border-color "var(--border-highlighted)"}
   :border-focused {:animation "input-normal-click .3s ease-in-out"}
   :border-hover {:border-color "var(--border-hover)"}
   :border-click {:border-color "var(--border-click)"}

   :bg-normal {:background-color "var(--background-normal)"}
   :bg-opaque {:background-color "var(--bg-opaque)"}
   :bg-focused {:background "var(--bg-focused)"}
   ; :bg-highlighted {:background "var(--bg-highlighted)"}
   ; :bg-hover {:background primary-green}

   ;; REACHER
   ; :reacher-download {:cursor "pointer" :color medium-green}
   ; :reacher-download-hover {:cursor "pointer" :color primary-green}

   ;; Modal
   ; :button {:color dark-green
   ;          :background-color "transparent"
   ;          :border-radius "1px"
   ;          :border (str "1px solid " dark-green)}
   ; :button-hover {:background-color dark-green
   ;                :color light-green}

   ;; Buttons
   :button-positive {:color "var(--button-positive-color)"
                     :background-color "var(--button-positive-bg)"}
   :button-positive-hover {:border-color "var(--button-positive-hover-border)"
                           :color "var(--button-positive-hover-color)"
                           :background-color "var(--button-positive-hover-bg)"}
   :button-negative {:color "var(--button-negative-color)"
                     :background-color "var(--button-negative-bg)"}

   :button-negative-hover {:border-color "var(--button-negative-hover-border)"
                           :color "var(--button-negative-hover-color)"
                           :background-color "var(--button-negative-hover-bg)"}
   :button-neutral {:color "var(--button-neutral-color)"
                    ; :border-color "var(--button-neutral-border)"
                    :background-color "var(--button-neutral-bg)"}
   :button-neutral-hover {:border-color "var(--button-neutral-hover-border)"
                          :background-color "var(--button-neutral-hover-bg)"
                          :color "var(--button-neutral-hover-color)"}
   :button-disabled {:color "var(--button-disabled-color)"
                     :border-color "var(--button-disabled-border)"
                     :background-color "var(--button-disabled-bg)"}

   :animate-border-click {:animation-name "var(--input-normal-click)"
                          :animation-duration ".5s"}
   :animate-text-click {:animation-name "var(--box-button-normal-click)"
                        :animation-duration ".3s"}

   ;; Colors
   :color-highlight {:color "var(--color-highlight)"}
   :color-hover {:color "var(--color-hover)"}
   :color-click {:color "var(--color-click)"}
   :color-positive {:color "var(--color-positive)"}
   :color-negative {:color "var(--color-negative)"}
   :color-neutral {:color "var(--color-neutral) "}
   :color-important {:color "var(--color-important)"}
   :color-exception {:color "var(--color-exception)"}
   :color-warning {:color "var (--color-warning)"}

   ;; Backgrounds
   :bg-positive {:background-color "var(--color-positive)"}
   :bg-negative {:background-color "var(--color-negative)"}
   :bg-neutral {:background-color "var(--color-neutral)"}
   :bg-warn {:background-color "var(--color-warning)"}
   :bg-important {:background-color "var(--color-important)"}
   :bg-exception {:background-color "var(--color-exception)"}
   :bg-exception- {:background-color "var(--color-exception-light)"}
   :bg-exception+ {:background-color "var(--color-exception-heavy)"}

   ;; Borders
   ; :border-modal {:border-color bg-modal}
   :border-positive {:border-color "var(--color-positive)"}
   :border-negative {:border-color "var(--color-negative)"}
   :border-neutral {:border-color "var(--color-neutral)"}
   :border-warn {:border-color "var(--color-warning)"}
   :border-important {:border-color "var(--color-important)"}
   :border-exception {:border-color "var(--color-exception)"}

   ;; Micro Actions
   :box-action {:background-color "var(--box-action-bg)"
                :color "var(--box-action-color)"
                :transition "color .3s ease-in-out"}
   :box-action-hover {:color "var(--box-action-color-hover)"}
   :box-action-selected {:color "var(--box-action-color-selected)"}
   ;;
   :tag {:background-color "var(--tag-bg)"
         :color "var(--tag-c)"
         :font-size "10px"
         :font-weight "600"}

   ;; Explorer
   :tag-cell {:border "1px solid"
              :border-radius "0.125rem"
              :padding-left "0.5rem"
              :padding-right "0.5rem"
              :padding-top "1px"
              :padding-bottom "1px"
              :cursor "pointer"
              :line-height "20px"
              :font-size "10px"
              :font-weight "600"
              :border-color "var(--tag-cell-border)"
              :background-color "var(--tag-cell-bg)"
              :color "var(--tag-cell-text)"}
   :tag-cell-hover {:border-color "var(--tag-cell-border-hover)"
                    :background-color "var(--tag-cell-bg-hover)"
                    :color "var(--tag-cell-text-hover)"}})

(def text
  {:pointer-events-none {:pointer-events "none"}
   :text-xxs {:font-size "0.6875rem" :line-height "1.125rem"}
   :text-xs {:font-size "0.76rem" :line-height "1.25rem"}
   :text-sm {:font-size "0.875rem" :line-height "1.5rem"}
   :text-base {:font-size "1rem" :line-height "1.5rem"}
   :text-pre {:font-size "0.6825" :line-height "1.125rem"}
   :text-column {:font-size "10px" :font-weight "600" :line-height "12px"}
   :box-content {:box-sizing "content-box"}
   :select-none {:user-select "none"}
   :box-border {:box-sizing "border-box"}
   :text-hover {:color "var(--text-hover)"}
   :text-click {:color "var(--text-click)"}
   :text-selected {:color "var(--text-selected)" :text-decoration "none"}
   :text-normal {:text-decoration "none" :color "var(--text-normal)"}
   :text-inactive {:color "var(--text-inactive)" :text-decoration "none"}
   :text-highlight {:text-decoration "none" :color "var(--text-highlight)"}
   ;; TEXT SPACING
   :body-text {:font-size "14px" :font-weight "400" :line-height "24px"}
   :body-text-md {:font-size "14px" :font-weight "500" :line-height "24px"}
   :body-text-bold {:font-size "14px" :font-weight "700" :line-height "24px"}
   :small-text {:font-size "11px" :font-weight "400" :line-height "18px"}
   :small-text-md {:font-size "11px" :font-weight "500" :line-height "18px"}
   :rounded-xs {:border-radius "0.0625rem"}
   :label {:font-size "12px" :font-weight "500" :line-height "20px"}
   :label-sm {:font-size "10px" :font-weight "500" :line-height "18px"}
   :stretch {:align-self "stretch"}})

(def aliases
  (merge
   components
   text))