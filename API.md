# Table of contents
-  [`triangulum.build-db`](#triangulum.build-db)
    -  [`-main`](#triangulum.build-db/-main) - A set of tools for building and maintaining the project database with Postgres.
-  [`triangulum.cli`](#triangulum.cli)  - Provides a command-line interface (CLI) for Triangulum applications.
    -  [`get-cli-options`](#triangulum.cli/get-cli-options) - Checks for a valid call from the CLI and returns the users options.
-  [`triangulum.config`](#triangulum.config) 
    -  [`-main`](#triangulum.config/-main) - Configuration management.
    -  [`get-config`](#triangulum.config/get-config) - Retrieves the key <code>k</code> from the config file.
    -  [`load-config`](#triangulum.config/load-config) - Re/loads a configuration file.
    -  [`namespaced-config?`](#triangulum.config/namespaced-config?) - Returns true if the given configuration map is namespaced, otherwise false.
    -  [`nested-config?`](#triangulum.config/nested-config?) - Returns true if the given configuration map is unnamespaced nested, otherwise false.
    -  [`split-ns-key`](#triangulum.config/split-ns-key) - Given a namespaced key, returns a vector of unnamespaced keys.
    -  [`valid-config?`](#triangulum.config/valid-config?) - Validates <code>file</code> as a configuration file.
-  [`triangulum.config-namespaced-spec`](#triangulum.config-namespaced-spec) 
-  [`triangulum.database`](#triangulum.database)  - To use <code>triangulum.database</code>, first add your database connection configurations to a <code>config.edn</code> file in your project's root directory.
    -  [`call-sql`](#triangulum.database/call-sql) - Currently call-sql only works with postgres.
    -  [`call-sqlite`](#triangulum.database/call-sqlite) - Runs a sqllite3 sql command.
    -  [`insert-rows!`](#triangulum.database/insert-rows!) - Insert new rows from 3d vector.
    -  [`p-insert-rows!`](#triangulum.database/p-insert-rows!) - A parallel implementation of insert-rows!.
    -  [`p-update-rows!`](#triangulum.database/p-update-rows!) - A parallel implementation of update-rows!.
    -  [`sql-primitive`](#triangulum.database/sql-primitive) - Return single value for queries that return a value instead of a table.
    -  [`update-rows!`](#triangulum.database/update-rows!) - Updates existing rows from a 3d vector.
-  [`triangulum.email`](#triangulum.email)  - Provides some functionality for sending email from an SMTP server.
    -  [`email?`](#triangulum.email/email?) - Checks if string is email.
    -  [`get-base-url`](#triangulum.email/get-base-url) - Gets the homepage url.
    -  [`send-mail`](#triangulum.email/send-mail) - Sends an email with a given subject and body to specified recipients.
-  [`triangulum.errors`](#triangulum.errors)  - Error handling utilities for the Triangulum application.
    -  [`init-throw`](#triangulum.errors/init-throw) - Takes a <code>message</code> string as input and throws an exception with the provided message.
    -  [`nil-on-error`](#triangulum.errors/nil-on-error) - Catches exception and returns <code>nil</code> if its body throws an exception.
    -  [`try-catch-throw`](#triangulum.errors/try-catch-throw) - Takes a function <code>try-fn</code> and a <code>message</code> string as input.
-  [`triangulum.git`](#triangulum.git) 
    -  [`current-version`](#triangulum.git/current-version) - Return current latest tag version from the configured tags url of repo.
    -  [`get-tags-url`](#triangulum.git/get-tags-url) - Gets repo tags url from config.edn.
-  [`triangulum.handler`](#triangulum.handler) 
    -  [`authenticated-routing-handler`](#triangulum.handler/authenticated-routing-handler) - Routing Handler that delegates authentication & redirection to handlers specified in your config.edn.
    -  [`case-insensitive-substring?`](#triangulum.handler/case-insensitive-substring?) - True if s includes substr regardless of case.
    -  [`create-handler-stack`](#triangulum.handler/create-handler-stack) - Create the Ring handler stack.
    -  [`development-app`](#triangulum.handler/development-app) - Handler function for development (figwheel).
    -  [`get-cookie-store`](#triangulum.handler/get-cookie-store) - Computes a new <code>ring.middleware.session.cookie/cookie-store</code> object.
    -  [`optional-middleware`](#triangulum.handler/optional-middleware) - Conditionally apply a middleware.
    -  [`parse-query-string`](#triangulum.handler/parse-query-string) - Parses query strings and returns a params map.
    -  [`random-string`](#triangulum.handler/random-string) - Returns a random alphanumeric string of length n.
    -  [`string-to-bytes`](#triangulum.handler/string-to-bytes) - Converts a string into a byte array.
    -  [`wrap-bad-uri`](#triangulum.handler/wrap-bad-uri) - Wrapper that checks if the request url contains a bad token from the provided set and returns a forbidden-response if so; otherwise, passes the request to the provided handler.
    -  [`wrap-edn-params`](#triangulum.handler/wrap-edn-params) - Wrapper that parses request query strings and puts in :params request map.
    -  [`wrap-exceptions`](#triangulum.handler/wrap-exceptions) - Wrapper to manage exception handling, logging it and responding with 500 in case of an exception.
    -  [`wrap-request-logging`](#triangulum.handler/wrap-request-logging) - Wrapper that logs the incoming requests.
    -  [`wrap-response-logging`](#triangulum.handler/wrap-response-logging) - Wrapper that logs served responses.
-  [`triangulum.https`](#triangulum.https) 
    -  [`-main`](#triangulum.https/-main) - A set of tools for using certbot as the server certificate manager.
-  [`triangulum.logging`](#triangulum.logging)  - To send a message to the logger use <code>log</code> or <code>log-str</code>.
    -  [`log`](#triangulum.logging/log) - Synchronously create a log entry.
    -  [`log-str`](#triangulum.logging/log-str) - A variadic version of log which concatenates all of the strings into one log line.
    -  [`set-log-path!`](#triangulum.logging/set-log-path!) - Sets a path to create file logs.
-  [`triangulum.migrate`](#triangulum.migrate) 
    -  [`*migrations-dir*`](#triangulum.migrate/*migrations-dir*) - Location of migrations dir.
    -  [`migrate!`](#triangulum.migrate/migrate!) - Performs the database migrations stored in the <code>src/sql/changes/</code> directory.
-  [`triangulum.notify`](#triangulum.notify)  - Provides functions to interact with systemd for process management and notifications.
    -  [`available?`](#triangulum.notify/available?) - Checks if this process is a process managed by systemd.
    -  [`ready!`](#triangulum.notify/ready!) - Sends ready message to systemd.
    -  [`reloading!`](#triangulum.notify/reloading!) - Sends reloading message to systemd.
    -  [`send-status!`](#triangulum.notify/send-status!) - Sends status message to systemd.
    -  [`stopping!`](#triangulum.notify/stopping!) - Sends stopping message to systemd.
-  [`triangulum.packaging`](#triangulum.packaging) 
    -  [`assert-required`](#triangulum.packaging/assert-required) - Check that each key in required coll is a key in params and throw if required are missing in params, otherwise return nil.
    -  [`assert-specs`](#triangulum.packaging/assert-specs) - Check that key in params satisfies the spec.
    -  [`basis`](#triangulum.packaging/basis) - Basis map, which contains the root and project deps.edn maps plus some additional required keys.
    -  [`build-folder`](#triangulum.packaging/build-folder) - Directory to contain generated JARs and a directory of JAR contents.
    -  [`build-jar`](#triangulum.packaging/build-jar) - Create a JAR suitable for deployment and use as a library.
    -  [`build-uberjar`](#triangulum.packaging/build-uberjar) - Create an UberJAR suitable for deployment and use as an application.
    -  [`clean`](#triangulum.packaging/clean) - Delete the build folder and its contents.
    -  [`deploy-jar`](#triangulum.packaging/deploy-jar) - Upload a library JAR to https://clojars.org.
    -  [`get-calendar-commit-version`](#triangulum.packaging/get-calendar-commit-version) - Returns the current git commit's date and hash as YYYY.MM.DD-HASH.
    -  [`get-jar-file-name`](#triangulum.packaging/get-jar-file-name) - Relative path to the generated JAR file.
    -  [`get-uberjar-file-name`](#triangulum.packaging/get-uberjar-file-name) - Relative path to the generated UberJAR file.
    -  [`jar-content`](#triangulum.packaging/jar-content) - Directory to contain the JAR's contents before it is packaged.
    -  [`print-calendar-commit-version`](#triangulum.packaging/print-calendar-commit-version) - Utility function for use with clojure -X.
-  [`triangulum.response`](#triangulum.response) 
    -  [`data-response`](#triangulum.response/data-response) - Creates a response object.
    -  [`edn-response`](#triangulum.response/edn-response) - Creates an edn response type.
    -  [`forbidden-response`](#triangulum.response/forbidden-response) - Returns a forbidden response.
    -  [`json-response`](#triangulum.response/json-response) - Creates a json response type.
    -  [`no-cross-traffic?`](#triangulum.response/no-cross-traffic?) - Checks for cross traffic.
    -  [`transit-response`](#triangulum.response/transit-response) - Creates a transit response type.
-  [`triangulum.security`](#triangulum.security) 
    -  [`hash-digest`](#triangulum.security/hash-digest) - Returns the hex digest of string.
    -  [`hash-file`](#triangulum.security/hash-file) - Returns the SHA-256 digest of a file.
-  [`triangulum.server`](#triangulum.server) 
    -  [`-main`](#triangulum.server/-main) - Server entry main function.
    -  [`reload-running-server!`](#triangulum.server/reload-running-server!) - Reloads the server namespace and its dependencies.
    -  [`send-to-nrepl-server!`](#triangulum.server/send-to-nrepl-server!) - Sends form to the nrepl server.
    -  [`start-server!`](#triangulum.server/start-server!) - See README.org -> Web Framework -> triangulum.server for details.
    -  [`stop-running-server!`](#triangulum.server/stop-running-server!) - Sends stop-server! call to the nrepl server.
    -  [`stop-server!`](#triangulum.server/stop-server!) - Stops server with workers jobs.
-  [`triangulum.sockets`](#triangulum.sockets)  - Provides functionality for creating and managing client and server sockets.
    -  [`send-to-server!`](#triangulum.sockets/send-to-server!) - Attempts to send socket message.
    -  [`socket-open?`](#triangulum.sockets/socket-open?) - Checks if the socket at host/port is open.
    -  [`start-socket-server!`](#triangulum.sockets/start-socket-server!) - Starts a socket server at port with handler.
    -  [`stop-socket-server!`](#triangulum.sockets/stop-socket-server!) - Stops the socket server at port with handler.
-  [`triangulum.systemd`](#triangulum.systemd) 
    -  [`-main`](#triangulum.systemd/-main) - The entry point for using the tools provided in systemd.clj.
    -  [`fmt-service-file`](#triangulum.systemd/fmt-service-file) - Formats <code>template</code> with the <code>config</code> dictionary.
-  [`triangulum.type-conversion`](#triangulum.type-conversion)  - Provides a collection of functions for converting between different data types and formats, including conversions between numbers, booleans, JSON, and PostgreSQL data types.
    -  [`clj->json`](#triangulum.type-conversion/clj->json) - Convert clj to JSON string.
    -  [`clj->jsonb`](#triangulum.type-conversion/clj->jsonb) - Convert clj to PG jsonb object.
    -  [`json->clj`](#triangulum.type-conversion/json->clj) - Convert JSON string to clj equivalent.
    -  [`json->jsonb`](#triangulum.type-conversion/json->jsonb) - Convert JSON string to PG jsonb object.
    -  [`jsonb->clj`](#triangulum.type-conversion/jsonb->clj) - Convert PG jsonb object to clj equivalent.
    -  [`jsonb->json`](#triangulum.type-conversion/jsonb->json) - Convert PG jsonb object to json string.
    -  [`str->pg`](#triangulum.type-conversion/str->pg) - Convert string to PG object of pg-type.
    -  [`val->bool`](#triangulum.type-conversion/val->bool) - Converts a value to a java Boolean.
    -  [`val->double`](#triangulum.type-conversion/val->double) - Converts a value to a java Double.
    -  [`val->float`](#triangulum.type-conversion/val->float) - Converts a value to a java Float.
    -  [`val->int`](#triangulum.type-conversion/val->int) - Converts a value to a java Integer.
    -  [`val->long`](#triangulum.type-conversion/val->long) - Converts a value to a java Long.
-  [`triangulum.utils`](#triangulum.utils)  - Collection of utility functions for various purposes, such as text parsing, shell command execution, response building, and operations on maps and namespaces.
    -  [`camel->kebab`](#triangulum.utils/camel->kebab) - Converts camelString to kebab-string.
    -  [`clj-namespaced-symbol->js-module`](#triangulum.utils/clj-namespaced-symbol->js-module) - Given a namespace-qualified symbol, return its string representation as a JS module.
    -  [`current-year`](#triangulum.utils/current-year) - Returns the current year as an integer.
    -  [`data-response`](#triangulum.utils/data-response) - DEPRECATED: Use [[triangulum.response/data-response]] instead.
    -  [`delete-recursively`](#triangulum.utils/delete-recursively) - Recursively delete all files and directories under the given directory.
    -  [`end-with`](#triangulum.utils/end-with) - Appends 'end' to the end of the string, if it is not already the end of the string.
    -  [`filterm`](#triangulum.utils/filterm) - Takes a map, filters on pred for each MapEntry, returns a map.
    -  [`find-missing-keys`](#triangulum.utils/find-missing-keys) - Returns true if m1's keys are a subset of m2's keys, and that any nested maps also maintain the same property.
    -  [`format-str`](#triangulum.utils/format-str) - Use any char after % for format.
    -  [`format-with-dict`](#triangulum.utils/format-with-dict) - Replaces <code>fmt-str</code> with values from <code>m</code>.
    -  [`kebab->camel`](#triangulum.utils/kebab->camel) - Converts kebab-string to camelString.
    -  [`kebab->snake`](#triangulum.utils/kebab->snake) - Converts kebab-str to snake_str.
    -  [`mapm`](#triangulum.utils/mapm) - Takes a map, applies f to each MapEntry, returns a map.
    -  [`parse-as-sh-cmd`](#triangulum.utils/parse-as-sh-cmd) - Split string into an array for use with clojure.java.shell/sh.
    -  [`path`](#triangulum.utils/path) - Takes variadic args and returns a path string.
    -  [`remove-end`](#triangulum.utils/remove-end) - Removes 'end' from string, only if it exists.
    -  [`resolve-foreign-symbol`](#triangulum.utils/resolve-foreign-symbol) - Given a namespace-qualified symbol, attempt to require its namespace and resolve the symbol within that namespace to a value.
    -  [`reverse-map`](#triangulum.utils/reverse-map) - Reverses the key-value pairs in a given map.
    -  [`sh-exec-with`](#triangulum.utils/sh-exec-with) - Provides a path (<code>dir</code>) and environment (<code>env</code>) to one bash <code>command</code> and executes it.
    -  [`sh-wrapper`](#triangulum.utils/sh-wrapper) - DEPRECATED: Use [[triangulum.utils/shell-wrapper]] instead.
    -  [`shell-wrapper`](#triangulum.utils/shell-wrapper) - A wrapper around babashka.process/shell that logs the output and errors.
    -  [`snake->kebab`](#triangulum.utils/snake->kebab) - Converts snake_str to kebab-str.
-  [`triangulum.views`](#triangulum.views) 
    -  [`body->transit`](#triangulum.views/body->transit) - Produces a transit response body.
    -  [`not-found-page`](#triangulum.views/not-found-page) - Produces a not found response.
    -  [`render-page`](#triangulum.views/render-page) - Returns the page's html.
-  [`triangulum.worker`](#triangulum.worker)  - Responsible for the management of worker lifecycle within the <code>:server</code> context, specifically those defined under the <code>:workers</code> key.
    -  [`start-workers!`](#triangulum.worker/start-workers!) - Starts a set of workers based on the provided configuration.
    -  [`stop-workers!`](#triangulum.worker/stop-workers!) - Stops a set of currently running workers.

-----
# <a name="triangulum.build-db">triangulum.build-db</a>






## <a name="triangulum.build-db/-main">`-main`</a><a name="triangulum.build-db/-main"></a>
``` clojure

(-main & args)
```

A set of tools for building and maintaining the project database with Postgres.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/build_db.clj#L188-L220">Source</a></sub></p>

-----
# <a name="triangulum.cli">triangulum.cli</a>


Provides a command-line interface (CLI) for Triangulum applications. It includes functions for parsing command-line options, displaying usage information, and checking for errors in the provided arguments.




## <a name="triangulum.cli/get-cli-options">`get-cli-options`</a><a name="triangulum.cli/get-cli-options"></a>
``` clojure

(get-cli-options args cli-options cli-actions alias-str & [config])
```

Checks for a valid call from the CLI and returns the users options.

   Takes the command-line arguments, a map of CLI options, a map of CLI actions,
   an alias string, and an optional config map.

   Example:
   ```clojure
   (def cli-options {...})

   (def cli-actions {...})
   (def alias-str "...")

   (get-cli-options command-line-args cli-options cli-actions alias-str)
   ```
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/cli.clj#L88-L116">Source</a></sub></p>

-----
# <a name="triangulum.config">triangulum.config</a>






## <a name="triangulum.config/-main">`-main`</a><a name="triangulum.config/-main"></a>
``` clojure

(-main & args)
```

Configuration management.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/config.clj#L172-L179">Source</a></sub></p>

## <a name="triangulum.config/get-config">`get-config`</a><a name="triangulum.config/get-config"></a>
``` clojure

(get-config & all-keys)
```

Retrieves the key `k` from the config file.
   Can also be called with the keys leading to a config.
   Examples:
   ```clojure
   (get-config :mail) -> {:host "google.com" :port 543}
   (get-config :mail :host) -> "google.com"
   (get-config :triangulum.email/host) -> "google.com"
   (get-config :triangulum.views/title :en) -> "english"
   ```
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/config.clj#L129-L158">Source</a></sub></p>

## <a name="triangulum.config/load-config">`load-config`</a><a name="triangulum.config/load-config"></a>
``` clojure

(load-config)
(load-config new-config-file)
```

Re/loads a configuration file. Defaults to the last loaded file, or config.edn.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/config.clj#L105-L111">Source</a></sub></p>

## <a name="triangulum.config/namespaced-config?">`namespaced-config?`</a><a name="triangulum.config/namespaced-config?"></a>
``` clojure

(namespaced-config? config)
```

Returns true if the given configuration map is namespaced, otherwise false.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/config.clj#L113-L116">Source</a></sub></p>

## <a name="triangulum.config/nested-config?">`nested-config?`</a><a name="triangulum.config/nested-config?"></a>
``` clojure

(nested-config? config)
```

Returns true if the given configuration map is unnamespaced nested, otherwise false.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/config.clj#L118-L121">Source</a></sub></p>

## <a name="triangulum.config/split-ns-key">`split-ns-key`</a><a name="triangulum.config/split-ns-key"></a>
``` clojure

(split-ns-key ns-key)
```

Given a namespaced key, returns a vector of unnamespaced keys.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/config.clj#L123-L126">Source</a></sub></p>

## <a name="triangulum.config/valid-config?">`valid-config?`</a><a name="triangulum.config/valid-config?"></a>
``` clojure

(valid-config? {:keys [file]})
```

Validates `file` as a configuration file.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/config.clj#L160-L163">Source</a></sub></p>

-----

-----
# <a name="triangulum.database">triangulum.database</a>


To use [`triangulum.database`](#triangulum.database), first add your database connection
  configurations to a `config.edn` file in your project's root directory.

  For example:
  ```clojure
  ;; config.edn
  {:database {:host     "localhost"
            :port     5432
            :dbname   "pyregence"
            :user     "pyregence"
            :password "pyregence"}}
  ```

  To run a postgres sql command use [[`call-sql`](#triangulum.database/call-sql)](#triangulum.database/call-sql). Currently [[`call-sql`](#triangulum.database/call-sql)](#triangulum.database/call-sql)
  only works with postgres. With the second parameter can be an
  optional settings map (default values shown below).

  ```clojure
  (call-sql "function" {:log? true :use-vec? false} "param1" "param2" ... "paramN")
  ```

  To run a sqllite3 sql command use [`call-sqlite`](#triangulum.database/call-sqlite). An existing sqllite3 database
  must be provided.

  ```clojure
  (call-sqlite "select * from table" "path/db-file")
  ```

  To insert new rows or update existing rows use [`insert-rows!`](#triangulum.database/insert-rows!) and
  [`update-rows!`](#triangulum.database/update-rows!). If fields are not provided, the first row will be assumed to
  be the field names.

  ```clojure
  (insert-rows! table-name rows-vector fields-map)
  (update-rows! table-name rows-vector column-to-update fields-map)
  ```




## <a name="triangulum.database/call-sql">`call-sql`</a><a name="triangulum.database/call-sql"></a>
``` clojure

(call-sql sql-fn-name & opts+args)
```

Currently call-sql only works with postgres. The second parameter
   can be an optional settings map.

   Defaults values are:
   {:log? true :use-vec? false}
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/database.clj#L86-L112">Source</a></sub></p>

## <a name="triangulum.database/call-sqlite">`call-sqlite`</a><a name="triangulum.database/call-sqlite"></a>
``` clojure

(call-sqlite query file-path)
```

Runs a sqllite3 sql command. An existing sqllite3 database must be provided.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/database.clj#L115-L123">Source</a></sub></p>

## <a name="triangulum.database/insert-rows!">`insert-rows!`</a><a name="triangulum.database/insert-rows!"></a>
``` clojure

(insert-rows! table rows)
(insert-rows! table rows fields)
```

Insert new rows from 3d vector. If the optional fields are not provided,
   the first row will be assumed to be the field names.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/database.clj#L137-L147">Source</a></sub></p>

## <a name="triangulum.database/p-insert-rows!">`p-insert-rows!`</a><a name="triangulum.database/p-insert-rows!"></a>
``` clojure

(p-insert-rows! table rows)
(p-insert-rows! table rows fields)
```

A parallel implementation of insert-rows!
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/database.clj#L149-L155">Source</a></sub></p>

## <a name="triangulum.database/p-update-rows!">`p-update-rows!`</a><a name="triangulum.database/p-update-rows!"></a>
``` clojure

(p-update-rows! table rows id-key)
(p-update-rows! table rows id-key fields)
```

A parallel implementation of update-rows!
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/database.clj#L187-L193">Source</a></sub></p>

## <a name="triangulum.database/sql-primitive">`sql-primitive`</a><a name="triangulum.database/sql-primitive"></a>




Return single value for queries that return a value instead of a table.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/database.clj#L73-L75">Source</a></sub></p>

## <a name="triangulum.database/update-rows!">`update-rows!`</a><a name="triangulum.database/update-rows!"></a>
``` clojure

(update-rows! table rows id-key)
(update-rows! table rows id-key fields)
```

Updates existing rows from a 3d vector.  One of the columns must be a
   identifier for the update command. If the optional fields are not provided,
   the first row will be assumed to be the field names.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/database.clj#L174-L185">Source</a></sub></p>

-----
# <a name="triangulum.email">triangulum.email</a>


Provides some functionality for sending email from an SMTP
   server. Given the configuration inside `:mail`:
   - `:host`     - Hostname of the SMTP server.
   - `:user`     - Email account to use via SMTP (and which emails will be addressed from)
   - `:pass`     - Password to use via SMTP.
   - `:port`     - Port to use for SMTP.
   - `:base-url` - The host's host url, used when sending links in emails.




## <a name="triangulum.email/email?">`email?`</a><a name="triangulum.email/email?"></a>
``` clojure

(email? string)
```

Checks if string is email.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/email.clj#L26-L30">Source</a></sub></p>

## <a name="triangulum.email/get-base-url">`get-base-url`</a><a name="triangulum.email/get-base-url"></a>
``` clojure

(get-base-url)
```

Gets the homepage url.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/email.clj#L21-L24">Source</a></sub></p>

## <a name="triangulum.email/send-mail">`send-mail`</a><a name="triangulum.email/send-mail"></a>
``` clojure

(send-mail to-addresses cc-addresses bcc-addresses subject body content-type)
```

Sends an email with a given subject and body to specified recipients.

  This function uses the [[`send-postal`](#triangulum.email/send-postal)](#triangulum.email/send-postal) internal function to send the email.
  It logs the success or failure of sending this email.

  Arguments:
  to-addresses   - a collection of email addresses to which the email will be sent
  cc-addresses   - a collection of email addresses to which the email will be carbon copied
  bcc-addresses  - a collection of email addresses to which the email will be blind carbon copied
  subject        - a string representing the subject of the email
  body           - a string representing the body of the email
  content-type   - a keyword indicating the content type of the email, either :text for 'text/plain' or :html for 'text/html'

  Returns:
  Result map returned by [[`send-postal`](#triangulum.email/send-postal)](#triangulum.email/send-postal).
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/email.clj#L47-L74">Source</a></sub></p>

-----
# <a name="triangulum.errors">triangulum.errors</a>


Error handling utilities for the Triangulum application.
   It includes functions and macros to handle exceptions and log errors.




## <a name="triangulum.errors/init-throw">`init-throw`</a><a name="triangulum.errors/init-throw"></a>
``` clojure

(init-throw message)
(init-throw message data)
(init-throw message data cause)
```

Takes a `message` string as input and throws an exception with the provided message.

   Example: `(init-throw "Error: Invalid input"))`
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/errors.clj#L6-L15">Source</a></sub></p>

## <a name="triangulum.errors/nil-on-error">`nil-on-error`</a><a name="triangulum.errors/nil-on-error"></a>
``` clojure

(nil-on-error & body)
```
Function.

Catches exception and returns `nil` if its body throws an exception.

  Example:
  ```clojure
  (nil-on-error (/ 1 0)) ; Returns nil
  (nil-on-error (+ 2 3)) ; Returns 5
  ```
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/errors.clj#L33-L42">Source</a></sub></p>

## <a name="triangulum.errors/try-catch-throw">`try-catch-throw`</a><a name="triangulum.errors/try-catch-throw"></a>
``` clojure

(try-catch-throw try-fn message)
```

Takes a function `try-fn` and a `message` string as input. Executes `try-fn` and,
   if it throws an exception, catches the exception, logs the error, and then throws
   an exception with the augmented input message.

  Example:
  ```clojure
  (try-catch-throw (fn [] (throw (ex-info "Initial error" {}))) "Augmented error message")
  ```
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/errors.clj#L17-L31">Source</a></sub></p>

-----
# <a name="triangulum.git">triangulum.git</a>






## <a name="triangulum.git/current-version">`current-version`</a><a name="triangulum.git/current-version"></a>
``` clojure

(current-version)
```

Return current latest tag version from the configured tags url of repo.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/git.clj#L42-L49">Source</a></sub></p>

## <a name="triangulum.git/get-tags-url">`get-tags-url`</a><a name="triangulum.git/get-tags-url"></a>
``` clojure

(get-tags-url)
```

Gets repo tags url from config.edn.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/git.clj#L15-L18">Source</a></sub></p>

-----
# <a name="triangulum.handler">triangulum.handler</a>






## <a name="triangulum.handler/authenticated-routing-handler">`authenticated-routing-handler`</a><a name="triangulum.handler/authenticated-routing-handler"></a>
``` clojure

(authenticated-routing-handler {:keys [uri request-method], :as request})
```

Routing Handler that delegates authentication & redirection
   to handlers specified in your config.edn
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/handler.clj#L45-L60">Source</a></sub></p>

## <a name="triangulum.handler/case-insensitive-substring?">`case-insensitive-substring?`</a><a name="triangulum.handler/case-insensitive-substring?"></a>
``` clojure

(case-insensitive-substring? s substr)
```

True if s includes substr regardless of case.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/handler.clj#L66-L70">Source</a></sub></p>

## <a name="triangulum.handler/create-handler-stack">`create-handler-stack`</a><a name="triangulum.handler/create-handler-stack"></a>
``` clojure

(create-handler-stack routing-handler ssl? reload?)
```

Create the Ring handler stack.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/handler.clj#L185-L210">Source</a></sub></p>

## <a name="triangulum.handler/development-app">`development-app`</a><a name="triangulum.handler/development-app"></a>




Handler function for development (figwheel).
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/handler.clj#L212-L220">Source</a></sub></p>

## <a name="triangulum.handler/get-cookie-store">`get-cookie-store`</a><a name="triangulum.handler/get-cookie-store"></a>
``` clojure

(get-cookie-store)
```

Computes a new `ring.middleware.session.cookie/cookie-store` object.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/handler.clj#L168-L172">Source</a></sub></p>

## <a name="triangulum.handler/optional-middleware">`optional-middleware`</a><a name="triangulum.handler/optional-middleware"></a>
``` clojure

(optional-middleware handler mw use?)
```

Conditionally apply a middleware.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/handler.clj#L178-L183">Source</a></sub></p>

## <a name="triangulum.handler/parse-query-string">`parse-query-string`</a><a name="triangulum.handler/parse-query-string"></a>
``` clojure

(parse-query-string query-string)
```

Parses query strings and returns a params map.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/handler.clj#L128-L138">Source</a></sub></p>

## <a name="triangulum.handler/random-string">`random-string`</a><a name="triangulum.handler/random-string"></a>
``` clojure

(random-string n)
```

Returns a random alphanumeric string of length n.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/handler.clj#L159-L166">Source</a></sub></p>

## <a name="triangulum.handler/string-to-bytes">`string-to-bytes`</a><a name="triangulum.handler/string-to-bytes"></a>
``` clojure

(string-to-bytes s)
```

Converts a string into a byte array.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/handler.clj#L154-L157">Source</a></sub></p>

## <a name="triangulum.handler/wrap-bad-uri">`wrap-bad-uri`</a><a name="triangulum.handler/wrap-bad-uri"></a>
``` clojure

(wrap-bad-uri handler)
```

Wrapper that checks if the request url contains a bad token from the
  provided set and returns a forbidden-response if so; otherwise,
  passes the request to the provided handler.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/handler.clj#L72-L81">Source</a></sub></p>

## <a name="triangulum.handler/wrap-edn-params">`wrap-edn-params`</a><a name="triangulum.handler/wrap-edn-params"></a>
``` clojure

(wrap-edn-params handler)
```

Wrapper that parses request query strings and puts in :params request map.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/handler.clj#L140-L152">Source</a></sub></p>

## <a name="triangulum.handler/wrap-exceptions">`wrap-exceptions`</a><a name="triangulum.handler/wrap-exceptions"></a>
``` clojure

(wrap-exceptions handler)
```

Wrapper to manage exception handling, logging it and responding with 500 in case of an exception.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/handler.clj#L116-L126">Source</a></sub></p>

## <a name="triangulum.handler/wrap-request-logging">`wrap-request-logging`</a><a name="triangulum.handler/wrap-request-logging"></a>
``` clojure

(wrap-request-logging handler)
```

Wrapper that logs the incoming requests.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/handler.clj#L83-L92">Source</a></sub></p>

## <a name="triangulum.handler/wrap-response-logging">`wrap-response-logging`</a><a name="triangulum.handler/wrap-response-logging"></a>
``` clojure

(wrap-response-logging handler)
```

Wrapper that logs served responses.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/handler.clj#L94-L114">Source</a></sub></p>

-----
# <a name="triangulum.https">triangulum.https</a>






## <a name="triangulum.https/-main">`-main`</a><a name="triangulum.https/-main"></a>
``` clojure

(-main & args)
```

A set of tools for using certbot as the server certificate manager.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/https.clj#L99-L116">Source</a></sub></p>

-----
# <a name="triangulum.logging">triangulum.logging</a>


To send a message to the logger use [[`log`](#triangulum.logging/log)](#triangulum.logging/log) or [[`log-str`](#triangulum.logging/log-str)](#triangulum.logging/log-str). [[`log`](#triangulum.logging/log)](#triangulum.logging/log) can take an
  optional argument to specify not default behavior. The default values are
  shown below. [[`log-str`](#triangulum.logging/log-str)](#triangulum.logging/log-str) always uses the default values.

  ```clojure
  (log "Hello world" {:newline? true :pprint? false :force-stdout? false})
  (log-str "Hello" "world")
  ```

  By default the above will log to standard out. If you would like to
  have the system log to YYYY-DD-MM.log, set a log path. You can either specify
  a path relative to the toplevel directory of the main project repository or an
  absolute path on your filesystem. The logger will keep the 10 most recent logs
  (where a new log is created every day at midnight). To stop the logging server
  set path to "".




## <a name="triangulum.logging/log">`log`</a><a name="triangulum.logging/log"></a>
``` clojure

(log
 data
 &
 {:keys [newline? pprint? force-stdout? truncate?],
  :or {newline? true, pprint? false, force-stdout? false, truncate? true}})
```

Synchronously create a log entry. Logs will got to standard out as default.
   A log file location can be specified with set-log-path!.

   Default options are {:newline? true :pprint? false :force-stdout? false}
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/logging.clj#L31-L49">Source</a></sub></p>

## <a name="triangulum.logging/log-str">`log-str`</a><a name="triangulum.logging/log-str"></a>
``` clojure

(log-str & data)
```

A variadic version of log which concatenates all of the strings into one log line.
   Uses the default options for log.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/logging.clj#L51-L55">Source</a></sub></p>

## <a name="triangulum.logging/set-log-path!">`set-log-path!`</a><a name="triangulum.logging/set-log-path!"></a>
``` clojure

(set-log-path! path)
```

Sets a path to create file logs. When set to a directory, log files will be
   created with the date as part of the file name. When an empty string is set
   logging will be sent to standard out.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/logging.clj#L69-L91">Source</a></sub></p>

-----
# <a name="triangulum.migrate">triangulum.migrate</a>






## <a name="triangulum.migrate/*migrations-dir*">`*migrations-dir*`</a><a name="triangulum.migrate/*migrations-dir*"></a>




Location of migrations dir
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/migrate.clj#L14-L14">Source</a></sub></p>

## <a name="triangulum.migrate/migrate!">`migrate!`</a><a name="triangulum.migrate/migrate!"></a>
``` clojure

(migrate! database user user-pass verbose?)
```

Performs the database migrations stored in the `src/sql/changes/` directory.
  Migrations must be stored in chronological order (e.g. `2021-02-28_add-users-table.sql`).

  Migrations run inside of a transaction block to ensure the entire migration is
  completed prior to being committed to the database.

  Currently, this tool does not support rollbacks.

  If a migration fails, all migrations which follow it will be cancelled.

  Migrations which have been completed are stored in a table `tri.migrations`,
  and include a SHA-256 hash of the migration file contents. If a migration has
  been altered, the migrations will fail. This is to ensure consistency as migrations
  are added.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/migrate.clj#L92-L127">Source</a></sub></p>

-----
# <a name="triangulum.notify">triangulum.notify</a>


Provides functions to interact with systemd for process management and notifications.

   Uses the SDNotify Java library to send notifications and check the availability of the
   current process.

   The functions in this namespace allow you to check if the process is managed by systemd,
   send "ready," "reloading," and "stopping" messages, and send custom status messages.

   These functions can be helpful when integrating your application with systemd for
   better process supervision and management.




## <a name="triangulum.notify/available?">`available?`</a><a name="triangulum.notify/available?"></a>
``` clojure

(available?)
```

Checks if this process is a process managed by systemd.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/notify.clj#L15-L18">Source</a></sub></p>

## <a name="triangulum.notify/ready!">`ready!`</a><a name="triangulum.notify/ready!"></a>
``` clojure

(ready!)
```

Sends ready message to systemd. Systemd file must include `Type=notify` to be used.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/notify.clj#L20-L23">Source</a></sub></p>

## <a name="triangulum.notify/reloading!">`reloading!`</a><a name="triangulum.notify/reloading!"></a>
``` clojure

(reloading!)
```

Sends reloading message to systemd. Must call `send-notify!` once reloading
   has been completed.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/notify.clj#L25-L29">Source</a></sub></p>

## <a name="triangulum.notify/send-status!">`send-status!`</a><a name="triangulum.notify/send-status!"></a>
``` clojure

(send-status! s)
```

Sends status message to systemd. (e.g. `(send-status! "READY=1")`).
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/notify.clj#L36-L39">Source</a></sub></p>

## <a name="triangulum.notify/stopping!">`stopping!`</a><a name="triangulum.notify/stopping!"></a>
``` clojure

(stopping!)
```

Sends stopping message to systemd.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/notify.clj#L31-L34">Source</a></sub></p>

-----
# <a name="triangulum.packaging">triangulum.packaging</a>






## <a name="triangulum.packaging/assert-required">`assert-required`</a><a name="triangulum.packaging/assert-required"></a>
``` clojure

(assert-required task params required)
```

Check that each key in required coll is a key in params and throw if
  required are missing in params, otherwise return nil.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/packaging.clj#L70-L77">Source</a></sub></p>

## <a name="triangulum.packaging/assert-specs">`assert-specs`</a><a name="triangulum.packaging/assert-specs"></a>
``` clojure

(assert-specs task params & key-specs)
```

Check that key in params satisfies the spec. Throw if it exists and
  does not conform to the spec, otherwise return nil.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/packaging.clj#L80-L88">Source</a></sub></p>

## <a name="triangulum.packaging/basis">`basis`</a><a name="triangulum.packaging/basis"></a>




Basis map, which contains the root and project deps.edn maps plus some additional required keys.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/packaging.clj#L35-L37">Source</a></sub></p>

## <a name="triangulum.packaging/build-folder">`build-folder`</a><a name="triangulum.packaging/build-folder"></a>




Directory to contain generated JARs and a directory of JAR contents.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/packaging.clj#L27-L29">Source</a></sub></p>

## <a name="triangulum.packaging/build-jar">`build-jar`</a><a name="triangulum.packaging/build-jar"></a>
``` clojure

(build-jar {:keys [lib-name src-dirs resource-dirs], :or {src-dirs ["src"], resource-dirs ["resources"]}, :as params})
```

Create a JAR suitable for deployment and use as a library.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/packaging.clj#L100-L140">Source</a></sub></p>

## <a name="triangulum.packaging/build-uberjar">`build-uberjar`</a><a name="triangulum.packaging/build-uberjar"></a>
``` clojure

(build-uberjar
 {:keys [app-name main-ns src-dirs resource-dirs bindings compile-opts java-opts manifest],
  :or {src-dirs ["src"], resource-dirs ["resources"], bindings {}, compile-opts {}, java-opts [], manifest {}},
  :as params})
```

Create an UberJAR suitable for deployment and use as an application.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/packaging.clj#L142-L194">Source</a></sub></p>

## <a name="triangulum.packaging/clean">`clean`</a><a name="triangulum.packaging/clean"></a>
``` clojure

(clean _)
```

Delete the build folder and its contents.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/packaging.clj#L94-L98">Source</a></sub></p>

## <a name="triangulum.packaging/deploy-jar">`deploy-jar`</a><a name="triangulum.packaging/deploy-jar"></a>
``` clojure

(deploy-jar {:keys [lib-name], :as params})
```

Upload a library JAR to https://clojars.org.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/packaging.clj#L196-L225">Source</a></sub></p>

## <a name="triangulum.packaging/get-calendar-commit-version">`get-calendar-commit-version`</a><a name="triangulum.packaging/get-calendar-commit-version"></a>
``` clojure

(get-calendar-commit-version)
```

Returns the current git commit's date and hash as YYYY.MM.DD-HASH.
  Depends on the `git` command being available on the JVM's `$PATH`.
  Must be run from within a `git` repository.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/packaging.clj#L43-L52">Source</a></sub></p>

## <a name="triangulum.packaging/get-jar-file-name">`get-jar-file-name`</a><a name="triangulum.packaging/get-jar-file-name"></a>
``` clojure

(get-jar-file-name lib-name version)
```

Relative path to the generated JAR file.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/packaging.clj#L59-L62">Source</a></sub></p>

## <a name="triangulum.packaging/get-uberjar-file-name">`get-uberjar-file-name`</a><a name="triangulum.packaging/get-uberjar-file-name"></a>
``` clojure

(get-uberjar-file-name app-name version)
```

Relative path to the generated UberJAR file.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/packaging.clj#L64-L67">Source</a></sub></p>

## <a name="triangulum.packaging/jar-content">`jar-content`</a><a name="triangulum.packaging/jar-content"></a>




Directory to contain the JAR's contents before it is packaged.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/packaging.clj#L31-L33">Source</a></sub></p>

## <a name="triangulum.packaging/print-calendar-commit-version">`print-calendar-commit-version`</a><a name="triangulum.packaging/print-calendar-commit-version"></a>
``` clojure

(print-calendar-commit-version & _)
```

Utility function for use with clojure -X.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/packaging.clj#L54-L57">Source</a></sub></p>

-----
# <a name="triangulum.response">triangulum.response</a>






## <a name="triangulum.response/data-response">`data-response`</a><a name="triangulum.response/data-response"></a>
``` clojure

(data-response body & [params])
```

Creates a response object.
   Body is required. Status, type, and session are optional.
   When a type keyword is passed, the body is converted to that type,
   otherwise the body is converted to your config.edn's :server :response-type.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/response.clj#L12-L21">Source</a></sub></p>

## <a name="triangulum.response/edn-response">`edn-response`</a><a name="triangulum.response/edn-response"></a>
``` clojure

(edn-response body & [params])
```

Creates an edn response type.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/response.clj#L28-L31">Source</a></sub></p>

## <a name="triangulum.response/forbidden-response">`forbidden-response`</a><a name="triangulum.response/forbidden-response"></a>
``` clojure

(forbidden-response _)
```

Returns a forbidden response.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/response.clj#L43-L46">Source</a></sub></p>

## <a name="triangulum.response/json-response">`json-response`</a><a name="triangulum.response/json-response"></a>
``` clojure

(json-response body & [params])
```

Creates a json response type.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/response.clj#L23-L26">Source</a></sub></p>

## <a name="triangulum.response/no-cross-traffic?">`no-cross-traffic?`</a><a name="triangulum.response/no-cross-traffic?"></a>
``` clojure

(no-cross-traffic? {:strs [referer host]})
```

Checks for cross traffic.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/response.clj#L38-L41">Source</a></sub></p>

## <a name="triangulum.response/transit-response">`transit-response`</a><a name="triangulum.response/transit-response"></a>
``` clojure

(transit-response body & [params])
```

Creates a transit response type.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/response.clj#L33-L36">Source</a></sub></p>

-----
# <a name="triangulum.security">triangulum.security</a>






## <a name="triangulum.security/hash-digest">`hash-digest`</a><a name="triangulum.security/hash-digest"></a>
``` clojure

(hash-digest input)
(hash-digest input hash-algorithm)
```

Returns the hex digest of string. Defaults to SHA-256 hash.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/security.clj#L5-L14">Source</a></sub></p>

## <a name="triangulum.security/hash-file">`hash-file`</a><a name="triangulum.security/hash-file"></a>
``` clojure

(hash-file filename)
```

Returns the SHA-256 digest of a file.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/security.clj#L16-L21">Source</a></sub></p>

-----
# <a name="triangulum.server">triangulum.server</a>






## <a name="triangulum.server/-main">`-main`</a><a name="triangulum.server/-main"></a>
``` clojure

(-main & args)
```

Server entry main function.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/server.clj#L166-L179">Source</a></sub></p>

## <a name="triangulum.server/reload-running-server!">`reload-running-server!`</a><a name="triangulum.server/reload-running-server!"></a>
``` clojure

(reload-running-server! {:keys [nrepl-bind nrepl-port]})
```

Reloads the server namespace and its dependencies.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/server.clj#L129-L135">Source</a></sub></p>

## <a name="triangulum.server/send-to-nrepl-server!">`send-to-nrepl-server!`</a><a name="triangulum.server/send-to-nrepl-server!"></a>
``` clojure

(send-to-nrepl-server! msg & [{:keys [host port], :or {host "127.0.0.1", port 5555}}])
```

Sends form to the nrepl server
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/server.clj#L108-L119">Source</a></sub></p>

## <a name="triangulum.server/start-server!">`start-server!`</a><a name="triangulum.server/start-server!"></a>
``` clojure

(start-server!
 {:keys
  [http-port
   https-port
   nrepl
   cider-nrepl
   nrepl-bind
   nrepl-port
   mode
   log-dir
   handler
   workers
   keystore-file
   keystore-type
   keystore-password],
  :or
  {nrepl-bind "127.0.0.1",
   nrepl-port 5555,
   keystore-file "./.key/keystore.pkcs12",
   keystore-type "pkcs12",
   keystore-password "foobar",
   log-dir "",
   mode "prod"}})
```

See README.org -> Web Framework -> triangulum.server for details.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/server.clj#L44-L96">Source</a></sub></p>

## <a name="triangulum.server/stop-running-server!">`stop-running-server!`</a><a name="triangulum.server/stop-running-server!"></a>
``` clojure

(stop-running-server! {:keys [nrepl-bind nrepl-port]})
```

Sends stop-server! call to the nrepl server.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/server.clj#L121-L127">Source</a></sub></p>

## <a name="triangulum.server/stop-server!">`stop-server!`</a><a name="triangulum.server/stop-server!"></a>
``` clojure

(stop-server!)
```

Stops server with workers jobs.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/server.clj#L98-L106">Source</a></sub></p>

-----
# <a name="triangulum.sockets">triangulum.sockets</a>


Provides functionality for creating and managing client and server sockets. It includes functions for opening and checking socket connections, sending messages to the server, and starting/stopping socket servers with custom request handlers. This namespace enables communication between distributed systems and allows you to implement networked applications.




## <a name="triangulum.sockets/send-to-server!">`send-to-server!`</a><a name="triangulum.sockets/send-to-server!"></a>
``` clojure

(send-to-server! host port message)
```

Attempts to send socket message. Returns :success if successful.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/sockets.clj#L22-L35">Source</a></sub></p>

## <a name="triangulum.sockets/socket-open?">`socket-open?`</a><a name="triangulum.sockets/socket-open?"></a>
``` clojure

(socket-open? host port)
```

Checks if the socket at host/port is open.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/sockets.clj#L13-L20">Source</a></sub></p>

## <a name="triangulum.sockets/start-socket-server!">`start-socket-server!`</a><a name="triangulum.sockets/start-socket-server!"></a>
``` clojure

(start-socket-server! port handler)
```

Starts a socket server at port with handler.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/sockets.clj#L71-L85">Source</a></sub></p>

## <a name="triangulum.sockets/stop-socket-server!">`stop-socket-server!`</a><a name="triangulum.sockets/stop-socket-server!"></a>
``` clojure

(stop-socket-server!)
```

Stops the socket server at port with handler.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/sockets.clj#L59-L69">Source</a></sub></p>

-----
# <a name="triangulum.systemd">triangulum.systemd</a>






## <a name="triangulum.systemd/-main">`-main`</a><a name="triangulum.systemd/-main"></a>
``` clojure

(-main & args)
```

The entry point for using the tools provided in systemd.clj.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/systemd.clj#L107-L117">Source</a></sub></p>

## <a name="triangulum.systemd/fmt-service-file">`fmt-service-file`</a><a name="triangulum.systemd/fmt-service-file"></a>
``` clojure

(fmt-service-file template {:keys [http https], :as config})
```

Formats `template` with the `config` dictionary.

  `template` must use handlebar syntax (e.g. `{{name}}`) which matches the
   keyword in `config`.

  Currently `config` supports:
  - `:repo-dir`      [string] - Directory to the repository
                                (sets `WorkingDirectory`)
  - `:extra-aliases` [string] - Additional aliases to run with startup (e.g. `:production`)
  - `:http`          [number] - HTTP Port
  - `:https`         [number] - HTTPS Port
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/systemd.clj#L35-L53">Source</a></sub></p>

-----
# <a name="triangulum.type-conversion">triangulum.type-conversion</a>


Provides a collection of functions for converting between different data types and formats, including conversions between numbers, booleans, JSON, and PostgreSQL data types.




## <a name="triangulum.type-conversion/clj->json">`clj->json`</a><a name="triangulum.type-conversion/clj->json"></a>




Convert clj to JSON string.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/type_conversion.clj#L92-L92">Source</a></sub></p>

## <a name="triangulum.type-conversion/clj->jsonb">`clj->jsonb`</a><a name="triangulum.type-conversion/clj->jsonb"></a>
``` clojure

(clj->jsonb clj)
```

Convert clj to PG jsonb object.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/type_conversion.clj#L107-L110">Source</a></sub></p>

## <a name="triangulum.type-conversion/json->clj">`json->clj`</a><a name="triangulum.type-conversion/json->clj"></a>
``` clojure

(json->clj json)
(json->clj json default)
```

Convert JSON string to clj equivalent.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/type_conversion.clj#L74-L81">Source</a></sub></p>

## <a name="triangulum.type-conversion/json->jsonb">`json->jsonb`</a><a name="triangulum.type-conversion/json->jsonb"></a>
``` clojure

(json->jsonb json)
```

Convert JSON string to PG jsonb object.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/type_conversion.clj#L102-L105">Source</a></sub></p>

## <a name="triangulum.type-conversion/jsonb->clj">`jsonb->clj`</a><a name="triangulum.type-conversion/jsonb->clj"></a>
``` clojure

(jsonb->clj jsonb)
(jsonb->clj jsonb default)
```

Convert PG jsonb object to clj equivalent.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/type_conversion.clj#L85-L90">Source</a></sub></p>

## <a name="triangulum.type-conversion/jsonb->json">`jsonb->json`</a><a name="triangulum.type-conversion/jsonb->json"></a>




Convert PG jsonb object to json string.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/type_conversion.clj#L83-L83">Source</a></sub></p>

## <a name="triangulum.type-conversion/str->pg">`str->pg`</a><a name="triangulum.type-conversion/str->pg"></a>
``` clojure

(str->pg s pg-type)
```

Convert string to PG object of pg-type
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/type_conversion.clj#L94-L100">Source</a></sub></p>

## <a name="triangulum.type-conversion/val->bool">`val->bool`</a><a name="triangulum.type-conversion/val->bool"></a>
``` clojure

(val->bool v)
(val->bool v default)
```

Converts a value to a java Boolean. Default value for failed conversion is false.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/type_conversion.clj#L56-L65">Source</a></sub></p>

## <a name="triangulum.type-conversion/val->double">`val->double`</a><a name="triangulum.type-conversion/val->double"></a>
``` clojure

(val->double v)
(val->double v default)
```

Converts a value to a java Double. Default value for failed conversion is -1.0.
   Note Postgres float is equivalent to java Double.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/type_conversion.clj#L43-L54">Source</a></sub></p>

## <a name="triangulum.type-conversion/val->float">`val->float`</a><a name="triangulum.type-conversion/val->float"></a>
``` clojure

(val->float v)
(val->float v default)
```

Converts a value to a java Float. Default value for failed conversion is -1.0.
   Note Postgres real is equivalent to java Float.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/type_conversion.clj#L30-L41">Source</a></sub></p>

## <a name="triangulum.type-conversion/val->int">`val->int`</a><a name="triangulum.type-conversion/val->int"></a>
``` clojure

(val->int v)
(val->int v default)
```

Converts a value to a java Integer. Default value for failed conversion is -1.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/type_conversion.clj#L6-L16">Source</a></sub></p>

## <a name="triangulum.type-conversion/val->long">`val->long`</a><a name="triangulum.type-conversion/val->long"></a>
``` clojure

(val->long v)
(val->long v default)
```

Converts a value to a java Long. Default value for failed conversion is -1.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/type_conversion.clj#L18-L28">Source</a></sub></p>

-----
# <a name="triangulum.utils">triangulum.utils</a>


Collection of utility functions for various purposes, such as text parsing,
   shell command execution, response building, and operations on maps and namespaces.




## <a name="triangulum.utils/camel->kebab">`camel->kebab`</a><a name="triangulum.utils/camel->kebab"></a>
``` clojure

(camel->kebab camel-string)
```

Converts camelString to kebab-string.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/utils.clj#L36-L42">Source</a></sub></p>

## <a name="triangulum.utils/clj-namespaced-symbol->js-module">`clj-namespaced-symbol->js-module`</a><a name="triangulum.utils/clj-namespaced-symbol->js-module"></a>
``` clojure

(clj-namespaced-symbol->js-module sym)
```

Given a namespace-qualified symbol, return its string representation as a JS module.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/utils.clj#L267-L273">Source</a></sub></p>

## <a name="triangulum.utils/current-year">`current-year`</a><a name="triangulum.utils/current-year"></a>
``` clojure

(current-year)
```

Returns the current year as an integer.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/utils.clj#L292-L295">Source</a></sub></p>

## <a name="triangulum.utils/data-response">`data-response`</a><a name="triangulum.utils/data-response"></a>
``` clojure

(data-response body)
(data-response body {:keys [status type session], :or {status 200, type :edn}, :as params})
```

DEPRECATED: Use [[triangulum.response/data-response]] instead.
   Create a response object.
   Body is required. Status, type, and session are optional.
   When a type keyword is passed, the body is converted to that type,
   otherwise the body is converted to edn.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/utils.clj#L183-L206">Source</a></sub></p>

## <a name="triangulum.utils/delete-recursively">`delete-recursively`</a><a name="triangulum.utils/delete-recursively"></a>
``` clojure

(delete-recursively dir)
```

Recursively delete all files and directories under the given directory.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/utils.clj#L277-L280">Source</a></sub></p>

## <a name="triangulum.utils/end-with">`end-with`</a><a name="triangulum.utils/end-with"></a>
``` clojure

(end-with s end)
```

Appends 'end' to the end of the string, if it is not already the end of the string.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/utils.clj#L93-L98">Source</a></sub></p>

## <a name="triangulum.utils/filterm">`filterm`</a><a name="triangulum.utils/filterm"></a>
``` clojure

(filterm pred coll)
```

Takes a map, filters on pred for each MapEntry, returns a map.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/utils.clj#L219-L228">Source</a></sub></p>

## <a name="triangulum.utils/find-missing-keys">`find-missing-keys`</a><a name="triangulum.utils/find-missing-keys"></a>
``` clojure

(find-missing-keys m1 m2)
```

Returns true if m1's keys are a subset of m2's keys, and that any nested maps
   also maintain the same property.

   Example:
   `(find-missing-keys {:a {:b "c"} :d 0} {:a {:b "e" :g 42} :d 1 :h 2}) ; => true`
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/utils.clj#L237-L256">Source</a></sub></p>

## <a name="triangulum.utils/format-str">`format-str`</a><a name="triangulum.utils/format-str"></a>
``` clojure

(format-str f-str & args)
```

Use any char after % for format. All % are converted to %s (string).
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/utils.clj#L44-L47">Source</a></sub></p>

## <a name="triangulum.utils/format-with-dict">`format-with-dict`</a><a name="triangulum.utils/format-with-dict"></a>
``` clojure

(format-with-dict fmt-str m)
```

Replaces `fmt-str` with values from `m`.

   NOTE: `fmt-str` must use handlebars (`{{<keyword>}}`) wherever a term is to be replaced.

   Example:
   ```
   (format-with-dict "Hi {{name}}! The moon is {{n}} billion years old." {:name "Bob" :n 4.5}) ; => "Hi Bob! The moon is 4.5 billion years old." 
   ```
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/utils.clj#L49-L63">Source</a></sub></p>

## <a name="triangulum.utils/kebab->camel">`kebab->camel`</a><a name="triangulum.utils/kebab->camel"></a>
``` clojure

(kebab->camel kebab-string)
```

Converts kebab-string to camelString.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/utils.clj#L26-L34">Source</a></sub></p>

## <a name="triangulum.utils/kebab->snake">`kebab->snake`</a><a name="triangulum.utils/kebab->snake"></a>
``` clojure

(kebab->snake kebab-str)
```

Converts kebab-str to snake_str.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/utils.clj#L21-L24">Source</a></sub></p>

## <a name="triangulum.utils/mapm">`mapm`</a><a name="triangulum.utils/mapm"></a>
``` clojure

(mapm f coll)
```

Takes a map, applies f to each MapEntry, returns a map.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/utils.clj#L210-L217">Source</a></sub></p>

## <a name="triangulum.utils/parse-as-sh-cmd">`parse-as-sh-cmd`</a><a name="triangulum.utils/parse-as-sh-cmd"></a>
``` clojure

(parse-as-sh-cmd s)
```

Split string into an array for use with clojure.java.shell/sh.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/utils.clj#L65-L91">Source</a></sub></p>

## <a name="triangulum.utils/path">`path`</a><a name="triangulum.utils/path"></a>
``` clojure

(path & args)
```

Takes variadic args and returns a path string. Args can be any type that can be coerced via `str`.

  Example:
  `(path (io/file "my-dir") "file.csv") ; => "my-dir/file.csv" (on Unix) 
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/utils.clj#L282-L288">Source</a></sub></p>

## <a name="triangulum.utils/remove-end">`remove-end`</a><a name="triangulum.utils/remove-end"></a>
``` clojure

(remove-end s end)
```

Removes 'end' from string, only if it exists.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/utils.clj#L100-L105">Source</a></sub></p>

## <a name="triangulum.utils/resolve-foreign-symbol">`resolve-foreign-symbol`</a><a name="triangulum.utils/resolve-foreign-symbol"></a>
``` clojure

(resolve-foreign-symbol sym)
```

Given a namespace-qualified symbol, attempt to require its namespace
  and resolve the symbol within that namespace to a value.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/utils.clj#L260-L265">Source</a></sub></p>

## <a name="triangulum.utils/reverse-map">`reverse-map`</a><a name="triangulum.utils/reverse-map"></a>
``` clojure

(reverse-map m)
```

Reverses the key-value pairs in a given map.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/utils.clj#L230-L233">Source</a></sub></p>

## <a name="triangulum.utils/sh-exec-with">`sh-exec-with`</a><a name="triangulum.utils/sh-exec-with"></a>
``` clojure

(sh-exec-with dir env command)
```

Provides a path (`dir`) and environment (`env`) to one bash `command`
   and executes it. Returns a map in the following format:
   `{:exit 0 :out 'Output message
' :err ''}`
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/utils.clj#L161-L170">Source</a></sub></p>

## <a name="triangulum.utils/sh-wrapper">`sh-wrapper`</a><a name="triangulum.utils/sh-wrapper"></a>
``` clojure

(sh-wrapper dir env verbose? & commands)
```

DEPRECATED: Use [[triangulum.utils/shell-wrapper]] instead.
  Takes a directory, an environment, a verbosity flag, and bash commands.
  Executes the commands using the given path and environment, then returns
  the output (errors by default).
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/utils.clj#L145-L159">Source</a></sub></p>

## <a name="triangulum.utils/shell-wrapper">`shell-wrapper`</a><a name="triangulum.utils/shell-wrapper"></a>
``` clojure

(shell-wrapper & args)
```

A wrapper around babashka.process/shell that logs the output and errors.
  Accepts an optional opts map as the first argument, followed by the command and its arguments.
  The :log? key in the opts map can be used to control logging (default is true).

  Usage:
  (shell-wrapper {} "ls" "-l") ; With an opts map
  (shell-wrapper "ls" "-l") ; Without an opts map
  (shell-wrapper {:log? false} "ls" "-l") ; Disabling logging

  Examples:
  1. Logs the output and errors by default:
  (shell-wrapper {} "ls" "-l")

  2. Can be called without an opts map, assuming default values:
  (shell-wrapper "ls" "-l")

  3. Disabling logging using the :log? key in the opts map:
  (shell-wrapper {:log? false} "ls" "-l")
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/utils.clj#L109-L143">Source</a></sub></p>

## <a name="triangulum.utils/snake->kebab">`snake->kebab`</a><a name="triangulum.utils/snake->kebab"></a>
``` clojure

(snake->kebab snake_str)
```

Converts snake_str to kebab-str.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/utils.clj#L16-L19">Source</a></sub></p>

-----
# <a name="triangulum.views">triangulum.views</a>






## <a name="triangulum.views/body->transit">`body->transit`</a><a name="triangulum.views/body->transit"></a>
``` clojure

(body->transit body)
```

Produces a transit response body.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/views.clj#L291-L297">Source</a></sub></p>

## <a name="triangulum.views/not-found-page">`not-found-page`</a><a name="triangulum.views/not-found-page"></a>
``` clojure

(not-found-page request)
```

Produces a not found response.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/views.clj#L284-L289">Source</a></sub></p>

## <a name="triangulum.views/render-page">`render-page`</a><a name="triangulum.views/render-page"></a>
``` clojure

(render-page uri)
```

Returns the page's html.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/views.clj#L262-L282">Source</a></sub></p>

-----
# <a name="triangulum.worker">triangulum.worker</a>


Responsible for the management of worker lifecycle within the
  `:server` context, specifically those defined under the `:workers`
  key. This namespace furnishes functions to initiate and terminate
  workers, maintaining their current state within an atom.




## <a name="triangulum.worker/start-workers!">`start-workers!`</a><a name="triangulum.worker/start-workers!"></a>




Starts a set of workers based on the provided configuration.
  The workers parameter can be either a map (for nested workers)
  or a vector (for namespaced workers).
  For nested workers, the map keys are worker names and values are
  maps with `:start` (a symbol representing the start function) and
  `:stop` keys. The start function is called to start the worker.

  For namespaced workers, the vector elements are maps with:
  - `::name`  - the worker name
  - `::start` - a symbol representing the start function
  - `::stop`  - symbol representing the stop function.

  The start function is called to start each worker.

  Arguments:
  - [`workers`](#triangulum.worker/workers) - a map or vector representing the workers to be started.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/worker.clj#L37-L54">Source</a></sub></p>

## <a name="triangulum.worker/stop-workers!">`stop-workers!`</a><a name="triangulum.worker/stop-workers!"></a>




Stops a set of currently running workers.
  The workers to stop are determined based on the current state of the [[`workers`](#triangulum.worker/workers)](#triangulum.worker/workers) atom. If the [[`workers`](#triangulum.worker/workers)](#triangulum.worker/workers) atom contains a map, it's assumed to be holding nested workers. If it contains a vector, it's assumed to be holding namespaced workers.
  The stop function is called with the value to stop each worker.
<p><sub><a href="https://github.com/sig-gis/triangulum/blob/main/src/triangulum/worker.clj#L82-L86">Source</a></sub></p>
