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

(defn replay-unscramble [unscrambled scrambled-word]
  (loop [unscrambled unscrambled
         scrambled (seq scrambled-word)]
    (if-let [letter (first unscrambled)]
      (recur (rest unscrambled) (remove-first #(= % letter) scrambled))
      scrambled)))

(deftemplate letter-box [c]
  [:span.letter c])

(deftemplate word-text [unscrambled scrambled]
  [:span#unscrambled (map letter-box unscrambled)]
  [:span#scrambled (map letter-box scrambled)])

(defn typing-word [scrambled-word]
  (let [c (chan)]
    (go
     (let [unscrambled []
           scrambled (seq scrambled-word)
           word-el (sel1 :#word)
           keypress (->> (listen js/window :keypress)
                         (map< #(.-keyCode %)))
           backspace (->> (listen js/window :keydown #(= (.-keyCode %) 8))
                          (map< #(.-keyCode %))
                          (filter< #(= % 8)))]

       (dommy/replace-contents! word-el (word-text unscrambled scrambled))

       (loop [unscrambled unscrambled
              scrambled scrambled
              [key-pressed key-chan] (alts! [keypress backspace])]

         (cond

          (= key-chan backspace)
          (let [unscrambled (butlast unscrambled)
                scrambled (replay-unscramble unscrambled scrambled-word)]
            (dommy/replace-contents! word-el (word-text unscrambled scrambled))
            (recur unscrambled scrambled (alts! [keypress backspace])))

          (= key-chan keypress)
          (let [letter-pressed (js/String.fromCharCode key-pressed)]
            (cond
             ((set scrambled) letter-pressed)
             (let [unscrambled (concat unscrambled (seq letter-pressed))
                   scrambled (remove-first #(= % letter-pressed) scrambled)]
               (dommy/replace-contents! word-el (word-text unscrambled scrambled))
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

(defn ^:export init []
  (go
   (loop [{:keys [word scrambled-word]} (<! (request "/word/scrambled"))]
     (js/console.log word)
     (let [guess (apply str (<! (typing-word scrambled-word)))
           points (:points (<! (request "/word/check" {:word guess})))]
       (js/console.log points)
       (recur (<! (request "/word/scrambled")))))))