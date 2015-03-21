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

;;; If you want to play around, jump to the bottom of the file. There won't be
;;; much to find, though, unless you're prepared for serious playing around. But
;;; I could tidy up everything and prepare some more entry points if you're
;;; interested for some reason.

(def skill-cost {:walls 30
                 :roof 20
                 :plumbing 10})

(defn empty-distribution [static-data]
  {:allocation {} :reservoir (it/generate-reservoir static-data)})

(def algo-params
  {; maximum number of "good" agents to consider during the neighborhood
   ; generation
   :n-good-agents 15

   ; number of iterations after which the tabu search shall terminate
   :n-iterations 200

   ; number of iterations a previously visit shall not be visited again
   :n-tabued 100

   ; function to use for generating an initial distribution
   :init-fn find-initial-distribution
   })

(defn prepare-result [initial-distribution static-data algo-params]
  (let [best-distrs (second (tabu-search initial-distribution static-data
                                         algo-params))]
    (->> best-distrs
         (map (fn [d] [d (netto-payoff (d :allocation) static-data)]))
         (sort-by second >)
         (take 5))))

(defn run-on-file*
  [file skill-cost algo-params]
  (let [filename (.getName file)
        data (it/read-data file)
        static-data (if (data :skill-cost)
                      data
                      (merge {:skill-cost skill-cost} data))
        init-distr ((algo-params :init-fn) static-data)
        res (tabu-search init-distr static-data algo-params)]
    (pprint res)))

(defn run-on-file
  [file skill-cost algo-params]
  (println file)
  (let [filename (.getName file)
        data (it/read-data file)
        static-data (if (data :skill-cost)
                      data
                      (merge {:skill-cost skill-cost} data))
        init-distr ((algo-params :init-fn) static-data)
        init-payoff (netto-payoff (:allocation init-distr) static-data)
        best-five (prepare-result init-distr static-data algo-params)
        opti-payoff (netto-payoff ((ffirst best-five) :allocation) static-data)]
    (pprint best-five)
    (println (netto-payoff (:allocation (ffirst best-five)) static-data))
    (viz/save-before-after init-distr (ffirst best-five) static-data filename)))

;;; There are example construction site and company data in resources/examples.
;;; You can add your own if you like.

(def examples (filter #(and (.isFile %) (not (.isHidden %)))
                      (file-seq
                        (io/file
                          (io/resource "examples")))))

(comment

  (pprint (it/read-data (first examples)))

  ;; Execute this in order to run the algorithm on every example in
  ;; resources/examples. The results will be printed at the REPL and
  ;; visualizations will be output as PNG and HTML files in the project root
  ;; directory.
  (refresh)
  (doseq [f examples]
    (run-on-file f skill-cost algo-params))

)
