(ns baustellen)

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
  (* dist (Math/sqrt cnt) (get skill-costs skill)))

(defn sum-allocations
  [skill allocation]
  (apply + (vals (get allocation skill))))

(defn sufficient-allocation?
  "True if the allocated skills meet the demand of the site."
  [{:keys [site allocation]} sites]
  (let [demand (:skills (get sites site))]
    (every?
      (fn [skill]
        (>= (sum-allocations skill allocation) (get demand skill)))
      (keys demand))))

(defn cost-in-coal
  [agent {:keys [site allocation]} sites agents skill-costs]
  (let [skill (:skill (get agents agent))
        cnt (get-in allocation [skill agent])]
    (cost (distance (:location (get sites site))
                    (:location (get agents agent)))
          skill cnt skill-costs)))

(defn total-cost
  [{:keys [site allocation] :as coal} sites agents skill-costs]
  (let [agents-in-coal (mapcat keys (vals allocation))]
    (apply + (map (fn [a] (cost-in-coal a coal sites agents skill-costs))
                  agents-in-coal))))

(defn coalition-value
  [coalition sites agents skill-costs]
  (if (sufficient-allocation? coalition sites)
    (- (:payoff (get sites (:site coalition)))
       (total-cost coalition sites agents skill-costs))
    0))
