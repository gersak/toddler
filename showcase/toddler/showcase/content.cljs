(ns toddler.showcase.content
  (:require [toddler.i18n.keyword :refer [add-translations]]))

(def large-text
  "In a quiet valley surrounded by rolling hills, a small village thrived where whispers of ancient stories echoed through the air. Beneath the golden sunlight, children laughed and played while elders gathered under the shade of the old oak tree, weaving tales of wisdom. Time seemed to flow gently, like the stream that meandered through the heart of the village, nourishing life in all its forms.

Far beyond the village boundaries, mysterious ruins stood as silent witnesses to a forgotten era. Their crumbling walls, cloaked in ivy and moss, whispered secrets to those who dared venture near. Travelers often spoke of peculiar happenings, of shadows moving where none should, and faint melodies carried on the wind.

Yet within the village, a sense of harmony persisted. The baker rose with the dawn, filling the streets with the warm scent of freshly baked bread. Farmers toiled in the fields, their songs blending with the rhythm of nature. Life, though simple, was rich with purpose and connection, a gentle reminder of the beauty in the mundane.")

(add-translations
 (merge
  #:showcase.content.large
   {:default
    "In a quiet valley surrounded by rolling hills, a small village thrived where whispers of ancient stories echoed through the air. Beneath the golden sunlight, children laughed and played while elders gathered under the shade of the old oak tree, weaving tales of wisdom. Time seemed to flow gently, like the stream that meandered through the heart of the village, nourishing life in all its forms.

Far beyond the village boundaries, mysterious ruins stood as silent witnesses to a forgotten era. Their crumbling walls, cloaked in ivy and moss, whispered secrets to those who dared venture near. Travelers often spoke of peculiar happenings, of shadows moving where none should, and faint melodies carried on the wind.

Yet within the village, a sense of harmony persisted. The baker rose with the dawn, filling the streets with the warm scent of freshly baked bread. Farmers toiled in the fields, their songs blending with the rhythm of nature. Life, though simple, was rich with purpose and connection, a gentle reminder of the beauty in the mundane."
    :hr "U mirnoj dolini okruženoj valovitim brdima, cvjetalo je malo selo gdje su šaptaji drevnih priča odjekivali zrakom. Pod zlatnim suncem, djeca su se smijala i igrala dok su se stariji okupljali u sjeni starog hrasta, pletući priče mudrosti. Vrijeme je protjecalo nježno, poput potoka koji se vijugavo probijao kroz srce sela, hraneći život u svim njegovim oblicima.

Daleko izvan granica sela, tajanstvene ruševine stajale su kao nijemi svjedoci zaboravljenog doba. Njihovi raspadajući zidovi, prekriveni bršljanom i mahovinom, šaptali su tajne onima koji su se usudili približiti. Putnici su često govorili o neobičnim događajima, o sjenama koje se kreću ondje gdje ne bi trebale i o tihim melodijama koje vjetar nosi."
    :gr "In einem ruhigen Tal, umgeben von sanften Hügeln, blühte ein kleines Dorf, wo das Flüstern uralter Geschichten durch die Luft hallte. Unter der goldenen Sonne lachten und spielten die Kinder, während die Ältesten sich im Schatten der alten Eiche versammelten, um Geschichten voller Weisheit zu erzählen. Die Zeit schien sanft zu fließen, wie der Bach, der sich durch das Herz des Dorfes schlängelte und das Leben in all seinen Formen nährte.

Weit außerhalb der Dorfgrenzen standen geheimnisvolle Ruinen, stumme Zeugen einer vergessenen Ära. Ihre zerfallenen Mauern, überwuchert von Efeu und Moos, flüsterten Geheimnisse denen zu, die sich wagten, näherzukommen. Reisende sprachen oft von seltsamen Vorkommnissen, von Schatten, die sich bewegten, wo sie nicht sein sollten, und von leisen Melodien, die der Wind trug.

Doch im Dorf herrschte ein Gefühl der Harmonie. Der Bäcker stand mit der Morgendämmerung auf und erfüllte die Straßen mit dem warmen Duft frisch gebackenen Brotes. Die Bauern arbeiteten auf den Feldern, ihre Lieder verschmolzen mit dem Rhythmus der Natur. Das Leben, obwohl einfach, war reich an Sinn und Verbundenheit, eine sanfte Erinnerung an die Schönheit im Alltäglichen."
    :fr "Dans une vallée paisible entourée de collines ondulantes, un petit village prospérait, où des murmures d’histoires anciennes résonnaient dans l’air. Sous le soleil doré, les enfants riaient et jouaient tandis que les anciens se rassemblaient à l’ombre du vieux chêne, tissant des récits pleins de sagesse. Le temps semblait couler doucement, comme le ruisseau qui serpentait à travers le cœur du village, nourrissant la vie sous toutes ses formes.

Loin au-delà des frontières du village, des ruines mystérieuses se tenaient en témoins silencieux d’une époque oubliée. Leurs murs en ruine, recouverts de lierre et de mousse, murmuraient des secrets à ceux qui osaient s’en approcher. Les voyageurs parlaient souvent de phénomènes étranges, d’ombres mouvantes là où il ne devrait pas y en avoir, et de mélodies légères portées par le vent.

Pourtant, au sein du village, un sentiment d’harmonie persistait. Le boulanger se levait à l’aube, remplissant les rues du parfum chaleureux de pain fraîchement cuit. Les fermiers travaillaient dans les champs, leurs chansons se mêlant au rythme de la nature. La vie, bien que simple, était riche de sens et de connexions, un doux rappel de la beauté dans l’ordinaire."
    :sp "En un valle tranquilo rodeado de colinas ondulantes, prosperaba un pequeño pueblo donde los susurros de historias antiguas resonaban en el aire. Bajo el sol dorado, los niños reían y jugaban mientras los ancianos se reunían bajo la sombra del viejo roble, entrelazando cuentos llenos de sabiduría. El tiempo parecía fluir suavemente, como el arroyo que serpenteaba por el corazón del pueblo, nutriendo la vida en todas sus formas.

Lejos, más allá de los límites del pueblo, unas misteriosas ruinas se alzaban como testigos silenciosos de una era olvidada. Sus muros derruidos, cubiertos de hiedra y musgo, susurraban secretos a quienes se atrevían a acercarse. Los viajeros hablaban a menudo de sucesos extraños, de sombras que se movían donde no deberían estar y de melodías suaves llevadas por el viento.

Sin embargo, dentro del pueblo, persistía una sensación de armonía. El panadero se levantaba al amanecer, llenando las calles con el cálido aroma del pan recién horneado. Los campesinos trabajaban en los campos, sus canciones se mezclaban con el ritmo de la naturaleza. La vida, aunque sencilla, estaba llena de propósito y conexión, un suave recordatorio de la belleza en lo cotidiano."
    :pt "Em um vale tranquilo cercado por colinas ondulantes, prosperava uma pequena vila onde sussurros de histórias antigas ecoavam pelo ar. Sob o sol dourado, as crianças riam e brincavam enquanto os anciãos se reuniam à sombra do velho carvalho, tecendo contos cheios de sabedoria. O tempo parecia fluir suavemente, como o riacho que serpenteava pelo coração da vila, nutrindo a vida em todas as suas formas.

Bem além dos limites da vila, ruínas misteriosas erguiam-se como testemunhas silenciosas de uma era esquecida. Seus muros em ruínas, cobertos por hera e musgo, sussurravam segredos àqueles que ousavam se aproximar. Os viajantes frequentemente falavam de acontecimentos estranhos, de sombras que se moviam onde não deveriam e de melodias suaves levadas pelo vento.

Ainda assim, dentro da vila, persistia um sentimento de harmonia. O padeiro levantava-se ao amanhecer, enchendo as ruas com o aroma acolhedor de pão recém-assado. Os agricultores trabalhavam nos campos, suas canções misturavam-se ao ritmo da natureza. A vida, embora simples, era rica em propósito e conexão, um suave lembrete da beleza no cotidiano."}))
