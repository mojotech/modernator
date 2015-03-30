(ns glug.cs
  (:require [om.core :as om]
            [om.dom :as dom]
            [clojure.walk :refer [keywordize-keys]]
            [ajax.core :refer [GET POST PUT]]))

(enable-console-print!)

(defn sort-by-votes
  [item-map]
  (into [] (sort-by :votes > item-map)))

(defonce app-state (atom {:item-list []
                          :new-item ""
                          :typeahead {:selected nil :list []}}))

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

; (defn debounce
;   ([func wait]
;    (debounce func wait nil))
;   ([func wait immediate]
;    (let [timeout (atom nil)]
;      (fn []
;        (this-as this
;                 (let [context this
;                       args js/arguments
;                       later (fn []
;                               (reset! timeout nil)
;                               (when-not immediate
;                                 (.apply func context args)))]
;                   (if (and immediate (not @timeout))
;                     (.apply func context args))
;                   (js/clearTimeout @timeout)
;                   (reset! timeout (js/setTimeout later wait))))))))

(defn item-view
  [item owner]
  (reify
    om/IRender
    (render [_]
      (dom/li nil
        (:title item)
        " (" (:votes item) ")"
        (dom/button #js {:onClick #(upvote-item! % item)} "Vote")))))

; (defn typeahead-view
;   [item owner]
;   (reify
;     om/IRender
;     (render [_]
;       (dom/li #js {:style #js {:fontWeight (if (:selected item) "bold" "normal")}}
;         (:title item)))))

; (defn update-typeahead!
;   [query data]
;   (GET (str "items/new-item/" query)
;        {:handler #(om/update!
;                     data
;                     (assoc-in data [:typeahead :list] (keywordize-keys %)))}))
;
; (def debounced-update-typeahead!
;   (debounce update-typeahead! 100))

(defn handle-change [e owner state data]
  (let [value (.. e -target -value)]
    (om/set-state! owner :new-item value)
    ;(debounced-update-typeahead! value data)
    ))

; (defn new-selected
;   [direction data]
;   (if (= direction "up")
;     (max 0 (dec (get-in data [:typeahead :selected])))
;     (min (dec (count (get-in data [:typeahead :list])))
;          (inc (get-in data [:typeahead :selected])))))

(defn sync-list! [data]
  (GET "items"
       {:handler #(om/update!
                    data
                    (assoc data :item-list (keywordize-keys %)))}))

(defn add-to-list!
  [item data]
  (PUT "items"
       {:format :json
        :params {:title item}
        :handler #(sync-list! data)}))

(defn item-list-view
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:new-item ""})
    om/IWillMount
    (will-mount [_]
      (sync-list! data))
    om/IRenderState
    (render-state [this state]
      (dom/div nil
        (dom/h1 nil "List")
        (dom/input #js {:type "text" :ref "new-item"
                        :onChange #(handle-change % owner state data)
                        :onKeyDown #(case (.-key %)
                                      ; "ArrowUp" (om/update! data (assoc-in data [:typeahead :selected] (new-selected "up" data)))
                                      ; "ArrowDown" (om/update! data (assoc-in data [:typeahead :selected] (new-selected "down" data)))
                                      "Enter" (add-to-list! (:new-item state) data)
                                      nil)
                        :value (:new-item state)})
        ; (apply dom/ul #js {:className "typeahead-list"}
        ;   (om/build-all typeahead-view (map-indexed #(let [selected-index (get-in data [:typeahead :selected])
        ;                                                    item %2
        ;                                                    index %1]
        ;                                                (assoc item :selected (= index selected-index)))
        ;                                             (get-in data [:typeahead :list]))))
        (apply dom/ul #js {:className "item-list"}
          (om/build-all item-view (sort-by-votes (:item-list data))))))))

(om/root item-list-view app-state
  {:target (. js/document (getElementById "app"))})
