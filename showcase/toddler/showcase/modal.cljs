(ns toddler.showcase.modal
  (:require
   [shadow.css :refer [css]]
   [toddler.layout :as layout]
   [toddler.router :as router]
   [toddler.ui :as ui]
   [helix.core :refer [$ defnc <> defhook]]
   [helix.dom :as d]
   [helix.hooks :as hooks]
   toddler.showcase.content
   [toddler.showcase.common
    :refer [$info use-code-refresh]]
   [toddler.hooks :as toddler]
   [toddler.i18n.keyword :refer [add-translations]]))

(defnc ModalInfo
  []
  (let [locale (toddler/use-current-locale)]
    (d/div
     {:className $info}
     (case locale
       :es
       (<>
        (d/h4 "Hola,")
        (d/br)
        (d/p
         "Este es un lugar para mostrar " (d/b "componentes modales")
         ". Hay solo unos pocos bloques de construcción modal. Modal "
         (d/b "fondo") ", " (d/b "diálogo"), " y " (d/b "tira") ".")
        (d/p
         "El diálogo modal y la tira modal utilizan el componente de fondo modal para mostrar"
         " los componentes que diseñarás e implementarás."))
       :de
       (<>
        (d/h4 "Hallo,")
        (d/br)
        (d/p
         "Dies ist ein Ort, um " (d/b "modale")
         " Komponenten zu präsentieren. Es gibt nur wenige modulare Bausteine. Modal "
         (d/b "Hintergrund") ", " (d/b "Dialog"), " und " (d/b "Leiste") ".")
        (d/p
         "Der modale Dialog und die modale Leiste verwenden die Hintergrundkomponente, um"
         " die Komponenten anzuzeigen, die Sie entwerfen und implementieren werden."))
       :fr
       (<>
        (d/h4 "Bonjour,")
        (d/br)
        (d/p
         "Voici un endroit pour présenter les " (d/b "composants modaux")
         ". Il y a seulement quelques blocs de construction modaux. Modal "
         (d/b "arrière-plan") ", " (d/b "dialogue"), " et " (d/b "bande") ".")
        (d/p
         "Le dialogue modal et la bande modale utilisent le composant d'arrière-plan modal pour afficher"
         " les composants que vous concevrez et mettrez en œuvre."))
       :hr
       (<>
        (d/h4 "Pozdrav,")
        (d/br)
        (d/p
         "Ovo je mjesto za prikazivanje " (d/b "modalnih")
         " komponenti. Postoji samo nekoliko osnovnih modalnih elemenata. Modal "
         (d/b "pozadina") ", " (d/b "dijalog"), " i " (d/b "traka") ".")
        (d/p
         "Modalni dijalog i modalna traka koriste komponentu pozadine modala za prikaz"
         " komponenti koje ćete dizajnirati i implementirati."))
       (<>
        (d/h4 "Hello,")
        (d/br)
        (d/p
         "This is place to showcase " (d/b "modal")
         " components. There are just few modal building blocks. Modal "
         (d/b "background") ", " (d/b "dialog"), " and " (d/b "strip") ".")
        (d/p
         "Modal dialog and modal strip use modal background component to display"
         " components that you will design and implement."))))))

(defnc BackgroundInfo
  []
  (let [locale (toddler/use-current-locale)
        translate (toddler/use-translate)]
    (case locale
      :es
      (d/div
       {:className $info}
       (d/h4 (translate :showcase.modal.background))
       (d/p
        "Como puedes ver en el código a continuación, el componente modal-background "
        "usará react/createPortal para montar el fondo modal "
        "en el nodo popup/*container* y pasar los hijos del componente al "
        "montado " (d/b "div") " (el que tiene :key ::background)")
       (d/p
        "Las propiedades opcionales para el componente modal-background incluyen:")
       (d/ul
        (d/li (d/strong "can-close?") " - controla si se puede llamar a on-close")
        (d/li (d/strong "on-close")  " - cuando se hace clic en el fondo, llamará a la función on-close")))
      :de
      (d/div
       {:className $info}
       (d/h4 (translate :showcase.modal.background))
       (d/p
        "Wie Sie im untenstehenden Code sehen können, verwendet die Komponente modal-background "
        "react/createPortal, um den Modal-Hintergrund "
        "im popup/*container* Knoten zu montieren und die Kinder des Komponentens an den "
        "montierten " (d/b "div") " (mit :key ::background) weiterzugeben.")
       (d/p
        "Optionale Eigenschaften für die modal-background Komponente umfassen:")
       (d/ul
        (d/li (d/strong "can-close?") " - steuert, ob on-close aufgerufen werden kann")
        (d/li (d/strong "on-close")  " - wenn auf den Hintergrund geklickt wird, wird die Funktion on-close aufgerufen")))
      :fr
      (d/div
       {:className $info}
       (d/h4 (translate :showcase.modal.background))
       (d/p
        "Comme vous pouvez le voir dans le code ci-dessous, le composant modal-background "
        "utilisera react/createPortal pour monter l'arrière-plan modal "
        "sur le nœud popup/*container* et transmettre les enfants du composant au "
        "monté " (d/b "div") " (celui avec :key ::background)")
       (d/p
        "Les propriétés optionnelles pour le composant modal-background incluent :")
       (d/ul
        (d/li (d/strong "can-close?") " - contrôle si on-close peut être appelé")
        (d/li (d/strong "on-close")  " - lorsqu'on clique sur l'arrière-plan, appelle la fonction on-close")))
      :hr
      (d/div
       {:className $info}
       (d/h4 (translate :showcase.modal.background))
       (d/p
        "Kao što možete vidjeti u kodu ispod, komponenta modal-background "
        "koristi react/createPortal za montiranje pozadine modala "
        "na popup/*container* čvor i prosljeđuje djecu komponente u "
        "montirani " (d/b "div") " (onaj s :key ::background)")
       (d/p
        "Opcionalna svojstva za komponentu modal-background uključuju:")
       (d/ul
        (d/li (d/strong "can-close?") " - kontrolira može li se pozvati on-close")
        (d/li (d/strong "on-close")  " - kad se klikne na pozadinu, poziva funkciju on-close")))
      (d/div
       {:className $info}
       (d/h4 (translate :showcase.modal.background))
       (d/p
        "As you can see in code bellow, modal-background component "
        "will use react/createPortal to mount modal backround "
        "on popup/*container* node and pass component children to "
        "mounted " (d/b "div") " (one with :key ::background)")
       (d/p
        "Optional properties for modal-background component include:")
       (d/ul
        (d/li (d/strong "can-close?") " - controls if on-close can be called")
        (d/li (d/strong "on-close")  " - when clicked on background will call on-close fn"))))))

(defnc DialogInfo
  []
  (let [locale (toddler/use-current-locale)
        translate (toddler/use-translate)]
    (case locale
      :es
      (d/div
       {:className $info}
       (d/h4 (translate :showcase.modal.dialog))
       (d/p
        "El componente Dialog creará una ventana de diálogo y ajustará su tamaño "
        "al contenido interno. Al igual que modal-background, admite el manejador " (d/b "on-close ")
        "que se llamará cuando el usuario haga clic fuera de la ventana de diálogo. Si se omite, "
        "deberás manejar el cierre del modal por otros medios. Como un botón o algo "
        "similar.")
       (d/p
        "Hay algunas características estilizadas que vienen con el componente dialog de forma predeterminada. "
        "Estas características se activan añadiendo clases al contenido que se agrega al componente ui/modal-dialog.")
       (d/ul
        (d/li (d/strong "title") " - Indicando el propósito del modal-dialog. Para enfatizar el estado actual.")
        (d/li (d/strong "content") " - Añadir la clase content añadirá padding para coincidir con el footer y el título.")
        (d/li (d/strong "footer")  " - Un lugar común para añadir botones, por lo que esta clase añadirá estilos para mostrar botones de manera estándar.")))
      :de
      (d/div
       {:className $info}
       (d/h4 (translate :showcase.modal.dialog))
       (d/p
        "Die Dialog-Komponente erstellt ein Dialogfenster und passt seine Größe "
        "an den internen Inhalt an. Wie modal-background unterstützt sie den " (d/b "on-close ")
        "Handler, der aufgerufen wird, wenn der Benutzer außerhalb des Dialogfensters klickt. Wenn weggelassen, "
        "müssen Sie das Schließen des Modals auf andere Weise handhaben, z. B. mit einem Button oder etwas "
        "Ähnlichem.")
       (d/p
        "Es gibt einige gestylte Funktionen, die standardmäßig mit der Dialog-Komponente geliefert werden. "
        "Diese Funktionen werden durch das Hinzufügen von Klassen zum Inhalt aktiviert, der zur ui/modal-dialog-Komponente hinzugefügt wird.")
       (d/ul
        (d/li (d/strong "title") " - Gibt den Zweck des modal-dialog an. Um den aktuellen Zustand zu betonen.")
        (d/li (d/strong "content") " - Durch Hinzufügen der Klasse content wird ein Padding hinzugefügt, um Footer und Titel anzugleichen.")
        (d/li (d/strong "footer")  " - Ein üblicher Ort, um Buttons hinzuzufügen, daher fügt diese Klasse Stile hinzu, um Buttons auf übliche Weise anzuzeigen.")))
      :fr
      (d/div
       {:className $info}
       (d/h4 (translate :showcase.modal.dialog))
       (d/p
        "Le composant Dialog créera une fenêtre de dialogue et ajustera sa taille "
        "au contenu interne. Comme modal-background, il prend en charge le gestionnaire " (d/b "on-close ")
        "qui sera appelé lorsque l'utilisateur cliquera en dehors de la fenêtre de dialogue. Si omis, "
        "vous devrez gérer la fermeture du modal par d'autres moyens, comme un bouton ou quelque chose de "
        "similaire.")
       (d/p
        "Il existe des fonctionnalités stylisées qui accompagnent le composant dialog par défaut. "
        "Ces fonctionnalités sont activées en ajoutant une classe au contenu ajouté au composant ui/modal-dialog.")
       (d/ul
        (d/li (d/strong "title") " - Indique l'objectif du modal-dialog. Pour mettre en valeur l'état actuel.")
        (d/li (d/strong "content") " - Ajouter la classe content ajoutera un padding pour correspondre au footer et au titre.")
        (d/li (d/strong "footer")  " - Un endroit commun pour ajouter des boutons, cette classe ajoutera des styles pour afficher les boutons de manière standardisée.")))
      :hr
      (d/div
       {:className $info}
       (d/h4 (translate :showcase.modal.dialog))
       (d/p
        "Komponenta Dialog će stvoriti prozor dijaloga i prilagoditi njegovu veličinu "
        "unutarnjem sadržaju. Kao i modal-background, podržava upravljač " (d/b "on-close ")
        "koji će se pozvati kada korisnik klikne izvan prozora dijaloga. Ako se izostavi, "
        "morat ćete zatvaranje modala riješiti na neki drugi način, poput gumba ili nečeg "
        "sličnog.")
       (d/p
        "Postoje neke stilizirane značajke koje dolaze s komponentom dialog već ugrađene. "
        "Te se značajke aktiviraju dodavanjem klase sadržaju koji se dodaje ui/modal-dialog komponenti.")
       (d/ul
        (d/li (d/strong "title") " - Navodi svrhu modal-dialog. Za naglašavanje trenutnog stanja.")
        (d/li (d/strong "content") " - Dodavanje klase content će dodati padding kako bi se uskladilo s footerom i naslovom.")
        (d/li (d/strong "footer")  " - Uobičajeno mjesto za dodavanje gumba, pa će ova klasa dodati stilove za prikaz gumba na uobičajen način.")))
      (d/div
       {:className $info}
       (d/h4 (translate :showcase.modal.dialog))
       (d/p
        "Dialog component will create dialog window and adjust its size "
        "to inside content. Same as modal-background it supports " (d/b "on-close ")
        "handler, that will be called when user clicks outside of dialog window. If omitted "
        "you will have to handle modal closing by some other means. Like button or something "
        "similar.")
       (d/p
        "There are some styled features that come with dialog component out of the box. "
        "This features are activated by adding class to content that is added to ui/modal-dialog component")
       (d/ul
        (d/li (d/strong "title") " - Stating the purpose of modal-dialog. To emphisize current state")
        (d/li (d/strong "content") " - Adding content class will add padding to match footer and title")
        (d/li (d/strong "footer")  " - Common place to add buttons, so this class will add styles for displaying buttons in common fashion"))))))

(add-translations
 (merge
  #:showcase.modal {:default "Modal"
                    :hr "Modalni"}
  #:showcase.modal.info {:default ModalInfo}
  #:showcase.modal.background.info {:default BackgroundInfo}
  #:showcase.modal.dialog.info {:default DialogInfo}
  #:showcase.modal.background {:default "modal background"
                               :hr "modalna pozadina"}
  #:showcase.modal.dialog {:default "modal dialog"
                           :hr "Pritisni za modalni dijalog"}
  #:showcase.modal.dialog.title {:default "This is title for modal dialog"
                                 :hr "Ovo je naslov za digitalni modalni prozor"}))

(defhook use-close []
  (let [close! (router/use-go-to :toddler.modal)]
    #(close!)))

(defhook use-register [id segment]
  (router/use-component-children
   :toddler.modal
   [{:id id :segment segment}]))

(defnc Background
  []
  (let [show! (router/use-go-to :toddler.modal.background)
        opened? (router/use-rendered? :toddler.modal.background)
        close! (use-close)
        translate (toddler/use-translate)]
    (use-register :toddler.modal.background "background")
    (<>
     ($ (translate :showcase.modal.background.info))
     ($ ui/row
        ($ ui/button
           {:on-click #(show!)}
           (translate :open))
        (when opened?
          ($ ui/modal-background
             {:on-close close!})))
     (d/pre
      (d/code
       {:class ["language-clojure"]}
       "(defnc modal-background
  [{:keys [class className can-close? on-close]
    :or {can-close? true
         on-close identity}
    :as props}]
  (let [container-node (hooks/use-context popup/*container*)]
    (rdom/createPortal
     (d/div
      {:key ::background
       :on-click (fn [] (when can-close? (on-close)))
       :class (cond-> [$modal-background
                       (when-not can-close? \" block \")]
                (string? class) (conj class)
                (sequential? class) (into class)
                (string? className) (conj className))
       & (dissoc props :class :className :can-close? :on-click :onClick)}
      (c/children props))
     @container-node)))")))))

(defnc Dialog
  []
  (let [id :toddler.modal.dialog
        show! (router/use-go-to id)
        opened? (router/use-rendered? id)
        close! (use-close)
        translate (toddler/use-translate)
        [context set-context!] (hooks/use-state nil)]
    (use-register :toddler.modal.dialog "dialog")
    (<>
     ($ (translate :showcase.modal.dialog.info))
     ($ ui/row
        ($ ui/button
           {:on-click #(do
                         (set-context! nil)
                         (show!))}
           (translate :neutral))
        ($ ui/button
           {:on-click #(do
                         (set-context! "positive")
                         (show!))
            :class ["positive"]}
           (translate :positive))
        ($ ui/button
           {:on-click #(do
                         (set-context! "negative")
                         (show!))
            :class ["negative"]}
           (translate :button.negative))
        ($ ui/button
           {:on-click #(do
                         (set-context! "warn")
                         (show!))
            :class ["warn"]}
           (translate :warn))
        (when opened?
          ($ ui/modal-dialog
             {:on-close close!
              :width 300
              :className (when context (name context))}
             (d/span
              {:className "title"}
              (translate :showcase.modal.dialog.title))
             (d/div
              {:class "content"
               :style {:max-width 400}}
              (d/pre
               {:className (css :mt-4 :word-break :whitespace-pre-wrap)}
               (translate :showcase.content.large)))
             (d/div
              {:className "footer"}
              ($ ui/button {:on-click close!} (translate :ok))
              ($ ui/button {:on-click close!} (translate :cancel))))))
     (d/pre
      (d/code
       {:className "language-clojure"}
       "($ ui/modal-dialog
 {:on-close close!
  :width 300
  :style {:max-width 400}
  :className (when context (name context))}
 (d/span
  {:className \"title\"}
  (translate :showcase.modal.dialog.title))
 (d/div
  {:class \"content\"}
  (d/pre
   {:className (css :mt-4 :word-break :whitespace-pre-wrap)}
   (translate :showcase.content.large)))
 (d/div
  {:className \"footer\"}
  ($ ui/button {:on-click close!} (translate :ok))
  ($ ui/button {:on-click close!} (translate :cancel))))")))))

(defnc Complex
  []
  (use-register :toddler.modal.complex "complex"))

(defnc Modal
  []
  (let [{:keys [height width]} (layout/use-container-dimensions)
        translate (toddler/use-translate)]
    (use-code-refresh)
    ($ ui/simplebar
       {:style {:height height
                :width width}}
       ($ ui/row {:align :center}
          ($ ui/column
             {:align :center
              :style {:max-width "30rem"}}
             ($ (translate :showcase.modal.info))
             ($ Background)
             ($ Dialog)
             ($ Complex))))))
