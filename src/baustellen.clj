(ns baustellen
  (:require [clojure.pprint :refer [pprint]])
  )

(defn distance [p1 p2]
  (Math/sqrt
    (apply +
           (map (fn [c1 c2] (Math/pow (- c1 c2) 2))
                p1 p2))))

(defn cost
  "Returns the cost for transporting cnt units of skill over a dist. The basis
  cost for transporting one unit of skill over one unit of distance is taken
  from skill-costs.

  Currently the cost increases linearly with the distance and squareroot-ly with
  the number of skill-units transported."
  [dist skill cnt skill-costs]
  (* dist (Math/sqrt cnt) (skill-costs skill)))

(defn sum-allocations
  [skill allocation]
  (apply + (vals (get allocation skill))))

(defn sufficient-allocation?
  "True if the allocated skills meet the demand of the site."
  [{:keys [site allocation]}]
  (let [demand (:skills site)]
    (every?
      (fn [skill]
        (>= (sum-allocations skill allocation) (demand skill)))
      (keys demand))))

(defn cost-in-coal
  [agent {:keys [site allocation]} skill-costs]
  (let [skill (:skill agent)
        cnt (get-in allocation [skill agent])]
    (cost (distance (:location site)
                    (:location agent))
          skill cnt skill-costs)))

(defn total-cost
  [{:keys [site allocation] :as coal} sites agents skill-costs]
  (let [agents-in-coal (mapcat keys (vals allocation))]
    (apply + (map (fn [a] (cost-in-coal a coal skill-costs))
                  agents-in-coal))))

(defn coalition-value
  [coalition sites agents skill-costs]
  (if (sufficient-allocation? coalition)
    (- (get-in coalition [:site :payoff])
       (total-cost coalition sites agents skill-costs))
    0))

(defn sorted-map-by-distance [location m]
  (into (sorted-map-by (fn [k1 k2]
                         (compare [(distance location (:location (m k1))) k1]
                                  [(distance location (:location (m k2))) k2])))
        m))

(defn filter-skill [skill agents]
  (into {} (filter (fn [[k v]] (= skill (:skill v)))
                   agents)))

(defn allocation-for-skill
  [location [skill n-needed] agents]
  (let [sorted-agents (sorted-map-by-distance location
                                              (filter-skill skill agents))]
    (loop [n-remaining n-needed
           remaining-agents sorted-agents
           processed-agents {}
           allocation {}]
      (cond
        (zero? n-remaining)
        [allocation (merge processed-agents remaining-agents)]

        (empty? remaining-agents)
        nil

        :else
        (let [[a-k a-v] (first remaining-agents)
              n-taken (min n-remaining (:capacity a-v))
              new-a-v (update-in a-v [:capacity] #(- % n-taken))]
          (recur (- n-remaining n-taken)
                 (dissoc remaining-agents a-k)
                 (assoc processed-agents a-k new-a-v)
                 (assoc allocation new-a-v n-taken)))))))

(defn find-initial-allocation
  "Given a construction site and information about available agents, finds a
  heuristically good allocation for this site. Returns the coalition and and the
  agent datastructure with capacities reduced according to the allocation."
  [site agents]
  (println site)
  (println (:skills site))
  (let [allocs-agents (map (fn [demand]
                             (allocation-for-skill (:location site)
                                                   demand
                                                   agents))
                           (:skills site))
        allocs (map first allocs-agents)
        agents (map second allocs-agents)]
    [(into {} allocs) (into {} agents)]))

;;; Credits: http://stackoverflow.com/questions/14488150/how-to-write-a-dissoc-in-command-for-clojure
(defn dissoc-in
  "Dissociates an entry from a nested associative structure returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new structure."
  [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))

(defn move-to-reservoir
  [{:keys allocation reservoir} [_ skill agent-k :as path]]
  (let [bundle-size (get-in allocation path)]
    {:allocation (dissoc-in allocation path)
     :reservoir (update-in reservoir [skill agent-k] #(+ % bundle-size))}))

(defn good-agents
  "Finds the n (or less) agents providing the skill that are closest to the site
  with key site-k and still have capacity."
  [n site-k skill reservoir static-data]
  (let [bundles (reservoir skill)
        non-exhausted (filter-map pos? bundles)
        by-distance (sort-by #(distance
                                (get-in static-data [:sites site-k :location])
                                (get-in static-data [:agents % :location]))
                             (keys non-exhausted))]
    (take n by-distance)))

(defn allocate-max-bundle
  [{:keys allocation reservoir} [site-k skill :as path] agent-k static-data]
  (let [demand (demand allocation path)
        to-allocate (min demand (get-in reservoir [skill agent-k]))]
    {:allocation (update-in allocation path
                            #(if (nil? %) to-allocate (+ % to-allocate)))
     :reservoir (update-in reservoir [skill agent-k] #(- % to-allocate))}))

(defn generate-neighborhood
  [{:keys allocation reservoir :as distribution} static-data
   {:keys n-good-agents}]
  (concat
    (for [site-k (keys allocation)
          skill (keys site)
          agent-k (keys skill)]
      (move-to-reservoir distribution [site-k skill agent-k]))
    (for [site-k (keys allocation)
          skill (keys site)]
      (if (incomplete-alloc? site-k (get-in [site-k skill] allocation)
                             static-data)
        (for [a (good-agents n-good-agents site-k skill reservoir static-data)]
          (allocate-max-bundle distribution [site-k skill] a static-data))))))

(defn tabu-search [allocation reservoir iterations]
  (let [tabu-queue (ring-buffer n-tabued)])
  (loop [best-allocs [allocation]
         tabu-queue (conj tabu-queue allocation)]
    (let [neighbors (filter #(not-tabu? % tabu-queue)
                            (generate-neighorhood allocation reservoir))
          best-neighbor (find-best-alloc neighbors)]
      (recur (conj best-allocs best-neighbor) (conj tabu best-neighbor)))))
                                              ; Does our queue support conj?
