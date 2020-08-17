(ns test.core-test
  (:require [clojure.test :refer :all]
            [magellan.core :refer :all]
            [clojure.java.io :as io])
  (:import
   (org.geotools.coverage.processing Operations)))

;; -----------------------------------------------------------------------------
;; Utils
;; -----------------------------------------------------------------------------

(defn file-path
  [filename]
  (str "test/data/" filename))

(defn clear-dir [fname]
  (let [func (fn [func f]
               (when (.isDirectory f)
                 (doseq [f2 (.listFiles f)]
                   (func func f2)))
               (clojure.java.io/delete-file f))]
    (func func (clojure.java.io/file fname))))

;; -----------------------------------------------------------------------------
;; Fixures
;; -----------------------------------------------------------------------------

(defn setup-once
  []
  (.mkdir (java.io.File. "test/output")))

(defn del-recur [fname]
  (let [f (clojure.java.io/file fname)]
    (when (.isDirectory f)
      (doseq [f2 (.listFiles f)]
        (del-recur f2)))
    (clojure.java.io/delete-file f)))

(defn teardown-once
  []
  (clear-dir "test/output"))

(defn fixture-once [test-fn]
  (setup-once)
  (test-fn)
  (teardown-once))

(use-fixtures :once fixture-once)

;; -----------------------------------------------------------------------------
;; Tests
;; -----------------------------------------------------------------------------

(deftest read-raster-test
  (testing "Reading raster from file"
    (let [sample-raster (read-raster (file-path "HYP_50M_SR.tif"))]
      (is (instance? magellan.core.Raster sample-raster)))))


(deftest write-raster-test
  (testing "write a raster to file"
    (let [samp-rast (read-raster (file-path "NE1_50M_SR_W.tif"))
          _         (write-raster samp-rast "test/output/raster.tif")
          my-rast   (read-raster "test/output/raster.tif")]
      (is (not (nil? my-rast)))
      (is (instance? magellan.core.Raster my-rast)))))


(deftest reproject-raster-test
  (testing "reproject raster"
    (let [samp1-rast (read-raster (file-path "HYP_50M_SR.tif"))
          samp2-rast (read-raster (file-path "NE1_50M_SR_W.tif"))
          new-raster (reproject-raster samp1-rast (:crs samp2-rast))]
      (is (instance? magellan.core.Raster new-raster))
      ;; TODO Verify operation
      )))


(deftest reproject-and-crop-test
  (testing "reproject and crop raster"
    (let [samp-rast  (read-raster (file-path "NE1_50M_SR_W.tif"))
          new-raster (crop-raster samp-rast (:envelope samp-rast))]
      (is (instance? magellan.core.Raster new-raster))

      ;; TODO Verify operation
      )))



