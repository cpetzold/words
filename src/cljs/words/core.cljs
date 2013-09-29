(ns words.core
  (:require-macros
   [cljs.core.async.macros :refer [go]]
   [dommy.macros :refer [deftemplate sel1]])
  (:require
   [clojure.string :as string]
   [cljs.core.async :as async :refer [chan put! <! >! close! timeout]]
   [dommy.core :as dommy]))

(defn scramble [s]
  (->> (seq s) shuffle (apply str)))

(defn map->query-string
  "{:foo \"bar\" :lorem \"ipsum\"} => foo=bar&lorem=ipsum"
  [query-params]
  (->> query-params
       (sort-by first)
       (map  (fn [[k v]]
               (str (name k) "=" (js/encodeURIComponent v))))
       (string/join "&")))

(defn request [url & [params]]
  (let [c (chan)
        req (js/XMLHttpRequest.)]
    (.open req "GET" (str url "?" (map->query-string params)) true)
    (set!
     (.-onreadystatechange req)
     #(when (= 4 (.-readyState req))
        (put! c (-> (.-responseText req)
                    js/JSON.parse
                    (js->clj :keywordize-keys true)))
        (close! c)))
    (.send req)
    c))

(defn listen [node type & [prevent?]]
  (let [c (chan)]
    (dommy/listen!
     node type
     (fn [e]
       (when (and (and prevent? (prevent? e))
                  (not (or (.-metaKey e)
                           (.-altKey e)
                           (.-ctrlKey e))))
         (.preventDefault e))
       (put! c e)))
    c))

;; Copied from http://git.io/ATzRXg (@swannodette)
(defn map< [f in]
  (let [out (chan)]
    (go (loop []
          (if-let [x (<! in)]
            (do (>! out (f x))
                (recur))
            (close! out))))
    out))

(defn filter< [pred in]
  (let [out (chan)]
    (go (loop []
          (if-let [x (<! in)]
            (do (when (pred x) (>! out x))
                (recur))
            (close! out))))
    out))

(defn remove-first [pred coll]
  (concat
   (take-while (complement pred) coll)
   (rest (drop-while (complement pred) coll))))

(deftemplate letter-box [c]
  [:span.letter c])

(deftemplate word-text [unscrambled scrambled]
  [:span#unscrambled (map letter-box unscrambled)]
  [:span#scrambled (map letter-box scrambled)])

(deftemplate round-template []
  [:#round
   [:#word]
   [:#hud
    [:span#time [:b [:span.count "60"] "s"] " left"]
    [:span.spacer "|"]
    [:span#points [:b [:span.count "0"]] " points"]
    [:span.spacer "|"]
    [:span#mult [:b [:span.count "1"] "x"] " multiplier"]]])

(defn replay-unscramble [unscrambled scrambled-word]
  (loop [unscrambled unscrambled
         scrambled (seq scrambled-word)]
    (if-let [letter (first unscrambled)]
      (recur (rest unscrambled) (remove-first #(= % letter) scrambled))
      scrambled)))

(defn timer-chan [seconds]
  (let [c (chan)]
    (go
     (loop [seconds seconds]
       (<! (timeout 1000))
       (let [seconds (dec seconds)]
         (put! c seconds)
         (if (neg? seconds)
           (close! c)
           (recur seconds)))))
    c))

(defn typing-word [scrambled-word unscrambled scrambled done]
  (let [c (chan)]
    (go
     (let [unscrambled unscrambled
           scrambled scrambled
           keypress (->> (listen js/window :keypress)
                         (map< #(.-keyCode %)))
           backspace (->> (listen js/window :keydown #(= (.-keyCode %) 8))
                          (map< #(.-keyCode %))
                          (filter< #(= % 8)))]

       (js/console.log scrambled-word (str unscrambled) (str scrambled))
       (doto (sel1 :#word)
         (dommy/remove-class! :success :error)
         (dommy/replace-contents! (word-text unscrambled scrambled)))

       (loop [unscrambled unscrambled
              scrambled scrambled
              [key-pressed channel] (alts! [keypress backspace done])]

         (cond

          (= channel done)
          (do (close! keypress)
              (close! backspace)
              (close! c))

          (= channel backspace)
          (let [unscrambled (butlast unscrambled)
                scrambled (replay-unscramble unscrambled scrambled-word)]
            (dommy/replace-contents! (sel1 :#word) (word-text unscrambled scrambled))
            (recur unscrambled scrambled (alts! [keypress backspace done])))

          (= channel keypress)
          (let [letter-pressed (js/String.fromCharCode key-pressed)]
            (cond
             ((set scrambled) letter-pressed)
             (let [unscrambled (concat unscrambled (seq letter-pressed))
                   scrambled (remove-first #(= % letter-pressed) scrambled)]
               (dommy/replace-contents! (sel1 :#word) (word-text unscrambled scrambled))
               (if (seq scrambled)
                 (recur unscrambled scrambled (alts! [keypress backspace done]))
                 (do
                   (put! c unscrambled)
                   (close! keypress)
                   (close! backspace)
                   (close! c))))

             :else (recur unscrambled scrambled (alts! [keypress backspace done]))
             ))

          :else (recur unscrambled scrambled (alts! [keypress backspace done])))

         )))
    c))

(defn guessing-word [scrambled-word done]
  (let [c (chan)
        space (->> (listen js/window :keydown)
                   (map< #(.-keyCode %))
                   (filter< #(= % 32)))]
    (go
     (loop [[v channel] (alts! [(typing-word scrambled-word [] (seq scrambled-word) done)
                                space done])]
       (cond
        (= channel done)
        (close! c)

        (= channel space)
        (do (put! c 0) (close! c))

        :else
        (if-let [points (:points (<! (request "/word/check" {:word (apply str v)})))]
          (do
            (dommy/add-class! (sel1 :#word) :success)
            (<! (timeout 500))
            (put! c points)
            (close! c))
          (do
            (dommy/add-class! (sel1 :#word) :error)
            (<! (timeout 500))
            (recur (alts! [(typing-word scrambled-word [] (seq scrambled-word) done)
                           space done])))))))
    c))

(defn word-points-chan [done]
  (let [c (chan)]
    (go
     (loop [[v channel] (alts! [done (request "/word/scrambled")])]
       (cond
        (= channel done)
        (close! c)

        :else
        (if-let [points (<! (guessing-word (:scrambled-word v) done))]
          (do (put! c points)
              (recur (alts! [done (request "/word/scrambled")])))
          (close! c)))))
    c))

(defn round []
  (let [round-el (round-template)
        timer (timer-chan 60)
        done (chan)
        word-points (word-points-chan done)
        c (chan)]
    (dommy/replace! (sel1 :#round) round-el)
    (go
     (loop [[v channel] (alts! [timer word-points])
            total-points 0
            multiplier 1]
       (cond
        (= channel timer)
        (if (neg? v)
          (do
            (put! c total-points)
            (put! done true)
            (close! done)
            (close! c))
          (do
            (dommy/set-text! (sel1 [:#time :.count]) v)
            (recur (alts! [timer word-points]) total-points multiplier)))

        (= channel word-points)
        (let [total-points (if v (+ total-points (* multiplier v)) total-points)
              multiplier (if (zero? v) 1 (inc multiplier))]
          (dommy/set-text! (sel1 [:#points :.count]) total-points)
          (dommy/set-text! (sel1 [:#mult :.count]) multiplier)
          (recur (alts! [timer word-points]) total-points multiplier)))))
    c))

(deftemplate score-modal [points]
  [:#overlay
   [:#score-modal.modal
    [:h3 (str "You got " points " points!")]
    [:button#play-again "Play Again"]]])

(defn ^:export init []
  (go
   (loop [round-points (<! (round))]
     (let [score-el (score-modal round-points)]
       (dommy/append! js/document.body score-el)
       (<! (listen (sel1 :#play-again) :click (constantly true)))
       (dommy/remove! score-el)
       (recur (<! (round)))))))
