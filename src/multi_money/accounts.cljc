(ns multi-money.accounts
  (:require #?(:clj [clojure.pprint :refer [pprint]]
               :cljs [cljs.pprint :refer [pprint]])))

(defn left-side?
  [{:account/keys [type]}]
  (#{:asset :expense} type))

; TODO: move polarze to a transactions ns
#_(defn- polarizer
  [action account]
  (if (left-side? account)
    (if (= :credit action) -1M 1M)
    (if (= :credit action) 1M -1M)))

#_(defn polarize
  ([{:keys [quantity action account]}]
   (polarize quantity action account))
  ([quantity action account]
   {:pre [quantity
          (#{:debit :credit} action)
          (:type account)]}
   (* quantity (polarizer action account))))

(defn- assoc-children
  ([account by-parent opts] (assoc-children account by-parent opts 0))
  ([{:keys [id path] :as account} by-parent {:keys [sort-fn] :as opts} depth]
   (assoc account
          :depth depth
          :children
          (->> (by-parent id)
               (sort-by sort-fn)
               (mapv (comp #(assoc %
                                   :path (conj path (:account/name %))
                                   :account/parent account)
                           #(assoc-children % by-parent opts (inc depth))))))))

(defn nest
  ([accounts] (nest accounts {:sort-fn :account/name}))
  ([accounts {:keys [sort-fn] :as opts}]
  (let [by-parent (->> accounts
                       (filter :account/parent)
                       (group-by (comp :id
                                       :account/parent)))]
    (->> accounts
         (remove :account/parent)
         (map #(assoc % :path [(:account/name %)]))
         (sort-by sort-fn)
         (map #(assoc-children % by-parent opts))))))


(defn- unnest-account
  [{:keys [children] :as account}]
  (lazy-seq (cons account (mapcat unnest-account children))))

(defn unnest
  [accounts]
  (mapcat unnest-account accounts))

(def annotate (comp unnest nest))
