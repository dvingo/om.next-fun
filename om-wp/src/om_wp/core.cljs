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

(defn wp-preview-uri [page-title]
  (str base-uri
    (string/join "&"
      ["action=query" "prop=extracts" "exchars=1000" "format=json"
        (str "titles=" page-title)])))

(defn jsonp
  ([uri] (jsonp (chan) uri))
  ([c uri]
   (let [gjsonp (Jsonp. (Uri. uri))]
     (.send gjsonp nil #(put! c %))
     c))
  ([c uri & args]
   (let [gjsonp (Jsonp. (Uri. uri))]
     (.send gjsonp nil #(put! c (conj args %)))
     c)))

(defui Preview
  static om/Ident
  (ident [this {:keys [name]}]
    [:search/pages-by-name name])
  static om/IQuery
  (query [this]
    [:preview :name])
  Object
  (render [this]
    (let [{:keys [preview name]} (om/props this)
         style
         #js {:maxWidth     "400px"
              :boxShadow    "2px 2px 6px rgba(0, 0, 0, .3)"
              :padding      "1rem"
              :borderRadius ".375rem"
              :margin       "1rem 0"
              :color        "dimGrey"
              :background   "floralWhite"}]
     (html [:div {:style style}
             [:h3 name]
             [:div {:dangerouslySetInnerHTML {:__html preview}}]]))))

(def wp-preview (om/factory Preview {:keyfn :name}))

(defn result-list [results]
  (html [:ul (for [r results] [:li {:key r} r])]))

(defui AutoCompleter
  static om/IQuery
  (query [_]
    [:search/query :search/results :search/preview-list])
  Object
  (render [this]
    (let [{:keys [search/query search/results search/preview-list]} (om/props this)]
      (html
        [:div [:h2 "Autocompleter"]
          [:input {
            :value query
            :on-change
              #(om/transact! this
                 `[(search/do-search {:query ~(.. % -target -value)})])}]
         (result-list results)
         [:div
          (map wp-preview preview-list)]]))))

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
        [:search/preview-list _]
          (when-let [search-results (:search/results st)]
            {:value
              (into [] (comp (map #(get-in st [:search/pages-by-name %]))
                             (filter some?)) search-results)})
        ['search/do-search {:query q}]
          (merge {:action #(swap! state assoc :search/query q)}
                 (when-not (or (string/blank? q) (< (count q) 3))
                   (println "adding :search key")
                   {:search ast}))
        :else {:value "Missing"})]
        ;;(println "resp: " (pr-str resp))
        resp)))

(declare reconciler)

(defn get-page-loop [c]
  (go-loop [[page title cb] (<! c)]
    (let [id (-> (aget page "query" "pages")
                 js/Object.keys first)
          preview (aget page "query" "pages" id "extract")
          preview (.substring preview 0 (- (.-length preview) 3))]
      (cb {:search/pages-by-name {title {:preview preview :name title}}})
      (recur (<! c)))))

(defn search-loop [c get-page-chan]
  (go-loop [[query cb] (<! c)]
    (let [[_ results] (<! (jsonp (search-uri query)))]
      (doseq [title results]
        (jsonp get-page-chan (wp-preview-uri title) title cb))
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
(def get-page-chan (chan))

(def parser (om/parser {:read parse :mutate parse}))

;; Copied from:
;; https://github.com/circleci/frontend/blob/e0ed3a1586337fcc4e25bbd2aff6679cb368ec8a/src-cljs/frontend/utils.cljs#L239
(defn deep-merge* [& maps]
  (let [f (fn [old new]
            (if (and (map? old) (map? new))
              (merge-with deep-merge* old new)
              new))]
    (if (every? map? maps)
      (apply merge-with f maps)
      (last maps))))

(defn deep-merge
  [& maps]
  (let [maps (filter identity maps)]
    (assert (every? map? maps))
    (apply merge-with deep-merge* maps)))

(def reconciler (om/reconciler
                 {:parser parser
                  :state {}
                  :merge-tree deep-merge
                  :send (send-to-chan send-chan)
                  :remotes [:search]}))

(search-loop send-chan get-page-chan)
(get-page-loop get-page-chan)

(om/add-root! reconciler AutoCompleter js/app)
