(ns com.guokr.stancnnlp.tag
  (:require [com.guokr.stancnnlp.seg :as seg]))

(def tagger (edu.stanford.nlp.tagger.maxent.MaxentTagger. "chinese-distsim.tagger"))

(defn tag [text] (.tagString tagger (seg/seg text)))
