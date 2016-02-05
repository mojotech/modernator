(ns modernator.models.main
  (:require [clojure.java.jdbc :as sql]
            [clojure.string :as str]
            [crypto.random :as random]
            [modernator.config :refer [config]]))

(def spec (config :database-url))

(defn users-create [users]
  (apply sql/insert! spec :users (mapv #(assoc % :auth_token (random/url-part 20)) users)))

(defn list-create [m-list]
  (first (sql/insert! spec :lists m-list)))

(defn list-find [column value]
  (first (sql/query spec [(str "select * from lists where " column " = ?") value])))

(defn user-update [set-map where-clause]
  (sql/update! spec :users set-map where-clause))

(defn user-find [column value]
  (first (sql/query spec [(str "select * from users where " column " = ?") value])))

(defn user-find-by-id-and-list [id m-list]
  (first (sql/query spec [(str "select * from users where id = ? and list_id = ?") id m-list])))

(defn users-find [column value]
  (sql/query spec [(str "select * from users where " column " = ?") value]))

(defn list-items [list-id]
  (sql/query spec [(str "select * from items where list_id = ?") list-id]))

(defn vote-find [list-id item-id user-id]
  (first (sql/query spec [(str "select * from votes where list_id = ? and item_id = ? and user_id = ?") list-id item-id user-id])))

(defn list-votes [list-id]
  (sql/query spec [(str "select * from votes where list_id = ?") list-id]))

(defn vote-create [vote]
  (first (sql/insert! spec :votes vote)))

(defn vote-delete [vote-id]
  (sql/delete! spec :votes ["id = ?" vote-id]))

(defn item-find [column value]
  (first (sql/query spec [(str "select * from items where " column " = ?") value])))

(defn votes-find [column value]
  (sql/query spec [(str "select user_id from votes where " column " = ?") value]))

(defn find-voters [item-id]
  (let [vote-user-ids (mapv #(:user_id %)
                             (votes-find "item_id" item-id))]
    (if-not (empty? vote-user-ids)
      (sql/query spec [(str "select * from users where id in ("
                            (clojure.string/join "," vote-user-ids)
                            ")")])
      '())))

(defn item-create! [item]
  (first (sql/insert! spec :items item)))
