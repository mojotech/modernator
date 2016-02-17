(ns modernator.cs
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.string :as string]
            [clojure.walk :refer [keywordize-keys]]
            [ajax.core :refer [GET PUT]]))

(enable-console-print!)

(defonce app-state (atom {:item-list []}))

(defn print-and-return [args]
  (println args)
  args)

(defn list-items []
  (om/ref-cursor (:items (om/root-cursor app-state))))

(defn url [suffix]
  (str (.-pathname (.-location js/window)) "/" suffix))

(defn sort-by-votes
  [item-map]
  (into [] (sort-by :votes > item-map)))

(defn new-selected
  [direction currently-selected total-items]
  (if (= direction "up")
    (max 0 (dec currently-selected))
    (min (dec total-items)
         (inc currently-selected))))

(defn sync-list! []
  (GET (url "items")
       {:handler #(swap! app-state assoc :item-list (keywordize-keys %))}))

(defn upvote-item!
  [e item]
  (om/update!
    item
    (merge item
           {:upvoted (not (:upvoted item))
            :votes (if (:upvoted item)
                     (dec (:votes item))
                     (inc (:votes item)))}))
  (PUT (str (url "votes/") (:id item)) {:error-handler #(om/update! item item)
                                        :handler #(sync-list!)}))

(defn partial-complete
  [needle attr haystack]
  (when-not (empty? needle)
    (let [pattern (js/RegExp. (str "^" needle ".*") "i")]
      (not-empty (filterv #(re-matches pattern (get % attr)) haystack)))))

(defn handle-change! [e owner state data]
  (let [value (.. e -target -value)]
    (om/set-state! owner :new-item value)
    (om/set-state! owner :filtered (partial-complete value :title data))))

(defn handle-typeahead-focus!
  [direction owner currently-selected total-items]
  (om/set-state! owner :selected (new-selected direction currently-selected total-items)))

(defn add-current-to-list!
  [state owner]
  (let [current (if (:selected state)
                  (print-and-return (get-in (:filtered state) [(:selected state) :title]))
                  (print-and-return (:new-item state)))]
    (when-not (empty? (print-and-return current))
      (PUT (url "items")
           {:format :json
            :params {:title (print-and-return current)}
            :handler #(sync-list!)})
      (om/set-state! owner :selected nil)
      (om/set-state! owner :new-item nil))))

(defn modernator-input
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:new-item ""
       :selected nil
       :filtered data})
    om/IDidMount
    (did-mount [_]
      (.addEventListener
        js/document
        "click"
        #(when-not (.contains
                     (.getElementById js/document "input-wrapper")
                     (.. % -target))
           (om/set-state! owner :new-item ""))))
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "input-wrapper" :id "input-wrapper"}
        (dom/i #js {:className "icon-pencil"})
        (dom/input #js {:type "text" :ref "new-item"
                        :className "input new-item form-field input-search"
                        :placeholder "Enter a Suggestion"
                        :onChange #(handle-change! % owner state data)
                        :onKeyDown #(let [currently-selected (or (:selected state) -1)
                                          typeahead-results (:filtered state)
                                          total-items (count typeahead-results)]
                                      (case (.-key %)
                                        "ArrowUp" (handle-typeahead-focus! "up" owner currently-selected total-items)
                                        "ArrowDown" (handle-typeahead-focus! "down" owner currently-selected total-items)
                                        "Enter" (add-current-to-list! state owner)
                                        (om/set-state! owner :selected nil)))
                        :value (let [currently-selected (:selected state)]
                                 (if-not (nil? currently-selected)
                                   (get-in (:filtered state) [currently-selected :title])
                                   (:new-item state)))})
        (dom/button #js {:className "btn-primary"
                         :onClick #(add-current-to-list! state owner)}
          "Submit")
        (when-not (string/blank? (:new-item state))
          (let [typeahead-items (map-indexed #(let [selected-index (:selected state)
                                                    item %2
                                                    index %1]
                                                (assoc item :selected (= index selected-index)))
                                             (:filtered state))]
            (when-not (empty? typeahead-items)
              (apply dom/ul #js {:className "typeahead-list"}
                (map-indexed
                  (fn [index item]
                    (dom/li #js {:className (when (:selected item) "active")
                                 :onClick #(add-current-to-list! state owner)
                                 :onMouseOver #(om/set-state! owner :selected index)}
                            (:title item)))
                  typeahead-items)))))))))

(defn gravatar
  [data owner]
  (om/component
    (dom/img #js {:className "avatar-image"
                  :src (str "http://www.gravatar.com/avatar/" data "?d=retro")})))

(defn modernator-item
  [data owner]
  (om/component
    (dom/li #js {:className "list-item"
                 :style #js {:top (str (* (:order data) 112) "px")}}
      (dom/div #js {:className "left"}
        (dom/h3 #js {:className "item"}
          (dom/div #js {:className "number"}
            (+ 1 (:order data)))
          (:title data))
        (dom/p #js {:className "meta"}
          (:added-by data)
          "  •  "
          (dom/time nil "Created on " (:created-at data))))
      (dom/div #js {:className "right controls"}
        (apply dom/div #js {:className "avatars"}
          (om/build-all gravatar (take 4 (:voter-gravatar-hashes data))))
        (apply dom/div #js {:className "circle vote-count"}
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
      (sync-list!)
      (js/setInterval
        (fn [] (sync-list!)) 1000))
    om/IRender
    (render [_]
      (dom/div
        nil

        (dom/div #js {:className "recently-added"}
                 (dom/h2 #js {:className "h2"} "Recently Added")
                 (apply dom/ul
                        nil
                        (om/build-all modernator-item (sort-by :created-at > (:item-list data)))))

        (dom/div #js {:className "list-wrapper"}
          (dom/div #js {:className "content"}
            (om/build modernator-input (:item-list data))
            (apply dom/ul #js {:className "the-list"}
              (let [items (:item-list data)
                    index-map (zipmap (map #(-> % :id) (sort-by-votes items)) (range))
                    items-with-orders (map #(assoc % :order (get index-map (:id %))) items)]
                (om/build-all modernator-item items-with-orders)))))))))

(om/root modernator app-state
  {:target (. js/document (getElementById "app"))})
