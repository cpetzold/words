(defproject words "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-1909"]
                 [org.clojure/data.json "0.2.3"]
                 [org.clojure/core.async "0.1.242.0-44b1e3-alpha"]
                 [compojure "1.1.5"]
                 [lib-noir "0.6.6"]
                 [garden "0.1.0-beta6"]
                 [digest "1.4.3"]
                 [clj-wordnik "0.1.0-alpha1"]
                 [prismatic/dommy "0.1.1"]]
  :plugins [[lein-ring "0.8.6"]
            [lein-cljsbuild "0.3.3"]]
  :source-paths ["src/clj"]
  :ring {:handler words.server/handler}
  :cljsbuild
  {:builds
   [{:source-paths ["src/cljs"]
     :compiler
     {:pretty-print true
      :output-to "resources/public/words.js"
      ;;:source-map "resources/public/words.js.map"
      }}]})
