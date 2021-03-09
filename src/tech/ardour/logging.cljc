(ns tech.ardour.logging
  #?(:cljs (:require-macros [tech.ardour.logging]))
  (:require
    [taoensso.timbre :as timbre]
    [tech.ardour.utensil :as u]))

(defn- current-time-ms []
  #?(:cljs (js/Date.now)
     :clj  (System/currentTimeMillis)))

(defonce ^:private init-time (current-time-ms))

(defn- uptime [inst]
  (- (inst-ms inst) init-time))

(def ^:dynamic *context* nil)

#?(:clj (defmacro merge-context [m & body]
          `(binding [*context* (merge *context* ~m)]
             ~@body)))

#?(:cljs (defn Error->map [o]
           (let [base (fn [t]
                        (merge {:type (cond
                                        (instance? ExceptionInfo t) 'ExceptionInfo
                                        (instance? js/Error t) (symbol "js" (.-name t))
                                        :else nil)}
                          (when-let [msg (ex-message t)]
                            {:message msg})
                          (when-let [ed (ex-data t)]
                            {:data ed})))
                 via (loop [via [], t o]
                       (if t
                         (recur (conj via t) (ex-cause t))
                         via))
                 root (peek via)]
             (merge {:via   (vec (map base via))
                     :trace nil}
               (when-let [root-msg (ex-message root)]
                 {:cause root-msg})
               (when-let [data (ex-data root)]
                 {:data data})
               (when-let [phase (-> o ex-data :clojure.error/phase)]
                 {:phase phase})))))

(defn edn-appender []
  (let [println-appender (timbre/println-appender)
        fallback-logger (:fn println-appender)]
    {:enabled?  true
     :async?    false
     :min-level nil
     :fn        (fn [{:keys [instant level ?ns-str ?file ?line ?err vargs] :as data}]
                  (let [[msg & args] vargs
                        context (first args)
                        arg-count (count args)
                        log-map (cond-> {:msg   msg
                                         :time  (uptime instant)
                                         :level level
                                         :unix  (inst-ms instant)}
                                  true (u/assoc-some
                                         :ns (when ?ns-str (str ?ns-str (when ?line (str ":" ?line))))
                                         :thread #?(:clj (.getName (Thread/currentThread)) :cljs nil)
                                         :context *context*
                                         :args (cond
                                                 (and (= 1 arg-count)
                                                      (map? context))
                                                 context

                                                 (and (pos? arg-count)
                                                      (even? arg-count))
                                                 (apply hash-map args)

                                                 (and (pos? arg-count)
                                                      (seq args))
                                                 args))
                                  ?err (u/assoc-some
                                         :err (#?(:clj Throwable->map :cljs Error->map) ?err)
                                         :file ?file
                                         :line ?line))]
                    (try
                      (let [output (pr-str log-map)]
                        #?(:clj  (println output)
                           :cljs (case level
                                   :error (js/console.error output)
                                   :warn (js/console.warn output)
                                   :debug (js/console.debug output)
                                   (js/console.info output))))

                      (catch #?(:clj Throwable :cljs :default) _
                        (fallback-logger data)))))}))

(defn bootstrap
  ([] (bootstrap {:level :info}))
  ([{:keys [level]
     :or   {level :info}}]
   (timbre/set-config! {:level     level
                        :appenders {:edn (edn-appender)}})))

(defmacro log [level & args]
  {:pre [(keyword? level)
         (let [l (last args)]
           (or (string? l)
               (map? l)
               (list? l)
               (symbol? l)))]}
  `(timbre/log ~level ~@args))

(defmacro debug [& args] `(log :debug ~@args))
(defmacro info [& args] `(log :info ~@args))
(defmacro warn [& args] `(log :warn ~@args))
(defmacro error [& args] `(log :error ~@args))

(comment
  (do
    (bootstrap)
    (info "test" {:and "again"})
    (info "other")
    (info (Exception. "help") "this")
    (info (Exception. "help") "this" {:again true})))
