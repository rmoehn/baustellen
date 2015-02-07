(ns user
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application."
  (:require
    [clojure.java.io :as io]
    [clojure.java.javadoc :refer [javadoc]]
    [clojure.pprint :refer [pprint]]
    [clojure.reflect :refer [reflect]]
    [clojure.repl :refer [apropos dir doc find-doc pst source]]
    [clojure.set :as set]
    [clojure.string :as str]
    [clojure.test :as test]
    [clojure.tools.namespace.repl :refer [refresh refresh-all]]
    [clojure.walk :as walk]
    [clojure.data :refer [diff]]
    [baustellen :refer :all]
    [baustellen.input-transformation :as it]
    [baustellen.visualization :as viz]))

(def skill-cost {:walls 30
                 :roof 20
                 :plumbing 10})

(def algo-params
  {; maximum number of "good" agents to consider during the neighborhood
   ; generation
   :n-good-agents 15

   ; number of iterations after which the tabu search shall terminate
   :n-iterations 200

   ; number of iterations a previously visit shall not be visited again
   :n-tabued 20

   ; function to use for generating an initial distribution
   :init-fn find-initial-distribution
   })

(defn empty-distribution [static-data]
  {:allocation {} :reservoir (it/generate-reservoir static-data)})

(def ex-distr {:allocation
               {:einfamilienhaus
                {:roof  {:dachdecker1 3},
                 :walls  {:maurer1 3, :maurer2 2},
                 :plumbing  {:klempner2 5}},
                :schwimmhalle
                {:walls  {:maurer1 0, :maurer2 2},
                 :plumbing  {:klempner2 1, :klempner1 7}},
                :sporthalle
                {:roof  {:dachdecker1 6},
                 :walls  {:maurer1 0, :maurer2 3},
                 :plumbing  {:klempner2 0, :klempner1 1}}},
               :reservoir
               {:plumbing  {:klempner1 0, :klempner2 2},
                :roof  {:dachdecker1 6},
                :walls  {:maurer1 7, :maurer2 0}}})

(defn prepare-result [initial-distribution static-data algo-params]
  (let [best-distrs (second (tabu-search initial-distribution static-data
                                         algo-params))]
    (->> best-distrs
         (map (fn [d] [d (netto-payoff (d :allocation) static-data)]))
         (sort-by second >=)
         (take 5))))

(defn run-on-file
  [file skill-cost algo-params]
  (let [filename (.getName file)
        data (read-string (slurp file))
        static-data (if (data :skill-cost)
                      data
                      (merge {:skill-cost skill-cost} data))
        init-distr ((algo-params :init-fn) static-data)
        init-payoff (netto-payoff (:allocation init-distr) static-data)
        best-five (prepare-result init-distr static-data algo-params)
        opti-payoff (netto-payoff ((ffirst best-five) :allocation) static-data)]
    (pprint best-five)
    (println filename)
    (println (str "initial: " init-payoff " optimized: " opti-payoff))
    (viz/save-before-after init-distr (ffirst best-five) static-data filename)))

(def examples (filter #(.isFile %) (file-seq (io/file (io/resource "examples")))))
