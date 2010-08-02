(ns leiningen.multi
  (:use [leiningen.deps :only [deps]])
  (:require [leiningen.test]))

(def task-whitelist ["deps" "test" "run" "compile" "jar" "uberjar"])

;; http://thinkrelevance.com/blog/2009/08/12/rifle-oriented-programming-with-clojure-2.html
(defn- indexed
  [coll]
  (map vector (iterate inc 0) coll))

(defn- multi-library-path
  [project]
  ;; Should the path be relative to the project root or the cwd?
  ;; The defaults in leiningen.core/defproject choose the latter, so I will as
  ;; well, but it seems incorrect.
  ;; TODO: Verify
  (or (:multi-library-path project)
      (str (:root project) "/multi-lib")))

(defn- project-for-set
  [project index deps]
  (merge project {:library-path (str (multi-library-path project) "/set" index)
		  :dependencies deps}))

(defn- run-multi-task
  ([task-fn project]
     (run-multi-task task-fn project nil))
  ([task-fn project delimiter-fn]
     (doseq [[index deps] (indexed (:multi-deps project))]
       (when delimiter-fn (delimiter-fn index deps))
       (task-fn (project-for-set project index deps)))))

(defn- run-deps
  [project & args]
  (println "Fetching base dependencies:" (:dependencies project))
  (apply deps project args)
  (run-multi-task #(deps % true)
		  project
		  #(println (format "Fetching dependencies set %d: %s" %1 %2))))

(defn- run-test
  [project & args]
  (println "Testing against base dependencies:" (:dependencies project))
  (apply leiningen.test/test project args)
  (run-multi-task leiningen.test/test
		  project
		  #(println (format "Testing against dependencies set %d: %s" %1 %2))))

(defn multi
  [project task & args]
  (cond (= task "deps") (apply run-deps project args)
	(= task "test") (apply run-test project args)))