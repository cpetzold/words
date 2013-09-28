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
     :line-height (px 48)
     :width (percent 300)
     ;;:overflow "hidden"
     }]

   [:.article-container
    {:display "inline-block"
     :vertical-align "top"
     :width (percent (/ 100 3))
     :overflow "hidden"}]

   [:.article
    {:width (px 800)
     :margin "0 auto"}]

   [:.article-image
    {:width (percent 100)
     :height (px 450)
     :background-size "cover"
     :margin-bottom (px 16)}]

   [:h1
    {:color (:headline colors)
     :font {:size (px 45)
            :weight "lighter"}
     :letter-spacing (px (- 2))
     :margin [0 0 (px 16) 0]}]
   ])

(defn css []
  (garden/css
   {:output-style :expanded}
   rules))