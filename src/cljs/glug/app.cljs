(ns glug.cs
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.string :as string]
            [clojure.walk :refer [keywordize-keys]]
            [ajax.core :refer [GET POST PUT]]))

(enable-console-print!)

(defonce app-state (atom {:item-list []}))

(defn sort-by-votes
  [item-map]
  (into [] (sort-by :votes > item-map)))

(defn new-selected
  [direction currently-selected total-items]
  (if (= direction "up")
    (max 0 (dec currently-selected))
    (min (dec total-items)
         (inc currently-selected))))

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

(defn handle-change! [e owner state data]
  (let [value (.. e -target -value)]
    (om/set-state! owner :new-item value)
    (om/set-state! owner :filtered (filterv #(zero? (.indexOf (:title %) value)) data))))

(defn handle-typeahead-focus!
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

(defn typeahead-item
  [item]
  (om/component
    (dom/li #js {:className (when (:selected item) "active")}
            (:title item))))

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
                        :className "input input-search"
                        :placeholder "Enter a Suggestion"
                        :onChange #(handle-change! % owner state data)
                        :onBlur #(om/set-state! owner :new-item "")
                        :onKeyDown #(let [currently-selected (or (:selected state) -1)
                                          typeahead-results (:filtered state)
                                          total-items (count typeahead-results)]
                                      (case (.-key %)
                                        "ArrowUp" (handle-typeahead-focus! "up" owner currently-selected total-items)
                                        "ArrowDown" (handle-typeahead-focus! "down" owner currently-selected total-items)
                                        "Enter" (when-not (empty? (:new-item state))
                                                  (add-to-list!
                                                    (if-not (= currently-selected -1)
                                                      (get-in (:filtered state) [currently-selected :title])
                                                      (:new-item state))
                                                    data)
                                                  (om/set-state! owner :new-item ""))
                                        (om/set-state! owner :selected nil)))
                        :value (let [currently-selected (:selected state)]
                                 (if-not (nil? currently-selected)
                                   (get-in (:filtered state) [currently-selected :title])
                                   (:new-item state)))})
        (when-not (string/blank? (:new-item state))
          (let [typeahead-items (map-indexed #(let [selected-index (:selected state)
                                                    item %2
                                                    index %1]
                                                (assoc item :selected (= index selected-index)))
                                             (:filtered state))]
            (when-not (empty? typeahead-items)
              (apply dom/ul #js {:className "typeahead-list"}
                     (om/build-all typeahead-item typeahead-items)))))))))

(defn gravatar
  [data owner]
  (om/component
    (dom/img #js {:className "avatar-image"
                  :src (str "http://www.gravatar.com/avatar/" data "?d=retro")})))

(defn modernator-item
  [data owner]
  (om/component
    (dom/li #js {:className "list-item"}
      (dom/div #js {:className "left"}
        (dom/h3 #js {:className "item"}
          (:title data))
        (dom/p #js {:className "meta"}
          (:added-by data)
          "  •  "
          (dom/time nil "Created on " (:created-at data))))
      (dom/div #js {:className "right"}
        (apply dom/div #js {:className "avatars"}
          (om/build-all gravatar (:voter-gravatar-hashes data)))
        (dom/button #js {:className "btn btn-circle"}
          (.toString (:votes data)))
        (dom/button #js {:className (str "btn btn-circle"
                                         (when (:upvoted data) " active"))}
          (dom/i #js {:className (str "fa fa-" (if (:upvoted data)
                                                 "check"
                                                 "chevron-up"))
                      :onClick #(upvote-item! % data)}))))))

(defn modernator
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (sync-list! (:item-list data))
      (js/setInterval
        (fn [] (sync-list! (:item-list data))) 1000))
    om/IRender
    (render [_]
      (dom/div nil
        (om/build modernator-input (:item-list data))

        (apply dom/ul #js {:className "the-list"}
          (om/build-all modernator-item (sort-by-votes (:item-list data))))))))

(om/root modernator app-state
  {:target (. js/document (getElementById "app"))})
