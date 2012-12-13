stan-cn-nlp
===========

a API wrapper of Stanford NLP packages for the convenience of Chinese users.

How to play
------------

Please follow below steps to play with:

* git clone git://github.com/guokr/stan-cn-nlp.git
* cd stan-cn-nlp
* mvn package
* java -Xms1g -Xmx2g -jar target/stan-cn-nlp-0.1.0.jar seg "大江东去浪淘尽"
* java -Xms1g -Xmx2g -jar target/stan-cn-nlp-0.1.0.jar ner "大江东去浪淘尽"
* java -Xms1g -Xmx2g -jar target/stan-cn-nlp-0.1.0.jar tag "大江东去浪淘尽"

The API
------------

We use a very simple API to reduce the complexity.

Refer to default configuration(optional):

    SegWrapper.defaults;

Loading configuration(optional):

    SegWrapper.reload(props, defaults);

Segmenting text:

    SegWrapper.segment(text);

NerWrapper and TagWrapper are very similar.



