(ns om-wp.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require
    [sablono.core :as sabl :refer-macros [html]]
    [cljs.core.async :refer [<! put! chan]]
    [cljs.core.match :refer-macros [match]]
    [om.next :as om :refer-macros [defui]]
    [om.dom :as dom :include-macros true]
    [clojure.string :as string])
  (:import [goog Uri] [goog.net Jsonp]))

(enable-console-print!)

(def base-uri "https://en.wikipedia.org/w/api.php?")
(defn search-uri [query]
  (str base-uri
    (string/join "&" ["action=opensearch" "origin=*" (str "search=" query)])))

(defn jsonp
  ([uri] (jsonp (chan) uri))
  ([c uri]
   (let [gjsonp (Jsonp. (Uri. uri))]
     (.send gjsonp nil #(put! c %))
     c)))

(defn result-list [results]
  (html [:ul (for [r results] [:li {:key r} r])]))

(defui AutoCompleter
  static om/IQuery
  (query [_]
    [:search/query :search/results])
  Object
  (render [this]
    (let [{:keys [search/query search/results]} (om/props this)]
      (html
        [:div [:h2 "Autocompleter"]
          [:input {
            :value query
            :on-change
              #(om/transact! this
                 `[(search/do-search {:query ~(.. % -target -value)})])}]
         (result-list results)]))))

(def auto-completer (om/factory AutoCompleter))

(defn parse [env key params]
  (let [state (:state env)
        st @state
        ast (:ast env)]
        (println)(println)
        (println "Parsing: " key " " params)
        (println "AST type: " (:type ast))
    (let [resp
      (match [key params]
        [:search/query _] {:value (:search/query st "")}
        [:search/results _] {:value (:search/results st [])}
        ['search/do-search {:query q}]
             (merge {:action #(swap! state assoc :search/query q)}
                    (when-not (or (string/blank? q) (< (count q) 3))
                      (println "adding :search key")
                      {:search ast}))
        :else {:value "Missing"})]
        (println "resp: " (pr-str resp))
        resp)))

(declare reconciler)

(defn search-loop [c]
  (go-loop [[query cb] (<! c)]
    (let [[_ results] (<! (jsonp (search-uri query)))]
      (cb {:search/results results}))
    (recur (<! c))))

(defn send-to-chan [c]
  (fn [{:keys [search] :as args} cb]
    (println "Send args: " args)
    (when search
      ;; search has this shape: [(search/do-search {:query "text here"})]
      (println "send-to-chan search: " search)
      (let [[[_ {query :query}]] search]
        (println "query: " query)
        (put! c [query cb])))))

(def send-chan (chan))
(def parser (om/parser {:read parse :mutate parse}))

(def reconciler (om/reconciler
                 {:parser parser
                  :state {}
                  :send (send-to-chan send-chan)
                  :remotes [:search]}))
(search-loop send-chan)
(om/add-root! reconciler AutoCompleter js/app)
