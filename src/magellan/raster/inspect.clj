(ns magellan.raster.inspect
  (:require [magellan.core :refer [crs-to-srid]])
  (:import (java.awt.image DataBuffer RenderedImage)
           org.geotools.coverage.GridSampleDimension
           org.geotools.coverage.grid.GridCoverage2D
           magellan.core.Raster))

;;=====================================================================
;; Raster inspection functions (ALPHA)
;;=====================================================================

(defn describe-image [^RenderedImage image]
  {:height (.getHeight image)
   :width  (.getWidth image)
   :bands  (.getNumBands (.getSampleModel image))
   :origin {:x (.getMinX image)
            :y (.getMinY image)}
   :tile   {:height (.getTileHeight image)
            :width  (.getTileWidth image)
            :min    {:x (.getMinTileX image)
                     :y (.getMinTileY image)}
            :max    {:x (.getMaxTileX image)
                     :y (.getMaxTileY image)}
            :total  {:x (.getNumXTiles image)
                     :y (.getNumYTiles image)}
            :offset {:x (.getTileGridXOffset image)
                     :y (.getTileGridYOffset image)}}})

(defn describe-envelope [envelope]
  (let [dimensions [:x :y :z]]
    (reduce (fn [acc ordinate]
              (assoc acc
                     (dimensions ordinate)
                     {:min  (.getMinimum envelope ordinate)
                      :max  (.getMaximum envelope ordinate)
                      :span (.getSpan    envelope ordinate)}))
            {}
            (range (.getDimension envelope)))))

(defn describe-band [^GridSampleDimension band]
  {:description (str (.getDescription band))
   :type        (str (.getSampleDimensionType band))
   ;; FIXME: missing band info when writing raster to disk via matrix-to-raster
   ;; :min         (.getMinimum (.getRange band))
   ;; :max         (.getMaximum (.getRange band))
   :no-data     (.getNoDataValues band)
   :offset      (.getOffset band)
   :scale       (.getScale band)
   :units       (.getUnits band)
   ;; FIXME: missing band info when writing raster to disk via matrix-to-raster
   ;; :categories  (reduce (fn [acc cat]
   ;;                        (let [range (.getRange cat)]
   ;;                          (assoc acc
   ;;                                 (str (.getName cat))
   ;;                                 {:min (.getMinimum range)
   ;;                                  :max (.getMaximum range)})))
   ;;                      {}
   ;;                      (.getCategories band))
   })

(defn describe-raster [^Raster raster]
  (let [image    (describe-image (:image raster))
        envelope (describe-envelope (:envelope raster))
        bands    (mapv describe-band (:bands raster))
        srid     (crs-to-srid (:crs raster))]
    {:image    image
     :envelope envelope
     :bands    bands
     :srid     srid}))

;; NOTE: .getSamples isn't supported for byte-array or short-array, so
;; we substitute int-array instead. If the type cannot be determined,
;; we fall back to using a double array.
(defn- get-typed-array [data-buffer-type]
  (condp = data-buffer-type
    DataBuffer/TYPE_BYTE      int-array
    DataBuffer/TYPE_USHORT    int-array
    DataBuffer/TYPE_SHORT     int-array
    DataBuffer/TYPE_INT       int-array
    DataBuffer/TYPE_FLOAT     float-array
    DataBuffer/TYPE_DOUBLE    double-array
    DataBuffer/TYPE_UNDEFINED double-array
    double-array))

(defn extract-matrix [^Raster raster]
  (let [image            (:image raster)
        {:keys [height
                width
                bands
                origin]} (describe-image image)
        {min-x :x
         min-y :y}       origin
        data             (.getData image)
        typed-array      (get-typed-array (.getDataType (.getDataBuffer data)))]
    (into-array (for [b (range bands)]
                  (into-array (for [y (range min-y (+ min-y height))]
                                (.getSamples data min-x y width 1 b (typed-array width))))))))

(defn show-raster [raster]
  (let [^GridCoverage2D coverage (:coverage raster)]
    (.show coverage nil)))
