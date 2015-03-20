(ns glug.assets.styles
  (:require [garden.def :refer [defstylesheet defstyles]]
            [garden.units :refer [px]]))

(defstyles screen
  [:.beer-list {:position "relative"}
   [:li {:padding-bottom "5px"}]])
