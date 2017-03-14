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

(defn norm [v min max] (/ v (- max min)))
(defn lerp [t min max] (+ (* t (- max min)) min))
(defn across [v min1 max1 min2 max2]
  (lerp (norm v min1 max1) min2 max2))

(defui Mover
  Object
  (initLocalState [_] {:x 0})
  (componentDidUpdate [this _ _]
    (om/update-state! this update :x inc))
  (componentDidMount [this]
    (om/update-state! this update :x inc))
  (render [this]
    (let [x (om/get-state this :x)]
      (html
        [:div {:style #js
          {:position "absolute"
           :transform (str "translateX(" x "px)")
           :width "400px" :height "400px"
           :background "yellow"}} "HI"]))))
(def mover (om/factory Mover))

(defui Sizer
  Object
  (initLocalState [_] {:size 300})
  (onMouse [this e]
    (om/update-state! this update :size #(across (.-clientY e) 0 200 200 500)))
  (componentDidMount [this]
    (js/addEventListener "mousemove" #(.onMouse this %)))
  (render [this]
    (let [size (om/get-state this :size)]
      (html
        [:div {:style #js
          {:position "absolute"
           :width size :height size
           :background "orange"}} "HI"]))))
(def sizer (om/factory Sizer))

(defui App
  static om/IQuery
  (query [_] [:hello/test-string])
  Object
  (render [this]
    (let [{:keys [hello/test-string]} (om/props this)]
      (html
        [:div
          ;; (sizer)
          ;; (mover)
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
