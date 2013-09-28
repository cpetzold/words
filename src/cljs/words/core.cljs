(ns words.core
  (:require-macros
   [cljs.core.async.macros :refer [go]]
   [dommy.macros :refer [deftemplate sel1]])
  (:require
   [clojure.string :as string]
   [cljs.core.async :as async :refer [chan put! <! >! close!]]
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

(defn listen [node type & [prevent?]]
  (let [c (chan)]
    (dommy/listen!
     node type
     (fn [e]
       (when (and prevent?
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

(defn replay-unscramble [unscrambled scrambled]
  [unscrambled scrambled])

(deftemplate word-text [unscrambled scrambled]
  [:span#unscrambled (apply str unscrambled)]
  [:span#scrambled (apply str scrambled)])

(defn ^:export init []
  (go
   (let [word (scramble "words")
         unscrambled []
         scrambled (seq word)
         word-el (sel1 :#word)
         keypress (->> (listen js/window :keypress true)
                       (map< #(.-keyCode %)))
         #_backspace #_(->> (listen js/window :keydown true)
                            (map< #(.-keyCode %))
                            (filter< #(= % 8)))]

     (dommy/replace-contents! word-el (word-text unscrambled scrambled))

     (loop [unscrambled unscrambled
            scrambled scrambled
            key-pressed (<! keypress)]

       (let [letter-pressed (js/String.fromCharCode key-pressed)]
         (js/console.log key-pressed letter-pressed)
         (cond
          ((set scrambled) letter-pressed)
          (let [unscrambled (conj unscrambled letter-pressed)
                scrambled (remove-first #(= % letter-pressed) scrambled)]
            (dommy/replace-contents! word-el (word-text unscrambled scrambled))
            (recur unscrambled scrambled (<! keypress)))

          :else (recur unscrambled scrambled (<! keypress))
          ))))))