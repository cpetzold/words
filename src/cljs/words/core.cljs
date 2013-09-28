(ns words.core
  (:use-macros
   [dommy.macros :only [deftemplate]])
  (:require
   [dommy.core :as dommy]))

(defn ^:export init []
  (js/console.log "pwned"))