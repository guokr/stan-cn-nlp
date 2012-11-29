(ns com.guokr.stancnnlp.seg)

(def props (doto (java.util.Properties.)
               (.setProperty "sighanCorporaDict" "./")
               (.setProperty "serDictionary" "./dict-chris6.ser.gz")
               (.setProperty "inputEncoding" "utf-8")
               (.setProperty "outputEncoding" "utf-8")
               (.setProperty "sighanPostProcessing" "true")
               (.setProperty "NormalizationTable" "./resources/norm.simp.utf8")
               (.setProperty "normTableEncoding" "UTF-8")))
(def classifier (edu.stanford.nlp.ie.crf.CRFClassifier/getClassifier "ctb.gz" props))

(defn seg [text] (.classifyToString classifier text))
