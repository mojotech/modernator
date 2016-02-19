(ns modernator.encrypt
  (:require [environ.core :refer [env]]
            [modernator.config :refer [config]]
            [digest :refer [digest]]))

(defn encrypt
  "Encryption of the user id based on the config salt"
  [user-id]
  (digest/sha-256 (str user-id (config :salt))))
