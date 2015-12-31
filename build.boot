(task-options!
 pom  {:project     'lambdatronic/magellan
       :version     "0.1.0"
       :description "Minimal API for Raster & Vector Manipulation over GeoTools"}
 repl {:init-ns     'magellan.core
       :eval        '(set! *warn-on-reflection* true)})

(set-env!
 :source-paths   #{"src"}
 :resource-paths #{"resources"}
 :dependencies   '[[org.clojure/clojure       "1.7.0"]
                   [org.geotools/gt-shapefile "13.2"]
                   [org.geotools/gt-swing     "13.2"]
                   [org.geotools/gt-epsg-hsql "13.2"]
                   [org.geotools/gt-geotiff   "13.2"]
                   [org.geotools/gt-image     "13.2"]
                   [org.geotools/gt-wms       "13.2"]
                   [org.geotools/gt-coverage  "13.2"]
                   [prismatic/schema          "1.0.4"]
                   [junit/junit               "4.11" :scope "test"]]
 :repositories   #(conj %
                        ["java.net"  "http://download.java.net/maven/2"]
                        ["osgeo.org" "http://download.osgeo.org/webdav/geotools/"]))

(deftask build
  "Build my project."
  []
  (comp (pom) (jar)))
