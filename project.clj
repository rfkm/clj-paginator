(defproject clj-paginator "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [prismatic/plumbing "0.3.3"]
                 [ring/ring-codec "1.0.0"]]
  :profiles {:dev {:dependencies [[com.h2database/h2 "1.4.181"]
                                  [enlive "1.1.5"]
                                  [hiccup "1.0.5"]
                                  [korma "0.4.0"]
                                  [midje "1.6.3"]]}})
