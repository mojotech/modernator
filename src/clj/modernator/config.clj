(ns modernator.config
  (:require [environ.core :refer [env]]))

(def overrides {:database-url (or (env :database-url) "postgresql://localhost:5432/modernator")
                :modernator-url (or (env :modernator-url) "http://localhost:8080/")
                :salt (or env :salt) "mojotech"})

(defn config
  "A centralized map for environment variables and their overrides"
  [env-var]
  (let [env-vars (merge env overrides)]
    (env-vars (keyword env-var))))
