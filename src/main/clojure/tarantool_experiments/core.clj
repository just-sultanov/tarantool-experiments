(ns tarantool-experiments.core
  (:import
    (clojure.lang
      PersistentVector)
    (io.tarantool.driver.api
      TarantoolClient
      TarantoolClientFactory
      TarantoolResult)
    (io.tarantool.driver.api.conditions
      Conditions)
    (io.tarantool.driver.api.space
      TarantoolSpaceOperations)
    (io.tarantool.driver.api.tuple
      DefaultTarantoolTupleFactory
      TarantoolTuple
      TarantoolTupleFactory)
    (io.tarantool.driver.mappers
      DefaultMessagePackMapperFactory)
    (java.util
      Date
      UUID)))


(defn ^TarantoolClient make-client
  []
  (-> (TarantoolClientFactory/createClient)
      (.withAddress "localhost" 3301)
      (.withCredentials "root" "root")
      (.build)))


(def mapper
  (.defaultComplexTypesMapper (DefaultMessagePackMapperFactory/getInstance)))


(defn make-tuple-factory
  []
  (DefaultTarantoolTupleFactory. mapper))


(defn ^TarantoolSpaceOperations space
  [^TarantoolClient client ^String space-name]
  (.space client space-name))


(defn ^TarantoolResult select
  [^TarantoolSpaceOperations space]
  (->> (Conditions/any)
       (.select space)
       (.get)))


(defn ^TarantoolResult insert
  [^TarantoolTupleFactory tuple-factory ^TarantoolSpaceOperations space ^PersistentVector coll]
  (->> coll
       (.create tuple-factory)
       (.insert space)
       (.get)))


(defn read-tuple
  [^TarantoolResult result]
  (mapv
    (fn [^TarantoolTuple tuple]
      (reduce
        (fn [acc field]
          (conj acc (.getValue field mapper)))
        [] (.getFields tuple))) result))


(defn to-unix
  [^Date date]
  (/ (.getTime date) 1000))


(defn random-uuid
  []
  (UUID/randomUUID))


(comment

  (def client (make-client))
  (def tuple-factory (make-tuple-factory))

  (def users (space client "users"))

  (defn make-user []
    [(random-uuid) "john" "doe" "john@doe.com" (to-unix #inst "2021-11-24") "Secret street" 42 true 123])

  (read-tuple (insert tuple-factory users (make-user)))
  ;; => [[#uuid"88b217b1-f302-49e1-be3f-7651d30d25ad" "john" "doe" "john@doe.com" 1637712000 "Secret street" 42 true 123]]

  (read-tuple (select users))
  ;; => [[#uuid"88b217b1-f302-49e1-be3f-7651d30d25ad" "john" "doe" "john@doe.com" 1637712000 "Secret street" 42 true 123]]
  )
