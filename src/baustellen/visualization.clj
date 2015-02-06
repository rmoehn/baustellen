(ns baustellen.visualization
  (:require [rhizome.viz :refer [view-graph]]
            [clojure.string :as s]
            [baustellen :refer :all]))

(def green "#00aa77")

(defn providers [site-k allocation]
  (for [bundles (vals (allocation site-k))
        agent-k (keys bundles)]
    agent-k))

(defn loc->str [[x y]]
  (str (* x 0.5) \, (* y 0.5) \! ))

(defn entities [static-data]
  (merge (:agents static-data) (:sites static-data)))

(defn site? [k static-data]
  ((set (keys (:sites static-data))) k))

(defn shorten [s]
  (s/replace s #"(.).*?(\d+)" "$1$2"))

(defmulti format-node (fn [k _ static-data]
                        (if (site? k static-data) :site :agent)))

(defmethod format-node :site [k {:keys [allocation]} static-data]
  {:label (shorten (name k))
   :pos (loc->str (:location (get-in static-data [:sites k])))
   :shape "box"
   :style "filled"
   :color (if (demand-met? [k (allocation k)] static-data) green "red")})

(defmethod format-node :agent [k {:keys [reservoir]} static-data]
  (let [skill (get-in static-data [:agents k :skill])]
    {:label (shorten (name k))
     :pos (loc->str (:location (get-in static-data [:agents k])))
     :shape "circle"
     :style "filled"
     :fillcolor "gray"
     :penwidth 3
     :color (if (pos? (get-in reservoir [skill k])) green "red")}))

(defn format-edge [site-k agent-k allocation static-data]
  (let [skill (get-in static-data [:agents agent-k :skill])]
    {:penwidth (get-in allocation [site-k skill agent-k])}))

(defn show-allocation [{:keys [allocation reservoir] :as distr} static-data]
  (let [site-ks (set (keys (:sites static-data)))
        agent-ks (set (keys (:agents static-data)))]
   (view-graph (concat site-ks agent-ks)
               (fn [k] (and (site? k static-data) (providers k allocation)))
               :directed? false
               :vertical? false
               :options {:layout "fdp"
                         :splines "true"
                         :sep "0.5,0.5"
                         :dpi 60
                         }
               :node->descriptor #(format-node % distr static-data)
               :edge->descriptor #(format-edge %1 %2 allocation static-data))))
