(ns glug.cs
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.string :as string]
            [clojure.walk :refer [keywordize-keys]]
            [ajax.core :refer [GET POST PUT]]))

(enable-console-print!)

(defn sort-by-votes
  [item-map]
  (into [] (sort-by :votes > item-map)))

(defonce app-state (atom {:item-list []}))

(defn upvote-item!
  [e item]
  (om/update!
    item
    (merge item
           {:upvoted (not (:upvoted item))
            :votes (if (:upvoted item)
                     (dec (:votes item))
                     (inc (:votes item)))}))
  (PUT (str "votes/" (:id item)) {:error-handler #(om/update! item item)}))

(defn typeahead-item
  [item]
  (om/component
    (dom/li #js {:style #js {:fontWeight (if (:selected item) "bold" "normal")}}
            (:title item))))

(defn handle-change [e owner state data]
  (let [value (.. e -target -value)]
    (om/set-state! owner :new-item value)
    (om/set-state! owner :filtered (filterv #(zero? (.indexOf (:title %) value)) data))))

(defn new-selected
  [direction currently-selected total-items]
  (if (= direction "up")
    (max 0 (dec currently-selected))
    (min (dec total-items)
         (inc currently-selected))))

(defn handle-typeahead-focus
  [direction owner currently-selected total-items]
  (om/set-state! owner :selected (new-selected direction currently-selected total-items)))

(defn sync-list! [items]
  (GET "items"
       {:handler #(om/update! items (keywordize-keys %))}))

(defn add-to-list!
  [item items]
  (PUT "items"
       {:format :json
        :params {:title item}
        :handler #(sync-list! items)}))

(defn modernator-input
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:new-item ""
       :selected nil
       :filtered data})
    om/IRenderState
    (render-state [_ state]
      (dom/div nil
        (dom/input #js {:type "text" :ref "new-item"
                        :onChange #(handle-change % owner state data)
                        :onKeyDown #(let [currently-selected (or (:selected state) -1)
                                          typeahead-results (:filtered state)
                                          total-items (count typeahead-results)]
                                      (case (.-key %)
                                        "ArrowUp" (handle-typeahead-focus "up" owner currently-selected total-items)
                                        "ArrowDown" (handle-typeahead-focus "down" owner currently-selected total-items)
                                        "Enter" (add-to-list! (:new-item state) data)
                                        (om/set-state! owner :selected nil)))
                        :value (let [currently-selected (:selected state)]
                                 (if-not (nil? currently-selected)
                                   (get-in (:filtered state) [currently-selected :title])
                                   (:new-item state)))})
        (when (not (string/blank? (:new-item state)))
          (apply dom/ul #js {:className "typeahead-list"}
                 (om/build-all typeahead-item (map-indexed #(let [selected-index (:selected state)
                                                                  item %2
                                                                  index %1]
                                                              (assoc item :selected (= index selected-index)))
                                                           (:filtered state)))))))))

(defn modernator-item
  [data owner]
  (om/component
    (dom/li nil
      (:title data)
      " (" (.toString (:votes data)) ")"
      (dom/button #js {:onClick #(upvote-item! % data)} "Vote"))))

(defn modernator
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (sync-list! (:item-list data)))
    om/IRender
    (render [_]
      (dom/div nil
        (dom/h1 nil "List")

        (om/build modernator-input (:item-list data))

        (apply dom/ul #js {:className "item-list"}
          (om/build-all modernator-item (sort-by-votes (:item-list data))))))))

(om/root modernator app-state
  {:target (. js/document (getElementById "app"))})
