(defproject tech.ardour/logging "0.0.2"
  :description "Ardour Tech Logging Tool for Clojure(Script)"
  :url "https://github.com/ArdourTech/logging"
  :license {:name         "Eclipse Public License - v 1.0"
            :url          "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments     "same as Clojure"}
  :dependencies [[com.taoensso/timbre "5.1.0"]
                 [tech.ardour/utensil "0.1.0"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.10.3"]
                                  [org.clojure/clojurescript "1.10.741"]]}}
  :source-paths ["src"]
  :test-paths ["test"]
  :deploy-repositories [["clojars" {:url           "https://clojars.org/repo"
                                    :sign-releases false}]])
