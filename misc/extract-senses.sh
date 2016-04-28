BABELNET_HOME=/PATH/TO/BabelNet-API-2.5.1/
BLOOKUP_HOME=/PATH/TO/babelnet-lookup

cd $BLOOKUP_HOME
mvn clean package
cd $BABELNET_HOME

# generate a text file with all triples
java -cp lib/*:$BLOOKUP_HOME:$BLOOKUP_HOME/target/lib/*:$BLOOKUP_HOME/target/babelnet-lookup-0.0.1-SNAPSHOT.jar spinoza.util.TripletGenerator -s 

