(ns baustellen.input-transformation)

(defn pull-in-names [mainmap]
  (into {} (map (fn [k] (let [submap (mainmap k)]
                          {k (assoc submap :name (name k))}))
                (keys mainmap))))
