(ns magellan.deploy
  (:import java.text.SimpleDateFormat
           java.util.Date)
  (:require [hf.depstar.uberjar :refer [build-jar]]
            [deps-deploy.deps-deploy :refer [deploy artifacts]]))

(defn- check-inputs [group-id artifact-id]
  (cond (not-every? #(and (string? %) (pos? (count %)))
                    [group-id artifact-id])
        "group-id and artifact-id must be non-empty strings."

        (not (and (System/getenv "CLOJARS_USERNAME")
                  (System/getenv "CLOJARS_PASSWORD")))
        "CLOJARS_USERNAME and CLOJARS_PASSWORD must be set."))

(defn -main [group-id artifact-id & _]
  (if-let [error-message (check-inputs group-id artifact-id)]
    (println "Error:" error-message)
    (let [software-version (.format (SimpleDateFormat. "yyyyMMdd") (Date.))
          jar-path         (str "target/" artifact-id "-" software-version ".jar")
          result           (build-jar {:jar         jar-path
                                       :jar-type    :thin
                                       :sync-pom    true
                                       :group-id    group-id
                                       :artifact-id artifact-id
                                       :version     software-version})]
      (if (:success result)
        (deploy {:installer    :clojars
                 :artifact-map (artifacts software-version ["pom.xml" jar-path])
                 :coordinates  [(symbol group-id artifact-id) software-version]})
        (println "Failed to create JAR:" (:reason result))))))
