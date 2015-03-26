(ns glug.models.main
  (:require [clojure.java.jdbc :as sql]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.core.cache :as cache]
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

(def untappd-uri "https://api.untappd.com/v4/")

(def beer-cache (atom {}))

(def parse-req (comp keywordize-keys json/read-str :body deref http/get))

(defn untappd-endpoint [method params]
  (str
    untappd-uri
    method
    "?client_id="
    (System/getenv "UNTAPPD_CLIENT_ID")
    "&client_secret="
    (System/getenv "UNTAPPD_CLIENT_SECRET")
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

(defn get-untappd-beer [id]
  (let [beer (get-in (parse-req (untappd-endpoint (str "beer/info/" id) nil))
                     [:response :beer])]
    {:name (:beer_name beer)
     :brewery (get-in beer [:brewery :brewery_name])
     :image (:beer_label beer)})
  ; {:name "Foo"
  ;  :brewery "Bar"
  ;  :image "http://beer.jpg.to"}

  )

(defn get-db-beer [column value]
  (first (sql/query spec [(str "select * from beers where " column " = ?") value])))

(defn beer-create! [beer]
  (first (sql/insert! spec :beers beer)))

;(def query "plin")
(defn beers-search [query]
  (map
    #(hash-map
       :name (get-in % [:beer :beer_name])
       :image (get-in % [:beer :beer_label])
       :untappd-id (get-in % [:beer :bid])
       :brewery (get-in % [:brewery :brewery_name]))
    (get-in
      (parse-req
        (untappd-endpoint "search/beer" (str "q=" query)))
      [:response :beers :items]))
  ; [{:name "Heinekin" :brewery "Crappy" :untappd-id 370448 :image "http://crap.jpg.to"}
  ;  {:name "Yuengling" :brewery "Some Other One" :untappd-id 511925 :image "http://yuengling.jpg.to"}]
  )

(defn beer-find [id]
  (let [beer-key (keyword (str id))]
    (if-let [cached-beer (beer-key @beer-cache)]
      cached-beer
      (let [fetched-beer (get-untappd-beer (:untappd_id (get-db-beer "id" id)))]
        (swap! beer-cache merge {beer-key fetched-beer})
        fetched-beer))))
