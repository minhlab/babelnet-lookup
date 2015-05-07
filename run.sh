BABELNET_HOME=../BabelNet-API-2.5.1
BLOOKUP_HOME=`pwd`

cd $BABELNET_HOME

java -cp lib/*:$BLOOKUP_HOME:$BLOOKUP_HOME/target/lib/*:$BLOOKUP_HOME/target/babelnet-lookup-0.0.1-SNAPSHOT.jar spinoza.blookup.BabelNetServer
