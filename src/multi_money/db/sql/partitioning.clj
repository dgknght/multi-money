; should this really exist in sql-storage?
(ns multi-money.db.sql.partitioning
  (:require [clojure.pprint :refer [pprint]]
            [next.jdbc :as jdbc]
            [java-time.api :as t]
            [config.core :refer [env]]
            [multi-money.util :as utl]))

(defn- first-day-of-the-month
  [date]
  (t/local-date (t/year date) (t/month date) 1))

(defn- periodic-seq
  [start period]
  (lazy-seq (cons start
                  (periodic-seq (t/plus start period)
                          period))))

(defmulti ^:private suffix :interval-type)

(defmethod ^:private suffix :year
  [{:keys [interval-count] [start-date next-start-date] :dates}]
  (if (= 1 interval-count)
    (format "_y%s" (t/year start-date))
    (format "_y%s_y%s"
            (t/year start-date)
            (t/year (t/minus next-start-date
                             (t/days 1))))))

(defn- year-month
  [date]
  (let [year-month (t/year-month date)]
    [(.getYear year-month)
     (.getMonthValue year-month)]))

(defmethod ^:private suffix :month
  [{:keys [interval-count] [start-date next-start-date] :dates}]
  (let [[year month] (year-month start-date)
        [_ end-month] (year-month (t/minus next-start-date (t/days 1)))]
    (if (= 1 interval-count)
      (format "_y%04d_m%02d" year month)
      (format "_y%04d_m%02d_m%02d"
              year
              month
              end-month)))) ; this naming convention assumes we won't cross a year boundary here

(defn- period-like
  [{:keys [interval-type interval-count]}]
  (let [f (case interval-type
            :month t/months
            :year t/years
            (throw (ex-info "Unsupported interval type" {:interval-type interval-type})))]
    (f interval-count)))

(def ^:private tables
  [{:table :transactions
    :interval-count 1
    :interval-type :year}
   {:table :transaction_items
    :interval-count 1
    :interval-type :year}
   #_{:table :prices
    :interval-count 1
    :interval-type :year}
   #_{:table :cached_prices
    :interval-count 1
    :interval-type :year}
   #_{:table :reconciliations
    :interval-count 5
    :interval-type :year}])

(defmulti ^:private period-range :interval-type)

(defmethod period-range :year
  [{:keys [date]}]
  (let [start-of-period (t/local-date (t/year date) 1 1)]
    [start-of-period                         ; The lower boundary is inclusive
     (t/plus start-of-period (t/years 1))])) ; The upper boundary is exclusive

(defmethod period-range :month
  [{:keys [date]}]
  (let [start-of-period (first-day-of-the-month date)]
    [start-of-period
     (t/plus start-of-period (t/months 1))]))

(defn- create-table-cmd
  [{:keys [table-name dates suffix]}]
  (format
   "create table if not exists %s%s partition of %s for values from ('%s') to ('%s');"
   table-name
   suffix
   table-name
   (first dates)
   (second dates)))

(def ^:private earliest-date (t/local-date 1900 1 1))

(defn- create-table-cmds
  "Given any two dates, calculates the tables that need to
  be created to accomodate data within the range and returns
  the commands to create them"
  [start-date end-date options]
  (->> tables
       (map (fn [{:keys [table] :as opts}]
              (-> opts
                  (update-in [:table-name] (fnil identity (name table)))
                  (merge (get-in options [:rules table])))))
       (mapcat (fn [opts]
                 (->> (periodic-seq earliest-date
                                    (period-like opts))
                      (partition 2 1)
                      (drop-while (fn [[s e]]
                                    (and (t/before? s start-date)
                                         (not (t/before? s start-date e)))))
                      (take-while (fn [[s e]]
                                    (or (t/before? s end-date e)
                                        (t/before? e end-date))))
                      (take 10)
                      (map #(assoc opts :dates %)))))
       (map #(assoc % :suffix (suffix %)))
       (map create-table-cmd)))

(defn create-partition-tables
  "Creates the specified partition tables.

  Arguments:
    start-date - the start of the range for which tables are to be created
    end-date   - the end of the range for which tables are to be created
  Options:
    :silent    - do not output the commands that are generated
    :dry-run   - do not execute the commands that are generated
    :rules     - a map of table names to interval type and count"
  ([start-date end-date options]
   (let [config (get-in env [:db :strategies :sql])]
     (doseq [cmd (create-table-cmds start-date end-date options)]
       (when-not (:silent options)
         (println cmd))
       (when-not (:dry-run options)
         (jdbc/execute! (jdbc/get-datasource config) cmd))))))
