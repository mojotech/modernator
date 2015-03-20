(ns glug.models.main
  (:require [clojure.java.jdbc :as sql]
            [crypto.random :as random]))

(def spec (or (System/getenv "DATABASE_URL")
              "postgresql://localhost:5432/glug"))

(defn users-create [users]
  (apply sql/insert! spec :users (mapv #(assoc % :auth_token (random/url-part 20)) users)))

(defn crowd-create [crowd]
  (first (sql/insert! spec :crowds crowd)))

(defn user-update [set-map where-clause]
  (sql/update! spec :users set-map where-clause))

(defn user-get [column value]
  (first (sql/query spec [(str "select * from users where " column " = ?") value])))

(defn users-get [column value]
  (sql/query spec [(str "select * from users where " column " = ?") value]))
