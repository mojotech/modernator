(ns glug.cs
  (:require [om.core :as om]
            [om.dom :as dom]
            [clojure.walk :refer [keywordize-keys]]
            [ajax.core :refer [GET POST PUT]]))

(enable-console-print!)

(defn sort-by-votes
  [beer-map]
  (into [] (sort-by :votes > beer-map)))

(defonce app-state (atom {:beer-list []
                          :search ""
                          :typeahead {:selected 0 :list []}}))

(defn upvote-beer!
  [e beer]
  (om/update!
    beer
    (merge beer
           {:upvoted (not (:upvoted beer))
            :votes (if (:upvoted beer)
                     (dec (:votes beer))
                     (inc (:votes beer)))}))
  (PUT (str "votes/" (:id beer)) {:error-handler #(om/update! beer beer)}))

(defn debounce
  ([func wait]
   (debounce func wait nil))
  ([func wait immediate]
   (let [timeout (atom nil)]
     (fn []
       (this-as this
                (let [context this
                      args js/arguments
                      later (fn []
                              (reset! timeout nil)
                              (when-not immediate
                                (.apply func context args)))]
                  (if (and immediate (not @timeout))
                    (.apply func context args))
                  (js/clearTimeout @timeout)
                  (reset! timeout (js/setTimeout later wait))))))))

(defn beer-view
  [beer owner]
  (reify
    om/IRender
    (render [_]
      (dom/li nil
        (:name beer)
        " (" (:votes beer) ")"
        (dom/button #js {:onClick #(upvote-beer! % beer)} "Vote")))))

(defn typeahead-view
  [item owner]
  (reify
    om/IRender
    (render [_]
      (dom/li #js {:style #js {:fontWeight (if (:selected item) "bold" "normal")}}
        (:name item)))))

(defn update-typeahead!
  [query data]
  (GET (str "beers/search/" query)
       {:handler #(om/update!
                    data
                    (assoc-in data [:typeahead :list] (keywordize-keys %)))}))

(def debounced-update-typeahead!
  (debounce update-typeahead! 500))

(defn handle-change [e owner state data]
  (let [value (.. e -target -value)]
    (om/set-state! owner :search value)
    (debounced-update-typeahead! value data)))

(defn new-selected
  [direction data]
  (if (= direction "up")
    (max 0 (dec (get-in data [:typeahead :selected])))
    (min (dec (count (get-in data [:typeahead :list])))
         (inc (get-in data [:typeahead :selected])))))

(defn sync-list! [data]
  (GET "beers"
       {:handler #(om/update!
                    data
                    (assoc data :beer-list (keywordize-keys %)))}))

(defn add-selected-to-list!
  [data]
  (PUT (str "beers/" (:api-id (nth (get-in data [:typeahead :list])
                                   (get-in data [:typeahead :selected]))))
       {:handler #(sync-list! data)}))

(defn beer-list-view
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:search ""})
    om/IWillMount
    (will-mount [_]
      (sync-list! data))
    om/IRenderState
    (render-state [this state]
      (dom/div nil
        (dom/h1 nil "Beer List")
        (dom/input #js {:type "text" :ref "new-beer"
                        :onChange #(handle-change % owner state data)
                        :onKeyDown #(case (.-key %)
                                      "ArrowUp" (om/update! data (assoc-in data [:typeahead :selected] (new-selected "up" data)))
                                      "ArrowDown" (om/update! data (assoc-in data [:typeahead :selected] (new-selected "down" data)))
                                      "Enter" (add-selected-to-list! data)
                                      nil)
                        :value (:search state)})
        (apply dom/ul #js {:className "typeahead-list"}
          (om/build-all typeahead-view (map-indexed #(let [selected-index (get-in data [:typeahead :selected])
                                                           item %2
                                                           index %1]
                                                       (assoc item :selected (= index selected-index)))
                                                    (get-in data [:typeahead :list]))))
        (apply dom/ul #js {:className "beer-list"}
          (om/build-all beer-view (sort-by-votes (:beer-list data))))))))

(om/root beer-list-view app-state
  {:target (. js/document (getElementById "app"))})
