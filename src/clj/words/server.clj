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
   [wordnik.api.words :as wn-words]
   [digest :as digest]
   [words.css :as css]))

(def wordnik-key "997ea3a64b190c6d8f0040df7b6003393e51bbd224bc5ec8d")

(defn scramble [s]
  (->> (seq s) shuffle (apply str)))

(defn scrambled-word []
  (wn/with-api-key wordnik-key
    (let [word (:word (wn-words/random-word :minCorpusCount 10))]
      {:word word
       :scrambled-word (scramble word)
       :encoded-word (digest/md5 word)})))

(defn layout [& content]
  (page/html5
   [:head
    [:title "words"]
    (page/include-css "style.css")]
   [:body content]))

(defroutes routes
  (context "/word" []
           (GET "/scrambled" [] (response/json (scrambled-word)))
           (GET "/check" [word encoded-word]
                (response/json (= (digest/md5 word) encoded-word))))
  (GET "/" []
       (layout
        [:div#word]
        (element/javascript-tag "var CLOSURE_NO_DEPS = true")
        (page/include-js "words.js")
        (element/javascript-tag "words.core.init()")))
  (GET "/style.css" [] (response/content-type "text/css" (css/css)))
  (resources "/"))

(def handler (-> routes handler/api))