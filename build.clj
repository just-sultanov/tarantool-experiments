(ns build
  (:refer-clojure :exclude [test])
  (:require
    [clojure.pprint :as pprint]
    [clojure.set :as set]
    [clojure.tools.build.util.file :as file]
    [org.corfield.build :as bb]))


(def defaults
  {:src-dirs       ["src/main/clojure"]
   :resource-dirs  ["src/main/resources"]
   :lib            'tarantool-experiments
   :target         "target"
   :coverage-dir   "coverage"
   :jar-file       "target/tarantool-experiments.jar"
   :build-meta-dir "src/main/resources/tarantool-experiments"})



(defn pretty-print
  [x]
  (binding [pprint/*print-right-margin* 130]
    (pprint/pprint x)))



(defn with-defaults
  [opts]
  (merge defaults opts))



(defn extract-meta
  [opts]
  (-> opts
      (select-keys [:lib
                    :version
                    :build-number
                    :build-timestamp
                    :git-url
                    :git-branch
                    :git-sha])
      (set/rename-keys {:lib :module})
      (update :module str)))



(defn write-meta
  [opts]
  (let [dir (:build-meta-dir opts)]
    (file/ensure-dir dir)
    (->> opts
         (extract-meta)
         (pretty-print)
         (with-out-str)
         (spit (format "%s/build.edn" dir)))))



(defn outdated
  [opts]
  (-> opts
      (with-defaults)
      (bb/run-task [:nop :outdated])))



(defn outdated:upgrade
  [opts]
  (-> opts
      (with-defaults)
      (bb/run-task [:nop :outdated :outdated/upgrade])))



(defn clean
  [opts]
  (-> opts
      (with-defaults)
      (bb/clean)))



(defn repl
  [opts]
  (let [opts (with-defaults opts)]
    (write-meta opts)
    (bb/run-task opts [:test :develop])))



(defn test
  [opts]
  (let [opts (with-defaults opts)]
    (write-meta opts)
    (bb/run-task opts [:test])))



(defn jar
  [opts]
  (let [opts (with-defaults opts)]
    (write-meta opts)
    (-> opts
        (assoc :scm {:url (:git-url opts)
                     :tag (:version opts)})
        (bb/jar))))



(defn install
  [opts]
  (-> opts
      (with-defaults)
      (bb/install)))



(defn deploy
  [opts]
  (-> opts
      (with-defaults)
      (bb/deploy)))
