(ns om-wp.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require
    [sablono.core :as sabl :refer-macros [html]]
    [cljs.core.async :as async :refer [<! put! chan]]
    [cljs.core.match :refer-macros [match]]
    [om.next :as om :refer-macros [defui]]
    [om.dom :as dom :include-macros true]
    [clojure.string :as string]))

(enable-console-print!)

(defui App
  static om/IQuery
  (query [_] [:hello/test-string])
  Object
  (render [this]
    (let [{:keys [hello/test-string]} (om/props this)]
      (html [:div (str "Prop is: " test-string)]))))

(defn parse [env key params]
  (let [state (:state env)
        st @state
        ast (:ast env)]
        (println)(println)
        (println "Parsing: " key " " params)
        (println "AST type: " (:type ast))
    (let [resp
      (match [key params]
        [:hello/test-string _] {:value "It works"}
        :else {:value "Missing"})]
        (println "resp: " (pr-str resp))
        resp)))

(def parser (om/parser {:read parse :mutate parse}))

(om/add-root! (om/reconciler {:parser parser :state {}}) App js/app)
