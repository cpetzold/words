(ns words.server
  (:require
   [clojure.string :as str]
   [compojure.core :refer [defroutes context GET]]
   [compojure.route :refer [resources]]
   [compojure.handler :as handler]
   [noir.response :as response]
   [noir.io :as io]
   [hiccup.page :as page]
   [hiccup.element :as element]
   [wordnik.core :as wn]
   [wordnik.api.word :as wn-word]
   [wordnik.api.words :as wn-words]
   [words.css :as css]))

(def +wordnik-key+ "997ea3a64b190c6d8f0040df7b6003393e51bbd224bc5ec8d")
(def +excluded-pos+ ["family-name"
                     "given-name"
                     "noun-plural"
                     "noun-posessive"
                     "proper-noun"
                     "proper-noun-plural"
                     "proper-noun-posessive"])

(defn scramble [s]
  (->> (seq s) shuffle (apply str)))

(defn word-points [word]
  (wn/with-api-key +wordnik-key+
    (try
      (when (seq (wn-word/definitions word))
        (let [frequency (:totalCount (wn-word/frequency word :startYear 2000))]
          (int (+ (count word) (* 2 (count word) (/ 1 (inc frequency)))))))
      (catch Throwable e
        nil))))

(defn valid-word? [word]
  (wn/with-api-key +wordnik-key+
    (let [parts-of-speech (->> (wn-word/definitions word)
                               (map :partOfSpeech)
                               (filter identity))]
      (boolean (seq parts-of-speech)))))

(defn scrambled-word []
  (wn/with-api-key +wordnik-key+
    (let [word (str/lower-case
                (:word (wn-words/random-word
                        :maxLength 6
                        :minCorpusCount 5000
                        :excludePartOfSpeech (str/join "," +excluded-pos+))))]
      (if-not (valid-word? word)
        (scrambled-word)
        {:word word
         :scrambled-word (scramble word)}))))

(defn layout [& content]
  (page/html5
   [:head
    [:title "words"]
    (page/include-css "http://fonts.googleapis.com/css?family=Open+Sans:800,700,400")
    (page/include-css "style.css")]
   [:body content]))

(defroutes routes
  (context "/word" []
           (GET "/scrambled" [] (response/json (scrambled-word)))
           (GET "/check" [word] (response/json {:points (word-points word)})))
  (GET "/" []
       (layout
        [:div#round]
        [:div#footer
         [:a.vote {:href "http://clojurecup.com/app.html?app=words"} "Vote for us on Clojure Cup!"]
         [:span#made "Made with " " in San Francisco by "
          [:a {:href "http://twitter.com/natenavasca"} "@natenavasca"]
          " and "
          [:a {:href "http://twitter.com/natenavasca"} "@cpetzold"]]]
        (element/javascript-tag "var CLOSURE_NO_DEPS = true")
        (page/include-js "words.js")
        (element/javascript-tag "words.core.init()")))
  (GET "/style.css" [] (response/content-type "text/css" (css/css)))
  (resources "/"))

(def handler (-> routes handler/api))