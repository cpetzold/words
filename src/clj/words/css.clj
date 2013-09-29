(ns words.css
  (:require
   [garden.core :as garden]
   [garden.units :as u :refer [px percent]]))

(def colors
  {:bg :#f2d31d
   :text :#dbbb09
   :bg-2 :#fee970
   :highlight :#fff
   })

(def rules
  [[:body :h1 :h2 :h3 :h4 :h5 :p
    {:margin 0
     :padding 0}]

   [:body
    {:background (:bg colors)
     :color (:text colors)
     :font {:family "Open Sans"
            :size (px 32)}
     }]

   [:#word
    {:text-align "center"
     :font {:size (px 60) :weight 800}
     :margin-top (px 270)
     }]

   (let [size (px 80)]
     [:.letter
      {:display "inline-block"
       :width size
       :height size
       :line-height size
       :background (:bg-2 colors)
       :color (:text colors)
       :border-radius (px 4)
       :margin [0 (px 5)]
       :text-transform "uppercase"
       }])

   [:#unscrambled
    [:.letter
     {:background (:highlight colors)
      :color (:bg colors)}
     ]]
   ])

(defn css []
  (garden/css
   {:output-style :expanded}
   rules))