(ns test.core-test
  (:require [clojure.test :refer :all]
            [magellan.core :refer :all]
            [clojure.java.io :as io])
  (:import
   (org.geotools.coverage.processing Operations)
   (org.geotools.geometry GeneralEnvelope)))

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
    (let [sample-raster (read-raster (file-path "SRS-EPSG-3857.tif"))]

      (is (instance? magellan.core.Raster sample-raster)))))


(deftest write-raster-test
  (testing "write a raster to file"
    (let [samp-rast (read-raster (file-path "SRS-EPSG-3857.tif"))
          _         (write-raster samp-rast "test/output/raster.tif")
          my-rast   (read-raster "test/output/raster.tif")]

      (is (not (nil? my-rast)))

      (is (instance? magellan.core.Raster my-rast))

      (is (= (:crs samp-rast)
             (:crs my-rast)))

      (is (= (:projection samp-rast)
             (:projection my-rast)))

      (is (= (:grid samp-rast)
             (:grid my-rast)))

      (is (= (:bands samp-rast)
             (:bands my-rast)))

      (is (= (:grid samp-rast)
             (:grid my-rast)))

      (is (= (:envelope samp-rast)
             (:envelope my-rast)))

      ;; TODO Failing
      (is (= samp-rast my-rast))
      )))


(deftest reproject-raster-test
  (testing "reproject raster"
    (let [samp-rast        (read-raster (file-path "SRS-EPSG-3857.tif"))
          new-crs          (:crs (read-raster (file-path "SRS-EPSG-32610.tif")))
          reprojected-rast (reproject-raster samp-rast new-crs)]

      (is (instance? magellan.core.Raster samp-rast))

      (is (not (= (:crs samp-rast) new-crs))
          "new crs to be projected to should not be the same as the original")

      (is (= (:crs reprojected-rast)
             new-crs)
          "should produce a new raster with updated coordinate reference system"))))


(deftest reproject-and-crop-test
  (testing "reproject and crop raster"
    (let [samp-rast (read-raster (file-path "SRS-EPSG-3857.tif"))
          lower     (-> (:envelope samp-rast) .getLowerCorner .getCoordinate)
          upper     (-> (:envelope samp-rast) .getUpperCorner .getCoordinate)
          new-upper (map #(/ (+ %1 %2) 2) lower upper)
          new-rast  (crop-raster samp-rast (GeneralEnvelope. lower (double-array new-upper)))]

      (is (instance? magellan.core.Raster new-rast))

      (is (not (= (:envelope new-rast) (:envelope samp-rast)))))))


(deftest matrix-to-raster-test
  (testing "Creating a raster from a 2d matrix"
    (let [envelope (:envelope (read-raster (file-path "SRS-EPSG-3857.tif")))
          height   100
          width    100
          envelope (make-envelope "EPSG:3857" 0.0 100.0 width height)
          matrix   (repeat height (repeat width 1.0))
          rast     (matrix-to-raster "some-name" matrix envelope)]
      (is (instance? magellan.core.Raster rast)))))
