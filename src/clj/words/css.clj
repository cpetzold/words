(ns words.css
  (:require
   [garden.core :as garden]
   [garden.units :as u :refer [px percent]]))

(def colors
  {:bg :#fff
   :headline :#222
   :text :#a4a9a9})

(def rules
  [[:body :h1 :h2 :h3 :h4 :h5 :p
    {:margin 0
     :padding 0}]

   [:body
    {:background (:bg colors)
     :color (:text colors)
     :font {:family "Myriad Pro"
            :size (px 32)}
     }]

   [:#word
    {:text-align "center"
     :font-size "5em"
     :margin-top (px 300)
     }
    ]

   [:#unscrambled
    {:color :#000}
    ]
   ])

(defn css []
  (garden/css
   {:output-style :expanded}
   rules))