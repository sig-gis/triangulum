(ns magellan.core
  (:require [schema.core :as s]
            [clojure.java.io :as io])
  (:import (org.geotools.coverage.grid GridCoverage2D GridGeometry2D
                                       RenderedSampleDimension GridCoverageFactory)
           (org.geotools.coverage.grid.io GridFormatFinder AbstractGridFormat)
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
           (org.opengis.parameter GeneralParameterValue)
           (java.awt.image RenderedImage)))

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

(s/defn to-raster :- Raster
  [coverage :- GridCoverage2D]
  (let [image (.getRenderedImage coverage)
        crs   (.getCoordinateReferenceSystem coverage)]
    (map->Raster
     {:coverage   coverage
      :image      image
      :crs        crs
      :projection (CRS/getMapProjection crs)
      :envelope   (.getEnvelope coverage)
      :grid       (.getGridGeometry coverage)
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
  (ReferencingFactoryFinder/addAuthorityFactory
   (PropertyAuthorityFactory.
    (ReferencingFactoryContainer.
     (Hints. Hints/CRS_AUTHORITY_FACTORY PropertyAuthorityFactory))
    (Citations/fromName authority-name)
    (io/resource filename)))
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
    (to-raster (.create (GridCoverageFactory.) name float-matrix envelope))))

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
  (to-raster (.crop Operations/DEFAULT (:coverage raster) envelope)))


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
  (let [band (nth (:bands raster) band-num)]
    {:min    (.getMinimumValue band)
     :max    (.getMaximumValue band)
     :nodata (.getNoDataValues band)}))

;;; ======================== Usage examples below here =============================

(comment
  (def asp-raster (read-raster "/home/kcheung/Work/magellan/BackscatterA_8101_2004_OffshoreSanFrancisco.tif"))
  (def fmod-iet (read-raster "/home/gjohnson/tmp/fuel_models/FMOD_IET_veg2015.tif"))
  (def fmod-reax (read-raster "/home/gjohnson/tmp/fuel_models/FMOD_REAX_v2005.tif"))
  (def lw-avg-20km (read-raster "/home/gjohnson/tmp/fuel_moisture_update/lw_avg_20km.tif"))
  (s/explain Raster)
  (s/validate Raster asp-raster)
  (s/validate Raster fmod-iet)
  (s/validate Raster fmod-reax)
  (s/validate Raster lw-avg-20km)
  (def fmod-iet-reprojected (reproject-raster fmod-iet (:crs fmod-reax)))
  (def fmod-iet-reprojected-and-cropped (crop-raster fmod-iet-reprojected (:envelope fmod-reax)))
  (s/validate Raster fmod-iet-reprojected)
  (s/validate Raster fmod-iet-reprojected-and-cropped)
  (register-new-crs-definitions-from-properties-file! "CALFIRE" "custom_projections.properties")
  (write-raster asp-raster "/home/gjohnson/asp.tif")
  (write-raster fmod-iet "/home/gjohnson/fmod-iet.tif")
  (doseq [x (range 0 84)]
    (println (map (comp #(.getSampleDouble (.getData %) x y 0) :image)
                  [fire-spread-raster flame-length-raster fire-line-intensity-raster])))
  (def fire-spread
    (read-string (slurp "/home/gjohnson/fire_spread_111-207_25_25.tif.clj")))
  (def fire-spread-raster
    (matrix-to-raster "fire-spread-matrix" fire-spread test-envelope))
  (let [data (.getData (:image fire-spread-raster))]
    (filter pos? (for [x (range 0 84) y (range 0 85)]
                   (.getSampleFloat data x y 0))))

  )
