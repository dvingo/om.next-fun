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
  Object
  (render [this]
    (html [:div "First component"])))

(om/add-root! (om/reconciler {}) App js/app)
