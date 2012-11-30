(ns com.guokr.nlp.ner)

(defn ner [text] (com.guokr.nlp.NerWrapper/recognize text))
