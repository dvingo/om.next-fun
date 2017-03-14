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
      (html
        [:div
          [:p (str "Prop is: " test-string)]
          [:input {:on-change
            #(om/transact! this
              `[(hello/set-string
                {:new-str ~(.. % -target -value)})])}]]))))

(defn parse [env key params]
  (let [state (:state env)
        st @state
        ast (:ast env)]
        (println)(println)
        (println "Parsing: " key " " params)
        (println "AST type: " (:type ast))
    (let [resp
      (match [key params]
        [:hello/test-string _] {:value (:hello/test-string st "")}
        ['hello/set-string {:new-str new-str}]
          {:action #(swap! state assoc :hello/test-string new-str)}
        :else {:value "Missing"})]
        (println "resp: " (pr-str resp))
        resp)))

(def parser (om/parser {:read parse :mutate parse}))
(def reconciler (om/reconciler {:parser parser :state {}}))
(om/add-root! reconciler App js/app)
