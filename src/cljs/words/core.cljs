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

(def +word-el+ (sel1 :#word))

(defn replay-unscramble [unscrambled scrambled-word]
  (loop [unscrambled unscrambled
         scrambled (seq scrambled-word)]
    (if-let [letter (first unscrambled)]
      (recur (rest unscrambled) (remove-first #(= % letter) scrambled))
      scrambled)))

(defn typing-word [scrambled-word unscrambled scrambled]
  (let [c (chan)]
    (go
     (let [unscrambled unscrambled
           scrambled scrambled
           keypress (->> (listen js/window :keypress)
                         (map< #(.-keyCode %)))
           backspace (->> (listen js/window :keydown #(= (.-keyCode %) 8))
                          (map< #(.-keyCode %))
                          (filter< #(= % 8)))]

       (dommy/replace-contents! +word-el+ (word-text unscrambled scrambled))

       (loop [unscrambled unscrambled
              scrambled scrambled
              [key-pressed key-chan] (alts! [keypress backspace])]

         (cond

          (= key-chan backspace)
          (let [unscrambled (butlast unscrambled)
                scrambled (replay-unscramble unscrambled scrambled-word)]
            (dommy/replace-contents! +word-el+ (word-text unscrambled scrambled))
            (recur unscrambled scrambled (alts! [keypress backspace])))

          (= key-chan keypress)
          (let [letter-pressed (js/String.fromCharCode key-pressed)]
            (cond
             ((set scrambled) letter-pressed)
             (let [unscrambled (concat unscrambled (seq letter-pressed))
                   scrambled (remove-first #(= % letter-pressed) scrambled)]
               (dommy/replace-contents! +word-el+ (word-text unscrambled scrambled))
               (if (seq scrambled)
                 (recur unscrambled scrambled (alts! [keypress backspace]))
                 (do
                   (put! c unscrambled)
                   (close! c))))

             :else (recur unscrambled scrambled (alts! [keypress backspace]))
             ))

          :else (recur unscrambled scrambled (alts! [keypress backspace])))

         )))
    c))

(defn guessing-word [scrambled-word]
  (let [c (chan)]
    (go
     (loop [unscrambled (<! (typing-word scrambled-word [] (seq scrambled-word)))]
       (if-let [points (:points (<! (request "/word/check" {:word (apply str unscrambled)})))]
         (do (put! c points) (close! c))
         (recur (<! (typing-word scrambled-word unscrambled []))))))
    c))

(defn timer-chan [seconds]
  (let [c (chan)]
    (go
     (loop [seconds seconds]
       (<! (timeout 1000))
       (let [seconds (dec seconds)]
         (put! c seconds)
         (if (zero? seconds)
           (close! c)
           (recur seconds)))))
    c))

(defn word-points-chan []
  (let [c (chan)]
    (go
     (loop [{:keys [word scrambled-word]} (<! (request "/word/scrambled"))]
       (js/console.log word)
       (let [points (<! (guessing-word scrambled-word))]
         (put! c points)
         (recur (<! (request "/word/scrambled"))))))
    c))

(defn round []
  (let [timer (timer-chan 60)
        word-points (word-points-chan)]
    (go
     (loop [[v channel] (alts! [timer word-points])
            total-points 0
            multiplier 1]
       (cond
        (= channel timer)
        (do
          (set! (.-title js/document) v)
          (recur (alts! [timer word-points]) total-points multiplier))

        (= channel word-points)
        (let [total-points (if v (+ total-points (* multiplier v)) total-points)
              multiplier (if v (inc multiplier) multiplier)]
          (js/console.log "total points:" total-points "multiplier:" multiplier)
          (recur (alts! [timer word-points]) total-points multiplier)))))))

(defn ^:export init []
  (round))
