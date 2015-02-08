(ns baustellen.input-transformation)

(defn pull-in-names [mainmap]
  (into {} (map (fn [k] (let [submap (mainmap k)]
                          {k (assoc submap :name (name k))}))
                (keys mainmap))))

(defn generate-reservoir
  "Generates an initial reservoir of skill bundles from the given static data."
  [static-data]
  (reduce (fn [prev-res [agent-k {:keys [skill capacity]}]]
            (assoc-in prev-res [skill agent-k] capacity))
          {}
          (:agents static-data)))

(defn- map->sorted-map [m]
  (into (sorted-map) m))

(defn read-data [file]
  (let [data (read-string (slurp file))]
    (-> data
        (update-in [:agents] map->sorted-map)
        (update-in [:sites] map->sorted-map))))
