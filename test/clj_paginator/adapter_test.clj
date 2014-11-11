(ns clj-paginator.adapter-test
  (:require [clj-paginator.adapter :refer :all]
            [clojure.string :as str]
            [korma.core :refer :all]
            [korma.db :refer :all]
            [midje.sweet :refer :all]))


(defdb mem-db (h2 {:db "mem:test"
                   :naming {:keys str/lower-case
                            :fields str/upper-case}}))
(defn truncate [table]
  (exec-raw [(str "TRUNCATE " table)]))

(defn create-dummy-table []
  (exec-raw "CREATE TABLE user (
  id int(11) NOT NULL,
  name varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
)"))

(defn insert-dummy []
  (insert :user
          (values (map (fn [n]
                         {:id n :name (str "name-" n)}) (range 1 11)))))

(defn drop-dummy-table [table]
  (exec-raw (str "DROP TABLE IF EXISTS " table)))

;; setup db
(drop-dummy-table "user")
(create-dummy-table)
(insert-dummy)

(facts "Pageable"
  (facts "Sequential"
    (fact "count-all"
      (count-all [1 2 3]) => 3
      (count-all '(1 2 3)) => 3
      (count-all (range 3)) => 3)
    (fact "find-all"
      (find-all [1 2 3] 1 1) => [1]
      (find-all [1 2 3] 2 1) => [2]
      (find-all [1 2 3] 4 1) => []
      (find-all [1 2 3] 0 1) => []
      (find-all [1 2 3] 1 2) => [1 2]
      (find-all [1 2 3] 2 2) => [3]
      (find-all '(1 2 3) 1 1) => '(1)
      (find-all '(1 2 3) 2 1) => '(2)
      (find-all '(1 2 3) 4 1) => '()
      (find-all '(1 2 3) 0 1) => '()
      (find-all '(1 2 3) 1 2) => '(1 2)
      (find-all '(1 2 3) 2 2) => '(3)
      (find-all (range 1 4) 1 2) => '(1 2)))

  (facts "KormaAdapter"
    (let [users (->KormaAdapter (select* :user))]
      (fact "count-all"
        (count-all users) => 10)
      (fact "find-all"
        (find-all users 3 2) => [{:id 5 :name "name-5"}
                                 {:id 6 :name "name-6"}]
        (find-all users 0 1) => []
        (find-all users 100 1) => []))))
