## Core
During years of frontend development I've often had issues with overflowing and scrolling.
Yes you can combine HTML and CSS and even use @media rule and build that house of cards
again and again. 

Every time I fixed something, something else broke. For now most reliable approach is to
combine CSS with JS and force components to have "fixed" width and height. This is why in
Toddler default components you will often find [Simplebar](https://grsmto.github.io/simplebar/)
that will control content container and display consistet scrollbars on different browsers
and operating systems.

#### container-dimensiosn
In `toddler.layout` namespace there is very important **\*container-dimensions\*** context.
This context can be used in child components to compute how much each of component is allowed
to take space inside of parent container.

I.E. Tabs. Lets say that i wan't to build tabs component that will have multiple tab content
bindings. Tabs should show what tabs are available and what is tab content. So with above
logic, tabs component would take `*container-dimensions*` and draw tabs, then compute what
is tabs height and render all other children with original `*container-dimensions*` reduced
by tabs height. All children should be provided new `*container-dimensions*` context with
updated height.

Many other components are built this way, by combining value of `*container-dimensions*`
context and passing it to simplebar component to take care of scroll.


Usually _App_ starts with window dimensions, than slices available space to navigation menu,
action-bar, content section, etc. By excercising little math on this main components you
will feel great relief and control over development of UI.

**Don't use body scroll.** Slice up that viewport to usable HUD like components and use
`*container-dimensions*` to divide with even finner gradation.



## Rows and Columns
Components are working on [flexbox](https://css-tricks.com/snippets/css/a-guide-to-flexbox/)
layout properties. Both will try to fill available space and grow as much as possible.

Both `column` and `row` accept `:position` or `:align` value with one of 4 available
options:

 * **:start**   - start displaying elements from start
 * **:center**  - group elements at center 
 * **:end**     - group elements at the end of row/column
 * **:explode** - move elements away from each other as much as possible


By combining rows/columns and alignment and other css properties it is
not that hard to create __complex__ stable layout.


<div id="rows-columns-example"></div>
