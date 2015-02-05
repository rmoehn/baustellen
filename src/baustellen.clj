(ns baustellen
  (:require [clojure.pprint :refer [pprint]]
            [amalloy.ring-buffer :refer [ring-buffer]]))

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

(defn indiv-alloc-cost [[site-k site-alloc] static-data]
  (let [site-loc (get-in static-data [:sites site-k :location])]
    (apply +
           (mapcat
             (fn [[skill bundles]]
               (map (fn [[agent-k cnt]]
                      (let [agent-loc (get-in static-data
                                              [:agents agent-k :location])
                            dist (distance agent-loc site-loc)]
                        (cost dist skill cnt (:skill-cost static-data))))
                    bundles))
             site-alloc))))

(defn demand
  "Return the demand for skill of the site with site-k given an allocation."
  [allocation [site-k skill :as path] static-data]
  {:post [(>= % 0)]}
  (let [bundles (get-in allocation path)
        n-allocated (apply + (vals bundles))
        initial-capacity (get-in static-data [:sites site-k :skills skill])]
    (- initial-capacity n-allocated)))

(defn demand-met? [[site-k site-alloc] static-data]
  (zero? (apply + (map #(demand {site-k site-alloc} [site-k %] static-data)
                       (keys site-alloc)))))

(defn indiv-alloc-brutto-payoff [[site-k site-alloc :as alloc] static-data]
  (if (demand-met? alloc static-data)
    (get-in static-data [:sites site-k :payoff])
    0))

(defn netto-payoff [allocation static-data]
  (- (apply + (map #(indiv-alloc-brutto-payoff % static-data) allocation))
     (apply + (map #(indiv-alloc-cost % static-data) allocation))))

;;; Credits: http://stackoverflow.com/a/2763660
(defn filter-map [p m]
  (select-keys m (for [[k v] m :when (p v)] k)))

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
  [{:keys [allocation reservoir]} [site-k skill :as path] agent-k static-data]
  (let [demand (demand allocation path static-data)
        to-allocate (min demand (get-in reservoir [skill agent-k]))]
    {:allocation (update-in allocation [site-k skill agent-k]
                            #(if % (+ % to-allocate) to-allocate))
     :reservoir (update-in reservoir [skill agent-k] #(- % to-allocate))}))

(defn allocate-full
  "Allocates as many units of skill as possible and needed to site with site-k,
  returning the new distribution."
  [{:keys [allocation reservoir] :as distribution} site-k skill static-data]
  (loop [available (good-agents (count (:agents static-data)) site-k skill
                                reservoir static-data)
         cur-distr distribution]
    (if (and (seq available)
             (pos? (demand allocation [site-k skill] static-data)))
      (recur (rest available)
             (allocate-max-bundle cur-distr [site-k skill] (first available)
                                  static-data))
      cur-distr)))

(defn find-single-distribution
  "For each skill needed by site site-k, allocates as many units of that skill
  as possible and needed to it. Returns the new distribution."
  [{:keys [allocation reservoir] :as distribution} site-k static-data]
  (let [allocator (fn [prev-distr skill]
                    (allocate-full prev-distr site-k skill static-data))]
    (reduce allocator distribution
            (keys (get-in static-data [:sites site-k :skills])))))

(defn find-initial-distribution [reservoir static-data]
  "Allocates as many skills as possible from the closest agents to each
  construction site."
  (let [allocator (fn [prev-distr site-k]
                    (find-single-distribution prev-distr site-k static-data))]
    (reduce allocator {:allocation {} :reservoir reservoir}
            (keys (:sites static-data)))))

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
  [{:keys [allocation reservoir]} [_ skill agent-k :as path]]
  (let [bundle-size (get-in allocation path)]
    {:allocation (dissoc-in allocation path)
     :reservoir (update-in reservoir [skill agent-k] #(+ % bundle-size))}))

(defn generate-neighborhood
  [{:keys [allocation reservoir] :as distribution} static-data
   {:keys [n-good-agents]}]
  (concat
    (for [site-k (keys allocation)
          skill (keys (allocation site-k))
          agent-k (keys (get-in allocation [site-k skill]))]
      (move-to-reservoir distribution [site-k skill agent-k]))
    (for [site-k (keys allocation)
          skill (keys (get-in static-data [:sites site-k :skills]))
          :when (pos? (demand allocation [site-k skill] static-data))
          a (good-agents n-good-agents site-k skill reservoir static-data)]
      (allocate-max-bundle distribution [site-k skill] a static-data))))

(defn find-best-distr [distrs static-data]
  (first (sort-by #(netto-payoff (:allocation %) static-data) distrs)))

(defn tabu-search
  [{:keys [allocation reservoir] :as distribution} static-data
   {:keys [n-iterations n-tabued] :as algo-params}]
  (let [tabu-allocs (ring-buffer n-tabued)]
    (take
      n-iterations
      (take-while
        some?
        (iterate
          (fn [[{:keys [allocation reservoir] :as distribution} best-allocs
                tabu-allocs]]
            (let [neighbors (filter #(not-any? #{(:allocation %)} tabu-allocs)
                                    (generate-neighborhood distribution
                                                           static-data
                                                           algo-params))
                  best-neighbor (find-best-distr neighbors static-data)
                  best-alloc (:allocation best-neighbor)]
              (and (seq neighbors)
                   [best-neighbor (conj best-allocs best-alloc)
                    (conj tabu-allocs best-alloc)])))
          [distribution [allocation] (conj tabu-allocs allocation)])))))
