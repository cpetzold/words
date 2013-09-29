(ns words.css
  (:require
   [garden.core :as garden]
   [garden.units :as u :refer [px percent]]
   [garden.color :as color]))

(def colors
  {:bg :#f2d31d
   :text :#dbbb09
   :text-2 :#b1970a
   :bg-2 :#fee970
   :highlight :#fff
   :error :#f03a3a
   :success :#06c96f
   })

(def +letter-size+ (px 80))

(def rules
  [[:body :h1 :h2 :h3 :h4 :h5 :p
    {:margin 0
     :padding 0}]

   [:body :html
    {:height (percent 100)}]

   [:body
    {:-webkit-user-select "none"
     :background (:bg colors)
     :color (:text colors)
     :font {:family "Open Sans"
            :size (px 20)
            :weight 600}
     }]

   [:#round
    {:position "relative"
     :top (percent 30)}]

   [:#word
    {:text-align "center"
     :font {:size (px 60) :weight 800}
     :margin-bottom (px 60)
     :height +letter-size+}
    [:&.success
     [:.letter {:color (:highlight colors)
                :background (:success colors)}]]
    [:&.error
     [:.letter {:color (:highlight colors)
                :background (:error colors)}]]]

   [:.letter
    {:display "inline-block"
     :width +letter-size+
     :height +letter-size+
     :line-height +letter-size+
     :background (:bg-2 colors)
     :color (:text colors)
     :border-radius (px 4)
     :margin [0 (px 5)]
     :text-transform "uppercase"
     }]

   [:#unscrambled
    [:.letter
     {:background (:highlight colors)
      :color (:bg colors)}
     ]]

   [:#hud
    {:text-align "center"}]

   [:.spacer {:margin [0 (px 10)]}]

   [:b {:color (:text-2 colors)}]

   [:#tweet-score
    {:display "block"
     :font-size (px 15)
     :color :#dbbb09
     :text-decoration "none"}]

   [:.twitter {:width (px 13)
               :height (px 11)
               :margin-right (px 5)}]

   [:#overlay
    {:position "fixed"
     :top 0
     :left 0
     :width (percent 100)
     :height (percent 100)
     :background "rgba(200, 171, 9, 0.6)"}]

   (let [width 460]
     [:.modal
      {:position "fixed"
       :left (percent 50)
       :top (percent 20)
       :width (px width)
       :margin-left (px (- (/ width 2)))
       :background (:highlight colors)
       :border-radius (px 4)
       :padding [(px 60) 0]
       :text-align "center"}

      [:h3
       {:font {:size (px 30)
               :weight 800}
        :text-align "center"
        :color :#b1970a
        :margin-bottom (px 5)
        }]

      [:button
       {:cursor "pointer"
        :margin-top (px 30)
        :padding [(px 20) (px 60)]
        :background (:bg-2 colors)
        :color :#c8ab09
        :border-radius (px 4)
        :border "none"
        :font {:family "Open Sans"
               :size (px 20)
               :weight 800}
        :transition "0.3s all"}
       [:&:hover
        {:background :#FCEB89}]]
      ])

   [:#footer
    {:position "fixed"
     :bottom (px 20)
     :width (percent 100)
     :font-weight 600
     :text-align "center"
     }
    ]

   [:a
    {:color (:text-2 colors)
     :transition "0.3s all"}
    [:&:hover
     {:color :#9b8409}]]

   [:.vote
    {:display "block"
     :margin-bottom (px 10)
     :text-decoration "none"}]

   [:#made
    {:line-height (px 50)}]

   [:.eggheart
    {:width (px 33)
     :height (px 47)
     :vertical-align (px (- 14))}]])

(defn css []
  (garden/css
   {:output-style :expanded}
   rules))