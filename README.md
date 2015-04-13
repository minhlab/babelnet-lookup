A simple BabelNet web service for user and non-Java programs.

## Build

### Install BabelNet API

1. Change directory into BabelNet-API-2.5.1
2. Compile: `ant -f build.xml`
3. Install into local Maven repository:
 
 `mvn install:install-file -Dfile=babelnet-api-2.5.1.jar -DgroupId=it.uniroma1.lcl -DartifactId=babelnet -Dversion=2.5.1 -Dpackaging=jar -DgeneratePom=true`
 
 `mvn install:install-file -Dfile=lib/jlt-1.0.0.jar -DgroupId=it.uniroma1.lcl -DartifactId=jlt -Dversion=1.0.0 -Dpackaging=jar -DgeneratePom=true`
4. Make sure that the `babelnet.dir` property in 
`config/babelnet.var.properties` point to the directory 
where BabelNet data lives. 

### Build babelnet-lookup

1. Change directory into `babelnet-lookup`
2. Run `mvn clean package`
3. You're ready!

## Usage

### Start

Run `run.sh` to start the server. By default it will listen to port `9000`.

The script assumes that BabelNet jar and libraries are in a directory named 
`BabelNet-API-2.5.1` in the same directory as babelnet-lookup. If your 
installation is different, you need to change it accordingly.

### Stop

Press `Ctrl+C` to terminate the server.

### Query using a web browser (or curl)

- Text to BabelNet: [localhost:9000/text/en/mouse](http://localhost:9000/text/en/mouse) (change to your language and key word).
- WordNet to BabelNet: [localhost:9000/wordnet/15203791n](http://localhost:9000/wordnet/15203791n) (change to your offset).
- Wikipedia to BabelNet: [localhost:9000/wikipedia/Mars/n](http://localhost:9000/wikipedia/Mars/n) 
(plugin your page, the second place is POS, being one of these values: 
`n` (noun), `v` (verb), `r` (adverb), `a` (adjective)).
- Related synsets: [localhost:9000/synset/bn:00000002n/related](http://localhost:9000/synset/bn:00000002n/related) (change to your offset).
- Senses: [localhost:9000/synset/bn:00000002n/senses/en](http://localhost:9000/synset/bn:00000002n/senses/en) (change to your offset, language code is optional).
- DBpedia URIs: [localhost:9000/synset/bn:00000002n/dbpedia_uri/en](http://localhost:9000/synset/bn:00000002n/dbpedia_uri/en) (change to your offset, language code is optional).

### Query using Python

**Text:**

```python
import urllib
url = "http://%s:%d/text/%s/%s" %(host, port, lang, query)
f = urllib.urlopen(url)
if f.getcode() == 200:
    synsets = f.read().strip().split("\n")
```

**WordNet:**

```python
import urllib
url = "http://%s:%d/wordnet/%s" %(host, port, offset)
f = urllib.urlopen(url)
if f.getcode() == 200:
    synsets = f.read().strip().split("\n")
```

**Wikipedia**

```python
url = "http://%s:%d/wikipedia/%s/n" %(host, port, offset)
f = urllib.urlopen(url)
if f.getcode() == 200:
	synsets = f.read().strip().split("\n")
```

**Related synsets**

```python
url = "http://%s:%d/synset/%s/related" %(host, port, offset)
f = urllib.urlopen(url)
if f.getcode() == 200:
    lines = f.read().strip().split("\n")
    lines = [line.split('\t') for line in lines]
    related_synsets = [{'relation': fields[0], 'synset': fields[1]}
                for fields in lines] 
```

**Senses**

```python
url = "http://%s:%d/synset/%s/senses/en" %(host, port, offset)
f = urllib.urlopen(url)
if f.getcode() == 200:
    lines = f.read().strip().split("\n")
    lines = [line.split('\t') for line in lines]
    senses = [{'lemma': fields[0], 'pos': fields[1], 'language': fields[2], 'source': fields[3]}
                for fields in lines] 
```


**DBpedia URI**

```python
url = "http://%s:%d/synset/%s/dbpedia_uri/en" %(host, port, offset)
f = urllib.urlopen(url)
if f.getcode() == 200:
    uris = f.read().strip().split("\n")
```

**Notice:** urllib doesn't support `with ... as ...` construction.
