(defproject com.guokr.nlp/stan-cn-nlp "0.1.0"
    :description "A wrapper of Stanford NLP packages for Simplified Chinese users"
    :dependencies [[org.clojure/clojure "1.4.0"]
                   [edu.stanford.nlp/stanford-corenlp "1.3.3"]]

    :profiles {:dev {:dependencies [[junit/junit "4.11"]]}}
    :plugins [[lein-junit "1.0.3"]]
    :hooks [leiningen.hooks.junit]

    :source-paths ["src/main/clojure"]
    :java-source-paths ["src/main/java"]
    :resource-paths ["src/main/resources"]

    :test-paths ["src/test/clojure" "src/test/java"]
    :junit ["src/test/java"]

    :compile-path "target/classes"
    :target-path "target/"
    :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]
    :jar-name "stan-cn-nlp.jar"
    :uberjar-name "stan-cn-nlp-standalone.jar" ;

    :jvm-opts ["-Xmx2g"])
