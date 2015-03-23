(ns glug.cs
  (:require [om.core :as om]
            [om.dom :as dom]))

(def ^:private meths
  {:get "GET"
   :put "PUT"
   :post "POST"
   :delete "DELETE"})

(defn edn-xhr [{:keys [method url data on-complete]}]
  (let [xhr (XhrIo.)]
    (events/listen xhr goog.net.EventType.COMPLETE
      (fn [e]
        (on-complete (reader/read-string (.getResponseText xhr)))))
    (. xhr
      (send url (meths method) (when data (pr-str data))
        #js {"Content-Type" "application/edn"}))))

(defn sort-by-votes
  [beer-map]
  (into [] (sort-by :votes > beer-map)))

(defonce app-state (atom {:beer-list
  [{:name "Sam Adams Winter" :votes 6 :upvoted false}
   {:name "Peche Mortel" :votes 3 :upvoted false}
   {:name "PBR" :votes 0 :upvoted false}]}))

(defn upvote-beer!
  [e beer]
  (om/update!
    beer
    (merge beer
           {:upvoted (not (:upvoted beer))
            :votes (if (:upvoted beer)
                     (dec (:votes beer))
                     (inc (:votes beer)))})))

(defn position [is-needle? haystack]
  (loop [i 0]
    (if (is-needle? (get haystack i))
      (+ i 1)
      (recur (+ i 1)))))

(defn placement
  [beer]
  (let [sorted-beers (sort-by-votes (:beer-list @app-state))
        beer-position (position #(= (:name beer) (:name %)) sorted-beers)]
    (* 16 beer-position)))

(defn beer-view
  [beer owner]
  (reify
    om/IRender
    (render [_]
      (dom/li nil
        (:name beer)
        " (" (:votes beer) ")"
        (dom/button #js {:onClick #(upvote-beer! % beer)} "Vote")))))

(defn beer-list-view
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (dom/h1 nil "Beer List")
        (apply dom/ul #js {:className "beer-list"}
          (om/build-all beer-view (sort-by-votes (:beer-list data))))))))

(om/root beer-list-view app-state
  {:target (. js/document (getElementById "app"))})
