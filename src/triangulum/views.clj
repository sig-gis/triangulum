(ns triangulum.views
  (:require [clojure.data.json   :as json]
            [clojure.java.io     :as io]
            [clojure.string      :as str]
            [clj-http.client     :as client]
            [cognitect.transit   :as transit]
            [triangulum.git      :refer [current-version]]
            [hiccup.page         :refer [html5 include-css include-js]]
            [triangulum.config   :refer [get-config]]
            [triangulum.database :refer [call-sql]])
  (:import
   java.io.ByteArrayOutputStream))

(defn kebab->camel [kebab]
  (let [pieces (str/split kebab #"-")]
    (apply str (first pieces) (map str/capitalize (rest pieces)))))

(defn find-bundle-js-files [page]
  (let [src-file (format "src/js/%s.jsx" page)
        manifest (if (= "dev" (get-config :server :mode))
                   (loop [resp    (try
                                    (client/get "http://localhost:5173/manifest.json"
                                                {:accept :json})
                                    (catch Exception e nil))
                          counter 0]
                     (cond
                       resp
                       (-> resp
                           :body
                           (json/read-str)
                           (get src-file))

                       (> counter 50)
                       (throw (ex-info "can't find manifest.json" {}))

                       :else
                       (do (Thread/sleep 200)
                           (recur resp (inc counter)))))
                   (-> (slurp "dist/public/manifest.json")
                       (json/read-str)
                       (get src-file)))

        entrypoint-file (str "/" (get manifest "file"))
        asset-files     (mapv #(str/replace % #"^_" "/assets/")
                              (get manifest "imports"))]
    (conj asset-files entrypoint-file)))

(defn find-bundle-css-files []
  (let [manifest        (-> (slurp "dist/public/manifest.json")
                            (json/read-str))
        asset-css-files (->> manifest
                             (filter (fn [[k _v]] (str/includes? k ".css")))
                             (map (fn [[_k v]] (str "/" (get v "file")))))]
    asset-css-files))

(defn head [bundle-js-files bundle-css-files lang]
  (let [title       {:en "Colombian Mining Monitoring"
                     :es "Monitoreo Minero Colombiano"}
        description {:en "Colombian Mining Monitoring (CoMiMo) is an online mining monitoring application that uses machine learning and satellite imagery to alert government authorities, NGOs and concerned citizens about possible mining activities anywhere in Colombia, and enables them to assess the location, lawfulness and potential impacts to the environment of those mines."
                     :es "Monitoreo Minero Colombiano (CoMiMo) es una aplicación de monitoreo minero en línea que utiliza aprendizaje automático e imágenes satelitales para alertar a las autoridades gubernamentales, ONGs y ciudadanos preocupados sobre posibles actividades mineras en cualquier lugar de Colombia, y les permite evaluar la ubicación, la legalidad y los posibles impactos al entorno de esas minas."}
        keywords    {:en "colombian mining monitoring, comimo, satellite imagery, illegal mining, machine learning, image analysis, detection, crowdsourcing"
                     :es "monitoreo minero colombiano, comimo, imágenes satelitales, minería ilegal, aprendizaje automático, análisis de imágenes, detección, crowdsourcing"}]
    [:head
     [:title (title lang)]
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport" :content "width=device-width, user-scalable=no"}]
     [:meta {:name "description" :content (description lang)}]
     [:meta {:name "keywords" :content (keywords lang)}]
                                        ; Favicon entries
     [:link {:rel "apple-touch-icon" :sizes "180x180" :href "/favicon/apple-touch-icon.png"}]
     [:link {:rel "icon" :type "image/png" :sizes "32x32" :href "/favicon/favicon-32x32.png"}]
     [:link {:rel "icon" :type "image/png" :sizes "16x16" :href "/favicon/favicon-16x16.png"}]
     [:link {:rel "manifest" :href "/favicon/site.webmanifest"}]
     [:link {:rel "mask-icon" :color "#5bbad5" :href "/favicon/safari-pinned-tab.svg"}]
     [:link {:rel "shortcut icon" :href "/favicon/favicon.ico"}]
     [:meta {:name "msapplication-TileColor" :content "#ffc40d"}]
     [:meta {:name "msapplication-config" :content "/favicon/browserconfig.xml"}]
     [:meta {:name "theme-color" :content "#ffffff"}]
                                        ; end Favicon

     (when-let [ga-id (get-config :ga-id)]
       (list [:script {:async true :src (str "https://www.googletagmanager.com/gtag/js?id=" ga-id)}]
             [:script (str "window.dataLayer = window.dataLayer || []; function gtag() {dataLayer.push(arguments);} gtag('js', new Date()); gtag('config', '" ga-id "', {'page_location': location.host + location.pathname});")]))
     (apply include-css
            "/css/comimo_global.css"
            bundle-css-files)
     (include-js "https://www.gstatic.com/charts/loader.js"
                 "/js/jquery-3.4.1.min.js")
     (if (= "dev" (get-config :server :mode))
       (list
         [:script {:type "module"}
          "// import RefreshRuntime from 'http://localhost:5173/@react-refresh'
         // RefreshRuntime.injectIntoGlobalHook(window)
         window.$RefreshReg$ = () => {}
         window.$RefreshSig$ = () => (type) => type
         window.__vite_plugin_react_preamble_installed__ = true"]
         [:script {:type "module" :src "http://localhost:5173/@vite/client"}]
         (map (fn [f] [:script {:type "module" :src (str "http://localhost:5173" f)}])
              bundle-js-files))
       (map (fn [f] [:script {:type "module" :src f}])
            bundle-js-files))]))

(defn uri->page [uri]
  (->> (str/split uri #"/")
       (remove str/blank?)
       (first)
       (kebab->camel)))

(defn js-init [entry-file params]
  (let [js-params  (-> params
                       (assoc
                         :mapboxToken     (get-config :mapbox-token)
                         :mapquestKey     (get-config :mapquest-key)
                         :versionDeployed (current-version))
                       (json/write-str))
        script-str (str "import {pageInit} from \"" entry-file "\";"
                        "window.onload = function () { pageInit(" js-params "); };")]
    [:script {:type "module"}
     script-str]))

(defn- announcement-banner []
  (let [announcement (-> (slurp "announcement.txt")
                         (str/split #"\n"))]            ; TODO This will be moved to the front end for better UX.
    (when-not (empty? (first announcement))
      [:div#banner {:style {:background-color "#f96841"
                            :box-shadow       "3px 1px 4px 0 rgb(0, 0, 0, 0.25)"
                            :color            "#ffffff"
                            :display          (if (pos? (count announcement)) "block" "none")
                            :margin           "0px"
                            :padding          "5px"
                            :position         "fixed"
                            :text-align       "center"
                            :top              "0"
                            :right            "0"
                            :left             "0"
                            :width            "100vw"
                            :z-index          "10000"}}
       [:script {:type "text/javascript"}
        "setTimeout (function () {document.getElementById ('banner') .style.display='none'}, 10000);"]
       (map (fn [line]
              [:p {:style {:font-size   "18px"
                           :font-weight "bold"
                           :margin      "0 30px 0 0"}
                   :key   line}
               line])
            announcement)
       [:button {:style   {:align-items      "center"
                           :background-color "transparent"
                           :border-color     "#ffffff"
                           :border-radius    "50%"
                           :border-style     "solid"
                           :border-width     "2px"
                           :cursor           "pointer"
                           :display          "flex"
                           :height           "25px"
                           :padding          "0"
                           :position         "fixed"
                           :right            "10px"
                           :top              "5px"
                           :width            "25px"}
                 :onClick "document.getElementById('banner').style.display='none'"}
        [:svg {:width "24px" :height "24px" :viewBox "0 0 48 48" :fill "#ffffff"}
         [:path {:d "M38 12.83l-2.83-2.83-11.17 11.17-11.17-11.17-2.83 2.83 11.17 11.17-11.17 11.17 2.83 2.83
                     11.17-11.17 11.17 11.17 2.83-2.83-11.17-11.17z"}]]]])))

(defn render-page [uri]
  (fn [request]
    (let [page             (uri->page uri)
          bundle-js-files  (find-bundle-js-files page)
          bundle-css-files (find-bundle-css-files)
          user-id          (-> request :params :userId)
          user             (first (call-sql "get_user_information" user-id))
          lang             (:default-lang user :en)]
      {:status  200
       :headers {"Content-Type" "text/html"}
       :body    (html5
                  (head (butlast bundle-js-files) bundle-css-files lang)
                  [:body
                   (if (seq bundle-js-files)
                     [:section
                      ;; TODO These will be moved to the front end for better UX.
                      (when-let [flash-message (get-in request [:params :flash_message])]
                        ;; TODO: we don't depend on class definition anymore
                        [:p {:class "alert"} flash-message])
                      (when (.exists (io/as-file "announcement.txt"))
                        (announcement-banner))
                      [:div#main-container]]
                     [:label "No JS bundle files found. Check if your bundler is running, or wait for it to finish compiling."])
                   (js-init (last bundle-js-files) (:params request))])})))

(defn not-found-page [request]
  (-> request
      ((render-page "/page-not-found"))
      (assoc :status 404)))

(defn body->transit [body]
  (let [out    (ByteArrayOutputStream. 4096)
        writer (transit/writer out :json)]
    (transit/write writer body)
    (.toString out)))
