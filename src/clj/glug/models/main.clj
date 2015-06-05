(ns glug.models.main
  (:require [clojure.java.jdbc :as sql]
            [clojure.string :as str]
            [crypto.random :as random]))

(def spec (or (System/getenv "DATABASE_URL")
              "postgresql://localhost:5432/glug"))

(defn users-create [users]
  (apply sql/insert! spec :users (mapv #(assoc % :auth_token (random/url-part 20)) users)))

(defn crowd-create [crowd]
  (first (sql/insert! spec :crowds crowd)))

(defn crowd-find [column value]
  (first (sql/query spec [(str "select * from crowds where " column " = ?") value])))

(defn user-update [set-map where-clause]
  (sql/update! spec :users set-map where-clause))

(defn user-find [column value]
  (first (sql/query spec [(str "select * from users where " column " = ?") value])))

(defn user-find-by-id-and-list [id m-list]
  (first (sql/query spec [(str "select * from users where id = ? and crowd_id = ?") id m-list])))

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
