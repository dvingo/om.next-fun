(ns om-wp.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require
    [sablono.core :as sabl :refer-macros [html]]
    [cljs.core.async :refer [<! put! chan]]
    [cljs.core.match :refer-macros [match]]
    [om.next :as om :refer-macros [defui]]
    [om.dom :as dom :include-macros true]
    [clojure.string :as string]))

(enable-console-print!)

(defui AutoCompleter
  static om/IQuery
  (query [_]
    [:search/query])
  Object
  (render [this]
    (let [{:keys [search/query]} (om/props this)]
      (html
        [:div [:h2 "Autocompleter"]
          [:input {
            :value query
            :on-change
              #(om/transact! this
                 `[(search/do-search {:query ~(.. % -target -value)})])}]]))))

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
        ['search/do-search {:query q}]
          {:action #(swap! state assoc :search/query q)}
        :else {:value "Missing"})]
        (println "resp: " (pr-str resp))
        resp)))

(def parser (om/parser {:read parse :mutate parse}))
(def reconciler (om/reconciler {:parser parser :state {}}))
(om/add-root! reconciler AutoCompleter js/app)
