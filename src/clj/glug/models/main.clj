(ns glug.models.main
  (:require [clojure.java.jdbc :as sql]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.core.cache :as cache]
            [cemerick.url :refer [url-encode]]
            [crypto.random :as random]
            [clojure.data.json :as json]
            [org.httpkit.client :as http]))

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

(def api-uri "http://api.brewerydb.com/v2/")

(def beer-cache (atom {}))

(def parse-req (comp keywordize-keys json/read-str :body deref http/get))

(defn api-endpoint [method params]
  (str
    api-uri
    method
    "?key="
    (System/getenv "BREWERY_DB_API_KEY")
    (when (not (empty? params))
      (str
        "&"
        params))))

(defn crowd-beers [crowd-id]
  (sql/query spec [(str "select * from beers where crowd_id = ?") crowd-id]))

(defn vote-find [crowd-id beer-id user-id]
  (first (sql/query spec [(str "select * from votes where crowd_id = ? and beer_id = ? and user_id = ?") crowd-id beer-id user-id])))

(defn crowd-votes [crowd-id]
  (sql/query spec [(str "select * from votes where crowd_id = ?") crowd-id]))

(defn vote-create [vote]
  (first (sql/insert! spec :votes vote)))

(defn vote-delete [vote-id]
  (sql/delete! spec :votes ["id = ?" vote-id]))

(defn get-api-beer [id]
  (let [beer (:data (parse-req (api-endpoint (str "beer/" id) "withBreweries=Y")))]
    {:name (:name beer)
     :brewery (-> beer :breweries first :name)
     :image (-> beer :labels :icon)}))

(defn get-db-beer [column value]
  (first (sql/query spec [(str "select * from beers where " column " = ?") value])))

(defn beer-create! [beer]
  (first (sql/insert! spec :beers beer)))

(defn beers-search [query]
  (map
    #(hash-map
       :name (:name %)
       :image (-> % :labels :icon)
       :brewery (-> % :breweries first :name)
       :api-id (:id %))
    (:data (parse-req (api-endpoint
                        "search"
                        (str "type=beer&withBreweries=Y&q=" (url-encode query)))))))

(defn beer-find [id]
  (let [beer-key (keyword (str id))]
    (if-let [cached-beer (beer-key @beer-cache)]
      cached-beer
      (let [fetched-beer (get-api-beer (:api_id (get-db-beer "id" id)))]
        (swap! beer-cache merge {beer-key fetched-beer})
        fetched-beer))))
