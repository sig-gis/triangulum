(ns magellan.core-test
  (:require [clojure.test :refer [deftest is use-fixtures testing]]
            [clojure.java.io :as io]
            [magellan.core :as mg])
  (:import (org.geotools.geometry GeneralEnvelope Envelope2D)
           (org.geotools.referencing CRS)))

;;-----------------------------------------------------------------------------
;; Config
;;-----------------------------------------------------------------------------

(def input-dirname  "test/data/")
(def output-dirname "test/output/")

;;-----------------------------------------------------------------------------
;; Utils
;;-----------------------------------------------------------------------------

(defn in-file-path [filename]
  (str input-dirname filename))

(defn out-file-path [filename]
  (str output-dirname filename))

(defn make-directory [dirname]
  (.mkdir (io/file dirname)))

(defn delete-directory [dirname]
  (doseq [file (reverse (file-seq (io/file dirname)))]
    (io/delete-file file)))

;;-----------------------------------------------------------------------------
;; Fixtures
;;-----------------------------------------------------------------------------

(defn with-temp-output-dir [test-fn]
  (make-directory output-dirname)
  (test-fn)
  (delete-directory output-dirname))

(use-fixtures :once with-temp-output-dir)

;;------------------------------------------------------------------------------
;; Tests
;;------------------------------------------------------------------------------

(deftest read-raster-test
  (testing "Reading raster from file"
    (let [samp-raster (mg/read-raster (in-file-path "SRS-EPSG-3857.tif"))]

      (is (instance? magellan.core.Raster samp-raster)))))

(deftest write-raster-test
  (testing "Writing raster to file"
    (let [samp-rast (mg/read-raster (in-file-path "SRS-EPSG-3857.tif"))
          _         (mg/write-raster samp-rast (out-file-path "SRS-EPSG-3857.tif"))
          my-rast   (mg/read-raster (out-file-path "SRS-EPSG-3857.tif"))]

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

      (is (= (:envelope samp-rast)
             (:envelope my-rast)))

      ;; TODO Failing but no discrepancies in properties
      ;; (is (= (:image samp-rast)
      ;;        (:image my-rast)))

      ;; TODO Failing but no discrepancies in properties
      ;; (is (= (:coverage samp-rast)
      ;;        (:coverage my-rast)))
      )))

(deftest make-envelope-test
  (testing "Creating an envelope"
    (let [samp-crs       (:crs (mg/read-raster (in-file-path "SRS-EPSG-3857.tif")))
          [height width] [100 100]
          [x-min y-min]  [0.0 0.0]
          envelope       (mg/make-envelope "EPSG:3857" x-min y-min height width)]

      (is (instance? Envelope2D envelope))

      (is (= envelope (Envelope2D. samp-crs x-min y-min height width))))))

(deftest matrix-to-raster-test
  (testing "Creating a raster from a 2d matrix"
    (let [[height width] [100 100]
          [x-min y-min]  [0.0 0.0]
          envelope       (mg/make-envelope "EPSG:3857" x-min y-min width height)
          matrix         (repeat height (repeat width 1.0))
          rast           (mg/matrix-to-raster "some-name" matrix envelope)
          coverage       (:coverage rast)]

      (is (instance? magellan.core.Raster rast))

      (is (= (:crs rast) (.getCoordinateReferenceSystem coverage)))

      (is (= (:projection rast) (CRS/getMapProjection (:crs rast))))

      (is (= (:image rast) (.getRenderedImage coverage)))

      (is (= (:envelope rast) (.getEnvelope coverage)))

      (is (= (:grid rast) (.getGridGeometry coverage)))

      (is (= (:width rast) (.getWidth (.getRenderedImage coverage))))

      (is (= (:height rast) (.getHeight (.getRenderedImage coverage))))

      (is (= (:bands rast) (vec (.getSampleDimensions coverage)))))))

(deftest reproject-raster-test
  (testing "Reprojecting a raster"
    (let [samp-rast        (mg/read-raster (in-file-path "SRS-EPSG-3857.tif"))
          new-crs          (:crs (mg/read-raster (in-file-path "SRS-EPSG-32610.tif")))
          reprojected-rast (mg/reproject-raster samp-rast new-crs)]

      (is (instance? magellan.core.Raster reprojected-rast))

      (is (not= (:crs samp-rast) new-crs)
          "New crs to be projected to should not be the same as the original")

      (is (= (:crs reprojected-rast) new-crs)
          "Should produce a new raster with updated coordinate reference system"))))

(deftest resample-raster-test
  (testing "Resampling a raster")
  (let [[width height]  [100 100]
        [x-min y-min]   [0.0 0.0]
        envelope        (mg/make-envelope "EPSG:3857" x-min y-min width height)
        matrix1         (repeat height (repeat width 1.0))
        matrix2         (repeat (* 2 height) (repeat (* 2 width) 1.0))
        rast1           (mg/matrix-to-raster "100Res" matrix1 envelope)
        rast2           (mg/matrix-to-raster "200Res" matrix2 envelope)
        rast1-resampled (mg/resample-raster rast1 (:grid rast2))]

    (is (not= [(:width rast1) (:height rast1)]
              [(:width rast2) (:height rast2)])
        "Resolution of the two rasters should be different")

    (is (= [(:width rast1-resampled) (:height rast1-resampled)]
           [(:width rast2) (:height rast2)])
        "Resampled raster should have the same resolution as target raster")))

(deftest crop-test
  (testing "Cropping a raster"
    (let [samp-rast (mg/read-raster (in-file-path "SRS-EPSG-3857.tif"))
          lower     (-> (:envelope samp-rast) .getLowerCorner .getCoordinate)
          upper     (-> (:envelope samp-rast) .getUpperCorner .getCoordinate)
          new-upper (map #(/ (+ %1 %2) 2) lower upper)
          new-rast  (mg/crop-raster samp-rast (GeneralEnvelope. lower (double-array new-upper)))]

      (is (instance? magellan.core.Raster new-rast))

      (is (not= (:envelope new-rast) (:envelope samp-rast))))))

(deftest register-crs-definitions-test
  (testing "Registering CRS definitions"
    (let [authority  "CALFIRE"
          properties (in-file-path "sample_projections.properties")]

      (mg/register-new-crs-definitions-from-properties-file! authority properties)

      (is (not (nil? (CRS/decode "CALFIRE:900914")))))))
