(ns baustellen.visualization
  (:require [rhizome.viz :refer [view-graph]]))

(defn providers [site-k allocation]
  (for [bundles (vals (allocation site-k))
        agent-k (keys bundles)]
    agent-k))

(defn loc->str [[x y]]
  (str x \, y \! ))

(defn entities [static-data]
  (merge (:agents static-data) (:sites static-data)))

(defn site? [k static-data]
  ((set (keys (:sites static-data))) k))

(defn format-node [k allocation static-data]
  {:label (name k)
   :pos (loc->str (:location (get (entities static-data) k)))
   :shape "box"
   :style "filled"
   :color (if (site? k static-data) "red" "")})

(defn format-edge [site-k agent-k allocation static-data]
  (let [skill (get-in static-data [:agents agent-k :skill])]
    {:penwidth (get-in allocation [site-k skill agent-k])}))

(defn show-allocation [allocation static-data]
  (let [site-ks (set (keys (:sites static-data)))
        agent-ks (set (keys (:agents static-data)))]
   (view-graph (concat site-ks agent-ks)
               (fn [k] (if (site-ks k)
                         (providers k allocation)
                         #_(customers k allocation)))
               :directed? false
               :vertical? false
               :options {:layout "neato"
                         :splines "curved"}
               :node->descriptor #(format-node % allocation static-data)
               :edge->descriptor #(format-edge %1 %2 allocation static-data))))
