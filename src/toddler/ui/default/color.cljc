(ns toddler.ui.default.color)


(def color
  (merge
    {:gray "#616161"
     :teal "#80d6d6"
     :blue "#85b7f1"
     :red "#fd6286"
     :orange "#ff872d"
     :yellow "#ffec72"
     :green "#0dda87"
     :asphalt "#9cb4d8"
     :white "white"
     :disabled "#bbbbbb"
     :link "rgba(44,29,191,0.78)"
     
     :color "#275f82"
     :background "white"}
    #:gray {:light "#f2eff2"
            :dark "#2c2c2c"}
    #:teal {:dark "#598ea7"
            :deep "#44666d"
            :saturated "#00cccc"}
    #:asphalt {:dark "#3562a2"
               :bleached "#598ea71a"}
    #:orange {:dark "#ff4007"}
    #:yellow {:dark "#FFDC00"
              :deep "#c7ac00"}
    #:green {:dark "#02bf72"}
    #:white {:transparent "#ffffffee"}))
