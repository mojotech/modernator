(ns glug.cs
  (:require [om.core :as om]
            [om.dom :as dom]
            [clojure.walk :refer [keywordize-keys]]
            [ajax.core :refer [GET POST]]))

(enable-console-print!)

(defn sort-by-votes
  [beer-map]
  (into [] (sort-by :votes > beer-map)))

(defonce app-state (atom {:beer-list []}))

(defn upvote-beer!
  [e beer]
  (om/update!
    beer
    (merge beer
           {:upvoted (not (:upvoted beer))
            :votes (if (:upvoted beer)
                     (dec (:votes beer))
                     (inc (:votes beer)))})))

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
    om/IWillMount
    (will-mount [_]
      (GET "beers"
           {:handler #(om/update!
                        data
                        (assoc data :beer-list (mapv
                                                 (fn [beer] (assoc
                                                              beer
                                                              :votes 6
                                                              :upvoted false))
                                                 (keywordize-keys %))))}))
    om/IRender
    (render [_]
      (dom/div nil
        (dom/h1 nil "Beer List")
        (apply dom/ul #js {:className "beer-list"}
          (om/build-all beer-view (sort-by-votes (:beer-list data))))))))

(om/root beer-list-view app-state
  {:target (. js/document (getElementById "app"))})
