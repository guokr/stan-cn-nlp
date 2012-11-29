(ns com.guokr.nlp.seg)

(def model (.getFile (clojure.java.io/resource "ctb.gz")))
(def dictionary (.getFile (clojure.java.io/resource "dict-chris6.ser.gz")))
(def norm (.getFile (clojure.java.io/resource "norm.simp.utf8")))
(def sighan (.substring model 0 (.lastIndexOf model java.io.File/separator)))

(def props (doto (java.util.Properties.)
               (.setProperty "serDictionary" dictionary)
               (.setProperty "NormalizationTable" norm)
               (.setProperty "sighanCorporaDict" sighan)
               (.setProperty "sighanPostProcessing" "true")
               (.setProperty "inputEncoding" "UTF-8")
               (.setProperty "outputEncoding" "UTF-8")
               (.setProperty "normTableEncoding" "UTF-8")))

(def classifier (edu.stanford.nlp.ie.crf.CRFClassifier/getClassifier model props))

(defn seg [text] (.classifyToString classifier text))
