## Welcome

This is an introduction to documenting your app components and... anything else!
As you can see, this document is written in `Markdown` format. Check out
your file `dev/docs/intro.md`, and you will find this text there.

**Try changing the text in this file to see updates here.**


File is served using ```shadow-cljs.edn``` and monitored by ```toddler.md/watch-url```
functional component with default refresh interval of 3 seconds.
```clojure
(defnc Intro
  {:wrap [(router/wrap-rendered ::intro)]}
  []
  (let [{:keys [width]} (layout/use-container-dimensions)]
    ($ ui/row {:align :center}
       ($ ui/simplebar
          {:style {:width (min width 500)}}
          ($ md/watch-url {:url "intro.md"})))))
```


## SIDEBAR

The left sidebar is populated by components in the `{{project}}.docs` namespace
by specifying which component is rendered at which URL pathname.

```clojure
(def components
  [{:id ::intro
    :segment "intro"
    :name "Intro"
    :render Intro}])
```

You can add items to the sidebar by adding more records to `components`.
Of course, this is just a template, so you can customize it however you like.

If you want to link subcomponents in the navigation,
you can do so by linking them to the `:id` of items defined in `components`.

See Toddler's [routing](https://gersak.github.io/toddler/routing).

## WHITE LABELING

You can change logo and actions in documentation layout by
providing `:render/logo` and `:render/actions` components
to `toddler.docs/page` component. Open  `{{project}}/docs.cljs`.

```clojure
(defnc _Docs
  []
  (router/use-link ::router/ROOT components)
  ($ docs/page
     {:max-width 1000
      :components components
      :render/logo logo
      :render/actions actions}))
```

## CUSTOM HTML

You can add custom HTML in this file as well. If you include HTML elements
with an `id` or another attribute that identifies a DOM element, you can use
React Portal to mount custom components to that element.

Or you can just use
[toddler.core/portal](https://github.com/gersak/toddler/blob/054d2fbef85ebf434ee699905e3a6cdfc968fe25/src/toddler/core.cljs#L32).

<div id="example-component-here" style="margin-top:20px;">
  Check me out... I'm right here in the <strong>/dev/docs/intro.md</strong> file.
</div>

<iframe src="https://giphy.com/embed/h8n8aJWronkmvRTB0y"  
        width="384" height="480" style="margin-top:40px;"  
        frameBorder="0" class="giphy-embed" allowFullScreen>
</iframe>  
<p><a href="https://giphy.com/gifs/toferra-trea-turner-h8n8aJWronkmvRTB0y">via GIPHY</a></p>
