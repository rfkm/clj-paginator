(ns clj-paginator.utils)

(defn lazy? [coll]
  (and (= (type coll) clojure.lang.LazySeq)
       (not (realized? coll))))

(defn has-keys? [m keys]
  (apply = (map count [keys (select-keys m keys)])))

(defn korma? [target]
  (and (map? target)
       (has-keys? target #{:ent :table :db :options})))
