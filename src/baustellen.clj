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
