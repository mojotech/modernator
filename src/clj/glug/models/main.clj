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

(defn user-find [column value]
  (first (sql/query spec [(str "select * from users where " column " = ?") value])))

(defn users-find [column value]
  (sql/query spec [(str "select * from users where " column " = ?") value]))

(defn crowd-items [crowd-id]
  (sql/query spec [(str "select * from items where crowd_id = ?") crowd-id]))

(defn vote-find [crowd-id item-id user-id]
  (first (sql/query spec [(str "select * from votes where crowd_id = ? and item_id = ? and user_id = ?") crowd-id item-id user-id])))

(defn crowd-votes [crowd-id]
  (sql/query spec [(str "select * from votes where crowd_id = ?") crowd-id]))

(defn vote-create [vote]
  (first (sql/insert! spec :votes vote)))

(defn vote-delete [vote-id]
  (sql/delete! spec :votes ["id = ?" vote-id]))

(defn item-find [column value]
  (first (sql/query spec [(str "select * from items where " column " = ?") value])))

(defn item-create! [item]
  (first (sql/insert! spec :items item)))
