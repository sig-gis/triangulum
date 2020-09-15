(ns magellan.core
  (:require [schema.core :as s]
            [clojure.java.io :as io])
  (:import (java.net URL)
           (java.awt.image RenderedImage)
           (org.geotools.coverage.grid GridCoordinates2D
                                       GridCoverageFactory
                                       GridEnvelope2D
                                       GridGeometry2D
                                       RenderedSampleDimension
                                       GridCoverage2D)
           (org.geotools.coverage.grid.io GridFormatFinder AbstractGridFormat)
           (org.geotools.coverage GridSampleDimension)
           (org.geotools.referencing CRS ReferencingFactoryFinder)
           (org.geotools.referencing.factory PropertyAuthorityFactory
                                             ReferencingFactoryContainer)
           (org.geotools.referencing.operation.projection MapProjection)
           (org.geotools.metadata.iso.citation Citations)
           (org.geotools.util.factory Hints)
           (org.geotools.geometry GeneralEnvelope Envelope2D)
           (org.geotools.coverage.processing Operations)
           (org.geotools.gce.geotiff GeoTiffWriter GeoTiffWriteParams)
           (org.opengis.referencing.crs CoordinateReferenceSystem)
           (org.opengis.parameter GeneralParameterValue)))

(s/defrecord Raster
    [coverage   :- GridCoverage2D
     image      :- RenderedImage
     crs        :- CoordinateReferenceSystem
     projection :- (s/maybe MapProjection)
     envelope   :- GeneralEnvelope
     grid       :- GridGeometry2D
     width      :- s/Int
     height     :- s/Int
     bands      :- [RenderedSampleDimension]])

(defn- evaluate-at
  [^GridCoverage2D coverage x y]
  (first (vec (.evaluate coverage (GridCoordinates2D. x y) (float-array 1)))))

(defn- get-bounds
  [^GridEnvelope2D envelope dimension]
  [(.getLow envelope dimension) (.getHigh envelope dimension)])

(defn- evaluate-all
  [coverage ^GridGeometry2D grid]
  (let [grid-envelope (.getGridRange2D grid)
        [x-min x-max] (get-bounds grid-envelope 0)
        [y-min y-max] (get-bounds grid-envelope 1)]
    (for [y (range y-min (inc y-max))]
      (mapv #(evaluate-at coverage % y) (range x-min (inc x-max))))))

(s/defn to-raster :- Raster
  [coverage :- GridCoverage2D]
  (let [image  (.getRenderedImage coverage)
        crs    (.getCoordinateReferenceSystem coverage)
        grid   (.getGridGeometry coverage)]

    (map->Raster
      {:coverage   coverage
       :matrix     (evaluate-all coverage grid)
       :image      image
       :crs        crs
       :projection (CRS/getMapProjection crs)
       :envelope   (.getEnvelope coverage)
       :grid       grid
       :width      (.getWidth image)
       :height     (.getHeight image)
       :bands      (vec (.getSampleDimensions coverage))})))

(s/defn read-raster :- Raster
  [filename :- s/Str]
  (let [file (io/file filename)]
    (if (.exists file)
      (try (-> file
               (GridFormatFinder/findFormat)
               (.getReader file)
               (.read nil)
               (to-raster))
           (catch Exception e
             (println "Cannot read raster. Exception:" (class e))))
      (println "Cannot read raster. No such file:" filename))))

(s/defn srid-to-crs :- CoordinateReferenceSystem
  [srid-code :- s/Str]
  (CRS/decode srid-code))

(s/defn wkt-to-crs :- CoordinateReferenceSystem
  [wkt :- s/Str]
  (CRS/parseWKT wkt))

(s/defn crs-to-srid :- s/Str
  [crs :- CoordinateReferenceSystem]
  (CRS/lookupIdentifier crs true))

(s/defn crs-to-wkt :- s/Str
  [crs :- CoordinateReferenceSystem]
  (.toWKT crs))

(s/defn register-new-crs-definitions-from-properties-file! :- s/Any
  [authority-name :- s/Str
   filename       :- s/Str]
  (let [^URL url (if (instance? URL filename)
                   filename
                   (io/as-url (io/file filename)))]
    (ReferencingFactoryFinder/addAuthorityFactory
      (PropertyAuthorityFactory.
        (ReferencingFactoryContainer.
          (Hints. Hints/CRS_AUTHORITY_FACTORY PropertyAuthorityFactory))
        (Citations/fromName authority-name)
        url)))
  (ReferencingFactoryFinder/scanForPlugins))

(s/defn make-envelope :- Envelope2D
  [srid       :- s/Str
   upperleftx :- Double
   upperlefty :- Double
   width      :- Double
   height     :- Double]
  (Envelope2D. (srid-to-crs srid) upperleftx upperlefty width height))

(s/defn matrix-to-raster :- GridCoverage2D
  [name     :- s/Str
   matrix   :- [[double]]
   envelope :- Envelope2D]
  (let [float-matrix (into-array (map float-array matrix))]
    (to-raster (.create (GridCoverageFactory.)
                        ^String name
                        ^"[[F" float-matrix
                        envelope))))

;; FIXME: Throws a NoninvertibleTransformException when reprojecting to EPSG:4326.
(s/defn reproject-raster :- Raster
  [raster :- Raster
   crs    :- CoordinateReferenceSystem]
  (to-raster (.resample Operations/DEFAULT (:coverage raster) crs)))

(s/defn resample-raster :- Raster
  [raster :- Raster
   grid   :- GridGeometry2D]
  (to-raster (.resample Operations/DEFAULT (:coverage raster) nil grid nil)))

(s/defn crop-raster :- Raster
  [raster   :- Raster
   envelope :- GeneralEnvelope]
  (to-raster (.crop Operations/DEFAULT ^GridCoverage2D (:coverage raster) envelope)))

;; FIXME: Throws a NullPointerException when writing a resampled coverage.
;; FIXME: Parameterize the compression and tiling operations.
;; REFERENCE: http://svn.osgeo.org/geotools/trunk/modules/plugin/geotiff/src/test/java/org/geotools/gce/geotiff/GeoTiffWriterTest.java
(s/defn write-raster :- s/Any
  [raster   :- Raster
   filename :- s/Str]
  (let [writer (GeoTiffWriter. (io/file filename))
        params (-> writer
                   (.getFormat)
                   (.getWriteParameters))]
    (-> params
        (.parameter (str (.getName AbstractGridFormat/GEOTOOLS_WRITE_PARAMS)))
        (.setValue (doto (GeoTiffWriteParams.)
                     (.setCompressionMode GeoTiffWriteParams/MODE_EXPLICIT)
                     (.setCompressionType "LZW")
                     (.setCompressionQuality 0.5)
                     (.setTilingMode GeoTiffWriteParams/MODE_EXPLICIT)
                     (.setTiling 256 16))))
    ;; Write the GeoTIFF to disk
    (try (.write writer
                 (:coverage raster)
                 (into-array GeneralParameterValue (.values params)))
         (catch Exception e
           (println "Cannot write raster. Exception:" (class e))))))

(s/defn raster-band-stats :- {:min s/Num :max s/Num :nodata (s/maybe s/Num)}
  [raster   :- Raster
   band-num :- s/Int]
  (let [^GridSampleDimension band (nth (:bands raster) band-num)]
    {:min    (.getMinimumValue band)
     :max    (.getMaximumValue band)
     :nodata (.getNoDataValues band)}))
